package pda.core.dependency.dependencyGraph;

public enum EdgeType {
    DEF_USE,
    DATA_DEPENDENCY,
    CONTROL_DEPENDENCY,
    METHOD_INVOCATION,
    FIELD,
    UNKNOWN
}
