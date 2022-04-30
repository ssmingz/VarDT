package fl.utils;

/**
 * @description:
 * @author:
 * @time: 2021/8/28 17:09
 */
public class BuggyMethodInfo {
    private static final String _name = "@BuggyMethodInfo ";

    // org.apache.commons.lang3.time$FastDateParser#parse(java.lang.String)
    public String _methodName; // parse
    public String _className; // FastDateParser
    public String _original; // org.apache.commons.lang3.time$FastDateParser#parse(java.lang.String)
    public String _nameWithTypes; // org.apache.commons.lang3.time.FastDateParser.parse(java.lang.String)

    public BuggyMethodInfo(String methodName, String className, String original, String nameWithTypes) {
        _methodName = methodName;
        _className = className;
        _original = original;
        _nameWithTypes = nameWithTypes;
    }
}
