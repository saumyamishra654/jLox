package com.sjlox.lox;

import java.util.HashMap;
import java.util.Map;

class LoxInstance {
  // stores a reference to the class this instance belongs to
  private LoxClass klass;

  // stores instance-specific fields
  private final Map<String, Object> fields = new HashMap<>();

  // constructor initializes the instance with its class
  LoxInstance(LoxClass klass) {
    this.klass = klass;
  }

  // retrieves a property or method from the instance
  Object get(Token name) {
    // check if the property exists in the instance's fields
    if (fields.containsKey(name.lexeme)) {
      return fields.get(name.lexeme);
    }

    // check if the method exists in the class definition
    LoxFunction method = klass.findMethod(name.lexeme);
    if (method != null) return method.bind(this); // bind method to this instance

    // throw an error if the property or method is not found
    throw new RuntimeError(name, "Undefined property '" + name.lexeme + "'.");
  }

  // sets a property on the instance
  void set(Token name, Object value) {
    fields.put(name.lexeme, value);
  }

  // returns a string representation of the instance
  @Override
  public String toString() {
    return klass.name + " instance";
  }
}
