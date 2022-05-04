/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.common.java;

import pda.common.conf.Constant;
import pda.common.utils.JavaFile;
import pda.common.utils.LevelLogger;
import pda.common.utils.Utils;

import java.util.LinkedList;
import java.util.List;

/**
 * @author: 
 * @date: 2021/11/2
 */
public class D4jSubject extends Subject {

    private List<String> _failedTestCases;

    public D4jSubject(String base, String name, int id) {
        this(base, name, id, false);
    }

    public D4jSubject(String base, String name, int id, boolean memCompile) {
        super(Utils.join(base, name, name + "_" + id + "_buggy"), name);
        _id = id;
        setClasspath(obtainClasspath(name));
        setSourceLevel(name.equals("chart") ? SOURCE_LEVEL.L_1_4 : SOURCE_LEVEL.L_1_7);
        setPath(name, id);
    }

    private void setPath(String projName, int id) {
        String file = Utils.join(Constant.D4J_SRC_INFO, projName, id + ".txt");
        List<String> paths = JavaFile.readFileToStringList(file);
        if(paths == null || paths.size() < 4) {
            LevelLogger.error(String.format("D4jSubject#setPath : path info error : <{0}>", file));
            return;
        }
        _ssrc = paths.get(0);
        _sbin = paths.get(1);
        _tsrc = paths.get(2);
        _tbin = paths.get(3);
    }

    @Override
    public String toString() {
        return "[_name=" + _name + ", " + ", _id=" + _id + ", _ssrc=" + _ssrc
                + ", _tsrc=" + _tsrc + ", _sbin=" + _sbin
                + ", _tbin=" + _tbin + "]";
    }

    private static List<String> obtainClasspath(final String projName) {
        List<String> classpath = new LinkedList<String>();
        switch (projName) {
            case "math":
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                break;
            case "chart":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/iText-2.1.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/servlet.jar");
                break;
            case "lang":
                classpath.add(Constant.D4J_LIB_DIR + "/cglib-nodep-2.2.2.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/commons-io-2.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/easymock-3.1.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/objenesis-1.2.jar");
                break;
            case "closure":
                classpath.add(Constant.D4J_LIB_DIR + "/caja-r4314.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/jarjar.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/ant.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/ant-launcher.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/args4j.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/jsr305.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/guava.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/json.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/protobuf-java.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/rhino.jar");
                break;
            case "time":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/joda-convert-1.2.jar");
                break;
            case "mockito":
                classpath.add(Constant.D4J_LIB_DIR + "/junit-4.11.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/asm-all-5.0.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/assertj-core-2.1.0.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/cglib-and-asm-1.0.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/cobertura-2.0.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/fest-assert-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/fest-util-1.1.4.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-all-1.3.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/hamcrest-core-1.1.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/objenesis-2.1.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/objenesis-2.2.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/powermock-reflect-1.2.5.jar");
                classpath.add(Constant.D4J_LIB_DIR + "/.jar");
                break;
            default:
                LevelLogger.warn("UNKNOWN project name : " + projName);
        }
        return classpath;
    }
}
