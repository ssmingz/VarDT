/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.common.utils;

import org.apache.commons.io.FileUtils;
import pda.common.conf.Constant;
import pda.common.java.Subject;
import pda.core.trace.path.CoverInfo;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author:
 * @date: 2021/11/2
 */
public class Utils {

    public static void pathGuarantee(String... paths) {
        File file = null;
        for (String string : paths) {
            file = new File(string);
            if (!file.exists()) {
                file.mkdirs();
            }
        }
    }

    public static String ensureExist(String path) {
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
        return path;
    }

    public static boolean moveFile(String src, String tar) {
        File file = new File(src);
        if (file.exists()) {
            try {
                FileUtils.moveFile(file, new File(tar));
            } catch (IOException e) {
                LevelLogger.error("Backup previous out file failed! " + src);
                return false;
            }
        }
        return true;
    }

    public static boolean deleteDirs(File dir) {
        boolean result = true;
        if (dir.exists()) {
            try {
                FileUtils.deleteDirectory(dir);
            } catch (IOException e) {
                LevelLogger.error("Delete directory failed!", e);
                result = false;
            }
        }
        return result;
    }

    public static boolean deleteDirs(String... dirs) {
        boolean result = true;
        File file;
        for (String dir : dirs) {
            file = new File(dir);
            if (file.exists()) {
                try {
                    FileUtils.deleteDirectory(file);
                } catch (IOException e) {
                    LevelLogger.error("Delete directory failed!", e);
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean deleteFiles(File f) {
        boolean result = true;
        if (f.exists()) {
            try {
                FileUtils.forceDeleteOnExit(f);
            } catch (IOException e) {
                LevelLogger.error("Delete file failed!", e);
                result = false;
            }
        }
        return result;
    }

    public static boolean deleteFiles(String... files) {
        boolean result = true;
        File file;
        for (String f : files) {
            file = new File(f);
            if (file.exists()) {
                try {
                    FileUtils.forceDelete(file);
                } catch (IOException e) {
                    LevelLogger.error("Delete file failed!", e);
                    result = false;
                }
            }
        }
        return result;
    }

    public static boolean copyDir(File srcFile, File tarFile) {
        try {
            FileUtils.copyDirectory(srcFile, tarFile);
        } catch (IOException e) {
            LevelLogger.error("Copy dir from " + srcFile.getAbsolutePath() +
                    " to " + tarFile.getAbsolutePath() + " " + "failed", e);
            return false;
        }
        return true;
    }

    public static boolean copyFile(File srcFile, File tarFile) {
        try {
            FileUtils.copyFile(srcFile, tarFile);
        } catch (IOException e) {
            LevelLogger.error("Copy file from " + srcFile.getAbsolutePath() +
                    " to " + tarFile.getAbsolutePath() + " " + "failed", e);
            return false;
        }
        return true;
    }

    public static boolean copyDir(String src, String tar) {
        return copyDir(new File(src), new File(tar));
    }

    public static boolean copyFile(String src, String tar) {
        return copyFile(new File(src), new File(tar));
    }

    public static boolean safeCollectionEqual(Set<String> c1, Set<String> c2) {
        if (c1 == c2)
            return true;
        if (c1 == null || c2 == null) {
            return false;
        }
        if (c1.size() == c2.size()) {
            for (String s : c1) {
                if (!c2.contains(s)) {
                    return false;
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public static boolean safeBufferEqual(StringBuffer s1, StringBuffer s2) {
        if (s1 == s2)
            return true;
        if (s1 == null || s2 == null)
            return false;
        return s1.toString().equals(s2.toString());
    }

    public static boolean safeStringEqual(String s1, String s2) {
        if (s1 == s2)
            return true;
        if (s1 == null)
            return false;
        return s1.equals(s2);
    }

    public static String join(String... element) {
        return join(Constant.DIR_SEPARATOR_CHAR, Arrays.asList(element));
    }

    public static String join(char delimiter, String... element) {
        return join(delimiter, Arrays.asList(element));
    }

    public static String join(char delimiter, List<String> elements) {
        return join(delimiter + "", elements);
    }

    public static String join(String delimiter, List<String> elements) {
        StringBuffer buffer = new StringBuffer();
        if (elements.size() > 0) {
            buffer.append(elements.get(0));
        }
        for (int i = 1; i < elements.size(); i++) {
            buffer.append(delimiter);
            buffer.append(elements.get(i));
        }
        return buffer.toString();
    }

    public synchronized static void serialize(Serializable object, String fileName) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(fileName));
        objectOutputStream.writeObject(object);
        objectOutputStream.flush();
        objectOutputStream.close();
    }

    public synchronized static Serializable deserialize(String fileName) throws IOException, ClassNotFoundException {
        File file = new File(fileName);
        ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(file));
        return (Serializable) objectInputStream.readObject();
    }

    public static boolean futureTaskWithin(int timeout, FutureTask futureTask) {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.execute(futureTask);

        boolean success;
        try {
            success = ((Boolean) futureTask.get(timeout, TimeUnit.SECONDS)).booleanValue();
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            success = false;
            LevelLogger.error(e);
            futureTask.cancel(true);
        }

        executorService.shutdownNow();
        return success;
    }

    /**
     * @see List<File> ergodic(File file, List<File> fileList, String
     *      srcFilePostfix)
     */
    public static List<File> ergodic(File file, List<File> fileList) {
        return ergodic(file, fileList, ".java");
    }

    /**
     * iteratively search files with the root as {@code file}
     *
     * @param file
     *                       : root file of type {@code File}
     * @param fileList
     *                       : list to save all the files
     * @param srcFilePostfix
     *                       : postfix of file names
     * @return : a list of all files
     */
    public static List<File> ergodic(File file, List<File> fileList, String srcFilePostfix) {
        if (file == null) {
            LevelLogger.error("#ergodic Illegal input file : null.");
            return fileList;
        }
        File[] files = file.listFiles();
        if (files == null)
            return fileList;
        for (File f : files) {
            if (f.isDirectory()) {
                ergodic(f, fileList, srcFilePostfix);
            } else if (f.getName().endsWith(srcFilePostfix))
                fileList.add(f);
        }
        return fileList;
    }

    public static List<File> ergodic(File file, List<File> fileList, Set<String> ignoreSet, String srcFilePostfix) {
        if (file == null) {
            LevelLogger.error("#ergodic Illegal input file : null.");
            return fileList;
        }
        if (file.isFile()) {
            fileList.add(file);
            return fileList;
        }
        File[] files = file.listFiles();
        if (files == null)
            return fileList;
        for (File f : files) {
            if (ignoreSet.contains(f.getName()))
                continue;
            if (f.isDirectory()) {
                ergodic(f, fileList, ignoreSet, srcFilePostfix);
            } else if (f.getName().endsWith(srcFilePostfix))
                fileList.add(f);
        }
        return fileList;
    }

    /**
     * @see List<String> ergodic(String directory, List<String> fileList, String
     *      srcFilePostfix)
     */
    public static List<String> ergodic(String directory, List<String> fileList) {
        return ergodic(directory, fileList, ".java");
    }

    /**
     * iteratively search the file in the given {@code directory}
     *
     * @param directory
     *                  : root directory
     * @param fileList
     *                  : list of file
     * @return : a list of file
     */
    public static List<String> ergodic(String directory, List<String> fileList, String srcFilePostfix) {
        if (directory == null) {
            LevelLogger.error("#ergodic Illegal input file : null.");
            return fileList;
        }
        File file = new File(directory);
        if (file.isFile()) {
            fileList.add(directory);
            return fileList;
        }
        File[] files = file.listFiles();
        if (files == null)
            return fileList;
        for (File f : files) {
            if (f.isDirectory()) {
                ergodic(f.getAbsolutePath(), fileList, srcFilePostfix);
            } else if (f.getName().endsWith(srcFilePostfix))
                fileList.add(f.getAbsolutePath());
        }
        return fileList;
    }

    private static List<String> ergodicJarFile(File path) {
        return ergodic(path.getAbsolutePath(), new LinkedList<>(), ".jar");
    }

    public static void printCoverage(Map<String, CoverInfo> coverage, String filePath, String name) {
        Utils.pathGuarantee(filePath);
        File file = new File(filePath + "/" + name);
        String header = "line\tfcover\tpcover\tf_observed_cover\tp_observed_cover\n";
        JavaFile.writeStringToFile(file, header, false);
        for (Map.Entry<String, CoverInfo> entry : coverage.entrySet()) {
            StringBuffer stringBuffer = new StringBuffer();
            String key = entry.getKey();
            String[] info = key.split("#");
            String methodString = null;
            try {
                methodString = Identifier.getMessage(Integer.parseInt(info[0]));
            } catch (Exception e) {
                continue;
            }
            stringBuffer.append(methodString);
            String moreInfo = key.substring(info[0].length() + 1);
            stringBuffer.append("#");
            stringBuffer.append(moreInfo);

            stringBuffer.append("\t");
            stringBuffer.append(entry.getValue().getFailedCount());
            stringBuffer.append("\t");
            stringBuffer.append(entry.getValue().getPassedCount());
            stringBuffer.append("\t");
            stringBuffer.append(entry.getValue().getFailedObservedCount());
            stringBuffer.append("\t");
            stringBuffer.append(entry.getValue().getPassedObservedCount());
            stringBuffer.append("\n");
            // view coverage.csv file
            JavaFile.writeStringToFile(file, stringBuffer.toString(), true);
        }
    }

    public static void backupFailedTestsAndCoveredMethod(Subject subject, int totalTestNumber, Set<Integer> failedTest,
            Set<Integer> coveredMethod) {
        LevelLogger.info("Start to backup failed tests & covered statement...");
        StringBuffer buffer = new StringBuffer(totalTestNumber + ":" + failedTest.size());
        for (Integer integer : failedTest) {
            buffer.append("\n" + integer);
        }
        String fileName = Utils.join(subject.getOutBase(), "failedTest.txt");
        JavaFile.writeStringToFile(fileName, buffer.toString());

        buffer = new StringBuffer();
        for (Integer string : coveredMethod) {
            buffer.append(string + "\n");
        }
        fileName = Utils.join(subject.getOutBase(), "coveredMethods.txt");
        JavaFile.writeStringToFile(fileName, buffer.toString().trim());
        LevelLogger.info("Finish to backup failed tests & covered statement...");
    }

    public static boolean recoverFailedTestsAndCoveredMethod(Subject subject, Set<Integer> failedTests,
            Set<Integer> allCoveredMethod) {
        LevelLogger.info("Start to recover failed tests & covered statement...");
        String fileName = Utils.join(subject.getOutBase(), "failedTest.txt");
        List<String> content = JavaFile.readFileToStringList(fileName);
        boolean containIllegal = false;
        int totalNumberOfTests = -1;
        int failedTestsNumber = -1;
        if (content.size() > 0) {
            String numbers = content.get(0);
            try {
                totalNumberOfTests = Integer.parseInt(numbers.split(":")[0]);
                failedTestsNumber = Integer.parseInt(numbers.split(":")[1]);
            } catch (Exception e) {
            }
            for (int index = 1; index < content.size(); index++) {
                Integer id = -1;
                try {
                    id = Integer.parseInt(content.get(index));
                } catch (Exception e) {
                }
                if (!Identifier.containKey(id)) {
                    containIllegal = true;
                    break;
                }
                failedTests.add(id);
            }
        }
        if (containIllegal || failedTests.size() != failedTestsNumber) {
            JavaFile.writeStringToFile(fileName, "");
            return false;
        }

        fileName = Utils.join(subject.getOutBase(), "coveredMethods.txt");
        List<String> coveredMethods = JavaFile.readFileToStringList(fileName);
        containIllegal = false;
        try {
            for (String string : coveredMethods) {
                Integer integer = Integer.parseInt(string);
                if (!Identifier.containKey(integer)) {
                    containIllegal = true;
                    break;
                }
                allCoveredMethod.add(integer);
            }
        } catch (Exception e) {
        }

        if (containIllegal) {
            JavaFile.writeStringToFile(fileName, "");
            return false;
        }
        LevelLogger.info("Finish to  recover failed tests & covered statement!");
        return true;
    }
}
