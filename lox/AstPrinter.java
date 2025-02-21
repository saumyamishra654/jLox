package com.sjlox.lox;
import java.util.List;

// astPrinter converts the abstract syntax tree into a string representation.
class AstPrinter implements Expr.Visitor<String>, Stmt.Visitor<String> {
  
  // method to print an expression by accepting this visitor
  String print(Expr expr) {
    return expr.accept(this);
  }

  // method to print a statement by accepting this visitor
  String print(Stmt stmt) {
    return stmt.accept(this);
  }

  @Override
  public String visitBlockStmt(Stmt.Block stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(block ");

    // iterate over all statements in the block
    for (Stmt statement : stmt.statements) {
      builder.append(statement.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitClassStmt(Stmt.Class stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(class " + stmt.name.lexeme);

    // if the class has a superclass, include it in the output
    if (stmt.superclass != null) {
      builder.append(" < " + print(stmt.superclass));
    }

    // add methods inside the class
    for (Stmt.Function method : stmt.methods) {
      builder.append(" " + print(method));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitExpressionStmt(Stmt.Expression stmt) {
    return parenthesize(";", stmt.expression);
  }

  @Override
  public String visitFunctionStmt(Stmt.Function stmt) {
    StringBuilder builder = new StringBuilder();
    builder.append("(fun " + stmt.name.lexeme + "(");

    // add function parameters
    for (Token param : stmt.params) {
      if (param != stmt.params.get(0)) builder.append(" ");
      builder.append(param.lexeme);
    }

    builder.append(") ");

    // add function body
    for (Stmt body : stmt.body) {
      builder.append(body.accept(this));
    }

    builder.append(")");
    return builder.toString();
  }

  @Override
  public String visitIfStmt(Stmt.If stmt) {
    if (stmt.elseBranch == null) {
      return parenthesize2("if", stmt.condition, stmt.thenBranch);
    }
    return parenthesize2("if-else", stmt.condition, stmt.thenBranch, stmt.elseBranch);
  }

  @Override
  public String visitPrintStmt(Stmt.Print stmt) {
    return parenthesize("print", stmt.expression);
  }

  @Override
  public String visitReturnStmt(Stmt.Return stmt) {
    if (stmt.value == null) return "(return)";
    return parenthesize("return", stmt.value);
  }

  @Override
  public String visitVarStmt(Stmt.Var stmt) {
    if (stmt.initializer == null) {
      return parenthesize2("var", stmt.name);
    }
    return parenthesize2("var", stmt.name, "=", stmt.initializer);
  }

  @Override
  public String visitWhileStmt(Stmt.While stmt) {
    return parenthesize2("while", stmt.condition, stmt.body);
  }

  @Override
  public String visitAssignExpr(Expr.Assign expr) {
    return parenthesize2("=", expr.name.lexeme, expr.value);
  }

  @Override
  public String visitBinaryExpr(Expr.Binary expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitCallExpr(Expr.Call expr) {
    return parenthesize2("call", expr.callee, expr.arguments);
  }

  @Override
  public String visitGetExpr(Expr.Get expr) {
    return parenthesize2(".", expr.object, expr.name.lexeme);
  }

  @Override
  public String visitGroupingExpr(Expr.Grouping expr) {
    return parenthesize("group", expr.expression);
  }

  @Override
  public String visitLiteralExpr(Expr.Literal expr) {
    if (expr.value == null) return "nil";
    return expr.value.toString();
  }

  @Override
  public String visitLogicalExpr(Expr.Logical expr) {
    return parenthesize(expr.operator.lexeme, expr.left, expr.right);
  }

  @Override
  public String visitBreakStmt(Stmt.Break stmt) {
      return "break;";
  }

  @Override
  public String visitDefaultStmt(Stmt.Default stmt) {
      return "(default " + stmt.statements + ")";
  }

  @Override
  public String visitCaseStmt(Stmt.Case stmt) {
      StringBuilder builder = new StringBuilder();
      builder.append("(case ").append(stmt.value.accept(this)).append(" ");
      for (Stmt statement : stmt.statements) {
          builder.append(statement.accept(this)).append(" ");
      }
      builder.append(")");
      return builder.toString();
  }
  
  @Override
  public String visitSwitchStmt(Stmt.Switch stmt) {
      StringBuilder builder = new StringBuilder();
      builder.append("(switch ").append(stmt.condition.accept(this)).append(" ");
  
      for (Stmt.Case caseStmt : stmt.cases) {
          builder.append(caseStmt.accept(this)).append(" ");
      }
  
      if (stmt.defaultCase != null) {
          builder.append(stmt.defaultCase.accept(this));
      }
  
      builder.append(")");
      return builder.toString();
  }
  

  @Override
  public String visitSetExpr(Expr.Set expr) {
    return parenthesize2("=", expr.object, expr.name.lexeme, expr.value);
  }

  @Override
  public String visitSuperExpr(Expr.Super expr) {
    return parenthesize2("super", expr.method);
  }

  @Override
  public String visitThisExpr(Expr.This expr) {
    return "this";
  }

  @Override
  public String visitUnaryExpr(Expr.Unary expr) {
    return parenthesize(expr.operator.lexeme, expr.right);
  }

  @Override
  public String visitVariableExpr(Expr.Variable expr) {
    return expr.name.lexeme;
  }

  // formats expressions into lisp-style parenthesized notation
  private String parenthesize(String name, Expr... exprs) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    for (Expr expr : exprs) {
      builder.append(" ").append(expr.accept(this));
    }
    builder.append(")");
    return builder.toString();
  }

  // formats statements and expressions in parenthesized notation
  private String parenthesize2(String name, Object... parts) {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(name);
    transform(builder, parts);
    builder.append(")");
    return builder.toString();
  }

  // processes and appends parts (expressions, statements, tokens, lists) to the string
  private void transform(StringBuilder builder, Object... parts) {
    for (Object part : parts) {
      builder.append(" ");
      if (part instanceof Expr) {
        builder.append(((Expr) part).accept(this));
      } else if (part instanceof Stmt) {
        builder.append(((Stmt) part).accept(this));
      } else if (part instanceof Token) {
        builder.append(((Token) part).lexeme);
      } else if (part instanceof List) {
        transform(builder, ((List<?>) part).toArray());
      } else {
        builder.append(part);
      }
    }
  }
}
