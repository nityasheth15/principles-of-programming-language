package A4;

import java.util.Vector;

public class CodeGenerator {
  
  private static Vector<String> variables = new Vector<>();  
  private static Vector<String> labels = new Vector<>();  
  private static Vector<String> instructions = new Vector<>();

  static void addInstruction(String instruction, String p1, String p2) {
    instructions.add(instruction + " " + p1 + ", " + p2);
  }

  
  public static void reset()
  {
	  variables = new Vector<>();
	  labels = new Vector<>(); 
	  instructions = new Vector<>();
  }
  
  public static Vector<String> getLabel()
  {
	  return labels;
  }
  
  public static Vector<String> getInstructions()
  {
	  return instructions;
  }
  
  static void addLabel(String name, int value) {
    labels.add("#"+name + ", int, " + value);
  }
    
  static void addVariable(String type, String name) {
    variables.add(name + ", " + type + ", global, 0" );
  }

  static void writeCode(Gui gui) {
    for (String variable : variables) {
      gui.writeCode(variable);    
    }
    for (String label : labels) {
      gui.writeCode(label);    
    }
    gui.writeCode("@");
    for (String instruction : instructions) {
      gui.writeCode(instruction);    
    }

  }
  
  static void clear(Gui gui) {
    variables.clear();
    instructions.clear();
  }  
}
