package dev.ahlquist.lox_interp.main;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
    private LoxClass klass;
    private Map<String, Object> fields = new HashMap<>();

    LoxInstance(LoxClass klass) {
        this.klass = klass;
    }

    @Override
    public String toString() {
        return klass.name + " instance";
    }

    public Object get(Token name) {
        // Fields shadow methods.
        if(fields.containsKey(name.lexeme))
            return fields.get(name.lexeme);

        LoxFunction method = klass.findMethod(name.lexeme);
        if(method != null) return method.bind(this);

        throw new RuntimeError(name, "Undefined property: " + name.lexeme);
    }

    public void set(Token name, Object value) {
        fields.put(name.lexeme, value);
    }
}