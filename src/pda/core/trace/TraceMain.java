/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.trace;

import org.apache.commons.cli.*;
import pda.common.conf.Configure;
import pda.common.conf.Constant;
import pda.common.java.D4jSubject;
import pda.common.java.JCompiler;
import pda.common.java.Subject;
import pda.common.utils.*;
import pda.core.trace.path.Collector;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author:
 * @date: 2021/11/12
 */
public class TraceMain {

    private static Options options() {
        Options options = new Options();

        Option option = new Option("dir", "DirectoryOfProject", true, "The base directory of buggy projects.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("name", "ProjectName", true, "The project name to trace.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("id", "BugId", true,
                "Ids of bugs, can be 1 or 2,3,4 or 1-10, and even their combinations like 1-10,13,18");
        option.setRequired(true);
        options.addOption(option);

        return options;
    }

    private static List<Subject> getSubjects(String[] args) {
        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LevelLogger.error(e.getMessage());
            formatter.printHelp("<command> -name <arg> -id <arg>", options);
            System.exit(1);
        }
        String base = cmd.getOptionValue("dir");
        String name = cmd.getOptionValue("name");
        String ids = cmd.getOptionValue("id");
        String[] info = ids.split(",");
        List<Subject> subjects = new LinkedList<>();
        for (String s : info) {
            String[] id = s.split("-");
            if (id.length == 2) {
                int start = Integer.parseInt(id[0]);
                int end = Integer.parseInt(id[1]);
                for (; start <= end; start++) {
                    subjects.add(new D4jSubject(base, name, start));
                }
            } else if (id.length == 1) {
                subjects.add(new D4jSubject(base, name, Integer.parseInt(id[0])));
            } else {
                LevelLogger.error("Skip unknown format : " + s);
            }
        }
        return subjects;
    }

    public static void main(String[] args) {
        List<Subject> subjects = getSubjects(args);
        for (Subject subject : subjects) {
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyy:MM:dd:HH:mm:ss");
                Date startTime = new Date();
                String begin = simpleDateFormat.format(startTime);
                LevelLogger.info("BEGIN : " + begin);

                String subjectInfo = subject.getName() + "_" + subject.getId();
                // set compile level : to refactor
                JCompiler.setSourceLevel(subject.getSourceLevelStr());
                JCompiler.setTargetLevel(subject.getSourceLevelStr());
                if (!proceed(subject)) {
                    String d4jOutput = JavaFile.readFileToString(Constant.STR_TMP_D4J_OUTPUT_FILE);
                    JavaFile.writeStringToFile(Constant.STR_ERROR_BACK_UP + "/" + subjectInfo + ".d4j.out", d4jOutput);
                }

                Date endTime = new Date();
                String end = simpleDateFormat.format(endTime);
                LevelLogger.info("BEGIN : " + begin + " - END : " + end);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean proceed(Subject subject) {
        LevelLogger
                .info("------------------ Begin : " + subject.getName() + "_" + subject.getId() + " ----------------");

        // remove auxiliary file
        String path = Utils.join(subject.getHome(), subject.getSsrc());
        String auxiliary = Utils.join(path, Constant.AUXILIARY_DUMPER_REL_FILE);

        Utils.deleteFiles(auxiliary);
        Utils.deleteFiles(Constant.STR_TMP_TRACE_OUTPUT_FILE);
        subject.clearClass();
        subject.backupSource();
        subject.restoreSource();
        Identifier.resetAll();

        // copy auxiliary file to subject path
        LevelLogger.info("copying auxiliary file to subject path.");
        Configure.config_dumper(subject);
        // fix bug for ast parser
        Configure.config_astlevel(subject);

        Identifier.restore(subject);
        Pair<Set<Integer>, Set<Integer>> failedTestsAndCoveredMethods = null;

        Date startTime = new Date();
        boolean recover = false;
        if (!Constant.BOOL_RECOMPUTE_ORI) {
            Set<Integer> allCoveredMethods = new HashSet<>();
            Set<Integer> failedTests = new HashSet<>();
            recover = Utils.recoverFailedTestsAndCoveredMethod(subject, failedTests, allCoveredMethods);
            failedTestsAndCoveredMethods = new Pair<>(failedTests, allCoveredMethods);
        }

        if (!recover) {
            LevelLogger.info("Start to collect failed test and covered methods...");
            failedTestsAndCoveredMethods = Collector.collectFailedTestAndCoveredMethod(subject);
            LevelLogger.info("Finish to collect failed test and covered methods.");
            Utils.backupFailedTestsAndCoveredMethod(subject, 0, failedTestsAndCoveredMethods.getFirst(),
                    failedTestsAndCoveredMethods.getSecond());
        }
        Identifier.backup(subject);
        Date middleTime = new Date();
        Tracer tracer = new Tracer(subject);
        tracer.setTestCases(failedTestsAndCoveredMethods.getFirst());
        tracer.setInstrumentMethods(failedTestsAndCoveredMethods.getSecond());
        tracer.trace(subject.getOutBase());

        Date endTime = new Date();

        JavaFile.writeStringToFile(Constant.STR_TIME_LOG, subject.getName() + "_" + subject.getId() + "\t"
                + Long.toString(middleTime.getTime() - startTime.getTime()) + "\t"
                + Long.toString(endTime.getTime() - middleTime.getTime()) + "\t" + startTime + "\n", true);
        return true;
    }

}
