package pda.core.dependency.dependencyGraph;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class TempVertex extends DependencyGraphVertex{
    private String clazz;
    private String simpleName;
    private String methodName;
    private String varName;
    private int lineNo;
    private int colNo;
    private int size;

    public TempVertex(String clazz, String methodName, int lineNo, String varName, int colNo, int size){
        this.clazz = clazz;
        this.methodName = methodName;
        this.lineNo = lineNo;
        this.varName = varName;
        this.vertexType = VertexType.Temp;
        this.colNo = colNo;
        this.size = size;
    }

    public TempVertex(CompilationUnit compilationUnit, ASTNode simpleName, String methodName, String clazz){
        this.simpleName = simpleName.toString();
        this.lineNo = compilationUnit.getLineNumber(simpleName.getStartPosition());
        this.colNo = compilationUnit.getColumnNumber(simpleName.getStartPosition());
        this.size = simpleName.getLength();
        this.clazz = clazz;
        this.methodName = methodName;
        this.vertexType = VertexType.Temp;
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

    public int getColNo(){
        return colNo;
    }

    public int getSize(){
        return size;
    }

    public String toString(){
        return vertexType.toString() + "#" + clazz + "." + methodName + ":" + lineNo + "|" + colNo + "|" + size + "-TEMP135241";
    }

    public String getVertexId(){
        return this.toString();
    }
}
