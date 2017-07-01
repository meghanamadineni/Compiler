package compiler;

import java.util.ArrayList;

import compiler.Scanner.Kind;

public class Scanner {

    static int line_pos = 0, line_start = 0;

    /**
     * enum Kind
     */
    public static enum Kind {
        	IDENT(""), INT_LIT(""), KW_INTEGER("integer"), KW_BOOLEAN("boolean"),
            KW_IMAGE("image"), KW_URL("url"), KW_FILE("file"), KW_FRAME("frame"),
            KW_WHILE("while"), KW_IF("if"), KW_TRUE("true"), KW_FALSE("false"),
            SEMI(";"), COMMA(","), LPAREN("("), RPAREN(")"), LBRACE("{"),
            RBRACE("}"), ARROW("->"), BARARROW("|->"), OR("|"), AND("&"),
            EQUAL("=="), NOTEQUAL("!="), LT("<"), GT(">"), LE("<="), GE(">="),
            PLUS("+"), MINUS("-"), TIMES("*"), DIV("/"), MOD("%"), NOT("!"),
            ASSIGN("<-"), OP_BLUR("blur"), OP_GRAY("gray"), OP_CONVOLVE("convolve"),
            KW_SCREENHEIGHT("screenheight"), KW_SCREENWIDTH("screenwidth"),
            OP_WIDTH("width"), OP_HEIGHT("height"), KW_XLOC("xloc"), KW_YLOC("yloc"),
            KW_HIDE("hide"), KW_SHOW("show"), KW_MOVE("move"), OP_SLEEP("sleep"),
            KW_SCALE("scale"), EOF("eof");

        Kind(String text) {
            this.text = text;
        }

        final String text;

        String getText() {
            return text;
        }
    }

    /** 
     * enum State
     */
    public static enum State {
        START,
        AFTER_DIV,
        AFTER_NOT,
        AFTER_EQUAL,
        AFTER_OR,
        AFTER_MINUS,
        AFTER_LESS_THAN,
        AFTER_GREATER_THAN,
        IN_IDENT,
        IN_INT_LIT;
    }

    /**
     * Thrown by Scanner when an illegal character is encountered
     */
    @SuppressWarnings("serial")
    public static class IllegalCharException extends Exception {
        public IllegalCharException(String message) {
            super(message);
        }
    }

    /**
     * Thrown by Scanner when an int literal is not a value that can be represented by an int.
     */
    @SuppressWarnings("serial")
    public static class IllegalNumberException extends Exception {
        public IllegalNumberException(String message) {
            super(message);
        }
    }

    /**
     * Holds the line and position in the line of a token.
     */
    static class LinePos {
        public final int line;
        public final int posInLine;

        public LinePos(int line, int posInLine) {
            super();
            this.line = line;
            this.posInLine = posInLine;
        }

        @Override
        public String toString() {
            return "LinePos [line=" + line + ", posInLine=" + posInLine + "]";
        }
    }

    public class Token {
        public final Kind kind;
        public final int pos;
        public final int length;
        LinePos linepos;

        public String getText() {
            String s = chars.substring(this.pos, this.pos + this.length);
            return s;
        }

        //returns a LinePos object representing the line and column of this Token
        LinePos getLinePos() {
            return this.linepos;
        }

        Token(Kind kind, int pos, int length) {
            this.kind = kind;
            this.pos = pos;
            this.length = length;
            this.linepos = new LinePos(line_pos, pos - line_start);
        }

        /** 
         * Precondition:  kind = Kind.INT_LIT,  the text can be represented with a Java int.
         * Note that the validity of the input should have been checked when the Token was created.
         * So the exception should never be thrown.
         * 
         * @return  int value of this token, which should represent an INT_LIT
         * @throws NumberFormatException
         */
        public int intVal() throws NumberFormatException, IllegalNumberException {
            int num = 0;

            try {
                num = Integer.parseInt(chars.substring(this.pos, this.pos + this.length));
            } catch (NumberFormatException e) {
                throw new IllegalNumberException("Integer Literal out of Java Int range");
            }
            return num;
        }

		public boolean isKind(Kind kind) {
			if(this.kind==kind){
				return true;
			}
			return false;
		}

    }

    Scanner(String chars) {
        this.chars = chars;
        tokens = new ArrayList < Token > ();
    }

    /**
     * Initializes Scanner object by traversing chars and adding tokens to tokens list.
     * 
     * @return this scanner
     * @throws IllegalCharException
     * @throws IllegalNumberException
     */
    public Scanner scan() throws IllegalCharException, IllegalNumberException {
        int pos = 0, startpos = 0;
        line_pos = 0;
        line_start = pos;
        char ch;
        State state = State.START;
        while (pos <= chars.length()) {
            ch = pos < chars.length() ? chars.charAt(pos) : (char) - 1;

            switch (state) {
                case START:
                    {
                        pos = skipWhiteSpace(pos);
                        ch = pos < chars.length() ? chars.charAt(pos) : (char) - 1;

                        switch (ch) {
                            case (char) - 1:
                                {
                                    tokens.add(new Token(Kind.EOF, pos, 0));pos++;
                                    break;
                                }
                            case '+':
                                {
                                    tokens.add(new Token(Kind.PLUS, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '-':
                                {
                                    state = State.AFTER_MINUS;startpos = pos;pos++;
                                    break;
                                }
                            case '/':
                                {
                                    state = State.AFTER_DIV;startpos = pos;pos++;
                                    break;
                                }
                            case '*':
                                {
                                    tokens.add(new Token(Kind.TIMES, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '%':
                                {
                                    tokens.add(new Token(Kind.MOD, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '&':
                                {
                                    tokens.add(new Token(Kind.AND, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '!':
                                {
                                    state = State.AFTER_NOT;startpos = pos;pos++;
                                    break;
                                }
                            case '=':
                                {
                                    state = State.AFTER_EQUAL;startpos = pos;pos++;
                                    break;
                                }
                            case '|':
                                {
                                    state = State.AFTER_OR;startpos = pos;pos++;
                                    break;
                                }
                            case '<':
                                {
                                    state = State.AFTER_LESS_THAN;startpos = pos;pos++;
                                    break;
                                }
                            case '>':
                                {
                                    state = State.AFTER_GREATER_THAN;startpos = pos;pos++;
                                    break;
                                }
                            case ';':
                                {
                                    tokens.add(new Token(Kind.SEMI, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case ',':
                                {
                                    tokens.add(new Token(Kind.COMMA, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '{':
                                {
                                    tokens.add(new Token(Kind.LBRACE, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '}':
                                {
                                    tokens.add(new Token(Kind.RBRACE, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '(':
                                {
                                    tokens.add(new Token(Kind.LPAREN, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case ')':
                                {
                                    tokens.add(new Token(Kind.RPAREN, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            case '0':
                                {
                                    tokens.add(new Token(Kind.INT_LIT, pos, 1));state = State.START;pos++;
                                    break;
                                }
                            default:
                                {
                                    if (Character.isDigit(ch)) {
                                        startpos = pos;
                                        state = State.IN_INT_LIT;
                                        pos++;
                                    } else if (Character.isJavaIdentifierStart(ch)) {
                                        startpos = pos;
                                        state = State.IN_IDENT;
                                        pos++;
                                    } else {
                                        throw new IllegalCharException(
                                            "illegal char " + ch + " at pos " + pos);
                                    }
                                    break;
                                }
                        }
                        break;

                    }
                case AFTER_DIV:
                    {
                        if ((pos != chars.length() && chars.charAt(pos) != '*') || pos == chars.length()) {
                            tokens.add(new Token(Kind.DIV, startpos, 1));
                            state = State.START;
                        } else if (chars.charAt(pos) == '*') {
                            pos++;
                            while (pos < chars.length()) {
                                pos = skipWhiteSpace(pos);
                                if (chars.charAt(pos) != '*') {
                                    pos++;
                                } else if (pos != chars.length() && chars.charAt(pos) == '*') {
                                    if ((pos + 1) != chars.length() && chars.charAt(pos + 1) == '/') {
                                        pos = pos + 2;
                                        state = State.START;
                                        break;
                                    } else {
                                        pos++;
                                    }
                                }
                            }
                            if (pos == chars.length())
                                state = State.START;
                        }
                        break;
                    }
                case AFTER_NOT:
                    {
                        if (pos != chars.length() && chars.charAt(pos) == '=') {
                            tokens.add(new Token(Kind.NOTEQUAL, startpos, 2));
                            pos++;
                        } else {
                            tokens.add(new Token(Kind.NOT, startpos, 1));
                        }
                        state = State.START;
                        break;
                    }
                case AFTER_EQUAL:
                    {
                        if (pos != chars.length() && chars.charAt(pos) == '=') {
                            tokens.add(new Token(Kind.EQUAL, startpos, 2));
                            state = State.START;
                            pos++;
                        } else {
                            throw new IllegalCharException("Illegal character encountered - AFTER_EQUAL State");
                        }
                        break;
                    }
                case AFTER_OR:
                    {
                        if (pos != chars.length() && chars.charAt(pos) == '-') {
                            if ((pos + 1) != chars.length() && chars.charAt(pos + 1) == '>') {
                                tokens.add(new Token(Kind.BARARROW, startpos, 3));
                                pos = pos + 2;
                            } else {
                                tokens.add(new Token(Kind.OR, startpos, 1));
                                tokens.add(new Token(Kind.MINUS, pos, 1));
                                pos++;
                                state = State.START;
                            }
                        } else {
                            tokens.add(new Token(Kind.OR, startpos, 1));
                        }
                        state = State.START;
                        break;
                    }
                case AFTER_MINUS:
                    {
                        if (pos != chars.length() && chars.charAt(pos) == '>') {
                            tokens.add(new Token(Kind.ARROW, startpos, 2));
                            pos++;
                        } else {
                            tokens.add(new Token(Kind.MINUS, startpos, 1));
                        }
                        state = State.START;
                        break;
                    }
                case AFTER_LESS_THAN:
                    {
                        if (pos != chars.length() && chars.charAt(pos) == '=') {
                            tokens.add(new Token(Kind.LE, startpos, 2));
                            pos++;
                        } else if (pos != chars.length() && chars.charAt(pos) == '-') {
                            tokens.add(new Token(Kind.ASSIGN, startpos, 2));
                            pos++;
                        } else {
                            tokens.add(new Token(Kind.LT, startpos, 1));
                        }
                        state = State.START;
                        break;
                    }
                case AFTER_GREATER_THAN:
                    {
                        if (pos != chars.length() && chars.charAt(pos) == '=') {
                            tokens.add(new Token(Kind.GE, startpos, 2));
                            pos++;
                        } else {
                            tokens.add(new Token(Kind.GT, startpos, 1));
                        }
                        state = State.START;
                        break;
                    }
                case IN_INT_LIT:
                    {
                        if (Character.isDigit(ch)) {
                            pos++;
                        } else {
                            Token t = new Token(Kind.INT_LIT, startpos, pos - startpos);
                            int ans = t.intVal();
                            if (ans != 0) {
                                tokens.add(t);
                                state = State.START;
                            }
                        }
                        break;
                    }
                case IN_IDENT:
                    {
                        if (Character.isJavaIdentifierPart(ch)) {

                            pos++;
                        } else if (!chars.substring(startpos, pos).contains("$") && !chars.substring(startpos, pos).contains("_")) {
                            switch (chars.substring(startpos, pos)) {
                                case "integer":
                                    tokens.add(new Token(Kind.KW_INTEGER, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "boolean":
                                    tokens.add(new Token(Kind.KW_BOOLEAN, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "image":
                                    tokens.add(new Token(Kind.KW_IMAGE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "url":
                                    tokens.add(new Token(Kind.KW_URL, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "file":
                                    tokens.add(new Token(Kind.KW_FILE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "frame":
                                    tokens.add(new Token(Kind.KW_FRAME, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "while":
                                    tokens.add(new Token(Kind.KW_WHILE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "if":
                                    tokens.add(new Token(Kind.KW_IF, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "true":
                                    tokens.add(new Token(Kind.KW_TRUE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "false":
                                    tokens.add(new Token(Kind.KW_FALSE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "screenheight":
                                    tokens.add(new Token(Kind.KW_SCREENHEIGHT, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "screenwidth":
                                    tokens.add(new Token(Kind.KW_SCREENWIDTH, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "xloc":
                                    tokens.add(new Token(Kind.KW_XLOC, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "yloc":
                                    tokens.add(new Token(Kind.KW_YLOC, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "hide":
                                    tokens.add(new Token(Kind.KW_HIDE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "show":
                                    tokens.add(new Token(Kind.KW_SHOW, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "move":
                                    tokens.add(new Token(Kind.KW_MOVE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "scale":
                                    tokens.add(new Token(Kind.KW_SCALE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "blur":
                                    tokens.add(new Token(Kind.OP_BLUR, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "gray":
                                    tokens.add(new Token(Kind.OP_GRAY, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "convolve":
                                    tokens.add(new Token(Kind.OP_CONVOLVE, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "width":
                                    tokens.add(new Token(Kind.OP_WIDTH, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "height":
                                    tokens.add(new Token(Kind.OP_HEIGHT, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                case "sleep":
                                    tokens.add(new Token(Kind.OP_SLEEP, startpos, pos - startpos));
                                    state = State.START;
                                    break;
                                default:
                                    {
                                        tokens.add(new Token(Kind.IDENT, startpos, pos - startpos));state = State.START;
                                        break;
                                    }
                            }
                        }
                        else{
                        	tokens.add(new Token(Kind.IDENT, startpos, pos - startpos));state = State.START;
                        }
                    }
            }
        }
        return this;
    }

    private int skipWhiteSpace(int pos) {
        while (pos < chars.length()) {
            if (chars.substring(pos, pos + 1).contains("\n")) {
                line_start = pos + 1;
                line_pos++;
                pos++;
            } else if (Character.isWhitespace(chars.charAt(pos))) {
                pos++;
            } else {
                break;
            }
        }
        return pos;
    }

    final ArrayList < Token > tokens;
    final String chars;
    int tokenNum;

    /*
     * Return the next token in the token list and update the state so that
     * the next call will return the Token..  
     */
    public Token nextToken() {
        if (tokenNum >= tokens.size())
            return null;
        return tokens.get(tokenNum++);
    }

    /*
     * Return the next token in the token list without updating the state.
     * (So the following call to next will return the same token.)
     */
    public Token peek() {
    	  if (tokenNum >= tokens.size())
    	        return null;
    	    return tokens.get(tokenNum);
    }

    /**
     * Returns a LinePos object containing the line and position in line of the 
     * given token.  
     * 
     * Line numbers start counting at 0
     * 
     * @param t
     * @return
     */
    public LinePos getLinePos(Token t) {
        //TODO IMPLEMENT THIS
        LinePos lp = t.getLinePos();
        return lp;
    }
}