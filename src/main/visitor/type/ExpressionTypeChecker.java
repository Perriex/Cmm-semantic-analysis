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
    private boolean isStatement = false;
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

    public void setAsStatement()
    {
        isStatement = true;
    }

    public void setAsNoneStatement()
    {
        isStatement = false;
    }

    private boolean checkLists(ListType l, ListType r)
    {
        if(l.getType() instanceof IntType && r.getType() instanceof IntType)
            return true;
        if(l.getType() instanceof BoolType && r.getType() instanceof BoolType)
            return true;
        if(l.getType() instanceof StructType && r.getType() instanceof StructType)
            return true;
        return l.getType() instanceof ListType && r.getType() instanceof ListType;
    }
    private boolean checkTwoArrayType(ArrayList<Type> a,ArrayList<Type> b)
    {
        for(int i = 0;i <a.size();i++)
        {
            if((b.get(i) instanceof BoolType && a.get(i) instanceof BoolType ) ||
                    (b.get(i) instanceof IntType && a.get(i) instanceof IntType ) ||
                    (b.get(i) instanceof StructType && a.get(i) instanceof StructType ) ||
                    (b.get(i) instanceof ListType && a.get(i) instanceof ListType ) ||
                    (b.get(i) instanceof FptrType && a.get(i) instanceof FptrType )) {
            }else{
                return false;
            }
        }
        return true;
    }
    private boolean checkFptrs(FptrType l, FptrType r)
    {
        if(l.getReturnType() instanceof IntType && !(r.getReturnType() instanceof IntType))
            return false;
        if(l.getReturnType() instanceof BoolType && !(r.getReturnType() instanceof BoolType))
            return false;
        if(l.getReturnType() instanceof StructType && !(r.getReturnType() instanceof StructType))
            return false;
        if(l.getReturnType() instanceof ListType && !(r.getReturnType() instanceof ListType))
            return false;
        if(l.getReturnType() instanceof VoidType && !(r.getReturnType() instanceof VoidType))
            return false;
        if(l.getReturnType() instanceof FptrType && !(r.getReturnType() instanceof FptrType))
            return false;
        if(l.getArgsType().size() != r.getArgsType().size())
            return false;
        return checkTwoArrayType(l.getArgsType(),r.getArgsType());
    }
    @Override
    public Type visit(BinaryExpression binaryExpression) {
        Type lType = binaryExpression.getFirstOperand().accept(this);
        Type rType = binaryExpression.getSecondOperand().accept(this);
        boolean both = false;
        if(lType instanceof VoidType )
        {
            both = true;
            lType = new NoType();
        }
        if(rType instanceof VoidType )
        {
            rType = new NoType();
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
                    (lType instanceof FptrType && rType instanceof FptrType && checkFptrs((FptrType)lType, (FptrType)rType)) ||
                    (lType instanceof ListType && rType instanceof ListType && checkLists((ListType)lType,(ListType)rType)))
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
                    (lType instanceof FptrType && rType instanceof FptrType) )
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
        if(exType instanceof VoidType || unaryExpression.getOperand() instanceof ListAppend ){
            exType =new NoType();
        }
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
            if(!(insType instanceof NoType))
                funcCall.addError(new CallOnNoneFptrType(funcCall.getLine()));
            return new NoType();
        }
        if (((FptrType)insType).getReturnType() instanceof VoidType && !isStatement)
        {
            funcCall.addError(new CantUseValueOfVoidFunction(funcCall.getLine()));
        }
        if(((FptrType) insType).getArgsType().size() > 0 && ((FptrType) insType).getArgsType().get(0) instanceof VoidType && funcCall.getArgs().size() == 0)
        {
            return ((FptrType) insType).getReturnType();
        }
        ArrayList<Type> args = new ArrayList<>();
        for(Expression arg :funcCall.getArgs())
        {
            Type item = arg.accept(this);
            args.add(item);
        }
        if(args.size() != ((FptrType) insType).getArgsType().size())
        {
            funcCall.addError(new ArgsInFunctionCallNotMatchDefinition(funcCall.getLine()));
            return ((FptrType) insType).getReturnType();
        }
        FptrType original = new FptrType(args,((FptrType) insType).getReturnType());
        if(!TypeChecker.isEqual(insType, original)){
            funcCall.addError(new ArgsInFunctionCallNotMatchDefinition(funcCall.getLine()));
            return ((FptrType) insType).getReturnType();
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
        if (!(indexType instanceof IntType || indexType instanceof NoType))
        {
            listAccessByIndex.addError(new ListIndexNotInt(listAccessByIndex.getLine()));
        }
         if (!(instType instanceof ListType || instType instanceof NoType) )
        {
            listAccessByIndex.addError(new AccessByIndexOnNonList(listAccessByIndex.getLine()));
        }
        return new NoType();
    }
private Type checkType(VariableSymbolTableItem id)
{
    if( id.getType() instanceof StructType){
        try {
            SymbolTable.root.getItem(StructSymbolTableItem.START_KEY + id.getName());
            return id.getType();
        }catch (ItemNotFoundException ex){
            return new NoType();
        }
    }
    else{
        return id.getType();
    }
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
                return checkType(element);
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
        return new NoType();
    }

    @Override
    public Type visit(ListAppend listAppend) {
        Type listType = listAppend.getListArg().accept(this);
        if(!isStatement)
        {
            listAppend.addError(new CantUseValueOfVoidFunction(listAppend.getLine()));
        }
        isStatement = false;
        if(!(listType instanceof ListType) && !(listType instanceof NoType))
        {
            listAppend.addError(new AppendToNonList(listAppend.getLine()));
            return new NoType();
        }
        Type listEl = listAppend.getElementArg().accept(this);
        if(listType instanceof NoType)
            return new NoType();
        Type listEls = ((ListType) listType).getType();
        if((listEl instanceof BoolType && listEls instanceof BoolType ) ||
                (listEl instanceof IntType && listEls instanceof IntType ) ||
                (listEl instanceof StructType && listEls instanceof StructType ) ||
                (listEl instanceof ListType && listEls instanceof ListType && checkLists((ListType)listEl, (ListType)listEls) ) )
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