package dev.ahlquist.lox_interp.main;

import java.util.List;

public class LoxFunction implements LoxCallable {

    private final Stmt.Function declaration;
    private final Environment closure;
    private final boolean isInitializer;

    public LoxFunction(Stmt.Function declaration, Environment closure, boolean isInitializer) {
        this.declaration = declaration;
        this.closure = closure;
        this.isInitializer = isInitializer;
    }

    LoxFunction bind(LoxInstance instance) {
        Environment environment = new Environment(closure);
        environment.define("this", instance);
        return new LoxFunction(declaration, environment, isInitializer);
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment environment = new Environment(this.closure);
        for (int i=0; i<declaration.params.size(); i++)
            environment.define(
                    declaration.params.get(i).lexeme,
                    args.get(i)
            );

        try {
            interpreter.executeBlock(declaration.body, environment);
        } catch (Return returnValue) {
            // Initializers may use return (with no value). Doing so returns the instance.
            if(isInitializer) return closure.getAt("this", 0);

            return returnValue.value;
        }

        if(isInitializer) return closure.getAt("this", 0);
        return null;
    }

    @Override
    public int arity() {
        return declaration.params.size();
    }

    @Override
    public String toString() {
        return "<fn" + declaration.name.lexeme + ">";
    }
}
