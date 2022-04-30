package pda.core.dependency.edgeGenerator;

import org.eclipse.jdt.core.dom.*;
import pda.common.utils.Pair;
import pda.core.dependency.DependencyParser;
import pda.core.dependency.dependencyGraph.DependencyGraph;
import pda.core.dependency.dependencyGraph.EdgeType;
import pda.core.dependency.dependencyGraph.MethodInvocationVertex;
import pda.core.dependency.dependencyGraph.VariableVertex;

import java.util.*;

public class MethodInvocationEdgeGenerator {
    List<Pair<MethodDeclaration, String>> trace;
    DependencyGraph dependencyGraph;
    DependencyParser dependencyParser;

    public MethodInvocationEdgeGenerator(List<Pair<MethodDeclaration, String>> trace, DependencyGraph dependencyGraph, DependencyParser dependencyParser){
        this.trace = trace;
        this.dependencyGraph = dependencyGraph;
        this.dependencyParser = dependencyParser;
    }


    private List<ASTNode> collectAllVar(ASTNode node, List<Pair<MethodDeclaration, String>> trace){
        return dependencyParser.collectAllVar(node, trace);
    }

    // kind为1是域变量 2是数组
    private void genFieldEdge(List<Pair<MethodDeclaration, String>> trace, ASTNode name, int kind){
        dependencyParser.genFieldEdge(trace, name, kind);
    }

    // 检测node1的定义是否能影响到node2
    private boolean isInSameStatement(ASTNode node1, ASTNode node2){
        if (node1.equals(node2)) {
            return true;
        }
        ASTNode tempNode = node1, tempNode2 = node2;
        while (tempNode.getParent() != null && !(tempNode.getParent() instanceof MethodDeclaration)){
            tempNode = tempNode.getParent();
        }
        if (tempNode.getParent() == null) {
            return false;
        }
        while (tempNode2.getParent() != null){
            if (tempNode2.getParent() instanceof MethodDeclaration){
                if (tempNode2.getParent().equals(tempNode.getParent())){
                    return true;
                }
            }
            tempNode2 = tempNode2.getParent();
        }
        return false;
    }

    private List<String> findAllArgumentForMethod(List<Pair<MethodDeclaration, String>> subTrace, String methodName, int argNum){
        List<ASTNode> args = new ArrayList<>();
        List<String> result = new ArrayList<>();
        for (Pair<MethodDeclaration, String> pair: subTrace){
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }
            MethodDeclaration methodDeclaration = pair.getFirst();
            String clazz = pair.getSecond().split(":")[0];
            if (methodDeclaration.getName().toString().equals(methodName)){
                if (checkMethodNum(methodDeclaration, argNum)){
                    int size = methodDeclaration.parameters().size();
                    // 最后一位可能有不定参数
                    if (argNum <= size){
                        args.addAll(methodDeclaration.parameters().subList(0, argNum));
                    }else {
                        args.addAll(methodDeclaration.parameters());
                        for (int i = 0;i < argNum - size;i++){
                            args.add((ASTNode) methodDeclaration.parameters().get(size - 1));
                        }
                    }
                    for (int i = 0;i < args.size();i++){
                        SingleVariableDeclaration singleVariableDeclaration = (SingleVariableDeclaration)args.get(i);
                        int lineNo = ((CompilationUnit) singleVariableDeclaration.getRoot()).getLineNumber(singleVariableDeclaration.getStartPosition());
                        result.add("MethodArgument#" + (i + 1) + "#" + clazz + "." + methodDeclaration.getName() + ":" + lineNo + "-" + singleVariableDeclaration.getName().toString());
                    }
                    break;
                }

            }
        }
        return result;
    }

    private boolean checkMethodNum(MethodDeclaration m, int argNum){
        List args = m.parameters();
        if (args.size() == argNum){
            return true;
        }
        // 检查最后一个参数是否为不定参数
        if (args.size() == 0) {
            return false;
        }

        if (((SingleVariableDeclaration) args.get(args.size() - 1)).toString().contains("...")){
            return true;
        }
        return false;
    }

    public void genMethodInvocationEdge(List<Pair<MethodDeclaration, String>> trace){
        System.out.println("Generating method invocation edges.....");
        for (int i = 0;i < trace.size();i++){
            Pair<MethodDeclaration, String> pair = trace.get(i);
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }
            MethodDeclaration methodDeclaration = pair.getFirst();
            int lineNo = Integer.parseInt(pair.getSecond().split(":")[1]);
            CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
            String clazz = pair.getSecond().split(":")[0];
            int i1 = i;
            methodDeclaration.accept(new ASTVisitor() {
                @Override
                public boolean visit(ConstructorInvocation node) {
                    if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo) {
                        return true;
                    }
                    List<List<ASTNode>> methodInvocationArgs = new ArrayList<>();
                    for (Object astNode: node.arguments()){
                        List<ASTNode> list = collectAllVar((ASTNode) astNode, trace.subList(0, trace.indexOf(pair)));
                        methodInvocationArgs.add(list);
                    }
                    String [] strings = clazz.split("\\.");
                    List<String> methodDelArgs = findAllArgumentForMethod(trace.subList(i1 +1, trace.size()), strings[strings.length - 1], methodInvocationArgs.size());
                    for (int i = 0;i < methodInvocationArgs.size();i++){
                        if (methodInvocationArgs.size() != methodDelArgs.size()){
//                            System.out.println(pair.getSecond() + ":" +  strings[strings.length - 1] + ":" + methodDelArgs + "    " +  methodInvocationArgs);
                            break;
                        }
                        List<ASTNode> list = methodInvocationArgs.get(i);
                        String methodDelArg = methodDelArgs.get(i);
                        MethodInvocationVertex methodInvocationVertex = (MethodInvocationVertex) dependencyGraph.getVertexes().get(methodDelArg);
                        if (methodInvocationVertex == null){
                            continue;
                        }
                        for (ASTNode simpleName:list){
                            VariableVertex vertex = new VariableVertex((CompilationUnit) simpleName.getRoot(), simpleName, methodDeclaration.getName().toString(), clazz);
                            dependencyGraph.addEdge(vertex, methodInvocationVertex, EdgeType.METHOD_INVOCATION);
                        }
                    }
                    return true;
                }

                @Override
                public boolean visit(MethodInvocation node) {
                    if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo) {
                        return true;
                    }
                    List<List<ASTNode>> methodInvocationArgs = new ArrayList<>();
                    for (Object astNode: node.arguments()){
                        List<ASTNode> list = collectAllVar((ASTNode) astNode, trace.subList(0, trace.indexOf(pair)));
                        methodInvocationArgs.add(list);
                    }
                    List<String> methodDelArgs = findAllArgumentForMethod(trace.subList(i1 +1, trace.size()), node.getName().toString(), methodInvocationArgs.size());
                    for (int i = 0;i < methodInvocationArgs.size();i++){
                        if (methodInvocationArgs.size() != methodDelArgs.size()){
//                            System.out.println(pair.getSecond() + ":" + node.getName().toString() + ":" + methodDelArgs + "    " +  methodInvocationArgs);
                            break;
                        }
                        List<ASTNode> list = methodInvocationArgs.get(i);
                        String methodDelArg = methodDelArgs.get(i);
                        MethodInvocationVertex methodInvocationVertex = (MethodInvocationVertex) dependencyGraph.getVertexes().get(methodDelArg);
                        if (methodInvocationVertex == null){
                            continue;
                        }
                        for (ASTNode simpleName:list){
                            VariableVertex vertex = new VariableVertex((CompilationUnit) simpleName.getRoot(), simpleName, methodDeclaration.getName().toString(), clazz);
                            dependencyGraph.addEdge(vertex, methodInvocationVertex, EdgeType.METHOD_INVOCATION);
                        }
                    }
                    return true;
                }
            });
        }
    }

}
