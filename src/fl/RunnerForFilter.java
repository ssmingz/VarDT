package fl;

import fl.instr.InstrumentForFilter;
import fl.utils.Constant;
import fl.utils.JavaLogger;

import java.io.File;


/**
 * java -jar fl-runner4filter.jar ${project_path} ${project_id} ${bug_id} ${topN_methods_path} TOP_N
 * @author
 * @date 2021/7/21
 */
public class RunnerForFilter {
    public static void main(String[] args) {
        try {
            Constant.PROJECT_PATH = args[0];
            Constant.PROJECT_ID = args[1];
            Constant.BUG_ID = args[2];
            Constant.TOPN_METHODS_PATH = args[3];
            Constant.TOP_N = Integer.valueOf(args[4]).intValue();
            resetLogs();
            // Instrument to filter bugs with illegal tests
            InstrumentForFilter instr2 = new InstrumentForFilter();
            instr2.run();
        } catch (ArrayIndexOutOfBoundsException e) {
            JavaLogger.error("Enter the project name to process");
            e.printStackTrace();
        }
    }

    private static void resetLogs() {
        String jarPath = Runner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String logsPath = jarPath.substring(0, jarPath.lastIndexOf("/")) + "/logs";
        File logsDir = new File(logsPath);
        File[] logList = logsDir.listFiles();
        if(logList == null) {
            return;
        }
        for(File aLog : logList) {
            aLog.delete();
        }
    }
}
