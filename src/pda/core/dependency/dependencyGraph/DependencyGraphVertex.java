package pda.core.dependency.dependencyGraph;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class DependencyGraphVertex implements Serializable {
    // 从这个点出发能直接到达的点
    Set<DependencyGraphVertex> toVertexes;
    // 以这个点为起始的边
    Set<DependencyGraphEdge> startEdges;

    // 能直接到达这个点的点
    Set<DependencyGraphVertex> fromVertexes;
    // 以这个点为结束的边
    Set<DependencyGraphEdge> endEdges;
    VertexType vertexType;

    public DependencyGraphVertex(){
        toVertexes = new HashSet<>();
        startEdges = new HashSet<>();
        fromVertexes = new HashSet<>();
        endEdges = new HashSet<>();
    }

    public void connect(DependencyGraphVertex node, EdgeType edgeType){
        // 节点去重 边去重
        DependencyGraphEdge edge = new DependencyGraphEdge(this, node, edgeType);
        if (!toVertexes.contains(node)){
            startEdges.add(edge);
            node.endEdges.add(edge);
        }
        toVertexes.add(node);
        node.fromVertexes.add(this);
    }

    public DependencyGraphEdge findStartEdge(DependencyGraphVertex vertex){
        for (DependencyGraphEdge startEdge : startEdges) {
            if (startEdge.getEndVertex().sameVertex(vertex)){
                return startEdge;
            }
        }
        return null;
    }

    public DependencyGraphEdge findEndEdge(DependencyGraphVertex vertex){
        for (DependencyGraphEdge endEdge : endEdges) {
            if (endEdge.getStartVertex().sameVertex(vertex)){
                return endEdge;
            }
        }
        return null;
    }

    public abstract String getVertexId();

    public boolean sameVertex(DependencyGraphVertex node){
        return getVertexId().equals(node.getVertexId());
    }

    public Set<DependencyGraphVertex> getFromVertexes() {
        return fromVertexes;
    }

    public Set<DependencyGraphVertex> getToVertexes() {
        return toVertexes;
    }

    public Set<DependencyGraphEdge> getEndEdges() {
        return endEdges;
    }

    public Set<DependencyGraphEdge> getStartEdges() {
        return startEdges;
    }

    public VertexType getVertexType() {
        return vertexType;
    }
}
