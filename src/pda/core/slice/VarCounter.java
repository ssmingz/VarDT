package pda.core.slice;

import java.io.File;

import org.eclipse.jdt.core.dom.*;
import org.eclipse.jdt.internal.compiler.lookup.VariableBinding;

import pda.common.utils.JavaFile;

public class VarCounter {
    public static void main(String[] args) {
        String dir = args[0];
        String path = args[1];
        int targetLine = Integer.valueOf(args[2]).intValue();
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("Source file doesn't exist");
            return;
        }
        String source = JavaFile.readFileToString(file);
        CompilationUnit cu = JavaFile.genASTFromSource(source, file.getAbsolutePath(), dir);
        LineVisitor visitor = new LineVisitor(cu, targetLine);
        int result = visitor.traverse();
        System.out.println(result);
    }

    public static int countVars(String adir, String apath, int line) {
        String dir = adir;
        String path = apath;
        int targetLine = line;
        File file = new File(path);
        if (!file.exists()) {
            System.out.println("Source file doesn't exist");
            return -1;
        }
        String source = JavaFile.readFileToString(file);
        CompilationUnit cu = JavaFile.genASTFromSource(source, file.getAbsolutePath(), dir);
        LineVisitor visitor = new LineVisitor(cu, targetLine);
        int result = visitor.traverse();
        System.out.println("var count result in " + dir + "/" + path + ":" + result);
        return result;
    }

    private static class LineVisitor extends ASTVisitor {
        protected CompilationUnit _cu;

        protected int _line;

        protected int _counter = 0;

        public LineVisitor(CompilationUnit cu, int targetLine) {
            this._cu = cu;
            this._line = targetLine;
        }

        public int traverse() {
            this._cu.accept(this);
            return this._counter;
        }

        private boolean checkLineNumber(Statement node) {
            int lineNo = this._cu.getLineNumber(node.getStartPosition());
            if (lineNo == this._line) {
                VarVisitor vv = new VarVisitor((ASTNode)node);
                this._counter = vv.traverse();
                // get target MethodDeclaration
                ASTNode cursor = node;
                while(!(cursor instanceof MethodDeclaration) && cursor != null) {
                    cursor = cursor.getParent();
                }
                if (cursor instanceof MethodDeclaration) {
                    SlicerMain._CURRENT_METHOD_DECL = (MethodDeclaration) cursor;
                }
                return false;
            }
            return true;
        }

        public boolean visit(Block node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(IfStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(ForStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(EnhancedForStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(WhileStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(DoStatement node) {
            int lineNo = this._cu.getLineNumber(node.getExpression().getStartPosition());
            if (lineNo == this._line) {
                VarVisitor vv = new VarVisitor((ASTNode)node.getExpression());
                this._counter = vv.traverse();
                return false;
            }
            return true;
        }

        public boolean visit(TryStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(SwitchStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(SwitchCase node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(SynchronizedStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(ReturnStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(ThrowStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(BreakStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(ContinueStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(EmptyStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(ExpressionStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(LabeledStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(AssertStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(VariableDeclarationStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(TypeDeclarationStatement node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(ConstructorInvocation node) {
            return checkLineNumber((Statement)node);
        }

        public boolean visit(SuperConstructorInvocation node) {
            return checkLineNumber((Statement)node);
        }
    }

    private static class VarVisitor extends ASTVisitor {
        protected ASTNode _stmt;

        protected int _count = 0;

        public VarVisitor(ASTNode stmt) {
            this._stmt = stmt;
        }

        public int traverse() {
            this._stmt.accept(this);
            return this._count;
        }

        public boolean visit(SimpleName node) {
            IBinding binding = node.resolveBinding();
            if (binding != null && binding.getKind() == IBinding.VARIABLE) {
                if (!((IVariableBinding)binding).isEnumConstant() && ((IVariableBinding)binding).getConstantValue() == null) {
                    this._count++;
                }
            }
            return true;
        }
    }
}
