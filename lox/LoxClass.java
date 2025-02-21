package com.sjlox.lox;

import java.util.List;
import java.util.Map;

class LoxClass implements LoxCallable {
  final String name; // stores the name of the class
  final LoxClass superclass; // reference to the superclass if the class has one
  private final Map<String, LoxFunction> methods; // stores methods defined in the class

  // constructor to initialize a LoxClass with its name, superclass, and methods
  LoxClass(String name, LoxClass superclass, Map<String, LoxFunction> methods) {
    this.superclass = superclass;
    this.name = name;
    this.methods = methods;
  }

  // searches for a method in the class, and if not found, checks the superclass
  LoxFunction findMethod(String name) {
    if (methods.containsKey(name)) {
      return methods.get(name);
    }
    if (superclass != null) {
      return superclass.findMethod(name);
    }
    return null;
  }

  @Override
  public String toString() {
    return name; // returns the class name as a string
  }

  // handles object instantiation and calls the initializer method if it exists
  @Override
  public Object call(Interpreter interpreter, List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this); // creates a new instance of the class
    LoxFunction initializer = findMethod("init"); // looks for an initializer method
    if (initializer != null) {
      initializer.bind(instance).call(interpreter, arguments); // binds and calls the initializer
    }
    return instance; // returns the newly created instance
  }

  // returns the number of arguments required by the initializer, if present
  @Override
  public int arity() {
    LoxFunction initializer = findMethod("init");
    if (initializer == null) return 0;
    return initializer.arity();
  }
}
