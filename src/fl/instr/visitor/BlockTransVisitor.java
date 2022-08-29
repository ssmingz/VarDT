package fl.instr.visitor;

import org.eclipse.jdt.core.dom.*;

/**
 * @description:
 * @author:
 * @time: 2021/8/18 11:24
 */
public class BlockTransVisitor extends ASTVisitor {
    private static String _name = "@BlockTransVisitor ";

    private ASTNode _targetRegion;
    private static AST _ast = AST.newAST(AST.JLS8);

    public BlockTransVisitor(ASTNode targetRegion) {
        _targetRegion = targetRegion;
    }

    public void traverse() {
        _targetRegion.accept(this);
    }

    @Override
    public boolean visit(IfStatement node) {
        Statement thenBody = node.getThenStatement();
        Statement elseBody = node.getElseStatement();
        if((thenBody != null) && !(thenBody instanceof Block)) {
            // reset then stmt to block
            Block newBlock = node.getAST().newBlock();
            node.setThenStatement(newBlock);
            newBlock.statements().add(thenBody);
        }
        if((elseBody != null) && !(elseBody instanceof Block)) {
            if(elseBody instanceof IfStatement) {
                // consider else if stmt, which is IfStmt type in its parent's else part
                // else if;else ===> else { if;else }
                // IfStmt ==> Block containing IfStmt
                Block newBlock = node.getAST().newBlock();
                node.setElseStatement(newBlock);
                newBlock.statements().add(elseBody);
                return true;
            }
            // reset else stmt to block
            Block newBlock = node.getAST().newBlock();
            node.setElseStatement(newBlock);
            newBlock.statements().add(elseBody);
        }
        return true;
    }

    @Override
    public boolean visit(WhileStatement node) {
        Statement body = node.getBody();
        if((body != null) && !(body instanceof Block)) {
            Block newBlock = node.getAST().newBlock();
            node.setBody(newBlock);
            newBlock.statements().add(body);
        }
        return true;
    }

    @Override
    public boolean visit(DoStatement node) {
        Statement body = node.getBody();
        if((body != null) && !(body instanceof Block)) {
            Block newBlock = node.getAST().newBlock();
            node.setBody(newBlock);
            newBlock.statements().add(body);
        }
        return true;
    }

    @Override
    public boolean visit(ForStatement node) {
        Statement body = node.getBody();
        if((body != null) && !(body instanceof Block)) {
            Block newBlock = node.getAST().newBlock();
            node.setBody(newBlock);
            newBlock.statements().add(body);
        }
        return true;
    }

    @Override
    public boolean visit(EnhancedForStatement node) {
        Statement body = node.getBody();
        if((body != null) && !(body instanceof Block)) {
            Block newBlock = node.getAST().newBlock();
            node.setBody(newBlock);
            newBlock.statements().add(body);
        }
        return true;
    }
}
