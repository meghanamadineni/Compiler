package compiler.AST;

import compiler.AST.Type.TypeName;
import compiler.Scanner.Token;

public class IdentLValue extends ASTNode {
	
	private Dec dec;

	public void setDec(Dec dec) {
		this.dec = dec;
	}

	public Dec getDec (){
		return dec;
	}
	
	public IdentLValue(Token firstToken) {
		super(firstToken);
	}
	
	@Override
	public String toString() {
		return "IdentLValue [firstToken=" + firstToken + "]";
	}

	@Override
	public Object visit(ASTVisitor v, Object arg) throws Exception {
		return v.visitIdentLValue(this,arg);
	}

	public String getText() {
		return firstToken.getText();
	}

}
