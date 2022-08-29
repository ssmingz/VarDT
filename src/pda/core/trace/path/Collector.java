/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
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
import pda.core.trace.inst.visitor.TestMethodInstrumentVisitor;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Jiajun
 *
 */
public class Collector {

	private final static String __name__ = "@Collector ";

	/**
	 * collect the failed test cases and the methods covered by them
	 * @param subject : the subject to be tested
	 * @return a pair that contains the <p>ids of failted test cases</p> and
	 * the <p>ids of covered methods</p>
	 */
	public static Pair<Set<Integer>, Set<Integer>> collectFailedTestAndCoveredMethod(Subject subject){
		// run all test
		try {
			ExecuteCommand.executeDefects4JTest(CmdFactory.createTestSuiteCmd(subject));
		} catch (Exception e) {
			LevelLogger.fatal(__name__ + "#collectAllTestCases run test failed !", e);
			return null;
		}
		Set<Integer> failedTest = findFailedTestFromFile(Constant.STR_TMP_D4J_OUTPUT_FILE);
		Utils.copyFile(Constant.STR_TMP_D4J_OUTPUT_FILE, Utils.join(subject.getOutBase(), "original_test.log"));
		Set<Integer> coveredMethods = collectCoveredMethod(subject, failedTest);
		return new Pair<Set<Integer>, Set<Integer>>(failedTest, coveredMethods);
	}

	/**
	 * collect all failed test cases by parsing the d4j output information
	 * 
	 * @param outputFilePath
	 *            : defects4j output file path
	 * @return a set of method ids of failed test cases
	 */
	public static Set<Integer> findFailedTestFromFile(String outputFilePath) {
		if (outputFilePath == null) {
			LevelLogger.error(__name__ + "#findFailedTestFromFile OutputFilePath is null.");
			return null;
		}
		File file = new File(outputFilePath);
		BufferedReader bReader = null;
		try {
			bReader = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			LevelLogger.error(__name__ + "@findFailedTestFromFile BufferedReader init failed.");
			return null;
		}

		String line = null;
		Set<Integer> failedTest = new HashSet<>();
		try {
			while ((line = bReader.readLine()) != null) {
				String trimed = line.trim();
				if (trimed.startsWith(Constant.ANT_BUILD_FAILED)) {
					LevelLogger.error(__name__ + "#findFailedTestFromFile Ant build failed.");
					break;
				}
				if (trimed.startsWith("Failing tests:")) {
					String count = trimed.substring("Failing tests:".length());
					int failingCount = Integer.parseInt(count.trim());
					while (failingCount > 0) {
						line = bReader.readLine();
						int index = line.indexOf("-");
//						if (index > 0 ) {
						if (index > 0 && !line.contains("MainTest")) {
							String testStr = line.substring(index + 2).trim();
							String[] testInfo = testStr.split("::");
							if (testInfo.length != 2) {
								LevelLogger
										.error(__name__ + "#findFailedTestFromFile Failed test cases format error !");
								System.exit(0);
							}
							// convert "org.jfree.chart.Clazz::test" to
							// "org.jfree.chart.Clazz#void#test#?"
							String identifierString = testInfo[0] + "#void#" + testInfo[1] + "#?";
							int methodID = Identifier.getIdentifier(identifierString);
							failedTest.add(methodID);
						}
						failingCount--;
					}
				}
			}
			bReader.close();
		} catch (IOException e) {
			LevelLogger.fatal(__name__ + "#findFailedTestFromFile Read line from file failed.", e);
		} finally {
			if (bReader != null) {
				try {
					bReader.close();
				} catch (IOException e) {
				}
			}
		}
		return failedTest;
	}

	/**
	 * collect all executed methods for given test cases {@code testcases}
	 *
	 * @param subject
	 *            : current subject, e.g., chart_1_buggy
	 * @param testcases
	 *            :a set of method ids of test cases to be collected
	 * @return a set of method ids covered by the given test cases
	 */
	public static Set<Integer> collectCoveredMethod(Subject subject, Set<Integer> testcases) {
		subject.backupSource();
		MethodInstrumentVisitor methodInstrumentVisitor = new MethodInstrumentVisitor();
		String subjectSourcePath = Utils.join(subject.getHome(), subject.getSsrc());
		Instrument.execute(subjectSourcePath, methodInstrumentVisitor);
		TestMethodInstrumentVisitor newTestMethodInstrumentVisitor = new TestMethodInstrumentVisitor(testcases, false);
		String subjectTestPath = Utils.join(subject.getHome(), subject.getTsrc());
		Instrument.execute(subjectTestPath, newTestMethodInstrumentVisitor);
		Set<Integer> allMethods = new HashSet<>();
		for (Integer methodID : testcases) {
			String methodString = Identifier.getMessage(methodID);
			String[] methodInfo = methodString.split("#");
			if (methodInfo.length < 4) {
				LevelLogger.error(__name__ + "#collectCoveredMethod method string format error : " + methodString);
				System.exit(1);
			}
			String clazzpath = methodInfo[0];
			String methodName = methodInfo[2];
			String singleTest = clazzpath + "::" + methodName;
			boolean success = Runner.testSingleCase(subject, singleTest);
			if (!success) {
				LevelLogger
						.error(__name__ + "#collectCoveredMethod build subject failed when running single test case.");
				System.exit(0);
			}
			Set<Integer> coveredMethodes = ExecutionPathBuilder
					.collectAllExecutedMethods(Constant.STR_TMP_INSTR_OUTPUT_FILE);

			if (coveredMethodes != null) {
				allMethods.addAll(coveredMethodes);
			}
		}

		subject.restoreSource();
		return allMethods;
	}
	
}
