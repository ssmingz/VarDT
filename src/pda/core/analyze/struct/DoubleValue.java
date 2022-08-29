/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

import org.eclipse.jdt.core.dom.Type;

public class DoubleValue extends ConstValue {

	private Double _value;
	
	public DoubleValue(String file, int line, int column, Double value, Type type) {
		super(file, line, column, type);
		_value = value;
	}

	@Override
	public Double getValue() {
		return _value;
	}
	
}
