package com.sjlox.tool;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

public class GenerateAst {
  public static void main(String[] args) throws IOException {
    // check if the user provided an output directory argument
    if (args.length != 1) {
      System.err.println("Usage: generate_ast <output directory>");
      System.exit(64);
    }
    String outputDir = args[0];

    // define the abstract syntax tree (ast) for expressions
    defineAst(outputDir, "Expr", Arrays.asList(
      "Assign   : Token name, Expr value", // variable assignment
      "Binary   : Expr left, Token operator, Expr right", // binary operations like +, -, *
      "Call     : Expr callee, Token paren, List<Expr> arguments", // function calls
      "Get      : Expr object, Token name", // property access (object.field)
      "Grouping : Expr expression", // grouping with parentheses
      "Literal  : Object value", // literal values like numbers, strings, booleans
      "Logical  : Expr left, Token operator, Expr right", // logical operations (and, or)
      "Set      : Expr object, Token name, Expr value", // property assignment (object.field = value)
      "Super    : Token keyword, Token method", // refers to superclass methods
      "This     : Token keyword", // refers to the current instance
      "Unary    : Token operator, Expr right", // unary operations like -5 or !true
      "Variable : Token name" // variable references
    ));

    // define the abstract syntax tree (ast) for statements
    defineAst(outputDir, "Stmt", Arrays.asList(
      "Block      : List<Stmt> statements", // block of statements (curly braces)
      "Class      : Token name, Expr.Variable superclass," +
                  " List<Stmt.Function> methods", // class declaration
      "Expression : Expr expression", // a statement that evaluates an expression
      "Function   : Token name, List<Token> params," +
                  " List<Stmt> body", // function declaration
      "If         : Expr condition, Stmt thenBranch," +
                  " Stmt elseBranch", // if-else statement
      "Print      : Expr expression", // print statement
      "Return     : Token keyword, Expr value", // return statement inside functions
      "Var        : Token name, Expr initializer", // variable declaration
      "While      : Expr condition, Stmt body" // while loop
    ));
  }

  // generates a java class for the ast with subclasses for different node types
  private static void defineAst(
      String outputDir, String baseName, List<String> types)
      throws IOException {
    // create the output file for the ast class
    String path = outputDir + "/" + baseName + ".java";
    PrintWriter writer = new PrintWriter(path, "UTF-8");

    // write the package declaration
    writer.println("//> Appendix II " + baseName.toLowerCase());
    writer.println("package com.craftinginterpreters.lox;");
    writer.println();
    writer.println("import java.util.List;");
    writer.println();
    writer.println("abstract class " + baseName + " {");

    // define the visitor interface for the ast nodes
    defineVisitor(writer, baseName, types);

    writer.println();
    writer.println("  // Nested " + baseName + " classes here...");

    // generate a subclass for each ast node type
    for (String type : types) {
      String className = type.split(":")[0].trim();
      String fields = type.split(":")[1].trim();
      defineType(writer, baseName, className, fields);
    }

    writer.println();
    writer.println("  abstract <R> R accept(Visitor<R> visitor);");

    writer.println("}");
    writer.println("//< Appendix II " + baseName.toLowerCase());
    writer.close();
  }

  // defines a visitor interface with visit methods for each ast node type
  private static void defineVisitor(
      PrintWriter writer, String baseName, List<String> types) {
    writer.println("  interface Visitor<R> {");

    for (String type : types) {
      String typeName = type.split(":")[0].trim();
      writer.println("    R visit" + typeName + baseName + "(" +
          typeName + " " + baseName.toLowerCase() + ");");
    }

    writer.println("  }");
  }

  // generates a nested class for each ast node type
  private static void defineType(
      PrintWriter writer, String baseName,
      String className, String fieldList) {
    writer.println("//> " +
        baseName.toLowerCase() + "-" + className.toLowerCase());
    writer.println("  static class " + className + " extends " +
        baseName + " {");

    // format the field list for readability
    if (fieldList.length() > 64) {
      fieldList = fieldList.replace(", ", ",\n          ");
    }

    // constructor for the ast node class
    writer.println("    " + className + "(" + fieldList + ") {");

    fieldList = fieldList.replace(",\n          ", ", ");
    String[] fields = fieldList.split(", ");
    for (String field : fields) {
      String name = field.split(" ")[1];
      writer.println("      this." + name + " = " + name + ";");
    }

    writer.println("    }");

    writer.println();
    writer.println("    @Override");
    writer.println("    <R> R accept(Visitor<R> visitor) {");
    writer.println("      return visitor.visit" +
        className + baseName + "(this);");
    writer.println("    }");

    writer.println();
    for (String field : fields) {
      writer.println("    final " + field + ";");
    }

    writer.println("  }");
    writer.println("//< " +
        baseName.toLowerCase() + "-" + className.toLowerCase());
  }
}
