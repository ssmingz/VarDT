package pda.core.dependency;

import org.eclipse.jdt.core.dom.*;
import pda.common.conf.Constant;
import pda.common.java.D4jSubject;
import pda.common.utils.JavaFile;
import pda.common.utils.LevelLogger;
import pda.common.utils.Pair;
import pda.common.utils.Utils;

import java.io.File;
import java.util.*;

public class ParseMethodFromString {
    private static Map<String, MethodDeclaration> methodDeclarationMap = new HashMap<>();
    private static Map<String, CompilationUnit> compilationUnitMap = new HashMap<>();

    public static List<Pair<MethodDeclaration, String>> parseMethodString(D4jSubject subject, List<String> methodStrings){
        List<Pair<MethodDeclaration, String>> result = new ArrayList<>();
        for (int i = 0;i < methodStrings.size();i++){
            result.add(parseSingleMethodString(subject, methodStrings.get(i)));
            if (i % 100 == 0){
                System.out.println("Progress : " + i + "/" + methodStrings.size());
            }
        }
        System.out.println("Parse finish!!!");
        return result;
    }

    private static CompilationUnit getCompilationUnit(D4jSubject subject, String classString){
        String srcBase = Utils.join(subject.getHome(), subject.getSsrc());
        String testBase = Utils.join(subject.getHome(), subject.getTsrc());
        if(classString.contains("$")) {
            classString = classString.substring(0, classString.indexOf("$"));
        }
        String file = classString.replace('.', File.separatorChar) + ".java";
        boolean isTest = false;
        if (new File(Utils.join(srcBase, file)).exists()) {
            isTest = false;
            file = Utils.join(srcBase, file);
        } else if (new File(Utils.join(testBase, file)).exists()) {
            isTest = true;
            file = Utils.join(testBase, file);
        } else {
            LevelLogger.error(Utils.join(srcBase, file));
            LevelLogger.error("File not found : " + file);
        }
        return JavaFile.genASTFromFileWithType(file, isTest?testBase:srcBase);
    }

    private static Pair<MethodDeclaration, String> parseSingleMethodString(D4jSubject subject, String methodString){
        Pair<MethodDeclaration, String> result = new Pair<MethodDeclaration, String>();
        String lineNo = methodString.split(":")[1];
        methodString = methodString.split(":")[0];
        String classString = methodString.split("#")[0];
        if (methodDeclarationMap.containsKey(methodString)) {
            return new Pair<>(methodDeclarationMap.get(methodString), classString + ":" + lineNo);
        }
        CompilationUnit compilationUnit = null;
        if (compilationUnitMap.containsKey(classString)) {
            compilationUnit = compilationUnitMap.get(classString);
        }
        if (compilationUnit == null){
            compilationUnit = getCompilationUnit(subject, classString);
            compilationUnitMap.put(classString,compilationUnit);
        }

        String [] array =  methodString.split("#");
        if (array.length != 4){
            System.out.println("Error methodString");
            return null;
        }
        String returnType = array[1];
        String methodName = array[2];
        String argsType = array[3];
        compilationUnit.accept(new ASTVisitor() {
            @Override
            public boolean visit(MethodDeclaration node) {
                if (methodName.equals(node.getName().toString())){
                    if ((node.getReturnType2() == null && returnType.equals("?"))
                            || returnType.equals(node.getReturnType2().toString())){
                        if (checkArgs(argsType, node.parameters())){
                            result.setFirst(node);
                            result.setSecond(classString + ":" + lineNo);
                            return false;
                        }
                    }
                }
                return true;
            }
        });
        methodDeclarationMap.put(methodString, result.getFirst());
        return result;
    }

    private static boolean checkArgs(String argsType, List parameters){
        String [] args = argsType.split(",");
        int i1 = 0, i2 = 0;
        if (args[0].equals("?")) i1++;
        while (i1 < args.length && i2 < parameters.size()){
            String string = parameters.get(i2).toString();
            String [] strings = string.split(" ");
            String type = strings[strings.length - 2];
            if (type.equals(args[i1])){
                i1++;
                i2++;
            }else if (type.contains(args[i1])){
                i1++;
            }else {
                if (type.contains("...")){
                    i2++;
                }else {
                    return false;
                }
            }
        }
        if (i1 != args.length) return false;
        if (i2 != parameters.size()){
            if (i2 == parameters.size() - 1){
                return parameters.get(i2).toString().contains("...");
            }else {
                return false;
            }
        }
        return true;
    }
}
