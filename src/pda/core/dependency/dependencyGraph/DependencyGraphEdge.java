package pda.core.dependency.dependencyGraph;

import java.io.Serializable;

public class DependencyGraphEdge  implements Serializable {
    private DependencyGraphVertex start, end;
    private EdgeType edgeType;
    public DependencyGraphEdge(DependencyGraphVertex start, DependencyGraphVertex end, EdgeType edgeType){
        this.start = start;
        this.edgeType = edgeType;
        this.end = end;
    }

    public EdgeType getEdgeType(){
        return edgeType;
    }

    public DependencyGraphVertex getStartVertex(){
        return start;
    }

    public DependencyGraphVertex getEndVertex(){
        return end;
    }

    public String toString(){return start + "->" + end + ":" + edgeType.toString();}
}
