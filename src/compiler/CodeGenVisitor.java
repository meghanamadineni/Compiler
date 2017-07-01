package compiler;

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.TraceClassVisitor;

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
import compiler.AST.WhileStatement;
import compiler.AST.Type.TypeName;
import compiler.Scanner.Kind;
import compiler.Scanner.Token;
import compiler.AST.AssignmentStatement;

import static compiler.AST.Type.TypeName.FRAME;
import static compiler.AST.Type.TypeName.IMAGE;
import static compiler.AST.Type.TypeName.URL;
import static compiler.Scanner.Kind.*;

public class CodeGenVisitor implements ASTVisitor, Opcodes {

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 */
	public CodeGenVisitor(boolean DEVEL, boolean GRADE, String sourceFileName) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
	}

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction
	FieldVisitor fv; // visitor of field 
	
	int paramDecCount = 0, slotNumber = 1;
	
	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		className = program.getName();
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object",
				new String[] { "java/lang/Runnable" });
		cw.visitSource(sourceFileName, null);

		// generate constructor code
		// get a MethodVisitor
		mv = cw.visitMethod(ACC_PUBLIC, "<init>", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		// Create label at start of code
		Label constructorStart = new Label();
		mv.visitLabel(constructorStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering <init>");
		// generate code to call superclass constructor
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
		// visit parameter decs to add each as field to the class
		// pass in mv so decs can add their initialization code to the
		// constructor.
		ArrayList<ParamDec> params = program.getParams();
		for (ParamDec dec : params)
			dec.visit(this, mv);
		mv.visitInsn(RETURN);
		// create label at end of code
		Label constructorEnd = new Label();
		mv.visitLabel(constructorEnd);
		// finish up by visiting local vars of constructor
		// the fourth and fifth arguments are the region of code where the local
		// variable is defined as represented by the labels we inserted.
		mv.visitLocalVariable("this", classDesc, null, constructorStart, constructorEnd, 0);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, constructorStart, constructorEnd, 1);
		// indicates the max stack size for the method.
		// because we used the COMPUTE_FRAMES parameter in the classwriter
		// constructor, asm
		// will do this for us. The parameters to visitMaxs don't matter, but
		// the method must
		// be called.
		mv.visitMaxs(1, 1);
		// finish up code generation for this method.
		mv.visitEnd();
		// end of constructor

		// create main method which does the following
		// 1. instantiate an instance of the class being generated, passing the
		// String[] with command line arguments
		// 2. invoke the run method.
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null,
				null);
		mv.visitCode();
		Label mainStart = new Label();
		mv.visitLabel(mainStart);
		// this is for convenience during development--you can see that the code
		// is doing something.
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering main");
		mv.visitTypeInsn(NEW, className);
		mv.visitInsn(DUP);
		mv.visitVarInsn(ALOAD, 0);
		mv.visitMethodInsn(INVOKESPECIAL, className, "<init>", "([Ljava/lang/String;)V", false);
		mv.visitMethodInsn(INVOKEVIRTUAL, className, "run", "()V", false);
		mv.visitInsn(RETURN);
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		mv.visitLocalVariable("instance", classDesc, null, mainStart, mainEnd, 1);
		mv.visitMaxs(0, 0);
		mv.visitEnd();

		// create run method
		mv = cw.visitMethod(ACC_PUBLIC, "run", "()V", null, null);
		mv.visitCode();
		Label startRun = new Label();
		mv.visitLabel(startRun);
		CodeGenUtils.genPrint(DEVEL, mv, "\nentering run");
		program.getB().visit(this, null);
		mv.visitInsn(RETURN);
		Label endRun = new Label();
		mv.visitLabel(endRun);
		mv.visitLocalVariable("this", classDesc, null, startRun, endRun, 0);
//TODO  visit the local variables
		ArrayList<Dec> dec= program.getB().getDecs();
		for( Dec dc: dec){
			mv.visitLocalVariable(dc.getIdent().getText(), dc.getTypeName().getJVMTypeDesc(), null, startRun, endRun, dc.getSlotNumber());
		}
		mv.visitMaxs(1, 1);
		mv.visitEnd(); // end of run method
		
		
		cw.visitEnd();//end of class
		
		//generate classfile and return it
		return cw.toByteArray();
	}



	@Override
	public Object visitAssignmentStatement(AssignmentStatement assignStatement, Object arg) throws Exception {
		assignStatement.getE().visit(this, arg);
		CodeGenUtils.genPrint(DEVEL, mv, "\nassignment: " + assignStatement.var.getText() + "=");
		CodeGenUtils.genPrintTOS(GRADE, mv, assignStatement.getE().getTypeName());
		assignStatement.getVar().visit(this, arg);
		return null;
	}

	@Override
	public Object visitBinaryChain(BinaryChain binaryChain, Object arg) throws Exception {
		binaryChain.getE0().visit(this, "left");
		TypeName typeName = binaryChain.getE0().getTypeName();
		switch(typeName){
			case URL:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className,"readFromURL", PLPRuntimeImageIO.readFromURLSig,false);
				break;
			}
			case FILE:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className,"readFromFile", PLPRuntimeImageIO.readFromFileDesc,false);
				break;
			}
			case NONE:{
				mv.visitInsn(POP);
			}
		}
		if(binaryChain.getE1().getClass() == FilterOpChain.class){
			Token operator = binaryChain.getArrow();
			if(operator.kind == ARROW){
				mv.visitInsn(ACONST_NULL);
			}
			else if(operator.kind == Kind.BARARROW){
				mv.visitInsn(DUP);
			}
		}
		binaryChain.getE1().visit(this, "right");
		return null;
	}

	@Override
	public Object visitBinaryExpression(BinaryExpression binaryExpression, Object arg) throws Exception {
		Expression expr1 = binaryExpression.getE0();
		Expression expr2 = binaryExpression.getE1();
		expr1.visit(this, arg);
		expr2.visit(this, arg);
		Kind operatorKind = binaryExpression.getOp().kind;
		switch(operatorKind){
		case LT:{
			Label l3 = new Label();
			mv.visitJumpInsn(IF_ICMPGE, l3);
			mv.visitInsn(ICONST_1);
			Label l4 = new Label();
			mv.visitJumpInsn(GOTO, l4);
			mv.visitLabel(l3);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l4);
			break;}
		case LE:{
			Label l3 = new Label();
			mv.visitJumpInsn(IF_ICMPGT, l3);
			mv.visitInsn(ICONST_1);
			Label l4 = new Label();
			mv.visitJumpInsn(GOTO, l4);
			mv.visitLabel(l3);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l4);
			break;
		}
		case GT:{
			Label l3 = new Label();
			mv.visitJumpInsn(IF_ICMPLE, l3);
			mv.visitInsn(ICONST_1);
			Label l4 = new Label();
			mv.visitJumpInsn(GOTO, l4);
			mv.visitLabel(l3);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l4);
			break;
		}
		case GE:{
			Label l3 = new Label();
			mv.visitJumpInsn(IF_ICMPLT, l3);
			mv.visitInsn(ICONST_1);
			Label l4 = new Label();
			mv.visitJumpInsn(GOTO, l4);
			mv.visitLabel(l3);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l4);
			break;
		}
		case EQUAL:{
			Label l3 = new Label();
			mv.visitJumpInsn(IF_ICMPNE, l3);
			mv.visitInsn(ICONST_1);
			Label l4 = new Label();
			mv.visitJumpInsn(GOTO, l4);
			mv.visitLabel(l3);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l4);
			break;
		}
		case NOTEQUAL:{
			Label l3 = new Label();
			mv.visitJumpInsn(IF_ICMPEQ, l3);
			mv.visitInsn(ICONST_1);
			Label l4 = new Label();
			mv.visitJumpInsn(GOTO, l4);
			mv.visitLabel(l3);
			mv.visitInsn(ICONST_0);
			mv.visitLabel(l4);
			break;
		}
		case PLUS:
			if(expr1.getTypeName() == IMAGE && expr2.getTypeName() == IMAGE){
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "add", PLPRuntimeImageOps.addSig , false);
			}
		    else if(expr1.getTypeName() == TypeName.INTEGER && expr2.getTypeName() == TypeName.INTEGER){
		    	mv.visitInsn(IADD);
		    }
			break;
		case MINUS:
			if(expr1.getTypeName() == IMAGE && expr2.getTypeName() == IMAGE){
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "sub", PLPRuntimeImageOps.subSig , false);
			}
		    else if(expr1.getTypeName() == TypeName.INTEGER && expr2.getTypeName() == TypeName.INTEGER){
		    	mv.visitInsn(ISUB);
		    }
			break;
		case OR:
			mv.visitInsn(IOR);
			break;
		case TIMES:
			if(expr1.getTypeName() == TypeName.INTEGER && expr2.getTypeName() == TypeName.INTEGER){
				mv.visitInsn(IMUL);
			}
			else if(expr1.getTypeName() == TypeName.INTEGER && expr2.getTypeName() == IMAGE){
				mv.visitInsn(SWAP);
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"mul", PLPRuntimeImageOps.mulSig, false);
			}
			else if(expr1.getTypeName() == IMAGE && expr2.getTypeName() == TypeName.INTEGER){
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"mul", PLPRuntimeImageOps.mulSig, false);
			}
			break;
		case DIV:
			if(expr1.getTypeName() == IMAGE && expr2.getTypeName() == TypeName.INTEGER){
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"div", PLPRuntimeImageOps.divSig, false);
			}
			else if(expr1.getTypeName() == TypeName.INTEGER && expr2.getTypeName() == TypeName.INTEGER){
				mv.visitInsn(IDIV);
			}
			break;
		case AND:
		    mv.visitInsn(IAND);
			break;
		case MOD:
			if(expr1.getTypeName() == IMAGE && expr2.getTypeName() == TypeName.INTEGER){
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"mod", PLPRuntimeImageOps.modSig, false);
			}
			else if(expr1.getTypeName() == TypeName.INTEGER && expr2.getTypeName() == TypeName.INTEGER){
				mv.visitInsn(IREM);
			}
			break;	
		}
		return null;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		ArrayList<Dec> arrlist1 = block.getDecs();
		ArrayList<Statement> arrlist2 = block.getStatements();
		for(Dec dec : arrlist1){
			dec.visit(this, null);
		}
		for(Statement st: arrlist2){
			st.visit(this, null);
			if(st.getClass() == BinaryChain.class){
				mv.visitInsn(POP);
			}
		}
		return null;
	}

	@Override
	public Object visitBooleanLitExpression(BooleanLitExpression booleanLitExpression, Object arg) throws Exception {
		if(booleanLitExpression.getValue() == false)
			mv.visitInsn(ICONST_0);
		else
			mv.visitInsn(ICONST_1);
		return null;
	}

	@Override
	public Object visitConstantExpression(ConstantExpression constantExpression, Object arg) {
		Token token = constantExpression.getFirstToken();
		Kind kind = token.kind;
		switch(kind){
			case KW_SCREENWIDTH:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenWidth", PLPRuntimeFrame.getScreenWidthSig, false);
				break;
			}
			case KW_SCREENHEIGHT:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName, "getScreenHeight", PLPRuntimeFrame.getScreenHeightSig, false);
				break;
			}
		}
		return null;
	}

	@Override
	public Object visitDec(Dec declaration, Object arg) throws Exception {
		declaration.setSlotNumber(slotNumber++);
		TypeName typeName = declaration.getTypeName();
		switch(typeName){
			case INTEGER:
			case BOOLEAN:{
				mv.visitInsn(ICONST_0);
				mv.visitVarInsn(ISTORE, declaration.getSlotNumber());
				break;
			}
			case IMAGE:
			case FRAME:{
				mv.visitInsn(ACONST_NULL);
				mv.visitVarInsn(ASTORE, declaration.getSlotNumber());
				break;
			}
		}
		return null;
	}

	@Override
	public Object visitFilterOpChain(FilterOpChain filterOpChain, Object arg) throws Exception {
		filterOpChain.getArg().visit(this, arg);
		Token operator = filterOpChain.getFirstToken();
		Kind kind = operator.kind;
		switch(kind){
			case OP_BLUR:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "blurOp", PLPRuntimeFilterOps.opSig, false);
				break;
			}
			case OP_GRAY:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "grayOp", PLPRuntimeFilterOps.opSig, false);
				break;
			}
			case OP_CONVOLVE:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFilterOps.JVMName, "convolveOp", PLPRuntimeFilterOps.opSig, false);
				break;			
			}
		}
		return null;
	}

	@Override
	public Object visitFrameOpChain(FrameOpChain frameOpChain, Object arg) throws Exception {
		frameOpChain.getArg().visit(this, arg);
		Token operator = frameOpChain.getFirstToken();
		Kind kind = operator.kind;
		switch(kind){
			case KW_SHOW :{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "showImage", PLPRuntimeFrame.showImageDesc, false);
				break;
			}
			case KW_HIDE :{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "hideImage", PLPRuntimeFrame.hideImageDesc, false);
				break;
			}
			case KW_MOVE :{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "moveFrame", PLPRuntimeFrame.moveFrameDesc, false);
				break;			
			}
			case KW_XLOC :{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getXVal", PLPRuntimeFrame.getXValDesc, false);
				break;
			}
			case KW_YLOC :{
				mv.visitMethodInsn(INVOKEVIRTUAL, PLPRuntimeFrame.JVMClassName, "getYVal", PLPRuntimeFrame.getYValDesc, false);
				break;			
			}
		}
		return null;
	}

	@Override
	public Object visitIdentChain(IdentChain identChain, Object arg) throws Exception {
		if(arg == "left"){
			if(identChain.getDec().getClass() == ParamDec.class){
				mv.visitVarInsn(ALOAD, 0);
				mv.visitFieldInsn(GETFIELD, className, identChain.getDec().getIdent().getText(), identChain.getDec().getTypeName().getJVMTypeDesc());
			}
			else{
				if(identChain.getDec().getTypeName() == TypeName.INTEGER || identChain.getDec().getTypeName() == TypeName.BOOLEAN ){
					mv.visitVarInsn(ILOAD, identChain.getDec().getSlotNumber());
				}
				else{
					mv.visitVarInsn(ALOAD, identChain.getDec().getSlotNumber());
				}
			}
		}
		else if(arg == "right"){
			if(identChain.getDec().getClass() == ParamDec.class){
				if(identChain.getDec().getTypeName() == TypeName.INTEGER){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitInsn(SWAP);
					mv.visitFieldInsn(PUTFIELD, className, identChain.getDec().getIdent().getText(), identChain.getDec().getTypeName().getJVMTypeDesc());
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, identChain.getDec().getIdent().getText(), identChain.getDec().getTypeName().getJVMTypeDesc());
				}
				else if(identChain.getDec().getTypeName() == TypeName.FILE){
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, identChain.getDec().getIdent().getText(), identChain.getDec().getTypeName().getJVMTypeDesc());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className,"write", PLPRuntimeImageIO.writeImageDesc,false);
					mv.visitInsn(POP);
					mv.visitVarInsn(ALOAD, 0);
					mv.visitFieldInsn(GETFIELD, className, identChain.getDec().getIdent().getText(), identChain.getDec().getTypeName().getJVMTypeDesc());
				}
			}
			else{
				if(identChain.getDec().getTypeName() == TypeName.FRAME){
					mv.visitVarInsn(ALOAD, identChain.getDec().getSlotNumber());
					mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeFrame.JVMClassName,"createOrSetFrame", PLPRuntimeFrame.createOrSetFrameSig,false);
					mv.visitVarInsn(ASTORE, identChain.getDec().getSlotNumber());
					mv.visitVarInsn(ALOAD, identChain.getDec().getSlotNumber());
				}
				else if(identChain.getDec().getTypeName() == TypeName.INTEGER || identChain.getDec().getTypeName() == TypeName.BOOLEAN){
					mv.visitVarInsn(ISTORE, identChain.getDec().getSlotNumber());
					mv.visitVarInsn(ILOAD, identChain.getDec().getSlotNumber());
				}
				else if(identChain.getDec().getTypeName() == TypeName.IMAGE){
					mv.visitVarInsn(ASTORE, identChain.getDec().getSlotNumber());
					mv.visitVarInsn(ALOAD, identChain.getDec().getSlotNumber());	
				}
			}
		}
		return null;
	}

	@Override
	public Object visitIdentExpression(IdentExpression identExpression, Object arg) throws Exception {
		if(identExpression.getDec().getClass() == ParamDec.class){
			mv.visitVarInsn(ALOAD, 0);
			mv.visitFieldInsn(GETFIELD, className, identExpression.getDec().getIdent().getText(), identExpression.getDec().getTypeName().getJVMTypeDesc());
		}
		else{
			if(identExpression.getDec().getTypeName() == TypeName.INTEGER || identExpression.getDec().getTypeName() == TypeName.BOOLEAN){
				mv.visitVarInsn(ILOAD, identExpression.getDec().getSlotNumber());
			}
			else{
				mv.visitVarInsn(ALOAD, identExpression.getDec().getSlotNumber());
			}
		}
		return null;
	}

	@Override
	public Object visitIdentLValue(IdentLValue identX, Object arg) throws Exception {
		if(identX.getDec().getClass() == ParamDec.class){
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(SWAP);
			mv.visitFieldInsn(PUTFIELD, className, identX.getDec().getIdent().getText(), identX.getDec().getTypeName().getJVMTypeDesc());
		}
		else{
			if(identX.getDec().getTypeName() == TypeName.INTEGER || identX.getDec().getTypeName() == TypeName.BOOLEAN){
				mv.visitVarInsn(ISTORE, identX.getDec().getSlotNumber());
			}
			else if(identX.getDec().getTypeName() == TypeName.FRAME){
				mv.visitVarInsn(ASTORE, identX.getDec().getSlotNumber());
			}
			else if(identX.getDec().getTypeName() == TypeName.IMAGE){
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName,"copyImage", PLPRuntimeImageOps.copyImageSig,false);
				mv.visitVarInsn(ASTORE, identX.getDec().getSlotNumber());
			}
		}
		return null;

	}

	@Override
	public Object visitIfStatement(IfStatement ifStatement, Object arg) throws Exception {
		Label l1 = new Label();
		ifStatement.getE().visit(this, arg);
		mv.visitJumpInsn(IFEQ, l1);
		ifStatement.getB().visit(this, arg);
		mv.visitLabel(l1);
		return null;
	}

	@Override
	public Object visitImageOpChain(ImageOpChain imageOpChain, Object arg) throws Exception {
		imageOpChain.getArg().visit(this, arg);
		Token operator = imageOpChain.getFirstToken();
		Kind kind = operator.kind;
		switch(kind){
			case OP_WIDTH:{
				mv.visitMethodInsn(INVOKEVIRTUAL, "java/awt/image/BufferedImage" , "getWidth", PLPRuntimeImageOps.getWidthSig, false);
				break;
			}
			case OP_HEIGHT:{
				mv.visitMethodInsn(INVOKEVIRTUAL,"java/awt/image/BufferedImage" , "getHeight", PLPRuntimeImageOps.getHeightSig, false);
				break;
			}
			case KW_SCALE:{
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageOps.JVMName, "scale", PLPRuntimeImageOps.scaleSig, false);
				break;			
			}
		}
		return null;
	}

	@Override
	public Object visitIntLitExpression(IntLitExpression intLitExpression, Object arg) throws Exception {
		mv.visitIntInsn(SIPUSH, intLitExpression.value);
		return null;
	}


	@Override
	public Object visitParamDec(ParamDec paramDec, Object arg) throws Exception {
		fv = cw.visitField(0, paramDec.getIdent().getText(), paramDec.getTypeName().getJVMTypeDesc(), null, null);
		fv.visitEnd();
		TypeName typeName = paramDec.getTypeName();
		switch(typeName){
			case INTEGER:{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitIntInsn(SIPUSH, paramDecCount++);
				mv.visitInsn(AALOAD);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
				break;
			}
			case BOOLEAN:{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitIntInsn(SIPUSH,paramDecCount++);
				mv.visitInsn(AALOAD);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);
				break;
			}
			case FILE:{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitTypeInsn(NEW, "java/io/File");
				mv.visitInsn(DUP);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitIntInsn(SIPUSH, paramDecCount++);
				mv.visitInsn(AALOAD);
				mv.visitMethodInsn(INVOKESPECIAL,  "java/io/File" , "<init>", "(Ljava/lang/String;)V", false);
				break;
			}
			case URL:{
				mv.visitVarInsn(ALOAD, 0);
				mv.visitVarInsn(ALOAD, 1);
				mv.visitIntInsn(SIPUSH, paramDecCount++);
				mv.visitMethodInsn(INVOKESTATIC, PLPRuntimeImageIO.className, "getURL",PLPRuntimeImageIO.getURLSig, false);
				break;
			}
		}
		mv.visitFieldInsn(PUTFIELD, className, paramDec.getIdent().getText(), paramDec.getTypeName().getJVMTypeDesc());
		return null;

	}

	@Override
	public Object visitSleepStatement(SleepStatement sleepStatement, Object arg) throws Exception {
		sleepStatement.getE().visit(this, arg);
		mv.visitInsn(I2L);
		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);
		return null;
	}

	@Override
	public Object visitTuple(Tuple tuple, Object arg) throws Exception {
		ArrayList<Expression> exprList = (ArrayList<Expression>) tuple.getExprList();
		for(Expression expr: exprList){
			expr.visit(this, arg);
		}
		return null;
	}

	@Override
	public Object visitWhileStatement(WhileStatement whileStatement, Object arg) throws Exception {
		Label l1 = new Label();
		Label l2 = new Label();
		mv.visitJumpInsn(GOTO, l1);
		mv.visitLabel(l2);
		whileStatement.getB().visit(this, arg);
		mv.visitLabel(l1);
		whileStatement.getE().visit(this, arg);
		mv.visitJumpInsn(IFNE, l2);
		return null;
	}

}
