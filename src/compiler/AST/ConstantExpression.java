package compiler.AST;

import compiler.Scanner.Token;

public class ConstantExpression extends Expression {

	public ConstantExpression(Token firstToken) {
		super(firstToken);
	}
	

	@Override
	public String toString() {
		return "ConstantExpression [firstToken=" + firstToken + "]";
	}


	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitConstantExpression(this,arg);
		
	}

}
