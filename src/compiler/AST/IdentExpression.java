package compiler.AST;

import compiler.Scanner.Token;

public class IdentExpression extends Expression {
	
	private Dec dec;

	public void setDec(Dec dec) {
		this.dec = dec;
	}

	public Dec getDec (){
		return dec;
	}

	public IdentExpression(Token firstToken) {
		super(firstToken);
	}

	@Override
	public String toString() {
		return "IdentExpression [firstToken=" + firstToken + "]";
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitIdentExpression(this, arg);
	}

}
