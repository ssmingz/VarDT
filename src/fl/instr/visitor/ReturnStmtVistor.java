package fl.instr.visitor;

import fl.utils.Constant;
import fl.utils.JavaLogger;
import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.Set;

/**
 * @description:
 * @author:
 * @time: 2021/8/18 10:18
 */
public class ReturnStmtVistor extends ASTVisitor {
    private static String _name = "@ReturnStmtVistor ";

    protected ASTNode _targetToVisit;
    protected ASTNode _targetToInsert;
    protected String _currentTestName;
    private static AST _ast = AST.newAST(AST.JLS8);

    protected int _afterLine = 0;
    protected CompilationUnit _cu;

    public ReturnStmtVistor(ASTNode visitedRegion, String testName) {
        _currentTestName = testName;
        _targetToVisit = visitedRegion;
    }

    public ReturnStmtVistor(ASTNode visitedRegion, int afterLine) {
        _targetToVisit = visitedRegion;
        _cu = (CompilationUnit) visitedRegion.getRoot();
        _afterLine = afterLine;
    }

    public void traverse(ASTNode insertedStmt) {
        _targetToInsert = insertedStmt;
        _targetToVisit.accept(this);
    }

    @Override
    public boolean visit(ReturnStatement node) {
        // check lineNo
        if(_afterLine != 0) {
            int lineNo = _cu.getLineNumber(node.getStartPosition());
            if(lineNo < _afterLine) {
                return true;
            }
        }

        // filter inner methods of current test method
        ASTNode cur = node.getParent();
        while(!(cur instanceof MethodDeclaration) && !(cur instanceof CompilationUnit) && cur!=null) {
            cur = cur.getParent();
        }
        if(cur instanceof CompilationUnit) {
            JavaLogger.error("get MethodDeclaration node of a ReturnStmt node failed : " + Constant.PROJECT_ID + " " + Constant.BUG_ID + " " + Constant.CURRENT_METHOD);
            return true;
        }
        if(cur!=null && !((MethodDeclaration) cur).getName().toString().equals(_currentTestName)) {
            return true;
        }
        // find the block that the return node belongs to
        ASTNode block = node.getParent();
        if(block == null) {
            JavaLogger.error(_name + "#visit No parent exists for this return node : " + ((CompilationUnit) node.getRoot()).getLineNumber(node.getStartPosition()));
            return true;
        }
        if(block instanceof Block) {
            int insertPos = ((Block) block).statements().indexOf(node);
            ((Block) block).statements().add(insertPos, ASTNode.copySubtree(block.getAST(), _targetToInsert));
        }
        else if(block instanceof SwitchStatement) {
            int insertPos = ((SwitchStatement) block).statements().indexOf(node);
            ((SwitchStatement) block).statements().add(insertPos, ASTNode.copySubtree(block.getAST(), _targetToInsert));
        }
        return true;
    }
}
