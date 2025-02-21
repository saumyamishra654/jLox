package com.craftinginterpreters.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lox.Lox;

import static com.craftinginterpreters.lox.TokenType.*; //static import to prevent TokenType from having to be written everywhere

class Scanner {
  private static final Map<String, TokenType> keywords;

  static {
    //map keywords to TokenType
    keywords = new HashMap<>();
    keywords.put("and", AND);
    keywords.put("class",CLASS);
    keywords.put("else", ELSE);
    keywords.put("false",FALSE);
    keywords.put("for", FOR);
    keywords.put("fun", FUN);
    keywords.put("if", IF);
    keywords.put("nil",NIL);
    keywords.put("or", OR);
    keywords.put("print",PRINT);
    keywords.put("return", RETURN);
    keywords.put("super",SUPER);
    keywords.put("this", THIS);
    keywords.put("true", TRUE);
    keywords.put("var", VAR);
    keywords.put("while", WHILE);
    //keywords for case-switch
    keywords.put("case", CASE);
    keywords.put("switch", SWITCH);
    keywords.put("default", DEFAULT);
    keywords.put("break", BREAK);


  }
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0; //offsets to first character in the lexeme being scanned
  private int current = 0; //character counter
  private int line = 1; // line that current is in

  Scanner(String source) {
    this.source = source;
  }
  List<Token> scanTokens() { //list of tokens
    while (!isAtEnd()) { 
      // at the beginning of the next lexeme
      start = current;
      scanToken();
    }

    tokens.add(new Token(EOF, "", null, line));//add end of file when you have no more normal tokens to add
    return tokens;
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      //scanning for single character lexemes
      case '(': addToken(LEFT_PAREN); break;
      case ')': addToken(RIGHT_PAREN); break;
      case '{': addToken(LEFT_BRACE); break;
      case '}': addToken(RIGHT_BRACE); break;
      case ',': addToken(COMMA); break;
      case '.': addToken(DOT); break;
      case '-': addToken(MINUS); break;
      case '+': addToken(PLUS); break;
      case ';': addToken(SEMICOLON); break;
      case '*': addToken(STAR); break; // [slash]
      //cases where second character needs to be looked for: operators!
      case '!':
        addToken(match('=') ? BANG_EQUAL : BANG);
        break;
      case '=':
        addToken(match('=') ? EQUAL_EQUAL : EQUAL);
        break;
      case '<':
        addToken(match('=') ? LESS_EQUAL : LESS);
        break;
      case '>':
        addToken(match('=') ? GREATER_EQUAL : GREATER);
        break;

      case '/':
        if (match('/')) {
          // A comment goes until the end of the line.
          while (peek() != '\n' && !isAtEnd()) advance();
        } else {
          addToken(SLASH);
        }
        break;

//ignore all kinds of meaningless whitespace:
      case ' ':
      case '\r':
      case '\t': 
        break;

      case '\n':
        line++;
        break;


      case '"': string(); break; //string literals always in ""


      default:

        if (isDigit(c)) { //number checker
          number();
        } else if (isAlpha(c)) { //assume everything starting with a letter is identifier
          identifier();
        } else {
          Lox.error(line, "unexpected character"); //error handling, but keep going in case other errors are present
        } //since hadError got set, none of the rest of the code gets run anyway
        break;
    }
  }

  private void identifier() { //check if its a keyword, else make identifier
    while (isAlphaNumeric(peek())) advance();

    String text = source.substring(start, current); //check if it matches a keyword
    TokenType type = keywords.get(text);
    if (type == null) type = IDENTIFIER;
    addToken(type);
  }

  private void number() {
    while (isDigit(peek())) advance();

//looking for fraction
    if (peek() == '.' && isDigit(peekNext())) {
      // get rid of decimal point
      advance();

      while (isDigit(peek())) advance();
    }

    addToken(NUMBER,
        Double.parseDouble(source.substring(start, current)));
  }

  private void string() { ;//string checker, should be om quotes
    while (peek() != '"' && !isAtEnd()) {
      if (peek() == '\n') line++;
      advance();
    }

    if (isAtEnd()) {
      Lox.error(line, "Unterminated string.");
      return;
    }

    // The closing ".
    advance();

    // Trim the surrounding quotes.
    String value = source.substring(start + 1, current - 1);
    addToken(STRING, value);
  }

  private boolean match(char expected) { //conditional advance: consume character if it being looked for
    if (isAtEnd()) return false;
    if (source.charAt(current) != expected) return false;

    current++;
    return true;
  }

  private char peek() { //lookahead advance
    if (isAtEnd()) return '\0';
    return source.charAt(current);
  }

  private char peekNext() { //peek post decimal, only want to consume if there is a digit afterwards
    if (current + 1 >= source.length()) return '\0';
    return source.charAt(current + 1);
  }
  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
           (c >= 'A' && c <= 'Z') ||
            c == '_';
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private boolean isDigit(char c) { // checking for digitness
    return c >= '0' && c <= '9';
  }
  private boolean isAtEnd() { //check whether you have consumed all characters
    return current >= source.length();
  }

  private char advance() {//consumes next character in the source file, for input
    return source.charAt(current++);
  }

  private void addToken(TokenType type) { //for output, grabs text of current lexeme and creates new token
    addToken(type, null);
  }

 //will override previous one when two arguments are passed
 //instead of one, for instances where two characters need to be read 
  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }
}
