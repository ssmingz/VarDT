/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.trace.inst.gen;

public class GenName {
	
	private static int variableCount = 0;
	
	public static String genVariableName(int line) {
		variableCount++;
		return "automatic_" + Integer.toString(line) + "_" + Integer.toString(variableCount); 
	}
}
