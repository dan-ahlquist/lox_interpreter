package dev.ahlquist.lox_interp.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.ahlquist.lox_interp.main.TokenType.*;

class Scanner {
    private final String source;

    private final List<Token> tokens = new ArrayList<>();

    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    AND);
        keywords.put("class",  CLASS);
        keywords.put("else",   ELSE);
        keywords.put("false",  FALSE);
        keywords.put("for",    FOR);
        keywords.put("fun",    FUN);
        keywords.put("if",     IF);
        keywords.put("nil",    NIL);
        keywords.put("or",     OR);
        keywords.put("print",  PRINT);
        keywords.put("return", RETURN);
        keywords.put("super",  SUPER);
        keywords.put("this",   THIS);
        keywords.put("true",   TRUE);
        keywords.put("var",    VAR);
        keywords.put("while",  WHILE);
    }

    Scanner(String source) {
        this.source = source;
    }

    List<Token> scanTokens() {
        while (!isAtEnd()) {
            // We are at the beginning of the next lexeme.
            start = current;
            scanToken();
        }

        tokens.add(new Token(EOF, "", null, line));
        return tokens;
    }

    private void scanToken() {
        char c = advance();
        switch (c) {

            // The 1-byte tokens.
            case '(': addToken(LEFT_PAREN); break;
            case ')': addToken(RIGHT_PAREN); break;
            case '{': addToken(LEFT_BRACE); break;
            case '}': addToken(RIGHT_BRACE); break;
            case ',': addToken(COMMA); break;
            case '.': addToken(DOT); break;
            case '-': addToken(MINUS); break;
            case '+': addToken(PLUS); break;
            case ';': addToken(SEMICOLON); break;
            case '*': addToken(STAR); break;

            // Look-ahead for 2-byte tokens. match() will advance the cursor if needed.
            case '!': addToken(match('=') ? BANG_EQUAL : BANG); break;
            case '=': addToken(match('=') ? EQUAL_EQUAL : EQUAL); break;
            case '<': addToken(match('=') ? LESS_EQUAL : LESS); break;
            case '>': addToken(match('=') ? GREATER_EQUAL : GREATER); break;

            // Look-ahead for a comment.
            case '/':
                if (match('/'))
                    lineComment();
                else if(match('*'))
                    blockComment();
                else
                    addToken(SLASH);
                break;

            // Ignore whitespace
            case ' ':
            case '\r':
            case '\t':
                break;

            // Newline
            case '\n': line++; break;

            // String literal
            case '"': string(); break;

            default:
                if (isDigit(c)) {
                    number();
                } else if (isAlpha(c)) { // [a-z][A-Z] or '_'
                    identifier();
                } else {
                    Lox.error(line, "Unexpected character.");
                }
                break;
        }
    }

    private boolean isAtEnd() {
        return current >= source.length();
    }

    private char advance() {
        current++;
        return source.charAt(current - 1);
    }

    private boolean match(char expected) {
        if (isAtEnd())
            return false;

        if (source.charAt(current) != expected) {
            return false;
        } else {
            current++;
            return true;
        }
    }

    private char peek() {
        if(isAtEnd())
            return '\0'; //null char
        else
            return source.charAt(current);
    }

    private char peekNext() {
        if(current + 1 >= source.length())
            return '\0'; //null char
        else
            return source.charAt(current + 1);
    }

    // Consume characters until we're at the EOL or EOF
    private void lineComment() {
        while (peek() != '\n' && !isAtEnd())
            advance();
        // The trailing newline will be consumed in the next scanToken() call
    }

    // Scan a block comment, which may span multiple newlines.
    private void blockComment() {
        while ( !(peek() == '*' && peekNext() == '/') && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated block comment.");
            return;
        }

        // The closing "*/". Implied by above conditions.
        advance();
        advance();
    }

    // Scan a string literal, which may span multiple newlines.
    // Those newlines are included in the resulting string.
    private void string() {
        while (peek() != '"' && !isAtEnd()) {
            if (peek() == '\n') line++;
            advance();
        }

        if (isAtEnd()) {
            Lox.error(line, "Unterminated string.");
            return;
        }

        // The closing double-quote. Implied by above conditions.
        advance();

        // Trim the surrounding quotes.
        String value = source.substring(start + 1, current - 1);
        addToken(STRING, value);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    // Scan in a number literal which may be floating-point or integer.
    // E.g.: 1234 or 12.34
    // But not: .1234 or 1234.
    private void number() {
        while (isDigit(peek())) advance();

        // Look for a fractional part.
        if (peek() == '.' && isDigit(peekNext())) {
            // Consume the "."
            advance();

            while (isDigit(peek())) advance();
        }

        // TODO catch NumberFormatException and give a Lox error
        addToken(NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    // Scan in alphanumeric lexemes, which will include reserved words and user-def identifiers.
    private void identifier() {
        while (isAlphaNumeric(peek())) advance();

        String text = source.substring(start, current);

        // Check if this is a reserved word. If not, it's an identifier.
        TokenType type = keywords.getOrDefault(text, IDENTIFIER);

        addToken(type);
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                c == '_';
    }

    private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private void addToken(TokenType type) {
        addToken(type, null);
    }

    private void addToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        tokens.add(new Token(type, text, literal, line));
    }
}
