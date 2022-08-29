package pda.core.slice;

import pda.common.java.D4jSubject;
import pda.core.dependency.DependencyParser;
import pda.core.dependency.dependencyGraph.DependencyGraph;
import pda.core.dependency.dependencyGraph.DependencyGraphVertex;
import pda.core.dependency.dependencyGraph.VertexType;

import java.util.*;

public class Slicer {
    DependencyParser dependencyParser;

    public Slicer(D4jSubject subject, String traceFile){
        dependencyParser = new DependencyParser();
        dependencyParser.parse(traceFile, subject);
    }

    public Slicer(D4jSubject subject, String traceFile, boolean optimize){
        dependencyParser = new DependencyParser(optimize);
        dependencyParser.parse(traceFile, subject);
    }

    public List<String> slice(String criterion){
        List<String> result = new ArrayList<>();
        String [] cs = criterion.split(":");
        if (cs.length != 2){
            System.out.println("Error criterion!");
        }
        Stack<DependencyGraphVertex> vertexesToParse = new Stack<>();
        Stack<DependencyGraphVertex> vertexesParsed = new Stack<>();
        String clazz = cs[0];
        String lineNo = cs[1];
        DependencyGraph dependencyGraph = dependencyParser.getDependencyGraph();
        for (String vs: dependencyGraph.getVertexes().keySet()){
            if (vs.contains(clazz)){
                if (vs.split(":")[1].startsWith(lineNo)){
                    vertexesToParse.push(dependencyGraph.getVertexes().get(vs));
                }
            }
        }

        if (vertexesToParse.isEmpty()){
            System.out.println("No vertex");
        }

        while (!vertexesToParse.isEmpty()){
            DependencyGraphVertex v = vertexesToParse.pop();
            List<DependencyGraphVertex> trace = new ArrayList<>();
            vertexParsed.clear();
            parseVertex(v, new ArrayList<>(), trace, false);
            for (DependencyGraphVertex v1 : trace){
                if (vertexesParsed.contains(v1) || vertexesToParse.contains(v1)){
                    continue;
                }else {
                    vertexesToParse.add(v1);
                }
            }
            vertexesParsed.add(v);
        }

        for (DependencyGraphVertex v : vertexesParsed){
            String line = v.getVertexId();
            if (line.startsWith("Var") || line.startsWith("Temp")){
                line = v.getVertexId().split("#")[1].split("\\|")[0];
            }else if (line.startsWith("Method")){
                continue;
            }
            String [] lines = line.split("\\.");
            lines[lines.length - 1] = ":" + lines[lines.length - 1].split(":")[1];
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0;i < lines.length;i++){
                if (i != 0 && i != lines.length - 1){
                    stringBuilder.append(".");
                }
                stringBuilder.append(lines[i]);
            }
            result.add(stringBuilder.toString());
        }
        Collections.sort(result);
        List<String> newList = new ArrayList<>();
        for (String s : result){
            if (!newList.contains(s)){
                newList.add(s);
            }
        }
        return newList;
    }

    private Set<DependencyGraphVertex> vertexParsed = new HashSet<>();

    private void parseVertex(DependencyGraphVertex vertex, List<DependencyGraphVertex> list, List<DependencyGraphVertex> result, boolean reverse){
        if ((vertex.getFromVertexes().size() == 0 && !reverse)
                || (vertex.getToVertexes().size() == 0 && reverse)
                || (list.size() != 0 && list.subList(0, list.size() - 1).contains(vertex))
                || (vertexParsed.contains(vertex))
        ){
            result.addAll(list);
        }else {
            vertexParsed.add(vertex);
            if (!reverse){
                for (DependencyGraphVertex v : vertex.getFromVertexes()){
                    list.add(v);
                    parseVertex(v, list, result, false);
                    list.remove(v);
                }
                for (DependencyGraphVertex v: vertex.getToVertexes()){
                    if (v.getVertexId().startsWith(VertexType.MethodArgument.toString())){
                        list.add(v);
                        parseVertex(v, list, result, true);
                        list.remove(v);
                    }
                }
            }else {
                for (DependencyGraphVertex v : vertex.getToVertexes()) {
                    list.add(v);
                    parseVertex(v, list, result, true);
                    list.remove(v);
                }
            }


        }
    }
}
