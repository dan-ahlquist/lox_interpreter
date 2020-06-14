package dev.ahlquist.lox_interp.main;

public class Interpreter implements Expr.Visitor<Object> {

    String interpret(Expr expression) {
        try {
            Object value = evaluate(expression);
            return stringify(value);
//            System.out.println(stringify(value));
        } catch (RuntimeError error) {
            Lox.runtimeError(error);
            return "";
        }
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
}
