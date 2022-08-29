package fl.instr.gen;

import fl.utils.JavaFile;

import java.io.File;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

/**
 * @description: generate class names for suite tests
 * @author:
 * @time: 2021/7/31 11:34
 */
public class GenSuiteTest {
    /**
     * returns a list of target test class names in test.csv
     * @param testsCsv
     */
    public static void genSuiteTestNames(File testsCsv) {
        LinkedHashMap<String, Set<String>> tests = JavaFile.readTestsCSV(testsCsv);
        Iterator itr = tests.keySet().iterator();
        String classes = "";
        while(itr.hasNext()) {
            String name = (String) itr.next();
            name = name.substring(name.lastIndexOf(".")+1);
            if(classes.length() != 0) {
                classes += "," + "\n" + name + ".class";
            } else {
                classes += name + ".class";
            }
        }
        System.out.println(classes);
    }

    public static void main(String[] args) {
        String projectPath = "E:/java/FaultLocalization/EXP/Time/Time_16_buggy";
        String postfix = "/build/sfl/txt/tests.csv";
        File csv = new File(projectPath+postfix);
        genSuiteTestNames(csv);
    }
}
