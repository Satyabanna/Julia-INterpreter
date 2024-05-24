package com.interpreter;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import static com.interpreter.TokenType.*;
public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private final List<Token> tokens;
    private int current = 0;

    Parser(List<Token> tokens) {
        this.tokens = tokens;
    }
    private Stmt statement() {
        if (match(IF)) return ifStatement();
        if (match(WHILE)) return whileStatement();
        if (match(PRINT)) return printStatement();
        if (match(PRINTLN)) return printlnStatement();
        //if (match(FOR)) return forStatement();
        return expressionStatement();
    }
    private Stmt printStatement() {
        consume(LEFT_PAREN,"expect '(' this after print");

        Expr value = expression();
         consume(RIGHT_PAREN,"expect ')' this after print");
        return new Stmt.Print(value);
    }
    private Stmt printlnStatement() {
        consume(LEFT_PAREN,"expect '(' this after print");

        Expr value = expression();
         consume(RIGHT_PAREN,"expect ')' this after print");
        return new Stmt.Println(value);
    }
    private List<Stmt> block() {
        List<Stmt> statements = new ArrayList<>();
        while (!check(BEGIN) && !isAtEnd()) {
            statements.add(declaration());
        }
        consume(BEGIN, "Expect 'end' after block.");
        return statements;
    }
    private Stmt varDeclaration() {
        Token name = previous();


        Expr initializer = null;
        if (match(EQUAL)) {
            initializer = expression();
            return new Stmt.Var(name, initializer);
        }

        return null;
    }
    private Stmt varDeclaration(Expr initializer) {
        Token name = previous();


        if (match(EQUAL)) {
            initializer = expression();
            return new Stmt.Var(name, initializer);
        }

        return null;
    }
    private Stmt expressionStatement() {
        Expr expr = expression();

        return new Stmt.Expression(expr);
    }

    List<Stmt> parse() {
        List<Stmt> statements = new ArrayList<>();
        while (!isAtEnd()) {
            statements.add(declaration());
        }
        return statements;
    }

    private Expr expression() {

        return assignment();
    }
    private Expr assignment() {
        Expr expr = or();

        if (match(EQUAL)) {
            Token equals = previous();
            Expr value = assignment();
            if (expr instanceof Expr.Variable) {
                Token name = ((Expr.Variable)expr).name;
                return new Expr.Assign(name, value);
            }
            error(equals, "Invalid assignment target.");
        }
        return expr;
    }
    private Stmt declaration() {
        try {
            if(match(IDENTIFIER)){

                return varDeclaration();}
            return statement();
        } catch (ParseError error) {
            synchronize();
            return null;
        }
    }

    private Expr equality() {
        Expr expr = comparison();
        while (match(BANG_EQUAL, EQUAL_EQUAL)) {
            Token operator = previous();
            Expr right = comparison();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    public  boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return peek().type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() {
        return peek().type == EOF;
    }

    public Token peek() {
        return tokens.get(current);
    }

    private Token previous() {
        return tokens.get(current - 1);
    }

    private Expr comparison() {
        Expr expr = term();
        while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token operator = previous();
            Expr right = term();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr term() {
        Expr expr = factor();
        while (match(MINUS, PLUS)) {
            Token operator = previous();
            Expr right = factor();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr factor() {
        Expr expr = unary();
        while (match(SLASH, STAR)) {
            Token operator = previous();
            Expr right = unary();
            expr = new Expr.Binary(expr, operator, right);
        }
        return expr;
    }

    private Expr unary() {
        if (match(BANG, MINUS)) {
            Token operator = previous();
            Expr right = unary();
            return new Expr.Unary(operator, right);
        }
        return primary();
    }

    private Expr primary() {
        if (match(FALSE)) return new Expr.Literal(false);
        if (match(TRUE)) return new Expr.Literal(true);
        if (match(NIL)) return new Expr.Literal(null);
        if (match(NUMBER, STRING)) {
            return new Expr.Literal(previous().literal);
        }
        if (match(IDENTIFIER)) {
            return new Expr.Variable(previous());
        }
        if (match(LEFT_PAREN)) {
            Expr expr = expression();
            consume(RIGHT_PAREN, "Expect ')' after expression.");
            return new Expr.Grouping(expr);
        }
        throw error(peek(), "Expect expression.");

    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private ParseError error(Token token, String message) {
        Julia.error(token, message);
        return new ParseError();
    }


    private void synchronize() {
        advance();
        while (!isAtEnd()) {

            switch (peek().type) {
                case CLASS:
                    case FOR:
                case IF:
                case WHILE:
                case PRINT:
                case RETURN:
                    return;
            }
            advance();
        }
    }
    private Stmt ifStatement() {
        if(match(LEFT_PAREN)){
            return null;
        }else{

        Expr condition = expression();
        List<Stmt> thenBranch = new ArrayList<Stmt>();
        List<Stmt> elseBranch = new ArrayList<Stmt>();
        while(!match(END)){
            if(match(ELSEIF)){
                return ifStatement();
            }
            if(match(ELSE)){
                while(!match(END)){
                    elseBranch.add(statement());
                }
                break;
            }
            thenBranch.add(statement());
        }
        return new Stmt.If(condition, thenBranch, elseBranch);
    }
    }

    private Expr or() {
        Expr expr = and();
        while (match(OR)) {
            Token operator = previous();
            Expr right = and();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Expr and() {
        Expr expr = equality();
        while (match(AND)) {
            Token operator = previous();
            Expr right = equality();
            expr = new Expr.Logical(expr, operator, right);
        }
        return expr;
    }
    private Stmt whileStatement() {
        Expr condition = expression();
        List<Stmt> body = new ArrayList<Stmt>();
        while(!match(END)){
            body.add(statement());
        }
        return new Stmt.While(condition,body);
    }
    

}