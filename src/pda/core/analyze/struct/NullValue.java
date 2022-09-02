/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

import org.eclipse.jdt.core.dom.AST;

public class NullValue extends ConstValue {

	public NullValue(String file, int line, int column) {
		super(file, line, column, AST.newAST(AST.JLS8).newWildcardType());
	}

	@Override
	public Object getValue() {
		return null;
	}

}
