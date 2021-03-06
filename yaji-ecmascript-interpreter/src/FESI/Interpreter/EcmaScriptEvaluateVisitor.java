// EcmaScriptEvaluateVisitor.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package FESI.Interpreter;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import FESI.AST.ASTAllocationExpression;
import FESI.AST.ASTAndExpressionSequence;
import FESI.AST.ASTArrayLiteral;
import FESI.AST.ASTAssignmentExpression;
import FESI.AST.ASTBinaryExpressionSequence;
import FESI.AST.ASTBreakStatement;
import FESI.AST.ASTCaseClause;
import FESI.AST.ASTCatch;
import FESI.AST.ASTCompositeReference;
import FESI.AST.ASTConditionalExpression;
import FESI.AST.ASTContinueStatement;
import FESI.AST.ASTDefaultClause;
import FESI.AST.ASTDoWhileStatement;
import FESI.AST.ASTElision;
import FESI.AST.ASTEmptyExpression;
import FESI.AST.ASTExpressionList;
import FESI.AST.ASTFinally;
import FESI.AST.ASTForInStatement;
import FESI.AST.ASTForStatement;
import FESI.AST.ASTForVarInStatement;
import FESI.AST.ASTForVarStatement;
import FESI.AST.ASTFormalParameterList;
import FESI.AST.ASTFunctionCallParameters;
import FESI.AST.ASTFunctionDeclaration;
import FESI.AST.ASTFunctionExpression;
import FESI.AST.ASTGetAccessor;
import FESI.AST.ASTIdentifier;
import FESI.AST.ASTIfStatement;
import FESI.AST.ASTLiteral;
import FESI.AST.ASTObjectLiteral;
import FESI.AST.ASTOperator;
import FESI.AST.ASTOrExpressionSequence;
import FESI.AST.ASTPostfixExpression;
import FESI.AST.ASTProgram;
import FESI.AST.ASTPropertyIdentifierReference;
import FESI.AST.ASTPropertyNameAndValue;
import FESI.AST.ASTPropertyValueReference;
import FESI.AST.ASTRegexp;
import FESI.AST.ASTReturnStatement;
import FESI.AST.ASTSetAccessor;
import FESI.AST.ASTStatement;
import FESI.AST.ASTStatementList;
import FESI.AST.ASTSuperReference;
import FESI.AST.ASTSwitchStatement;
import FESI.AST.ASTThisReference;
import FESI.AST.ASTThrowStatement;
import FESI.AST.ASTTryStatement;
import FESI.AST.ASTUnaryExpression;
import FESI.AST.ASTVariableDeclaration;
import FESI.AST.ASTWhileStatement;
import FESI.AST.ASTWithStatement;
import FESI.AST.AbstractEcmaScriptVisitor;
import FESI.AST.Node;
import FESI.AST.SimpleNode;
import FESI.Data.ConstructedFunctionObject;
import FESI.Data.ESAppendable;
import FESI.Data.ESArguments;
import FESI.Data.ESBoolean;
import FESI.Data.ESLoader;
import FESI.Data.ESNull;
import FESI.Data.ESNumber;
import FESI.Data.ESObject;
import FESI.Data.ESReference;
import FESI.Data.ESString;
import FESI.Data.ESUndefined;
import FESI.Data.ESValue;
import FESI.Data.ObjectObject;
import FESI.Data.RegExpPrototype;
import FESI.Data.StandardProperty;
import FESI.Exceptions.EcmaScriptException;
import FESI.Exceptions.ProgrammingError;
import FESI.Exceptions.ReferenceError;
import FESI.Exceptions.SyntaxError;
import FESI.Exceptions.TypeError;
import FESI.Interpreter.Evaluator.EvaluationResult;
import FESI.Parser.EcmaScriptConstants;
import FESI.Parser.StrictMode;
import FESI.Util.IAppendable;

/**
 * The evaluate visitor use the visitor pattern to evaluate the parsed code. Use
 * for both main program and functions (using different entries). A new
 * evaluator is used everytime as it contains the return code (because we can
 * return only one variable).
 * <P>
 * The parse tree must have been preprocessed by the variable and function
 * visitors first.
 */
public class EcmaScriptEvaluateVisitor extends AbstractEcmaScriptVisitor implements EcmaScriptConstants {

    // Continuation codes
    public static final int C_NORMAL = 0;
    public static final int C_RETURN = 1;
    public static final int C_BREAK = 2;
    public static final int C_CONTINUE = 3;

    public static final String REPRESENTATION_RESULT_NAME = "result";

    private boolean debug = false;

    // Indicator for the data to be returned by the accept
    static final Object FOR_VALUE = new Object();
    private static final Object FOR_REFERENCE = new Object();

    // The visitor work on behalf on an evaluator which provide
    // the context information (as global variables)
    private Evaluator evaluator;

    // This is the final completion code - mark unused to protect against
    // recursive use
    protected int completionCode = -1;

    private boolean useRepresentationOptimisation = false;
    private IAppendable representationOutputBuffer = null;
    private EvaluationSource evaluationSource;
    private String targetLabel;
    
    /**
     * Create a new visitor
     * 
     * @param evaluator
     *            On behalf of this evaluator
     */
    public EcmaScriptEvaluateVisitor(Evaluator evaluator) {
        super();
        this.evaluator = evaluator;
    }

    /**
     * Return the completion code of this evaluation
     * 
     * @return the last completion code as an int
     */
    public int getCompletionCode() {
        return completionCode;
    }

    /**
     * Return the completion code of this evaluation
     * 
     * @return the last completion code as a String
     */
    public String getCompletionCodeString() {
        final String[] data = { "normal", "return", "break", "continue" };
        return data[completionCode];
    }

    /**
     * Used to enable string concatenation optimisations when executing a
     * representation
     * 
     * @param value
     *            true to enable optimisation
     */
    public void setRepresentationOptimisation(boolean value,
            IAppendable markupHolder) {
        useRepresentationOptimisation = value;
        representationOutputBuffer = markupHolder;
    }

    /**
     * Evaluate a tree which represents a main program or the source of an eval
     * function.
     * 
     * @param node
     *            The parsed tree (annotated for variables)
     * @param es
     *            A description of the source for error messages
     * @return The result of the evaluation
     * @exception EcmaScriptException
     *                In case of any error during the evaluation
     */
    public ESValue evaluateProgram(ASTProgram node, EvaluationSource es)
            throws EcmaScriptException {

        evaluationSource = es;
        if (completionCode != -1) {
            throw new ProgrammingError("Multiple use of evalution visitor");
        }
        completionCode = C_NORMAL;
        ESValue result = null;

        if (debug) {
            System.out.println("evaluateProgram for: " + node);
        }

        try {
            result = (ESValue) node.jjtAccept(this, FOR_VALUE);
        } catch (PackagedException e) {
            e.exception.appendEvaluationSource(new LineEvaluationSource(e.node
                    .getLineNumber(), es));
            throw e.exception;
        }
        if (debug)
            System.out.println("evaluateProgram result: " + result);
        return result;
    }

    /**
     * Evaluate a tree which represents a function. The local variables must
     * have been established by the caller.
     * 
     * @param node
     *            The parsed tree (annotated for variables)
     * @param es
     *            A description of the source for error messages
     * @return The result of the evaluation
     * @exception EcmaScriptException
     *                In case of any error during the evaluation
     */
    public ESValue evaluateFunction(ASTStatementList node, EvaluationSource es)
            throws EcmaScriptException {
        this.evaluationSource = es;
        if (completionCode != -1) {
            throw new ProgrammingError("Multiple use of evaluation visitor");
        }
        completionCode = C_NORMAL;
        ESValue result = null;
        if (debug)
            System.out.println("evaluateFunction for: " + node);
        try {
            result = (ESValue) node.jjtAccept(this, FOR_VALUE);
        } catch (PackagedException e) {
            e.exception.appendEvaluationSource(new LineEvaluationSource(e.node
                    .getLineNumber(), es));
            throw e.exception;
        }
        if (debug)
            System.out.println("evaluateFunction result: " + result);
        return result;
    }

    /**
     * This is a subevaluator. It evaluates a tree which represents a
     * <b>with</b> statement. It is called indirectly via the evaluator when a
     * with statement is encountered in the tree.
     * 
     * @param node
     *            The parsed tree (annotated for variables)
     * @param es
     *            A description of the source for error messages
     * @return The result of the evaluation
     * @exception EcmaScriptException
     *                In case of any error during the evaluation
     */
    public ESValue evaluateWith(ASTStatement node, EvaluationSource es)
            throws EcmaScriptException {
        evaluationSource = es;
        if (completionCode != -1) {
            throw new ProgrammingError("Multiple use of evalution visitor");
        }
        completionCode = C_NORMAL;
        ESValue result = null;
        if (debug)
            System.out.println("evaluateWith for: " + node);
        try {
            result = (ESValue) node.jjtAccept(this, FOR_VALUE);
        } catch (PackagedException e) {
            e.exception.appendEvaluationSource(new LineEvaluationSource(e.node
                    .getLineNumber(), es));
            throw e.exception;
        }
        if (debug)
            System.out.println("evaluateWith result: " + result);
        return result;
    }

    /*--------------------------------------------------------------------
     * The following routines implement the interpretation process
     * For detail see the EcmaScript standard to which they refer
     *------------------------------------------------------------------*/

    // EcmaScript standard 11.8.5
    private int compare(ESValue x, ESValue y, boolean swapped) throws EcmaScriptException {
        ESValue px;
        ESValue py;
        if (swapped) {
            py = y.toESPrimitive(ESValue.EStypeNumber);
            px = x.toESPrimitive(ESValue.EStypeNumber);
        } else {
            px = x.toESPrimitive(ESValue.EStypeNumber);
            py = y.toESPrimitive(ESValue.EStypeNumber);
        }
        if ((px instanceof ESString) && (py instanceof ESString)) {
            String s1 = px.toString();
            String s2 = py.toString();
            int c = s1.compareTo(s2);
            return (c < 0) ? ESValue.COMPARE_TRUE : ESValue.COMPARE_FALSE;
        }
        return x.toESNumber().compareNumbers(y);
    }

    /**
       EcmaScript standard 11.9.3
       If x is null and y is undefined, return true.
       If x is undefined and y is null, return true.
       If Type(x) is Number and Type(y) is String, return the result of the comparison x == ToNumber(y).
       If Type(x) is String and Type(y) is Number, return the result of the comparison ToNumber(x) == y.
       If Type(x) is Boolean, return the result of the comparison ToNumber(x) == y.
       If Type(y) is Boolean, return the result of the comparison x == ToNumber(y).
       If Type(x) is either String or Number and Type(y) is Object, return the result of the comparison x == ToPrimitive(y).
       If Type(x) is Object and Type(y) is either String or Number, return the result of the comparison ToPrimitive(x) == y.
     * @param x
     * @param y
     * @return
     * @throws EcmaScriptException
     */
    private boolean equal(ESValue x, ESValue y) throws EcmaScriptException {

        // Not possible to optimize same object, as NaN != NaN

        if (x.getTypeOf() == y.getTypeOf()) {
            // Same types
            return x.equalsSameType(y);
        }

        if (x instanceof ESUndefined && y instanceof ESNull) {
            return true;
        }
        if (x instanceof ESNull && y instanceof ESUndefined) {
            return true;
        }

        if (x instanceof ESNumber && y instanceof ESString) {
            return x.equalsSameType(y.toESNumber());
        }
        if (x instanceof ESString && y instanceof ESNumber) {
            return y.equalsSameType(x.toESNumber());
        }

        if (x instanceof ESBoolean) {
            return equal(x.toESNumber(),y);
        }
        if (y instanceof ESBoolean) {
            return equal(x,y.toESNumber());
        }

        if ((x instanceof ESNumber || x instanceof ESString) && y instanceof ESObject) {
            return equal(x, y.toESPrimitive());
        }
        if (x instanceof ESObject && (y instanceof ESNumber || y instanceof ESString)) {
            return equal(x.toESPrimitive(),y);
        }

        return false;

    }

    // The dispatching is by node type - if the specific visitor
    // is not implemented, then this routine is called
    @Override
    public Object visit(SimpleNode node, Object data) {
        throw new ProgrammingError("Visitor not implemented for node type "
                + node.getClass());
    }

    // All routines have about the same form - thet check the number of
    // children and then iterate the children appropriately, taking the
    // the appropritate action for each children. Recursing the evaluation
    // is done via the routine jjtAccept.
    @Override
    public Object visit(ASTProgram node, Object data) {
        evaluator.setStrictMode(node.isStrictMode());
        int n = node.jjtGetNumChildren();
        Object result = ESUndefined.theUndefined;
        if (n > 0) {
            result = node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
            for (int i = 1; i < node.jjtGetNumChildren(); i++) {
                Node statement = node.jjtGetChild(i);
                result = statement.jjtAccept(this, FOR_VALUE);
            }
        }
        return result;
    }

    @Override
    public Object visit(ASTStatementList node, Object data) {
        int n = node.jjtGetNumChildren();
        Object result = null;
        for (int i = 0; i < n; i++) {
            if (completionCode != C_NORMAL)
                return result;
            Node statement = node.jjtGetChild(i);
            Object s = statement.jjtAccept(this, FOR_VALUE);
            if (completionCode == C_BREAK) {
                if (targetLabel != null && node.labelSetContains(targetLabel)) {
                    completionCode = C_NORMAL;
                    targetLabel = null;
                }
            } else if (completionCode == C_CONTINUE) {
                if (targetLabel != null && node.labelSetContains(targetLabel)) {
                    completionCode = C_NORMAL;
                    targetLabel = null;
                }
            }

            if (s != null) {
                result = s;
            }
        }
        return result;
    }

    @Override
    public Object visit(ASTFunctionDeclaration node, Object data) {
        return null; // Ignored during interpretation
    }

    @Override
    public Object visit(ASTFormalParameterList node, Object data) {
        // Should not occur during interpretation as we skip parent node
        throw new ProgrammingError("Should not visit this");
    }

    @Override
    public Object visit(ASTStatement node, Object data) {
        Object result = null;
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 1) {
            result = node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
        } else if (nChildren != 0) {
            throw new ProgrammingError("Bad AST in statement (>1 child");
        }
        return result;
    }

    @Override
    public Object visit(ASTVariableDeclaration node, Object data) {
        int nChildren = assertInRange(node, 1, 2, "variable declaration");
        if (nChildren == 2) {
            try {
                Object lvo = node.jjtGetChild(0).jjtAccept(this, FOR_REFERENCE);
                ESReference lv;
                if (lvo instanceof ESReference) {
                    lv = (ESReference) lvo;
                } else {
                    // Should not happen as first node should be an identifier
                    throw new ProgrammingError("Value '" + lvo.toString()
                            + "' is not a variable");
                }
                ESValue rv = (ESValue) node.jjtGetChild(1).jjtAccept(this,
                        FOR_VALUE);
                if (useRepresentationOptimisation
                        && rv instanceof ESString
                        && REPRESENTATION_RESULT_NAME.equals(lv
                                .getPropertyName())) {
                    ESString initialValue = (ESString) rv;
                    rv = new ESAppendable(representationOutputBuffer);
                    ((ESAppendable) rv).appendString(initialValue, evaluator);
                }
                lv.putValue(null, rv); // null because the variable should be
                                       // undefined!
            } catch (EcmaScriptException e) {
                throw new PackagedException(e, node);
            }
        }
        return null;
    }

    @Override
    public Object visit(ASTIfStatement node, Object data) {
        Object result = null;
        int nChildren = assertInRange(node, 2, 3, "IF statement");
        try {
            ESValue testValue = acceptNull(node.jjtGetChild(0).jjtAccept(this,
                    FOR_VALUE));
            boolean test = testValue.booleanValue();
            if (test) {
                result = node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
            } else {
                if (nChildren == 3) {
                    result = node.jjtGetChild(2).jjtAccept(this, FOR_VALUE);
                }
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    private int assertInRange(SimpleNode node, int start, int end,
            String location) throws ProgrammingError {
        int nChildren = node.jjtGetNumChildren();
        if (nChildren < start || nChildren > end) {
            throw new ProgrammingError("Bad AST in "+location);
        }
        return nChildren;
    }

    @Override
    public Object visit(ASTWhileStatement node, Object data) {
        Object result = null;
        node.assertTwoChildren();
        try {
            ESValue testValue = acceptNull(node.jjtGetChild(0).jjtAccept(this,
                    FOR_VALUE));
            while (testValue.booleanValue()) {
                result = node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
                if (completionCode == C_RETURN) {
                    return result;
                } else if (completionCode == C_BREAK) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    }
                    return result;
                } else if (completionCode == C_CONTINUE) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        testValue = acceptNull(node.jjtGetChild(0).jjtAccept(this,
                                FOR_VALUE));
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    } else {
                        return result;
                    }
                } else {
                    testValue = acceptNull(node.jjtGetChild(0).jjtAccept(this,
                            FOR_VALUE));
                }
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTDoWhileStatement node, Object data) {
        Object result = null;
        node.assertTwoChildren();
        try {
            ESValue testValue;
            do {
                result = node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
                if (completionCode == C_RETURN) {
                    return result;
                } else if (completionCode == C_BREAK) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    }
                    return result;
                } else {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        if (completionCode == C_CONTINUE) {
                            completionCode = C_NORMAL;
                            targetLabel = null;
                        }
                        testValue = acceptNull(node.jjtGetChild(1).jjtAccept(this,
                                FOR_VALUE));
                    } else {
                        return result;
                    }
                }
            } while (testValue.booleanValue());
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTForStatement node, Object data) {
        Object theValue = null;
        try {
            node.assertFourChildren();
            // Evaluate first expression if present
            node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);

            Node testNode = node.jjtGetChild(1);
            ESValue testValue;
            if (testNode instanceof ASTEmptyExpression) {
                testValue = ESBoolean.valueOf(true);
            } else {
                testValue = acceptNull(testNode.jjtAccept(this, FOR_VALUE));
            }
            while (testValue.booleanValue()) {
                Object result = node.jjtGetChild(3).jjtAccept(this, FOR_VALUE);
                if (result != null) {
                    theValue = result;
                }

                if (completionCode == C_RETURN) {
                    return theValue;
                } else if (completionCode == C_BREAK) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    }
                    return theValue;
                } else if (completionCode == C_CONTINUE) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        node.jjtGetChild(2).jjtAccept(this, FOR_VALUE);
                        if (testNode instanceof ASTEmptyExpression) {
                            testValue = ESBoolean.valueOf(true);
                        } else {
                            testValue = acceptNull(testNode.jjtAccept(this,
                                    FOR_VALUE));
                        }
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    } else {
                        return theValue;
                    }
                } else {
                    node.jjtGetChild(2).jjtAccept(this, FOR_VALUE);
                    if (testNode instanceof ASTEmptyExpression) {
                        testValue = ESBoolean.valueOf(true);
                    } else {
                        testValue = acceptNull(testNode.jjtAccept(this,
                                FOR_VALUE));
                    }
                }

            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return theValue;
    }

    // Assume that in 12.6.2, for var, step 7, should be goto 17
    @Override
    public Object visit(ASTForVarStatement node, Object data) {
        Object result = null; // No value by default
        try {
            node.assertFourChildren();
            node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);

            Node testNode = node.jjtGetChild(1);
            ESValue testValue;
            if (testNode instanceof ASTEmptyExpression) {
                testValue = ESBoolean.valueOf(true);
            } else {
                testValue = acceptNull(testNode.jjtAccept(this, FOR_VALUE));
            }
            while (testValue.booleanValue()) {
                result = node.jjtGetChild(3).jjtAccept(this, FOR_VALUE);

                if (completionCode == C_RETURN) {
                    return result;
                } else if (completionCode == C_BREAK) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    }
                    return result;
                } else if (completionCode == C_CONTINUE) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        node.jjtGetChild(2).jjtAccept(this, FOR_VALUE);
                        if (testNode instanceof ASTEmptyExpression) {
                            testValue = ESBoolean.valueOf(true);
                        } else {
                            testValue = acceptNull(testNode.jjtAccept(this,
                                    FOR_VALUE));
                        }
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    } else {
                    	return result;
                    }
                } else {
                    node.jjtGetChild(2).jjtAccept(this, FOR_VALUE);
                    if (testNode instanceof ASTEmptyExpression) {
                        testValue = ESBoolean.valueOf(true);
                    } else {
                        testValue = acceptNull(testNode.jjtAccept(this,
                                FOR_VALUE));
                    }
                }

            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTForInStatement node, Object data) {
        Object result = null; // No value by default
        node.assertThreeChildren();
        try {
            ESValue ob = acceptNull(node.jjtGetChild(1).jjtAccept(this,
                    FOR_VALUE));
            if (ob == ESNull.theNull || ob == ESUndefined.theUndefined) {
                return data;
            }
            ESObject obj = ob.toESObject(evaluator);
            boolean directEnumeration = obj.isDirectEnumerator();
            for (Enumeration<String> e = obj.getProperties(); e
                    .hasMoreElements();) {
                Object en = e.nextElement();
                ESValue s;
                if (directEnumeration) {
                    s = ESLoader.normalizeValue(en, evaluator);
                } else {
                    s = new ESString((en.toString()));
                }

                Object lvo = node.jjtGetChild(0).jjtAccept(this, FOR_REFERENCE);
                ESReference lv;
                if (lvo instanceof ESReference) {
                    lv = (ESReference) lvo;
                } else {
                    throw new ReferenceError("Value '" + lvo.toString()
                            + "' is not an assignable object or property");
                }
                evaluator.putValue(lv, s);
                result = node.jjtGetChild(2).jjtAccept(this, FOR_VALUE);
                if (completionCode == C_RETURN) {
                    break;
                } else if (completionCode == C_BREAK) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    }
                    break;
                } else if (completionCode == C_CONTINUE) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        targetLabel = null;
                        continue;
                    }
                    break;
                }
            }

        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTForVarInStatement node, Object data) {
        Object result = null; // No value by default
        node.assertFourChildren();
        
        try {
            Object lvo = node.jjtGetChild(0).jjtAccept(this, FOR_REFERENCE);
            ESReference lv;
            if (lvo instanceof ESReference) {
                lv = (ESReference) lvo;
            } else {
                // Should not happen as it should be an identifier
                throw new ProgrammingError("Value '" + lvo.toString()
                        + "' is not a variable");
            }
            ESValue init = acceptNull(node.jjtGetChild(1).jjtAccept(this,
                    FOR_VALUE));
            evaluator.putValue(lv, init);

            ESValue ob = acceptNull(node.jjtGetChild(2).jjtAccept(this,
                    FOR_VALUE));
            if (ob == ESNull.theNull || ob == ESUndefined.theUndefined) {
                return data;
            }

            ESObject obj = ob.toESObject(evaluator);
            boolean directEnumeration = obj.isDirectEnumerator();
            for (Enumeration<String> e = obj.getProperties(); e
                    .hasMoreElements();) {
                Object en = e.nextElement();
                ESValue s;
                if (directEnumeration) {
                    s = ESLoader.normalizeValue(en, evaluator);
                } else {
                    s = new ESString((en.toString()));
                }
                // Typing already checked above - will generate an error anyhow
                lv = (ESReference) node.jjtGetChild(0).jjtAccept(this,
                        FOR_REFERENCE);
                evaluator.putValue(lv, s);
                result = node.jjtGetChild(3).jjtAccept(this, FOR_VALUE);
                if (completionCode == C_RETURN) {
                    break;
                } else if (completionCode == C_BREAK) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        targetLabel = null;
                    }
                    break;
                } else if (completionCode == C_CONTINUE) {
                    if (targetLabel == null || node.labelSetContains(targetLabel)) {
                        completionCode = C_NORMAL;
                        continue;
                    }
                    break;
                }
            }

        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTContinueStatement node, Object data) {
        int n = node.jjtGetNumChildren();
        if (n == 1) {
            targetLabel = node.jjtGetChild(0).toString();
        } else if (n == 0) {
            targetLabel = null;
        } else {
            throw new ProgrammingError("AST Should have zero or one child");
        }
        completionCode = C_CONTINUE;
        return null;
    }

    @Override
    public Object visit(ASTBreakStatement node, Object data) {
        int n = node.jjtGetNumChildren();
        if (n == 1) {
            targetLabel = node.jjtGetChild(0).toString();
        } else if (n == 0) {
            targetLabel = null;
        } else {
            throw new ProgrammingError("AST Should have zero or one child");
        }
        completionCode = C_BREAK;
        return null;
    }

    @Override
    public Object visit(ASTReturnStatement node, Object data) {
        node.assertOneChild();
        Object result = node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
        completionCode = C_RETURN;
        return result;
    }

    @Override
    public Object visit(ASTWithStatement node, Object data) {
        node.assertTwoChildren();
        ESValue result = null;
        try {
            EvaluationSource es = (EvaluationSource) node.getEvaluationSource();
            ESValue scopeValue = acceptNull(node.jjtGetChild(0).jjtAccept(this,
                    FOR_VALUE));
            ASTStatement statementNode = (ASTStatement) (node.jjtGetChild(1));
            ESObject scopeObject = scopeValue.toESObject(evaluator);
            EvaluationResult evaluationResult = evaluator.evaluateWith(statementNode, scopeObject, es);
            result = evaluationResult.value;
            completionCode = evaluationResult.completionCode;
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTThisReference node, Object data) {
        try {
            node.assertNoChildren();
            return evaluator.getThisObject();
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
    }

    @Override
    public Object visit(ASTSuperReference node, Object data) {
        try {
            node.assertNoChildren();
            return evaluator.getSuperObject();
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
    }

    /*
     * Attempt to minimize the creation of intermediate ESReferences. This is
     * prettry tricky. The trick is to keep the last result as a delayed
     * reference while looking ahead for parameter or array index. A delayed
     * reference is represented by a non null currentProperty with an object
     * (which is the last result returned). If the last Result is null, this
     * indicates access to the global environment.
     */
    @Override
    public Object visit(ASTCompositeReference node, Object forWhat) {
        int nChildren = node.jjtGetNumChildren();
        if (nChildren < 2)
            throw new ProgrammingError("Bad ast");

        try {
            // The base node is the first node in the serie.
            // If it is an identifier, it is a reference to a property
            // of the global object, so it can be a delayed reference to the
            // specified property of the global object. Otherwise it is
            // some kind of expression returning a value, and therefore cannot
            // be a delayed reference.
            ESValue lastResult;
            ESValue currentProperty;
            {
                Node baseNode = node.jjtGetChild(0);
                if (baseNode instanceof ASTIdentifier) {
                    lastResult = null; // Means lookup in global environment
                                       // (with scope)
                    String id = ((ASTIdentifier) baseNode).getName();
                    currentProperty = new ESString(id);
                } else {
                    lastResult = acceptNull(baseNode.jjtAccept(this, FOR_VALUE));
                    currentProperty = null; // No reference so far
                }
            }

            // Here the currentProperty and lastResult are initialized.

            // ****
            // System.out.println("--->ASTCompositeReference: for: " +
            // (forWhat==FOR_VALUE ? "VALUE" : "REF") + " lr="+lastResult+
            // ", cp="+currentProperty + "<---");

            for (int i = 1; i < nChildren; i++) {

                Node compositor = node.jjtGetChild(i);

                if ((compositor instanceof ASTPropertyValueReference)
                        || (compositor instanceof ASTPropertyIdentifierReference)) {
                    // Object property accessor, will build a new delayed
                    // reference.

                    // First dereference any indirect reference left by a
                    // previous iteration
                    if (currentProperty != null) {
                        ESValue newBase;
                        String propertyName = currentProperty.toString();
                        if (lastResult == null) {
                            newBase = evaluator.getValue(propertyName,
                                    propertyName.hashCode());
                            // ****
                            // System.out.println("--->NB = " + newBase +
                            // "<---");
                            if (newBase instanceof ESUndefined) {
                                throw new TypeError("Variable '" + propertyName
                                        + "' has an undefined value");
                            }
                        } else {
                            ESObject currentBase = lastResult.toESObject(evaluator);
                            // ****
                            // System.out.println("--->CB *** " +
                            // currentBase.getClass() + " " + propertyName +
                            // "<---");
                            newBase = currentBase.getProperty(propertyName,
                                    propertyName.hashCode());
                            if (newBase instanceof ESUndefined) {
                                throw new TypeError("The property '"
                                        + propertyName
                                        + "' is not defined in object '"
                                        + currentBase.toString() + "'");
                            }
                        }
                        lastResult = newBase;
                        currentProperty = null; // Assure invariant at end of if
                    }
                    // Here the lastResult contains the result of the expression
                    // before the
                    // current compositor, as a value. currentProperty is null
                    // as this is not a
                    // delayed reference.

                    // We get the current property name (in principle any
                    // expression, for example
                    // in obj['base'+index.toString()].
                    currentProperty = (ESValue) compositor.jjtAccept(this,
                            FOR_VALUE);
                    // System.out.println("--->LR = " + lastResult +
                    // " currentProperty = " + currentProperty.toString() +
                    // "<---"); // *********

                } else if (compositor instanceof ASTFunctionCallParameters) {
                    // We have parameters. The function object may be
                    // represented
                    // by a Function value, or a delayed reference to a base
                    // object.

                    // First get the arguments (evaluated)
                    ESValue[] arguments = (ESValue[]) compositor.jjtAccept(
                            this, FOR_VALUE);

                    // Find the 'this' for the function call. If it is a delayed
                    // reference, the this object is represented by the last
                    // result (the
                    // base of the reference).
                    ESObject thisObject;
                    // System.out.println("--->CP: " + currentProperty +
                    // "<---"); // *************
                    if (currentProperty != null) {
                        // Delayed reference, find base object (global if base
                        // is null)
                        // System.out.println("--->LR: " + lastResult + "<---");
                        // // *************
                        if (lastResult == null) {
                            thisObject = evaluator.getGlobalObject();
                            // System.out.println("--->GO: " + thisObject +
                            // "<---"); // *************
                        } else {
                            thisObject = lastResult.toESObject(evaluator); // if function is
                                                            // called via an
                                                            // object
                        }
                        // Special case (see standard document)
                        if (thisObject instanceof ESArguments) {
                            thisObject = evaluator.getGlobalObject();
                        }
                    } else {
                        // Assume that global may never be an ESArgument (for
                        // performance)
                        thisObject = evaluator.getGlobalObject(); // Global
                                                                  // object by
                                                                  // default
                    }

                    // The thisObject will be the target of the call.
                    // If we have a delayed reference, we can do an indirect
                    // call,
                    // leaving it up to the target object to find the routine to
                    // call.
                    // This allow native objects to implement their own lookup
                    // without
                    // requiring the creating of an intermediate Function
                    // object.
                    // System.out.println("--->THIS: " +
                    // thisObject.toDetailString() + "<---"); // *************
                    if (currentProperty != null) {
                        // Use lastResult and property name for indirect call
                        String functionName = currentProperty.toString();
                        // System.out.println("--->FN: " + functionName +
                        // ", LR: " + lastResult + "<---"); // *************
                        if (lastResult == null) {
                            lastResult = evaluator.doIndirectCall(thisObject,
                                    functionName, functionName.hashCode(),
                                    arguments);
                        } else {
                            try {
                                lastResult = thisObject.doIndirectCall(
                                        evaluator, thisObject, functionName,
                                        arguments);
                            } catch (NoSuchMethodException e) {
                                throw new EcmaScriptException(e.getMessage());
                            }
                        }
                        currentProperty = null;
                    } else {
                        if (lastResult == null) {
                            throw new ProgrammingError("lastResult is null. Cannot invoke function");
                        }
                        // Via global or WITH, use current object
                        ESValue theFunction = lastResult
                                .toESObject(evaluator); // Conversion needed ?
                        lastResult = theFunction.callFunction(thisObject,
                                arguments);
                    }
                    completionCode = C_NORMAL;

                } else {
                    throw new ProgrammingError("Bad AST");
                }

            } // for

            // Either build reference or return object depending on type of
            // request
            // Here, if propertyName is not null, then lastResult.propertyName
            // contains
            // the value (delayed dereferencing). Otherwise the value is in
            // lastResult.
            Object result; // Will be the returned value
            if (forWhat == FOR_VALUE) {
                // We want a value
                // System.out.println("--->Build value cp: " + currentProperty +
                // " lr: " + lastResult + "<---"); // ********
                if (currentProperty != null) {
                    // Must dereference value
                    if (lastResult == null) {
                        throw new EcmaScriptException(
                                "'undefined' is not an object with properties");
                    }
                    ESObject currentBase = lastResult.toESObject(evaluator);
                    String propertyName = currentProperty.toString();
                    // System.out.println("--->getProperty in cb: " +
                    // currentBase + " pn: " + propertyName + "<---"); //
                    // *******
                    result = currentBase.getProperty(propertyName, propertyName
                            .hashCode());
                } else {
                    // Last value is already the final value
                    result = lastResult;
                }
            } else {
                // We want a reference - therefore it cannot be just a value, it
                // must be a delayed reference.
                if (lastResult == null) {
                    result = new ESReference( "'undefined' is not an assignable value");
                } else if (currentProperty == null) {
                    result = new ESReference("'" + lastResult.toString()
                            + "' is not an assignable value");
                } else {
                    ESObject currentBase = lastResult.toESObject(evaluator);
                    String propertyName = currentProperty.toString();
                    // System.out.println("--->Build ref cb: " + currentBase +
                    // " pn: " + propertyName + "<---"); // ********
                    result = new ESReference(currentBase, propertyName,
                            propertyName.hashCode());
                }
            }

            return result;

        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
    }

    @Override
    public Object visit(ASTFunctionCallParameters node, Object data) {

        int nChildren = node.jjtGetNumChildren();
        ESValue[] arguments = new ESValue[nChildren];
        for (int i = 0; i < nChildren; i++) {
            arguments[i] = (ESValue) node.jjtGetChild(i).jjtAccept(this,
                    FOR_VALUE);
        }
        return arguments;
    }

    @Override
    public Object visit(ASTPropertyValueReference node, Object data) {
        node.assertOneChild();
        return acceptNull(node.jjtGetChild(0).jjtAccept(this, FOR_VALUE));
    }

    @Override
    public Object visit(ASTPropertyIdentifierReference node, Object data) {
        node.assertOneChild();
        Object result = null;
        Node idNode = node.jjtGetChild(0);
        if (idNode instanceof ASTIdentifier) {
            result = ((ASTIdentifier) idNode).getESName();
        } else {
            throw new ProgrammingError("Bad AST");
        }
        return result;
    }

    @Override
    public Object visit(ASTAllocationExpression node, Object data) {
        node.assertAtLeastOneChild();
        ESValue result = null;
        try {
            Node baseNode = node.jjtGetChild(0);
            // Can be any expression (in fact a a.b.c sequence) [code bizare
            // here]
            ESValue constr = acceptNull(baseNode.jjtAccept(this, FOR_VALUE));
            if (node.jjtGetNumChildren() == 1) { 
                result = constr.doConstruct(ESValue.EMPTY_ARRAY);
            } else {
                Node compositor = node.jjtGetChild(1);
                if (compositor instanceof ASTFunctionCallParameters) {
                    ASTFunctionCallParameters fc = (ASTFunctionCallParameters) compositor;
                    ESValue[] arguments = (ESValue[]) fc.jjtAccept(this, FOR_VALUE);
                    result = constr.doConstruct(
                            arguments);
                } else {
                    throw new ProgrammingError("Bad AST");
                }
            }
            if (result == null) {
                throw new EcmaScriptException("new did not return an object");
            }
            completionCode = C_NORMAL;
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTOperator node, Object data) {
        throw new ProgrammingError("Bad AST walk");
    }

    @Override
    public Object visit(ASTPostfixExpression node, Object data) {
        ESValue result;
        try {
            node.assertTwoChildren();
            Object lvo = node.jjtGetChild(0).jjtAccept(this, FOR_REFERENCE);
            ESReference lv;
            if (lvo instanceof ESReference) {
                lv = (ESReference) lvo;
            } else {
                throw new ReferenceError("Value '" + lvo.toString()
                        + "' is not an assignable object or property");
            }
            int operator = ((ASTOperator) (node.jjtGetChild(1))).getOperator();
            result = lv.getValue().toESNumber();
            ESValue vr;
            if (operator == INCR) {
                vr = result.increment();
            } else if (operator == DECR ) {
                vr = result.decrement();
            } else {
                throw new ProgrammingError("Bad operator");
            }
            evaluator.putValue(lv, vr);
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTUnaryExpression node, Object data) {
        ESValue r = null;
        try {
            node.assertTwoChildren();
            int operator = ((ASTOperator) (node.jjtGetChild(0))).getOperator();
            switch (operator) {
            case DELETE: {
                Node firstChild = node.jjtGetChild(1);
                if (evaluator.isStrictMode() && firstChild instanceof ASTIdentifier) {
                    throw new SyntaxError("Cannot delete a variable, function argument or function in strict mode");
                }
                Object lvo = firstChild.jjtAccept(this, FOR_REFERENCE);
                if (lvo instanceof ESReference) {
                    ESReference lv = (ESReference) lvo;
                    ESValue base = lv.getBase();
                    if (base instanceof ESObject) {
                        String propertyName = lv.getPropertyName();
                        r = ESBoolean.valueOf(((ESObject) base).deleteProperty(
                                propertyName, propertyName.hashCode()));
                    } else {
                        r = ESBoolean.valueOf(true);
                    }
                } else {
                    r = ESBoolean.valueOf(true);
                }
            }
                break;
            case VOID:
                node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
                r = ESUndefined.theUndefined;
                break;
            case TYPEOF: {
                Node n = node.jjtGetChild(1);
                if (n instanceof ASTIdentifier) {
                    // We need to get a reference, as an null based referenced
                    // is "undefined"
                    ESReference ref = (ESReference) n.jjtAccept(this,
                            FOR_REFERENCE);
                    // If reference to nothing, consider undefined
                    if (ref == null || ref.getBase() == null) {
                        r = new ESString("undefined");
                    } else {
                        ESValue v = ref.getValue();
                        r = new ESString(v.getTypeofString());
                    }
                } else {
                    // It is a value, directly get its string
                    ESValue v = acceptNull(n.jjtAccept(this, FOR_VALUE));
                    r = new ESString(v.getTypeofString());
                }
            }
                break;
            case INCR:
            case NL_INCR: {
                Object lvo = node.jjtGetChild(1).jjtAccept(this, FOR_REFERENCE);
                ESReference lv;
                if (lvo instanceof ESReference) {
                    lv = (ESReference) lvo;
                } else {
                    throw new ReferenceError("Value '" + lvo.toString()
                            + "' is not an assignable object or property");
                }
                ESValue v = lv.getValue();
                r = v.increment();
                evaluator.putValue(lv, r);
            }
                break;
            case DECR:
            case NL_DECR: {
                Object lvo = node.jjtGetChild(1).jjtAccept(this, FOR_REFERENCE);
                ESReference lv;
                if (lvo instanceof ESReference) {
                    lv = (ESReference) lvo;
                } else {
                    throw new ReferenceError("Value '" + lvo.toString()
                            + "' is not an assignable object or property");
                }
                ESValue v = lv.getValue();
                r = v.decrement();
                evaluator.putValue(lv, r);
            }
                break;
            case PLUS: {
                ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,
                        FOR_VALUE);
                r = v.toESNumber();
            }
                break;
            case MINUS: {
                ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,
                        FOR_VALUE);
                double dv = v.doubleValue();
                r = ESNumber.valueOf(-dv);
            }
                break;
            case TILDE: {
                ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,
                        FOR_VALUE);
                int iv = v.toInt32();
                r = ESNumber.valueOf(~iv);
            }
                break;
            case BANG: {
                ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,
                        FOR_VALUE);
                boolean bv = v.booleanValue();
                r = ESBoolean.valueOf(!bv);
            }
                break;
            default:
                throw new ProgrammingError("Unimplemented unary");
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }

        return r;
    }

    @Override
    public Object visit(ASTBinaryExpressionSequence node, Object data) {
        ESValue result = null;
        try {
            ESValue v1 = acceptNull(node.jjtGetChild(0).jjtAccept(this,
                    FOR_VALUE));
            for (int i = 0; i < node.jjtGetNumChildren() - 1; i += 2) {
                ESValue v2 = acceptNull(node.jjtGetChild(i + 2).jjtAccept(this,
                        FOR_VALUE));
                int operator = ((ASTOperator) (node.jjtGetChild(i + 1)))
                        .getOperator();
                // System.out.println("V1 = " + v1 + " v2 = " + v2);
                switch (operator) {
                case PLUS: {
                    ESValue v1p = v1.toESPrimitive();
                    ESValue v2p = v2.toESPrimitive();
                    // System.out.println("v1p = " + v1 + " v2p = " + v2);
                    if ((v1p.isStringValue()) || (v2p.isStringValue())) {
                        result = concatenateStrings(v1p, v2p);
                    } else {
                        result = v1.addValue(v2);
                    }
                }
                    break;
                case MINUS: {
                    result = v1.subtract(v2);
                }
                    break;
                case STAR: {
                    result = v1.multiply(v2);
                }
                    break;
                case SLASH: {
                    result = v1.divide(v2);
                }
                    break;
                case REM: {
                    result = v1.modulo(v2);
                }
                    break;
                case LSHIFT: {
                    result = ESNumber.valueOf(v1.toInt32() << (v2.toUInt32()&0x1f));
                }
                    break;
                case RSIGNEDSHIFT: {
                    int lnum = v1.toInt32();
                    long rnum = v2.toUInt32()&0x1f;
                    result = ESNumber.valueOf(lnum >> rnum);
                }
                    break;
                case RUNSIGNEDSHIFT: {
                    result = ESNumber.valueOf(v1.toUInt32() >>> (v2.toUInt32()&0x1f));
                }
                    break;
                case LT: {
                    int compareCode = compare(v1, v2, false);
                    result = ESBoolean.valueOf(compareCode == ESValue.COMPARE_TRUE);
                }
                    break;
                case GT: {
                    int compareCode = compare(v2, v1, true);
                    result = ESBoolean.valueOf(compareCode == ESValue.COMPARE_TRUE);
                }
                    break;
                case LE: {
                    int compareCode = compare(v2, v1, true);
                    result = ESBoolean.valueOf(compareCode == ESValue.COMPARE_FALSE);
                }
                    break;
                case GE: {
                    int compareCode = compare(v1, v2, false);
                    result = ESBoolean.valueOf(compareCode == ESValue.COMPARE_FALSE);
                }
                    break;
                case EQ: {
                    result = ESBoolean.valueOf(equal(v1, v2));
                }
                    break;
                case NE: {
                    result = ESBoolean.valueOf(!equal(v1, v2));
                }
                    break;
                case BIT_AND: {
                    int iv1 = v1.toInt32();
                    int iv2 = v2.toInt32();
                    result = ESNumber.valueOf(iv1 & iv2);
                }
                    break;
                case BIT_OR: {
                    int iv1 = v1.toInt32();
                    int iv2 = v2.toInt32();
                    result = ESNumber.valueOf(iv1 | iv2);
                }
                    break;
                case XOR: {
                    int iv1 = v1.toInt32();
                    int iv2 = v2.toInt32();
                    result = ESNumber.valueOf(iv1 ^ iv2);
                }
                    break;
                case STRICT_EQ: {
                    result = ESBoolean.valueOf(v1.strictEqual(v2));
                }
                    break;
                case STRICT_NEQ: {
                    result = ESBoolean.valueOf(!v1.strictEqual(v2));
                }
                    break;
                case INSTANCEOF: {
                    if (! (v2 instanceof ESObject) ) {
                        throw new TypeError("RHS of instanceof must be an object");
                    }
                    result = ESBoolean.valueOf(((ESObject)v2).hasInstance(v1));
                }   break;
                case IN: {
                    if (! (v2 instanceof ESObject)) {
                        throw new TypeError("RHS of in must be an object");
                    }
                    String propertyName = v1.toString();
                    result = ESBoolean.valueOf(((ESObject)v2).hasProperty(propertyName, propertyName.hashCode()));
                }   break;
                default:
                    throw new ProgrammingError("Unimplemented binary");
                } // switch
                v1 = result;
            } // for
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTAndExpressionSequence node, Object data) {
        ESValue result = null;
        int nChildren = node.jjtGetNumChildren();
        try {
            result = acceptNull(node.jjtGetChild(0).jjtAccept(this, FOR_VALUE));
            int i = 1;
            while (result.booleanValue() && (i < nChildren)) {
                result = acceptNull(node.jjtGetChild(i).jjtAccept(this,
                        FOR_VALUE));
                i++;
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTOrExpressionSequence node, Object data) {
        int nChildren = node.jjtGetNumChildren();
        ESValue result = null;
        try {
            result = acceptNull(node.jjtGetChild(0).jjtAccept(this, FOR_VALUE));
            int i = 1;
            while ((!result.booleanValue()) && (i < nChildren)) {
                result = acceptNull(node.jjtGetChild(i).jjtAccept(this,
                        FOR_VALUE));
                i++;
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTEmptyExpression node, Object data) {
        node.assertNoChildren();
        return ESUndefined.theUndefined;
    }

    @Override
    public Object visit(ASTConditionalExpression node, Object data) {
        node.assertThreeChildren();
        Object result = null;
        try {
            ESValue t = acceptNull(node.jjtGetChild(0).jjtAccept(this,
                    FOR_VALUE));
            boolean test = t.booleanValue();
            if (test) {
                result = node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
            } else {
                result = node.jjtGetChild(2).jjtAccept(this, FOR_VALUE);
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTAssignmentExpression node, Object data) {
        node.assertThreeChildren();
        ESValue result = null;
        try {
            // Get left hand side
            Object lvo = node.jjtGetChild(0).jjtAccept(this, FOR_REFERENCE);
            // System.out.println("REF: " + lvo);
            ESReference lv;
            if (lvo instanceof ESReference) {
                lv = (ESReference) lvo;
            } else {
                throw new ReferenceError("Value '" + lvo.toString()
                        + "' is not an assignable object or property");
            }

            ESValue v2 = acceptNull(node.jjtGetChild(2).jjtAccept(this,
                    FOR_VALUE));

            // Case analysis based on assignement operator type
            int operator = ((ASTOperator) (node.jjtGetChild(1))).getOperator();
            if (operator == ASSIGN) {
                // Simple assignement may create a new property
                evaluator.putValue(lv, v2);
                if (lv.getHash() == StandardProperty.LENGTHhash
                        && StandardProperty.LENGTHstring.equals(lv.getPropertyName())) {
                    // Array length assignment return may not actually be the 
                    // specified length, so get the length after the fact. 
                    // N.B.: could also be achieved by modifying the ESNumber 
                    // value were it not final (presumably for performance)
                    ESValue baseValue = lv.getBase();
                    if (baseValue != null && baseValue.isArray()) {
                        ESObject o = (ESObject) baseValue;
                        result = o.getProperty(StandardProperty.LENGTHstring, StandardProperty.LENGTHhash);
                    }
                } else {
                    result = v2;
                }
            } else {
                // All composite assignement requires a current value
                ESValue v1 = lv.getValue();
                switch (operator) {
                case PLUSASSIGN: {
                    ESValue v1p = v1.toESPrimitive();
                    ESValue v2p = v2.toESPrimitive();
                    if (v1 instanceof ESAppendable) {
                        ((ESAppendable) v1).appendString(v2, evaluator);
                        result = v1;
                    } else if ((v1p.isStringValue())
                            || (v2p.isStringValue())) {
                        // Note: Convert v1/2 instead of v1/2p for correct
                        // behaviour of "" + Object;
                        result = concatenateStrings(v1, v2);
                    } else {
                        result = ESNumber.valueOf(v1.doubleValue()
                                + v2.doubleValue());
                    }
                }
                    break;
                case MINUSASSIGN: {
                    result = ESNumber.valueOf(v1.doubleValue()
                            - v2.doubleValue());
                }
                    break;
                case STARASSIGN: {
                    result = ESNumber.valueOf(v1.doubleValue()
                            * v2.doubleValue());
                }
                    break;
                case SLASHASSIGN: {
                    result = ESNumber.valueOf(v1.doubleValue()
                            / v2.doubleValue());
                }
                    break;
                case ANDASSIGN: {
                    int iv1 = v1.toInt32();
                    int iv2 = v2.toInt32();
                    result = ESNumber.valueOf(iv1 & iv2);
                }
                    break;
                case ORASSIGN: {
                    int iv1 = v1.toInt32();
                    int iv2 = v2.toInt32();
                    result = ESNumber.valueOf(iv1 | iv2);
                }
                    break;
                case XORASSIGN: {
                    int iv1 = v1.toInt32();
                    int iv2 = v2.toInt32();
                    result = ESNumber.valueOf(iv1 ^ iv2);
                }
                    break;
                case REMASSIGN: {
                    result = ESNumber.valueOf(v1.doubleValue()
                            % v2.doubleValue());
                }
                    break;
                case LSHIFTASSIGN: {
                    result = ESNumber.valueOf(v1.toInt32() << v2.toUInt32());
                }
                    break;
                case RSIGNEDSHIFTASSIGN: {
                    result = ESNumber.valueOf(v1.toInt32() >> v2.toUInt32());
                }
                    break;
                case RUNSIGNEDSHIFTASSIGN: {
                    result = ESNumber.valueOf(v1.toUInt32() >>> v2.toUInt32());
                }
                    break;
                default:
                    throw new ProgrammingError("Unimplemented assign operator");
                } // switch
                evaluator.putValue(lv, result);
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    private ESValue concatenateStrings(ESValue v1, ESValue v2) {


        if (v1 instanceof ESAppendable) {
            IAppendable appendable = evaluator.createAppendable(16, 1024);
            ((ESAppendable) v1).appendSelfToAppendable(appendable);
            if (v2 instanceof ESAppendable) {
                ((ESAppendable) v2).appendSelfToAppendable(appendable);
            } else {
                appendable.append(v2.toString());
            }

            return new ESAppendable(appendable);
        }

        if (v2 instanceof ESAppendable) {
            IAppendable appendable = evaluator.createAppendable(16, 1024);
            appendable.append(v1.toString());
            ((ESAppendable) v2).appendSelfToAppendable(appendable);
            return new ESAppendable(appendable);
        }

        return new ESString(v1.toString()+v2.toString());
    }

    @Override
    public Object visit(ASTExpressionList node, Object data) {
        int n = node.jjtGetNumChildren();
        Object result = null;
        if (n <= 0) {
            throw new ProgrammingError("Empty expression list");
        }
        result = node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
        for (int i = 1; i < node.jjtGetNumChildren(); i++) {
            Node statement = node.jjtGetChild(i);
            result = statement.jjtAccept(this, FOR_VALUE);
        }
        return result;
    }

    @Override
    public Object visit(ASTLiteral node, Object data) {
        node.assertNoChildren();
        return node.getValue();
    }

    @Override
    public Object visit(ASTIdentifier node, Object forWhat) {
        Object result;
        try {
            if (forWhat == FOR_VALUE) {
                result = evaluator.getValue(node.getName(), node.hashCode());
            } else {
                result = evaluator
                        .getReference(node.getName(), node.hashCode());
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    /**
     * To transform a null (empty) result (but not an ESNull!) in 'ESUndefined'.
     * null results may be returned if a statement as the empty statement,
     * missing else clause of an if statement, loop not executed at all, etc..
     * is executed for value (for example as the last statement of a called
     * function used in an assignement). This is a programming error, and it may
     * be useful to modify this function to generate an exception during
     * debugging. However other implementation seem to accept ESUndefined in
     * these cases. The standard is not totally clear to me.
     * <P>
     * An alternative would be to return ESUndefined in all cases, but then we
     * lose a useful distinction (at least for debugging...).
     * <P>
     * A couple of tests are done in visit(ASTCompositeReference node, ... too.
     */
    static protected ESValue acceptNull(Object v) {
        if (v == null) {
            // Accept null (could generate an optional exception).
            return ESUndefined.theUndefined;
        }
        // Take advantage to convert...
        return (ESValue) v;
    }

    @Override
    public Object visit(ASTObjectLiteral node, Object data) {
        ESObject result = ObjectObject.createObject(evaluator);
        ESObject properties = ObjectObject.createObject(evaluator);
        int numChildren = node.jjtGetNumChildren();
        for (int i=0; i<numChildren; i++) {
            node.jjtGetChild(i).jjtAccept(this, properties);
        }
        try {
            ObjectObject.defineProperties(result, properties, evaluator);
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTPropertyNameAndValue node, Object data) {
        ESObject properties = (ESObject) data;
        
        Node nameNode = node.jjtGetChild(0);
        if (nameNode == null) {
            throw new ProgrammingError("Bad AST in function expression");
        }

        try {
            ESObject descriptor;
            if (nameNode instanceof ASTGetAccessor) {
                node.assertThreeChildren();

                String property = ((ASTIdentifier) node.jjtGetChild(1)).getName();
                descriptor = getDescriptor(properties, property, StandardProperty.VALUEstring, StandardProperty.GETstring);
                
                
                FunctionEvaluationSource fes = new FunctionEvaluationSource(
                        evaluationSource, property);
                ASTStatementList sl = (ASTStatementList) (node.jjtGetChild(2));
                List<String> variableNames = evaluator.getVarDeclarationVisitor()
                        .processVariableDeclarations(sl, fes);
                ConstructedFunctionObject cfo = ConstructedFunctionObject
                        .makeNewConstructedFunction(evaluator, property, fes, "",
                                new String[0], variableNames, sl,
                                evaluator.getScopeChain(), StrictMode.hasStrictModeDirective(sl));

                descriptor.putProperty(StandardProperty.GETstring, cfo, StandardProperty.GEThash);
            } else if (nameNode instanceof ASTSetAccessor) {
                node.assertFourChildren();

                String property = ((ASTIdentifier) node.jjtGetChild(1)).getName();
                descriptor = getDescriptor(properties, property, StandardProperty.VALUEstring, StandardProperty.SETstring);

                FunctionEvaluationSource fes = new FunctionEvaluationSource(
                        evaluationSource, property);
                ASTStatementList sl = (ASTStatementList) (node.jjtGetChild(3));
                List<String> variableNames = evaluator.getVarDeclarationVisitor()
                        .processVariableDeclarations(sl, fes);
                ConstructedFunctionObject cfo = ConstructedFunctionObject
                        .makeNewConstructedFunction(evaluator, property, fes, "",
                                new String[] { ((ASTIdentifier) node.jjtGetChild(2))
                                .getName() }, variableNames, sl, evaluator.getScopeChain(), StrictMode.hasStrictModeDirective(sl));

                descriptor.putProperty(StandardProperty.SETstring, cfo, StandardProperty.SEThash);
            } else {
                node.assertTwoChildren();
                String property;
                if (nameNode instanceof ASTIdentifier) {
                    ASTIdentifier identifier = (ASTIdentifier) nameNode;
                    property = identifier.getName();
                } else {
                    property = nameNode.jjtAccept(this, FOR_VALUE).toString();
                }
                if (evaluator.isStrictMode()) {
                    descriptor = getDescriptor(properties, property, StandardProperty.VALUEstring, StandardProperty.GETstring, StandardProperty.SETstring);
                } else {
                    descriptor = getDescriptor(properties, property, StandardProperty.GETstring, StandardProperty.SETstring);
                }
                
                ESValue value = (ESValue) node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
                descriptor.putProperty(StandardProperty.VALUEstring, value, StandardProperty.VALUEhash);
                descriptor.putProperty(StandardProperty.WRITABLEstring, ESBoolean.TRUE, StandardProperty.WRITABLEhash);
            }
            descriptor.putProperty(StandardProperty.CONFIGURABLEstring, ESBoolean.TRUE, StandardProperty.CONFIGURABLEhash);
            descriptor.putProperty(StandardProperty.ENUMERABLEstring, ESBoolean.TRUE, StandardProperty.ENUMERABLEhash);

        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return data;
    }
    
    private void assertDescriptorDoesntContain(ESObject descriptor, String ... propertyNames) throws EcmaScriptException {
        for (String propertyName : propertyNames) {
            if (descriptor.hasProperty(propertyName, propertyName.hashCode())) {
                throw new SyntaxError("Invalid object literal - Already defined "+propertyName);
            }
        }
    }

    private ESObject getDescriptor(ESObject properties, String property, String ... propertyNames)
            throws EcmaScriptException {
        ESValue descriptorValue = properties.getOwnProperty(property, property.hashCode());
        ESObject descriptor;
        if (descriptorValue == null) {
            descriptor = ObjectObject.createObject(evaluator);
            properties.putProperty(property, ESObject.WRITEABLE|ESObject.CONFIGURABLE|ESObject.ENUMERABLE, descriptor);
        } else {
            descriptor = (ESObject) descriptorValue;
            assertDescriptorDoesntContain(descriptor, propertyNames);
        }
        return descriptor;
    }

    @Override
    public Object visit(ASTFunctionExpression node, Object data) {
        int nChildren = node.jjtGetNumChildren();
        if (nChildren != 2 && nChildren != 3) {
            throw new ProgrammingError("Bad AST in function expression");
        }

        int child = 0;
        String procName = "<anonymous>";
        if (nChildren == 3) {
            ASTIdentifier idNode = (ASTIdentifier) (node.jjtGetChild(child++));
            procName = idNode.getName();
        }
        FunctionEvaluationSource fes = new FunctionEvaluationSource(
                evaluationSource, procName);

        ASTFormalParameterList fpl = (ASTFormalParameterList) (node
                .jjtGetChild(child));
        ASTStatementList sl = (ASTStatementList) (node.jjtGetChild(child+1));
        EcmaScriptVariableVisitor varDeclarationVisitor = evaluator.getVarDeclarationVisitor();
        List<String> variableNames = varDeclarationVisitor.processVariableDeclarations(sl, fes);

        try {
            ConstructedFunctionObject func = ConstructedFunctionObject
                    .makeNewConstructedFunction(evaluator, procName,
                            fes, "", fpl.getArguments(),
                            variableNames, sl, evaluator.getScopeChain(), node.isStrictMode());
            return func;
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
    }

    @Override
    public Object visit(ASTGetAccessor node, Object data) {
        return ESUndefined.theUndefined;
    }
    
    @Override
    public Object visit(ASTSetAccessor node, Object data) {
        return ESUndefined.theUndefined;
    }

    @Override
    public Object visit(ASTArrayLiteral node, Object data) {
        ESObject result = null;
        try {
            result = evaluator.createArray();
            
            int length = node.jjtGetNumChildren();
            for (int i = 0; i < length; i++) {
                Node child = node.jjtGetChild(i);
                if (!(child instanceof ASTElision)) {
                    result.putOwnProperty((long)i,(ESValue) child.jjtAccept(this, FOR_VALUE));
                    result.putProperty(StandardProperty.LENGTHstring, ESNumber.valueOf(i+1), StandardProperty.LENGTHhash);
                } else if (i == length-1) {
                    result.putProperty(StandardProperty.LENGTHstring, ESNumber.valueOf(length-1), StandardProperty.LENGTHhash);
                }
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }

    @Override
    public Object visit(ASTElision node, Object data) {
        return ESUndefined.theUndefined;
    }
    
    @Override
    public Object visit(ASTTryStatement node, Object data) {
        ASTCatch catchStatement = null;
        ASTFinally finallyStatement = null;
        int nChildren = node.jjtGetNumChildren();
        if (nChildren == 3){
            catchStatement = (ASTCatch) node.jjtGetChild(1);
            finallyStatement = (ASTFinally) node.jjtGetChild(2);
        } else if (nChildren == 2){
            Node child = node.jjtGetChild(1);
            if (child instanceof ASTCatch) {
                catchStatement = (ASTCatch) child;
            } else {
                finallyStatement = (ASTFinally) child;
            }
        } else {
            throw new ProgrammingError("Bad AST in try statement");
        }
        Object B,C,F;
        try {
            B = node.jjtGetChild(0).jjtAccept(this, data);
            data = B;
        } catch ( PackagedException e ) {
            if (catchStatement != null) {
                try {
                    C = catchStatement.jjtAccept(this, e.exception.getErrorObject(evaluator));
                } catch (EcmaScriptException eInCatch) {
                    throw new PackagedException(eInCatch, catchStatement);
                }
                data = C;
            }
        } finally {
            if (finallyStatement != null) {
                int blockCompletionCode = completionCode;
                completionCode = C_NORMAL;
                F = finallyStatement.jjtAccept(this, data);
                if (completionCode != C_NORMAL) {
                    data = F;
                } else {
                    completionCode = blockCompletionCode;
                }
            }
        }
        return data;
    }
    
    @Override
    public Object visit(ASTCatch node, Object data) {
        node.assertTwoChildren();
        ESValue result = null;
        try {
            EvaluationSource es = (EvaluationSource) node.getEvaluationSource();
            ASTIdentifier id = (ASTIdentifier) node.jjtGetChild(0);
            String propertyName = id.getName();
            ASTStatementList statementNode = (ASTStatementList) (node.jjtGetChild(1));
            
            ESObject scopeObject = ObjectObject.createObject(evaluator);
            List<String> lvn = new ArrayList<String>();
            scopeObject.putProperty(propertyName, (ESValue) data, propertyName.hashCode());
            
            EvaluationResult evaluationResult = evaluator.evaluateStatementList(statementNode, es, scopeObject, lvn, evaluator.getThisObject(),evaluator.getScopeChain(), new Evaluator.EvaluationResultBuilder() {
            public EvaluationResult getEvaluationResult(ESValue theValue,
                EcmaScriptEvaluateVisitor evaluationVisitor)
                        throws EcmaScriptException {
            int cc = evaluationVisitor.getCompletionCode();

            return new EvaluationResult(theValue,cc);
            }
         });
            result = evaluationResult.value;
            completionCode = evaluationResult.completionCode;
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        return result;
    }
    
    @Override
    public Object visit(ASTFinally node, Object data) {
        node.assertOneChild();
        return node.jjtGetChild(0).jjtAccept(this, data);
    }

    @Override
    public Object visit(ASTThrowStatement node, Object data) {
        node.assertOneChild();
        EcmaScriptException exception = new EcmaScriptException("throw");
        exception.setErrorObject((ESValue)node.jjtGetChild(0).jjtAccept(this, data));
        throw new PackagedException(exception, node);
    }
    
    
    private static class SwitchState {
        boolean found;
        ESValue input;
        ESValue result;
    }
    @Override
    public Object visit(ASTSwitchStatement node, Object data) {
        node.assertAtLeastOneChild();
        
        SwitchState switchState = new SwitchState();
        switchState.input = (ESValue) node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
        switchState.result = ESUndefined.theUndefined;
        int n = node.jjtGetNumChildren();
        int defaultNode = -1;
        for (int i=1; i<n && completionCode == C_NORMAL; i++) {
            Node child = node.jjtGetChild(i);
            if (child instanceof ASTCaseClause) {
                switchState.result = (ESValue) child.jjtAccept(this, switchState);
            } else if (switchState.found) {
                switchState.result = (ESValue) child.jjtAccept(this, switchState); 
            } else {
                defaultNode = i;
            }
        }
        if (!switchState.found) { 
            if (defaultNode != -1) {
                switchState.found = true;
                for (int i=defaultNode; i<n && completionCode == C_NORMAL; i++) {
                    Node child = node.jjtGetChild(i);
                    switchState.result = (ESValue) child.jjtAccept(this, switchState);
                }
           } else {
               switchState.result = ESUndefined.theUndefined;
            }
        }
        if (completionCode == C_BREAK) {
            completionCode = C_NORMAL;
        }
        return switchState.result;
    }
    
    @Override
    public Object visit(ASTCaseClause node, Object data) {
        node.assertAtLeastOneChild();
        int n = node.jjtGetNumChildren();
        
        SwitchState switchState = (SwitchState) data;
        ESValue clauseSelector = (ESValue) node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
        
        Object result;
        try {
            result = null;
            switchState.found = switchState.found || switchState.input.strictEqual(clauseSelector);
            if (switchState.found && n>1) {
                result = node.jjtGetChild(1).jjtAccept(this, data);
            }
        } catch (EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
        
        return result;
    }
    
    @Override
    public Object visit(ASTDefaultClause node, Object data) {
        if (node.jjtGetNumChildren() == 0) {
            return ((SwitchState)data).result;
        }
        return node.jjtGetChild(0).jjtAccept(this, data);
    }
    
    @Override
    public Object visit(ASTRegexp node, Object data) {
        try {
            return new RegExpPrototype(evaluator.getRegExpPrototype(), evaluator, node.getBody(), node.getFlags());
        } catch(EcmaScriptException e) {
            throw new PackagedException(e, node);
        }
    }
    
}