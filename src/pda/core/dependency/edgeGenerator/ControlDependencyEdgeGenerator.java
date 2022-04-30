package pda.core.dependency.edgeGenerator;

import org.eclipse.jdt.core.dom.*;
import pda.common.utils.Pair;
import pda.core.dependency.DependencyParser;
import pda.core.dependency.dependencyGraph.DependencyGraph;
import pda.core.dependency.dependencyGraph.EdgeType;
import pda.core.dependency.dependencyGraph.VariableVertex;

import java.util.*;

public class ControlDependencyEdgeGenerator {
    List<Pair<MethodDeclaration, String>> trace;
    DependencyGraph dependencyGraph;
    DependencyParser dependencyParser;

    public ControlDependencyEdgeGenerator(List<Pair<MethodDeclaration, String>> trace, DependencyGraph dependencyGraph,DependencyParser dependencyParser){
        this.trace = trace;
        this.dependencyParser = dependencyParser;
        this.dependencyGraph = dependencyGraph;
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


    private boolean isInSubTree(ASTNode child, ASTNode parent){
        ASTNode tempNode = child;
        while(tempNode.getParent() != null && !tempNode.getParent().equals(parent) ){
            tempNode = tempNode.getParent();
        }
        return tempNode.getParent() != null;
    }

    Map<Statement, List<SimpleName>> statementMap = new HashMap<>();
    private List<SimpleName> findVarInStatement(Statement statement, List<Pair<MethodDeclaration, String>> subTrace, String type){
        if (statementMap.containsKey(statement)){
            return statementMap.get(statement);
        }
        List<SimpleName> simpleNames = new ArrayList<>();
        for (Pair<MethodDeclaration, String> pair: subTrace) {
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }
            MethodDeclaration methodDeclaration = pair.getFirst();
            CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
            int lineNo = Integer.parseInt(pair.getSecond().split(":")[1]);
            String clazz = pair.getSecond().split(":")[0];
            switch (type){
                case "for":{
                    Expression expression = ((ForStatement) statement).getExpression();
                    methodDeclaration.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(SimpleName node) {
                            if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo){
                                return true;
                            }
                            if(isInSubTree(node, statement) && !isInSubTree(node, expression)){
                                simpleNames.add(node);
                            }
                            return true;
                        }
                    });
                    break;
                }
                case "while":{
                    Expression expression = ((WhileStatement) statement).getExpression();
                    methodDeclaration.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(SimpleName node) {
                            if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo){
                                return true;
                            }
                            if(isInSubTree(node, statement) && !isInSubTree(node, expression)){
                                simpleNames.add(node);
                            }
                            return true;
                        }
                    });
                    break;
                }
                case "if":{
                    Expression expression = ((IfStatement) statement).getExpression();
                    methodDeclaration.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(SimpleName node) {
                            if (compilationUnit.getLineNumber(node.getStartPosition()) != lineNo){
                                return true;
                            }
                            if(isInSubTree(node, statement) && !isInSubTree(node, expression)){
                                simpleNames.add(node);
                            }
                            return true;
                        }
                    });
                    break;
                }
            }
        }
        statementMap.put(statement, simpleNames);
        return simpleNames;
    }

    public void genSpecialControlDependencyEdge(Expression conditionExpression,MethodDeclaration methodDeclaration, List<Pair<MethodDeclaration, String>> trace, String clazz){
        List<ASTNode> rightList = collectAllVar(methodDeclaration, trace);
        IfStatement parentIf = (IfStatement) conditionExpression.getParent();
        CompilationUnit compilationUnit = (CompilationUnit) conditionExpression.getRoot();
        int line = compilationUnit.getLineNumber(parentIf.getStartPosition() + parentIf.getLength());
        List<ASTNode> conditionSimpleNames = collectAllVar(conditionExpression, trace);
        List<VariableVertex> conditionVertexes = new ArrayList<>();
        for (ASTNode start: conditionSimpleNames){
            conditionVertexes.add(new VariableVertex(compilationUnit, start, methodDeclaration.getName().toString(), clazz));
        }
        for (ASTNode end: rightList){
            VariableVertex endVertex = new VariableVertex(compilationUnit, end, methodDeclaration.getName().toString(), clazz);
            if (compilationUnit.getLineNumber(end.getStartPosition()) > line){
                for (VariableVertex startVertex: conditionVertexes){
                    if (dependencyGraph.getVertexes().containsKey(startVertex.getVertexId()) && dependencyGraph.getVertexes().containsKey(endVertex.getVertexId())){
                        dependencyGraph.addEdge(startVertex, endVertex, EdgeType.CONTROL_DEPENDENCY);
                    }
                }
            }
        }
    }

    public void genControlDependencyEdge(List<Pair<MethodDeclaration, String>> trace){
        System.out.println("Generating control dependency edges.....");
        for (int i = 0;i < trace.size();i++){
            Pair<MethodDeclaration, String> pair = trace.get(i);
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }
            MethodDeclaration methodDeclaration = pair.getFirst();
            CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
            int lineNo = Integer.parseInt(pair.getSecond().split(":")[1]);
            String clazz = pair.getSecond().split(":")[0];
            int i1 = i;
            methodDeclaration.accept(new ASTVisitor() {
                @Override
                public boolean visit(IfStatement node) {
                    if (compilationUnit.getLineNumber(node.getExpression().getStartPosition()) != lineNo) {
                        return true;
                    }

                    node.accept(new ASTVisitor() {
                        @Override
                        public boolean visit(ReturnStatement returnNode) {
                            genSpecialControlDependencyEdge(node.getExpression(), methodDeclaration, trace.subList(0, trace.indexOf(pair)), clazz);
                            return true;
                        }

                        public boolean visit(ThrowStatement throwNode){
                            genSpecialControlDependencyEdge(node.getExpression(), methodDeclaration, trace.subList(0, trace.indexOf(pair)), clazz);
                            return true;
                        }
                    });

                    List<ASTNode> simpleNames = collectAllVar(node.getThenStatement(), trace.subList(0, trace.indexOf(pair)));
                    simpleNames.addAll(collectAllVar(node.getElseStatement(), trace.subList(0, trace.indexOf(pair))));
                    List<ASTNode> conditionSimpleNames = collectAllVar(node.getExpression(), trace.subList(0, trace.indexOf(pair)));
                    for (ASTNode startSimpleName:conditionSimpleNames){
                        VariableVertex vertex = new VariableVertex((CompilationUnit) startSimpleName.getRoot(), startSimpleName, methodDeclaration.getName().toString(), clazz);
                        for (ASTNode endSimpleName:simpleNames){
                            VariableVertex endVertex = new VariableVertex((CompilationUnit) endSimpleName.getRoot(), endSimpleName, methodDeclaration.getName().toString(), clazz);
                            if (dependencyGraph.getVertexes().containsKey(vertex.getVertexId()) && dependencyGraph.getVertexes().containsKey(endVertex.getVertexId())){
                                dependencyGraph.addEdge(vertex, endVertex, EdgeType.CONTROL_DEPENDENCY);
                            }
                        }
                    }
                    return true;
                }

                @Override
                public boolean visit(WhileStatement node) {
                    if (compilationUnit.getLineNumber(node.getExpression().getStartPosition()) != lineNo) {
                        return true;
                    }
                    List<ASTNode> simpleNames = collectAllVar(node.getBody(), trace.subList(0, trace.indexOf(pair)));
                    List<ASTNode> conditionSimpleNames = collectAllVar(node.getExpression(), trace.subList(0, trace.indexOf(pair)));
                    for (ASTNode startSimpleName:conditionSimpleNames){
                        VariableVertex vertex = new VariableVertex((CompilationUnit) startSimpleName.getRoot(), startSimpleName, methodDeclaration.getName().toString(), clazz);
                        for (ASTNode endSimpleName:simpleNames){
                            VariableVertex endVertex = new VariableVertex((CompilationUnit) endSimpleName.getRoot(), endSimpleName, methodDeclaration.getName().toString(), clazz);
                            if (dependencyGraph.getVertexes().containsKey(vertex.getVertexId()) && dependencyGraph.getVertexes().containsKey(endVertex.getVertexId())){
                                dependencyGraph.addEdge(vertex, endVertex, EdgeType.CONTROL_DEPENDENCY);
                            }
                        }
                    }
                    return true;
                }

                @Override
                public boolean visit(ForStatement node) {
                    if(node.getExpression() == null) {
                        return true;
                    }
                    if (compilationUnit.getLineNumber(node.getExpression().getStartPosition()) != lineNo) {
                        return true;
                    }
                    List<ASTNode> simpleNames = collectAllVar(node.getBody(), trace.subList(0, trace.indexOf(pair)));
                    List<ASTNode> conditionSimpleNames = collectAllVar(node.getExpression(), trace.subList(0, trace.indexOf(pair)));
                    for (ASTNode startSimpleName:conditionSimpleNames){
                        VariableVertex vertex = new VariableVertex((CompilationUnit) startSimpleName.getRoot(), startSimpleName, methodDeclaration.getName().toString(), clazz);
                        for (ASTNode endSimpleName:simpleNames){
                            VariableVertex endVertex = new VariableVertex((CompilationUnit) endSimpleName.getRoot(), endSimpleName, methodDeclaration.getName().toString(), clazz);
                            if (dependencyGraph.getVertexes().containsKey(vertex.getVertexId()) && dependencyGraph.getVertexes().containsKey(endVertex.getVertexId())){
                                dependencyGraph.addEdge(vertex, endVertex, EdgeType.CONTROL_DEPENDENCY);
                            }
                        }
                    }
                    return true;
                }
            });
        }
    }
}
