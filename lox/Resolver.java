package com.sjlox.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.sjlox.lox.Lox;

class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void> {
  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>(); // Stack of scope maps
  private FunctionType currentFunction = FunctionType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }
  
  // enum to track whether we are inside a function, initializer, or method
  private enum FunctionType {
    NONE,
    FUNCTION,
    INITIALIZER,
    METHOD
  }

  // enum to track whether we are inside a class or subclass
  private enum ClassType {
    NONE,
    CLASS,
    SUBCLASS
  }

  private ClassType currentClass = ClassType.NONE;

  //Resolves a list of statements by iterating over them.
   
  void resolve(List<Stmt> statements) {
    for (Stmt statement : statements) {
      resolve(statement);
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope(); // enter a new block scope
    resolve(stmt.statements); // resolve the statements inside the block
    endScope(); // exit the block scope
    return null;
  }

  @Override
  public Void visitClassStmt(Stmt.Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS; // mark that we are inside a class

    declare(stmt.name);
    define(stmt.name);

    // prevent a class from inheriting from itself
    if (stmt.superclass != null &&
        stmt.name.lexeme.equals(stmt.superclass.name.lexeme)) {
      Lox.error(stmt.superclass.name,
          "A class can't inherit from itself.");
    }

    // if the class has a superclass, resolve it first
    if (stmt.superclass != null) {
      currentClass = ClassType.SUBCLASS;
      resolve(stmt.superclass);
    }

    // if the class is a subclass, introduce 'super' in its scope
    if (stmt.superclass != null) {
      beginScope();
      scopes.peek().put("super", true);
    }

    // introduce 'this' in the class scope
    beginScope();
    scopes.peek().put("this", true);

    // resolve methods inside the class
    for (Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("init")) {
        declaration = FunctionType.INITIALIZER;
      }

      resolveFunction(method, declaration); // resolve method declarations
    }

    endScope(); // exit method scope

    if (stmt.superclass != null) endScope(); // exit superclass scope if applicable

    currentClass = enclosingClass; // restore previous class context
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression); // resolve the expression statement
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt, FunctionType.FUNCTION); // resolve function body
    return null;
  }

  @Override
  public Void visitDefaultStmt(Stmt.Default stmt) {
      return null; // no additional resolution needed for default case
  }
    
  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    return null; // no additional resolution needed for break
  }
  
  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition); // resolve the condition expression
    resolve(stmt.thenBranch); // resolve the 'then' branch
    if (stmt.elseBranch != null) resolve(stmt.elseBranch); // resolve the 'else' branch if present
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    resolve(stmt.expression); // resolve the expression to be printed
    return null;
  }
  
  @Override
  public Void visitCaseStmt(Stmt.Case stmt) {
      resolve(stmt.value);
      resolve(stmt.statements);
      return null;
  }  
  
  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }

    if (stmt.value != null) {
      if (currentFunction == FunctionType.INITIALIZER) {
        Lox.error(stmt.keyword,
            "Can't return a value from an initializer.");
      }

      resolve(stmt.value); // resolve the return value
    }

    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name); // declare the variable
    if (stmt.initializer != null) {
      resolve(stmt.initializer); // resolve the initializer expression if present
    }
    define(stmt.name); // define the variable
    return null;
  }
  
  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition); // resolve the loop condition
    resolve(stmt.body); // resolve the loop body
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value); // resolve the assigned value
    resolveLocal(expr, expr.name); // resolve variable assignment in the correct scope
    return null;
  }

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left); // resolve left operand
    resolve(expr.right); // resolve right operand
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee); // resolve the function being called

    for (Expr argument : expr.arguments) {
      resolve(argument); // resolve each argument
    }

    return null;
  }

  @Override
  public Void visitGetExpr(Expr.Get expr) {
    resolve(expr.object); // resolve the object whose property is accessed
    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression); // resolve the grouped expression
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null; // resolve are already fully resolved
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left); // resolve left operand
    resolve(expr.right); // resolve right operand
    return null;
  }

  @Override
  public Void visitSetExpr(Expr.Set expr) {
    resolve(expr.value); // resolve the value being assigned
    resolve(expr.object); // resolve the object whose property is being set
    return null;
  }

  @Override
  public Void visitSwitchStmt(Stmt.Switch stmt) {
    resolve(stmt.condition);
    for (Stmt.Case caseStmt : stmt.cases) {
        resolve(caseStmt.value);
        resolve(caseStmt.statements);
    }
    if (stmt.defaultCase != null) {
        resolve(stmt.defaultCase.statements);
    }
    return null;
  }

  @Override
  public Void visitSuperExpr(Expr.Super expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Can't use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      Lox.error(expr.keyword,
          "Can't use 'super' in a class with no superclass.");
    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitThisExpr(Expr.This expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Can't use 'this' outside of a class.");
      return null;
    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr) {
    if (!scopes.isEmpty() &&
        scopes.peek().get(expr.name.lexeme) == Boolean.FALSE) {
      Lox.error(expr.name,
          "Can't read local variable in its own initializer.");
    }

    resolveLocal(expr, expr.name);
    return null;
  }

  private void resolve(Stmt stmt) {
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void resolveFunction(
      Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
  }

  private void beginScope() {
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope() {
    scopes.pop();
  }

  private void declare(Token name) {
    if (scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();
    if (scope.containsKey(name.lexeme)) {
      Lox.error(name,
          "Already a variable with this name in this scope.");
    }

    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if (scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }
  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if (scopes.get(i).containsKey(name.lexeme)) {
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }
}
