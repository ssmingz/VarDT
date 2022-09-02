/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.common.conf;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import pda.common.utils.LevelLogger;
import pda.common.utils.Utils;

import java.io.*;
import java.util.Properties;

/**
 * @author:
 * @date: 2021/11/2
 */
public class Constant {

    /* basic path configurations */
    public final static String HOME = System.getProperty("user.dir");

    /**
     * string of "/" in Linux, but "\\" in Windows
     */
    public final static String DIR_SEPARATOR = File.separator;
    /**
     * char of '/' in Linux, but '\\' in Windows
     */
    public final static char DIR_SEPARATOR_CHAR = File.separatorChar;
    /**
     * string of ":" in Linux, but ";" in Windows
     */
    public final static String PATH_SEPARATOR = File.pathSeparator;
    /**
     * char of ':' in Linux, but ';' in Windows
     */
    public final static char PATH_SEPARATOR_CHAR = File.pathSeparatorChar;

    public final static String NEW_LINE = "\n";

    // used for instrument
    public final static String INSTRUMENT_DOT_SEPARATOR = ".";
    public final static String INSTRUMENT_FLAG = "";// "[INST]"
    public final static String INSTRUMENT_K_TEST = "T";
    public final static String INSTRUMENT_K_SOURCE = "M";

    public final static boolean BOOL_ADD_NULL_PREDICATE_FOR_ASSGIN = true;
    public final static boolean BOOL_RECOMPUTE_ORI = true;

    public static int AST_LEVEL = AST.JLS8;
    public static String JAVA_VERSION = JavaCore.VERSION_1_7;

    // system command
    public static String COMMAND_CD = null;
    public static String COMMAND_TIMEOUT = null;
    public static String COMMAND_JAVA = null;
    public static String COMMAND_D4J = null;
    public static String COMMAND_JAVA_HOME = null;

    // build flags
    public final static String ANT_BUILD_FAILED = "BUILD FAILED";
    public final static String ANT_BUILD_SUCCESS = "BUILD SUCCESSFUL";

    /**
     * ./resources
     */
    public final static String RES_DIR = Utils.join(HOME, "resources");
    /**
     * =========== Defects4j configure information =========
     */
    /**
     * ./resources/d4j-info
     */
    private final static String D4J_INFO_DIR = Utils.join(RES_DIR, "d4j-info");
    /**
     * ./resources/d4jlibs
     */
    public final static String D4J_LIB_DIR = Utils.join(D4J_INFO_DIR, "d4jlibs");
    /**
     * ./resources/src_path
     */
    public final static String D4J_SRC_INFO = Utils.join(D4J_INFO_DIR, "src_path");

    /**
     * ./out
     */
    public final static String STR_OUT_PATH = Utils.join(HOME, "out");
    /**
     * ./info
     */
    public final static String STR_INFO_OUT_PATH = Utils.join(HOME, "info");
    /**
     * ./out/d4j.out
     */
    public final static String STR_TMP_D4J_OUTPUT_FILE = Utils.join(STR_OUT_PATH, "d4j.out");
    /**
     * ./out/path.out
     */
    public final static String STR_TMP_INSTR_OUTPUT_FILE = Utils.join(STR_OUT_PATH, "path.out");
    /**
     * ./out/trace.out
     */
    public final static String STR_TMP_TRACE_OUTPUT_FILE = Utils.join(STR_OUT_PATH, "trace.out");
    /**
     * ./out/failed.test
     */
    public final static String STR_FAILED_TEST_FILE = Utils.join(STR_OUT_PATH, "failed.test");
    /**
     * ./out/passed.test
     */
    public final static String STR_PASSED_TEST_FILE = Utils.join(STR_OUT_PATH, "passed.test");
    /**
     * ./out/data
     */
    public final static String STR_ALL_DATA_COLLECT_PATH = Utils.join(STR_OUT_PATH, "data");
    /**
     * ./out/debug.log
     */
    public final static String STR_LOG_FILE = Utils.join(STR_OUT_PATH, "debug.log");
    /**
     * ./out/error.log
     */
    public final static String STR_ERROR_BACK_UP = Utils.join(STR_OUT_PATH, "error.log");
    /**
     * ./out/time.log
     */
    public final static String STR_TIME_LOG = Utils.join(STR_OUT_PATH, "time.log");
    /**
     * ./rlst.log
     */
    public final static String STR_RESULT_RECORD_LOG = Utils.join(HOME, "rlst.log");

    /**
     * base directory for info output
     */
    public static String DUMPER_HOME = HOME;

    /**
     * auxiliary/Dumper.java
     */
    public final static String AUXILIARY_DUMPER_REL_FILE = Utils.join("auxiliary", "Dumper.java");

    static {
        Properties prop = new Properties();
        try {
            String filePath = Utils.join(Constant.RES_DIR, "conf", "system.properties");
            InputStream in = new BufferedInputStream(new FileInputStream(filePath));
            prop.load(in);

            // System commands
            Constant.COMMAND_JAVA_HOME = prop.getProperty("COMMAND.JAVA_HOME").replace("/", Constant.DIR_SEPARATOR);
            Constant.COMMAND_JAVA = Utils.join(COMMAND_JAVA_HOME, "bin", "java ");

            Constant.COMMAND_CD = prop.getProperty("COMMAND.CD").replace("/", Constant.DIR_SEPARATOR) + " ";
            Constant.COMMAND_TIMEOUT = prop.getProperty("COMMAND.TIMEOUT").replaceAll("/", Constant.DIR_SEPARATOR)
                    + " ";
            Constant.COMMAND_D4J = prop.getProperty("COMMAND.D4J").replace("/", Constant.DIR_SEPARATOR) + " ";
            in.close();
        } catch (IOException e) {
            LevelLogger.error("#config_system get properties failed!" + e.getMessage());
        }
    }

}
