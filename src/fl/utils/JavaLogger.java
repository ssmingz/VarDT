package fl.utils;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.File;

/**
 * @description:
 * @author:
 * @time: 2021/7/21 18:00
 */
public class JavaLogger {
    private static Logger _logger = Logger.getLogger(JavaLogger.class);

    static {
        File f = new File("res/conf/log4j.properties");
        if (f.exists()) {
            PropertyConfigurator.configure("res/conf/log4j.properties");
        } else {
            BasicConfigurator.configure();
        }
    }

    public static void error(Object message) {
        _logger.error(message);
    }

    public static void info(Object message) {
        _logger.info(message);
    }

    public static void warn(Object message) {
        _logger.warn(message);
    }

    public static void debug(Object message) {
        _logger.debug(message);
    }
}
