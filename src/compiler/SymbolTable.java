package compiler;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import compiler.AST.Dec;


public class SymbolTable {

	
		
		public class SymbolTableEntry{
			int scope;
			Dec declaration;
		}

		int c_scope=0,n_scope=0;
		Stack<Integer> scopeStack = new Stack<Integer>();
		HashMap<String, ArrayList<SymbolTableEntry>> symbolTable = new HashMap<String, ArrayList<SymbolTableEntry>>();
		
		/** 
		 * to be called when block entered
		 */
		public void enterScope(){
			c_scope = n_scope++;
			scopeStack.push(c_scope);
			
		}
		
		
		/**
		 * leaves scope
		 */
		public void leaveScope(){
			scopeStack.pop();
			c_scope = scopeStack.peek();
		}
	
		public boolean insert(String ident, Dec dec){
			if(symbolTable.containsKey(ident)){
				ArrayList<SymbolTableEntry> temp = symbolTable.get(ident);
				for(SymbolTableEntry t: temp){
					if(t.scope == c_scope){
						return false;
					}
				}
				SymbolTableEntry e = new SymbolTableEntry();
				e.scope = c_scope;
				e.declaration = dec;
				temp.add(0,e);
				symbolTable.put(ident, temp);
				
			}
			else{
				ArrayList<SymbolTableEntry> temp = new ArrayList<SymbolTableEntry>();
				SymbolTableEntry e = new SymbolTableEntry();
				e.scope = c_scope;
				e.declaration = dec;
				temp.add(0,e);
				symbolTable.put(ident, temp);
				
			}
			return true;
		}
	
	public Dec lookup(String ident){
		if(symbolTable.containsKey(ident)){
			ArrayList<SymbolTableEntry> temp = symbolTable.get(ident);
			for(SymbolTableEntry t: temp){
				if(t.scope <= c_scope){
					return t.declaration;
				}
			}
		}
		return null;
	}
		
	public SymbolTable() {
		enterScope();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(String str : symbolTable.keySet()){
			sb.append(str);
			for(SymbolTableEntry st : symbolTable.get(str)){
				sb.append(st.scope);
				sb.append(st.declaration);
			}
			sb.append(" ");
		}
		return sb.toString();
	}
	
	


}
