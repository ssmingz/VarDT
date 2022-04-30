package pda.core.dependency;

import com.google.googlejavaformat.Indent;
import org.eclipse.jdt.core.dom.*;
import pda.common.conf.Constant;
import pda.common.java.D4jSubject;
import pda.common.utils.*;
import pda.core.dependency.dependencyGraph.*;
import pda.core.dependency.edgeGenerator.ControlDependencyEdgeGenerator;
import pda.core.dependency.edgeGenerator.DataDependencyEdgeGenerator;
import pda.core.dependency.edgeGenerator.DefUseEdgeGenerator;
import pda.core.dependency.edgeGenerator.MethodInvocationEdgeGenerator;

import java.io.*;
import java.util.*;

public class DependencyParser {

    DependencyGraph dependencyGraph;
    String graphPath = "";
    boolean optimize = false;
    public DependencyParser(){
        graphPath = Config.graphPath;
    }

    public DependencyParser(boolean optimize){
        this.optimize = optimize;
        graphPath = Config.graphPath;
    }

    public void parse(String traceFile, D4jSubject subject){
        TraceParser traceParser = new TraceParser(traceFile);
        //List<Pair<MethodDeclaration, String>> trace = traceParser.parse(subject);
        if (!graphPath.equals("")){
            String graph = graphPath + File.separator + subject.getName() + "_" + subject.getId() + ".ser";
            File file = new File(graph);
            if (file.exists()){
                try{
                    dependencyGraph = (DependencyGraph) Utils.deserialize(graph);
                }catch (Exception e){
                    e.printStackTrace();
                }
            }else {
                List<Pair<MethodDeclaration, String>> trace = traceParser.parse(subject);
                genGraph(trace);
                try{
                    if(subject.getName().equals("closure")) {
                        if(subject.getId()==1||subject.getId()==29||subject.getId()==33||subject.getId()==35||subject.getId()==54||subject.getId()==135){
                            return;
                        } else {
                            Utils.serialize(dependencyGraph, graph);
                        }
                    } else if(subject.getName().equals("jacksondatabind")) {
                        if(subject.getId()==1||subject.getId()==4||subject.getId()==10||subject.getId()==15||subject.getId()==29||subject.getId()==107){
                            return;
                        } else {
                            Utils.serialize(dependencyGraph, graph);
                        }
                    } else {
                        Utils.serialize(dependencyGraph, graph);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }else {
            System.err.println("Please set the graph save path in the config.txt");
        }
    }

    private List<ASTNode> collectAllVarAtLine(MethodDeclaration node, int lineNo, List<Pair<MethodDeclaration, String>> trace){
        List<ASTNode> result = new ArrayList<>();
        CompilationUnit compilationUnit = (CompilationUnit) node.getRoot();
        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) == lineNo){
                    if (node.resolveBinding() instanceof IMethodBinding){
                        ASTNode root = node;
                        while (root != null && root.getNodeType() != ASTNode.METHOD_INVOCATION){
                            root = root.getParent();
                        }
                        if (root != null){
                            result.add(root);
                        }
                        return true;
                    }else if(node.resolveBinding() instanceof ITypeBinding){
                        return true;
                    }else {
                        Name temp = node;
                        while (temp.getParent().getNodeType() == ASTNode.QUALIFIED_NAME){
                            temp = (Name) temp.getParent();
                        }
                        // 数组
                        if (temp.resolveTypeBinding() != null && temp.resolveTypeBinding().isArray()){
                            if(ASTNode.ARRAY_ACCESS == temp.getParent().getNodeType()){
                                if (!result.contains(temp.getParent())){
                                    genFieldEdge(trace, temp, 2, (ArrayAccess) temp.getParent());
                                    result.add(temp.getParent());
                                }
                                return true;
                            }
                        }
                        // 域变量里的this特别处理
                        if (temp.getParent().getNodeType() == ASTNode.FIELD_ACCESS){
                            FieldAccess temp1 = (FieldAccess) temp.getParent();
                            // 针对 形如 this.b[0]额外判断
                            if (temp1.resolveTypeBinding() != null && temp1.resolveTypeBinding().isArray()){
                                if(ASTNode.ARRAY_ACCESS == temp1.getParent().getNodeType()){
                                    if (!result.contains(temp1.getParent())){
                                        genFieldEdge(trace, temp1, 2, (ArrayAccess) temp1.getParent());
                                        result.add(temp1.getParent());
                                    }
                                    return true;
                                }
                            }
                            if (!result.contains(temp.getParent())){
                                genFieldEdge(trace, temp.getParent(), 3);
                                result.add(temp.getParent());
                            }
                            return  true;
                        }
                        if (!result.contains(temp)){
                            if (temp.isQualifiedName()){
                                genFieldEdge(trace, (QualifiedName) temp, 1);
                            }
                            result.add(temp);
                        }
                    }
                }
                return true;
            }
        });
        return result;
    }

    public List<ASTNode> collectAllVar(ASTNode node, List<Pair<MethodDeclaration, String>> trace){
        List<ASTNode> result = new ArrayList<>();
        if (node == null) return result;
        node.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                if (node.resolveBinding() instanceof IMethodBinding){
                    ASTNode root = node;
                    while (root != null && root.getNodeType() != ASTNode.METHOD_INVOCATION){
                        root = root.getParent();
                    }
                    if (root != null){
                        result.add(root);
                    }

                    return true;
                }else if(node.resolveBinding() instanceof ITypeBinding){
                    return true;
                }else {
                    Name temp = node;
                    while (temp.getParent().getNodeType() == ASTNode.QUALIFIED_NAME){
                        temp = (Name) temp.getParent();
                    }
                    // 数组
                    if (temp.resolveTypeBinding() != null && temp.resolveTypeBinding().isArray()){
                        if(ASTNode.ARRAY_ACCESS == temp.getParent().getNodeType()){
                            if (!result.contains(temp.getParent())){
                                genFieldEdge(trace, temp, 2, (ArrayAccess) temp.getParent());
                                result.add(temp.getParent());
                            }
                            return true;
                        }
                    }
                    // 域变量里的this特别处理
                    if (temp.getParent().getNodeType() == ASTNode.FIELD_ACCESS){
                        FieldAccess temp1 = (FieldAccess) temp.getParent();
                        // 针对 形如 this.b[0]额外判断
                        if (temp1.resolveTypeBinding() != null && temp1.resolveTypeBinding().isArray()){
                            if(ASTNode.ARRAY_ACCESS == temp1.getParent().getNodeType()){
                                if (!result.contains(temp1.getParent())){
                                    genFieldEdge(trace, temp1, 2,(ArrayAccess) temp1.getParent());
                                    result.add(temp1.getParent());
                                }
                                return true;
                            }
                        }
                        if (!result.contains(temp.getParent())){
                            genFieldEdge(trace, temp.getParent(), 3);
                            result.add(temp.getParent());
                        }
                        return  true;
                    }
                    if (!result.contains(temp)){
                        if (temp.isQualifiedName()){
                            genFieldEdge(trace, (QualifiedName) temp, 1);
                        }
                        result.add(temp);
                    }
                }
                return true;
            }
        });
        return result;
    }

    private void removeDuplicateTrace(List<Pair<MethodDeclaration, String>> trace){
        System.out.println("Starting remove trace....");
        System.out.println("Starting with trace size:" + trace.size());
        for (int i = 0;i < trace.size();i++){
            Pair<MethodDeclaration, String> pair = trace.get(i);
            if (pair.getFirst() == null || pair.getSecond() == null){
                continue;
            }
            if (trace.subList(0, i).contains(pair)){
                trace.remove(i);
                i--;
            }
        }
        System.out.println("Ending with trace size:" + trace.size());
    }

    private void genGraph(List<Pair<MethodDeclaration, String>> trace){
        System.out.println("Starting generate graph....");
        long startTime = System.currentTimeMillis();
        dependencyGraph = new DependencyGraph();

        if (optimize){
            removeDuplicateTrace(trace);
        }

        for (int j = 0; j < trace.size();j++){
            Pair<MethodDeclaration, String> pair = trace.get(j);
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }

            addLineIntoTrace(trace, pair);


            MethodDeclaration methodDeclaration = pair.getFirst();
            String lineNo = pair.getSecond().split(":")[1];
            String clazz = pair.getSecond().split(":")[0];
            int i = 1;
            List<ASTNode> simpleNames = collectAllVarAtLine(methodDeclaration, Integer.parseInt(lineNo), trace.subList(0, trace.indexOf(pair)));
            for (Object node:methodDeclaration.parameters()){
                List<ASTNode> list = collectAllVar((ASTNode) node, trace.subList(0, trace.indexOf(pair)));
                ASTNode simpleName = list.get(list.size() - 1);
                dependencyGraph.addVertex(new MethodInvocationVertex((CompilationUnit) simpleName.getRoot(), simpleName, methodDeclaration.getName().toString(), clazz, i));
                i++;
            }
            for (ASTNode simpleName: simpleNames){
                dependencyGraph.addVertex(new VariableVertex((CompilationUnit) simpleName.getRoot(), simpleName, methodDeclaration.getName().toString(), clazz));
            }
        }

        for (Pair<MethodDeclaration, String> pair : trace) {
            if(pair.getFirst()==null || pair.getSecond()==null){
                continue;
            }
            collectAllTempVertex(pair);
        }

        DefUseEdgeGenerator defUseEdgeGenerator = new DefUseEdgeGenerator(trace, dependencyGraph, this);
        DataDependencyEdgeGenerator dataDependencyEdgeGenerator = new DataDependencyEdgeGenerator(trace, dependencyGraph, this);
        ControlDependencyEdgeGenerator controlDependencyEdgeGenerator = new ControlDependencyEdgeGenerator(trace, dependencyGraph, this);
        MethodInvocationEdgeGenerator methodInvocationEdgeGenerator = new MethodInvocationEdgeGenerator(trace, dependencyGraph, this);
        defUseEdgeGenerator.genDefUseEdge(trace);
        dataDependencyEdgeGenerator.genDataDependencyEdge(trace);
        controlDependencyEdgeGenerator.genControlDependencyEdge(trace);
        methodInvocationEdgeGenerator.genMethodInvocationEdge(trace);
        replaceExpressionWithTempVariable(dependencyGraph);

        replaceMethodInvocationWithTempVariable(dependencyGraph);

        System.out.println("Finish generate graph!");
        System.out.println("Total time: " + String.valueOf((System.currentTimeMillis() - startTime) / 1000) + "s");
    }

    private void replaceMethodInvocationWithTempVariable(DependencyGraph dependencyGraph){
        Iterator<Map.Entry<String, DependencyGraphVertex>> iterator = dependencyGraph.getVertexes().entrySet().iterator();
        Set<TempVertex> toBeAdded = new HashSet<>();
        Set<VariableVertex> toBeDeleted = new HashSet<>();
        while (iterator.hasNext()){
            Map.Entry<String, DependencyGraphVertex> entry = iterator.next();
            if (entry.getKey().endsWith(")")){
                DependencyGraphVertex dependencyGraphVertex = entry.getValue();
                if (dependencyGraphVertex.getVertexType().equals(VertexType.Variable)){
                    VariableVertex v = (VariableVertex) dependencyGraphVertex;
                    String clazz = v.getClazz();
                    String methodName = v.getMethodName();
                    int lineNo = v.getLineNo();
                    String varName = v.getSimpleName();
                    int colNo = v.getColNo();
                    int size = varName.length();
                    TempVertex tempVertex = new TempVertex(clazz, methodName, lineNo, varName, colNo, size);

                    Set<DependencyGraphEdge> startEdges = v.getStartEdges();
                    Set<DependencyGraphEdge> endEdges = v.getEndEdges();
                    Set<DependencyGraphVertex> fromVertexes = v.getFromVertexes();
                    Set<DependencyGraphVertex> toVertexes = v.getToVertexes();

                    for (DependencyGraphEdge startEdge : startEdges) {
                        DependencyGraphVertex toVertex = startEdge.getEndVertex();

                        toVertex.getFromVertexes().remove(v);
                        toVertex.getEndEdges().remove(startEdge);

                        DependencyGraphEdge newStartEdge = new DependencyGraphEdge(tempVertex, toVertex, startEdge.getEdgeType());
                        toVertex.getFromVertexes().add(tempVertex);
                        toVertex.getEndEdges().add(newStartEdge);

                        tempVertex.getStartEdges().add(newStartEdge);
                        tempVertex.getToVertexes().add(toVertex);
                    }
                    for (DependencyGraphEdge endEdge : endEdges) {
                        DependencyGraphVertex fromVertex = endEdge.getStartVertex();

                        fromVertex.getToVertexes().remove(v);
                        fromVertex.getStartEdges().remove(endEdge);

                        DependencyGraphEdge newEndEdge =new DependencyGraphEdge(fromVertex, tempVertex, endEdge.getEdgeType());
                        fromVertex.getToVertexes().add(tempVertex);
                        fromVertex.getStartEdges().add(newEndEdge);

                        tempVertex.getEndEdges().add(newEndEdge);
                        tempVertex.getFromVertexes().add(fromVertex);
                    }

                    v.getStartEdges().clear();
                    v.getEndEdges().clear();
                    v.getToVertexes().clear();
                    v.getFromVertexes().clear();

                    toBeAdded.add(tempVertex);
                    toBeDeleted.add(v);
                }
            }
        }
        for (TempVertex tempVertex : toBeAdded) {
            dependencyGraph.addVertex(tempVertex);
        }

        for (VariableVertex variableVertex : toBeDeleted) {
            dependencyGraph.getVertexes().remove(variableVertex.getVertexId());
        }
    }

    private List<ASTNode> collectParas(MethodDeclaration methodDeclaration, List<Pair<MethodDeclaration, String>> trace){
        List paras = methodDeclaration.parameters();
        List<ASTNode> result = new ArrayList<>();
        for (Object o: paras){
            result.addAll(collectAllVar((ASTNode) o, trace));
        }
        return result;
    }

    Map<ASTNode, Integer> nameMap = new HashMap<>();

    // kind为1是域变量 2是数组
    public void genFieldEdge(List<Pair<MethodDeclaration, String>> trace, ASTNode name, int kind, ArrayAccess... arrayAccesses){
        if (kind != 2 && nameMap.containsKey(name)){
            return;
        }else if(kind == 2 && nameMap.containsKey(arrayAccesses[0])){
            if (nameMap.get(arrayAccesses[0]) > 5){
                return;
            }else {
                nameMap.put(arrayAccesses[0], nameMap.get(arrayAccesses[0]) + 1);
            }
        } else{
            if (kind != 2){
                nameMap.put(name, kind);
            }else {
                nameMap.put(arrayAccesses[0], 1);
            }
        }
        List<Pair<MethodDeclaration, String>> temp = new ArrayList<>(trace);
        Collections.reverse(temp);
        for (Pair<MethodDeclaration, String> pair: temp) {
            if (pair.getFirst() == null || pair.getSecond() == null) {
                continue;
            }
            MethodDeclaration methodDeclaration = pair.getFirst();
            int lineNo = Integer.parseInt(pair.getSecond().split(":")[1]);
            CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
            String clazz = pair.getSecond().split(":")[0];

            if (kind == 2){
                List<ASTNode> paras = collectParas(methodDeclaration, trace.subList(0, trace.indexOf(pair)));
                for (int i = 0; i < paras.size(); i++) {
                    ASTNode astNode = paras.get(i);
                    dependencyGraph.addVertex(new MethodInvocationVertex((CompilationUnit) astNode.getRoot(), astNode, methodDeclaration.getName().toString(), clazz, i + 1));
                    if (astNode.toString().equals(name.toString())){
                        MethodInvocationVertex startVertex = new MethodInvocationVertex((CompilationUnit) astNode.getRoot(), astNode, methodDeclaration.getName().toString(), clazz, i + 1);
                        VariableVertex endVertex = new VariableVertex((CompilationUnit) name.getRoot(), arrayAccesses[0], methodDeclaration.getName().toString(), clazz);
                        dependencyGraph.addVertex(endVertex);
                        if(dependencyGraph.addEdge(startVertex, endVertex, EdgeType.FIELD)){
                            nameMap.put(arrayAccesses[0], 999);
                        }
                    }
                }
            }


            if (kind == 1){
                QualifiedName name1 = (QualifiedName) name;
                methodDeclaration.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(Assignment node) {
                        if (lineNo != compilationUnit.getLineNumber(node.getStartPosition())){
                            return true;
                        }
                        List<ASTNode> left = collectAllVar(node.getLeftHandSide(), trace.subList(0, trace.indexOf(pair)));
                        for (ASTNode astNode: left){
                            if (astNode.toString().equals(name1.getQualifier().toString()) && isInSameStatement(node, name1) ){
                                VariableVertex startVertex = new VariableVertex((CompilationUnit) astNode.getRoot(), astNode, methodDeclaration.getName().toString(), clazz);
                                VariableVertex endVertex = new VariableVertex((CompilationUnit) name1.getRoot(), name1, methodDeclaration.getName().toString(), clazz);
                                dependencyGraph.addEdge(startVertex, endVertex, EdgeType.FIELD);
                                return false;
                            }
                        }
                        return true;
                    }
                    public boolean visit(VariableDeclarationFragment node){
                        if (lineNo != compilationUnit.getLineNumber(node.getStartPosition())){
                            return true;
                        }
                        if (node.getName().toString().equals(name1.getQualifier().toString()) && isInSameStatement(node, name1)){
                            VariableVertex startVertex = new VariableVertex((CompilationUnit) node.getRoot(), node, methodDeclaration.getName().toString(), clazz);
                            VariableVertex endVertex = new VariableVertex((CompilationUnit) name1.getRoot(), name1, methodDeclaration.getName().toString(), clazz);
                            dependencyGraph.addEdge(startVertex, endVertex, EdgeType.FIELD);
                            return false;
                        }
                        return true;
                    }
                });
            } else if (kind == 2){
                methodDeclaration.accept(new ASTVisitor() {
                    @Override
                    public boolean visit(Assignment node) {
                        if (lineNo != compilationUnit.getLineNumber(node.getStartPosition())){
                            return true;
                        }
                        List<ASTNode> r = collectAllVar(node.getRightHandSide(), trace.subList(0, trace.indexOf(pair)));
                        for (ASTNode astNode: r){
                            if (astNode.toString().equals(name.toString()) && isInSameStatement(node, name) ){
                                VariableVertex startVertex = new VariableVertex((CompilationUnit) astNode.getRoot(), astNode, methodDeclaration.getName().toString(), clazz);
                                VariableVertex endVertex = new VariableVertex((CompilationUnit) name.getRoot(), name, methodDeclaration.getName().toString(), clazz);
                                dependencyGraph.addEdge(startVertex, endVertex, EdgeType.FIELD);
                                return false;
                            }
                        }
                        return true;
                    }
                    public boolean visit(VariableDeclarationFragment node){
                        if (lineNo != compilationUnit.getLineNumber(node.getStartPosition())){
                            return true;
                        }
                        List<ASTNode> r = collectAllVar(node.getInitializer(), trace.subList(0, trace.indexOf(pair)));
                        for (ASTNode astNode: r){
                            if (astNode.toString().equals(name.toString()) && isInSameStatement(node, name) ){
                                VariableVertex startVertex = new VariableVertex((CompilationUnit) astNode.getRoot(), astNode, methodDeclaration.getName().toString(), clazz);
                                VariableVertex endVertex = new VariableVertex((CompilationUnit) name.getRoot(), name, methodDeclaration.getName().toString(), clazz);
                                dependencyGraph.addEdge(startVertex, endVertex, EdgeType.FIELD);
                                return false;
                            }
                        }
//                        if (node.getName().toString().equals(name.toString()) && isInSameStatement(node, name)){
//                            VariableVertex startVertex = new VariableVertex((CompilationUnit) node.getRoot(), node, methodDeclaration.getName().toString(), clazz);
//                            VariableVertex endVertex = new VariableVertex((CompilationUnit) name.getRoot(), name, methodDeclaration.getName().toString(), clazz);
//                            dependencyGraph.addEdge(startVertex, endVertex, EdgeType.FIELD);
//                            return false;
//                        }
                        return true;
                    }
                });
            }else if (kind == 3){
                // 这种应该没有 this ＝ 什么什么之类的东西吧？？？

            }
        }
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

    public DependencyGraph getDependencyGraph(){
        return dependencyGraph;
    }

    public boolean hasDependency(String clazz, String methodName, int lineNo, String varName, int colNo, String clazz2, String methodName2, int lineNo2, String varName2, int colNo2){
        VariableVertex vertex1 = new VariableVertex(clazz, methodName, lineNo, varName, colNo);
        VariableVertex vertex2 = new VariableVertex(clazz2, methodName2, lineNo2, varName2, colNo);
        return dependencyGraph.hasDependency(vertex1, vertex2);
    }

    public List<String> getDependencyTrace(DependencyGraphVertex v1, DependencyGraphVertex v2){
        return dependencyGraph.getDependencyTrace(v1, v2);
    }


    public boolean isEquivalent(DependencyGraphVertex vertex1, DependencyGraphVertex vertex2){
        if (dependencyGraph.getVertexes().containsKey(vertex1.getVertexId()) && dependencyGraph.getVertexes().containsKey(vertex2.getVertexId())){
            vertex1 = dependencyGraph.getVertexes().get(vertex1.getVertexId());
            vertex2 = dependencyGraph.getVertexes().get(vertex2.getVertexId());
        }else if (!dependencyGraph.getVertexes().containsKey(vertex1.getVertexId())){
            System.out.println(vertex1.getVertexId() + " is not in graph!!!");
            String line = vertex1.getVertexId().split("\\|")[0];
            String name = vertex1.getVertexId().split("-")[1];
            List<String> nearVertex = new ArrayList<>();
            for (String s : dependencyGraph.getVertexes().keySet()) {
                if (s.startsWith(line) && s.endsWith(name)){
                    nearVertex.add(s);
                }
            }
            System.out.println("Perhaps it is :" + nearVertex);
            return false;
        }else {
            System.out.println(vertex2.getVertexId() + " is not in graph!!!");
            String line = vertex2.getVertexId().split("\\|")[0];
            String name = vertex2.getVertexId().split("-")[1];
            List<String> nearVertex = new ArrayList<>();
            for (String s : dependencyGraph.getVertexes().keySet()) {
                if (s.startsWith(line) && s.endsWith(name)){
                    nearVertex.add(s);
                }
            }
            System.out.println("Perhaps it is :" + nearVertex);
            return false;
        }
        if (vertex1.sameVertex(vertex2)){
            return true;
        }
//        if (!v1.getSimpleName().toString().equals(v2.getSimpleName().toString())){
//            return false;
//        }
        // 变量节点一定存在一个def-use关系 最起码是和函数参数存在def-use
        DependencyGraphVertex v1_def = null, v2_def = null;
        for (DependencyGraphEdge endEdge : vertex1.getEndEdges()) {
            if (endEdge.getEdgeType().equals(EdgeType.DEF_USE)){
                v1_def = endEdge.getStartVertex();
            }
        }

        for (DependencyGraphEdge endEdge : vertex2.getEndEdges()) {
            if (endEdge.getEdgeType().equals(EdgeType.DEF_USE)){
                v2_def = endEdge.getStartVertex();
            }
        }

        if (v1_def == null && v2_def == null){
            String name2 = vertex2.getVertexId().split("-")[1];
            String name1 = vertex1.getVertexId().split("-")[1];
            return name1.equals(name2);
        }

        if (v1_def == null){
            return v2_def.sameVertex(vertex1);
        }
        if (v2_def == null){
            return v1_def.sameVertex(vertex2);
        }
        return v1_def.sameVertex(v2_def);
    }

    public void replaceExpressionWithTempVariable(DependencyGraph dependencyGraph){
        List<Pair<TempVertex, ASTNode>> vertexToBeReplace = new ArrayList<>();
        for (Map.Entry<String, Pair<TempVertex, ASTNode>> entry: allTempVertexes.entrySet()) {
            TempVertex tempVertex = entry.getValue().getFirst();
            dependencyGraph.addVertex(tempVertex);

            String lineNow = vertexToBeReplace.size() == 0?"":vertexToBeReplace.get(0).getFirst().getVertexId().split("\\|")[0];
            String vertexLine = tempVertex.getVertexId().split("\\|")[0];
            String vLine = vertexLine.substring(vertexLine.indexOf("#") + 1, vertexLine.lastIndexOf(".")) + ":" + vertexLine.split(":")[1];
            if (vertexToBeReplace.size() != 0
                    && !lineNow.equals(vertexLine)
                    && !addedLine.contains(vLine)) {
                replaceAndConnectTempVariable(vertexToBeReplace);
                vertexToBeReplace.clear();
            }
            vertexToBeReplace.add(entry.getValue());
        }
        replaceAndConnectTempVariable(vertexToBeReplace);
    }

    private void replaceAndConnectTempVariable(List<Pair<TempVertex, ASTNode>> vertexToBeReplace){

        if (vertexToBeReplace.size() == 0) {
            return;
        }
        Map<ASTNode, TempVertex> vars = new HashMap<>();
        TempVertex longestTemp =  vertexToBeReplace.get(0).getFirst();
        String clazz = vertexToBeReplace.get(0).getFirst().getClazz();
        String methodName = vertexToBeReplace.get(0).getFirst().getMethodName();
        for (Pair<TempVertex, ASTNode> tempVertexASTNodePair : vertexToBeReplace) {
            for (ASTNode node : checkHasVar(tempVertexASTNodePair.getSecond())) {
                if (longestTemp.getSize() < tempVertexASTNodePair.getFirst().getSize()){
                    longestTemp = tempVertexASTNodePair.getFirst();
                }

                if (!vars.containsKey(node)){
                    vars.put(node, tempVertexASTNodePair.getFirst());
                }else {
                    if (vars.get(node).getSize() > tempVertexASTNodePair.getFirst().getSize()){
                        vars.put(node, tempVertexASTNodePair.getFirst());
                    }
                }
            }
        }


        for (ASTNode astNode: vars.keySet()){
            VariableVertex v = new VariableVertex((CompilationUnit) astNode.getRoot(), astNode, methodName, clazz);
            v = (VariableVertex) dependencyGraph.getVertexes().get(v.getVertexId());
            if (v == null) continue;


            List<DependencyGraphVertex> vToRemove = new ArrayList<>();
            // 将最长的表达式指向全部原节点指向的位置
            for (DependencyGraphVertex toVertex : v.getToVertexes()) {
                DependencyGraphEdge dependencyGraphEdge = v.findStartEdge(toVertex);
                if (dependencyGraphEdge != null && dependencyGraphEdge.getEdgeType().equals(EdgeType.CONTROL_DEPENDENCY)){
                    dependencyGraph.addEdge(longestTemp, toVertex, dependencyGraphEdge.getEdgeType());
                    toVertex.getFromVertexes().remove(v);
                    toVertex.getEndEdges().remove(dependencyGraphEdge);
                    vToRemove.add(toVertex);
                    v.getStartEdges().remove(dependencyGraphEdge);
                }
            }
            for (DependencyGraphVertex toVertex : vToRemove) {
                v.getToVertexes().remove(toVertex);
            }

            // 将原节点指向新节点
            dependencyGraph.addEdge(v, vars.get(astNode), EdgeType.DATA_DEPENDENCY);
        }

        // 新节点间的数据依赖构建
        for (Pair<TempVertex, ASTNode> tempVertexASTNodePair : vertexToBeReplace) {
            TempVertex startTemp = tempVertexASTNodePair.getFirst();
            ASTNode startNode = tempVertexASTNodePair.getSecond();
            TempVertex shortestTemp = startTemp;
            for (Pair<TempVertex, ASTNode> tempVertexASTNodePair1 : vertexToBeReplace){
                TempVertex endTemp = tempVertexASTNodePair1.getFirst();
                ASTNode endNode = tempVertexASTNodePair1.getSecond();

                if (!startTemp.getVertexId().equals(endTemp.getVertexId())
                        && endNode.getStartPosition() <= startNode.getStartPosition()
                        && endNode.getStartPosition() + endNode.getLength() >= startNode.getStartPosition() + startNode.getLength()){
                    if (shortestTemp.getVertexId().equals(startTemp.getVertexId())){
                        shortestTemp = endTemp;
                    }else if (shortestTemp.getSize() > endTemp.getSize()){
                        shortestTemp = endTemp;
                    }
                }
            }
            if (!shortestTemp.getVertexId().equals(startTemp.getVertexId())){
                dependencyGraph.addEdge(startTemp, shortestTemp, EdgeType.DATA_DEPENDENCY);
            }
        }
    }

    Map<String, Pair<TempVertex, ASTNode>> allTempVertexes = new TreeMap<>();
    Set <ASTNode> astNodeSet = new HashSet<>();

    public void collectAllTempVertex(Pair<MethodDeclaration, String> pair){
        MethodDeclaration methodDeclaration = pair.getFirst();
        String lineNo = pair.getSecond().split(":")[1];
        String clazz = pair.getSecond().split(":")[0];
        CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
        methodDeclaration.accept(new ASTVisitor() {

            @Override
            public boolean visit(ReturnStatement node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                if (astNodeSet.contains(node)){
                    return true;
                }
                collectTempVariable(node, compilationUnit, methodDeclaration, clazz);
                astNodeSet.add(node);
                return true;
            }

            @Override
            public boolean visit(IfStatement node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                if (astNodeSet.contains(node)){
                    return true;
                }
                collectTempVariable(node, compilationUnit, methodDeclaration, clazz);
                astNodeSet.add(node);
                return true;
            }

            @Override
            public boolean visit(MethodInvocation node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                if (astNodeSet.contains(node)){
                    return true;
                }
                collectTempVariable(node, compilationUnit, methodDeclaration, clazz);
                astNodeSet.add(node);
                return true;
            }

        });
    }

    private void collectTempVariable(ASTNode astNode, CompilationUnit compilationUnit, MethodDeclaration methodDeclaration, String clazz){
        astNode.accept(new ASTVisitor() {
            @Override
            public boolean visit(InfixExpression node) {
                if (checkHasVar(node).size() != 0){
                    TempVertex v = new TempVertex(compilationUnit, node, methodDeclaration.getName().toString(), clazz);
                    allTempVertexes.put(v.getVertexId(), new Pair<>(v, node));
                }
                return true;
            }

            @Override
            public boolean visit(PostfixExpression node) {
                if (checkHasVar(node).size() != 0){
                    TempVertex v = new TempVertex(compilationUnit, node, methodDeclaration.getName().toString(), clazz);
                    allTempVertexes.put(v.getVertexId(), new Pair<>(v, node));
                }
                return true;
            }

            @Override
            public boolean visit(PrefixExpression node) {
                if (checkHasVar(node).size() != 0){
                    TempVertex v = new TempVertex(compilationUnit, node, methodDeclaration.getName().toString(), clazz);
                    allTempVertexes.put(v.getVertexId(), new Pair<>(v, node));
                }
                return true;
            }
        });
    }

    private List<ASTNode> checkHasVar(ASTNode astNode){
        List<ASTNode> result = new ArrayList<>();
        astNode.accept(new ASTVisitor() {
            @Override
            public boolean visit(SimpleName node) {
                if (node.resolveBinding() instanceof IMethodBinding){
                    return true;
                }else if(node.resolveBinding() instanceof ITypeBinding){
                    return true;
                }else {
                    Name temp = node;
                    while (temp.getParent().getNodeType() == ASTNode.QUALIFIED_NAME){
                        temp = (Name) temp.getParent();
                    }
                    // 数组
                    if (temp.resolveTypeBinding() != null && temp.resolveTypeBinding().isArray()){
                        if(ASTNode.ARRAY_ACCESS == temp.getParent().getNodeType()){
                            if (!result.contains(temp.getParent())){
                                result.add(temp.getParent());
                            }
                            return true;
                        }
                    }
                    // 域变量里的this特别处理
                    if (temp.getParent().getNodeType() == ASTNode.FIELD_ACCESS){
                        FieldAccess temp1 = (FieldAccess) temp.getParent();
                        // 针对 形如 this.b[0]额外判断
                        if (temp1.resolveTypeBinding() != null && temp1.resolveTypeBinding().isArray()){
                            if(ASTNode.ARRAY_ACCESS == temp1.getParent().getNodeType()){
                                if (!result.contains(temp1.getParent())){
                                    result.add(temp1.getParent());
                                }
                                return true;
                            }
                        }
                        if (!result.contains(temp.getParent())){
                            result.add(temp.getParent());
                        }
                        return  true;
                    }
                    if (!result.contains(temp)){
                        result.add(temp);
                    }
                }
                return true;
            }
        });
        return result;
    }

    private void addLineIntoTrace(List<Pair<MethodDeclaration, String>> trace, Pair<MethodDeclaration, String> pair){
        MethodDeclaration methodDeclaration = pair.getFirst();
        String lineNo = pair.getSecond().split(":")[1];
        String clazz = pair.getSecond().split(":")[0];
        CompilationUnit compilationUnit = (CompilationUnit) methodDeclaration.getRoot();
        methodDeclaration.accept(new ASTVisitor() {

            @Override
            public boolean visit(ReturnStatement node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                if (node.toString().contains("nextPreciserScopeKnowingConditionOutcome")){
                    System.out.println(1);
                }
                addLines(trace.indexOf(pair), compilationUnit, node, trace, methodDeclaration, clazz);

                return true;
            }

            @Override
            public boolean visit(IfStatement node) {
                if (compilationUnit.getLineNumber(node.getExpression().getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                addLines(trace.indexOf(pair), compilationUnit, node.getExpression(), trace, methodDeclaration, clazz);
                return true;
            }

            @Override
            public boolean visit(ThrowStatement node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                addLines(trace.indexOf(pair), compilationUnit, node, trace, methodDeclaration, clazz);
                return true;
            }

            @Override
            public boolean visit(Assignment node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                addLines(trace.indexOf(pair), compilationUnit, node, trace, methodDeclaration, clazz);
                return true;
            }

            @Override
            public boolean visit(VariableDeclarationFragment node) {
                if (compilationUnit.getLineNumber(node.getStartPosition()) != Integer.parseInt(lineNo)){
                    return true;
                }
                addLines(trace.indexOf(pair), compilationUnit, node, trace, methodDeclaration, clazz);
                return true;
            }
        });
    }

    String lastAdd = "";
    List<String> addedLine = new ArrayList<>();
    private void addLines(int pos, CompilationUnit compilationUnit, ASTNode astNode, List<Pair<MethodDeclaration, String>> trace, MethodDeclaration methodDeclaration, String clazz){
        int startLine = compilationUnit.getLineNumber(astNode.getStartPosition());
        int endLine = compilationUnit.getLineNumber(astNode.getStartPosition() + astNode.getLength());
        if (startLine < endLine){
            for (int i = startLine + 1; i <= endLine;i++){
                if (addedLine.contains(clazz + ":" + i)){
                    break;
                }
                trace.add(pos + 1, new Pair<>(methodDeclaration, clazz + ":" + i));
                lastAdd = clazz + ":" + i;
                addedLine.add(lastAdd);
                System.out.println("add Line " + clazz + " " + i);
            }
        }
    }


}
