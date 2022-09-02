/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze;

import org.eclipse.jdt.core.dom.Modifier;
import pda.core.analyze.struct.BasicBlock;
import pda.core.analyze.struct.StmtType;
import pda.core.analyze.struct.Use;
import pda.core.analyze.struct.Variable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This class is an interface of code analysis, which provide several useful
 * functions
 * 
 * @author
 *
 */
public class CodeAnalyzer {

	/**
	 * obtain all variables used in the given code line, including variables with
	 * use type {@code USETYPE.READ} and {@code USETYPE.WRITE}.
	 * 
	 * @param base
	 *                    : base path of source files
	 * @param relJavaFile
	 *                    : relative path of java file to parse
	 * @param line
	 *                    : line number to parse
	 * @return a set of variable names
	 */
	public static Set<String> getAllVariablesUsed(String base, String relJavaFile, int line) {
		BasicBlock basicBlock = Analyzer.analyze(relJavaFile, base);
		Set<Use> uses = basicBlock.getVariableUses(line);
		Set<String> variabls = new HashSet<>();
		for (Use use : uses) {
			if (use.getUseType() == Use.USETYPE.DEFINE) {
				continue;
			}
			variabls.add(use.getVariableDefine().getName());
		}
		return variabls;
	}

	/**
	 * obtain all variables that can be used in the given code line with type
	 * information, all variables (local variables, arguments, and fields with
	 * initializers) will be included.
	 * 
	 * @param base
	 *                    : base path of source files
	 * @param relJavaFile
	 *                    : relative path of java file to parse
	 * @param line
	 *                    : line number to parse
	 * @return a map contains all variables that can be used in the code line with
	 *         type info.
	 */
	public static Map<String, String> getAllVariablesAvailableWithType(String base, String relJavaFile, int line) {
		BasicBlock basicBlock = Analyzer.analyze(relJavaFile, base);
		Set<Variable> variables = basicBlock.getAllValidVariables(line);
		Map<String, String> varMap = new HashMap<>();
		for (Variable v : variables) {
			if (v.isField() && v.getInitializer() == null) {
				continue;
			}
			varMap.put(v.getName(), v.getType().toString());
		}
		return varMap;
	}

	/**
	 * obtain variables that can be used in the given code line, exclude field
	 * variables. ONLY include arguments and local defined variables.
	 * 
	 * @param base
	 *                    : base path of source files
	 * @param relJavaFile
	 *                    : relative path of java file to parse
	 * @param line
	 *                    : line number to parse
	 * @return a map contains all illegal variables can be used in the given code
	 *         line
	 */
	public static Map<String, String> getAllLocalVariablesAvailableWithType(String base, String relJavaFile, int line,
			boolean mustInitialized) {
		BasicBlock basicBlock = Analyzer.analyze(relJavaFile, base);
		Set<Variable> variables = basicBlock.getAllValidVariables(line);
		Map<String, String> varMap = new HashMap<>();
		for (Variable v : variables) {
			if (v.isField()) {
				continue;
			}
			if (mustInitialized && !v.isArgument()) {
				if (Modifier.isFinal(v.getModifiers())) {
					continue;
				}
				BasicBlock minimal = basicBlock.getMinimalBasicBlock(line);
				int level = minimal.getBlockLevel();
				boolean initialized = false;
				for (Use use : v.getUseSet()) {
					int lel = use.getParentBlock().getBlockLevel();
					int lin = use.getLineNumber();
					// argument of enhanced for statement considered initialized
					if (use.getStmtType() == StmtType.ENHANCEDFOR) {
						initialized = true;
						break;
					} else if (lin < line && lel <= level) {
						// find a legal write operation
						// or defined with initializer
						if (use.getUseType() == Use.USETYPE.WRITE || (use.getUseType() == Use.USETYPE.DEFINE
								&& use.getVariableDefine().getInitializer() != null)) {
							initialized = true;
							break;
						}
					}
				}
				if (!initialized) {
					continue;
				}
			}
			varMap.put(v.getName(), v.getType().toString());
		}
		return varMap;
	}

	/**
	 * obtain a set of variables in the given code line, which are
	 * {@code USETYPE.READ} use.
	 * 
	 * @param base
	 *                    : base path of source files
	 * @param relJavaFile
	 *                    : relative path of java file to parse
	 * @param line
	 *                    : line number of source code to parse
	 * @return a set of variable names
	 */
	public static Set<String> getAllVariablesReadUse(String base, String relJavaFile, int line) {
		BasicBlock basicBlock = Analyzer.analyze(relJavaFile, base);
		Set<Use> uses = basicBlock.getVariableUses(line);
		Set<String> variabls = new HashSet<>();
		for (Use use : uses) {
			if (use.getUseType() == Use.USETYPE.READ) {
				variabls.add(use.getVariableDefine().getName());
			}
		}
		return variabls;
	}

	/**
	 * obtain a set of variables in the given code line, which are
	 * {@code USETYPE.WRITE} use.
	 * 
	 * @param base
	 *                    : base path of source files.
	 * @param relJavaFile
	 *                    : relative path of java file to parse
	 * @param line
	 *                    : line number of source code to parse
	 * @return a set of variable names
	 */
	public static Set<String> getAllVariablesWriteUse(String base, String relJavaFile, int line) {
		BasicBlock basicBlock = Analyzer.analyze(relJavaFile, base);
		Set<Use> uses = basicBlock.getVariableUses(line);
		Set<String> variabls = new HashSet<>();
		for (Use use : uses) {
			if (use.getUseType() == Use.USETYPE.WRITE) {
				variabls.add(use.getVariableDefine().getName());
			}
		}
		return variabls;
	}

}
