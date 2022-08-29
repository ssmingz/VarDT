package fl.utils;

import org.eclipse.jdt.core.dom.*;

import java.io.File;

/**
 * Used for counting the number of vars for a given line of a given .java file
 * 1st para - dir path
 * 2nd para - file path
 * 3rd para - line
 */
public class VarCounter {
    public static void main(String[] args) {
        String dir = args[0];
        String path = args[1];
        int targetLine = Integer.valueOf(args[2]);

        File file = new File(path);
        if(!file.exists()) {
            System.out.println("Source file doesn't exist");
            return;
        }

        // get the target ast
        String source = JavaFile.readFileToString(file);
        CompilationUnit cu = JavaFile.genASTFromSource(source, file.getAbsolutePath(), dir);
        LineVisitor visitor = new LineVisitor(cu, targetLine);
        int result = visitor.traverse();
        System.out.println(result);
    }

    private static class LineVisitor extends ASTVisitor {
        protected CompilationUnit _cu;
        protected int _line;
        protected int _counter = 0;

        public LineVisitor(CompilationUnit cu, int targetLine) {
            _cu = cu;
            _line = targetLine;
        }

        public int traverse() {
            _cu.accept(this);
            return _counter;
        }

        private boolean checkLineNumber(Statement node) {
            int lineNo = _cu.getLineNumber(node.getStartPosition());
            if(lineNo == _line) {
                VarVisitor vv = new VarVisitor(node);
                _counter = vv.traverse();
                return false;
            }
            return true;
        }

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
            int lineNo = _cu.getLineNumber(node.getExpression().getStartPosition());
            if(lineNo == _line) {
                VarVisitor vv = new VarVisitor((ASTNode)node.getExpression());
                _counter = vv.traverse();
                return false;
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

    private static class VarVisitor extends ASTVisitor {
        protected ASTNode _stmt;
        protected int _count = 0;
        public VarVisitor(ASTNode stmt) {
            _stmt = stmt;
        }
        public int traverse() {
            _stmt.accept(this);
            return _count;
        }
        @Override
        public boolean visit(SimpleName node) {
            if(node.resolveBinding() instanceof IVariableBinding) {
                _count++;
            }
            return true;
        }
    }
}
