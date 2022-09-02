/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.trace.path;

import pda.common.conf.Constant;
import pda.common.java.Subject;
import pda.common.utils.*;
import pda.core.trace.Runner;
import pda.core.trace.inst.Instrument;
import pda.core.trace.inst.visitor.MethodInstrumentVisitor;
import pda.core.trace.inst.visitor.StatementInstrumentVisitor;
import pda.core.trace.inst.visitor.TraceTestMethodInstrumentVisitor;
import pda.core.trace.inst.visitor.TraversalVisitor;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * @author
 *
 */
public class Coverage {

    private final static String __name__ = "@Coverage ";

    /**
     * compute the original coverage information of statements, it can compute the
     * traditional statement coverage and branch coverage based on the argument
     * {@code visitor}
     * 
     * @param subject:
     *                                    subject to be tested
     * @param failedTestAndCoveredMethods
     *                                    : a pair that contains the ids of failed
     *                                    test cases and covered
     *                                    methods by failed test cases.
     *
     * @return
     */
    public static Map<String, CoverInfo> computeOriginalCoverage(Subject subject,
            Pair<Set<Integer>, Set<Integer>> failedTestAndCoveredMethods) {

        String src = Utils.join(subject.getHome(), subject.getSsrc());
        String test = Utils.join(subject.getHome(), subject.getTsrc());

        TraversalVisitor traversalVisitor = new MethodInstrumentVisitor(failedTestAndCoveredMethods.getSecond());

        // instrument those methods ran by failed tests
        Instrument.execute(src, traversalVisitor);

        TraceTestMethodInstrumentVisitor newTestMethodInstrumentVisitor = new TraceTestMethodInstrumentVisitor(
                failedTestAndCoveredMethods.getFirst());
        Instrument.execute(test, newTestMethodInstrumentVisitor);

        // delete all bin file to make it re-compiled
        subject.clearClass();
        Utils.deleteFiles(Constant.STR_TMP_INSTR_OUTPUT_FILE);

        Set<Integer> testcases = failedTestAndCoveredMethods.getFirst();
        for (Integer methodID : testcases) {
            Method method = Method.parse(Identifier.getMessage(methodID));
            if (method == null) {
                LevelLogger.error(__name__ + "#collectCoveredMethod method string format error : "
                        + Identifier.getMessage(methodID));
                continue;
            }
            String singleTest = method.getClazz() + "::" + method.getName();
            boolean success = Runner.testSingleCase(subject, singleTest);
            if (!success) {
                LevelLogger
                        .error(__name__ + "#collectCoveredMethod build subject failed when running single test case.");
                continue;
            }
        }

        subject.restoreSource();
        return ExecutionPathBuilder.buildCoverage(Constant.STR_TMP_INSTR_OUTPUT_FILE, new HashSet<>());
    }

    /**
     * get all statements covered by given test cases
     * 
     * @param subject
     *                  : subject to be tested
     * @param testcases
     *                  : test cases to be computed
     * @return a set of statements covered by the given test cases
     */
    public static Set<String> getAllCoveredStatement(Subject subject, Set<Integer> testcases) {
        Set<String> coveredStatement = new HashSet<>();

        // instrument those methods ran by failed tests
        // StatementInstrumentVisitor statementInstrumentVisitor = new
        // StatementInstrumentVisitor(coveredMethod);
        StatementInstrumentVisitor statementInstrumentVisitor = new StatementInstrumentVisitor();
        Instrument.execute(Utils.join(subject.getHome(), subject.getSsrc()), statementInstrumentVisitor);

        int allTestCount = testcases.size();
        int currentCount = 1;
        for (Integer testID : testcases) {
            String testString = Identifier.getMessage(testID);

            LevelLogger.info("Test [" + (currentCount++) + " / " + allTestCount + "] : " + testString);

            String[] testInfo = testString.split("#");
            if (testInfo.length < 4) {
                LevelLogger.error(__name__ + "#getAllCoveredStatement test format error : " + testString);
                System.exit(0);
            }
            String testcase = testInfo[0] + "::" + testInfo[2];
            // run each test case and collect all test statements covered
            try {
                ExecuteCommand.executeDefects4JTest(CmdFactory.createTestSingleCmd(subject, testcase));
            } catch (Exception e) {
                LevelLogger.fatal(__name__ + "#getAllCoveredStatement run test suite failed !", e);
            }

            Map<String, Integer> tmpCover = ExecutionPathBuilder
                    .collectAllExecutedStatements(Constant.STR_TMP_INSTR_OUTPUT_FILE);
            for (Entry<String, Integer> entry : tmpCover.entrySet()) {
                String statement = entry.getKey();
                coveredStatement.add(statement);
            }
        }
        return coveredStatement;
    }

    // /**
    // *
    // * @param subject
    // * @param allStatements
    // * @param failedTests
    // * @return <statementString, coverageInformation>,
    // * statementString:"MethodID#line"
    // */
    // public static Map<String, CoverInfo> computePredicateCoverage(Subject
    // subject, Model model, Set<String> allStatements,
    // Set<Integer> failedTests, boolean useSober) {
    // String srcPath = subject.getHome() + subject.getSsrc();
    // String testPath = subject.getHome() + subject.getTsrc();
    // TestMethodInstrumentVisitor newTestMethodInstrumentVisitor = new
    // TestMethodInstrumentVisitor(failedTests, useSober);
    // Instrument.execute(testPath, newTestMethodInstrumentVisitor);
    //
    // long start = System.currentTimeMillis();
    // Map<String, Map<Integer, List<Pair<String, String>>>> file2Line2Predicates =
    // model.getAllPredicates(subject,
    // allStatements, useSober);
    // model.instrumentPredicates(file2Line2Predicates, useSober);
    // long duration = System.currentTimeMillis() - start;
    // LevelLogger.info("Predicate validation time : " +
    // Utils.transformMilli2Time(duration));
    //
    // // delete all bin file to make it re-compiled
    // ExecuteCommand.deleteGivenFolder(subject.getHome() + subject.getSbin());
    // ExecuteCommand.deleteGivenFolder(subject.getHome() + subject.getTbin());
    // ExecuteCommand.deleteGivenFile(Constant.STR_TMP_INSTR_OUTPUT_FILE);
    // Configure.compileAuxiliaryJava(subject);
    // // if the instrumented project builds success, and the test
    // // result is the same with original project
    // if (!Runner.testSuite(subject, Constant.TIME_OUT_RUN_TEST_SUITE)) {
    // LevelLogger.error("Build failed by predicates : ");
    // JavaFile.writeStringToFile(Constant.STR_RESULT_RECORD_LOG, "Project : " +
    // subject.getName() + "_" + subject.getId() + " Build failed by predicates!\n",
    // true);
    // return null;
    // }
    // if (!isSameTestResult(failedTests, Constant.STR_TMP_D4J_OUTPUT_FILE)) {
    // LevelLogger.error("Cause different test state by predicates :");
    // JavaFile.writeStringToFile(Constant.STR_RESULT_RECORD_LOG, "Project : " +
    // subject.getNameAndId() + " Different test result!\n", true);
    // logErrorInfo(subject, failedTests);
    // } else {
    // JavaFile.writeStringToFile(Constant.STR_RESULT_RECORD_LOG, "Project : " +
    // subject.getNameAndId() + " Success!\n", true);
    // }
    //
    // Map<String, CoverInfo> coverage = null;
    // if (!useSober) {
    // coverage =
    // ExecutionPathBuilder.buildCoverage(Constant.STR_TMP_INSTR_OUTPUT_FILE);
    // }
    //
    // ExecuteCommand.copyFolder(srcPath + "_ori", srcPath);
    // ExecuteCommand.copyFolder(testPath + "_ori", testPath);
    // return coverage;
    // }

    /**
     * log error information when the predicates cause different test results
     * 
     * @param subject
     *                    : the subject to test
     * @param failedTests
     *                    : the ids of failed test cases
     */
    private static void logErrorInfo(Subject subject, Set<Integer> failedTests) {
        String subjectNameAndId = subject.getName() + "_" + subject.getId();
        String diff_result_error = Constant.HOME + "/info/" + subjectNameAndId + "_diff.log";
        StringBuffer stringBuffer = new StringBuffer();
        stringBuffer.append("Project : " + subjectNameAndId + "\n");
        stringBuffer.append("---Original failed test cases : \n");
        for (Integer integer : failedTests) {
            stringBuffer.append(Identifier.getMessage(integer) + "\n");
        }
        stringBuffer.append("---Failed test cases after instrument: \n");
        for (Integer integer : Collector.findFailedTestFromFile(Constant.STR_TMP_D4J_OUTPUT_FILE)) {
            stringBuffer.append(Identifier.getMessage(integer) + "\n");
        }
        stringBuffer.append(JavaFile.readFileToString(Constant.STR_TMP_D4J_OUTPUT_FILE));
        JavaFile.writeStringToFile(diff_result_error, stringBuffer.toString());
    }

    /**
     * test result should have the same failed test cases with the given failed
     * test cases
     * 
     * @param failedTest
     * @param outputFile
     * @return
     */
    private static boolean isSameTestResult(Set<Integer> failedTest, String outputFile) {
        Set<Integer> realFailedTests = Collector.findFailedTestFromFile(outputFile);
        if (realFailedTests.size() != failedTest.size()) {
            return false;
        }
        for (Integer fail : realFailedTests) {
            if (!failedTest.contains(fail)) {
                return false;
            }
        }
        return true;
    }

}
