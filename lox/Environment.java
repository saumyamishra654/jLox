package com.sjlox.lox;

import java.util.HashMap;
import java.util.Map;

class Environment {
  final Environment enclosing; //giving reference to the environment it is envlosed in
  private final Map<String, Object> values = new HashMap<>(); // map to store bindings

  Environment() {
    enclosing = null;
  }

  Environment(Environment enclosing) {
    this.enclosing = enclosing;
  }

  Object get(Token name) { //search for value of variable, given its na,e
    if (values.containsKey(name.lexeme)) {
      return values.get(name.lexeme);
    }
    if (enclosing != null) return enclosing.get(name); //throw error if the variable has not been previously defined (cannot be too lax, even in lox)
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }

  void assign(Token name, Object value) {
    if (values.containsKey(name.lexeme)) {
      values.put(name.lexeme, value);
      return;
    }
    if (enclosing != null) {
      enclosing.assign(name, value);
      return;
    }
    throw new RuntimeError(name, "Undefined variable '" + name.lexeme + "'.");
  }
  // defines a new variable in the current environment
  void define(String name, Object value) { //bind name to value
    values.put(name, value);
  }

    // finds an ancestor environment at a given distance
  Environment ancestor(int distance) {
    Environment environment = this;
    for (int i = 0; i < distance; i++) {
      environment = environment.enclosing; // move up the environment chain
    }
    return environment;
  }

    // retrieves a variable value at a specific distance in the environment chain
  Object getAt(int distance, String name) {
    return ancestor(distance).values.get(name);
  }

  // assigns a new value to an existing variable at a specific distance
  void assignAt(int distance, Token name, Object value) {
    ancestor(distance).values.put(name.lexeme, value);
  }

  // returns a string representation of the environment, including parent environments
  @Override
  public String toString() {
    String result = values.toString();
    if (enclosing != null) {
      result += " -> " + enclosing.toString();
    }
    return result;
  }
}
