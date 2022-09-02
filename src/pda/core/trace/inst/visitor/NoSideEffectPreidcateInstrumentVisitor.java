/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.trace.inst.visitor;

import pda.common.utils.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class NoSideEffectPreidcateInstrumentVisitor extends TraversalVisitor {

	protected Map<Integer, List<Pair<String, String>>> _predicates = new HashMap<>();

	public abstract void initOneRun(Set<Integer> lines, String srcPath, String relJavaPath);

	public Map<Integer, List<Pair<String, String>>> getPredicates() {
		return _predicates;
	}

}
