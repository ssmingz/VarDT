package pda.core.dependency.edgeGenerator;

import org.eclipse.jdt.core.dom.*;
import pda.common.utils.Pair;
import pda.core.dependency.DependencyParser;
import pda.core.dependency.dependencyGraph.*;

import java.util.*;

public class DefUseEdgeGenerator {
    List<Pair<MethodDeclaration, String>> trace;
    DependencyGraph dependencyGraph;
    DependencyParser dependencyParser;

    public DefUseEdgeGenerator(List<Pair<MethodDeclaration, String>> trace, DependencyGraph dependencyGraph, DependencyParser dependencyParser){
        this.dependencyParser = dependencyParser;
        this.trace = trace;
        this.dependencyGraph = dependencyGraph;
    }

    private List<ASTNode> collectParas(MethodDeclaration methodDeclaration, List<Pair<MethodDeclaration, String>> trace){
        List paras = methodDeclaration.parameters();
        List<ASTNode> result = new ArrayList<>();
        for (Object o: paras){
            result.addAll(collectAllVar((ASTNode) o, trace));
        }
        return result;
    }

    private boolean isInStatement(int nodeType, ASTNode node){
        while (node != null){
            if (node.getNodeType() == nodeType){
                return true;
            }else {
                node = node.getParent();
            }
        }
        return false;
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

    private List<ASTNode> collectAllVar(ASTNode node, List<Pair<MethodDeclaration, String>> trace){
        return dependencyParser.collectAllVar(node, trace);
    }

    // kind为1是域变量 2是数组
    private void genFieldEdge(List<Pair<MethodDeclaration, String>> trace, ASTNode name, int kind){
       dependencyParser.genFieldEdge(trace, name, kind);
    }


    public void genDefUseEdge(List<Pair<MethodDeclaration, String>> trace){
        System.out.println("Generating def-use edges.....");
        Map<String, DependencyGraphVertex> nameToVertex = new HashMap<>();
        Map<String, ASTNode> nameToSimpleName = new HashMap<>();
        MethodDeclaration lastMethod = null;
        for (Pair<MethodDeclaration, String> pair: trace){
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }
            MethodDeclaration methodDeclaration = pair.getFirst();
            int lineNo = Integer.parseInt(pair.getSecond().split(":")[1]);
            CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
            String clazz = pair.getSecond().split(":")[0];
            String methodName = methodDeclaration.getName().toString();
            String methodStartLine = String.valueOf(compilationUnit.getLineNumber(methodDeclaration.getStartPosition()));
            boolean updateParas = true;
            if (lastMethod == null){
                lastMethod = methodDeclaration;
            }else {
                if (lastMethod.equals(methodDeclaration)){
                    updateParas = false;
                }
            }
            if (updateParas){
                List<ASTNode> paras = collectParas(methodDeclaration, trace.subList(0, trace.indexOf(pair)));
                for (int i = 0;i < paras.size();i++){
                    ASTNode s = paras.get(i);
                    if (!nameToSimpleName.containsKey(methodName+ "|" + methodStartLine  + "." + s.toString())){
                        nameToSimpleName.put(methodName+ "|" + methodStartLine  + "." + s.toString(), s);
                        nameToVertex.put(methodName + "|" + methodStartLine + "." + s.toString(), new MethodInvocationVertex((CompilationUnit) s.getRoot(), s, methodDeclaration.getName().toString(), clazz, 1 + i));
                    }else {
                        if (compilationUnit.getLineNumber(nameToSimpleName.get(methodName+ "|" + methodStartLine  + "." + s.toString()).getStartPosition()) > lineNo){
                            nameToSimpleName.put(methodName+ "|" + methodStartLine  + "." + s.toString(), s);
                            nameToVertex.put(methodName + "|" + methodStartLine + "." + s.toString(), new MethodInvocationVertex((CompilationUnit) s.getRoot(), s, methodDeclaration.getName().toString(), clazz, 1 + i));
                        }
                    }
                }
            }
            methodDeclaration.accept(new ASTVisitor() {
                @Override
                public boolean visit(VariableDeclarationFragment node) {
                    if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo) {
                        return true;
                    }
                    List<ASTNode> rightList = collectAllVar(node.getInitializer(), trace.subList(0, trace.indexOf(pair)));
                    for (ASTNode s: rightList){
                        if (nameToVertex.containsKey(methodName+ "|" + methodStartLine  + "." + s.toString())){
                            if (isInSameStatement(nameToSimpleName.get(methodName+ "|" + methodStartLine  + "." + s.toString()), s)){
                                VariableVertex endVertex = new VariableVertex((CompilationUnit) s.getRoot(), s, methodDeclaration.getName().toString(), clazz);
                                DependencyGraphVertex startVertex = nameToVertex.get(methodName+ "|" + methodStartLine  + "." + s.toString());
                                dependencyGraph.addEdge(startVertex, endVertex, EdgeType.DEF_USE);
//                                System.out.println(startVertex + "->" + endVertex);
                            }
                        }
                    }

                    List<ASTNode> leftList = collectAllVar(node.getName(), trace.subList(0, trace.indexOf(pair)));
                    for (ASTNode s: leftList){
                        nameToSimpleName.put(methodName+ "|" + methodStartLine  + "." + s.toString(), s);
                        nameToVertex.put(methodName+ "|" + methodStartLine  + "." + s.toString(), new VariableVertex((CompilationUnit) s.getRoot(), s, methodDeclaration.getName().toString(), clazz));
                    }
                    return true;
                }

                @Override
                public boolean visit(Assignment node) {
                    if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo) {
                        return true;
                    }
                    List<ASTNode> rightList = collectAllVar(node.getRightHandSide(), trace.subList(0, trace.indexOf(pair)));
                    for (ASTNode s: rightList){
                        if (nameToVertex.containsKey(methodName+ "|" + methodStartLine  + "." + s.toString())){
                            if (isInSameStatement(nameToSimpleName.get(methodName + "|" + methodStartLine + "." + s.toString()), s)){
                                VariableVertex endVertex = new VariableVertex((CompilationUnit) s.getRoot(), s, methodDeclaration.getName().toString(), clazz);
                                DependencyGraphVertex startVertex = nameToVertex.get(methodName+ "|" + methodStartLine  + "." + s.toString());
                                dependencyGraph.addEdge(startVertex, endVertex, EdgeType.DEF_USE);
//                                System.out.println(startVertex + "->" + endVertex);
                            }
                        }
                    }
                    List<ASTNode> leftList = collectAllVar(node.getLeftHandSide(), trace.subList(0, trace.indexOf(pair)));
                    for (ASTNode s: leftList){
                        nameToSimpleName.put(methodName + "|" + methodStartLine + "." + s.toString(), s);
                        nameToVertex.put(methodName+ "|" + methodStartLine  + "." + s.toString(), new VariableVertex((CompilationUnit) s.getRoot(), s, methodDeclaration.getName().toString(), clazz));
                    }
                    return true;
                }

                @Override
                public boolean visit(SimpleName node) {
                    if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo) {
                        return true;
                    }
                    // 防止和上面的重复
                    if (isInStatement(ASTNode.ASSIGNMENT, node) || isInStatement(ASTNode.VARIABLE_DECLARATION_FRAGMENT, node)){
                        return true;
                    }
                    if (nameToVertex.containsKey(methodName+ "|" + methodStartLine  + "." + node.toString())){
                        if (isInSameStatement(nameToSimpleName.get(methodName + "|" + methodStartLine + "." + node.toString()), node)){
                            VariableVertex endVertex = new VariableVertex((CompilationUnit) node.getRoot(), node, methodDeclaration.getName().toString(), clazz);
                            DependencyGraphVertex startVertex = nameToVertex.get(methodName+ "|" + methodStartLine  + "." + node.toString());
                            dependencyGraph.addEdge(startVertex, endVertex, EdgeType.DEF_USE);
//                                System.out.println(startVertex + "->" + endVertex);
                        }
                    }
                    return true;
                }
            });
        }
    }

}
