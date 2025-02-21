package com.sjlox.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sjlox.lox.Environment;

import com.sjlox.lox.Lox;
import com.sjlox.lox.Stmt.Return;

// interpreter class that evaluates expressions and executes statements
class Interpreter implements Expr.Visitor<Object>, Stmt.Visitor<Void> {
  
  // global environment (stores variables and functions)
  final Environment globals = new Environment();
  private Environment environment = globals;
  private final Map<Expr, Integer> locals = new HashMap<>();

  // constructor initializes native functions
  Interpreter() {
    globals.define("clock", new LoxCallable() {
      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
  }
  
  // interpret a list of statements
  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Lox.runtimeError(error);
    }
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void resolve(Expr expr, int depth) {
    locals.put(expr, depth);
  }

  // execute a block of statements within a new environment
  void executeBlock(List<Stmt> statements, Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      for (Stmt statement : statements) {
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = null;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }
    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitSuperExpr(Expr.Super expr) {
      int distance = locals.get(expr);
      LoxClass superclass = (LoxClass) environment.getAt(distance, "super");

      LoxInstance object = (LoxInstance) environment.getAt(distance - 1, "this");
      LoxFunction method = superclass.findMethod(expr.method.lexeme);

      if (method == null) {
          throw new RuntimeError(expr.method, "Undefined property '" + expr.method.lexeme + "'.");
      }

      return method.bind(object);
  }
  
  

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    while (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.body);
    }
    return null;
  }
  @Override
  public Object visitSetExpr(Expr.Set expr) {
      Object object = evaluate(expr.object);
      if (!(object instanceof LoxInstance)) {
          throw new RuntimeError(expr.name, "Only instances have fields.");
      }
  
      Object value = evaluate(expr.value);
      ((LoxInstance) object).set(expr.name, value);
      return value;
  }
  
  //switch statement handling: NEW FEATURE
  @Override
  public Void visitSwitchStmt(Stmt.Switch stmt) {
    Object switchValue = evaluate(stmt.condition);
    boolean caseMatched = false;

    for (Stmt.Case caseStmt : stmt.cases) {
        Object caseValue = evaluate(caseStmt.value);
        
        // if a case matches or a previous case has matched (for fall-through)
        if (caseMatched || isEqual(switchValue, caseValue)) {
            caseMatched = true; // from now on, execute statements in fall-through mode
            executeCaseStatements(caseStmt.statements);
        }
    }

    // if no case matched, execute the default case if it exists
    if (!caseMatched && stmt.defaultCase != null) {
        executeCaseStatements(stmt.defaultCase.statements);
    }

    return null;
}

private void executeCaseStatements(List<Stmt> statements) {
    for (Stmt stmt : statements) {
        execute(stmt);
    }
}

@Override
public Void visitClassStmt(Stmt.Class stmt) {
    Object superclass = null;
    if (stmt.superclass != null) {
        superclass = evaluate(stmt.superclass);
        if (!(superclass instanceof LoxClass)) {
            throw new RuntimeError(stmt.superclass.name, "Superclass must be a class.");
        }
    }

    environment.define(stmt.name.lexeme, null);

    if (stmt.superclass != null) {
        environment = new Environment(environment);
        environment.define("super", superclass);
    }

    Map<String, LoxFunction> methods = new HashMap<>();
    for (Stmt.Function method : stmt.methods) {
        LoxFunction function = new LoxFunction(method, environment, method.name.lexeme.equals("init"));
        methods.put(method.name.lexeme, function);
    }

    LoxClass klass = new LoxClass(stmt.name.lexeme, (LoxClass)superclass, methods);

    if (superclass != null) {
        environment = environment.enclosing;
    }

    environment.assign(stmt.name, klass);
    return null;
}

@Override
public Void visitCaseStmt(Stmt.Case stmt) {
    executeCaseStatements(stmt.statements);
    return null;
}

@Override
public Void visitFunctionStmt(Stmt.Function stmt) {
    LoxFunction function = new LoxFunction(stmt, environment, false);
    environment.define(stmt.name.lexeme, function);
    return null;
}

@Override
public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) {
        value = evaluate(stmt.value);
    }
    throw new Return(stmt.keyword, stmt.value);
}

private void checkNumberOperand(Token operator, Object operand) {
  if (operand instanceof Double) return;
  throw new RuntimeError(operator, "Operand must be a number.");
}

private void checkNumberOperands(Token operator, Object left, Object right) {
  if (left instanceof Double && right instanceof Double) return;
  throw new RuntimeError(operator, "Operands must be numbers.");
}

private static class BreakException extends RuntimeException {
  BreakException() {
      super(null, null, false, false);
  }
}

@Override
public Void visitBreakStmt(Stmt.Break stmt) {
    throw new BreakException();
}

@Override
public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);

    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
        arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable)) {
        throw new RuntimeError(expr.paren, "Can only call functions and classes.");
    }

    LoxCallable function = (LoxCallable)callee;

    if (arguments.size() != function.arity()) {
        throw new RuntimeError(expr.paren, "Expected " + function.arity() + " arguments but got " + arguments.size() + ".");
    }

    return function.call(this, arguments);
}

@Override
public Void visitDefaultStmt(Stmt.Default stmt) {
    executeCaseStatements(stmt.statements);
    return null;
}

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    Integer distance = locals.get(expr);
    if (distance != null) {
      environment.assignAt(distance, expr.name, value);
    } else {
      globals.assign(expr.name, value);
    }
    return value;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);
    switch (expr.operator.type) {
      case BANG_EQUAL: return !isEqual(left, right);
      case EQUAL_EQUAL: return isEqual(left, right);
      case GREATER: return (double)left > (double)right;
      case GREATER_EQUAL: return (double)left >= (double)right;
      case LESS: return (double)left < (double)right;
      case LESS_EQUAL: return (double)left <= (double)right;
      case MINUS: return (double)left - (double)right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }
        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        throw new RuntimeError(expr.operator, "operands must be of same type.");
      case SLASH: return (double)left / (double)right;
      case STAR: return (double)left * (double)right;
    }
    return null;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
      Object left = evaluate(expr.left);
  
      // Short-circuit evaluation for logical OR and AND
      if (expr.operator.type == TokenType.OR) {
          if (isTruthy(left)) return left; // If left is true, return it (OR short-circuits)
      } else {
          if (!isTruthy(left)) return left; // If left is false, return it (AND short-circuits)
      }
  
      // If short-circuiting didn't happen, evaluate the right operand
      return evaluate(expr.right);
  }

  @Override
public Object visitGetExpr(Expr.Get expr) {
    Object object = evaluate(expr.object);

    if (object instanceof LoxInstance) {
        return ((LoxInstance) object).get(expr.name);
    }

    throw new RuntimeError(expr.name, "Only instances have properties.");
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
  public Object visitVariableExpr(Expr.Variable expr) {
    return lookUpVariable(expr.name, expr);
  }

  @Override
  public Object visitThisExpr(Expr.This expr) {
      return lookUpVariable(expr.keyword, expr);
  }
  

  private Object lookUpVariable(Token name, Expr expr) {
    Integer distance = locals.get(expr);
    if (distance != null) {
      return environment.getAt(distance, name.lexeme);
    } else {
      return globals.get(name);
    }
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    return a.equals(b);
  }
  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
      Object right = evaluate(expr.right);
      switch (expr.operator.type) {
          case BANG:
              return !isTruthy(right);
          case MINUS:
              return -(double) right;
      }
      return null;
  }
 
  class Return extends RuntimeException {
    final Token keyword;
    final Expr value;

    Return(Token keyword, Expr value) {
        this.keyword = keyword;
        this.value = value;
    }
}

  private String stringify(Object object) {
    if (object == null) return "nil";
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


