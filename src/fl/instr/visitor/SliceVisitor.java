package fl.instr.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.*;

/**
 * @description: traverse compilation unit to obtain statement-list in the slice result
 * @author:
 * @time: 2021/7/23 13:11
 */
public class SliceVisitor extends ASTVisitor {
    private final String _name = "@SliceVisitor ";

    protected CompilationUnit _cu;
    protected List<Integer> _sliceRes;
    private Set<Integer> _checkedList;
    protected LinkedHashMap<Statement, Integer> _stmtList = new LinkedHashMap<>();

    public SliceVisitor(CompilationUnit cu, List<Integer> sliceRes) {
        _cu = cu;
        _sliceRes = sliceRes;
        _checkedList = new HashSet<>(sliceRes);
    }

    /**
     * traverse compilation unit by ASTVisitor
     * @return
     */
    public LinkedHashMap<Statement, Integer> traverse() {
        _cu.accept(this);
        return _stmtList;
    }

    /**
     * check whether the statement is what we're looking for
     * @param node : current traversing ast node
     * @return
     */
    private boolean checkLineNumber(Statement node) {
        int lineNo = _cu.getLineNumber(node.getStartPosition());
        if(_checkedList.isEmpty()) {
            return false;
        }
        if(_checkedList.contains(lineNo)) {
            _stmtList.put(node, lineNo);
            _checkedList.remove(lineNo);
        }
        return true;
    }

    /**
     * kinds of statements:
     * Block, IfStmt, ForStmt, EnhancedForStmt, WhileStmt, DoStmt,
     * TryStmt, SwitchCase, SwitchStmt, SynchronizedStmt, ReturnStmt, ThrowStmt,
     * BreakStmt, ThrowStmt, BreakStmt, ContinueStmt, EmptyStmt,
     * ExpressionStmt, LabeledStmt, AssertStmt,
     * VariableDeclarationStmt, TypeDeclarationStmt,
     * ConstructorInvocation, SuperConstructorInvocation
     */

    @Override
    public boolean visit(Block node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(IfStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(ForStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(WhileStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(DoStatement node) {
        int lineNo = _cu.getLineNumber(node.getStartPosition());
        if(_checkedList.isEmpty()) {
            return false;
        }
        if(_checkedList.contains(lineNo)) {
            _stmtList.put(node, lineNo);
            _checkedList.remove(lineNo);
        }
        // check doWhile exp
        lineNo = _cu.getLineNumber(node.getExpression().getStartPosition());
        if(_checkedList.contains(lineNo)) {
            _stmtList.put(node, lineNo);
            _checkedList.remove(lineNo);
        }
        return true;
    }

    @Override
    public boolean visit(TryStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(SwitchStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(SwitchCase node) { return checkLineNumber(node); }

    @Override
    public boolean visit(SynchronizedStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(ReturnStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(ThrowStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(BreakStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(ContinueStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(EmptyStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(ExpressionStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(LabeledStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(AssertStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(VariableDeclarationStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(TypeDeclarationStatement node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(ConstructorInvocation node) {
        return checkLineNumber(node);
    }

    @Override
    public boolean visit(SuperConstructorInvocation node) {
        return checkLineNumber(node);
    }
}
