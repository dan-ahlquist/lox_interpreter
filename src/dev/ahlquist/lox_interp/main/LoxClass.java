package dev.ahlquist.lox_interp.main;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
    final String name;

    LoxClass(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /* LoxCallable */

    @Override
    public Object call(Interpreter interpreter, List<Object> args) {
        return new LoxInstance(this);
    }

    @Override
    public int arity() {
        return 0;
    }
}
