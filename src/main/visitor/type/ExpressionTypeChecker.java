package main.visitor.type;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.operators.UnaryOperator;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.types.*;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.IntType;
import main.ast.types.primitives.VoidType;
import main.compileError.typeError.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.FunctionSymbolTableItem;
import main.symbolTable.items.StructSymbolTableItem;
import main.symbolTable.items.SymbolTableItem;
import main.symbolTable.items.VariableSymbolTableItem;
import main.visitor.Visitor;
import java.util.ArrayList;

public class ExpressionTypeChecker extends Visitor<Type> {
    enum Types {
        BOOL,INT,ALL,EQ
    }
    private Types getTypeOfOperands(BinaryOperator operator)
    {
        if(operator == BinaryOperator.and || operator == BinaryOperator.or)
        {
            return Types.BOOL;
        }
        if(operator == BinaryOperator.assign)
        {
            return Types.ALL;
        }
        if(operator == BinaryOperator.eq)
        {
            return Types.EQ;
        }
        return Types.INT;
    }
    @Override
    public Type visit(BinaryExpression binaryExpression) {
        Type lType = binaryExpression.getFirstOperand().accept(this);
        Type rType = binaryExpression.getSecondOperand().accept(this);
        boolean both = false;
        if(lType instanceof VoidType )
        {
            binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
            both = true;
        }
        if(rType instanceof VoidType )
        {
            binaryExpression.addError(new CantUseValueOfVoidFunction(binaryExpression.getLine()));
            if(both)
                return new NoType();
        }
        BinaryOperator operator = binaryExpression.getBinaryOperator();
        Types types = getTypeOfOperands(operator);
        if(types == Types.ALL)
        {
            if((lType instanceof IntType && rType instanceof IntType) ||
                    (lType instanceof BoolType && rType instanceof BoolType) ||
                    (lType instanceof StructType && rType instanceof StructType) ||
                    (lType instanceof FptrType && rType instanceof FptrType) ||
                    (lType instanceof ListType && rType instanceof ListType))
            {
                return rType;
            }
            else if (!(lType instanceof NoType) && !(rType instanceof NoType)){
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),operator.name()));
            }
            return new NoType();
        }
        if(types == Types.EQ)
        {
            if(lType instanceof ListType || rType instanceof ListType)
            {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),operator.name()));
            }
            else if((lType instanceof IntType && rType instanceof IntType) ||
                    (lType instanceof BoolType && rType instanceof BoolType) ||
                    (lType instanceof StructType && rType instanceof StructType) ||
                    (lType instanceof FptrType && rType instanceof FptrType))
            {
                return new BoolType();
            }
            else if (!(lType instanceof NoType) && !(rType instanceof NoType)){
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),operator.name()));
            }
            return new NoType();
        }
        if(types == Types.INT)
        {
            if((lType instanceof IntType && rType instanceof IntType))
            {
                if(operator == BinaryOperator.lt || operator == BinaryOperator.eq ||
                    operator == BinaryOperator.gt  )
                    return new BoolType();
                return new IntType();
            }
            else if (!(lType instanceof NoType && rType instanceof IntType) &&
                    !(lType instanceof IntType && rType instanceof NoType) &&
                    !(lType instanceof NoType && rType instanceof NoType))
            {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),operator.name()));
            }
            return new NoType();
        }
        if(types == Types.BOOL)
        {
            if((lType instanceof BoolType && rType instanceof BoolType))
            {
                return new BoolType();
            }
            else if (!(lType instanceof NoType && rType instanceof BoolType) &&
                    !(lType instanceof BoolType && rType instanceof NoType) &&
                    !(lType instanceof NoType && rType instanceof NoType))
            {
                binaryExpression.addError(new UnsupportedOperandType(binaryExpression.getLine(),operator.name()));
            }
            return new NoType();
        }
        return new NoType();
    }

    @Override
    public Type visit(UnaryExpression unaryExpression) {
        Type exType =  unaryExpression.getOperand().accept(this);
        if (unaryExpression.getOperator() == UnaryOperator.not)
        {
            if (exType instanceof BoolType)
            {
                return new BoolType();
            }
            else if(exType instanceof IntType)
            {
                unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), "not"));
            }
        }
        else
        {
            if(exType instanceof IntType)
            {
                return new IntType();
            }
            else if(exType instanceof BoolType)
            {
                unaryExpression.addError(new UnsupportedOperandType(unaryExpression.getLine(), "minus"));
            }
        }
        return new NoType();
    }

    @Override
    public Type visit(FunctionCall funcCall) {
        Type insType = funcCall.getInstance().accept(this);
        if(!(insType instanceof FptrType))
        {
            funcCall.addError(new CallOnNoneFptrType(funcCall.getLine()));
            return new NoType();
        }
        ArrayList<Type> args = new ArrayList<>();
        for(Expression arg :funcCall.getArgs())
        {
            Type item = arg.accept(this);
            if (item instanceof VoidType)
            {
                funcCall.addError(new CantUseValueOfVoidFunction(funcCall.getLine()));
            }
            args.add(item);
        }
        if(args.size() != funcCall.getArgs().size())
        {
            funcCall.addError(new ArgsInFunctionCallNotMatchDefinition(funcCall.getLine()));
            return ((FptrType) insType).getReturnType();
        }
        for(int i = 0;i <funcCall.getArgs().size();i++)
        {
            if(!((args.get(i) instanceof BoolType && ((FptrType) insType).getArgsType().get(i) instanceof BoolType ) ||
                    (args.get(i) instanceof IntType && ((FptrType) insType).getArgsType().get(i) instanceof IntType ) ||
                    (args.get(i) instanceof StructType && ((FptrType) insType).getArgsType().get(i) instanceof StructType ) ||
                    (args.get(i) instanceof ListType && ((FptrType) insType).getArgsType().get(i) instanceof ListType ) ||
                    (args.get(i) instanceof FptrType && ((FptrType) insType).getArgsType().get(i) instanceof FptrType )) ) {
                funcCall.addError(new ArgsInFunctionCallNotMatchDefinition(funcCall.getLine()));
                return ((FptrType) insType).getReturnType();
            }
        }
        return ((FptrType) insType).getReturnType();
    }

    @Override
    public Type visit(Identifier identifier) {
        try
        {
            SymbolTableItem item = SymbolTable.top.getItem( VariableSymbolTableItem.START_KEY+identifier.getName());
            Type id =  ((VariableSymbolTableItem) item).getType();
            if(id instanceof StructType)
            {
                Identifier nameStruct = ((StructType) id).getStructName();
                try
                {
                    SymbolTable.top.getItem( StructSymbolTableItem.START_KEY+nameStruct.getName());
                    return id;
                }
                catch (ItemNotFoundException ex)
                {
                    return new NoType();
                }
            }
            else {
                return id;
            }

        }
        catch (ItemNotFoundException ex) {
            try {
                FunctionSymbolTableItem item = (FunctionSymbolTableItem) SymbolTable.top.getItem(FunctionSymbolTableItem.START_KEY + identifier.getName());
                return new FptrType(item.getArgTypes(), item.getReturnType());
            }catch (ItemNotFoundException ex2)
            {
                identifier.addError(new VarNotDeclared(identifier.getLine(), identifier.getName()));
            }
        }
        return new NoType();
    }

    @Override
    public Type visit(ListAccessByIndex listAccessByIndex) {
        Type instType = listAccessByIndex.getInstance().accept(this);
        Type indexType = listAccessByIndex.getIndex().accept(this);
        if (instType instanceof ListType && indexType instanceof IntType)
        {
            return ((ListType) instType).getType();
        }
        else if (instType instanceof ListType && !(indexType instanceof NoType))
        {
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
        }
        else if (!(instType instanceof NoType) && indexType instanceof IntType)
        {
            listAccessByIndex.addError(new AccessByIndexOnNonList(listAccessByIndex.getLine()));
        }
        return new NoType();
    }

    @Override
    public Type visit(StructAccess structAccess) {
        Type instType = structAccess.getInstance().accept(this);
        if(instType instanceof NoType)
        {
            return new NoType();
        }
        if (!(instType instanceof StructType))
        {
            structAccess.addError(new AccessOnNonStruct(structAccess.getLine()));
            return new NoType();
        }
        String varName = structAccess.getElement().getName();
        String structName = ((StructType) instType).getStructName().getName();
        try
        {
            StructSymbolTableItem struct = (StructSymbolTableItem) SymbolTable.root.getItem(StructSymbolTableItem.START_KEY+structName);
            SymbolTable structTable = struct.getStructSymbolTable();
            try
            {
                VariableSymbolTableItem element = (VariableSymbolTableItem) structTable.getItem(VariableSymbolTableItem.START_KEY+varName);
                return element.getType();
            }
            catch (ItemNotFoundException ex)
            {
                structAccess.addError(new StructMemberNotFound(structAccess.getLine(),structName,varName));
                return new NoType();
            }
        }
        catch (ItemNotFoundException ex)
        {
            return new NoType();
        }
    }

    @Override
    public Type visit(ListSize listSize) {
        Type list = listSize.getArg().accept(this);
        if(list instanceof ListType)
        {
            return new IntType();
        }
        if(!(list instanceof NoType))
        {
            listSize.addError(new GetSizeOfNonList(listSize.getLine()));
        }
        return null;
    }

    @Override
    public Type visit(ListAppend listAppend) {
        Type listType = listAppend.getListArg().accept(this);
        if(!(listType instanceof ListType))
        {
            listAppend.addError(new AppendToNonList(listAppend.getLine()));
            return new NoType();
        }
        Type listEl = listAppend.getElementArg().accept(this);
        Type listEls = ((ListType) listType).getType();
        if((listEl instanceof BoolType && listEls instanceof BoolType ) ||
                (listEl instanceof IntType && listEls instanceof IntType ) ||
                (listEl instanceof StructType && listEls instanceof StructType ) ||
                (listEl instanceof ListType && listEls instanceof ListType ) )
        {
            return new VoidType();
        }
        if(!(listEls instanceof NoType))
        {
            listAppend.addError(new NewElementTypeNotMatchListType(listAppend.getLine()));
        }
        return new NoType();
    }

    @Override
    public Type visit(ExprInPar exprInPar) {
        return exprInPar.getInputs().get(0).accept(this);
    }

    @Override
    public Type visit(IntValue intValue) {
        return new IntType();
    }

    @Override
    public Type visit(BoolValue boolValue) {
        return new BoolType();
    }
}