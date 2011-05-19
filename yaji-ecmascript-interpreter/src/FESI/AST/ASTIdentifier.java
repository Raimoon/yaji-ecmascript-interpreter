/* Generated By:JJTree: Do not edit this line. ASTIdentifier.java */

package FESI.AST;

import FESI.Data.ESString;
import FESI.Parser.EcmaScript;

public class ASTIdentifier extends SimpleNode {
    private static final long serialVersionUID = 2673088409445831343L;
    private ESString identifierName = null;
    private int hash = 0;

    public ASTIdentifier(int id) {
        super(id);
    }

    public ASTIdentifier(EcmaScript p, int id) {
        super(p, id);
    }

    public static Node jjtCreate(int id) {
        return new ASTIdentifier(id);
    }

    public static Node jjtCreate(EcmaScript p, int id) {
        return new ASTIdentifier(p, id);
    }

    /** Accept the visitor. **/
    @Override
    public Object jjtAccept(EcmaScriptVisitor visitor, Object data) {
        return visitor.visit(this, data);
    }

    // JMCL
    public void setName(String identifierName) {
        this.identifierName = ESString.valueOf(identifierName.intern()); // to
                                                                         // lower
                                                                         // number
                                                                         // of
                                                                         // strings
        this.hash = identifierName.hashCode();
    }

    @Override
    public int hashCode() {
        return hash;
    }

    public String getName() {
        return identifierName.toString();
    }

    public ESString getESName() {
        return identifierName;
    }

    @Override
    public String toString() {
        return "<" + identifierName + ">";
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ASTIdentifier) {
            ASTIdentifier other = (ASTIdentifier) obj;
            return this.identifierName.equals(other.identifierName);
        }
        return false;
    }
}
