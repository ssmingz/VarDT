package fl.instr.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

/**
 * @description:
 * @author:
 * @time: 2021/8/28 0:53
 */
public class InsertVisitor extends ASTVisitor {
    private final String _name = "@InsertVisitor ";

    protected MethodDeclaration _targetMethod;
    private static AST _ast = AST.newAST(AST.JLS8);
    private List<Statement> _inserted;
    private Statement _location;


    public InsertVisitor(MethodDeclaration method, List<Statement> inserted, Statement location) {
        _targetMethod = method;
        _inserted = inserted;
        _location = location;
    }

    public void traverse() {
        _targetMethod.accept(this);
        return ;
    }

    @Override
    public boolean visit(Block node) {
        List<Statement> checked = node.statements();
        if(checked.contains(_location)) {
            int loc = _inserted.indexOf(_location);
            List<Statement> newinsert = ASTNode.copySubtrees(node.getAST(), _inserted);
            checked.addAll(checked.indexOf(_location), newinsert);
            checked.remove(newinsert.get(loc));
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(SwitchStatement node) {
        List<Statement> checked = node.statements();
        if(checked.contains(_location)) {
            int loc = _inserted.indexOf(_location);
            List<Statement> newinsert = ASTNode.copySubtrees(node.getAST(), _inserted);
            checked.addAll(checked.indexOf(_location), newinsert);
            checked.remove(newinsert.get(loc));
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(IfStatement node) {
        Block newBlock = node.getAST().newBlock();
        int loc = _inserted.indexOf(_location);
        List<Statement> newinsert = ASTNode.copySubtrees(node.getAST(), _inserted);
        newinsert.add(_location);
        newinsert.remove(loc);
        if(node.getThenStatement() == _location) {
            node.setThenStatement(newBlock);
            newBlock.statements().addAll(newinsert);
            return false;
        } else if(node.getElseStatement() == _location) {
            node.setElseStatement(newBlock);
            newBlock.statements().addAll(newinsert);
            return false;
        }
        return true;
    }
}
