/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

import org.eclipse.jdt.core.dom.Type;

public class IntValue extends ConstValue {

	private Integer _value = 0;
	
	public IntValue(String file, int line, int column, Integer value, Type type) {
		super(file, line, column, type);
		_value = value;
	}

	@Override
	public Integer getValue() {
		return _value;
	}

}
