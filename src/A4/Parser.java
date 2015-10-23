package A4;

import java.io.IOException;
import java.util.Hashtable;
//import java.util.Stack;
import java.util.Vector;

import A4.SemanticAnalyzer;

import javax.swing.tree.DefaultMutableTreeNode;

import com.sun.org.apache.bcel.internal.classfile.Code;
import com.sun.org.apache.bcel.internal.classfile.LineNumber;

public class Parser {

	private static DefaultMutableTreeNode root;
	private static Vector<A2.Token> tokens;
	private static int currentToken;
	private static Gui gui;
	private static int whileCounterGlobal = 0;
	private static int ifCounterGlobal = 0;
	
	private static int switchCaseCounterGlobal = 0;
	private static int switchCaseCounterLocal = 0;
	
	
	
	private static String nameOfSwitchID = "";
	private static String typeOfSwitchID = "";
	
	public static DefaultMutableTreeNode run(Vector<A2.Token> t, Gui gui) throws IOException {
		Parser.gui = gui;
		tokens = t;
		currentToken = 0;
		root = new DefaultMutableTreeNode("Program");
		
		//clearing the unwanted data.
		SemanticAnalyzer.setSymbolTable(new Hashtable<String, Vector<SymbolTableItem>>());
		whileCounterGlobal = 0;
		ifCounterGlobal = 0;
		switchCaseCounterGlobal = 0;
		switchCaseCounterLocal = 0;
		CodeGenerator.reset();
		CodeGenerator.clear(gui);
		rule_Program(root);
		
		//for testing purpose!
		//Hashtable<String, Vector<SymbolTableItem>> symbolTable1 = SemanticAnalyzer.getSymbolTable();
		//System.out.println(symbolTable1);

		gui.writeSymbolTable(SemanticAnalyzer.getSymbolTable());
		
		/*if(SemanticAnalyzer.SEMANTICERROR == true)
		{*/
			CodeGenerator.writeCode(gui);
		//}
		//if there is semantic error, no intermediate code will be produced!
		
		
		return root;
	}

	private static boolean rule_Program(DefaultMutableTreeNode parent) throws IOException
 {
		boolean error = true; //remove
		DefaultMutableTreeNode node;
		boolean insideProgram = false;

		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("{"))
		{
			node = new DefaultMutableTreeNode("{");
			parent.add(node);
			currentToken++;
			node = new DefaultMutableTreeNode("Body");
			parent.add(node);
			insideProgram = true;
			error = rule_Body(node);
		/*	Stack<String> stack = SemanticAnalyzer.getStack();
			System.out.println(stack);*/
		}
		else
		{
			error(1); //'{' expected
			node = new DefaultMutableTreeNode("Body");
			parent.add(node);
			insideProgram = true;
			error = rule_Body(node);
			
		}

		error = true;
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("}"))
		{
			node = new DefaultMutableTreeNode("}");
			parent.add(node);
			currentToken++;
			error = false;
			
			//exit the program, on the last closing curly brace.
			if(currentToken == tokens.size())
				CodeGenerator.addInstruction("OPR", "0", "0");
		}
		else if(insideProgram == true)
		{
			error(2); //'}' expected - need to think!
		}
		return error;
	}

	private static boolean rule_Body(DefaultMutableTreeNode parent) throws IOException
	{
		boolean error = false; //remove!
		DefaultMutableTreeNode node;
		boolean flag = false; //to check if the parser is or is not into While or if
		while (currentToken < tokens.size() && !tokens.get(currentToken).getWord().equals("}"))
		{
			if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("print"))
			{
				node = new DefaultMutableTreeNode("Print");
				parent.add(node);
				error = rule_print(node);
				flag = true;
			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("IDENTIFIER"))
			{
				node = new DefaultMutableTreeNode("Assignment");
				parent.add(node);
				error = rule_assignment(node);
				flag = true;
			}
			else if((currentToken < tokens.size() && tokens.get(currentToken).getWord().trim().equals("int")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("float")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("boolean")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("char")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("string")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("void")))
			{
				node = new DefaultMutableTreeNode("Variable"); //in the rule variable - put condition for token 'identifier'!
				parent.add(node);
				error = rule_variable(node);
				flag = true;
			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("return"))
			{
				node = new DefaultMutableTreeNode("Return");
				parent.add(node);
				error = rule_return(node);
				flag = true;
			}
			else if (!tokens.get(currentToken).getWord().equals("while") && !tokens.get(currentToken).getWord().equals("if") && !tokens.get(currentToken).getWord().equals("switch") )
			{
				//the if-else ladder above checks First(Body), if none of them occurs then error is displayed
				error(4); //expected identifier or keyword
				error = true; //if already error has occurred, it means that there is no keyword that matches, hence error = true!
				flag = true;
				currentToken++; //false!
			}

			if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(";"))
			{
				node = new DefaultMutableTreeNode(";");
				parent.add(node);
				currentToken++;
			}
			else if(flag == true) //we don't need this for 'While' and 'if'
			{
				error(3); //missing ';'
			}

			if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("while")) //&& flag == false) check if this part is necessary!
			{
				node = new DefaultMutableTreeNode("While");
				parent.add(node);
				error = rule_while(node);
			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("if")) //&& flag == false) check if this part is necessary!
			{
				node = new DefaultMutableTreeNode("If");
				parent.add(node);
				error = rule_if(node);
			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("switch"))
			{
				node = new DefaultMutableTreeNode("switch");
				parent.add(node);
				error = rule_switch(node);
			}
			else if (error == false && flag == false)
			{
				error(4);
			}
		}
		
		return error;
	}

	private static boolean rule_assignment(DefaultMutableTreeNode parent)
	{
		boolean error = false;
		DefaultMutableTreeNode node;
		String variableName = ""; //for ICG.
		
		//this condition is always true for this method, only then this method would have been called.
		if(currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("IDENTIFIER"))
		{
			node = new DefaultMutableTreeNode("identifier" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			//Hashtable<String, Vector<SymbolTableItem>> symbolTable = SemanticAnalyzer.getSymbolTable();
			
			Vector<SymbolTableItem> token = SemanticAnalyzer.getSymbolTable().get(tokens.get(currentToken).getWord());
			if(token != null)
			{
				SemanticAnalyzer.pushStack(token.get(0).getType()); //need to check if ever there are more than one element!
				
				//store the name only if the token exists in the symbol table!
				variableName = tokens.get(currentToken).getWord(); //for ICG
			}
			else
			{
				SemanticAnalyzer.error(gui, 4, tokens.get(currentToken).getLine(), tokens.get(currentToken).getWord());
			}
			currentToken++;
		}

		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("="))
		{
			node = new DefaultMutableTreeNode("=");// + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			currentToken++;
		}
		else
		{
			error(5);
			error = true;
			//return error;
		}
		
			node = new DefaultMutableTreeNode("Expression");
			parent.add(node);
			rule_expression(node);
			
			String typeOne = "";
			String typeTwo = "";
			if(!SemanticAnalyzer.getStack().isEmpty())
				typeOne = SemanticAnalyzer.popStack(); 
			if(!SemanticAnalyzer.getStack().isEmpty())
				typeTwo = SemanticAnalyzer.popStack();
			
			String result = SemanticAnalyzer.calculateCube(typeTwo, typeOne, "="); //because stack is LIFO!
			//String result = SemanticAnalyzer.calculateCube(typeOne, typeTwo, "="); //because stack is LIFO!
			if(result.equals("error"))
			{//error for type mismatch
				SemanticAnalyzer.error(gui, 2, tokens.get(currentToken).getLine(), tokens.get(currentToken).getWord()); //check for the line number!
			}
			else //for ICG - print the variable name and assignment ONLY IF there is no error.
			{
				CodeGenerator.addInstruction("STO", variableName, "0");
			}
			
		return error;
	}

	private static boolean rule_variable(DefaultMutableTreeNode parent) throws IOException
	{
		boolean error = true;
		DefaultMutableTreeNode node;

		if( (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("int")))
		{
			node = new DefaultMutableTreeNode("int");
			parent.add(node);
			currentToken++;
			error = false;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("float"))
		{
			node = new DefaultMutableTreeNode("float");
			parent.add(node);
			currentToken++;
			error = false;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("boolean")) 
		{
			node = new DefaultMutableTreeNode("boolean");
			parent.add(node);
			currentToken++;
			error = false;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("char"))
		{
			node = new DefaultMutableTreeNode("char");
			parent.add(node);
			currentToken++;
			error = false;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("string"))
		{
			node = new DefaultMutableTreeNode("string");
			parent.add(node);
			currentToken++;
			error = false;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("void"))
		{
			node = new DefaultMutableTreeNode("void");
			parent.add(node);
			currentToken++;
			error = false;
		}

		error = true;
		if(currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("IDENTIFIER")) 
		{
			node = new DefaultMutableTreeNode("identifier" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			boolean result =  SemanticAnalyzer.checkVariable(tokens.get(currentToken - 1).getWord(), tokens.get(currentToken).getWord());
			error = false;
			if(result == true)
			{
				SemanticAnalyzer.error(gui, 1, tokens.get(currentToken).getLine(), tokens.get(currentToken).getWord());
			}
			else //if there is no error in adding the new symbol.
			{
				CodeGenerator.addVariable(tokens.get(currentToken - 1).getWord(), tokens.get(currentToken).getWord());
			}
			currentToken++;
		}
		else
		{
			error(6); //expected identifier - removing for assignment 3 and 4.
		}

		return error;
	}

	private static boolean rule_while(DefaultMutableTreeNode parent) throws IOException
	{
		int whileCounterLocal = whileCounterGlobal;
		boolean error = true;
		DefaultMutableTreeNode node = new DefaultMutableTreeNode("keyword" + "(" + tokens.get(currentToken).getWord() + ")"); //find another way!!

		//this condition is always true for this method, only then this method would have been called.
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("while"))
		{
			node = new DefaultMutableTreeNode("while");
			parent.add(node);
			currentToken++;
			
			//e1
			whileCounterLocal++;
			CodeGenerator.addLabel("e"+whileCounterLocal, 0); //initially value is zero!
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("("))
		{
			node = new DefaultMutableTreeNode("(");
			parent.add(node);
			currentToken++;
			error = false;
		}
		else
		{
			//error(8); //removing the error for A3 and A4.
		}
		
		//this is the place where we want to move back after loop completes for one time!
		int lineNumberForStartOfLoop = CodeGenerator.getInstructions().size() + 1; 
		
		node = new DefaultMutableTreeNode("Expression");
		parent.add(node);
		error = rule_expression(node);
		
		String x = "";
		if(!SemanticAnalyzer.getStack().isEmpty())
			x = SemanticAnalyzer.popStack();
		
		if(!x.equals("boolean"))
		{
			SemanticAnalyzer.error(gui, 3, tokens.get(currentToken).getLine(), tokens.get(currentToken).getWord() );
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(")"))
		{
			node = new DefaultMutableTreeNode(")");
			parent.add(node);
			currentToken++;
			
			//add instruction - e1
			CodeGenerator.addInstruction("JMC", "#e"+whileCounterLocal, "false");
		}
		else
		{
			error(7);
			while(currentToken < tokens.size() && !tokens.get(currentToken).getWord().equals("{")) //skip all the tokens and look for first of Program
			{
				currentToken++;
			}
			if(currentToken >= tokens.size())
				return error; //if the loop goes infinite - backup
		}
		
		whileCounterLocal++; //just reserving the second variable!
		whileCounterGlobal = whileCounterLocal;
		node = new DefaultMutableTreeNode("Program");
		parent.add(node);
		error = rule_Program(node);
		
		if(tokens.get(currentToken - 1).getWord().equals("}"))//need to think if previous token wasn't '}'
		{
			
			/*//adding jump - e2.
			whileCounter++;
			CodeGenerator.addLabel("e"+whileCounter, lineNumberForStartOfLoop);*/
			
			//e2
			CodeGenerator.addLabel("e"+whileCounterLocal, lineNumberForStartOfLoop);
			//first line for while condition.
			
			//adding instruction e2
			CodeGenerator.addInstruction("JMP", "#e"+whileCounterLocal, "0");
			
			Vector<String> addedLabels = CodeGenerator.getLabel();
			for(String label : addedLabels)
			{
				if(label.contains("e"+(whileCounterLocal-1)))
				{
					addedLabels.remove(label);
					label = "#e"+(whileCounterLocal-1) + ", int, " + (CodeGenerator.getInstructions().size()+1);
					addedLabels.add(label);
					break;
				}
			}
			
			/*//whileCounter++;
			CodeGenerator.addLabel("e"+(whileCounter-2), CodeGenerator.getInstructions().size()+1);
			//whileCounter++;
			CodeGenerator.addLabel("e"+(whileCounter-1), lineNumberForStartOfLoop);//look for the name!
*/		}
		
		return error;
	}

	private static boolean rule_if(DefaultMutableTreeNode parent) throws IOException
	{
		boolean error = true;
		int ifCounterLocal = ifCounterGlobal;
		DefaultMutableTreeNode node = null;// new DefaultMutableTreeNode("keyword" + "(" + tokens.get(currentToken).getWord() + ")"); //find another way!!

		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("if"))
		{
			node = new DefaultMutableTreeNode("if");
			parent.add(node);
			currentToken++;
			error = false;
		}

		error = true;
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("("))
		{
			node = new DefaultMutableTreeNode("(");
			parent.add(node);
			currentToken++;
			error = false;
			
			ifCounterLocal++;
			CodeGenerator.addLabel("if"+ifCounterLocal, 0); //initially value is zero!
		}

		
		node = new DefaultMutableTreeNode("Expression");
		parent.add(node);
		error = rule_expression(node);

		String x = "";
		if(!SemanticAnalyzer.getStack().isEmpty())
			x = SemanticAnalyzer.popStack();

		if(!x.equals("boolean"))
		{
			SemanticAnalyzer.error(gui, 3, tokens.get(currentToken).getLine(), tokens.get(currentToken).getWord());
		}

		error = true;
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(")"))
		{
			node = new DefaultMutableTreeNode(")");
			parent.add(node);
			currentToken++;
			error = false;
			
			//adding instruction e1
			CodeGenerator.addInstruction("JMC", "#if"+ifCounterLocal, "false");
		}

		ifCounterLocal++;
		ifCounterGlobal = ifCounterLocal;
		node = new DefaultMutableTreeNode("Program");
		parent.add(node);
		error = rule_Program(node);

		
		if(tokens.get(currentToken-1).getWord().equals("}"))
		{
			//update #if1 to have the value of end of if!
			Vector<String> addedLabels = CodeGenerator.getLabel();
			for(String label : addedLabels)
			{
				if(label.contains("if"+(ifCounterLocal-1)))
				{
					addedLabels.remove(label);
					label = "#if"+(ifCounterLocal-1) + ", int, " + (CodeGenerator.getInstructions().size()+1+1); //because we have an endIf condition every time.
					addedLabels.add(label);
					break;
				}
			}
			//JMP!
			//ifCounterLocal++; - already incremented above
			CodeGenerator.addLabel("if"+ifCounterLocal, 0); //initially value zero!
			CodeGenerator.addInstruction("JMP", "#if"+ifCounterLocal, "0");
		}
		
		
		
		//else part is optional!
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("else"))
		{
			//either the current token will be '}' or 'else' - here logic if there is an else.
			
			
			node = new DefaultMutableTreeNode("else");
			parent.add(node);
			currentToken++;


			node = new DefaultMutableTreeNode("Program");
			parent.add(node);
			error = rule_Program(node);
		}

		//even if there is else or not ,we need to update the value of jump at the end of if.
		if(tokens.get(currentToken-1).getWord().equals("}"))
		{
			//update #if1 to have the value of end of if!
			Vector<String> addedLabels = CodeGenerator.getLabel();
			for(String label : addedLabels)
			{
				if(label.contains("if"+ifCounterLocal))
				{
					addedLabels.remove(label);
					label = "#if"+(ifCounterLocal) + ", int, " + (CodeGenerator.getInstructions().size()+1);
					addedLabels.add(label);
					break;
				}
			}
		}
		
		
		return error;
	}
	
	private static boolean rule_return(DefaultMutableTreeNode parent) 
	{
		boolean error = true; //remove!
		DefaultMutableTreeNode node;
		if(tokens.get(currentToken).getWord().equals("return"))
		{
			node = new DefaultMutableTreeNode("return");
			parent.add(node);
			currentToken++;
			error = false;
			CodeGenerator.addInstruction("OPR", "1", "0");
		}
		return error;
	}
	
	private static boolean rule_switch(DefaultMutableTreeNode parent) throws IOException
	{
		boolean error;
		DefaultMutableTreeNode node = null;
		int caseCounter = 0;
		Vector<String> vectorOfAllSWEndInstruction = new Vector<String>();
		String nameOfSwitchIDLocal = "";
		
		//this condition is always true for this method, only then this method would have been called.
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("switch"))
		{
			node = new DefaultMutableTreeNode("switch");
			parent.add(node);
			currentToken++;
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("("))
		{
			node = new DefaultMutableTreeNode("(");
			parent.add(node);
			currentToken++;
			
			//ICG - no need for this!!
			/*switchCounterLocal++;
			CodeGenerator.addLabel("sw"+switchCounterLocal, 0); //value zero initially.
			CodeGenerator.addInstruction("JMC", "#sw"+switchCounterLocal, "false");*/
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("IDENTIFIER"))
		{
			node = new DefaultMutableTreeNode("identifier" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			Vector<SymbolTableItem> symbol =  SemanticAnalyzer.getSymbolTable().get(tokens.get(currentToken).getWord());
			if(symbol == null)
			{
				SemanticAnalyzer.error(gui, 4, tokens.get(currentToken).getLine(), tokens.get(currentToken).getWord());
			}//semantic error - id not found!
			else
			{
				typeOfSwitchID = symbol.get(0).getType();//saving the type of switchID. (always zero!)
				nameOfSwitchIDLocal = tokens.get(currentToken).getWord();
			}
			
			currentToken++;
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(")"))
		{
			node = new DefaultMutableTreeNode(")");
			parent.add(node);
			currentToken++;
			//error missing!
		} else { error(8);  }//missing ')' 
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("{"))
		{
			node = new DefaultMutableTreeNode("{");
			parent.add(node);
			currentToken++;
			//error missing!
		}else {error (1); }//missing '{'
		
		while(currentToken < tokens.size() && (!tokens.get(currentToken).getWord().equals("}") && !tokens.get(currentToken).getWord().equals("default")))
		{
			caseCounter++;
			node = new DefaultMutableTreeNode("Case"); 
			parent.add(node);
			
			nameOfSwitchID = nameOfSwitchIDLocal;
			error = rule_cases(node); //check for type of case number and switch id and report semantic error.
		    //nameOfSwitchIDLocal = nameOfSwitchID;
			
		    //ICG - at the end of every case - jump to end of switch
			switchCaseCounterLocal++;
			CodeGenerator.addLabel("swc"+switchCaseCounterLocal, 0); //label 2!
			CodeGenerator.addInstruction("JMP", "#swc"+switchCaseCounterLocal /*add counterNumber*/, "0"); //update value of swc to go to end of switch
			vectorOfAllSWEndInstruction.add("swc"+switchCaseCounterLocal);
		}
		
		
		//optional!
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("default"))
		{
			caseCounter++;
			node = new DefaultMutableTreeNode("Default");
			parent.add(node);
			error = rule_default(node);
			//error missing!
		}
		
		if(caseCounter == 0)
		{
			error(10); //syntactic error - no case or default inside switch.
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("}"))
		{
			
			for(String label : vectorOfAllSWEndInstruction)
			{
				for(String labelsInVector : CodeGenerator.getLabel())
				{
					if(labelsInVector.contains(label))
					{
						CodeGenerator.getLabel().remove(labelsInVector);
						CodeGenerator.addLabel(label, CodeGenerator.getInstructions().size()+1);
						break;
					}
				}
			}
			
			node = new DefaultMutableTreeNode("}");
			parent.add(node);
			currentToken++;
			//error missing!
		}else {error (2); } //missing '}' 
		
		
		error = true;
		return error;
	}
	
	private static boolean rule_cases(DefaultMutableTreeNode parent) throws IOException
	{
		
		boolean error = false;
		DefaultMutableTreeNode node = null;
		int switchCaseCounter = switchCaseCounterLocal;
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("case"))
		{
			node = new DefaultMutableTreeNode("case");
			parent.add(node);
			currentToken++;
			//error missing!
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("INTEGER") || currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("HEXADECIMAL") || currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("BINARY") || currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("OCTAL"))
		{
			node = new DefaultMutableTreeNode("(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			
			//IDG
			CodeGenerator.addInstruction("LOD", nameOfSwitchID, "0");
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			CodeGenerator.addInstruction("OPR", "15", "0");
			
			switchCaseCounter++;
			CodeGenerator.addLabel("swc"+switchCaseCounter, 0); //initially value is zero.
			CodeGenerator.addInstruction("JMC", "#swc"+switchCaseCounter, "false"); //label 1! - value of variable should be set to start of next case.
			
			currentToken++;
			//error missing!
		}
	
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(":"))
		{
			node = new DefaultMutableTreeNode(":");
			parent.add(node);
			currentToken++;
			//error missing!
		}
		switchCaseCounterLocal = switchCaseCounter;
		node = new DefaultMutableTreeNode("Program");
		parent.add(node);
		error = rule_Program(node);
		
		
		//ICG
		if(currentToken < tokens.size() && tokens.get(currentToken-1).getWord().equals("}"))
		{
			Vector<String> addedLabels = CodeGenerator.getLabel();
			for(String label : addedLabels)
			{
				if(label.contains("swc"+switchCaseCounter))
				{
					addedLabels.remove(label);
					label = "#swc"+switchCaseCounter + ", int, " + (CodeGenerator.getInstructions().size()+2); //because JMP is to be added after case is over!
					addedLabels.add(label);
					break;
				}
			}
		}
		return error;
	}
	
	private static boolean rule_default(DefaultMutableTreeNode parent) throws IOException
	{
		boolean error = false;
		DefaultMutableTreeNode node = null;
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("default"))
		{
			node = new DefaultMutableTreeNode("default");
			parent.add(node);
			currentToken++;
			//error missing!
		}
		
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(":"))
		{
			node = new DefaultMutableTreeNode(":");
			parent.add(node);
			currentToken++;
			//error missing!
		}
		
		node = new DefaultMutableTreeNode("Program");
		parent.add(node);
		error = rule_Program(node);
		
		return error;
	}
	
	private static boolean rule_print(DefaultMutableTreeNode parent)
	{
		boolean error = true;
		DefaultMutableTreeNode node;
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("print")) //if_print
		{
			node = new DefaultMutableTreeNode("print");
			parent.add(node);
			currentToken++;
		}

		if(tokens.get(currentToken).getWord().equals("(")) //check for this, if should be out of if_print.
		{
			node = new DefaultMutableTreeNode("(");
			parent.add(node);
			currentToken++;
			node = new DefaultMutableTreeNode("Expression");
			parent.add(node);
			error = rule_expression(node);
		}
		else
		{
			error(8);
		}

		error = true;
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(")"))
		{
			node = new DefaultMutableTreeNode(")");
			parent.add(node);
			currentToken++;
			error = false;

			//ICG
			CodeGenerator.addInstruction("OPR", "21", "0");
		}
		else
		{
			error(7);
		}
		return error;

	}
	
	private static boolean rule_expression(DefaultMutableTreeNode parent) 
	{
		boolean error = true;
		DefaultMutableTreeNode node;

		node = new DefaultMutableTreeNode("X");
		parent.add(node);
		error = rule_X(node);

		while (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("|"))
		{
			node = new DefaultMutableTreeNode("|");
			parent.add(node);
			currentToken++;
			node = new DefaultMutableTreeNode("X");
			parent.add(node);
			error = rule_X(node);

			String x = "";
			String y = "";
			if(!SemanticAnalyzer.getStack().isEmpty())
				x = SemanticAnalyzer.popStack();
			if(!SemanticAnalyzer.getStack().isEmpty())
				y = SemanticAnalyzer.popStack();

			String result = SemanticAnalyzer.calculateCube(x, y, "|");
			SemanticAnalyzer.pushStack(result);

			//ICG - if there is no semantic error, only then produce IC.
			//If there is no error, it implies that we are here twice - hence no need to check "ifTwice" here!
			if(!result.equals("error"))
			{
				CodeGenerator.addInstruction("OPR", "8", "0");
			}
		}

		return error;
	}
	
	private static boolean rule_X(DefaultMutableTreeNode parent) 
	{
		//int operatorUsed = 0;
		boolean error = true;
		DefaultMutableTreeNode node;
		node = new DefaultMutableTreeNode("Y");
		parent.add(node);
		error = rule_Y(node);
		while (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("&"))
		{
			//operatorUsed++;
			node = new DefaultMutableTreeNode("&");
			parent.add(node);
			currentToken++;
			node = new DefaultMutableTreeNode("Y");
			parent.add(node);
			error = rule_Y(node);


			String x = ""; String y = "";
			if(!SemanticAnalyzer.getStack().isEmpty())
				x = SemanticAnalyzer.popStack();
			if(!SemanticAnalyzer.getStack().isEmpty())
				y = SemanticAnalyzer.popStack();

			String result = SemanticAnalyzer.calculateCube(x, y, "&");
			SemanticAnalyzer.pushStack(result);

			//ICG - explanation in rule_expression
			if(!result.equals("error"))
			{
				CodeGenerator.addInstruction("OPR", "9", "0");
			}
		}

		return error;	  
	}
	
	private static boolean rule_Y(DefaultMutableTreeNode parent) 
	{
		boolean operatorUsed = false;
		boolean error = true;
		DefaultMutableTreeNode node;
		if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("!"))
		{
			operatorUsed = true;
			node = new DefaultMutableTreeNode("!");
			parent.add(node);
			currentToken++;
		}
		node = new DefaultMutableTreeNode("R");
		parent.add(node);
		error = rule_R(node);

		if(operatorUsed == true)
		{
			String x = "";
			if(!SemanticAnalyzer.getStack().isEmpty())
				x = SemanticAnalyzer.popStack();

			String result = SemanticAnalyzer.calculateCube(x, "!");
			SemanticAnalyzer.pushStack(result);
			operatorUsed = false;

			//ICG
			if(!result.equals("error"))
			{
				CodeGenerator.addInstruction("OPR", "10", "0");
			}
		}
		return error;
	}
	
	private static boolean rule_R(DefaultMutableTreeNode parent) 
	{
		//int notEq = 0; int eqEq = 0; int lt = 0; int gt = 0;
		boolean error = true;
		DefaultMutableTreeNode node;
		node = new DefaultMutableTreeNode("E");
		parent.add(node);
		error = rule_E(node);
		while ((currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("!=")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("==") || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(">"))|| (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("<"))))                
		{
			String x = "";
			String y = "";

			if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("!="))
			{
				//notEq++;
				node = new DefaultMutableTreeNode("!=");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("E");
				parent.add(node);
				error = rule_E(node);


				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();
				String result = SemanticAnalyzer.calculateCube(x, y, "!=");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error")){
					CodeGenerator.addInstruction("OPR", "16", "0"); //16 - for not equal
				}

			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("=="))
			{
				//eqEq++;
				node = new DefaultMutableTreeNode("==");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("E");
				parent.add(node);
				error = rule_E(node);

				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();
				String result = SemanticAnalyzer.calculateCube(x, y, "==");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error")){
					CodeGenerator.addInstruction("OPR", "15", "0"); //15 - for equal
				}

			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("<"))
			{
				//lt++;
				node = new DefaultMutableTreeNode("<");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("E");
				parent.add(node);
				error = rule_E(node);

				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();
				String result = SemanticAnalyzer.calculateCube(x, y, "<");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error")){
					CodeGenerator.addInstruction("OPR", "12", "0"); //12 - less than
				}
			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(">"))
			{
				//gt++;
				node = new DefaultMutableTreeNode(">");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("E");
				parent.add(node);
				error = rule_E(node);

				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();
				String result = SemanticAnalyzer.calculateCube(x, y, ">");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error")){
					CodeGenerator.addInstruction("OPR", "11", "0"); //11 - greater than
				}
			}
		}


		return error;
	}
	
	private static boolean rule_E(DefaultMutableTreeNode parent) 
	{
		boolean error = true;
		/*int countPlus = 0;
		int countMinus = 0;*/
		DefaultMutableTreeNode node;
		node = new DefaultMutableTreeNode("A");
		parent.add(node);
		error = rule_A(node);
		while ((currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("+")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("-")))                
		{
			if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("+"))
			{
				//countPlus++;
				node = new DefaultMutableTreeNode("+");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("A");
				parent.add(node);
				error = rule_A(node);


				String x = "";
				String y = "";
				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();
				String result = SemanticAnalyzer.calculateCube(x, y, "+");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error"))
				{
					CodeGenerator.addInstruction("OPR", "2", "0"); //2 - add
				}
			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("-"))
			{
				//countMinus++;
				node = new DefaultMutableTreeNode("-");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("A");
				parent.add(node);
				error = rule_A(node);

				String x = "";
				String y = "";
				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();
				String result = SemanticAnalyzer.calculateCube(x, y, "-");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error"))
				{
					CodeGenerator.addInstruction("OPR", "3", "0"); //3 - subtract
				}

			}

		}

		return error;
	}
	
	private static boolean rule_A(DefaultMutableTreeNode parent) 
	{
		boolean error = true;
		DefaultMutableTreeNode node;
		node = new DefaultMutableTreeNode("B");
		parent.add(node);
		error = rule_B(node);

		while ((currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("/")) || (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("*")))                
		{
			String x = "";
			String y = "";

			if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("/"))
			{

				node = new DefaultMutableTreeNode("/");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("B");
				parent.add(node);
				error = rule_B(node);

				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();

				String result = SemanticAnalyzer.calculateCube(x, y, "/");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error"))
				{
					CodeGenerator.addInstruction("OPR", "5", "0"); //5 - divide
				}
			}
			else if(currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("*"))
			{
				//countMult++; //to check if the execution was here twice for multiplication.
				node = new DefaultMutableTreeNode("*");
				parent.add(node);
				currentToken++;
				node = new DefaultMutableTreeNode("B");
				parent.add(node);
				error = rule_B(node);


				if(!SemanticAnalyzer.getStack().isEmpty())
					x = SemanticAnalyzer.popStack();
				if(!SemanticAnalyzer.getStack().isEmpty())
					y = SemanticAnalyzer.popStack();

				String result = SemanticAnalyzer.calculateCube(x, y, "*");
				SemanticAnalyzer.pushStack(result);

				//ICG
				if(!result.equals("error"))
				{
					CodeGenerator.addInstruction("OPR", "4", "0"); //4 - multiply
				}
			}
		}

		return error;
	}
	
	private static boolean rule_B(DefaultMutableTreeNode parent) {
		boolean error;
		boolean operatorUsed = false;
		DefaultMutableTreeNode node;
		if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("-")) {
			node = new DefaultMutableTreeNode("-");
			parent.add(node);
			operatorUsed = true;
			currentToken++;

			//ICG - because we don't have a unary minus; hence we subtract the number from zero: hence, add literal zero in stack...
			CodeGenerator.addInstruction("LIT", "0", "0");
		}
		node = new DefaultMutableTreeNode("C");
		parent.add(node);
		error = rule_C(node);
		if(operatorUsed == true)
		{
			String x = SemanticAnalyzer.popStack();
			String result = SemanticAnalyzer.calculateCube(x, "-");
			SemanticAnalyzer.pushStack(result);


			//ICG - subtract the number from zero to make it negative.
			if(!result.equals("error"))
			{
				CodeGenerator.addInstruction("OPR", "3", "0"); //3 - subtract.
			}
		}

		return error;
	}
	
	private static boolean rule_C(DefaultMutableTreeNode parent) {
		boolean error = false;
		DefaultMutableTreeNode node;
		if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("INTEGER")) 
		{
			node = new DefaultMutableTreeNode("integer" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			SemanticAnalyzer.pushStack("int"); //pushing the type 'int' in the stack! 
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("OCTAL")) 
		{
			node = new DefaultMutableTreeNode("octal" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			SemanticAnalyzer.pushStack("int"); //pushing type as int. 
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("HEXADECIMAL")) 
		{
			node = new DefaultMutableTreeNode("hexadecimal" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			SemanticAnalyzer.pushStack("int"); //pushing type as int.
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("BINARY")) 
		{
			node = new DefaultMutableTreeNode("binary" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			SemanticAnalyzer.pushStack("int"); //pushing type as int.
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("true")) 
		{
			node = new DefaultMutableTreeNode("true");
			parent.add(node);
			SemanticAnalyzer.pushStack("boolean"); //pushing type as boolean.
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("false")) 
		{
			node = new DefaultMutableTreeNode("false");
			parent.add(node);
			SemanticAnalyzer.pushStack("boolean");
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("STRING")) 
		{
			node = new DefaultMutableTreeNode("string" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			SemanticAnalyzer.pushStack("string");
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord().replace("\"", ""), "0"); //look for the output!
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("CHARACTER")) 
		{
			node = new DefaultMutableTreeNode("char" + "(" + tokens.get(currentToken).getWord().replace("\'", "") + ")");
			parent.add(node);
			SemanticAnalyzer.pushStack("char"); 
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("FLOAT")) 
		{
			node = new DefaultMutableTreeNode("float" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);
			SemanticAnalyzer.pushStack("float");
			CodeGenerator.addInstruction("LIT", tokens.get(currentToken).getWord(), "0");
			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getToken().equals("IDENTIFIER")) {
			node = new DefaultMutableTreeNode("identifier" + "(" + tokens.get(currentToken).getWord() + ")");
			parent.add(node);

			Vector<SymbolTableItem> token = SemanticAnalyzer.getSymbolTable().get(tokens.get(currentToken).getWord());
			if(token != null)
			{
				SemanticAnalyzer.pushStack(token.get(0).getType()); //need to check if ever there are more than one element!

				//ICG - add only if the expression is semantically secured.
				CodeGenerator.addInstruction("LOD", tokens.get(currentToken).getWord(), "0");
			}
			else
			{
				SemanticAnalyzer.error(gui, 4 ,tokens.get(currentToken).getLine(), tokens.get(currentToken).getWord()); //variable id not defined!
			}

			currentToken++;
		}
		else if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals("(")) {
			node = new DefaultMutableTreeNode("(");
			parent.add(node);
			currentToken++;
			//
			node = new DefaultMutableTreeNode("Expression");
			parent.add(node);
			error = rule_expression(node);


			if (currentToken < tokens.size() && tokens.get(currentToken).getWord().equals(")")) 
			{
				node = new DefaultMutableTreeNode(")");
				parent.add(node);
				currentToken++;
				error = false;
			}
			else
				error(7);
		}
		else// if (!tokens.get(currentToken).getWord().equals(";"))
		{
			error(9);
		}

		return error;
	}
	
	public static void error(int err) {
		int n = 0;
		if(currentToken < tokens.size())
			n = tokens.get(currentToken).getLine();
		else
			n = tokens.get(currentToken - 1).getLine() + 1;

		//ERROR RECOVERY
		switch (err) {
		case 1: 
			gui.writeConsole("Line " + n + ": expected {");
			break;
		case 2: 
			gui.writeConsole("Line " + n + ": expected }"); 
			break;
		case 3: 
			n = tokens.get(currentToken-1).getLine(); //change because the token was already incremented!
			gui.writeConsole("Line " + n + ": expected ;"); break;
		case 4: 
			gui.writeConsole("Line " + n +": expected identifier or keyword"); 
			break;
		case 5: 
			gui.writeConsole("Line " + n +": expected ="); 
			break;
		case 6: 
			gui.writeConsole("Line " + n +": expected identifier"); 
			break;
		case 7: 
			gui.writeConsole("Line " + n +": expected )"); 
			break;
		case 8: 
			gui.writeConsole("Line " + n +": expected ("); 
			break;
		case 9: gui.writeConsole("Line " + n +": expected value, identifier, ("); break;
		case 10: gui.writeConsole("Line " + n +": expected case or default"); break;
		}
	}

}

//ICG - Intermediate code generation