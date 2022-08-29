/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

import org.eclipse.jdt.core.dom.Type;

public class BoolValue extends ConstValue {

	private Boolean _value = false;
	
	public BoolValue(String file, int line, int column, Boolean value, Type type) {
		super(file, line, column, type);
		_value = value;
	}

	@Override
	public Boolean getValue() {
		return _value;
	}

}
