package fl.soot;

import fl.utils.DataFlowParaPack;
import fl.utils.JavaFile;
import fl.utils.SearchFile;
import org.eclipse.jdt.core.dom.CompilationUnit;

import java.io.File;

/**
 * @description: each time load only one DataFlowParaPack object
 * @author:
 * @time: 2021/8/30 10:17
 */
public class LoadDataFlowAnalysis {
    /**
     * java -jar fl-dfa.jar \
     * buggy_folder/sootOutput/dataFlowParaPack1.dat
     * @param args
     */
    public static void main(String[] args) {
        runAt(args[0]);
    }

    private static void runAt(String path) {
        DataFlowParaPack dfpp = (DataFlowParaPack) JavaFile.readObjectFromFile(path);
        CompilationUnit cu = JavaFile.genASTFromSource(dfpp._src, dfpp._targetPath);

        // change paths of dfpp since using in linux while creating in win
        dfpp._classpath = dfpp._classpath.replaceAll("\\\\", "/");
        dfpp._processdir = dfpp._processdir.replaceAll("\\\\", "/");
        dfpp._projectPath = dfpp._projectPath.replaceAll("\\\\", "/");
        dfpp._targetPath = dfpp._targetPath.replaceAll("\\\\", "/");

        String pro_id = path.substring(0, path.indexOf("/sootOutput"));
        pro_id = pro_id.substring(pro_id.lastIndexOf("/"));
        String path_should_be = path.substring(0, path.indexOf(pro_id));
        String path_to_replace = dfpp._classpath.substring(0, dfpp._classpath.indexOf(pro_id));

        dfpp._classpath = dfpp._classpath.replaceAll(path_to_replace, path_should_be);
        dfpp._processdir = dfpp._processdir.replaceAll(path_to_replace, path_should_be);
        dfpp._projectPath = dfpp._projectPath.replaceAll(path_to_replace, path_should_be);
        dfpp._targetPath = dfpp._targetPath.replaceAll(path_to_replace, path_should_be);
        DataFlowAnalysis analysis = new DataFlowAnalysis(dfpp._classpath, dfpp._processdir, dfpp._className, dfpp._methodName, dfpp._paras, cu);
        //IFDSDataFlowAnalysis analysis = new IFDSDataFlowAnalysis(dfpp._classpath, dfpp._processdir, dfpp._className, dfpp._methodName, dfpp._paras, cu);
        analysis.doAnalysis();
        // get result
        String name1 = "defUseByLine" + dfpp._id;
        String name2 = "aggreEquiLocals" + dfpp._id;
        JavaFile.writeObjectToFile(analysis._defUse, dfpp._projectPath + "/sootOutput/" + name1 + ".dat");
        JavaFile.writeObjectToFile(analysis._aggreEquiLocals, dfpp._projectPath + "/sootOutput/" + name2 + ".dat");
    }

}
