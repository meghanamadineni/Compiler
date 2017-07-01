package compiler.AST;

import compiler.AST.Type.TypeName;
import compiler.Scanner.Token;


public abstract class Chain extends Statement {

	private TypeName typeName;
	
	public TypeName getTypeName() {
		return typeName;
	}

	public void setTypeName(TypeName typeName) {
		this.typeName = typeName;
	}
	
	public Chain(Token firstToken) {
		super(firstToken);
	}

}
