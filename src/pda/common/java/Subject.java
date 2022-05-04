/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.common.java;

import pda.common.conf.Constant;
import pda.common.utils.LevelLogger;
import pda.common.utils.Utils;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: 
 * @date: 2021/11/2
 */
public class Subject {

    protected String _base;
    protected String _name;
    protected String _ssrc;
    protected String _tsrc;
    protected String _sbin;
    protected String _tbin;
    protected int _id = 1;

    protected boolean _compile_file = false;
    protected boolean _compileProject = false;
    protected boolean _test_subject = false;
    // for compile
    protected SOURCE_LEVEL _src_level;
    protected List<String> _classpath;
    // for compile the complete subject
    protected String _compile_command;
    protected String _test_command;
    protected String _key_compile_suc;
    protected String _key_test_suc;
    protected String _jdk_home;

    protected Subject(String base, String name) {
        this(base, name, null, null, null, null);
    }
    /**
     * subject
     *
     * @param base : the base directory of subject, e.g., "/home/ubuntu/code/chart"
     * @param name : name of subject, e.g., "chart".
     * @param ssrc : relative path for source folder, e.g., "source"
     * @param tsrc : relative path for test folder, e.g., "tests"
     * @param sbin : relative path for source byte code, e.g., "classes"
     * @param tbin : relative path for test byte code, e.g., "test-classes"
     */
    public Subject(String base, String name, String ssrc, String tsrc, String sbin, String tbin) {
        this(base, name, ssrc, tsrc, sbin, tbin, SOURCE_LEVEL.L_1_6, new LinkedList<>());
    }

    private Subject(String base, String name, String ssrc, String tsrc, String sbin, String tbin,
                  SOURCE_LEVEL sourceLevel, List<String> classpath) {
        _base = base + "/";
        _name = name;
        _ssrc = ssrc;
        _tsrc = tsrc;
        _sbin = sbin;
        _tbin = tbin;
        _src_level = sourceLevel;
        _classpath = classpath;
    }

    public String getHome() {
        return _base;
    }

    public String getName() {
        return _name;
    }

    public int getId() {
        return _id;
    }

    public String getSsrc() {
        return _ssrc;
    }

    public String getTsrc() {
        return _tsrc;
    }

    public String getSbin() {
        return _sbin;
    }

    public String getTbin() {
        return _tbin;
    }

    public void setSourceLevel(String sourceLeve) {
        setSourceLevel(SOURCE_LEVEL.toSourceLevel(sourceLeve));
    }

    public void setSourceLevel(SOURCE_LEVEL sourceLevel) {
        _src_level = sourceLevel;
    }

    public String getSourceLevelStr() {
        return _src_level.toString();
    }

    public void setClasspath(List<String> classpath) {
        _classpath = classpath;
    }

    public List<String> getClasspath() {
        return _classpath;
    }

    public String getOutBase() {
        return Utils.join(Constant.STR_INFO_OUT_PATH, getName(), getName() + "_" + getId());
    }

    public void backupSource() {
        LevelLogger.debug("Start to backup source code...");
        String src = Utils.join(getHome(), getSsrc());
        String ori = Utils.join(getHome(), getSsrc() + "_ori");
        if (!new File(ori).exists()) {
            Utils.copyDir(src, ori);
        }
        src = Utils.join(getHome(), getTsrc());
        ori = Utils.join(getHome(), getTsrc() + "_ori");
        if (!new File(ori).exists()) {
            Utils.copyDir(src, ori);
        }
        LevelLogger.debug("Finish to backup source code.");
    }

    public void restoreSource() {
        LevelLogger.debug("Start to restore source code...");
        Utils.copyDir(Utils.join(getHome(), getSsrc() + "_ori"), Utils.join(getHome(), getSsrc()));
        Utils.copyDir(Utils.join(getHome(), getTsrc() + "_ori"), Utils.join(getHome(), getTsrc()));
        LevelLogger.debug("Finsih to restore source code.");
    }

    public void clearClass() {
        LevelLogger.debug("Start to clear classes...");
        Utils.deleteDirs(Utils.join(getHome(), getTbin()));
        Utils.deleteDirs(Utils.join(getHome(), getSbin()));
        LevelLogger.debug("Finish to clear classes.");
    }

    public boolean checkAndInitDir() {
        File file = new File(Utils.join(getHome(), getSbin()));
        if(!file.exists()) {
            file.mkdirs();
        }
        file = new File(Utils.join(getHome(), getTbin()));
        if(!file.exists()) {
            file.mkdirs();
        }
        return true;
    }

    @Override
    public String toString() {
        return "[_name=" + _name + ", _ssrc=" + _ssrc + ", _tsrc=" + _tsrc + ", _sbin=" + _sbin
                + ", _tbin=" + _tbin + "]";
    }

    public enum SOURCE_LEVEL {
        L_1_4("1.4"),
        L_1_5("1.5"),
        L_1_6("1.6"),
        L_1_7("1.7"),
        L_1_8("1.8");

        private String value;

        public static SOURCE_LEVEL toSourceLevel(String string) {
            if (string == null) return L_1_7;
            switch (string) {
                case "1.4": return L_1_4;
                case "1.5": return L_1_5;
                case "1.6": return L_1_5;
                case "1.7": return L_1_5;
                case "1.8": return L_1_8;
            }
            return L_1_7;
        }

        SOURCE_LEVEL(String val) {
            value = val;
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
