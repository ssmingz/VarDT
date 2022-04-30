package pda.core.dependency.dependencyGraph;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

import java.io.Serializable;

public class VariableVertex extends DependencyGraphVertex implements Serializable {
    private String clazz;
    private String simpleName;
    private String methodName;
    private String varName;
    private int lineNo;
    private int colNo;

    public VariableVertex(String clazz, String methodName, int lineNo, String varName, int colNo){
        this.clazz = clazz;
        this.methodName = methodName;
        this.lineNo = lineNo;
        this.varName = varName;
        this.vertexType = VertexType.Variable;
        this.colNo = colNo;
    }

    public VariableVertex(CompilationUnit compilationUnit, ASTNode simpleName, String methodName, String clazz){
        this.simpleName = simpleName.toString();
        this.lineNo = compilationUnit.getLineNumber(simpleName.getStartPosition());
        this.colNo = compilationUnit.getColumnNumber(simpleName.getStartPosition());
        this.clazz = clazz;
        this.methodName = methodName;
        this.vertexType = VertexType.Variable;
    }

    public String getClazz() {
        return clazz;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNo() {
        return lineNo;
    }

    public int getColNo() {
        return colNo;
    }

    public String toString(){
        return vertexType.toString() + "#" + clazz + "." + methodName + ":" + lineNo + "|" + colNo + "-" + ((simpleName == null)?varName:simpleName);
    }

    public String getVertexId(){
        return this.toString();
    }

}
