package lox;

enum TokenType {
  //single tokens
  LEFT_PAREN, RIGHT_PAREN, LEFT_BRACE, RIGHT_BRACE,
  COMMA, DOT, MINUS, PLUS, SEMICOLON, SLASH, STAR,

  //double character tokens
  BANG, BANG_EQUAL,
  EQUAL, EQUAL_EQUAL,
  GREATER, GREATER_EQUAL,
  LESS, LESS_EQUAL,

  //literals
  IDENTIFIER, STRING, NUMBER,

  //keywords
  AND, CLASS, ELSE, FALSE, FUN, FOR, IF, NIL, OR,
  PRINT, RETURN, SUPER, THIS, TRUE, VAR, WHILE,

  //end of file
  EOF,

  //case switch (new addition)
  SWITCH, CASE, DEFAULT, BREAK

}
