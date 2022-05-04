/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.trace.inst.visitor;

import org.eclipse.jdt.core.dom.*;
import pda.common.conf.Constant;
import pda.common.utils.Identifier;
import pda.core.trace.inst.gen.GenStatement;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is used for instrument for each method, only one print statement
 * for each method
 * 
 * @author Jiajun
 *
 */
public class MethodInstrumentVisitor extends TraversalVisitor {

	private final static String __name__ = "@MethodInstrumentVisitor ";

	public MethodInstrumentVisitor() {
	}

	public MethodInstrumentVisitor(String methodFlag) {
		_methodFlag = methodFlag;
	}

	public MethodInstrumentVisitor(Set<Integer> methods) {
		_methods = methods;
	}

	@Override
	public boolean visit(MethodDeclaration node) {

		String message = buildMethodInfoString(node);
		if (message == null) {
			return true;
		}

		int keyValue = Identifier.getIdentifier(message);

		if (shouldSkip(node, keyValue)) {
			return true;
		}

		if (node.getBody() != null) {
			Block body = node.getBody();
			List<ASTNode> backupStatement = new ArrayList<>();
			AST ast = AST.newAST(Constant.AST_LEVEL);

			ASTNode thisOrSuperStatement = null;
			if (body.statements().size() > 0) {
				ASTNode astNode = (ASTNode) body.statements().get(0);
				int startIndex = 0;
				if (astNode instanceof SuperConstructorInvocation
						|| (astNode instanceof ConstructorInvocation && astNode.toString().startsWith("this"))) {
					thisOrSuperStatement = ASTNode.copySubtree(ast, astNode);
					startIndex = 1;
				}
				for (; startIndex < body.statements().size(); startIndex++) {
					ASTNode statement = (ASTNode) body.statements().get(startIndex);
					backupStatement.add(ASTNode.copySubtree(ast, statement));
				}
			}

			// optimize instrument
//			message = Constant.INSTRUMENT_FLAG + _methodFlag + Constant.INSTRUMENT_STR_SEP + String.valueOf(keyValue);
			message = String.valueOf(keyValue);

			int lineNumber = _cu.getLineNumber(node.getBody().getStartPosition());

			Statement insert = GenStatement.genASTNode(message, lineNumber);

			body.statements().clear();
			if (thisOrSuperStatement != null) {
				body.statements().add(ASTNode.copySubtree(body.getAST(), thisOrSuperStatement));
			}
			body.statements().add(ASTNode.copySubtree(body.getAST(), insert));
			for (ASTNode statement : backupStatement) {
				body.statements().add(ASTNode.copySubtree(body.getAST(), statement));
			}
		}

		return true;
	}

}