package fl.instr.visitor;

import fl.instr.Instrument;
import fl.utils.JavaFile;
import fl.utils.JavaLogger;
import org.eclipse.jdt.core.dom.*;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * @description: traverse compilation unit to obtain MethodDeclaration-list in the test class
 * @author:
 * @time: 2021/7/28 20:40
 */
public class TestMethodVisitor extends ASTVisitor {
    private final String _name = "@TestMethodVisitor ";

    protected CompilationUnit _cu;
    protected Set<String> _methodNames, _uncheckedMethods;
    protected HashSet<MethodDeclaration> _methodList = new HashSet<>();
    private static AST _ast = AST.newAST(AST.JLS8);
    protected String _projectPath;
    protected String _currentTestFile;


    public TestMethodVisitor(CompilationUnit cu, Set<String> methodNames, String projectPath) {
        _cu = cu;
        _methodNames = methodNames;
        _uncheckedMethods = methodNames;
        _projectPath = projectPath;
    }

    public TestMethodVisitor(CompilationUnit cu, Set<String> methodNames, String projectPath, String currentTestFile) {
        _cu = cu;
        _methodNames = methodNames;
        _uncheckedMethods = methodNames;
        _projectPath = projectPath;
        _currentTestFile = currentTestFile;
    }

    /**
     * traverse compilation unit by ASTVisitor
     * @return
     */
    public HashSet<MethodDeclaration> traverse() {
        _cu.accept(this);
        if(!_uncheckedMethods.isEmpty()) {
            _cu.accept(new SuperClassVisitor());
            for(String name : _uncheckedMethods) {
                JavaLogger.error(_name + "#traverse Test method not found : " + name + " " + _projectPath);
            }
        }
        return _methodList;
    }

    @Override
    public boolean visit(MethodDeclaration node){
        return checkMethodName(node);
    }

    @Override
    public boolean visit(TypeDeclaration node) {
        /**
         * isMemberTypeDeclaration() is a convenience method that simply checks whether
         * this node's parent is a type declaration node or an anonymous class declaration
         */
        if(_uncheckedMethods.isEmpty()) {
            return false;
        }
        if(!node.isInterface() && !node.isMemberTypeDeclaration() && !node.isLocalTypeDeclaration()) {
            insertLoggerInit(node, _projectPath);
        }
        return true;
    }

    public static void insertLoggerInit(TypeDeclaration node, String projectPath) {
        boolean alreadyInsert = false;
        for(Object f : node.getFields()) {
            if(((FieldDeclaration) f).toString().trim().equals("org.apache.log4j.Logger logger_log4j=org.apache.log4j.Logger.getLogger(\"\");")) {
                alreadyInsert = true;
                break;
            }
        }
        if(alreadyInsert) {
           return;
        }
        // 1. initialize "Logger logger = Logger.getLogger("")"
        VariableDeclarationFragment vdf = _ast.newVariableDeclarationFragment();
        vdf.setName(_ast.newSimpleName("logger_log4j"));
        MethodInvocation loggerInit = _ast.newMethodInvocation();
        //loggerInit.setExpression(_ast.newSimpleName("Logger"));
        loggerInit.setExpression(_ast.newName("org.apache.log4j.Logger"));
        loggerInit.setName(_ast.newSimpleName("getLogger"));
        StringLiteral arg0 = _ast.newStringLiteral();
        arg0.setLiteralValue("");
        loggerInit.arguments().add(arg0);
        vdf.setInitializer(loggerInit);
        FieldDeclaration fd = _ast.newFieldDeclaration(vdf);
        //fd.setType(_ast.newSimpleType(_ast.newName("Logger")));
        fd.setType(_ast.newNameQualifiedType(_ast.newName("org.apache.log4j"), _ast.newSimpleName("Logger")));
        // 2. set configuration file "static{PropertyConfigurator.configure("src/log4j.properties");}"
        MethodInvocation m1 = _ast.newMethodInvocation();
        m1.setExpression(_ast.newSimpleName("PropertyConfigurator"));
        m1.setName(_ast.newSimpleName("configure"));
        StringLiteral arg1 = _ast.newStringLiteral();
        String properties = JavaFile.getSrcClasses(projectPath) + "/instrument-log4j.properties";
        //String properties = "/mnt/hgfs/FaultLocalization/EXP/buggyProjects/chart/chart_20_buggy/source" + "/instrument-log4j.properties";
        arg1.setLiteralValue(properties);
        m1.arguments().add(arg1);
        ExpressionStatement call1 = _ast.newExpressionStatement(m1);
        Block block = _ast.newBlock();
        block.statements().add(call1);
        Initializer init = _ast.newInitializer();
        init.setBody(block);
        Modifier mod = _ast.newModifier(Modifier.ModifierKeyword.STATIC_KEYWORD);
        init.modifiers().add(mod);
        // insert
        node.bodyDeclarations().add(0, ASTNode.copySubtree(node.getAST(), init));
        node.bodyDeclarations().add(0, ASTNode.copySubtree(node.getAST(), fd));
    }

    private boolean checkMethodName(MethodDeclaration node) {
        String name = node.getName().getIdentifier();
        if(_methodNames.contains(name)) {
            // check if body is empty
            if(!node.getBody().statements().isEmpty()) {
                _methodList.add(node);
                _uncheckedMethods.remove(name);
            }
        }
        return true;
    }

    private class SuperClassVisitor extends ASTVisitor {
        @Override
        public boolean visit(TypeDeclaration node) {
            if(_uncheckedMethods.isEmpty()) {
                return false;
            }
            if(node.getSuperclassType() == null) {
                return true;
            }
            // TODO: handle superInterfaceTypes
            String superClass = node.getSuperclassType().toString();
            if(superClass.contains("<")) {
                superClass = superClass.substring(0, superClass.indexOf("<"));
            }
            String packageName = _cu.getPackage().getName().getFullyQualifiedName();
            for(Object imp : _cu.imports()) {
                if (((ImportDeclaration) imp).getName().getFullyQualifiedName().endsWith(superClass)) {
                    packageName = ((ImportDeclaration) imp).getName().getFullyQualifiedName();
                    break;
                }
            }
            String fullpath;
            if(packageName.endsWith(superClass)) {
                fullpath = JavaFile.getSrcTests(_projectPath) + "/" + packageName.replaceAll("\\.","/") + ".java";
            } else {
                fullpath = JavaFile.getSrcTests(_projectPath) + "/" + packageName.replaceAll("\\.","/") + "/" + superClass +".java";
            }
            File superFile = new File(fullpath);
            if(!superFile.exists()) {
                JavaLogger.warn(_name + "#visit Super class of " + node.getName().toString() + " : " + superClass + " file not found");
                return true;
            }
            Instrument.instrumentSuperClazzTestMethod(superFile, _uncheckedMethods, _projectPath); // _uncheckedMethods will change, but no need to return MethodDecl since already instrument at superclass
            return true;
        }
    }
}
