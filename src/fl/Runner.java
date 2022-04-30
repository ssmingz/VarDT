package fl;

import fl.instr.Instrument;
import fl.instr.InstrumentForFilter;
import fl.utils.Constant;
import fl.utils.JavaLogger;

import java.io.File;


/**
 * java -jar fl-runner.jar ${project_path} ${project_id} ${bug_id} ${slice_path} PDAtrace/PDAslice TOP_N
 * @author
 * @date 2021/7/21
 */
public class Runner {
    public static void main(String[] args) {
        try {
            Constant.PROJECT_PATH = args[0];
            Constant.PROJECT_ID = args[1];
            Constant.BUG_ID = args[2];
            Constant.SLICE_PATH = args[3];
            Constant.SLICER_SWITCH = args[4];
            Constant.TOP_N = Integer.valueOf(args[5]).intValue();
            resetLogs();
            Instrument instr = new Instrument();
            instr.run();
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
