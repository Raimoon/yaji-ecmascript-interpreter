/* Generated By:JJTree: Do not edit this line. ASTAndExpressionSequence.java */

package FESI.AST;

import FESI.Parser.EcmaScript;

public class ASTAndExpressionSequence extends SimpleNode {
    /**
	 * 
	 */
    private static final long serialVersionUID = -3249882484364964195L;

    public ASTAndExpressionSequence(int id) {
        super(id);
    }

    public ASTAndExpressionSequence(EcmaScript p, int id) {
        super(p, id);
    }

    public static Node jjtCreate(int id) {
        return new ASTAndExpressionSequence(id);
    }

    public static Node jjtCreate(EcmaScript p, int id) {
        return new ASTAndExpressionSequence(p, id);
    }

    /** Accept the visitor. **/
    public Object jjtAccept(EcmaScriptVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }
}