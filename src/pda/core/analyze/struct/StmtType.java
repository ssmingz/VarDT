/**
 * Copyright (C) CIC, TJU, PRC. - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.core.analyze.struct;

/**
 * @author Jiajun
 *
 */
public enum StmtType {

	METHODECL("MethodDeclaration"),
	FIELDDECL("FieldDeclaration"),
	ASSERT("AssertStatement"),
    BLOCK("Block"),
    BREAK("BreakStatement"),
    CONSTRUCTINV("ConstructorInvocation"),
    CONTINUE("ContinueStatement"),
    DO("DoStatement"),
    EMPTY("EmptyStatement"),
    ENHANCEDFOR("EnhancedForStatement"),
    ASSIGNMENT("Assignment"),
    EXRESSION("ExpressionStatement"),
    FOR_INIT("ForStatement-init"),
    FOR_COND("ForStatement-cond"),
    FOR_UPDT("ForStatement-updt"),
    FOR_BODY("ForStatement-body"),
    IF("IfStatement"),
    LABELED("LabeledStatement"),
    RETURN("ReturnStatement"),
    SUPERCONSTRUCTORINV("SuperConstructorInvocation"),
    CASE("SwitchCase"),
    SWITCH("SwitchStatement"),
    SYNCHRONIZED("SynchronizedStatement"),
    THROW("ThrowStatement"),
    TRY("TryStatement"),
    TYPEDECL("TypeDeclarationStatement"),
    VARDECL("VariableDeclarationStatement"),
    WHILE("WhileStatement"),
    CATCH("CatchClause"),
    FINALLY("Finally"),
    UNKNOWN("UNKNOWN");
    
    private String _value; 
    private StmtType(String value) {
    		_value = value;
    }
    
    @Override
    public String toString() {
    		return _value;
    }
	
}
