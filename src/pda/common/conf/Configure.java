/**
 * Copyright (C) SEI, PKU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by Jiajun Jiang<jiajun.jiang@pku.edu.cn>.
 */

package pda.common.conf;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;
import pda.common.java.JCompiler;
import pda.common.java.Subject;
import pda.common.utils.JavaFile;
import pda.common.utils.LevelLogger;
import pda.common.utils.Utils;

import java.io.File;
import java.io.IOException;

public class Configure {

	private final static String __name__ = "@Configure ";
	private static String auxiliaryString;

	public static boolean compileAuxiliaryJava(Subject subject) {
		JCompiler compiler = JCompiler.getInstance();
		File file = new File(Utils.join(subject.getHome(), subject.getSbin()));
		if(!file.exists()) {
			file.mkdirs();
		}
		return compiler.compile(subject, "auxiliary/Dumper.java", auxiliaryString);
	}

	
	public static void config_astlevel(Subject subject) {
		if(subject.getName().equals("lang") && subject.getId() >= 42) {
			Constant.AST_LEVEL = AST.JLS3;
			Constant.JAVA_VERSION = JavaCore.VERSION_1_4;
		} else {
			Constant.AST_LEVEL = AST.JLS8;
			Constant.JAVA_VERSION = JavaCore.VERSION_1_7;
		}
	}

	/**
	 * copy the auxiliary file into the subject source path to make the
	 * instrument running correctly
	 * 
	 * @param subject
	 *            : current subject
	 */
	public static void config_dumper(Subject subject) {
		File file = new File(Constant.HOME + "/resources/auxiliary/Dumper.java");
		if (!file.exists()) {
			LevelLogger.error("File : " + file.getAbsolutePath() + " not exist.");
			System.exit(0);
		}
		CompilationUnit cu = (CompilationUnit) JavaFile.genASTFromSource(JavaFile.readFileToString(file),
				ASTParser.K_COMPILATION_UNIT);
		cu.accept(new ConfigDumperVisitor());
		String formatSource = null;
		Formatter formatter = new Formatter();
		try {
			formatSource = formatter.formatSource(cu.toString());
		} catch (FormatterException e) {
			System.err.println(__name__ + "#execute Format Code Error for : " + file.getAbsolutePath());
			formatSource = cu.toString();
		}

		String path = Utils.join(subject.getHome(), subject.getSsrc());
		String target = path + Constant.DIR_SEPARATOR + "auxiliary/Dumper.java";
		File targetFile = new File(target);
		if (!targetFile.exists()) {
			targetFile.getParentFile().mkdirs();
			try {
				targetFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		auxiliaryString = formatSource;
		JavaFile.writeStringToFile(targetFile, formatSource);
		// fix bug for not compiling
		compileAuxiliaryJava(subject);
	}
}

/**
 * configure some properties in the dumper file
 * 
 * @author Jiajun
 *
 */
class ConfigDumperVisitor extends ASTVisitor {
	@Override
	public boolean visit(FieldDeclaration node) {
		for (Object object : node.fragments()) {
			if (object instanceof VariableDeclarationFragment) {
				VariableDeclarationFragment vdf = (VariableDeclarationFragment) object;
				String name = vdf.getName().getFullyQualifiedName();
				// configure the dependence (library) path and output path
				if (name.equals("OUT_AND_LIB_PATH")) {
					StringLiteral stringLiteral = node.getAST().newStringLiteral();
					stringLiteral.setLiteralValue(Constant.DUMPER_HOME);
					vdf.setInitializer(stringLiteral);
				}
			}
		}
		return true;
	}
}
