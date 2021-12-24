package main.visitor.type;

import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.types.NoType;
import main.ast.types.Type;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.IntType;
import main.compileError.typeError.UnsupportedOperandType;
import main.visitor.Visitor;

public class ExpressionTypeChecker extends Visitor<Type> {

    private String getOperand(BinaryOperator type)
    {
        if(type == BinaryOperator.eq)
            return "eq";
        if(type == BinaryOperator.gt)
            return "gt";
        if(type == BinaryOperator.lt)
            return "lt";
        if(type == BinaryOperator.add)
            return "add";
        if(type == BinaryOperator.sub)
            return "sub";
        if(type == BinaryOperator.mult)
            return "mult";
        if(type == BinaryOperator.div)
            return "div";
        if(type == BinaryOperator.and)
            return "and";
        if(type == BinaryOperator.or)
            return "or";
        if(type == BinaryOperator.assign)
            return "assign";
        return "";
    }
    @Override
    public Type visit(BinaryExpression binaryExpression) {
        //Todo
        Expression l = binaryExpression.getFirstOperand();
        Expression r = binaryExpression.getSecondOperand();
        Type lType = l.accept(this);
        Type rType = r.accept(this);
        BinaryOperator operator = binaryExpression.getBinaryOperator();
        if(operator == BinaryOperator.and || operator == BinaryOperator.or )
        {
            if(lType instanceof BoolType && rType instanceof BoolType)
            {
                return new BoolType();
            }
            else if((lType instanceof IntType && rType instanceof NoType) ||
                    (lType instanceof NoType && rType instanceof IntType) ||
                    (lType instanceof IntType && rType instanceof IntType)){
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(), getOperand(operator) ));
                return new NoType();
            }
        }
        else{
            if(lType instanceof IntType && rType instanceof IntType)
            {
                return new IntType();
            }
            else if((lType instanceof BoolType && rType instanceof NoType) ||
                    (lType instanceof NoType && rType instanceof BoolType) ||
                    (lType instanceof BoolType && rType instanceof BoolType)){
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),getOperand(operator) ));
                return new NoType();
            }
        }
        return new NoType();
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        //Todo
        Expression ex = unaryExpression.getOperand();
        Type exType = ex.accept(this);
        if(unaryExpression.getOperator() == UnaryOperator.not)
        {
            if(exType instanceof BoolType)
            {
                return new BoolType();
            }
            else if(exType instanceof IntType)
            {
                unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), "not"));
                return new NoType();
            }
        }else
        {
            if(exType instanceof IntType)
            {
                return new IntType();
            }
            else if(exType instanceof BoolType)
            {
                unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), "minus"));
                return new NoType();
            }
        }
        return new NoType();
    }

    @Override
    public Type visit(FunctionCall funcCall) {
        //Todo
        return null;
    }

    @Override
    public Type visit(Identifier identifier) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        //Todo
        return null;
    }

    @Override
    public Type visit(StructAccess structAccess) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ListSize listSize) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ListAppend listAppend) {
        //Todo
        return null;
    }

    @Override
    public Type visit(ExprInPar exprInPar) {
        //Todo
        return null;
    }

    @Override
    public Type visit(IntValue intValue) {
        //Todo
        return null;
    }

    @Override
    public Type visit(BoolValue boolValue) {
        //Todo
        return null;
    }
}
