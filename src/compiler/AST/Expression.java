package compiler.AST;

import compiler.AST.Type.TypeName;
import compiler.Scanner.Token;

public abstract class Expression extends ASTNode {

	
	private TypeName typeName;
	
	public TypeName getTypeName() {
		return typeName;
	}

	public void setTypeName(TypeName typeName) {
		this.typeName = typeName;
	}
	protected Expression(Token firstToken) {
		super(firstToken);
	}

	@Override
	abstract public Object visit(ASTVisitor v, Object arg) throws Exception;

}
