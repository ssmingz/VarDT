package pda.core.dependency.dependencyGraph;

import java.io.Serializable;

public class DefUseEdge extends DependencyGraphEdge implements Serializable {

    public DefUseEdge(DependencyGraphVertex start, DependencyGraphVertex end) {
        super(start, end, EdgeType.DEF_USE);
    }

}
