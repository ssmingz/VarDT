/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

import org.eclipse.jdt.core.dom.Type;

public class CharValue extends ConstValue {

	private Character _value = ' ';

	public CharValue(String file, int line, int column, Character ch, Type type) {
		super(file, line, column, type);
		_value = ch;
	}

	@Override
	public Character getValue() {
		return _value;
	}

}
