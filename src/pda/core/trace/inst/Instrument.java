/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.trace.inst;

import org.eclipse.jdt.core.dom.CompilationUnit;
import pda.common.utils.JavaFile;
import pda.common.utils.LevelLogger;
import pda.common.utils.Utils;
import pda.core.trace.inst.visitor.TraversalVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jiajun
 *
 */
public class Instrument {

	private final static String __name__ = "@Instrument ";

	public static boolean execute(String path, TraversalVisitor traversalVisitor) {
		if (path == null || path.length() <= 1) {
			LevelLogger.error(__name__ + "#execute illegal input file : " + path);
			return false;
		}
		File file = new File(path);
		if (!file.exists()) {
			LevelLogger.error(__name__ + "#execute input file not exist : " + path);
			return false;
		}
		List<File> fileList = new ArrayList<>();
		if (file.isDirectory()) {
			fileList = Utils.ergodic(file, fileList);
		} else if (file.isFile()) {
			fileList.add(file);
		} else {
			LevelLogger.error(
					__name__ + "#execute input file is neither a file nor directory : " + file.getAbsolutePath());
			return false;
		}
		return execute(traversalVisitor, fileList);
	}

	public static boolean execute(TraversalVisitor traversalVisitor, List<File> fileList) {
		for (File f : fileList) {
			CompilationUnit unit = JavaFile.genAST(f.getAbsolutePath());
			if (unit == null || unit.toString().trim().length() < 1) {
				continue;
			}
			traversalVisitor.traverse(unit);
			String formatSource = null;
			// Formatter formatter = new Formatter();
			// try {
			// formatSource = formatter.formatSource(unit.toString());
			// } catch (FormatterException e) {
			// LevelLogger.error(__name__ + "#execute Format Code Error for : "
			// + f.getAbsolutePath());
			formatSource = unit.toString();
			// }
			JavaFile.writeStringToFile(f, formatSource);
		}
		return true;
	}
}
