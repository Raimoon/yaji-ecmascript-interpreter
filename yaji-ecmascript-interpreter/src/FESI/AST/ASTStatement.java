/* Generated By:JJTree: Do not edit this line. ASTStatement.java */

package FESI.AST;

import FESI.Parser.EcmaScript;

public class ASTStatement extends SimpleNode {
    /**
	 * 
	 */
    private static final long serialVersionUID = -847892398470193741L;

    public ASTStatement(int id) {
        super(id);
    }

    public ASTStatement(EcmaScript p, int id) {
        super(p, id);
    }

    public static Node jjtCreate(int id) {
        return new ASTStatement(id);
    }

    public static Node jjtCreate(EcmaScript p, int id) {
        return new ASTStatement(p, id);
    }

    /** Accept the visitor. **/
    public Object jjtAccept(EcmaScriptVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}