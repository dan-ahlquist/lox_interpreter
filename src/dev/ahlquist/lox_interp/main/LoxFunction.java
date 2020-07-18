package dev.ahlquist.lox_interp.main;

import java.util.List;

public class LoxFunction implements LoxCallable {

    Stmt.Function declaration;

    public LoxFunction(Stmt.Function declaration) {
        this.declaration = declaration;
    }

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        Environment environment = new Environment(interpreter.globals);
        for (int i=0; i<declaration.params.size(); i++)
            environment.define(
                    declaration.params.get(i).lexeme,
                    args.get(i)
            );

        interpreter.executeBlock(declaration.body, environment);
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
