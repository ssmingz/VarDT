/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

import org.eclipse.jdt.core.dom.Type;

public class StringValue extends ConstValue {

	private String _value = "";
	
	public StringValue(String file, int line, int column, String value, Type type) {
		super(file, line, column, type);
		_value = value;
	}

	@Override
	public String getValue() {
		return _value;
	}
	
	
}
