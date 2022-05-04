/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.trace;

import pda.common.conf.Constant;
import pda.common.java.Subject;
import pda.common.utils.Identifier;
import pda.common.utils.LevelLogger;
import pda.common.utils.Method;
import pda.common.utils.Utils;
import pda.core.trace.inst.Instrument;
import pda.core.trace.inst.visitor.StatementInstrumentVisitor;
import pda.core.trace.inst.visitor.TraceTestMethodInstrumentVisitor;
import pda.core.trace.inst.visitor.TraversalVisitor;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author: 
 * @date: 2021/11/16
 */
public class Tracer {

    private Subject _subject;
    private Set<Integer> _testcases;
    private Set<Integer> _instMethods;

    public Tracer(Subject subject) {
        _subject = subject;
    }

    public void setTestCases(final Set<Integer> testcases) {
        _testcases = testcases;
    }

    public void setInstrumentMethods(final Set<Integer> methods) {
        _instMethods = methods;
    }

    private List<File> buildFiles(String base, Set<Integer> ids) {
        List<File> files = new LinkedList<>();
        Set<String> paths = new HashSet<>();
        for (Integer method : ids) {
            Method m = Method.parse(Identifier.getMessage(method));
            if (m == null) {
                LevelLogger.error("Parse method id error : " + method);
                continue;
            }
            String clazz = m.getClazz();
            if (clazz.contains("$")) {
                clazz = clazz.substring(0, clazz.indexOf("$"));
            }
            String f = Utils.join(base, clazz.replace(".", Constant.DIR_SEPARATOR) + ".java");
            if (paths.contains(f)) {
                continue;
            }
            paths.add(f);
            File file = new File(f);
            if (file.exists()) {
                files.add(file);
            } else {
                LevelLogger.error("File not exist : " + file.getAbsolutePath());
            }
        }
        return files;
    }

    public void trace(String outPath) {
//        _subject.restoreSource();

        List<File> srcFiles = buildFiles(Utils.join(_subject.getHome(), _subject.getSsrc()), _instMethods);
        List<File> testFiles = buildFiles(Utils.join(_subject.getHome(), _subject.getTsrc()), _testcases);

        LevelLogger.debug("Start to instrument source code...");
        TraversalVisitor traversalVisitor = new StatementInstrumentVisitor(_instMethods);
        Instrument.execute(traversalVisitor, srcFiles);
        LevelLogger.debug("Finish to instrument.");

        LevelLogger.debug("Start to instrument test code...");
        TraceTestMethodInstrumentVisitor newTestMethodInstrumentVisitor = new TraceTestMethodInstrumentVisitor(_testcases);
        Instrument.execute(newTestMethodInstrumentVisitor, testFiles);
        LevelLogger.debug("Start to instrument.");

        // delete all bin file to make it re-compiled
        _subject.clearClass();
        Utils.deleteFiles(Constant.STR_TMP_TRACE_OUTPUT_FILE);

        for (Integer methodID : _testcases) {
            Method testcase = Method.parse(Identifier.getMessage(methodID));
            if (testcase == null) continue;
            String singleTest = testcase.getClazz() + "::" + testcase.getName();
            boolean success = Runner.testSingleCase(_subject, singleTest);
            if (!success) {
                LevelLogger.error("#collectCoveredMethod build subject failed when running single test case.");
                System.exit(0);
            }
        }

        File file = new File(Constant.STR_TMP_TRACE_OUTPUT_FILE);
        String target = Utils.join(outPath, file.getName());
        Utils.copyFile(file, new File(target));
        LevelLogger.debug("Save trace file to : " + target);
        _subject.restoreSource();
    }

}
