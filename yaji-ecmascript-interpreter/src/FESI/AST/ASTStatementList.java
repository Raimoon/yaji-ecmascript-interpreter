/* Generated By:JJTree: Do not edit this line. ASTStatementList.java */

package FESI.AST;

import FESI.Parser.EcmaScript;

public class ASTStatementList extends SimpleNode {
  /**
	 * 
	 */
	private static final long serialVersionUID = 1564671250651384378L;

public ASTStatementList(int id) {
    super(id);
  }

  public ASTStatementList(EcmaScript p, int id) {
    super(p, id);
  }

  public static Node jjtCreate(int id) {
      return new ASTStatementList(id);
  }

  public static Node jjtCreate(EcmaScript p, int id) {
      return new ASTStatementList(p, id);
  }

  /** Accept the visitor. **/
  public Object jjtAccept(EcmaScriptVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}