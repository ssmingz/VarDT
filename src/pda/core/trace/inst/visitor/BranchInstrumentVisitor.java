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

import java.util.Set;

/**
 * This class is used for instrument IfStatement in the given method,
 * which will generate reverse condition for the original IfStatement
 * even if it does not as an 'Else' branch.
 * 
 * @author Jiajun
 *
 */
public class BranchInstrumentVisitor extends TraversalVisitor {

	private final static String __name__ = "@BranchInstrumentVisitor ";

	public BranchInstrumentVisitor() {
	}

	public BranchInstrumentVisitor(Set<Integer> methods) {
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

		// optimize instrument
		message = String.valueOf(keyValue);

		Block methodBody = node.getBody();

		if (methodBody == null) {
			return true;
		}

		methodBody.accept(new BranchInstrumenter(message, _cu));

		return true;
	}
	
	static class BranchInstrumenter extends ASTVisitor {
		private String message = null;
		private CompilationUnit unit = null;
		public BranchInstrumenter(String message, CompilationUnit unit) {
			this.message = message;
			this.unit = unit;
		}
		
		@Override
		public boolean visit(IfStatement node) {
			Expression expression = node.getExpression();
			int line = unit.getLineNumber(expression.getStartPosition());
			String condition = expression.toString();
			
			ASTNode inserted = GenStatement.genWriter(message + "#" + line + "#" + condition + "#1#1");
			node.setThenStatement((Statement) ASTNode.copySubtree(node.getAST(), wrapAndInstert(node.getThenStatement(), inserted)));
			condition = "!(" + condition + ")";
			inserted = GenStatement.genWriter(message + "#" + line + "#" + condition + "#1#2");
			node.setElseStatement((Statement) ASTNode.copySubtree(node.getAST(), wrapAndInstert(node.getElseStatement(), inserted)));
			return true;
		}
		
		/**
		 *
		 * @param node : original node
		 * @param inserted : to be inserted at the first index of the returned block 
		 * @return
		 */
		private Block wrapAndInstert(ASTNode node, ASTNode inserted) {
			Block block = null;
			AST ast = AST.newAST(Constant.AST_LEVEL);
			if(node == null) {
				block = ast.newBlock();
			} else if(node instanceof Block) {
				block = (Block) node;
			} else {
				block = ast.newBlock();
				block.statements().add(ASTNode.copySubtree(block.getAST(), node));
			}
			block.statements().add(0, ASTNode.copySubtree(block.getAST(), inserted));
			return block;
		}
	}
}
