/* Generated By:JJTree: Do not edit this line. ASTProgram.java */

package FESI.AST;

import FESI.Parser.EcmaScript;

public class ASTProgram extends SimpleNode {
  /**
	 * 
	 */
	private static final long serialVersionUID = 1395362606410066353L;

public ASTProgram(int id) {
    super(id);
  }

  public ASTProgram(EcmaScript p, int id) {
    super(p, id);
  }

  public static Node jjtCreate(int id) {
      return new ASTProgram(id);
  }

  public static Node jjtCreate(EcmaScript p, int id) {
      return new ASTProgram(p, id);
  }

  /** Accept the visitor. **/
  public Object jjtAccept(EcmaScriptVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}