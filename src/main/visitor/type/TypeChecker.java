package main.visitor.type;

import main.ast.nodes.Program;
import main.ast.nodes.declaration.*;
import main.ast.nodes.declaration.struct.*;
import main.ast.nodes.expression.Identifier;
import main.ast.nodes.expression.operators.BinaryOperator;
import main.ast.nodes.expression.values.primitive.BoolValue;
import main.ast.nodes.expression.values.primitive.IntValue;
import main.ast.nodes.statement.*;
import main.ast.types.ListType;
import main.ast.types.primitives.BoolType;
import main.ast.types.primitives.IntType;
import main.ast.types.primitives.VoidType;
import main.compileError.typeError.*;
import main.symbolTable.SymbolTable;
import main.symbolTable.exceptions.ItemAlreadyExistsException;
import main.symbolTable.exceptions.ItemNotFoundException;
import main.symbolTable.items.VariableSymbolTableItem;
import main.visitor.ErrorReporter;
import main.visitor.Visitor;
import parsers.CmmParser;

public class TypeChecker extends Visitor<Void> {
    ExpressionTypeChecker expressionTypeChecker;
    Identifier RETID = new Identifier("#RETURN");
    public TypeChecker(){
        this.expressionTypeChecker = new ExpressionTypeChecker();
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
        SymbolTable.push(new SymbolTable(SymbolTable.root));
        functionDec.getBody().accept(this);
        SymbolTable.pop();
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
        var item = new VariableSymbolTableItem(variableDec.getVarName());
        item.setType(variableDec.getVarType());
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
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(SetGetVarDeclaration setGetVarDec) {
        SymbolTable.push(new SymbolTable(SymbolTable.top));
        var item = new VariableSymbolTableItem(setGetVarDec.getVarName());
        item.setType(setGetVarDec.getVarType());
        try {
            SymbolTable.top.put(item);
        } catch (ItemAlreadyExistsException ignore) {
        }
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
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(AssignmentStmt assignmentStmt) {
        var lexpr = assignmentStmt.getLValue();
        if(lexpr instanceof IntValue || lexpr instanceof BoolValue){
            assignmentStmt.addError(new LeftSideNotLvalue(lexpr.getLine()));
        }
        var ltype = assignmentStmt.getLValue().accept(expressionTypeChecker);
        var rtype = assignmentStmt.getRValue().accept(expressionTypeChecker);
        if(!ltype.getClass().equals(rtype.getClass()))
        {
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
        if (!(conditionType instanceof BoolType))
        {
            conditionalStmt.addError(new ConditionNotBool(conditionalStmt.getCondition().getLine()));
        }
        SymbolTable.push(new SymbolTable(SymbolTable.top));
        conditionalStmt.getThenBody().accept(this);
        SymbolTable.pop();
        if(conditionalStmt.getElseBody() != null)
        {
            SymbolTable.push(new SymbolTable(SymbolTable.top));
            conditionalStmt.getElseBody().accept(this);
            SymbolTable.pop();
        }
        return null;
    }

    @Override
    public Void visit(FunctionCallStmt functionCallStmt) {
        functionCallStmt.getFunctionCall().accept(expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(DisplayStmt displayStmt) {
        var type = displayStmt.getArg().accept(expressionTypeChecker);
        if(!(type instanceof BoolType || type instanceof IntType || type instanceof ListType))
        {
            displayStmt.addError(new UnsupportedTypeForDisplay(displayStmt.getLine()));
        }
        return null;
    }

    @Override
    public Void visit(ReturnStmt returnStmt) {
        VariableSymbolTableItem item = null;
        try {
            item = (VariableSymbolTableItem)SymbolTable.top.getItem(VariableSymbolTableItem.START_KEY+"ret");
        } catch (ItemNotFoundException ignore) {
            returnStmt.addError(new CannotUseReturn(returnStmt.getLine()));
            return null;
        }

        if(returnStmt.getReturnedExpr() == null && !(item.getType() instanceof VoidType))
        {
            returnStmt.addError(new ReturnValueNotMatchFunctionReturnType(returnStmt.getLine()));
        }
        else {
            var retType = returnStmt.getReturnedExpr().accept(expressionTypeChecker);
            if (!retType.getClass().equals(item.getType().getClass())){
                returnStmt.addError(new ReturnValueNotMatchFunctionReturnType(returnStmt.getLine()));
            }
        }
        return null;
    }

    @Override
    public Void visit(LoopStmt loopStmt) {
        var conditionType = loopStmt.getCondition().accept(expressionTypeChecker);
        if (!(conditionType instanceof BoolType))
        {
            loopStmt.addError(new ConditionNotBool(loopStmt.getCondition().getLine()));
        }
        SymbolTable.push(new SymbolTable(SymbolTable.top));
        loopStmt.getBody().accept(this);
        SymbolTable.pop();
        return null;
    }

    @Override
    public Void visit(VarDecStmt varDecStmt) {
        for (VariableDeclaration var : varDecStmt.getVars()) {
            var item = new VariableSymbolTableItem(var.getVarName());
            item.setType(var.getVarType());
            try {
                SymbolTable.top.put(item);
            } catch (ItemAlreadyExistsException ignore) {
            }
            if(var.getDefaultValue() != null)
            {
                var type = var.getDefaultValue().accept(expressionTypeChecker);
                if (!type.toString().equals(var.getVarType().toString()))
                {
                    var.addError(new UnsupportedOperandType(var.getLine(), BinaryOperator.assign.toString()));
                }
            }
        }
        return null;
    }

    @Override
    public Void visit(ListAppendStmt listAppendStmt) {
        listAppendStmt.getListAppendExpr().accept(expressionTypeChecker);
        return null;
    }

    @Override
    public Void visit(ListSizeStmt listSizeStmt) {
        listSizeStmt.getListSizeExpr().accept(expressionTypeChecker);
        return null;
    }
}
