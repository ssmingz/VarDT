/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.common.utils;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import pda.common.conf.Constant;

import java.io.*;
import java.util.*;

/**
 * @author:
 * @date: 2021/11/2
 */
public class JavaFile {

    // public static Method parseMethodFromString(String s) {
    // ASTNode node = JavaFile.genASTFromSource(s + ";", JavaCore.VERSION_1_7,
    // AST.JLS8, ASTParser.K_CLASS_BODY_DECLARATIONS);
    // if (!(node instanceof TypeDeclaration)) return null;
    // TypeDeclaration td = (TypeDeclaration) node;
    // if (td.getMethods().length < 1) return null;
    // MethodDeclaration declaration = td.getMethods()[0];
    // Type type = declaration.getReturnType2();
    // String retType = type == null ? null : type.toString();
    // String name = declaration.getName().getIdentifier();
    // List<SingleVariableDeclaration> params = declaration.parameters();
    // List<String> args = new ArrayList<>(params.size());
    // for (SingleVariableDeclaration svd : params) {
    // args.add(svd.getType().toString());
    // }
    // return new Method(retType, name, args);
    // }
    //
    // /**
    // * extract the given method node
    // *
    // * @param file : class file
    // * @param method : given method signature
    // * @return node of the corresponds to the given method signature
    // */
    // public static Node getNode(String file, Method method) {
    // if (method == null) return null;
    // CompilationUnit unit = genAST(file);
    // MethodDeclaration declaration = getDeclaration(unit, method);
    // if (declaration == null) {
    // return null;
    // }
    // NodeParser parser = new NodeParser();
    // return parser.setCompilationUnit(file, unit).process(declaration);
    // }
    //
    // public static MethodDeclaration getDeclaration(CompilationUnit unit, Method
    // method) {
    // if (method == null || unit == null) return null;
    // final List<MethodDeclaration> declarations = new ArrayList<>(1);
    // unit.accept(new ASTVisitor() {
    // public boolean visit(MethodDeclaration m) {
    // if (method.same(m)) {
    // declarations.add(m);
    // return false;
    // }
    // return true;
    // }
    // });
    // if (declarations.size() == 0) {
    // return null;
    // }
    // return declarations.get(0);
    // }

    public static Type typeFromBinding(AST ast, ITypeBinding typeBinding) {
        if (typeBinding == null) {
            return ast.newWildcardType();
        }

        if (typeBinding.isPrimitive()) {
            return ast.newPrimitiveType(
                    PrimitiveType.toCode(typeBinding.getName()));
        }

        if (typeBinding.isCapture()) {
            ITypeBinding wildCard = typeBinding.getWildcard();
            WildcardType capType = ast.newWildcardType();
            ITypeBinding bound = wildCard.getBound();
            if (bound != null) {
                capType.setBound(typeFromBinding(ast, bound),
                        wildCard.isUpperbound());
            }
            return capType;
        }

        if (typeBinding.isArray()) {
            Type elType = typeFromBinding(ast, typeBinding.getElementType());
            return ast.newArrayType(elType, typeBinding.getDimensions());
        }

        if (typeBinding.isParameterizedType()) {
            ParameterizedType type = ast.newParameterizedType(
                    typeFromBinding(ast, typeBinding.getErasure()));

            @SuppressWarnings("unchecked")
            List<Type> newTypeArgs = type.typeArguments();
            for (ITypeBinding typeArg : typeBinding.getTypeArguments()) {
                newTypeArgs.add(typeFromBinding(ast, typeArg));
            }

            return type;
        }

        if (typeBinding.isWildcardType()) {
            WildcardType type = ast.newWildcardType();
            if (typeBinding.getBound() != null) {
                type.setBound(typeFromBinding(ast, typeBinding.getBound()));
            }
            return type;
        }

        // if(typeBinding.isGenericType()) {
        // System.out.println(typeBinding.toString());
        // return typeFromBinding(ast, typeBinding.getErasure());
        // }

        // simple or raw type
        String qualName = typeBinding.getQualifiedName();
        if ("".equals(qualName)) {
            return ast.newWildcardType();
        }
        try {
            return ast.newSimpleType(ast.newName(qualName));
        } catch (Exception e) {
            return ast.newWildcardType();
        }
    }

    /**
     * @param icu
     * @return
     * @see #genASTFromICU(ICompilationUnit icu, String jversion, int astLevel)
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu) {
        return genASTFromICU(icu, JavaCore.VERSION_1_7);
    }

    /**
     * @param icu
     * @param jversion
     * @return
     * @see #genASTFromICU(ICompilationUnit icu, String jversion, int astLevel)
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu, String jversion) {
        return genASTFromICU(icu, jversion, AST.JLS8);
    }

    /**
     * @param icu
     * @param astLevel
     * @return
     * @see #genASTFromICU(ICompilationUnit icu, String jversion, int astLevel)
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu, int astLevel) {
        return genASTFromICU(icu, JavaCore.VERSION_1_7, astLevel);
    }

    /**
     * create AST from {@code ICompilationUnit}
     *
     * @param icu
     *                 : ICompilationUnit for creating AST
     * @param jversion
     *                 : the version of JAVA, can be one of the following:
     *                 <ul>
     *                 <li>{@code JavaCore.VERSION_1_1}</li>
     *                 <li>{@code JavaCore.VERSION_1_2}</li>
     *                 <li>{@code JavaCore.VERSION_1_3}</li>
     *                 <li>{@code JavaCore.VERSION_1_4}</li>
     *                 <li>{@code JavaCore.VERSION_1_5}</li>
     *                 <li>{@code JavaCore.VERSION_1_6}</li>
     *                 <li>{@code JavaCore.VERSION_1_7}</li>
     *                 <li>{@code JavaCore.VERSION_1_8}</li>
     *                 </ul>
     * @param astLevel
     *                 : AST level of created AST, can be one of the following:
     *                 <ul>
     *                 <li>{@code AST.JLS2}</li>
     *                 <li>{@code AST.JLS3}</li>
     *                 <li>{@code AST.JLS4}</li>
     *                 <li>{@code AST.JLS8}</li>
     *                 </ul>
     * @return : AST
     */
    public static CompilationUnit genASTFromICU(ICompilationUnit icu, String jversion, int astLevel) {
        ASTParser astParser = ASTParser.newParser(astLevel);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(jversion, options);
        astParser.setCompilerOptions((Map<String, String>) options);
        astParser.setSource(icu);
        astParser.setKind(ASTParser.K_COMPILATION_UNIT);
        astParser.setResolveBindings(true);
        return (CompilationUnit) astParser.createAST(null);
    }

    /**
     * @param fileName
     * @return
     * @see #genASTFromSource(String icu, String jversion, int astLevel, int type)
     */
    public static CompilationUnit genAST(String fileName) {
        return (CompilationUnit) genASTFromSource(readFileToString(fileName), JavaCore.VERSION_1_7, AST.JLS8,
                ASTParser.K_COMPILATION_UNIT);
    }

    /**
     * @param fileName
     * @param jversion
     * @return
     * @see #genASTFromSource(String icu, String jversion, int astLevel, int type)
     */
    public static CompilationUnit genAST(String fileName, String jversion) {
        return (CompilationUnit) genASTFromSource(readFileToString(fileName), jversion, AST.JLS8,
                ASTParser.K_COMPILATION_UNIT);
    }

    /**
     * @param fileName
     * @param astLevel
     * @return
     * @see #genASTFromSource(String icu, String jversion, int astLevel, int type)
     */
    public static CompilationUnit genAST(String fileName, int astLevel) {
        return (CompilationUnit) genASTFromSource(readFileToString(fileName), JavaCore.VERSION_1_7, astLevel,
                ASTParser.K_COMPILATION_UNIT);
    }

    public static ASTNode genASTFromSource(String icu, int type) {
        return genASTFromSource(icu, Constant.JAVA_VERSION, Constant.AST_LEVEL, type);
    }

    /**
     * create AST from source code
     *
     * @param icu
     *                 : source code with {@code String} format
     * @param jversion
     *                 : the version of JAVA, can be one of the following:
     *                 <ul>
     *                 <li>{@code JavaCore.VERSION_1_1}</li>
     *                 <li>{@code JavaCore.VERSION_1_2}</li>
     *                 <li>{@code JavaCore.VERSION_1_3}</li>
     *                 <li>{@code JavaCore.VERSION_1_4}</li>
     *                 <li>{@code JavaCore.VERSION_1_5}</li>
     *                 <li>{@code JavaCore.VERSION_1_6}</li>
     *                 <li>{@code JavaCore.VERSION_1_7}</li>
     *                 <li>{@code JavaCore.VERSION_1_8}</li>
     *                 </ul>
     * @param astLevel
     *                 : AST level of created AST, can be one of the following:
     *                 <ul>
     *                 <li>{@code AST.JLS2}</li>
     *                 <li>{@code AST.JLS3}</li>
     *                 <li>{@code AST.JLS4}</li>
     *                 <li>{@code AST.JLS8}</li>
     *                 </ul>
     * @param type
     *                 : the type of AST node, can be one of the following:
     *                 <ul>
     *                 <li>{@code ASTParser.K_CLASS_BODY_DECLARATIONS}</li>
     *                 <li>{@code ASTParser.K_COMPILATION_UNIT}</li>
     *                 <li>{@code ASTParser.K_EXPRESSION}</li>
     *                 <li>{@code ASTParser.K_STATEMENTS}</li>
     *                 </ul>
     * @return : AST
     */
    public static ASTNode genASTFromSource(String icu, String jversion, int astLevel, int type) {
        ASTParser astParser = ASTParser.newParser(astLevel);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(jversion, options);
        astParser.setCompilerOptions((Map<String, String>) options);
        astParser.setSource(icu.toCharArray());
        astParser.setKind(type);
        astParser.setResolveBindings(true);
        return astParser.createAST(null);
    }

    /**
     * @see ASTNode genASTFromSourceWithType(String icu, int type, String filePath,
     *      String srcPath)
     * @param srcFile
     * @return
     */
    public static CompilationUnit genASTFromFileWithType(String srcFile) {
        return (CompilationUnit) genASTFromSourceWithType(readFileToString(srcFile), ASTParser.K_COMPILATION_UNIT,
                srcFile, null);
    }

    /**
     * @see ASTNode genASTFromSourceWithType(String icu, int type, String filePath,
     *      String srcPath)
     * @param srcFile
     * @return
     */
    public static CompilationUnit genASTFromFileWithType(File srcFile) {
        return (CompilationUnit) genASTFromSourceWithType(readFileToString(srcFile), ASTParser.K_COMPILATION_UNIT,
                srcFile.getAbsolutePath(), null);
    }

    /**
     * create {@code CompilationUnit} from given file {@code srcFile}
     * 
     * @param srcFile : absolute path of java source code
     * @param srcPath : base path of source code
     * @return a compilation unit
     */
    public static CompilationUnit genASTFromFileWithType(String srcFile, String srcPath) {
        return (CompilationUnit) genASTFromSourceWithType(readFileToString(srcFile), ASTParser.K_COMPILATION_UNIT,
                srcFile, srcPath);
    }

    /**
     * @param icu
     * @param type
     * @param filePath
     * @param srcPath
     * @return
     * @see #genASTFromSourceWithType(String icu, String jversion, int astLevel, int
     *      type, String filePath, String srcPath)
     */
    public static ASTNode genASTFromSourceWithType(String icu, int type, String filePath, String srcPath) {
        return genASTFromSourceWithType(icu, JavaCore.VERSION_1_7, AST.JLS8, type, filePath, srcPath);
    }

    /**
     * @param icu
     * @param jversion
     * @param type
     * @param filePath
     * @param srcPath
     * @return
     * @see #genASTFromSourceWithType(String icu, String jversion, int astLevel, int
     *      type, String filePath, String srcPath)
     */
    public static ASTNode genASTFromSourceWithType(String icu, String jversion, int type, String filePath,
            String srcPath) {
        return genASTFromSourceWithType(icu, jversion, AST.JLS8, type, filePath, srcPath);
    }

    /**
     * @param icu
     * @param astLevel
     * @param type
     * @param filePath
     * @param srcPath
     * @return
     * @see #genASTFromSourceWithType(String icu, String jversion, int astLevel, int
     *      type, String filePath, String srcPath)
     */
    public static ASTNode genASTFromSourceWithType(String icu, int astLevel, int type, String filePath,
            String srcPath) {
        return genASTFromSourceWithType(icu, JavaCore.VERSION_1_7, astLevel, type, filePath, srcPath);
    }

    /**
     * @param icu
     *                 : source code with {@code String} format
     * @param jversion
     *                 : the version of JAVA, can be one of the following:
     *                 <ul>
     *                 <li>{@code JavaCore.VERSION_1_1}</li>
     *                 <li>{@code JavaCore.VERSION_1_2}</li>
     *                 <li>{@code JavaCore.VERSION_1_3}</li>
     *                 <li>{@code JavaCore.VERSION_1_4}</li>
     *                 <li>{@code JavaCore.VERSION_1_5}</li>
     *                 <li>{@code JavaCore.VERSION_1_6}</li>
     *                 <li>{@code JavaCore.VERSION_1_7}</li>
     *                 <li>{@code JavaCore.VERSION_1_8}</li>
     *                 </ul>
     * @param astLevel
     *                 : AST level of created AST, can be one of the following:
     *                 <ul>
     *                 <li>{@code AST.JLS2}</li>
     *                 <li>{@code AST.JLS3}</li>
     *                 <li>{@code AST.JLS4}</li>
     *                 <li>{@code AST.JLS8}</li>
     *                 </ul>
     * @param type
     *                 : the type of AST node, can be one of the following:
     *                 <ul>
     *                 <li>{@code ASTParser.K_CLASS_BODY_DECLARATIONS}</li>
     *                 <li>{@code ASTParser.K_COMPILATION_UNIT}</li>
     *                 <li>{@code ASTParser.K_EXPRESSION}</li>
     *                 <li>{@code ASTParser.K_STATEMENTS}</li>
     *                 </ul>
     * @param filePath
     *                 : source file absolute path
     * @param srcPath
     *                 : the base of source file
     * @return AST
     */
    public synchronized static ASTNode genASTFromSourceWithType(String icu, String jversion, int astLevel, int type,
            String filePath, String srcPath) {
        if (icu == null || icu.isEmpty())
            return null;
        ASTParser astParser = ASTParser.newParser(astLevel);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(jversion, options);
        astParser.setCompilerOptions((Map<String, String>) options);
        astParser.setSource(icu.toCharArray());
        astParser.setKind(type);
        astParser.setResolveBindings(true);
        srcPath = srcPath == null ? "" : srcPath;
        astParser.setEnvironment(getClassPath(), new String[] { srcPath }, null, true);
        astParser.setUnitName(filePath);
        astParser.setBindingsRecovery(true);
        try {
            return astParser.createAST(null);
        } catch (Exception e) {
            return null;
        }
    }

    private static String[] getClassPath() {
        String property = System.getProperty("java.class.path", ".");
        File file = new File(Constant.D4J_LIB_DIR);
        String result = "";
        for (File jar : file.listFiles()) {
            result += jar.getAbsolutePath() + ";";
        }
        property += ";" + result;
        return property.split(File.pathSeparator);
    }

    /**
     * write {@code string} into file with mode as "not append"
     *
     * @param filePath
     *                 : path of file
     * @param string
     *                 : message
     * @return
     */
    public static boolean writeStringToFile(String filePath, String string) {
        return writeStringToFile(filePath, string, false);
    }

    /**
     * write {@code string} to file with mode as "not append"
     *
     * @param file
     *               : file of type {@code File}
     * @param string
     *               : message
     * @return
     */
    public static boolean writeStringToFile(File file, String string) {
        return writeStringToFile(file, string, false);
    }

    /**
     * @param filePath
     * @param string
     * @param append
     * @return
     * @see #writeStringToFile(File, String, boolean)
     */
    public static boolean writeStringToFile(String filePath, String string, boolean append) {
        if (filePath == null) {
            LevelLogger.error("#writeStringToFile Illegal file path : null.");
            return false;
        }
        File file = new File(filePath);
        return writeStringToFile(file, string, append);
    }

    /**
     * write {@code string} into file with specific mode
     *
     * @param file
     *               : file of type {@code File}
     * @param string
     *               : message
     * @param append
     *               : writing mode
     * @return
     */
    public static boolean writeStringToFile(File file, String string, boolean append) {
        if (file == null) {
            LevelLogger.error("#writeStringToFile Illegal arguments (File) : null.");
            return false;
        }
        if (string == null) {
            LevelLogger.error("#writeStringToFile Illegal arguments (string) : null.");
            return false;
        }
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                LevelLogger.error("#writeStringToFile Create new file failed : " + file.getAbsolutePath());
                return false;
            }
        }
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            bufferedWriter.write(string);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    public static boolean writeSetToFile(String fileName, Collection<? extends Object> strings, boolean append) {
        if (fileName == null) {
            LevelLogger.error("#writeSetToFile Illegal arguments (File) : null.");
            return false;
        }
        if (strings == null) {
            LevelLogger.error("#writeSetToFile Illegal arguments (string) : null.");
            return false;
        }
        File file = new File(fileName);
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                LevelLogger.error("#writeSetToFile Create new file failed : " + file.getAbsolutePath());
                return false;
            }
        }
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            for (Object string : strings) {
                bufferedWriter.write(string.toString());
                bufferedWriter.newLine();
            }
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return true;
    }

    /**
     * read string from file
     *
     * @param filePath
     *                 : file path
     * @return : string in the file
     */
    public static String readFileToString(String filePath) {
        if (filePath == null) {
            LevelLogger.error("#readFileToString Illegal input file path : null.");
            return new String();
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LevelLogger.error("#readFileToString Illegal input file path : " + filePath);
            return new String();
        }
        return readFileToString(file);
    }

    /**
     * read string from file
     *
     * @param file
     *             : file of type {@code File}
     * @return : string in the file
     */
    public static String readFileToString(File file) {
        if (file == null) {
            LevelLogger.error("#readFileToString Illegal input file : null.");
            return new String();
        }
        StringBuffer stringBuffer = new StringBuffer();
        InputStream in = null;
        InputStreamReader inputStreamReader = null;
        try {
            in = new FileInputStream(file);
            inputStreamReader = new InputStreamReader(in, "UTF-8");
            char[] ch = new char[1024];
            int readCount = 0;
            while ((readCount = inputStreamReader.read(ch)) != -1) {
                stringBuffer.append(ch, 0, readCount);
            }
            inputStreamReader.close();
            in.close();

        } catch (Exception e) {
            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e1) {
                    return new String();
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    return new String();
                }
            }
        }
        return stringBuffer.toString();
    }

    /**
     * read string from file
     *
     * @param filePath
     *                 : file path
     * @return : list of string in the file
     */
    public static List<String> readFileToStringList(String filePath) {
        if (filePath == null) {
            LevelLogger.error("#readFileToStringList Illegal input file path : null.");
            return new ArrayList<String>();
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LevelLogger.error("#readFileToString Illegal input file path : " + filePath);
            return new ArrayList<String>();
        }
        return readFileToStringList(file);
    }

    /**
     * read string from file
     *
     * @param file
     *             : file of type {@code File}
     * @return : list of string in the file
     */
    public static List<String> readFileToStringList(File file) {
        List<String> results = new ArrayList<String>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                results.add(line);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            LevelLogger.error("#readFileToStringList file not found : " + file.getAbsolutePath());
        } catch (IOException e) {
            LevelLogger.error("#readFileToStringList IO exception : " + file.getAbsolutePath(), e);
        }
        return results;
    }

    /**
     * read string from file
     *
     * @param filePath
     *                 : file path
     * @return : set of string in the file
     */
    public static Set<String> readFileToStringSet(String filePath) {
        if (filePath == null) {
            LevelLogger.error("#readFileToStringSet Illegal input file path : null.");
            return new HashSet<>();
        }
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            LevelLogger.error("#readFileToStringSet Illegal input file path : " + filePath);
            return new HashSet<>();
        }
        return readFileToStringSet(file);
    }

    /**
     * read string from file
     *
     * @param file
     *             : file of type {@code File}
     * @return : set of string in the file
     */
    public static Set<String> readFileToStringSet(File file) {
        Set<String> results = new HashSet<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                results.add(line);
            }
            reader.close();
        } catch (FileNotFoundException e) {
            LevelLogger.error("#readFileToStringSet file not found : " + file.getAbsolutePath());
        } catch (IOException e) {
            LevelLogger.error("#readFileToStringSet IO exception : " + file.getAbsolutePath(), e);
        }
        return results;
    }

    public static CompilationUnit genASTFromSource(String source, String fileName, String dirPath) {
        ASTParser parser = ASTParser.newParser(8);
        parser.setResolveBindings(true);
        parser.setSource(source.toCharArray());
        parser.setKind(8);
        String property = System.getProperty("java.class.path", ".");
        String srcPath = getSrcClasses(dirPath);
        parser.setEnvironment(getClassPath(), new String[] { srcPath }, null, true);
        System.out.println(property);
        parser.setUnitName(fileName);
        parser.setBindingsRecovery(true);
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions("1.7", options);
        parser.setCompilerOptions(options);
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        unit.recordModifications();
        return unit;
    }

    public static String getSrcClasses(String projectPath) {
        String fname = projectPath + "/defects4j.build.properties";
        File file = new File(fname);
        String result = "";
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            String tmp = "";
            while ((tmp = bReader.readLine()) != null) {
                if (tmp.startsWith("d4j.dir.src.classes")) {
                    tmp = tmp.trim();
                    result = tmp.substring(tmp.indexOf('=') + 1);
                    break;
                }
            }
            result = projectPath + "/" + result;
            bReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("[ERROR] JavaFile#getSrcClasses File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("[ERROR] JavaFile#getSrcClasses IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }
}
