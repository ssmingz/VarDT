package fl.instr.visitor;

import org.eclipse.jdt.core.dom.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @description:
 * @author:
 * @time: 2021/8/21 18:13
 */
public class MethodDecVisitor extends ASTVisitor {
    private final String _name = "@MethodDecVisitor ";

    protected CompilationUnit _cu;
    protected String _methodName;
    protected List<String> _paraTypes;
    protected MethodDeclaration _targetMethod;
    private static AST _ast = AST.newAST(AST.JLS8);

    public MethodDecVisitor(CompilationUnit cu, String methodName, List<String> paras) {
        _cu = cu;
        _methodName = methodName;
        _paraTypes = paras;
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
            // check para types
            List<SingleVariableDeclaration> paras = node.parameters();
            if(paras.size() == _paraTypes.size()) {
                for(int i=0; i<paras.size(); i++) {
                    String paras_type = paras.get(i).getType().toString();
                    String _paraTypes_type = _paraTypes.get(i);
                    if(!_paraTypes_type.contains(paras_type)) {
                        // TODO: special case for generic type
                        // for now, only consider "paras: (T[] array1, T... array2) --> _paraTypes: (Object[], Object[])"
                        if(paras_type.equals("T[]") || paras_type.equals("T")) { }
                        // also like "paras: (Predicate<Node>) --> _paraTypes: (com.google.common.base.Predicate)"
                        if(paras_type.contains("<") && paras_type.contains(">")) {
                            if(_paraTypes_type.contains(paras_type.substring(0, paras_type.indexOf("<")))) { }
                            else { return true; }
                        }
                        // special case for lang-13
                        if(_paraTypes_type.equals("java.io.Serializable") && paras_type.equals("T")) { }
                        else { return true; }
                    }
                }
                _targetMethod = node;
                return false;
            }
        }
        return true;
    }
}
