package fl.instr.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class MethodDeclVisitor extends ASTVisitor {

    private final String _name = "@MethodDeclVisitor ";

    protected CompilationUnit _cu;
    protected String _methodName;
    protected String _returnType;
    protected String _clazz;
    protected List<String> _paraTypes = new ArrayList<>();
    protected MethodDeclaration _targetMethod = null;
    private static AST _ast = AST.newAST(AST.JLS8);

    public MethodDeclVisitor(CompilationUnit cu, String currentMethod) {
        _cu = cu;

        _clazz = currentMethod.split("#")[0];
        _returnType = currentMethod.split("#")[1];
        _methodName = currentMethod.split("#")[2];
        List<String> paraTypes_tmp = new ArrayList<>();
        for(String arg :currentMethod.split("#")[3].split(",")) {
            if(!arg.equals("?")) {
                paraTypes_tmp.add(arg);
            }
        }
        String buffer = "";
        for(String arg : paraTypes_tmp) {
            if(bracketValid(arg)) {
                _paraTypes.add(arg);
                continue;
            }
            if(buffer == "") {
                buffer += arg;
            } else {
                buffer += "," + arg;
            }
            if(bracketValid(buffer)) {
                _paraTypes.add(buffer);
                buffer = "";
            }
        }
        _paraTypes.size();
    }

    public static boolean bracketValid(String s) {
        Stack<Character> stack = new Stack();
        //取出字符串中的每一个括号
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            //如果遇到左括号，进行压栈，
            //如果遇到右括号，进行出栈
            switch (ch) {
                case '(':
                case '[':
                case '{':
                case '<':
                    stack.push(ch);
                    break;
                case ')':
                case '}':
                case ']':
                case '>':
                    //如果此时遇到了右括号
                    if (stack.isEmpty()) {
                        return false;
                    }
                    //如果此时栈中保存着左括号,看与右括号是否匹配
                    //移除栈顶元素
                    char left = stack.pop();
                    //如果不匹配
                    if (!((left == '(' && ch == ')') || (left == '{' && ch == '}') || (left == '[' && ch == ']') || (left == '<' && ch == '>'))) {
                        return false;
                    }
                    break;
                default:
                    break;
            }

        }
        //此时字符串中字符已经都走过了
        //栈中应该是空的
        if (stack.isEmpty()) {
            return true;
        } else {
            return false;
        }
    }

    public MethodDeclaration traverse() {
        _cu.accept(this);
        return _targetMethod;
    }

    @Override
    public boolean visit(MethodDeclaration node){
        String mname = node.getName().getIdentifier();
        // check name
        if(mname.equals(_methodName)) {
            // check clazz
            String clazz = "";
            ASTNode cursor = node.getParent();
            while(cursor != null) {
                String add = "";
                if(cursor instanceof TypeDeclaration) {
                    add = ((TypeDeclaration) cursor).getName().toString();
                } else if(cursor instanceof CompilationUnit) {
                    add = ((CompilationUnit) cursor).getPackage().getName().toString();
                }
                if(clazz.equals("")) {
                    clazz = add;
                } else {
                    clazz = add + "." + clazz;
                }
                cursor = cursor.getParent();
            }
            if(clazz.contains("$")) {
                clazz = clazz.replaceAll("\\$",".");
            }
            if(!clazz.equals(_clazz.replaceAll("\\$","."))) {
                return true;
            }
            // check return type
            String returnType;
            if(node.getReturnType2() == null) {
                returnType = "?";
            } else{
                returnType = node.getReturnType2().toString();
            }
            if(!returnType.equals(_returnType)) {
                return true;
            }
            // check para types
            List<ASTNode> paras = node.parameters();
            if(paras.size() == _paraTypes.size()) {
                for(int i=0; i<paras.size(); i++) {
                    String paras_type = ((SingleVariableDeclaration) paras.get(i)).getType().toString();
                    String _paraTypes_type = _paraTypes.get(i);
                    if(!_paraTypes_type.equals(paras_type)) {
                        return true;
                    }
                }
                _targetMethod = node;
                return false;
            }
        }
        return true;
    }

}
