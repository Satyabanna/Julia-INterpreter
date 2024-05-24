package com.interpreter;
import java.util.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
public class Julia {
    static boolean hadError = false;
    static boolean hadRuntimeError = false;
    private static final Interpreter interpreter = new Interpreter();

    public static void main(String[] args) throws IOException {
        if (args.length > 1) {
            System.out.println("Usage: julia[script]");
            System.exit(64);
        } else if (args.length == 1) {
            runFile(args[0]);
        } else {
            runPrompt();
        }
    }

    private static void runFile(String path) throws IOException {
        if (hadError) System.exit(65);
        if (hadRuntimeError) System.exit(70);
        byte[] bytes = Files.readAllBytes(Paths.get(path));
        run(new String(bytes, Charset.defaultCharset()));

    }

    private static void runPrompt() throws IOException {
        // InputStreamReader input = new InputStreamReader(System.in);
        FileReader fr = new FileReader(new File(
            "demo\\src\\main\\java\\com\\interpreter\\juliaTest.txt"));
        BufferedReader reader = new BufferedReader(fr);
        String code = "";
        for (; ; ) {
            String line = reader.readLine();
            code += " \n"+line;
            if (line == null) break;
            hadError = false;
        }
        System.out.print("julia> ");
        run(code);
        // reader.close();
    }

    private static void run(String source) {
        Scanner scanner = new Scanner(source);

        List<Token> tokens = scanner.scanTokens();
        // For now, just print the tokens.
        for (Token token : tokens) {
            System.out.println(token);
        }
        Parser parser = new Parser(tokens);
//        Expr expression = parser.parse();
        List<Stmt> statements = parser.parse();
        // Stop if there was a syntax error.
        if (hadError) return;
//        System.out.println(new AstPrinter().print(expression));
        interpreter.interpret(statements);
    }

    private static void report(int line, String where,
                               String message) {
        System.err.println("[line " + line + "] Error" + where + ": " + message);
        hadError = true;
    }

    static void error(int line, String message) {
        report(line, "", message);
    }

    static void error(Token token, String message) {
        if (token.type == TokenType.EOF) {
            report(token.line, " at end", message);
        } else {
            report(token.line, " at '" + token.lexeme + "'", message);
        }


    }
    static void runtimeError(RuntimeError error) {
        System.err.println(error.getMessage() +
                "\n[line " + error.token.line + "]");
        hadRuntimeError = true;
    }
}