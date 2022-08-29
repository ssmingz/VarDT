package fl.instr.visitor;

import org.eclipse.jdt.core.dom.*;

public class CheckInitVisitor extends ASTVisitor {
    private static String _name = "@CheckInitVisitor ";

    private MethodDeclaration _method;

    private String _targetVarName;
    private int _targetVarPos;
    private int _curLine;
    private boolean _initValid = false;

    public CheckInitVisitor(MethodDeclaration md) {
        _method = md;
    }

    public void traverse(String target, int line) {
        _targetVarName = target;
        _targetVarPos = line;
        _method.accept(this);
    }

    public boolean initValid(String identifier, int line) {
        traverse(identifier, line);
        return _initValid;
    }

    // TODO: how to handle all possible init cases
    // more than Assignment/argument/declaration, method invocation can also initialize
    @Override
    public boolean visit(Assignment node) {
        Expression assigned = node.getLeftHandSide();
        if (node.getRoot() instanceof CompilationUnit) {
            // check line
            if (_curLine < _targetVarPos) {
                // check name
                if (assigned.toString().equals(_targetVarName)) {
                    _initValid = true;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        Expression assigned = node.getName();
        if (node.getInitializer() == null) {
            return true;
        }
        if (node.getRoot() instanceof CompilationUnit) {
            // check line
            if (_curLine < _targetVarPos) {
                // check name
                if (assigned.toString().equals(_targetVarName)) {
                    _initValid = true;
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        Expression assigned = node.getName();
        if (node.getParent() instanceof MethodDeclaration) {
            // check if argument
            if (((MethodDeclaration) node.getParent()).parameters().contains(node)) {
                // check name
                if (assigned.toString().equals(_targetVarName)) {
                    _initValid = true;
                    return false;
                }
            }
        }
        return true;
    }

    private boolean setLine(Statement node) {
        _curLine = Integer.MAX_VALUE;
        if (node.getRoot() instanceof CompilationUnit) {
            _curLine = ((CompilationUnit) node.getRoot()).getLineNumber(node.getStartPosition());
        }
        return true;
    }
    @Override
    public boolean visit(Block node) { return setLine(node); }
    @Override
    public boolean visit(IfStatement node) { return setLine(node); }
    @Override
    public boolean visit(ForStatement node) { return setLine(node); }
    @Override
    public boolean visit(EnhancedForStatement node) { return setLine(node); }
    @Override
    public boolean visit(WhileStatement node) { return setLine(node); }
    @Override
    public boolean visit(DoStatement node) { return setLine(node); }
    @Override
    public boolean visit(TryStatement node) { return setLine(node); }
    @Override
    public boolean visit(SwitchStatement node) { return setLine(node); }
    @Override
    public boolean visit(SwitchCase node) { return setLine(node); }
    @Override
    public boolean visit(SynchronizedStatement node) { return setLine(node); }
    @Override
    public boolean visit(ReturnStatement node) { return setLine(node); }
    @Override
    public boolean visit(ThrowStatement node) { return setLine(node); }
    @Override
    public boolean visit(BreakStatement node) { return setLine(node); }
    @Override
    public boolean visit(ContinueStatement node) { return setLine(node); }
    @Override
    public boolean visit(EmptyStatement node) { return setLine(node); }
    @Override
    public boolean visit(ExpressionStatement node) { return setLine(node); }
    @Override
    public boolean visit(LabeledStatement node) { return setLine(node); }
    @Override
    public boolean visit(AssertStatement node) { return setLine(node); }
    @Override
    public boolean visit(VariableDeclarationStatement node) { return setLine(node); }
    @Override
    public boolean visit(TypeDeclarationStatement node) { return setLine(node); }
    @Override
    public boolean visit(ConstructorInvocation node) { return setLine(node); }
    @Override
    public boolean visit(SuperConstructorInvocation node) { return setLine(node); }
}
