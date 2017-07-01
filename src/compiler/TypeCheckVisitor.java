package compiler;

import compiler.AST.*;

import java.util.ArrayList;
import java.util.List;

import compiler.AST.ASTVisitor;
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
import compiler.AST.Type;
import compiler.AST.WhileStatement;
import compiler.AST.Type.TypeName.*;
import compiler.Scanner.Kind;
import compiler.Scanner.LinePos;
import compiler.Scanner.Token;

import static compiler.AST.Type.*;
import static compiler.Scanner.Kind.*;
import static compiler.AST.Type.TypeName.*;

public class TypeCheckVisitor implements ASTVisitor {

	@SuppressWarnings("serial")
	public static class TypeCheckException extends Exception {
		TypeCheckException(String message) {
			super(message);
		}
	}

	SymbolTable symtab = new SymbolTable();

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {

		Chain chain1 = binaryChain.getE0();
		ChainElem chainElm1 = binaryChain.getE1();
		chain1.visit(this,null);
		chainElm1.visit(this,null);
		Token t = binaryChain.getArrow();
		TypeName chain = chain1.getTypeName();
		TypeName chainElm = chainElm1.getTypeName();
		if(chain == URL && chainElm == IMAGE && t.isKind(ARROW)){
			binaryChain.setTypeName(IMAGE);
		}
		else if(chain == FILE && chainElm == IMAGE && t.isKind(ARROW)){
			binaryChain.setTypeName(IMAGE);
		}
		else if(chain == IMAGE && chainElm == FRAME && t.isKind(ARROW) ){
			binaryChain.setTypeName(FRAME);
		}
		else if(chain == IMAGE && chainElm == FILE && t.isKind(ARROW)){
			binaryChain.setTypeName(NONE);
		}
		else if(chain == FRAME && (binaryChain.getE1() instanceof FrameOpChain) && (binaryChain.getE1().getFirstToken().isKind(KW_XLOC) || binaryChain.getE1().getFirstToken().isKind(KW_YLOC)) && t.isKind(ARROW)){
			binaryChain.setTypeName(INTEGER);
		}
		else if(chain == FRAME && (binaryChain.getE1() instanceof FrameOpChain) && (binaryChain.getE1().getFirstToken().isKind(KW_SHOW) || binaryChain.getE1().getFirstToken().isKind(KW_HIDE) || binaryChain.getE1().getFirstToken().isKind(KW_MOVE)) && t.isKind(ARROW)){
			binaryChain.setTypeName(FRAME);
		}
		else if(chain == IMAGE && (binaryChain.getE1() instanceof ImageOpChain) && (binaryChain.getE1().getFirstToken().isKind(OP_WIDTH) || binaryChain.getE1().getFirstToken().isKind(OP_HEIGHT)) && t.isKind(ARROW)){
			binaryChain.setTypeName(INTEGER);
		}
		else if(chain == IMAGE && (binaryChain.getE1() instanceof FilterOpChain) && (binaryChain.getE1().getFirstToken().isKind(OP_BLUR) || binaryChain.getE1().getFirstToken().isKind(OP_CONVOLVE) || binaryChain.getE1().getFirstToken().isKind(OP_GRAY)) && (t.isKind(ARROW) || t.isKind(BARARROW))){
			binaryChain.setTypeName(IMAGE);
		}
		else if(chain == IMAGE && (binaryChain.getE1() instanceof ImageOpChain) && (binaryChain.getE1().getFirstToken().isKind(KW_SCALE)) && t.isKind(ARROW)){
			binaryChain.setTypeName(IMAGE);
		}
		else if(chain == IMAGE && (binaryChain.getE1() instanceof IdentChain) && t.isKind(ARROW)){
			binaryChain.setTypeName(IMAGE);
		}
		else if(chain == INTEGER && (binaryChain.getE1() instanceof IdentChain) && t.isKind(ARROW)  && binaryChain.getE1().getTypeName() == INTEGER ){
			binaryChain.setTypeName(INTEGER);
		}
		else if(chain == IMAGE && (binaryChain.getE1() instanceof IdentChain) && t.isKind(ARROW)  && binaryChain.getE1().getTypeName() == IMAGE ){
			binaryChain.setTypeName(IMAGE);
		}
		else{
			throw new TypeCheckException("Unknown Type found");
		}
		return binaryChain.getTypeName();
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		Expression e0 = binaryExpression.getE0();
		Expression e1 = binaryExpression.getE1();
		e0.visit(this,null);
		e1.visit(this,null);
		TypeName type1 = e0.getTypeName();
		TypeName type2 = e1.getTypeName();
		Token op = binaryExpression.getOp();
		if(op.isKind(EQUAL) || op.isKind(NOTEQUAL)){
			if(type1 == type2){
				binaryExpression.setTypeName(BOOLEAN);
			}
			else{
				throw new TypeCheckException("type mismatch");
			}
		}
		else if(type1==INTEGER && type2 == INTEGER){
			if(op.isKind(PLUS) || op.isKind(MINUS) ||op.isKind(TIMES)|| op.isKind(DIV) || op.isKind(MOD)) {
				binaryExpression.setTypeName(INTEGER);
			}
			else if(op.isKind(LT) || op.isKind(GT) ||op.isKind(LE) || op.isKind(GE)){
				binaryExpression.setTypeName(BOOLEAN);
			}
			else{
				throw new TypeCheckException("operator not intended");
			}
		}
		else if(type1 == IMAGE && type2 == IMAGE){
			if(op.isKind(PLUS) || op.isKind(MINUS) ) {
				binaryExpression.setTypeName(IMAGE);
			}
			else{
				throw new TypeCheckException("illegal operator for image");
			}
		}
		else if(type1 == IMAGE && type2 == TypeName.INTEGER){
			if(op.isKind(TIMES) || op.isKind(DIV) || op.isKind(MOD)) {
				binaryExpression.setTypeName(IMAGE);
			}
			else{
				throw new TypeCheckException("illegal operator found");
			}
		}
		else if(type1 == TypeName.INTEGER && type2 == IMAGE){
			if(op.isKind(TIMES)) {
				binaryExpression.setTypeName(IMAGE);
			}
			else{
				throw new TypeCheckException("illegal operator found");
			}
		}
		else if(type1 == BOOLEAN && type2 == BOOLEAN){
			if(op.isKind(LT) || op.isKind(GT) ||op.isKind(LE) || op.isKind(GE) || op.isKind(AND) || op.isKind(OR)) {
				binaryExpression.setTypeName(BOOLEAN);
			}
			else{
				throw new TypeCheckException("illegal operator found for boolean");
			}
		}

		else{
			throw new TypeCheckException("type mismatch");
		}
		return binaryExpression.getTypeName();
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		symtab.enterScope();
		ArrayList<Dec> arrlist1 = block.getDecs();
		ArrayList<Statement> arrlist2 = block.getStatements();
		for(Dec dec : arrlist1){
			dec.visit(this, null);
		}
		for(Statement st: arrlist2){
			st.visit(this, null);
		}
		symtab.leaveScope();
		return block;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		booleanLitExpression.setTypeName(BOOLEAN);
		return booleanLitExpression.getTypeName();
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		Tuple arg1 = filterOpChain.getArg();
		arg1.visit(this,null);
		List<Expression> list = filterOpChain.getArg().getExprList();
		if(list.size() != 0){
			throw new TypeCheckException("there are some expressions left to evaluate");
		}
		filterOpChain.setTypeName(IMAGE);
		return filterOpChain.getTypeName();
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		frameOpChain.getArg().visit(this, null);
		Token t = frameOpChain.getFirstToken();
		if(frameOpChain.getFirstToken().isKind(KW_SHOW) || frameOpChain.getFirstToken().isKind(KW_HIDE)){
			if(frameOpChain.getArg().getExprList().size() != 0){
				throw new TypeCheckException(" expression length greater than zero");
			}
			frameOpChain.setTypeName(NONE);
		}
		else if(t.isKind(KW_XLOC) || t.isKind(KW_YLOC)){
			if(frameOpChain.getArg().getExprList().size() != 0){
				throw new TypeCheckException(" expression length greater than zero");
			}
			frameOpChain.setTypeName(INTEGER);
		}
		else if(t.isKind(KW_MOVE)){
			if(frameOpChain.getArg().getExprList().size() != 2){
				throw new TypeCheckException(" expression length not equals two");
			}
			frameOpChain.setTypeName(NONE);
		}
		else{
			throw new TypeCheckException("Error in frame op chain");
		}
		return frameOpChain.getTypeName();
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		Dec dec = symtab.lookup(identChain.getFirstToken().getText());
		if(dec == null){
			throw new TypeCheckException("declaration is null");
		}
		identChain.setDec(dec);
		identChain.setTypeName(dec.getTypeName());
		return identChain.getTypeName();
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		Dec dec = symtab.lookup(identExpression.getFirstToken().getText());
		if (dec == null) {
			throw new TypeCheckException("declaration is null");
		}
		identExpression.setTypeName(dec.getTypeName());
		identExpression.setDec(dec);
		return identExpression.getTypeName();
	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		ifStatement.getB().visit(this,null);
		ifStatement.getE().visit(this,null);
		if(ifStatement.getE().getTypeName() != BOOLEAN){
			throw new TypeCheckException("if statement not giving a boolean answer");
		}
		return ifStatement;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		intLitExpression.setTypeName(INTEGER);
		return intLitExpression.getTypeName();
	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		sleepStatement.getE().visit(this,null);
		if(sleepStatement.getE().getTypeName() != INTEGER){
			throw new TypeCheckException("sleep statement not giving integer");
		}
		return sleepStatement;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		whileStatement.getE().visit(this,null);
		whileStatement.getB().visit(this, null);
		if(whileStatement.getE().getTypeName() != BOOLEAN){
			throw new TypeCheckException("while statement not returning boolean");
		}
		return whileStatement;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		Token firstToken = declaration.getType();
		TypeName typename = Type.getTypeName(firstToken);
		declaration.setTypeName(typename);
		boolean isInsert = symtab.insert(declaration.getIdent().getText(),declaration);
		if(!isInsert){
			throw new TypeCheckException("Exception");
		}
		return declaration.getTypeName();
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		ArrayList<ParamDec> arrlist = program.getParams();
		for(ParamDec pd : arrlist){
			pd.visit(this,null);
		}
		Block block = program.getB();
		block.visit(this,null);
		return program;
	}

	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		TypeName exprType = (TypeName) assignStatement.getE().visit(this, null);
		assignStatement.getVar().visit(this, null);
		IdentLValue idl = assignStatement.getVar();
		Dec dec = symtab.lookup(idl.getFirstToken().getText());
		if(dec==null){
			throw new TypeCheckException("Incompatible types in assign statement");
		}
		if(dec.getTypeName() != exprType){
			throw new TypeCheckException("Incompatible types in assign statement");
		}
		return assignStatement;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		Dec dec = symtab.lookup(identX.getText());
		if(dec == null){
			throw new TypeCheckException("declaration is null");
		}
		identX.setDec(dec);
		return identX.getDec().getTypeName();
	}

	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		Token token = paramDec.getType();
		TypeName typename = Type.getTypeName(token);
		paramDec.setTypeName(typename);
		boolean isInsert = symtab.insert(paramDec.getIdent().getText() , paramDec);
		if(!isInsert){
			throw new TypeCheckException("exception");
		}
		return paramDec.getTypeName();
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		constantExpression.setTypeName(INTEGER);
		return constantExpression.getTypeName();
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		imageOpChain.getArg().visit(this, null);
		Token t = imageOpChain.getFirstToken();
		if(t.isKind(OP_WIDTH) || t.isKind(OP_HEIGHT)){
			if(imageOpChain.getArg().getExprList().size() != 0){
				throw new TypeCheckException("expression list not zero");
			}
			imageOpChain.setTypeName(INTEGER);
		}
		else if(t.isKind(KW_SCALE)){
			if(imageOpChain.getArg().getExprList().size() != 1){
				throw new TypeCheckException("more expressions than intended");
			}
			imageOpChain.setTypeName(IMAGE);
		}
		return imageOpChain.getTypeName();
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		List<Expression> arrlist = tuple.getExprList();
		for(Expression expr : arrlist){
			expr.visit(this,null);
			if(expr.getTypeName() != INTEGER){
				throw new TypeCheckException("illegal type name of expression in tuple");
			}
		}
		return tuple;
	}


}
