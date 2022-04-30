package fl.utils;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;

import java.io.Serializable;
import java.util.List;

/**
 * @description:
 * @author:
 * @time: 2021/8/30 9:27
 */
public class DataFlowParaPack implements Serializable {
    private static final String _name = "@DataFlowParaPack ";

    public String _src;
    public String _projectPath;
    public String _targetPath;
    public String _curBuggyMethodWithType;
    public String _curBuggyMethod;

    public String _classpath;
    public String _processdir;
    public String _className;
    public String _methodName;
    public List<String> _paras;

    public String _id;


    public DataFlowParaPack(String src, String projectPath, String targetPath, String curBuggyMethodWithType, String curBuggyMethod) {
        _src = src;
        _projectPath = projectPath;
        _targetPath = targetPath;
        _curBuggyMethodWithType = curBuggyMethodWithType;
        _curBuggyMethod = curBuggyMethod;
    }

    public void initOtherInfo(String classpath, String processdir, String className, String methodName, List<String> paras) {
        _classpath = classpath;
        _processdir = processdir;
        _className = className;
        _methodName = methodName;
        _paras = paras;
    }
}
