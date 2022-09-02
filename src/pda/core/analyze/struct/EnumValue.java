/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

import org.eclipse.jdt.core.dom.Type;

public class EnumValue extends ConstValue {

	private Object _value;

	public EnumValue(String file, int line, int column, Object object, Type type) {
		super(file, line, column, type);
		_value = object;
	}

	@Override
	public Object getValue() {
		return _value;
	}

}
