package A4;


import java.io.IOException;
import java.util.Hashtable;
import java.util.Stack;
import java.util.Vector;

import A4.Gui;

/*import com.sun.java_cup.internal.runtime.Symbol;
import com.sun.org.apache.xerces.internal.util.SymbolTable;*/

public class SemanticAnalyzer {
  
  private static Hashtable<String, Vector<SymbolTableItem>> symbolTable = new Hashtable<String, Vector<SymbolTableItem>>();
  private static final Stack<String> stack = new Stack<String>();
  
  public static boolean SEMANTICERROR = false;
  
  private static final int INT 			= 0;
  private static final int FLOAT 		= 1;
  private static final int CHAR			= 2;
  private static final int STRING 		= 3;
  private static final int BOOL 		= 4;
  private static final int VOID 		= 5;
  private static final int ERROR 		= 6;
  
  private static final int OPE_MINUS	= 0; //-
  private static final int OPE_MULT		= 1; //*
  private static final int OPE_DIV		= 2; // -> /
  private static final int OPE_PLUS		= 3; //+
  private static final int U_MINUS		= 4; //unary minus
  private static final int OPE_GT		= 5; //>
  private static final int OPE_LT		= 6; //<
  private static final int OPE_NOTEQ	= 7;//!=
  private static final int OPE_EQEQ		= 8;//==
  private static final int OPE_AND		= 9;//&
  private static final int OPE_OR 		= 10;//|
  private static final int U_NOT		= 11;//unary !
  private static final int OPE_EQ		= 12; //=
  
  
  // create here a data structure for the cube of types
  private static final int [][][] dataTypeCube = {
		  { //OPERATOR - (binary) -> 0
			  {INT, FLOAT, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {FLOAT, FLOAT, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR * -> 1
			  {INT, FLOAT, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {FLOAT, FLOAT, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR / -> 2
			  {INT, FLOAT, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {FLOAT, FLOAT, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  { //OPERATOR + -> 3
			  {INT, FLOAT, ERROR, STRING, ERROR, ERROR, ERROR},
			  {FLOAT, FLOAT, ERROR, STRING, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, STRING, ERROR, ERROR, ERROR},
			  {STRING, STRING, STRING, STRING, STRING, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, STRING, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR UNARY - -> 4
			  {INT, FLOAT, ERROR, STRING, ERROR, ERROR, ERROR},
		  },
		  {//OPERATOR >  -> 5
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR <  -> 6
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR !=  -> 7
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, BOOL, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, BOOL, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, BOOL, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR  ==   -> 8
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {BOOL, BOOL, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, BOOL, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, BOOL, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, BOOL, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR  &  -> 9
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, BOOL, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR |   -> 10
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, BOOL, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  },
		  {//OPERATOR !  -> 11
			  {ERROR, ERROR, ERROR, ERROR, BOOL, ERROR, ERROR}
		  },
		  {//OPERATOR =   -> 12   [NEED TO CHECK]
			  {INT, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR},
			  {FLOAT, FLOAT, ERROR, ERROR, ERROR, ERROR, ERROR,},
			  {ERROR, ERROR, CHAR, ERROR, ERROR, ERROR, ERROR},
			  {ERROR, ERROR, ERROR, STRING, ERROR, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, BOOL, ERROR, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, VOID, ERROR}, 
			  {ERROR, ERROR, ERROR, ERROR, ERROR, ERROR, ERROR}
		  }
  };
  
  public static Stack<String> getStack()
  {
	  return stack;
  }
  
  public static Hashtable<String, Vector<SymbolTableItem>> getSymbolTable() {
    return symbolTable;
  }
  
  public static void setSymbolTable(Hashtable<String, Vector<SymbolTableItem>> ST)
  {
	  symbolTable = ST;
  }
  
  public static boolean checkVariable(String type, String id) throws IOException {
   
   
    // A. search the id in the symbol table
	  Vector<SymbolTableItem> tempVector = symbolTable.get(id);

    // B. if !exist then insert: type, scope=global, value={0, false, "", '')
	  if(tempVector == null)
	  {
		  Vector<SymbolTableItem> v = new Vector<SymbolTableItem>();
		  v.add(new SymbolTableItem(type,"global", ""));
		  symbolTable.put(id, v);
		  return false;
	  }//C. else error: “variable id is already defined”
	  else
	  {
		  return true; //changed the structure, error will be displayed as per the boolean value from Parser.java!
		  //error(new Gui("Yo"), 1, 1);//check for GUI 
	  }
  }

  public static void pushStack(String type) {
  
    // push type in the stack
	  stack.push(type);
  }
  
  public static String popStack() {
    String result="";
    // pop a value from the stack
    result = stack.pop();
    return result;
  }
  
  private static int getTypeNumber(String typeInString)
  {
	  switch(typeInString)
	  {
	  case "int":
		  return INT;
	  case "float":
		  return FLOAT;
	  case "char":
		  return CHAR;
	 /* case "String": //need to confirm for this one.
		  return STRING;*/
	  case "string":
		  return STRING;
	  case "boolean":
		  return BOOL;
	  case "void":
		  return VOID;
	  default:
		  return ERROR;

	  }
  }
  
  private static String getTypeName(int type)
  {
	  switch(type)
	  {
	  case INT:
		  return "int";
	  case FLOAT:
		  return "float";
	  case CHAR:
		  return "char";
	  case STRING:
		  return "string";
	  case BOOL:
		  return "boolean";
	  case VOID:
		  return "void";
	  default:
	  	return "error";
	  }
  }
  
  public static String calculateCube(String type, String operator) {
    String result="";
    // unary operator ( - and !)
    int typeOne = getTypeNumber(type);
    if(operator.equals("-"))//unary minus
    {
    	int typeNumber = dataTypeCube[U_MINUS][0][typeOne];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("!"))//unary Not
    {
    	int typeNumber = dataTypeCube[U_NOT][0][typeOne];
    	result = getTypeName(typeNumber);
    } 
    else
    	result = "error";
    return result;
  }
  
  public static String calculateCube(String type1, String type2, String operator) {
    String result="";
    // binary operator ( - and !)
   int typeOne = getTypeNumber(type1);
   int typeTwo = getTypeNumber(type2);
   
    if(operator.equals("-"))//binary minus
    {
    	int typeNumber = dataTypeCube[OPE_MINUS][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("*"))//multiply
    {
    	int typeNumber = dataTypeCube[OPE_MULT][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("/"))//divide
    {
    	int typeNumber = dataTypeCube[OPE_DIV][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("+"))//plus
    {
    	int typeNumber = dataTypeCube[OPE_PLUS][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals(">"))//greater than
    {
    	int typeNumber = dataTypeCube[OPE_GT][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("<"))//less than
    {
    	int typeNumber = dataTypeCube[OPE_LT][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("!="))//!= not equal
    {
    	int typeNumber = dataTypeCube[OPE_NOTEQ][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("=="))//equal equal
    {
    	int typeNumber = dataTypeCube[OPE_EQEQ][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("&"))//and
    {
    	int typeNumber = dataTypeCube[OPE_AND][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("|"))//or
    {
    	int typeNumber = dataTypeCube[OPE_OR][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    else if(operator.equals("="))//equal =
    {
    	int typeNumber = dataTypeCube[OPE_EQ][typeOne][typeTwo];
    	result = getTypeName(typeNumber);
    }
    
    return result;
  }
  
  public static void error(Gui gui, int err, int n, String info) {
	   SEMANTICERROR = true; 
	   switch (err) {
	      case 1: 
	        gui.writeConsole("Line" + n + ": variable <" + info +"> id is already defined"); 
	        break;
	      case 2: 
	        gui.writeConsole("Line" + n + ": incompatible types: type mismatch "); 
	        break;
	      case 3: 
	        gui.writeConsole("Line" + n + ": incompatible types: expected boolean"); 
	        break;
	      case 4://newly added.
	    	  gui.writeConsole("Line" + n + ": variable <"+ info +"> not found "); 
	          break;
	    }
	  }
  
  
}

