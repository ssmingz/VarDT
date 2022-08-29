package pda.core.dependency.dependencyGraph;

import java.io.Serializable;

public class MethodInvocationEdge extends DependencyGraphEdge implements Serializable {
    public MethodInvocationEdge(DependencyGraphVertex start, DependencyGraphVertex end) {
        super(start, end, EdgeType.METHOD_INVOCATION);
    }
}
