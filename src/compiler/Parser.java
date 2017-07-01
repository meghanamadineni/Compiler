package compiler;

import static compiler.Scanner.Kind.*;

import java.util.ArrayList;

import compiler.AST.ASTNode;
import compiler.AST.AssignmentStatement;
import compiler.AST.BinaryChain;
import compiler.AST.BinaryExpression;
import compiler.AST.Block;
import compiler.AST.BooleanLitExpression;
import compiler.AST.Chain;
import compiler.AST.ChainElem;
import compiler.AST.ConstantExpression;
import compiler.AST.Dec;
import compiler.AST.Expression;
import compiler.AST.FilterOpChain;
import compiler.AST.FrameOpChain;
import compiler.AST.IdentChain;
import compiler.AST.IdentExpression;
import compiler.AST.IdentLValue;
import compiler.AST.IfStatement;
import compiler.AST.ImageOpChain;
import compiler.AST.IntLitExpression;
import compiler.AST.ParamDec;
import compiler.AST.Program;
import compiler.AST.SleepStatement;
import compiler.AST.Statement;
import compiler.AST.Tuple;
import compiler.AST.WhileStatement;
import compiler.Scanner.IllegalNumberException;
import compiler.Scanner.Kind;
import compiler.Scanner.Token;
import compiler.AST.*;


public class Parser {

    /**
     * Exception to be thrown if a syntax error is detected in the input.
     * You will want to provide a useful error message.
     *
     */
    @SuppressWarnings("serial")
    public static class SyntaxException extends Exception {
        public SyntaxException(String message) {
            super(message);
        }
    }

    /**
     * Useful during development to ensure unimplemented routines are
     * not accidentally called during development.  Delete it when 
     * the Parser is finished.
     *
     */
    @SuppressWarnings("serial")
    public static class UnimplementedFeatureException extends RuntimeException {
        public UnimplementedFeatureException() {
            super();
        }
    }

    Scanner scanner;
    Token t;

    Parser(Scanner scanner) {
        this.scanner = scanner;
        t = scanner.nextToken();
    }

    /**
     * parse the input using tokens from the scanner.
     * Check for EOF (i.e. no trailing junk) when finished
     * 
     * @throws SyntaxException
     */
    ASTNode parse() throws SyntaxException {
        ASTNode program = program();
        matchEOF();
        return program;
    }

    Expression expression() throws SyntaxException {
        Expression expr0 = null, expr1 = null;
        Token firstToken = t, operator = null;
        try {
            expr0 = term();
            while (t.isKind(LT) || t.isKind(GT) || t.isKind(LE) || t.isKind(GE) || t.isKind(Kind.EQUAL) || t.isKind(NOTEQUAL)) {
                operator = t;
                consume();
                expr1 = term();
                expr0 = new BinaryExpression(firstToken, expr0, operator, expr1);
            }
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in expression.");
        }
        return expr0;
    }



    Expression term() throws SyntaxException {
        Expression term0 = null, term1 = null;
        Token firstToken = t, operator = null;
        try {
            term0 = elem();
            while (t.isKind(PLUS) || t.isKind(MINUS) || t.isKind(OR)) {
                operator = t;
                consume();
                term1 = elem();
                term0 = new BinaryExpression(firstToken, term0, operator, term1);
            }
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in term.");
        }
        return term0;
    }

    Expression elem() throws SyntaxException {
        Expression elem0 = null, elem1 = null;
        Token firstToken = t, operator = null;
        try {
            elem0 = factor();
            while (t.isKind(TIMES) || t.isKind(DIV) || t.isKind(AND) || t.isKind(MOD)) {
                operator = t;
                consume();
                elem1 = factor();
                elem0 = new BinaryExpression(firstToken, elem0, operator, elem1);
            }
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in elem.");
        }
        return elem0;
    }

    Expression factor() throws SyntaxException, NumberFormatException, IllegalNumberException {
        Expression factor = null;
        Kind kind = t.kind;
        switch (kind) {
            case IDENT:
                factor = new IdentExpression(t);
                consume();
                break;
            case INT_LIT:
                factor = new IntLitExpression(t);
                consume();
                break;
            case KW_TRUE:
            case KW_FALSE:
                factor = new BooleanLitExpression(t);
                consume();
                break;
            case KW_SCREENWIDTH:
            case KW_SCREENHEIGHT:
                factor = new ConstantExpression(t);
                consume();
                break;
            case LPAREN:
                consume();
                factor = expression();
                match(RPAREN);
                break;
            default:
                throw new SyntaxException("Illegal token found in factor.");
        }
        return factor;
    }

    Block block() throws SyntaxException {
        Token firstToken = t;
        ArrayList < Dec > declarations = new ArrayList < Dec > ();
        ArrayList < Statement > statements = new ArrayList < Statement > ();
        try {
            match(LBRACE);
            while (!t.isKind(RBRACE)) {
                if (t.isKind(KW_INTEGER) | t.isKind(KW_BOOLEAN) || t.isKind(KW_IMAGE) || t.isKind(KW_FRAME)) {
                    declarations.add(dec());
                } else {
                    statements.add(statement());
                }
            }
            match(RBRACE);
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in block.");
        }
        return new Block(firstToken, declarations, statements);
    }

    Program program() throws SyntaxException {
        Token firstToken = t;
        ArrayList < ParamDec > paramDeclarations = new ArrayList < ParamDec > ();
        Block block = null;
        try {
            match(IDENT);
            if (t.isKind(KW_URL) || t.isKind(KW_FILE) || t.isKind(KW_INTEGER) || t.isKind(KW_BOOLEAN)) {
                paramDeclarations.add(paramDec());
            }
            while (t.isKind(COMMA)) {
                consume();
                paramDeclarations.add(paramDec());
            }
            block = block();
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in program.");
        }
        return new Program(firstToken, paramDeclarations, block);
    }

    ParamDec paramDec() throws SyntaxException {
        Token firstToken = t;
        Token ident = null;
        try {
            if (t.isKind(KW_URL) || t.isKind(KW_FILE) || t.isKind(KW_INTEGER) || t.isKind(KW_BOOLEAN)) {
                consume();
            }
            ident = t;
            match(Kind.IDENT);
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in paramDec.");
        }
        return new ParamDec(firstToken, ident);
    }

    Dec dec() throws SyntaxException {
        Token firstToken = t;
        Token ident = null;
        try {
            if (t.isKind(KW_INTEGER) || t.isKind(KW_BOOLEAN) | t.isKind(KW_IMAGE) | t.isKind(KW_FRAME)) {
                consume();
                ident = t;
            }
            match(Kind.IDENT);
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in dec.");
        }
        return new Dec(firstToken, ident);
    }

    Statement statement() throws SyntaxException {
        Statement statement = null;
        switch (t.kind) {
            case OP_SLEEP:
                statement = opSleep();
                break;
            case KW_WHILE:
                statement = whileBlock();
                break;
            case KW_IF:
                statement = ifBlock();
                break;
            case IDENT:
                if (scanner.peek().isKind(ASSIGN)) {
                    statement = assign();
                } else {
                    statement = chain();
                }
                match(SEMI);
                break;
            case OP_BLUR:
            case OP_GRAY:
            case OP_CONVOLVE:
            case KW_SHOW:
            case KW_HIDE:
            case KW_MOVE:
            case KW_XLOC:
            case KW_YLOC:
            case OP_WIDTH:
            case OP_HEIGHT:
            case KW_SCALE:
                statement = chain();
                match(SEMI);
                break;
            default:
                throw new SyntaxException("Illegal token found in statement.");
        }
        return statement;
    }

    public WhileStatement whileBlock() throws SyntaxException {
        Expression whileExpression = null;
        Block block = null;
        Token firstToken = t;
        try {
            consume();
            match(LPAREN);
            whileExpression = expression();
            match(RPAREN);
            block = block();
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in if or while block.");
        }
        return new WhileStatement(firstToken, whileExpression, block);
    }

    public IfStatement ifBlock() throws SyntaxException {
        Expression ifExpression = null;
        Block block = null;
        Token firstToken = t;
        try {
            consume();
            match(LPAREN);
            ifExpression = expression();
            match(RPAREN);
            block = block();
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in if or while block.");
        }
        return new IfStatement(firstToken, ifExpression, block);
    }

    public SleepStatement opSleep() throws SyntaxException {
        Token firstToken = t;
        Expression sleepExpression = null;
        try {
            consume();
            sleepExpression = expression();
            match(SEMI);
        } catch (Exception e) {
            throw new SyntaxException("Illegal sleep operator.");
        }
        return new SleepStatement(firstToken, sleepExpression);
    }

    public AssignmentStatement assign() throws SyntaxException {
        Expression assignExpression = null;
        Token firstToken = t;
        IdentLValue var = new IdentLValue(firstToken);
        try {
            consume();
            if (t.isKind(ASSIGN)) {
                match(ASSIGN);
                assignExpression = expression();
            } else {
                throw new SyntaxException("Illegal token found in assign.");
            }
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in assign.");
        }
        return new AssignmentStatement(firstToken, var, assignExpression);
    }

    Chain chain() throws SyntaxException {
        Chain chain = null;
        ChainElem chainElem = null;
        Token firstToken = t;
        Token operator = null;
        try {
            chain = chainElem();
            operator = t;
            if (t.isKind(ARROW)) {
                match(Kind.ARROW);
                chainElem = chainElem();
            } else {
                match(Kind.BARARROW);
                chainElem = chainElem();
            }
            chain = new BinaryChain(firstToken, chain, operator, chainElem);
            while (t.isKind(ARROW) || t.isKind(BARARROW)) {
            	operator =t;
                consume();
                chainElem = chainElem();
                chain = new BinaryChain(firstToken, chain, operator, chainElem);
            }

        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in chain.");
        }
        return chain;
    }

    ChainElem chainElem() throws SyntaxException {
    	ArrayList<Expression> exprList = new ArrayList<Expression>();
        Token firstToken = t;
        Tuple tuple = new Tuple(firstToken, exprList);
        try {
            if (t.isKind(IDENT)) {
                consume();
                IdentChain identChain = new IdentChain(firstToken);
                return identChain;
            } else if (t.isKind(OP_BLUR) || t.isKind(OP_GRAY) || t.isKind(OP_CONVOLVE)) {
                consume();
                if (t.isKind(LPAREN)) {
                    tuple = arg();
                }
                FilterOpChain filterOpChain = new FilterOpChain(firstToken, tuple);
                return filterOpChain;
            } else if (t.isKind(KW_SHOW) || t.isKind(KW_HIDE) || t.isKind(KW_MOVE) || t.isKind(KW_XLOC) || t.isKind(KW_YLOC)) {
                consume();
                if (t.isKind(LPAREN)) {
                    tuple = arg();
                }
                FrameOpChain frameOpChain = new FrameOpChain(firstToken, tuple);
                return frameOpChain;
            } else if (t.isKind(OP_WIDTH) || t.isKind(OP_HEIGHT) || t.isKind(KW_SCALE)) {
                consume();
                if (t.isKind(LPAREN)) {
                    tuple = arg();
                }
                ImageOpChain imageOpChain = new ImageOpChain(firstToken, tuple);
                return imageOpChain;
            } else {
                throw new SyntaxException("Illegal token found in chainElem.");
            }
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in chainElem.");
        }
    }

    Tuple arg() throws SyntaxException {
        Token firstToken = t;
        ArrayList < Expression > expressions = new ArrayList < Expression > ();
        try {
            if (t.isKind(LPAREN)) {
                consume();
                expressions.add(expression());
                while (t.isKind(COMMA)) {
                    consume();
                    expressions.add(expression());
                }
                match(RPAREN);
            }
        } catch (Exception e) {
            throw new SyntaxException("Illegal token found in arg.");
        }
        return new Tuple(firstToken, expressions);
    }

    void arrowOP() throws SyntaxException {
        if (t.isKind(ARROW) || t.isKind(BARARROW)) {
            consume();
        } else {
            throw new SyntaxException("Illegal arrow operator.");
        }
    }

    void filterOP() throws SyntaxException {
        if (t.isKind(OP_BLUR) || t.isKind(OP_GRAY) || t.isKind(OP_CONVOLVE)) {
            consume();
        } else {
            throw new SyntaxException("Illegal filter operator.");
        }
    }

    void relOp() throws SyntaxException {
        if (t.isKind(LT) || t.isKind(GT) || t.isKind(LE) || t.isKind(GE) || t.isKind(Kind.EQUAL) || t.isKind(NOTEQUAL)) {
            consume();
        } else {
            throw new SyntaxException("Illegal relational operator.");
        }
    }

    void frameOP() throws SyntaxException {
        if (t.isKind(KW_SHOW) || t.isKind(KW_HIDE) || t.isKind(KW_MOVE) || t.isKind(KW_XLOC) || t.isKind(KW_YLOC)) {
            consume();
        } else {
            throw new SyntaxException("Illegal frame operator.");
        }
    }

    void imageOp() throws SyntaxException {
        if (t.isKind(OP_WIDTH) || t.isKind(OP_HEIGHT) || t.isKind(KW_SCALE)) {
            consume();
        } else {
            throw new SyntaxException("Illegal image operator.");
        }
    }

    void weakOp() throws SyntaxException {
        if (t.isKind(PLUS) || t.isKind(MINUS) || t.isKind(OR)) {
            consume();
        } else {
            throw new SyntaxException("Illegal weak operator.");
        }
    }

    void strongOp() throws SyntaxException {
        if (t.isKind(TIMES) || t.isKind(DIV) || t.isKind(AND) || t.isKind(MOD)) {
            consume();
        } else {
            throw new SyntaxException("Illegal strong operator.");
        }
    }

    /**
     * Checks whether the current token is the EOF token. If not, a
     * SyntaxException is thrown.
     * 
     * @return
     * @throws SyntaxException
     */
    private Token matchEOF() throws SyntaxException {
        if (t.isKind(EOF)) {
            return t;
        }
        throw new SyntaxException("expected EOF");
    }

    /**
     * Checks if the current token has the given kind. If so, the current token
     * is consumed and returned. If not, a SyntaxException is thrown.
     * 
     * Precondition: kind != EOF
     * 
     * @param kind
     * @return
     * @throws SyntaxException
     */
    private Token match(Kind kind) throws SyntaxException {
        if (t.isKind(kind)) {
            return consume();
        }
        throw new SyntaxException("saw " + t.kind + "expected " + kind);
    }

    /**
     * Checks if the current token has one of the given kinds. If so, the
     * current token is consumed and returned. If not, a SyntaxException is
     * thrown.
     * 
     * * Precondition: for all given kinds, kind != EOF
     * 
     * @param kinds
     *            list of kinds, matches any one
     * @return
     * @throws SyntaxException
     */
    private Token match(Kind...kinds) throws SyntaxException {
        for (Kind temp: kinds) {
            if (temp == t.kind && t.kind != EOF) {
                consume();
                return t;
            }
        }
        throw new SyntaxException("Token check with given kinds.");
    }

    /**
     * Gets the next token and returns the consumed token.
     * 
     * Precondition: t.kind != EOF
     * 
     * @return
     * 
     */
    private Token consume() throws SyntaxException {
        Token tmp = t;
        t = scanner.nextToken();
        return tmp;
    }

}