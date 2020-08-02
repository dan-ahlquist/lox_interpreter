package dev.ahlquist.lox_interp.main;

import java.util.HashMap;
import java.util.Map;

class Environment {

    final Environment parent;
    private final Map<String, Object> values = new HashMap<>();

    public Environment() {
        parent = null;
    }

    public Environment(Environment parent) {
        this.parent = parent;
    }

    void define(String name, Object value) {
        // Note: overwriting here means declaring a variable multiple times is allowed.
        values.put(name, value);
    }

    Object get(Token name) {
        if (values.containsKey(name.lexeme)) {
            return values.get(name.lexeme);
        }

        if(parent != null) return parent.get(name);

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    Object getAt(Token name, int depth) {
        return ancestor(depth).get(name);
    }

    private Environment ancestor(int depth) {
        Environment environment = this;
        for (int i = 0; i < depth; i++) {
            environment = environment.parent;
        }
        return environment;
    }

    void assign(Token name, Object value) {
        if (values.containsKey(name.lexeme)) {
            values.put(name.lexeme, value);
            return;
        }

        if(parent != null) {
            parent.assign(name, value);
            return;
        }

        throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
    }

    void assignAt(Token name, Object value, int depth) {
        ancestor(depth).values.put(name.lexeme, value);
    }
}
