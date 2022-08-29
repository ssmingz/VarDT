package fl.instr.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.List;

/**
 * @description:
 * @author:
 * @time: 2021/8/21 19:52
 */
public class LocalNameVisitor extends ASTVisitor {
    private final String _name = "@LocalNameVisitor ";

    protected String _lineNo;
    protected CompilationUnit _cu;
    protected MethodDeclaration _targetMethod;
    private static AST _ast = AST.newAST(AST.JLS8);
    private String _original_name = null;

    public LocalNameVisitor(CompilationUnit cu, MethodDeclaration method, String line) {
        _cu = cu;
        _targetMethod = method;
        _lineNo = line;
    }

    public String traverse() {
        _targetMethod.accept(this);
        return _original_name;
    }

    @Override
    public boolean visit(Assignment node) {
        String curLine = "" + _cu.getLineNumber(node.getStartPosition());
        if(curLine.equals(_lineNo)) {
            Expression left = node.getLeftHandSide();
            _original_name = left.toString();
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(VariableDeclarationFragment node) {
        String curLine = "" + _cu.getLineNumber(node.getStartPosition());
        if(curLine.equals(_lineNo)) {
            _original_name = node.getName().getIdentifier();
            return false;
        }
        return true;
    }

    @Override
    public boolean visit(SingleVariableDeclaration node) {
        String curLine = "" + _cu.getLineNumber(node.getStartPosition());
        if(curLine.equals(_lineNo)) {
            _original_name = node.getName().getIdentifier();
            return false;
        }
        return true;
    }
}
