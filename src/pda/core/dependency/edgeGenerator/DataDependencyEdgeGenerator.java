package pda.core.dependency.edgeGenerator;

import org.eclipse.jdt.core.dom.*;
import pda.common.utils.Pair;
import pda.core.dependency.DependencyParser;
import pda.core.dependency.dependencyGraph.DependencyGraph;
import pda.core.dependency.dependencyGraph.EdgeType;
import pda.core.dependency.dependencyGraph.VariableVertex;

import java.util.*;

public class DataDependencyEdgeGenerator {
    List<Pair<MethodDeclaration, String>> trace;
    DependencyGraph dependencyGraph;
    DependencyParser dependencyParser;

    public DataDependencyEdgeGenerator(List<Pair<MethodDeclaration, String>> trace, DependencyGraph dependencyGraph, DependencyParser dependencyParser){
        this.dependencyGraph = dependencyGraph;
        this.trace = trace;
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

    public void genDataDependencyEdge(List<Pair<MethodDeclaration, String>> trace){
        System.out.println("Generating data dependency edges.....");
        for (Pair<MethodDeclaration, String> pair: trace){
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }
            MethodDeclaration methodDeclaration = pair.getFirst();
            int lineNo = Integer.parseInt(pair.getSecond().split(":")[1]);
            CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
            String clazz = pair.getSecond().split(":")[0];
            methodDeclaration.accept(new ASTVisitor() {
                @Override
                public boolean visit(Assignment node) {
                    if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo) {
                        return true;
                    }
                    List<ASTNode> leftList = collectAllVar(node.getLeftHandSide(), trace.subList(0, trace.indexOf(pair)));
                    List<ASTNode> rightList = collectAllVar(node.getRightHandSide(), trace.subList(0, trace.indexOf(pair)));
                    ASTNode leftSimpleName = leftList.get(0);
                    VariableVertex startVertex = new VariableVertex((CompilationUnit) leftSimpleName.getRoot(), leftSimpleName, methodDeclaration.getName().toString(), clazz);
                    for (ASTNode simpleName: rightList){
                        VariableVertex endVertex = new VariableVertex((CompilationUnit) simpleName.getRoot(), simpleName, methodDeclaration.getName().toString(), clazz);
                        // 保证所有的边都是被依赖方指向依赖方
                        dependencyGraph.addEdge(endVertex, startVertex, EdgeType.DATA_DEPENDENCY);
                    }
                    return true;
                }

                @Override
                public boolean visit(VariableDeclarationFragment node) {
                    if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo) {
                        return true;
                    }
                    SimpleName leftSimpleName = node.getName();
                    List<ASTNode> rightList = collectAllVar(node.getInitializer(), trace.subList(0, trace.indexOf(pair)));
                    VariableVertex startVertex = new VariableVertex((CompilationUnit) leftSimpleName.getRoot(), leftSimpleName, methodDeclaration.getName().toString(), clazz);
                    for (ASTNode simpleName: rightList){
                        VariableVertex endVertex = new VariableVertex((CompilationUnit) simpleName.getRoot(), simpleName, methodDeclaration.getName().toString(), clazz);
                        // 保证所有的边都是被依赖方指向依赖方
                        dependencyGraph.addEdge(endVertex, startVertex, EdgeType.DATA_DEPENDENCY);
                    }
                    return true;
                }

            });
        }
    }
}
