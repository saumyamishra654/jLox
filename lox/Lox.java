package com.craftinginterpreters.lox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Lox {
  public static void main(String[] args) throws IOException {
    if (args.length > 1) {
      System.out.println("Usage: jlox [script]");
      System.exit(64); 
    } else if (args.length == 1) {
      runFile(args[0]);
    } else {
      runPrompt();
    }
  }

  // Access the LOX file via the filepath
  private static void runFile(String path) throws IOException {
    byte[] bytes = Files.readAllBytes(Paths.get(path)); // convert file to bytes
    run(new String(bytes, Charset.defaultCharset())); // run program
  }

// run the program one line at a time
  private static void runPrompt() throws IOException {
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader reader = new BufferedReader(input);

    for (;;) { 
      System.out.print("> ");
      String line = reader.readLine();
      if (line == null) break;
      run(line); // run each line of code one line at a time
      hadError = false;

    }
  }

  private static void run(String source) { //defining run
    Scanner scanner = new Scanner(source);
    List<Token> tokens = scanner.scanTokens();

    // just print the tokens
    for (Token token : tokens) {
      System.out.println(token);
    
    if (hadError) System.exit(65); //error code reporting
          hadError = false;

    }
  }

  static void error(int line, String message) {
    report(line, "", message);
  }

  private static void report(int line, String where, String message) {
    System.err.println(
        "[line " + line + "] Error" + where + ": " + message);
    hadError = true;
  }
  
  static boolean hadError = false;





}