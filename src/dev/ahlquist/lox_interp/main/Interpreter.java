package dev.ahlquist.lox_interp.main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {

    final Environment globals = new Environment();
    private Environment environment = globals;
    private Map<Expr, Integer> locals = new HashMap<>();

    public Interpreter() {
        globals.define("clock", new LoxCallable() {
            @Override
            public Object call(Interpreter interpreter, List<Object> args) {
                return (double) System.currentTimeMillis() / 1000.0;
            }

            @Override
            public int arity() {
                return 0;
            }
        });
    }

    void interpret(List<Stmt> statements) {
        try {
            for (Stmt statement : statements) {
                execute(statement);
            }
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
        }
    }

    private void execute(Stmt stmt) {
        stmt.accept(this);
    }

    /* Implement Expr.Visitor<Object> */
    @Override
    public Object visitBinaryExpr(Expr.Binary expr) {
        Object left = evaluate(expr.left);
        Object right = evaluate(expr.right);

        switch(expr.operator.type) {
            case GREATER:
                checkNumberOperands(expr.operator, left, right);
                return (double)left > (double)right;
            case GREATER_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left >= (double)right;
            case LESS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left < (double)right;
            case LESS_EQUAL:
                checkNumberOperands(expr.operator, left, right);
                return (double)left <= (double)right;

            case PLUS:
                return evaluatePlus(expr.operator, left, right);
            case MINUS:
                checkNumberOperands(expr.operator, left, right);
                return (double)left - (double)right;
            case SLASH:
                checkNumberOperands(expr.operator, left, right);
                return (double)left / (double)right;
            case STAR:
                checkNumberOperands(expr.operator, left, right);
                return (double)left * (double)right;

            case BANG_EQUAL:
                return !isEqual(left, right);
            case EQUAL_EQUAL:
                return isEqual(left, right);

            default:
                return null; // unreachable
        }
    }

    @Override
    public Object visitGroupingExpr(Expr.Grouping expr) {
        return evaluate(expr.expression);
    }

    @Override
    public Object visitLiteralExpr(Expr.Literal expr) {
        return expr.value;
    }

    @Override
    public Object visitLogicalExpr(Expr.Logical expr) {
        Object left = evaluate(expr.left);

        if(expr.operator.type == TokenType.OR) {
           if(isTruthy(left)) return left;
        } else {
            if(!isTruthy(left)) return left;
        }
        return evaluate(expr.right);
    }

    @Override
    public Object visitSetExpr(Expr.Set expr) {
        Object object = evaluate(expr.expr);

        if(!(object instanceof LoxInstance))
            throw new RuntimeError(expr.name, "Only instances have fields.");

        Object value = evaluate(expr.value);
        ((LoxInstance) object).set(expr.name, value);

        return value;
    }

    @Override
    public Object visitThisExpr(Expr.This expr) {
        return lookupVariable(expr.keyword, expr);
    }

    @Override
    public Object visitUnaryExpr(Expr.Unary expr) {
        Object right = expr.right;

        switch (expr.operator.type) {
            case BANG:
                return !isTruthy(right);
            case MINUS:
                checkNumberOperand(expr.operator, right);
                return (double)right;

            default:
                return null; // unreachable
        }
    }

    @Override
    public Object visitVariableExpr(Expr.Variable expr) {
        return lookupVariable(expr.name, expr);
    }

    private Object lookupVariable(Token name, Expr expr) {
        Integer distance = locals.get(expr);
        if(distance != null) {
            return environment.getAt(name.lexeme, distance);
        } else {
            return globals.get(name);
        }
    }

    @Override
    public Object visitAssignExpr(Expr.Assign expr) {
        Object value = evaluate(expr.value);

        Integer depth = locals.get(expr);
        if(depth != null) {
            environment.assignAt(expr.name, value, depth);
        } else {
            globals.assign(expr.name, value);
        }

        return value;
    }

    @Override
    public Object visitCallExpr(Expr.Call expr) {
        Object callee = evaluate(expr.callee);

        List<Object> args = new ArrayList<>();
        for (Expr arg : expr.args) {
            args.add(evaluate(arg));
        }

        if(!(callee instanceof LoxCallable)) {
            throw new RuntimeError(expr.paren, "Can only call functions and classes.");
        }
        LoxCallable function = (LoxCallable) callee;
        if(args.size() != function.arity()) {
            throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments, but got " + args.size());
        }

        return function.call(this, args);
    }

    @Override
    public Object visitGetExpr(Expr.Get expr) {
        Object object = expr.expr;
        if (object instanceof LoxInstance)
            return ((LoxInstance) object).get(expr.name);

        throw new RuntimeError(expr.name, "Only instances have properties.");
    }

    @Override
    public Void visitReturnStmt(Stmt.Return stmt) {
        Object value = null;
        if(stmt.value != null)
            value = evaluate(stmt.value);

        throw new Return(value);
    }

    /* Implement Stmt.Visitor<Void> */
    @Override
    public Void visitExpressionStmt(Stmt.Expression stmt) {
        evaluate(stmt.expression);
        return null;
    }

    @Override
    public Void visitPrintStmt(Stmt.Print stmt) {
        Object result = evaluate(stmt.expression);
        System.out.println(stringify(result));
        return null;
    }

    @Override
    public Void visitVarStmt(Stmt.Var stmt) {
        Object value = null;
        if (stmt.initializer != null) {
            value = evaluate(stmt.initializer);
        }

        environment.define(stmt.name.lexeme, value);
        return null;
    }

    @Override
    public Void visitFunctionStmt(Stmt.Function stmt) {
        LoxFunction function = new LoxFunction(stmt, environment, false);
        environment.define(stmt.name.lexeme, function);
        return null;
    }

    @Override
    public Void visitBlockStmt(Stmt.Block stmt) {
        executeBlock(stmt.statements, new Environment(environment));
        return null;
    }

    @Override
    public Void visitClassStmt(Stmt.Class stmt) {
        environment.define(stmt.name.lexeme, null);

        Map<String, LoxFunction> methods = new HashMap<>();
        for(Stmt.Function method : stmt.methods) {
            LoxFunction function
                    = new LoxFunction(method, environment, "init".equals(method.name.lexeme));
            methods.put(method.name.lexeme, function);
        }

        LoxClass klass = new LoxClass(stmt.name.lexeme, methods);
        environment.assign(stmt.name, klass);
        return null;
    }

    @Override
    public Void visitIfStmt(Stmt.If stmt) {
        if(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.thenBranch);
        } else if(stmt.elseBranch != null) {
            execute(stmt.elseBranch);
        }
        return null;
    }

    @Override
    public Void visitWhileStmt(Stmt.While stmt) {
        while(isTruthy(evaluate(stmt.condition))) {
            execute(stmt.body);
        }
        return null;
    }

    public void executeBlock(List<Stmt> statements, Environment env) {
        Environment previous = this.environment;
        try {
            this.environment = env;
            for(Stmt statement : statements)
                execute(statement);

        } finally {
            this.environment = previous;
        }
    }

    /* Private Methods */
    private void checkNumberOperand(Token operator, Object operand) {
        if (operand instanceof Double) return;
        throw new RuntimeError(operator, "Operand must be a number.");
    }

    private void checkNumberOperands(Token operator, Object left, Object right) {
        if (left instanceof Double && right instanceof Double) return;
        throw new RuntimeError(operator, "Operands must be numbers.");
    }

    // Add numbers or concatenate strings
    private Object evaluatePlus(Token operator, Object left, Object right) {
        if(left instanceof Double && right instanceof Double)
            return (double)left + (double)right;
        else if(left instanceof String && right instanceof String)
            return (String)left + (String)right;
        else
            throw new RuntimeError(operator, "Operands must be both numbers or both strings.");
    }

    // 'nil' and 'false' are falsey. Everything else is truthy.
    private boolean isTruthy(Object object) {
        if (object == null) return false;
        if (object instanceof Boolean) return (boolean) object;
        return true;
    }

    private boolean isEqual(Object left, Object right) {
        // 'nil' is only equal to 'nil'
        if(left == null && right == null) return true;
        if(left == null) return false; // avoid NPE below

        return left.equals(right);
    }

    private Object evaluate(Expr expr) {
        return expr.accept(this);
    }

    private String stringify(Object object) {
        if (object == null) return "nil";

        // Hack. Work around Java adding ".0" to integer-valued doubles.
        if (object instanceof Double) {
            String text = object.toString();
            if (text.endsWith(".0")) {
                text = text.substring(0, text.length() - 2);
            }
            return text;
        }

        return object.toString();
    }

    public void resolve(Expr expr, int depth) {
        locals.put(expr, depth);
    }
}
