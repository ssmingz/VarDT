package fl.instr.gen;

import fl.utils.Constant;
import fl.utils.JavaLogger;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.*;


/**
 * @description: this class used to define the output format for different data types, like predicates
 * @author:
 * @time: 2021/7/25 10:39
 */
public class GenPredicate {
    private static String _name = "@GenPredicate ";
    private static AST _ast = AST.newAST(AST.JLS8);

    public static Map<Expression, int[]> lineColSizeByExpNode = new LinkedHashMap<>();

    /**
     * extract identifier for Name
     * @param exp
     * @return
     */
    public static LinkedHashMap<String, Expression> genPred(Name exp) {
        LinkedHashMap<String, Expression> result = new LinkedHashMap<>();
        // TODO: should check exp's type in a more general way such as resolveTypeBinding
        if(exp.isSimpleName()) {
            String content = ((SimpleName) exp).getIdentifier();
            // by default, names that begin with an uppercase letter are not variables
            if(!Character.isUpperCase(content.charAt(0)) && content.charAt(0) != '$') {
                result.put(content, exp);
            }
            if(exp.resolveBinding() instanceof ITypeBinding) {
                String clazz = Constant.CURRENT_CLAZZ.substring(Constant.CURRENT_CLAZZ.lastIndexOf('.')+1);
                if(clazz.contains("$")) {
                    clazz = clazz.substring(clazz.lastIndexOf("$")+1);
                }
                if(clazz.equals(content)) {
                    result.put(content, exp);
                }
            }
        } else if(exp.isQualifiedName()) {
            String content = exp.getFullyQualifiedName();
            if(!Character.isUpperCase(content.charAt(0)) && content.charAt(0) != '$') {
                result.put(content, exp);
            }
        }

        return result;
    }

    /**
     * generate writers for expressions in argument list and return statement
     * @param exp : argument
     * @param stmt : invocated method
     */
    public static void genArgumentPred(Expression exp, Statement stmt, ASTNode curMD, int index) {
        if(exp == null) {
            return;
        }
        switch (exp.getNodeType()) {
            case ASTNode.BOOLEAN_LITERAL:
            case ASTNode.NUMBER_LITERAL:
            case ASTNode.STRING_LITERAL:
            case ASTNode.NULL_LITERAL:
            case ASTNode.CHARACTER_LITERAL:
            case ASTNode.TYPE_LITERAL:
                return;
            default:
        }
        if(exp instanceof SimpleName) {
            // check whether is vars created by counter, quit if true
            String iden = ((SimpleName) exp).getIdentifier();
            if(iden.startsWith("_")) {
                String postfix = iden.substring(iden.indexOf('_')+1);
                if(StringUtils.isNumeric(postfix) && Integer.valueOf(postfix) <= Constant.INSTRUMENT_COUNTER) {
                    return;
                }
            }
            return; // ignore var in expression and only handle it in writeName()
        } else if(exp instanceof QualifiedName) {
            return;
        }

        MethodInvocation pred = exp.getAST().newMethodInvocation();
        pred.setExpression(exp.getAST().newName("auxiliary.InstrAux"));
        pred.setName(exp.getAST().newSimpleName("getValue"));
        StringLiteral iden = exp.getAST().newStringLiteral();
        iden.setLiteralValue(""+Constant.CURRENT_METHOD_ID+"#"+"(" + exp.toString() + ")");

        int line = GenWriter.getLineNo(exp);
        int col = GenWriter.getColNo(exp);
        int size = GenWriter.getExpSize(exp);
        NumberLiteral line1 = exp.getAST().newNumberLiteral(""+line);
        NumberLiteral col1 = exp.getAST().newNumberLiteral(""+col);
        NumberLiteral size1 = exp.getAST().newNumberLiteral(""+size);

        // cast to the old type
        ITypeBinding castType = exp.resolveTypeBinding();
        Expression argnew;
        if(castType == null) {
            JavaLogger.error(_name + "#genArgumentPred parameter type not resolving : " + exp.toString() + " " + Constant.PROJECT_PATH + " " + Constant.CURRENT_METHOD);
            return ;
        } else if(castType.isPrimitive()) {
            argnew = pred;
        } else if(castType.isNullType()) {
            // TODO: check whether need to handle with null arg
            return ;
        } else {
            CastExpression cast = exp.getAST().newCastExpression();
            cast.setExpression(pred);
            cast.setType((Type) ASTNode.copySubtree(exp.getAST(), resolvingType(castType, exp)));
            argnew = cast;
        }

        // placeholder to replace the old arg
        MethodInvocation placeholder = exp.getAST().newMethodInvocation();
        if (curMD instanceof MethodInvocation) {
            ((MethodInvocation) curMD).arguments().set(index, placeholder);
        } else if (curMD instanceof SuperMethodInvocation) {
            ((SuperMethodInvocation) curMD).arguments().set(index, placeholder);
        } else if (curMD instanceof ConstructorInvocation) {
            ((ConstructorInvocation) curMD).arguments().set(index, placeholder);
        } else if (curMD instanceof SuperConstructorInvocation) {
            ((SuperConstructorInvocation) curMD).arguments().set(index, placeholder);
        } else if (curMD instanceof ReturnStatement) {
            ((ReturnStatement) curMD).setExpression(placeholder);
        }

        pred.arguments().add(exp);
        pred.arguments().add(iden);
        pred.arguments().add(line1);
        pred.arguments().add(col1);
        pred.arguments().add(size1);

        // replace the placeholder
        if (curMD instanceof MethodInvocation) {
            ((MethodInvocation) curMD).arguments().set(index, argnew);
        } else if (curMD instanceof SuperMethodInvocation) {
            ((SuperMethodInvocation) curMD).arguments().set(index, argnew);
        } else if (curMD instanceof ConstructorInvocation) {
            ((ConstructorInvocation) curMD).arguments().set(index, argnew);
        } else if (curMD instanceof SuperConstructorInvocation) {
            ((SuperConstructorInvocation) curMD).arguments().set(index, argnew);
        } else if (curMD instanceof ReturnStatement) {
            ((ReturnStatement) curMD).setExpression(argnew);
        }
    }

    // TODO : handle other special types
    private static Type resolvingType(ITypeBinding binding, Expression exp) {
        Type type = null;
        if(exp instanceof ClassInstanceCreation) {
            // TODO: check other similar type creation
            type = (Type) ASTNode.copySubtree(_ast, ((ClassInstanceCreation) exp).getType());
        } else if(binding.isArray()) {
            type = _ast.newArrayType(resolvingType(binding.getElementType(), exp), binding.getDimensions());
        } else if(binding.isPrimitive()) {
            type = _ast.newPrimitiveType(PrimitiveType.toCode(binding.getName()));
        } else if(binding.isWildcardType()) {
            type = _ast.newWildcardType();
        } else if(binding.isParameterizedType()) {
            String name = binding.getQualifiedName();
            if(name.contains("<")) {
                String fullname = binding.getBinaryName();
                if(fullname.contains("$")) {
                    fullname = fullname.replaceAll("\\$",".");
                }
                if(fullname.contains(".")) {
                    Name qName = _ast.newName(fullname.substring(0, fullname.lastIndexOf('.')));
                    SimpleName bName = _ast.newSimpleName(fullname.substring(fullname.lastIndexOf('.')+1));
                    type = _ast.newNameQualifiedType(qName, bName);
                } else {
                    type = _ast.newSimpleType(_ast.newSimpleName(fullname));
                }
                // TODO: check whether must need type arguments
                //for(Object t : binding.getTypeArguments()) {
                //    ((ParameterizedType) type).typeArguments().add(resolvingType((ITypeBinding) t));
                //}
            } else {
                JavaLogger.error(_name + "#resolvingType parameter type node not created : " + binding.getName() + " " + Constant.PROJECT_PATH + " " + Constant.CURRENT_METHOD);
            }
        } else {
            String fullname = binding.getName();
            if(!isImport(fullname, exp)) {
                fullname = binding.getQualifiedName();
            }
            if(fullname.contains("$")) {
                fullname = fullname.replaceAll("\\$",".");
            }
            if(fullname.contains(".")) {
                Name qName = _ast.newName(fullname.substring(0, fullname.lastIndexOf('.')));
                SimpleName bName = _ast.newSimpleName(fullname.substring(fullname.lastIndexOf('.')+1));
                type = _ast.newNameQualifiedType(qName, bName);
            } else {
                // TODO: check if need to use qualified name, or simply use simple name if already import
                type = _ast.newSimpleType(_ast.newSimpleName(fullname));
            }
        }
        return type;
    }

    private static boolean isImport(String fullname, ASTNode node) {
        if (node.getRoot() instanceof CompilationUnit) {
            CompilationUnit cu = (CompilationUnit) node.getRoot();
            for(Object imp : cu.imports()) {
                String imp_name = ((ImportDeclaration) imp).getName().toString();
                if (imp_name.endsWith(fullname)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * generate writers for expressions in if/for/while statement
     * @param exp
     * @param stmt
     */
    public static void genBooleanPred(Expression exp, Statement stmt, MethodDeclaration curMD) {
        if(exp instanceof BooleanLiteral) {
            // do not instrument for special case like while(true)
            return;
        }
        Expression oldCond, newCond;
        switch (stmt.getNodeType()) {
            case Type.IF_STATEMENT:
                oldCond = ((IfStatement) stmt).getExpression();
                // only take the whole condition as the processing start
                if(exp == oldCond) {
                    recordLineColSize(exp);
                    Expression cond = stmt.getAST().newParenthesizedExpression();
                    ((IfStatement) stmt).setExpression(cond);
                    newCond = assignCond(exp, stmt, curMD);
                    ((IfStatement) stmt).setExpression(newCond);
                }
                break;
            case Type.FOR_STATEMENT:
                oldCond = ((ForStatement) stmt).getExpression();
                if(exp == oldCond) {
                    recordLineColSize(exp);
                    Expression cond = stmt.getAST().newParenthesizedExpression();
                    ((ForStatement) stmt).setExpression(cond);
                    newCond = assignCond(exp, stmt, curMD);
                    ((ForStatement) stmt).setExpression(newCond);
                }
                break;
            case Type.WHILE_STATEMENT:
                oldCond = ((WhileStatement) stmt).getExpression();
                if(exp == oldCond) {
                    recordLineColSize(exp);
                    Expression cond = stmt.getAST().newParenthesizedExpression();
                    ((WhileStatement) stmt).setExpression(cond);
                    newCond = assignCond(exp, stmt, curMD);
                    ((WhileStatement) stmt).setExpression(newCond);
                }
                break;
            case Type.DO_STATEMENT:
                oldCond = ((DoStatement) stmt).getExpression();
                if(exp == oldCond) {
                    recordLineColSize(exp);
                    Expression cond = stmt.getAST().newParenthesizedExpression();
                    ((DoStatement) stmt).setExpression(cond);
                    newCond = assignCond(exp, stmt, curMD);
                    ((DoStatement) stmt).setExpression(newCond);
                }
                break;
            default:
                return;
        }
    }

    private static void recordLineColSize(Expression exp) {
        if(exp instanceof ParenthesizedExpression) {
            recordLineColSize(((ParenthesizedExpression) exp).getExpression());
        } else if(exp instanceof InfixExpression &&
                (((InfixExpression) exp).getOperator() == InfixExpression.Operator.CONDITIONAL_AND ||
                        ((InfixExpression) exp).getOperator() == InfixExpression.Operator.CONDITIONAL_OR)) {
            recordLineColSize(((InfixExpression) exp).getLeftOperand());
            recordLineColSize(((InfixExpression) exp).getRightOperand());
            for(int i=0; i<((InfixExpression) exp).extendedOperands().size(); i++) {
                recordLineColSize((Expression) ((InfixExpression) exp).extendedOperands().get(i));
            }
        }
        int[] linecolsize = new int[3];
        linecolsize[0] = GenWriter.getLineNo(exp);
        linecolsize[1] = GenWriter.getColNo(exp);
        linecolsize[2] = GenWriter.getExpSize(exp);
        if(linecolsize[0]!=-1 && linecolsize[1]!=-1 && linecolsize[2]!=-1) {
            lineColSizeByExpNode.put(exp, linecolsize);
        }
    }

    /**
     * recursively process the old condition in if/while/for statement
     * include assigned to a boolean var in old condition
     * declare and output this var in new statements
     * @param exp
     * @param stmt
     */
    private static Expression assignCond(Expression exp, Statement stmt, MethodDeclaration curMD) {
        Expression newExp;
        if (exp instanceof ParenthesizedExpression) {
            Expression iop = exp.getAST().newParenthesizedExpression(); // placeholder
            Expression iold = ((ParenthesizedExpression) exp).getExpression();
            ((ParenthesizedExpression) exp).setExpression(iop);
            iop = assignCond(iold, stmt, curMD);
            ((ParenthesizedExpression) exp).setExpression(iop);
            newExp = exp;
        } else {
            // 1.cond -> (_count=(cond))
            Assignment assign = exp.getAST().newAssignment();
            assign.setOperator(Assignment.Operator.ASSIGN);
            Constant.INSTRUMENT_COUNTER++;
            assign.setLeftHandSide(exp.getAST().newSimpleName("_" + Constant.INSTRUMENT_COUNTER));
            ParenthesizedExpression inParen = exp.getAST().newParenthesizedExpression();
            inParen.setExpression(exp); // since already set placeholder for exp's parent in previous method genBooleanPred() and assignCond()
            assign.setRightHandSide(inParen);
            ParenthesizedExpression outParen = exp.getAST().newParenthesizedExpression();
            outParen.setExpression(assign);
            newExp = outParen;
            // 2.boolean _count = false;
            VariableDeclarationFragment vdf = _ast.newVariableDeclarationFragment();
            vdf.setName(_ast.newSimpleName("_" + Constant.INSTRUMENT_COUNTER));
            vdf.setInitializer(_ast.newBooleanLiteral(false));
            VariableDeclarationStatement vds = _ast.newVariableDeclarationStatement(vdf);
            vds.setType(_ast.newPrimitiveType(PrimitiveType.BOOLEAN));

            // 2b. record _count at default: logger.debug(content:_count)
            StringLiteral iden2 = _ast.newStringLiteral();
            iden2.setLiteralValue(""+Constant.CURRENT_METHOD_ID+"#"+"(" + exp.toString() + ")");

            int line = GenWriter.getLineNo(exp);
            int col = GenWriter.getColNo(exp);
            int size = GenWriter.getExpSize(exp);
            NumberLiteral line2 = _ast.newNumberLiteral(""+line);
            NumberLiteral col2 = _ast.newNumberLiteral(""+col);
            NumberLiteral size2 = _ast.newNumberLiteral(""+size);

            String content2 = "_" + Constant.INSTRUMENT_COUNTER;
            MethodInvocation logger2 = _ast.newMethodInvocation();
            logger2.setExpression(_ast.newName("auxiliary.InstrAux"));
            logger2.setName(_ast.newSimpleName("getValue"));
            logger2.arguments().add(_ast.newSimpleName(content2));
            logger2.arguments().add(iden2);
            logger2.arguments().add(line2);
            logger2.arguments().add(col2);
            logger2.arguments().add(size2);
            Statement writer = _ast.newExpressionStatement(logger2);

            // 3.record _count in body: logger.debug(content+_count)
            StringLiteral iden = _ast.newStringLiteral();
            iden.setLiteralValue(""+Constant.CURRENT_METHOD_ID+"#"+"(" + exp.toString() + ")");
            NumberLiteral line1 = _ast.newNumberLiteral(""+line);
            NumberLiteral col1 = _ast.newNumberLiteral(""+col);
            NumberLiteral size1 = _ast.newNumberLiteral(""+size);
            String content = "_" + Constant.INSTRUMENT_COUNTER;
            MethodInvocation logger = _ast.newMethodInvocation();
            logger.setExpression(_ast.newName("auxiliary.InstrAux"));
            logger.setName(_ast.newSimpleName("getValue"));
            logger.arguments().add(_ast.newSimpleName(content));
            logger.arguments().add(iden);
            logger.arguments().add(line1);
            logger.arguments().add(col1);
            logger.arguments().add(size1);
            Statement writer2 = _ast.newExpressionStatement(logger);
            // insert 2 before stmt
            List<Statement> insert = new ArrayList<>();
            insert.add(vds);
            insert.add(writer);
            insertBeforeStmt(stmt, insert, content, 0);
            // insert 3 in body head
            insertAtBodyHead(stmt, writer2);

            if(exp instanceof InfixExpression &&
                    (((InfixExpression) exp).getOperator() == InfixExpression.Operator.CONDITIONAL_AND ||
                            ((InfixExpression) exp).getOperator() == InfixExpression.Operator.CONDITIONAL_OR)) {
                Expression lop = exp.getAST().newParenthesizedExpression(); // placeholder
                Expression lold = ((InfixExpression) exp).getLeftOperand();
                ((InfixExpression) exp).setLeftOperand(lop);
                lop = assignCond(lold, stmt, curMD);
                ((InfixExpression) exp).setLeftOperand(lop);

                Expression rop = exp.getAST().newParenthesizedExpression(); // placeholder
                Expression rold = ((InfixExpression) exp).getRightOperand();
                ((InfixExpression) exp).setRightOperand(rop);
                rop = assignCond(rold, stmt, curMD);
                ((InfixExpression) exp).setRightOperand(rop);

                for(int i=0; i<((InfixExpression) exp).extendedOperands().size(); i++) {
                    Expression eop = exp.getAST().newParenthesizedExpression(); // placeholder
                    Expression eold = (Expression) ((InfixExpression) exp).extendedOperands().get(i);
                    ((InfixExpression) exp).extendedOperands().set(i, eop);
                    eop = assignCond(eold, stmt, curMD);
                    ((InfixExpression) exp).extendedOperands().set(i, eop);
                }
                //newExp = exp;
            }

        }
        return newExp;
    }

    private static void insertBeforeStmt(Statement stmt, List<Statement> insert, String content, int writeAfter) {
        // insert writer to body
        ASTNode targetBlock = GenWriter.findNearBlock(stmt, content);
        int insertIndex = GenWriter.findIndexInBlock(stmt, targetBlock, content);
        if (targetBlock != null && insertIndex != -1) {
            List<Statement> stmtlist;
            if (targetBlock instanceof Block) {
                stmtlist = ((Block) targetBlock).statements();
            } else if (targetBlock instanceof SwitchStatement) {
                stmtlist = ((SwitchStatement) targetBlock).statements();
            } else {
                JavaLogger.error(_name + "#insertBeforeStmt Target statement not in a block " + content);
                return;
            }
            stmtlist.addAll(insertIndex+writeAfter, ASTNode.copySubtrees(targetBlock.getAST(), insert));
        } else {
            JavaLogger.error(_name + "#insertBeforeStmt Target statement not in a block " + content);
            return;
        }
    }

    /**
     * insert logger at head of if/for/while body
     * @param stmt
     * @param writer
     */
    private static void insertAtBodyHead(Statement stmt, Statement writer) {
        ASTNode oldBody;
        switch (stmt.getNodeType()) {
            case Type.IF_STATEMENT:
                oldBody = ((IfStatement) stmt).getThenStatement();
                break;
            case Type.FOR_STATEMENT:
                oldBody = ((ForStatement) stmt).getBody();
                break;
            case Type.WHILE_STATEMENT:
                oldBody = ((WhileStatement) stmt).getBody();
                break;
            case Type.DO_STATEMENT:
                oldBody = ((DoStatement) stmt).getBody();
                break;
            default:
                return;
        }
        if(oldBody instanceof Block) {
            // TODO: simply insert to the head may cause problem like using before init
            if (stmt instanceof DoStatement) {
                ((Block) oldBody).statements().add(ASTNode.copySubtree(stmt.getAST(), writer));
            } else {
                ((Block) oldBody).statements().add(0, ASTNode.copySubtree(stmt.getAST(), writer));
            }
            return;
        } else {
            Block newBody = stmt.getAST().newBlock();
            switch (stmt.getNodeType()) {
                case Type.IF_STATEMENT:
                    ((IfStatement) stmt).setThenStatement(newBody);
                    return;
                case Type.FOR_STATEMENT:
                    ((ForStatement) stmt).setBody(newBody);
                    return;
                case Type.WHILE_STATEMENT:
                    ((WhileStatement) stmt).setBody(newBody);
                    return;
                case Type.DO_STATEMENT:
                    ((DoStatement) stmt).setBody(newBody);
                default:
            }
            newBody.statements().add(oldBody);
            newBody.statements().add(ASTNode.copySubtree(stmt.getAST(), writer));
        }
    }

    /**
     * write value of expression in return statement
     * @param stmt
     * @param lineNo
     */
    public static void genReturnValue(Statement stmt, int lineNo) {
        if(!(stmt instanceof ReturnStatement)) {
            return;
        }
        Expression exp = ((ReturnStatement) stmt).getExpression();
        // handle if it is "return;"
        if(exp == null) {
            return;
        }
        switch (exp.getNodeType()) {
            case ASTNode.BOOLEAN_LITERAL:
            case ASTNode.NUMBER_LITERAL:
            case ASTNode.STRING_LITERAL:
            case ASTNode.NULL_LITERAL:
            case ASTNode.CHARACTER_LITERAL:
            case ASTNode.TYPE_LITERAL:
                return;
            default:
        }

        genArgumentPred(exp, stmt, stmt, -1);
    }
}
