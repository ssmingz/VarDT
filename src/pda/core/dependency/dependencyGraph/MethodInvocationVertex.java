package pda.core.dependency.dependencyGraph;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.SimpleName;

import java.io.Serializable;

public class MethodInvocationVertex  extends DependencyGraphVertex implements Serializable {

    private String clazz;
    private String simpleName;
    private String methodName;
    private String varName;
    private int lineNo;
    private int location;

    public MethodInvocationVertex(String clazz, String methodName, int lineNo, String varName, int location){
        this.clazz = clazz;
        this.methodName = methodName;
        this.lineNo = lineNo;
        this.varName = varName;
        this.vertexType = VertexType.MethodArgument;
        this.location = location;
    }

    public MethodInvocationVertex(CompilationUnit compilationUnit, ASTNode simpleName, String methodName, String clazz, int location){
        this.simpleName = simpleName.toString();
        this.lineNo = compilationUnit.getLineNumber(simpleName.getStartPosition());
        this.clazz = clazz;
        this.methodName = methodName;
        this.vertexType = VertexType.MethodArgument;
        this.location = location;
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

    public String toString(){
        return vertexType.toString() + "#" + location + "#" + clazz + "." + methodName + ":" + lineNo + "-" + ((simpleName == null)?varName:simpleName);
    }

    public String getVertexId(){
        return this.toString();
    }
}
