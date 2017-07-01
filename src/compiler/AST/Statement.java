package compiler.AST;

import compiler.Scanner.Token;

public abstract class Statement extends ASTNode {

	public Statement(Token firstToken) {
		super(firstToken);
	}

	abstract public Object visit(ASTVisitor v, Object arg) throws Exception;

}
