package FESI.AST;

import FESI.Exceptions.ProgrammingError;

public class AbstractEcmaScriptVisitor implements EcmaScriptVisitor {

    public Object defaultAction(SimpleNode node, Object data) {
        throw new ProgrammingError("defaultAction not overridden");
    }
    
    public Object visit(SimpleNode node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTLiteral node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTIdentifier node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTThisReference node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTSuperReference node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTCompositeReference node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTFunctionExpression node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTFormalParameterList node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTFunctionCallParameters node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTPropertyValueReference node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTPropertyIdentifierReference node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTAllocationExpression node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTArrayLiteral node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTElision node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTObjectLiteral node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTPropertyNameAndValue node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTGetAccessor node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTSetAccessor node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTOperator node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTPostfixExpression node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTUnaryExpression node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTBinaryExpressionSequence node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTAndExpressionSequence node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTOrExpressionSequence node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTConditionalExpression node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTAssignmentExpression node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTExpressionList node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTStatementList node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTVariableDeclaration node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTIfStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTWhileStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTForStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTEmptyExpression node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTForVarStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTForInStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTForVarInStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTContinueStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTBreakStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTReturnStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTWithStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTFunctionDeclaration node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTProgram node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTTryStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTCatch node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTFinally node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTThrowStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTSwitchStatement node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTCaseClause node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTDefaultClause node, Object data) {
        return defaultAction(node,data);
    }

    public Object visit(ASTDoWhileStatement node, Object data) {
        return defaultAction(node,data);
    }
    
    public Object visit(ASTRegexp node, Object data) {
        return defaultAction(node, data);
    }
    
}