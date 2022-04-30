package fl.instr.visitor;

import fl.utils.Constant;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MethodStmtCountVisitor extends ASTVisitor {
    private static String _name = "@MethodStmtCountVisitor ";

    protected CompilationUnit _cu;
    protected String _md;
    protected String _mName;
    protected String _cName;
    protected List<String> _mParalist = new ArrayList<>();

    public static int _start = -1;
    public static int _end = -1;

    public MethodStmtCountVisitor(CompilationUnit cu) {
        _cu = cu;
    }

    /**
     *
     * @param targetMethod: method identifier with type
     * @return
     */
    public String traverse(String targetMethod) {
        _md = targetMethod;
        // get name
        // "org.apache.commons.math.complex.Complex.add(org.apache.commons.math.complex.Complex)"
        _mName = _md.substring(0, _md.indexOf("("));
        _mName = _mName.substring(_mName.lastIndexOf('.')+1);

        _cName = _md.substring(0, _md.lastIndexOf(_mName));

        // "org.apache.commons.math.linear.OpenMapRealMatrix.<init>(int,int)"
        if(_mName.equals("<init>")) {
            String clazz = _md.substring(0, _md.indexOf(".<init>"));
            clazz = clazz.substring(clazz.lastIndexOf('.')+1);
            _mName = clazz;
        }
        // get para list
        List<String> tmp = Arrays.asList(_md.substring(_md.indexOf("(")+1, _md.indexOf(")")).split(","));
        for(String t : tmp) {
            if(!t.equals("")) {
                _mParalist.add(t);
            }
        }
        _cu.accept(this);
        String result = "" + _start + ":" + _end;
        return result;
    }

    @Override
    public boolean visit(MethodDeclaration node) {
        // check class
        String checkClazz = "";
        ASTNode parent = node.getParent();
        while (parent != null && !(parent instanceof CompilationUnit)) {
            if(parent instanceof TypeDeclaration) {
                String pName = ((TypeDeclaration) parent).getName().getIdentifier();
                checkClazz = pName + "." + checkClazz;
            }
            parent = parent.getParent();
        }
        if(parent instanceof CompilationUnit) {
            checkClazz = ((CompilationUnit) parent).getPackage().getName().getFullyQualifiedName() + "." + checkClazz;
        }
        if(!_cName.equals(checkClazz)) {
            return true;
        }
        String checkName = node.getName().getIdentifier();
        // check name
        if(_mName.equals(checkName)) {
            // check para types
            int paraSize = node.parameters().size();
            boolean flag = true;
            if(paraSize != _mParalist.size()) {
                flag = false;
            } else {
                for(int i=0; i<paraSize; i++) {
                    String checkType = ((SingleVariableDeclaration) node.parameters().get(i)).getType().toString();
                    // "org.apache.commons.math.complex.Complex" - Complex
                    String mp = _mParalist.get(i);
                    if(!(mp.equals(checkType))) {
                        mp = mp.replaceAll("\\$", ".");
                        mp = mp.replaceAll("#", ".");
                        if(mp.contains(".")) {
                            mp = mp.substring(mp.lastIndexOf('.')+1);
                        }
                        if(!mp.equals(checkType)) {
                            flag = false;
                            break;
                        }
                    }
                }
            }
            if(flag) {
                StatementCounterVisitor stmtCounter = new StatementCounterVisitor(node);
                List<Integer> linelist = stmtCounter.traverse();
                Collections.sort(linelist);
                _start = linelist.get(0).intValue();
                _end = linelist.get(linelist.size()-1).intValue();

                ASTNode par = node;
                while(par != null && !(par instanceof TypeDeclaration)) {
                    par = par.getParent();
                }
                if (par instanceof TypeDeclaration) {
                    Constant.CURRENT_CLZ_NODE = (TypeDeclaration) par;
                }
            }
            return false;
        }
        return true;
    }


    private class StatementCounterVisitor extends ASTVisitor {
        protected MethodDeclaration _md2;
        private List<Integer> _linelist = null;
        public StatementCounterVisitor(MethodDeclaration node) {
            _md2 = node;
        }
        public List<Integer> traverse() {
            _linelist = new ArrayList<>();
            _md2.accept(this);
            return _linelist;
        }
        private boolean addLineNo(Statement node) {
            int lineNo = _cu.getLineNumber(node.getStartPosition());
            _linelist.add(lineNo);
            return true;
        }
        @Override
        public boolean visit(Block node) { return addLineNo(node); }
        @Override
        public boolean visit(IfStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(ForStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(EnhancedForStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(WhileStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(DoStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(TryStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(SwitchStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(SwitchCase node) { return addLineNo(node); }
        @Override
        public boolean visit(SynchronizedStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(ReturnStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(ThrowStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(BreakStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(ContinueStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(EmptyStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(ExpressionStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(LabeledStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(AssertStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(VariableDeclarationStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(TypeDeclarationStatement node) { return addLineNo(node); }
        @Override
        public boolean visit(ConstructorInvocation node) { return addLineNo(node); }
        @Override
        public boolean visit(SuperConstructorInvocation node) { return addLineNo(node); }
    }
}
