package fl.instr.gen;

/**
 * @description:
 * @author:
 * @time: 2021/7/21 22:15
 */
public class GenPath {
    /**
     * compute path of the file containing the given method
     * @param fullMethodName : full method name split by dot, e.g. org.apache.commons.lang3.math.NumberUtils.createNumber
     * @return
     */
    public static String generateFilePath(String projectPath, String fullMethodName) {
        String result = fullMethodName.replace('.', '/');
        result = result.substring(0, result.lastIndexOf('/'));
        result = projectPath + "/src/main/java/" + result + ".java";
        return result;
    }
}
