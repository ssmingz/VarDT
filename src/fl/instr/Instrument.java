package fl.instr;

import fl.Runner;
import fl.instr.gen.GenStatement;
import fl.instr.gen.GenWriter;
import fl.instr.visitor.MethodStmtCountVisitor;
import fl.instr.visitor.SliceVisitor;
import fl.instr.visitor.TestMethodVisitor;
import fl.utils.*;
import org.eclipse.jdt.core.dom.*;

import java.io.*;
import java.util.*;

/**
 * @author
 * @date 2021/7/21
 */
public class Instrument {
    private String _name = "@Instrument ";

    /** refer to the buggy method current working on */
    private String curBuggyMethod = "";
    private String curBuggyMethodWithType = "";
    private static AST _ast = AST.newAST(AST.JLS8);

    private static String modifiedClassPaths = "";

    public static void instrumentSuperClazzTestMethod(File testFile, Set<String> methodNames, String projectPath) {
        // should not backup super test since it may be modified already
        // backup this test class
        //String originalTarget = testFile.getAbsolutePath().substring(0, testFile.getAbsolutePath().indexOf(".java")) + "-original.txt";
        //File originalFile = new File(originalTarget);
        //if(!originalFile.exists()) {
        //    JavaFile.copyFile(originalFile, testFile);
        //} else {
        //    testFile.delete();
        //    JavaFile.copyFile(testFile, originalFile);
        //}

        //if(!modifiedClassPaths.equals("")) {
        //    modifiedClassPaths += "\n";
        //}
        //modifiedClassPaths += originalFile.getAbsolutePath();

        long startTime = System.currentTimeMillis();
        // start instrumentation
        JavaLogger.info("instrument test method of super class at " + testFile.getAbsolutePath());
        String source = JavaFile.readFileToString(testFile);
        CompilationUnit cu = JavaFile.genASTFromSource(source, testFile.getAbsolutePath());
        GenWriter.writerInitForTestMethod(cu);
        // obtain related method declarations by traversing
        SuperClazzTestMethodVisitor visitor = new SuperClazzTestMethodVisitor(cu, methodNames, projectPath);
        Set<MethodDeclaration> methods = visitor.traverse(methodNames);
        // modify target AST
        GenStatement.genForSuperClazzTestMethod(methods);
        GenStatement.genForTestMethod(methods);
        long instruEnd = System.currentTimeMillis();
        float instruTime = (instruEnd - startTime) / 1000F;
        JavaLogger.info("instrumenting time : " + instruTime);
        // write to a new file
        JavaFile.writeStringToFile(cu.toString(), testFile);
        long newWriteEnd = System.currentTimeMillis();
        float newWriteTime = (newWriteEnd - instruEnd) / 1000F;
        JavaLogger.info("writing to new file time : " + newWriteTime);
    }

    /**
     * match slices with projects to complete instrumentation, also transform std.log to values.csv
     */
    public void run() {
        File aProject = new File(Constant.PROJECT_PATH);
        File aSlice = new File(Constant.SLICE_PATH);
        if(!aSlice.exists()) {
            JavaLogger.error(_name + "#run Slice " + Constant.SLICE_PATH + " not found");
            return;
        }
        JavaLogger.info(aProject.getName());

        // get the related line numbers
        this.checkSlice();
    }

    /**
     * instrument by inserting statements to target AST
     * @param sliceResult : extracted line numbers
     */
    public void execute(List<Integer> sliceResult, String targetPath, CompilationUnit cu) {
        JavaLogger.info("Start instrumentation ...");
        // obtain sliced statements by traversing
        SliceVisitor visitor = new SliceVisitor(cu, sliceResult);
        LinkedHashMap<Statement, Integer> stmtList = visitor.traverse();
        // generate printers in target AST
        JavaLogger.info("generate statements ...");
        GenStatement.generate(cu, stmtList, Constant.PROJECT_PATH);
    }

    /**
     * copy InstrAux.java to SRC_PATH/auxiliary/InstrAux.java
     * @param targetPath
     */
    private static void copyAuxClass(String targetPath, PackageDeclaration pdToAdd) {
        String srcPath = JavaFile.getSrcClasses(Constant.PROJECT_PATH);
        String jarPath = Runner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String fromPath = jarPath.substring(0, jarPath.lastIndexOf("/")) + "/res/instrument-aux/InstrAux.java";
        File auxdir = new File(srcPath+"/auxiliary");
        if(!auxdir.exists() || !auxdir.isDirectory()) {
            auxdir.mkdir();
        }
        String toPath = srcPath + "/auxiliary/InstrAux.java";
        File auxClass = new File(fromPath);
        File auxClass_copy = new File(toPath);
        JavaFile.copyFile(auxClass_copy, auxClass);
    }

    /**
     * instrument at related test methods, according to tests.csv
     * @param projectPath
     */
    private void instrumentTestMethods(String projectPath) {
        String all_tests_path = Constant.ALL_TESTS_AFTER_TP + "/" + Constant.PROJECT_ID + "/" + Constant.BUG_ID + ".txt";
        String relevant_tests_path = Constant.D4J_HOME + "/framework/projects/" + Constant.PROJECT_NAME_MAP.get(Constant.PROJECT_ID) + "/relevant_tests/" + Constant.BUG_ID;
        File all_tests = new File(all_tests_path), relevant_tests = new File(relevant_tests_path);
        if(!all_tests.exists()) {
            JavaLogger.error(_name + "#instrumentTestMethods File doesn't exist : " + all_tests.getAbsolutePath());
            return;
        }
        if(!relevant_tests.exists()) {
            JavaLogger.error(_name + "#instrumentTestMethods File doesn't exist : " + relevant_tests.getAbsolutePath());
            return;
        }
        LinkedHashMap<String, Set<String>> targetTests = getAllTestMethodsByClazz(all_tests, relevant_tests);

        // get src.dir
        String srcDir = null;
        String propertiesPath = projectPath + "/defects4j.build.properties";
        File propertiesFile = new File(propertiesPath);
        if(!propertiesFile.exists()) {
            JavaLogger.error(_name + "#instrumentTestMethods File doesn't exist : " + propertiesFile.getAbsolutePath());
            return;
        }
        String propertiesStr = JavaFile.readFileToString(propertiesFile);
        String[] lines = propertiesStr.split("\n");
        for(String line : lines) {
            if(line.startsWith("d4j.dir.src.tests")) {
                line = line.trim();
                srcDir = line.substring(line.indexOf('=')+1);
            }
        }
        if(srcDir == null) {
            JavaLogger.error(_name + "#instrumentTestMethods srcDir not found in defects4j.build.properties : " + propertiesFile.getAbsolutePath());
            return;
        }
        // get each test file
        Iterator itr = targetTests.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry<String, Set<String>> entry = (Map.Entry<String, Set<String>>) itr.next();
            //org.apache.commons.lang3.RandomStringUtilsTest
            String targetPath = entry.getKey();
            if (targetPath.contains("$")) { // handle inner class
                targetPath = targetPath.substring(0, targetPath.indexOf("$"));
            }
            targetPath = targetPath.replaceAll("\\.", "/");
            targetPath += ".java";
            targetPath = projectPath + "/" + srcDir + "/" + targetPath;
            File testFile = new File(targetPath);
            instrumentTestMethod(testFile, entry.getValue(), projectPath);
        }
    }

    private LinkedHashMap<String, Set<String>> getAllTestMethodsByClazz(File all_tests, File relevant_tests) {
        LinkedHashMap<String, Set<String>> result = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(relevant_tests));
            String line;
            while((line = reader.readLine()) != null) {
                result.put(line.trim(), new LinkedHashSet<>());
            }
            BufferedReader reader2 = new BufferedReader(new FileReader(all_tests));
            while((line = reader2.readLine()) != null) {
                //testSerialization(org.jfree.chart.annotations.junit.CategoryLineAnnotationTests)
                String clazz = line.substring(line.indexOf("(")+1, line.indexOf(")"));
                String method = line.substring(0, line.indexOf("("));
                if(result.keySet().contains(clazz)) {
                    Set<String> newset = new LinkedHashSet<>();
                    newset.addAll(result.get(clazz));
                    newset.add(method);
                    result.put(clazz, newset);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * instrument at the given test file
     * @param testFile
     * @param testMethods : methods need to be instrumented
     * @param projectPath
     */
    public static void instrumentTestMethod(File testFile, Set<String> testMethods, String projectPath) {
        // backup this test class
        String originalTarget = testFile.getAbsolutePath().substring(0, testFile.getAbsolutePath().indexOf(".java")) + "-original.txt";
        File originalFile = new File(originalTarget);
        if(!originalFile.exists()) {
            JavaFile.copyFile(originalFile, testFile);
        } else {
            testFile.delete();
            JavaFile.copyFile(testFile, originalFile);
        }

        if(!modifiedClassPaths.equals("")) {
            modifiedClassPaths += "\n";
        }
        modifiedClassPaths += originalFile.getAbsolutePath();

        long startTime = System.currentTimeMillis();
        // start instrumentation
        JavaLogger.info("instrument test method at " + testFile.getAbsolutePath());
        String source = JavaFile.readFileToString(testFile);
        CompilationUnit cu = JavaFile.genASTFromSource(source, testFile.getAbsolutePath());

        // add logger init
        // 1. import
        GenWriter.writerInitForTestMethod(cu);
        // obtain related method declarations by traversing
        TestMethodVisitor visitor = new TestMethodVisitor(cu, testMethods, projectPath);
        HashSet<MethodDeclaration> methods = visitor.traverse();
        // modify target AST
        GenStatement.genForTestMethod(methods);
        long instruEnd = System.currentTimeMillis();
        float instruTime = (instruEnd - startTime) / 1000F;
        JavaLogger.info("instrumenting time : " + instruTime);
        // write to a new file
        JavaFile.writeStringToFile(cu.toString(), testFile);
        long newWriteEnd = System.currentTimeMillis();
        float newWriteTime = (newWriteEnd - instruEnd) / 1000F;
        JavaLogger.info("writing to new file time : " + newWriteTime);
    }

    /**
     * set package name in InstrAux.java
     * @param pd
     * @param targetPath
     */
    private void setPackageName(PackageDeclaration pd, String targetPath) {
        String path = targetPath.substring(0, targetPath.lastIndexOf("/")) + "/InstrAux.java";
        File targetFile = new File(path);
        String source = JavaFile.readFileToString(targetFile);
        ASTParser parser0 = ASTParser.newParser(AST.JLS8);
        parser0.setSource(source.toCharArray());
        CompilationUnit unit0 = (CompilationUnit) parser0.createAST(null);
        unit0.recordModifications();
        unit0.setPackage((PackageDeclaration) ASTNode.copySubtree(unit0.getAST(), pd));
        JavaFile.writeStringToFile(unit0.toString(), targetFile);
    }

    /**
     * copy (1)log4j.jar and (2)instrument-log4j.properties to project folder
     * (3)InstrAux.java to folder containing target class file
     * @param projectPath
     */
    private void copyConfigurators(String projectPath) {
        String jarPath = Runner.class.getProtectionDomain().getCodeSource().getLocation().getPath();

        // check whether lib/ exists
        String lib = projectPath + "/lib";
        File libDir = new File(lib);
        if(!libDir.exists() && !libDir.isDirectory()) {
            libDir.mkdirs();
        }

        String fromPath1 = jarPath.substring(0, jarPath.lastIndexOf("/")) + "/lib/log4j-1.2.17.jar";
        String fromPath2 = jarPath.substring(0, jarPath.lastIndexOf("/")) + "/res/conf/instrument-log4j.properties";
        String toPath1 = projectPath + "/lib/log4j-1.2.17.jar";
        String toPath2 = JavaFile.getSrcClasses(projectPath) + "/instrument-log4j.properties";
        // (1)log4j.jar
        File jar = new File(fromPath1);
        File jar_copy = new File(toPath1);
        JavaFile.copyFile(jar_copy, jar);
        // (2)instrument-log4j.properties
        File conf = new File(fromPath2);
        File conf_copy = new File(toPath2);
        JavaFile.copyFile(conf_copy, conf);
    }

    /**
     * extract the line numbers related to the buggy method
     * sliceDir : path of slice file, e.g. 1
     * projectPath : path of project folder, e.g. Lang_1_buggy
     * topN : number of selected methods in ochiai.ranking.csv
     * targetMethodInfo : info of target method to be analyzed
     * @return : key - curBuggyMethodWithType, value - lines
     */
    public void checkSlice() {
        JavaLogger.info("Start slice checking ...");

        // { clazz : { method : [lines] } }
        Map<String, Map<String, List<Integer>>> lines_by_method_by_clazz = new LinkedHashMap<>();
        List<String> method_ids = new ArrayList<>();
        String writebuffer = "";
        try {
            FileReader reader = new FileReader(Constant.SLICE_PATH);
            BufferedReader bReader = new BufferedReader(reader);
            String l = "";
            while((l = bReader.readLine()) != null) {
                String[] parts = l.trim().split(":");
                if(parts.length != 3) {
                    JavaLogger.error("Invalid format line : " + l);
                    continue;
                }
                double pos = Double.valueOf(parts[0]).doubleValue();
                if(pos > Constant.TOP_N) {
                    break;
                }
                // check belonged file
                String filename = parts[1].split("#")[0];
                if(filename.contains("$")) {
                    filename = filename.substring(0, filename.indexOf("$"));
                }
                String method = parts[1];
                BuggyMethodInfo bmi = getBuggyMethodInfo(method);
                if(bmi == null) {
                    continue;
                }
                this.curBuggyMethodWithType = bmi._nameWithTypes;
                this.curBuggyMethod = curBuggyMethodWithType.substring(0, curBuggyMethodWithType.indexOf("("));
                List<Integer> lines = new ArrayList<>();
                if(Constant.SLICER_SWITCH.equals("PDAtrace")) {
                    lines = JavaFile.extractLineNumberFromTraceForPDA(Constant.SLICE_PATH, this.curBuggyMethodWithType);
                } else if(Constant.SLICER_SWITCH.equals("PDAslice")) {
                    // TODO: parsing slice result by PDA
                } else {
                    throw new IllegalArgumentException("Wrong slicer switch settings.");
                }
                Map<String, List<Integer>> tmp = new LinkedHashMap<>();
                if(lines_by_method_by_clazz.containsKey(filename)) {
                    tmp.putAll(lines_by_method_by_clazz.get(filename));
                }
                tmp.put(method, lines);
                lines_by_method_by_clazz.put(filename, tmp);
                method_ids.add(method);
                writebuffer += String.format("%d:%s\n", method_ids.indexOf(method), method);
            }
            bReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // write method_ids to file
        File method_ids_map_file = new File(Constant.PROJECT_PATH + "/instrumented_method_id.txt");
        if(method_ids_map_file.exists()) {
            method_ids_map_file.delete();
        }
        JavaFile.writeStringToFile(writebuffer, method_ids_map_file);
        JavaLogger.info("write method_id to : " + method_ids_map_file.getAbsolutePath());

        // copy log4j.jar and log4j.properties to project folder
        JavaLogger.info("copying configuration files ...");
        copyConfigurators(Constant.PROJECT_PATH);
        // check whether logs folder exists and delete std.log
        JavaLogger.info("check folder /logs ...");
        String dirPath = Constant.PROJECT_PATH + "/logs";
        File logFolder = new File(dirPath);
        if(!logFolder.exists() && !logFolder.isDirectory()) {
            logFolder.mkdir();
        }
        JavaLogger.info("check file std.log ...");
        String fpath = Constant.PROJECT_PATH + "/logs/std.log";
        File logFile = new File(fpath);
        if(logFile.exists()) {
            logFile.delete();
        }

        // instrument by file
        for(String clazz : lines_by_method_by_clazz.keySet()) {
            Constant.CURRENT_CLAZZ = clazz;
            // get src.dir
            String srcDir = null;
            String propertiesPath = Constant.PROJECT_PATH + "/" + "defects4j.build.properties";
            File propertiesFile = new File(propertiesPath);
            if(!propertiesFile.exists()) {
                JavaLogger.error(_name + "#checkSlice File doesn't exist : " + propertiesFile.getAbsolutePath());
                return;
            }
            String propertiesStr = JavaFile.readFileToString(propertiesFile);
            String[] lines = propertiesStr.split("\n");
            for(String line : lines) {
                if(line.startsWith("d4j.dir.src.classes")) {
                    line = line.trim();
                    srcDir = line.substring(line.indexOf('=')+1);
                }
            }
            if(srcDir == null) {
                JavaLogger.error(_name + "#checkSlice srcDir not found in defects4j.build.properties : " + propertiesFile.getAbsolutePath());
                return;
            }

            // check original target .java file
            String targetPath = Constant.PROJECT_PATH + "/" + srcDir + "/" + clazz.replaceAll("\\.", "/") +".java";
            File targetFile = new File(targetPath);
            JavaLogger.info("check original target file ...");
            String originalTarget0 = targetFile.getAbsolutePath().substring(0, targetPath.indexOf(".java")) + "-original.txt";
            File originalFile = new File(originalTarget0);
            if(!originalFile.exists()) {
                JavaFile.copyFile(originalFile, targetFile);
            } else {
                targetFile.delete();
                JavaFile.copyFile(targetFile, originalFile);
            }

            // get target ast
            JavaLogger.info("parsing AST ...");
            String source = JavaFile.readFileToString(targetFile);
            CompilationUnit cu = pda.common.utils.JavaFile.genASTFromSource(source, targetPath, Constant.PROJECT_PATH);

            // add import
            boolean cuAddImportInstrAux = false;
            if(Constant.PACKAGE_OF_INSTRAUX == null) {
                copyAuxClass(targetPath, cu.getPackage());
                Constant.PACKAGE_OF_INSTRAUX = cu.getPackage().getName().getFullyQualifiedName() + ".InstrAux";
            } else {
                cuAddImportInstrAux = true;
            }
            if(cuAddImportInstrAux) {
                ImportDeclaration imp = _ast.newImportDeclaration();
                imp.setName(_ast.newName(Constant.PACKAGE_OF_INSTRAUX));
                cu.imports().add(ASTNode.copySubtree(cu.getAST(), imp));
            }

            // traverse each lines list
            for(String method : lines_by_method_by_clazz.get(clazz).keySet()) {
                Constant.CURRENT_METHOD = method;
                Constant.CURRENT_METHOD_ID = method_ids.indexOf(method);
                BuggyMethodInfo bmi = getBuggyMethodInfo(method);
                if(bmi == null) {
                    continue;
                }
                this.curBuggyMethodWithType = bmi._nameWithTypes;
                this.curBuggyMethod = curBuggyMethodWithType.substring(0, curBuggyMethodWithType.indexOf("("));

                // get start and end line number of target method
                MethodStmtCountVisitor mStmtCounter = new MethodStmtCountVisitor(cu);
                mStmtCounter.traverse(this.curBuggyMethodWithType);
                List<Integer> result = lines_by_method_by_clazz.get(clazz).get(method);
                if(result.size() == 0) {
                    JavaLogger.error(_name + "#checkSlice Empty slice for " + method);
                    continue;
                }

                // insert statements
                execute(result, targetPath, cu);
                JavaLogger.info("Finish method instrumentation at " + method);
            }
            // write to a new file
            JavaLogger.info("write to a new file ...");
            JavaFile.writeStringToFile(cu.toString(), targetFile);
            JavaLogger.info("Finish class instrumentation at " + targetPath);

            // reset some global vars
            Constant.PACKAGE_OF_INSTRAUX = null;
        }
        // instrument at test methods
        JavaLogger.info("instrument at test methods ...");
        instrumentTestMethods(Constant.PROJECT_PATH);

        // modify build.xml
        JavaFile.modifyBuildXML(Constant.PROJECT_PATH);
        // modify build.gradle for mockito
        if (Constant.PROJECT_ID.equals("mockito")) {
            File tmp = new File(Constant.PROJECT_PATH + "/HOWTO.BUILD.TXT");
            if(tmp.exists()) {
                String c = JavaFile.readFileToString(tmp);
                if(c.startsWith("> gradlew build")) {
                    JavaFile.modifyBuildGradle(Constant.PROJECT_PATH);
                    JavaFile.modifyCoreBnd(Constant.PROJECT_PATH);
                }
            }
        }
    }

    /*
      @param targetMethodInfo : format like ochiai.ranking.csv
      e.g. org.apache.commons.lang3.time$FastDateParser#parse(java.lang.String)
      or like traceLineByTopN.txt
      e.g. org.joda.time.DateTimeZone#int#getOffsetFromLocal#?,long
     */
    private BuggyMethodInfo getBuggyMethodInfo(String targetMethodInfo) {
        BuggyMethodInfo result;
        if(!targetMethodInfo.contains("?")) {
            JavaLogger.error(_name + "#getBuggyMethodInfo Illegal format : " + targetMethodInfo);
            return null;
        }
        String[] list = targetMethodInfo.split("#");
        if(list.length != 4) {
            JavaLogger.error(_name + "#getBuggyMethodInfo Illegal format : " + targetMethodInfo);
            return null;
        }
        String clazz = list[0];
        String method = list[2];
        String paras = list[3];
        if(paras.startsWith("?,")) {
            paras = paras.substring(2);
        } else {
            paras = "";
        }
        targetMethodInfo = clazz + "#" + method + "(" + paras + ")";
        String methodName = targetMethodInfo.substring(targetMethodInfo.lastIndexOf("#")+1, targetMethodInfo.indexOf("("));
        String tmp = targetMethodInfo.replace("#", ".");

        // use ase-19 result
        String tmp00 = tmp.substring(0, tmp.lastIndexOf("."));
        String className = tmp00.substring(tmp00.lastIndexOf(".")+1);

        targetMethodInfo = targetMethodInfo.replace("$",".");
        targetMethodInfo = targetMethodInfo.replace("#", ".");
        // org.apache.commons.lang3.math.NumberUtils.createNumber
        String fullName = targetMethodInfo.substring(0, targetMethodInfo.indexOf("("));
        JavaLogger.info("buggy method name: " + fullName);
        // org.apache.commons.lang3.math.NumberUtils.createNumber(java.lang.String)
        String nameWithTypes = targetMethodInfo;
        if(methodName.equals(className) && !Constant.SLICER_SWITCH.equals("PDAtrace")) {
            // since constructors are named as <init> in slices
            nameWithTypes = nameWithTypes.replace(methodName+"(", "<init>(");
        }
        result = new BuggyMethodInfo(methodName, className, targetMethodInfo, nameWithTypes);
        return result;
    }

    private static class SuperClazzTestMethodVisitor extends ASTVisitor {
        private final String _name = "@SuperClazzTestMethodVisitor ";

        protected CompilationUnit _cu;
        protected Set<String> _methodNames;
        protected HashSet<MethodDeclaration> _methodList = new HashSet<>();
        private static AST _ast = AST.newAST(AST.JLS8);
        protected String _projectPath;


        public SuperClazzTestMethodVisitor(CompilationUnit cu, Set<String> methodNames, String projectPath) {
            _cu = cu;
            _methodNames = methodNames;
            _projectPath = projectPath;
        }

        /**
         * traverse compilation unit by ASTVisitor
         * @return
         */
        public Set<MethodDeclaration> traverse(Set<String> methodNames) {
            _cu.accept(this);
            methodNames = new HashSet<>(_methodNames);
            return _methodList;
        }

        @Override
        public boolean visit(MethodDeclaration node){
            return checkMethodName(node);
        }

        @Override
        public boolean visit(TypeDeclaration node) {
            /**
             * isMemberTypeDeclaration() is a convenience method that simply checks whether
             * this node's parent is a type declaration node or an anonymous class declaration
             */
            if(!node.isInterface() && !node.isMemberTypeDeclaration() && !node.isLocalTypeDeclaration()) {
                TestMethodVisitor.insertLoggerInit(node, _projectPath);
            }
            return true;
        }

        private boolean checkMethodName(MethodDeclaration node) {
            String name = node.getName().getIdentifier();
            if(_methodNames.contains(name)) {
                _methodList.add(node);
                _methodNames.remove(name);
            }
            return true;
        }

    }
}
