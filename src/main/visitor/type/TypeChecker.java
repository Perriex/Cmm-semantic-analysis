package main.visitor.type;

import main.ast.nodes.Node;
import main.ast.nodes.Program;
import main.ast.nodes.declaration.Declaration;
import main.ast.nodes.declaration.FunctionDeclaration;
import main.ast.nodes.declaration.MainDeclaration;
import main.ast.nodes.declaration.VariableDeclaration;
import main.ast.nodes.declaration.struct.StructDeclaration;
import main.ast.nodes.expression.*;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.statement.*;
import main.ast.types.*;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.IntType;
import main.ast.types.primitives.VoidType;
import main.compileError.typeError.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.StructSymbolTableItem;
import main.symbolTable.items.VariableSymbolTableItem;
import main.visitor.Visitor;

import java.util.ArrayList;
import java.util.Stack;

class Scope {
    boolean hasReturn = false;
}

public class TypeChecker extends Visitor<Void> {
    ExpressionTypeChecker expressionTypeChecker;
    Identifier RETID = new Identifier("#RETURN");
    Scope top = new Scope();
    Stack<Scope> scopes;

    private void addScope(SymbolTable pre){
        SymbolTable.push(new SymbolTable(pre));
        top = new Scope();
        scopes.push(top);
    }

    private void removeScope() {
        SymbolTable.pop();
        scopes.pop();
        top = scopes.peek();
    }

    boolean noDeclare = false;
    boolean typeError = false;

    public TypeChecker() {
        this.expressionTypeChecker = new ExpressionTypeChecker();
        this.scopes = new Stack<>();
        scopes.push(top);
    }

    private void checkType(ListType type, Node node)
    {
        checkType(type.getType(), node);
    }

    private void checkType(FptrType type, Node node)
    {
        for (Type innerType : type.getArgsType()) {
            checkType(innerType, node);
        }
        checkType(type.getReturnType(), node);
    }

    private void checkType(StructType type, Node node) {
        try {
            SymbolTable.root.getItem(StructSymbolTableItem.START_KEY + type.getStructName().getName());
        } catch (ItemNotFoundException e) {
            typeError = true;
            node.addError(new StructNotDeclared(node.getLine(), type.getStructName().getName()));
        }
    }

    private void checkType(Type type, Node node)
    {
        if(type instanceof StructType) {
            checkType((StructType) type, node);
        }
        if(type instanceof ListType){
            checkType((ListType) type, node);
        }
        if(type instanceof FptrType){
            checkType((FptrType) type, node);
        }
    }

    static boolean isEqual(StructType type1, StructType type2)
    {
        return type1.getStructName().getName().equals(type2.getStructName().getName());
    }

    static boolean isEqual(FptrType type1, FptrType type2)
    {
        if(type1.getArgsType().size() != type2.getArgsType().size())
            return false;
        if(!isEqual(type1.getReturnType(), type2.getReturnType()))
            return false;
        for (int i =0; i < type1.getArgsType().size(); i++) {
            if(!isEqual(type1.getArgsType().get(i), type2.getArgsType().get(i)))
                return false;
        }
        return true;
    }

    static boolean isEqual(ListType type1, ListType type2) {
        return isEqual(type1.getType(), type2.getType());
    }

    static boolean isEqual(Type type1, Type type2)
    {
        if(type1 instanceof NoType || type2 instanceof NoType)
            return true;
        if(!type1.getClass().equals(type2.getClass()))
            return false;
        if(type1 instanceof StructType) {
            return isEqual((StructType) type1, (StructType) type2);
        }
        if(type1 instanceof ListType){
            return isEqual((ListType) type1, (ListType) type2);
        }
        if(type1 instanceof FptrType){
            return isEqual((FptrType) type1, (FptrType) type2);
        }
        return true;
    }

    @Override
    public Void visit(Program program) {
        for (StructDeclaration struct : program.getStructs()) {
            struct.accept(this);
        }
        for (FunctionDeclaration function : program.getFunctions()) {
            function.accept(this);
        }
        program.getMain().accept(this);
        return null;
    }

    @Override
    public Void visit(FunctionDeclaration functionDec) {
        addScope(SymbolTable.root);
        typeError = false;
        checkType(functionDec.getReturnType(), functionDec);
        var returnItem = new VariableSymbolTableItem(RETID);
        returnItem.setType(typeError ? new NoType() : functionDec.getReturnType());
        try {
            SymbolTable.top.put(returnItem);
        } catch (ItemAlreadyExistsException ignore) {
        }
        for (VariableDeclaration arg : functionDec.getArgs()) {
            arg.accept(this);
        }
        functionDec.getBody().accept(this);
        if(!(top.hasReturn || functionDec.getReturnType() instanceof VoidType))
        {
            functionDec.addError(new MissingReturnStatement(functionDec.getLine(), functionDec.getFunctionName().getName()));
        }
        removeScope();
        return null;
    }

    @Override
    public Void visit(MainDeclaration mainDec) {
        SymbolTable.push(new SymbolTable(SymbolTable.root));
        mainDec.getBody().accept(this);
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(VariableDeclaration variableDec) {
        typeError = false;
        var item = new VariableSymbolTableItem(variableDec.getVarName());
        checkType(variableDec.getVarType(), variableDec);
        item.setType(typeError ? new NoType() : variableDec.getVarType());
        try {
            SymbolTable.top.put(item);
        } catch (ItemAlreadyExistsException ignore) {
        }
        return null;
    }

    @Override
    public Void visit(StructDeclaration structDec) {
        SymbolTable.push(new SymbolTable(SymbolTable.root));
        structDec.getBody().accept(this);
        try {
            var structItem = SymbolTable.root.getItem(StructSymbolTableItem.START_KEY+structDec.getStructName().getName());
            ((StructSymbolTableItem)structItem).setStructSymbolTable(SymbolTable.top);
        } catch (ItemNotFoundException ignore) {
        }
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(SetGetVarDeclaration setGetVarDec) {
        noDeclare = true;
        typeError = false;
        checkType(setGetVarDec.getVarType(), setGetVarDec);
        var item = new VariableSymbolTableItem(setGetVarDec.getVarName());
        item.setType(typeError ? new NoType() : setGetVarDec.getVarType());
        try {
            SymbolTable.top.put(item);
        } catch (ItemAlreadyExistsException ignore) {
        }
        SymbolTable.push(new SymbolTable(SymbolTable.top));
        for (VariableDeclaration arg : setGetVarDec.getArgs()) {
            arg.accept(this);
        }
        setGetVarDec.getSetterBody().accept(this);
        SymbolTable.pop();
        SymbolTable.push(new SymbolTable(SymbolTable.top));
        var returnItem = new VariableSymbolTableItem(RETID);
        returnItem.setType(setGetVarDec.getVarType());
        try {
            SymbolTable.top.put(returnItem);
        } catch (ItemAlreadyExistsException ignore) {
        }
        setGetVarDec.getGetterBody().accept(this);
        SymbolTable.pop();
        //added this
        ArrayList<Type> args = new ArrayList<>();
        for(VariableDeclaration i: setGetVarDec.getArgs()){
            args.add(i.getVarType());
        }
        item.setType(new FptrType(args, setGetVarDec.getVarType()));
        try {
            SymbolTable.top.put(item);
        } catch (ItemAlreadyExistsException ignore) {
        }
        noDeclare = false;
        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        var lexpr = assignmentStmt.getLValue();
        if (!(lexpr instanceof StructAccess || lexpr instanceof Identifier || lexpr instanceof ListAccessByIndex ||
                (lexpr instanceof ExprInPar && ((ExprInPar) lexpr).getInputs().size() == 1))) {
            assignmentStmt.addError(new LeftSideNotLvalue(lexpr.getLine()));
        }
        expressionTypeChecker.setAsStatement();
        var ltype = assignmentStmt.getLValue().accept(expressionTypeChecker);
        expressionTypeChecker.setAsNoneStatement();
        var rtype = assignmentStmt.getRValue().accept(expressionTypeChecker);
        if(ltype instanceof FptrType && assignmentStmt.getRValue() instanceof ExprInPar) {
            var rfptr = new FptrType(new ArrayList<>(), ((FptrType)ltype).getReturnType());
            ((ExprInPar) assignmentStmt.getRValue()).getInputs().forEach(expression -> rfptr.addArgType(expression.accept(expressionTypeChecker)));
            if(isEqual(rfptr, ltype)) return null;
        }
        if (!isEqual(ltype, rtype) && !(ltype instanceof VoidType)) {
            assignmentStmt.addError(new UnsupportedOperandType(assignmentStmt.getLine(), BinaryOperator.assign.toString()));
        }
        return null;
    }

    @Override
    public Void visit(BlockStmt blockStmt) {
        for (Statement statement : blockStmt.getStatements()) {
            statement.accept(this);
        }
        return null;
    }

    @Override
    public Void visit(ConditionalStmt conditionalStmt) {
        var conditionType = conditionalStmt.getCondition().accept(expressionTypeChecker);
        if (!isEqual(conditionType, new BoolType())) {
            conditionalStmt.addError(new ConditionNotBool(conditionalStmt.getCondition().getLine()));
        }

        addScope(SymbolTable.top);
        conditionalStmt.getThenBody().accept(this);
        var hasReturn = top.hasReturn;
        removeScope();
        if (conditionalStmt.getElseBody() != null) {
            addScope(SymbolTable.top);
            conditionalStmt.getElseBody().accept(this);
            hasReturn = top.hasReturn && hasReturn;
            removeScope();
        }

        top.hasReturn = hasReturn && (conditionalStmt.getElseBody() != null) || top.hasReturn;

        return null;
    }

    @Override
    public Void visit(FunctionCallStmt functionCallStmt) {
        expressionTypeChecker.setAsStatement();
        functionCallStmt.getFunctionCall().accept(expressionTypeChecker);
        expressionTypeChecker.setAsNoneStatement();
        return null;
    }

    @Override
    public Void visit(DisplayStmt displayStmt) {
        var type = mustBeValue(displayStmt.getArg());
        if (!(type instanceof BoolType || type instanceof IntType || type instanceof ListType || type instanceof NoType)) {
            displayStmt.addError(new UnsupportedTypeForDisplay(displayStmt.getLine()));
        }
        return null;
    }

    private Type mustBeValue(Expression expression)
    {
        var type = expression.accept(expressionTypeChecker);
        if(type instanceof VoidType ){
            return new NoType();
        }
        if(expression instanceof StructAccess && type instanceof FptrType)//added
            return ((FptrType) type).getReturnType();
        return type;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        top.hasReturn = true;
        VariableSymbolTableItem item = null;
        var retType = returnStmt.getReturnedExpr() == null ? new VoidType() : returnStmt.getReturnedExpr().accept(expressionTypeChecker);
        if(retType instanceof FptrType && ((FptrType) retType).getArgsType().size() == 0){
            ((FptrType) retType).addArgType(new VoidType());
        }
        try {
            item = (VariableSymbolTableItem) SymbolTable.top.getItem(VariableSymbolTableItem.START_KEY + RETID.getName());
        } catch (ItemNotFoundException ignore) {
            returnStmt.addError(new CannotUseReturn(returnStmt.getLine()));
            return null;
        }
        if (returnStmt.getReturnedExpr() == null && !(item.getType() instanceof VoidType)) {
            returnStmt.addError(new ReturnValueNotMatchFunctionReturnType(returnStmt.getLine()));
        } else {
            if (!isEqual(retType, item.getType())) {
                returnStmt.addError(new ReturnValueNotMatchFunctionReturnType(returnStmt.getLine()));
            }
        }
        return null;
    }

    @Override
    public Void visit(LoopStmt loopStmt) {
        var conditionType = loopStmt.getCondition().accept(expressionTypeChecker);
        if (!isEqual(conditionType, new BoolType())) {
            loopStmt.addError(new ConditionNotBool(loopStmt.getCondition().getLine()));
        }
        addScope(SymbolTable.top);
        loopStmt.getBody().accept(this);
        removeScope();
        return null;
    }

    @Override
    public Void visit(VarDecStmt varDecStmt) {
        if (noDeclare) {
            varDecStmt.addError(new CannotUseDefineVar(varDecStmt.getLine()));
        }
        for (VariableDeclaration var : varDecStmt.getVars()) {
            var item = new VariableSymbolTableItem(var.getVarName());
            typeError = false;
            checkType(var.getVarType(), varDecStmt);
            item.setType(typeError ? new NoType() : var.getVarType());
            try {
                SymbolTable.top.put(item);
            } catch (ItemAlreadyExistsException ignore) {
            }
            if (var.getDefaultValue() != null) {
                var type = mustBeValue(var.getDefaultValue());
                if (!isEqual(type, var.getVarType()) && !(type instanceof NoType)) {
                    var.addError(new UnsupportedOperandType(var.getLine(), BinaryOperator.assign.toString()));
                    var.addError(new UnsupportedOperandType(var.getLine(), BinaryOperator.assign.toString()));
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(ListAppendStmt listAppendStmt) {
        expressionTypeChecker.setAsStatement();
        listAppendStmt.getListAppendExpr().accept(expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(ListSizeStmt listSizeStmt) {
        listSizeStmt.getListSizeExpr().accept(expressionTypeChecker);
        return null;
    }
}
