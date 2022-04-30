package fl.instr.gen;

import fl.instr.visitor.BlockTransVisitor;
import fl.instr.visitor.CheckInitVisitor;
import fl.utils.Constant;
import fl.utils.JavaFile;
import fl.utils.JavaLogger;
import org.apache.commons.lang.StringUtils;
import org.eclipse.jdt.core.dom.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @description: use Logger to write recorded value to file
 * @author:
 * @time: 2021/7/23 21:05
 */
public class GenWriter {
    private static String _name = "@GenWriter ";
    private static AST _ast = AST.newAST(AST.JLS8);

    /**
     * insert imports related to Logger to the given compilation unit
     * @param cu
     */
    public static void writerInitImports(CompilationUnit cu) {
        // create statements
        // 1. import
        ImportDeclaration imp2 = _ast.newImportDeclaration();
        imp2.setName(_ast.newName("org.apache.log4j.PropertyConfigurator"));
        // insert to cu
        boolean alreadyExist = false;
        for(Object i : cu.imports()) {
            ImportDeclaration imp = (ImportDeclaration) i;
            if(imp.getName().getFullyQualifiedName().equals("org.apache.log4j.PropertyConfigurator")) {
                alreadyExist = true;
                break;
            }
        }
        if(!alreadyExist) {
            cu.imports().add(ASTNode.copySubtree(cu.getAST(), imp2));
        }
    }

    /**
     * insert logger init to the given method declaration
     * @param cu
     */
    public static void writerInit(CompilationUnit cu, MethodDeclaration md, String projectPath) {
        // create statements
        // 1. initialize "Logger logger = Logger.getLogger("")"
        VariableDeclarationFragment vdf = _ast.newVariableDeclarationFragment();
        vdf.setName(_ast.newSimpleName("logger_log4j"));
        MethodInvocation loggerInit = _ast.newMethodInvocation();
        loggerInit.setExpression(_ast.newName("org.apache.log4j.Logger"));
        loggerInit.setName(_ast.newSimpleName("getLogger"));
        StringLiteral arg0 = _ast.newStringLiteral();
        arg0.setLiteralValue("");
        loggerInit.arguments().add(arg0);
        vdf.setInitializer(loggerInit);
        VariableDeclarationStatement vds = _ast.newVariableDeclarationStatement(vdf);
        vds.setType(_ast.newSimpleType(_ast.newName("Logger")));
        vds.setType(_ast.newNameQualifiedType(_ast.newName("org.apache.log4j"), _ast.newSimpleName("Logger")));

        // 2. set configuration file "PropertyConfigurator.configure("src/log4j.properties")"
        MethodInvocation m1 = _ast.newMethodInvocation();
        m1.setExpression(_ast.newSimpleName("PropertyConfigurator"));
        m1.setName(_ast.newSimpleName("configure"));
        StringLiteral arg1 = _ast.newStringLiteral();
        String properties = JavaFile.getSrcClasses(projectPath) + "/instrument-log4j.properties";
        //arg1.setLiteralValue("src/instrument-log4j.properties");
        arg1.setLiteralValue(properties);
        m1.arguments().add(arg1);
        ExpressionStatement call1 = _ast.newExpressionStatement(m1);

        // locate the target method
        Block targetMethod = md.getBody();
        int startindex = 0;
        if(!targetMethod.statements().isEmpty()) {
            if(targetMethod.statements().get(0) instanceof SuperConstructorInvocation || targetMethod.statements().get(0) instanceof ConstructorInvocation) {
                startindex = 1;
            } else {
                startindex = 0;
            }
        }

        // 3. record arguments
        for(Object arg : md.parameters()) {
            Constant.INSTRUMENT_COUNTER++;
            Name arg_node = ((SingleVariableDeclaration) arg).getName();
            String arg_name = nameFormat(arg_node.toString());
            int lineNo = getLineNo((ASTNode) arg);
            int colNo = getColNo((ASTNode) arg);
            MethodInvocation md0 = _ast.newMethodInvocation();
            md0.setExpression(_ast.newName("auxiliary.InstrAux"));
            md0.setName(_ast.newSimpleName("getValue"));
            // getValue(Object obj, String name, int lineNo, Logger logger)
            Name arg11 = (Name) ASTNode.copySubtree(_ast, arg_node);
            StringLiteral arg22 = _ast.newStringLiteral();
            arg22.setLiteralValue(arg_name);
            NumberLiteral arg33 = _ast.newNumberLiteral(String.valueOf(lineNo));
            NumberLiteral arg44 = _ast.newNumberLiteral(String.valueOf(colNo));
            md0.arguments().add(arg11);
            md0.arguments().add(arg22);
            md0.arguments().add(arg33);
            md0.arguments().add(arg44);
            //md0.arguments().add(arg55);
            Statement arg_writer = _ast.newExpressionStatement(md0);
            // add to stmt list
            targetMethod.statements().add(startindex+md.parameters().indexOf(arg), ASTNode.copySubtree(cu.getAST(), arg_writer));
        }
    }

    public static void writerInitForFilterVersion(CompilationUnit cu, MethodDeclaration md, String projectPath) {
        // create statements
        // 1. initialize "Logger logger = Logger.getLogger("")"
        VariableDeclarationFragment vdf = _ast.newVariableDeclarationFragment();
        vdf.setName(_ast.newSimpleName("logger_log4j"));
        MethodInvocation loggerInit = _ast.newMethodInvocation();
        loggerInit.setExpression(_ast.newName("org.apache.log4j.Logger"));
        loggerInit.setName(_ast.newSimpleName("getLogger"));
        StringLiteral arg0 = _ast.newStringLiteral();
        arg0.setLiteralValue("");
        loggerInit.arguments().add(arg0);
        vdf.setInitializer(loggerInit);
        VariableDeclarationStatement vds = _ast.newVariableDeclarationStatement(vdf);
        vds.setType(_ast.newNameQualifiedType(_ast.newName("org.apache.log4j"), _ast.newSimpleName("Logger")));
        // 2. set configuration file "PropertyConfigurator.configure("src/log4j.properties")"
        MethodInvocation m1 = _ast.newMethodInvocation();
        m1.setExpression(_ast.newSimpleName("PropertyConfigurator"));
        m1.setName(_ast.newSimpleName("configure"));
        StringLiteral arg1 = _ast.newStringLiteral();
        String properties = JavaFile.getSrcClasses(projectPath) + "/instrument-log4j.properties";
        arg1.setLiteralValue(properties);
        m1.arguments().add(arg1);
        ExpressionStatement call1 = _ast.newExpressionStatement(m1);
        // locate the target method
        Block targetMethod = md.getBody();
        int startindex = 0;
        if(!targetMethod.statements().isEmpty()) {
            if(targetMethod.statements().get(0) instanceof SuperConstructorInvocation || targetMethod.statements().get(0) instanceof ConstructorInvocation) {
                startindex = 1;
            } else {
                startindex = 0;
            }
        }
        targetMethod.statements().add(startindex, ASTNode.copySubtree(cu.getAST(), vds));
        targetMethod.statements().add(startindex+1, ASTNode.copySubtree(cu.getAST(), call1));

        // 3. record method name
        // change single-line to block
        BlockTransVisitor btVistor = new BlockTransVisitor(md);
        btVistor.traverse();
        // record current test name: auxiliary.InstrAux.updateRecorder_filterTest(recordcontent)
        String recordcontent = Constant.CURRENT_METHOD;
        //for(Object arg : md.parameters()) {
        //    if(arg instanceof SingleVariableDeclaration) {
        //        recordcontent += "," + ((SingleVariableDeclaration) arg).getType().toString();
        //    } else {
        //        JavaLogger.error(_name + "#writerInitForFilterVersion Type of argument is not SingleVariableDeclaration for method : " + recordcontent + " " + projectPath);
        //    }
        //}
        StringLiteral str = _ast.newStringLiteral();
        str.setLiteralValue(recordcontent);
        MethodInvocation logger = _ast.newMethodInvocation();
        logger.setExpression(_ast.newName("auxiliary.InstrAux"));
        logger.setName(_ast.newSimpleName("updateRecorder_filterTest"));
        logger.arguments().add(str);
        ExpressionStatement recordTestName = _ast.newExpressionStatement(logger);
        targetMethod.statements().add(startindex+2, ASTNode.copySubtree(cu.getAST(), recordTestName));
    }

    public static String nameFormat(String toString) {
        return String.format("%s#%s", Constant.CURRENT_METHOD_ID, toString);
    }

    public static void write(String element, Expression exp, Statement stmt, int writeAfter) {
        if(exp instanceof Name) {
            writeName(element, (Name) exp, stmt, writeAfter);
        }
        //else {
        //    writeOthers(element, exp, stmt, writeAfter);
        //}
        else if(exp instanceof FieldAccess) {
            writeName(element, (FieldAccess) exp, stmt, writeAfter);
        }
    }

    /**
     * insert statment for expression calculation:
     * String _count = "";try{_count=""+exp;}catch(Exception e){_count="";}finally{logger.info(content+_count);}
     * @param element
     * @param exp
     * @param stmt
     * @param writeAfter
     */
    private static void writeOthers(String element, Expression exp, Statement stmt, int writeAfter) {
        if(stmt instanceof ReturnStatement) {
            writeAfter = 0;
        }
        int line = getLineNo(exp);
        int col = getColNo(exp);

        String content = element + "-" + line + ":";
        MethodInvocation logger = _ast.newMethodInvocation();
        logger.setExpression(_ast.newName("auxiliary.InstrAux"));
        logger.setName(_ast.newSimpleName("getValue"));
        StringLiteral iden = _ast.newStringLiteral();
        iden.setLiteralValue(nameFormat(element));

        NumberLiteral line1 = _ast.newNumberLiteral(""+line);
        NumberLiteral col1 = _ast.newNumberLiteral(""+col);

        logger.arguments().add(ASTNode.copySubtree(_ast, exp));
        logger.arguments().add(iden);
        logger.arguments().add(line1);
        logger.arguments().add(col1);
        Statement writer = _ast.newExpressionStatement(logger);

        // writers[0] is vds, [1] is tryStmt
        Statement tryStmt = writer;
        // insert vds and tryStmt to target body
        insertToBody(stmt, tryStmt, content, writeAfter);
    }

    /**
     * generate writer in try/catch type
     * String _count = "";try{_count=""+exp;}catch(Exception e){_count="";}finally{logger.info(content+_count);}
     * @return a list, [0] is vds and [1] is tryStmt
     */
    public static List<Statement> genWriterInTryCatch(Expression content, Expression exp) {
        List<Statement> result = new ArrayList<>();
        Constant.INSTRUMENT_COUNTER++;
        // generate declaration
        VariableDeclarationFragment vdf = _ast.newVariableDeclarationFragment();
        vdf.setName(_ast.newSimpleName("_" + Constant.INSTRUMENT_COUNTER));
        vdf.setInitializer(_ast.newStringLiteral());
        VariableDeclarationStatement vds = _ast.newVariableDeclarationStatement(vdf);
        vds.setType(_ast.newSimpleType(_ast.newSimpleName("String")));
        result.add(vds);
        // generate try/catch
        TryStatement tryStmt = _ast.newTryStatement();
        // try{_count=""+exp;}
        Block tryBody = _ast.newBlock();
        Assignment tryAssign = _ast.newAssignment();
        tryAssign.setOperator(Assignment.Operator.ASSIGN);
        tryAssign.setLeftHandSide(_ast.newSimpleName("_" + Constant.INSTRUMENT_COUNTER));
        InfixExpression inf = _ast.newInfixExpression();
        inf.setOperator(InfixExpression.Operator.PLUS);
        inf.setLeftOperand(_ast.newStringLiteral());
        inf.setRightOperand((Expression) ASTNode.copySubtree(_ast, exp));
        tryAssign.setRightHandSide(inf);
        ExpressionStatement assign = _ast.newExpressionStatement(tryAssign);
        tryBody.statements().add(assign);
        tryStmt.setBody(tryBody);
        // catch(Exception e){_count="";}
        CatchClause catchClause = _ast.newCatchClause();
        SingleVariableDeclaration svd = _ast.newSingleVariableDeclaration();
        svd.setName(_ast.newSimpleName("_e_"+Constant.INSTRUMENT_COUNTER));
        svd.setType(_ast.newSimpleType(_ast.newSimpleName("Exception")));
        catchClause.setException(svd);
        Block catchBody = _ast.newBlock();
        Assignment catchAssign = _ast.newAssignment();
        catchAssign.setOperator(Assignment.Operator.ASSIGN);
        catchAssign.setLeftHandSide(_ast.newSimpleName("_" + Constant.INSTRUMENT_COUNTER));
        catchAssign.setRightHandSide(_ast.newStringLiteral());
        ExpressionStatement assign2 = _ast.newExpressionStatement(catchAssign);
        catchBody.statements().add(assign2);
        catchClause.setBody(catchBody);
        tryStmt.catchClauses().add(catchClause);
        // finally{logger.info(content+_count);}
        MethodInvocation logger = _ast.newMethodInvocation();
        logger.setExpression(_ast.newSimpleName("logger_log4j"));
        logger.setName(_ast.newSimpleName("debug"));
        InfixExpression inf2 = _ast.newInfixExpression();
        inf2.setOperator(InfixExpression.Operator.PLUS);
        inf2.setLeftOperand((Expression) ASTNode.copySubtree(_ast, content));
        inf2.setRightOperand(_ast.newSimpleName("_" + Constant.INSTRUMENT_COUNTER));
        logger.arguments().add(inf2);
        Statement writer = _ast.newExpressionStatement(logger);
        Block finallyBody = _ast.newBlock();
        finallyBody.statements().add(writer);
        tryStmt.setFinally(finallyBody);
        result.add(tryStmt);
        return result;
    }

    /**
     * insert statement for SimpleName: "logger.info(content+(exp==null?"":""+exp))"
     * @param element : string format of the target expression
     * @param exp : expression to be recorded
     * @param stmt : statement containing exp
     * @param writeAfter : whether insert after the target statement, equals to 0 usually
     */
    public static void writeName(String element, Name exp, Statement stmt, int writeAfter) {
        if(stmt instanceof ReturnStatement) {
            writeAfter = 0;
        }

        int line = getLineNo(exp);
        int col = getColNo(exp);

        /*
        // instrument for Class static field
        if (Character.isUpperCase(element.charAt(0))) {
            for(FieldDeclaration field : Constant.CURRENT_CLZ_NODE.getFields()) {
                for(Object m : field.modifiers()) {
                    if(((Modifier) m).isStatic()) {
                        for(Object v : field.fragments()) {
                            String fname = ((VariableDeclarationFragment) v).getName().getIdentifier();
                            // generate new statement
                            MethodInvocation md = _ast.newMethodInvocation();
                            md.setExpression(_ast.newName("auxiliary.InstrAux"));
                            md.setName(_ast.newSimpleName("getValue"));

                            QualifiedName arg1 = _ast.newQualifiedName(_ast.newSimpleName(element), _ast.newSimpleName(fname));
                            StringLiteral arg2 = _ast.newStringLiteral();
                            arg2.setLiteralValue(nameFormat(element+"."+fname));
                            NumberLiteral arg3 = _ast.newNumberLiteral(String.valueOf(line));
                            NumberLiteral arg4 = _ast.newNumberLiteral(String.valueOf(col));
                            md.arguments().add(arg1);
                            md.arguments().add(arg2);
                            md.arguments().add(arg3);
                            md.arguments().add(arg4);
                            Statement writer= _ast.newExpressionStatement(md);
                            String content = element+"."+fname + "-" + line + ":";

                            insertToBody(stmt, writer, content, 1);
                        }
                    }
                }
            }
            return;
        }
         */

        String content = element + "-" + line + ":";
        // check whether already init only for simple name
        // TODO: how to handle all possible init cases
        // more than Assignment/argument/declaration, method invocation can also initialize
        if (needCheckInit()) {
            if (exp instanceof SimpleName) {
                if(!(exp.resolveBinding() instanceof IVariableBinding)) {
                    return;
                }
                CheckInitVisitor initVisitor = new CheckInitVisitor(GenStatement._curMethodDec);
                if (!initVisitor.initValid(((SimpleName) exp).getIdentifier(), getLineNo(stmt))) {
                    writeAfter = 1;
                }
            }
        }

        // generate new statement
        MethodInvocation md = _ast.newMethodInvocation();
        md.setExpression(_ast.newName("auxiliary.InstrAux"));
        md.setName(_ast.newSimpleName("getValue"));
        // getValue(Object obj, String name, int lineNo, Logger logger)
        ASTNode arg1 = _ast.newNullLiteral();
        if(exp instanceof SimpleName) {
            arg1 = (Name) ASTNode.copySubtree(_ast, exp);
            // check whether is vars created by counter, quit if true
            String iden = ((SimpleName) exp).getIdentifier();
            if(iden.startsWith("_")) {
                String postfix = iden.substring(iden.indexOf('_')+1);
                if(StringUtils.isNumeric(postfix) && Integer.valueOf(postfix) <= Constant.INSTRUMENT_COUNTER) {
                    return;
                }
            }
            if(iden.equals("auxiliary")) {
                return;
            }
        }
        if(exp instanceof QualifiedName) {
            // TODO: need to handle case like l.f and l is null, and type(l.f) may not be an object
            if(((QualifiedName) exp).getFullyQualifiedName().equals("auxiliary.InstrAux")) {
                return;
            }
            arg1 = (Name) ASTNode.copySubtree(_ast, exp);
        }

        StringLiteral arg2 = _ast.newStringLiteral();
        arg2.setLiteralValue(nameFormat(element));
        NumberLiteral arg3 = _ast.newNumberLiteral(String.valueOf(line));
        NumberLiteral arg4 = _ast.newNumberLiteral(String.valueOf(col));
        md.arguments().add(arg1);
        md.arguments().add(arg2);
        md.arguments().add(arg3);
        md.arguments().add(arg4);
        //md.arguments().add(arg5);
        Statement writer= _ast.newExpressionStatement(md);

        // insert to body
        if(exp instanceof SimpleName) {
            if(!(exp.resolveBinding() instanceof IVariableBinding)) {
                return;
            }
            insertToBody(stmt, writer, content, writeAfter);
        }
        if(exp instanceof QualifiedName) {
            // record(l.f) ---> if(l!=null){record(l.f)}
            if(!(((QualifiedName) exp).getQualifier().resolveBinding() instanceof IVariableBinding)){
                // no need to check null
                insertToBody(stmt, writer, content, writeAfter);
                return;
            }
            IfStatement checkNull_writer = _ast.newIfStatement();
            InfixExpression checkExp = _ast.newInfixExpression();
            checkExp.setOperator(InfixExpression.Operator.NOT_EQUALS);
            checkExp.setLeftOperand((Expression) ASTNode.copySubtree(_ast, ((QualifiedName) exp).getQualifier()));
            checkExp.setRightOperand(_ast.newNullLiteral());
            checkNull_writer.setExpression(checkExp);
            checkNull_writer.setThenStatement(writer);
            insertToBody(stmt, checkNull_writer, content, writeAfter);
        }
    }

    public static void writeName(String element, FieldAccess exp, Statement stmt, int writeAfter) {
        if(stmt instanceof ReturnStatement) {
            writeAfter = 0;
        }

        int line = getLineNo(exp);
        int col = getColNo(exp);

        String content = element + "-" + line + ":";
        // check whether already init only for simple name
        // TODO: how to handle all possible init cases
        // more than Assignment/argument/declaration, method invocation can also initialize
        if (needCheckInit()) {
            CheckInitVisitor initVisitor = new CheckInitVisitor(GenStatement._curMethodDec);
            if (!initVisitor.initValid(exp.getName().getIdentifier(), getLineNo(stmt))) {
                writeAfter = 1;
            }
        }

        // generate new statement
        MethodInvocation md = _ast.newMethodInvocation();
        md.setExpression(_ast.newName("auxiliary.InstrAux"));
        md.setName(_ast.newSimpleName("getValue"));
        ASTNode arg1 = ASTNode.copySubtree(_ast, exp);

        StringLiteral arg2 = _ast.newStringLiteral();
        arg2.setLiteralValue(nameFormat(element));
        NumberLiteral arg3 = _ast.newNumberLiteral(String.valueOf(line));
        NumberLiteral arg4 = _ast.newNumberLiteral(String.valueOf(col));
        md.arguments().add(arg1);
        md.arguments().add(arg2);
        md.arguments().add(arg3);
        md.arguments().add(arg4);
        Statement writer= _ast.newExpressionStatement(md);

        // insert to body
        insertToBody(stmt, writer, content, writeAfter);
    }

    private static boolean needCheckInit() {
        boolean need = false;
        if (Constant.PROJECT_ID.equals("cli") && Constant.BUG_ID.equals("32") && Constant.CURRENT_METHOD.contains("findWrapPos")) {
            need = true;
        }
        return need;
    }

    /**
     * add import and logger init in test class
     * @param cu
     */
    public static void writerInitForTestMethod(CompilationUnit cu) {
        // 1. import
        ImportDeclaration imp2 = _ast.newImportDeclaration();
        imp2.setName(_ast.newName("org.apache.log4j.PropertyConfigurator"));
        // insert to cu
        boolean alreadyExist = false;
        for(Object i : cu.imports()) {
            ImportDeclaration imp = (ImportDeclaration) i;
            if(imp.getName().getFullyQualifiedName().equals("org.apache.log4j.PropertyConfigurator")) {
                alreadyExist = true;
                break;
            }
        }
        if(!alreadyExist) {
            cu.imports().add(ASTNode.copySubtree(cu.getAST(), imp2));
        }
    }

    private static void insertToBody(Statement stmt, Statement writer, String content, int writeAfter) {
        // insert writer to body
        ASTNode targetBlock = null;
        // insert to loop body inside
        if (stmt instanceof ForStatement) {
            targetBlock = ((ForStatement) stmt).getBody();
            if (targetBlock instanceof Block) {
                ((Block) targetBlock).statements().add(0, ASTNode.copySubtree(targetBlock.getAST(), writer));
            } else {
                Block newbody = stmt.getAST().newBlock();
                ((ForStatement) stmt).setBody(newbody);
                newbody.statements().add(writer);
                newbody.statements().add(targetBlock);
            }
        } else if (stmt instanceof EnhancedForStatement) {
            targetBlock = ((EnhancedForStatement) stmt).getBody();
            if (targetBlock instanceof Block) {
                ((Block) targetBlock).statements().add(0, ASTNode.copySubtree(targetBlock.getAST(), writer));
            } else {
                Block newbody = stmt.getAST().newBlock();
                ((EnhancedForStatement) stmt).setBody(newbody);
                newbody.statements().add(writer);
                newbody.statements().add(targetBlock);
            }
        }
        else if (stmt instanceof WhileStatement) {
            targetBlock = ((WhileStatement) stmt).getBody();
            if (targetBlock instanceof Block) {
                ((Block) targetBlock).statements().add(0, ASTNode.copySubtree(targetBlock.getAST(), writer));
            } else {
                Block newbody = stmt.getAST().newBlock();
                ((WhileStatement) stmt).setBody(newbody);
                newbody.statements().add(writer);
                newbody.statements().add(targetBlock);
            }
        }
        else if (stmt instanceof DoStatement) {
            targetBlock = ((DoStatement) stmt).getBody();
            if (targetBlock instanceof Block) {
                insertToBodyTailWithReturnCheck((Block) targetBlock, (Statement) ASTNode.copySubtree(targetBlock.getAST(), writer));
                //((Block) targetBlock).statements().add(ASTNode.copySubtree(targetBlock.getAST(), writer));
            } else {
                Block newbody = stmt.getAST().newBlock();
                ((DoStatement) stmt).setBody(newbody);
                newbody.statements().add(targetBlock);
                insertToBodyTailWithReturnCheck(newbody, writer);
                //newbody.statements().add(writer);
            }
        }
        else {
            // also insert to body if it is if-exp
            if (stmt instanceof IfStatement) {
                targetBlock = ((IfStatement) stmt).getThenStatement();
                if (targetBlock instanceof Block) {
                    ((Block) targetBlock).statements().add(0, ASTNode.copySubtree(targetBlock.getAST(), writer));
                } else {
                    Block newbody = stmt.getAST().newBlock();
                    ((WhileStatement) stmt).setBody(newbody);
                    newbody.statements().add(writer);
                    newbody.statements().add(targetBlock);
                }
            }
            // or insert to outside body
            targetBlock = findNearBlock(stmt, content);
            int insertIndex = findIndexInBlock(stmt, targetBlock, content);
            if (targetBlock != null && insertIndex != -1) {
                List<Statement> stmtlist;
                if (targetBlock instanceof Block) {
                    stmtlist = ((Block) targetBlock).statements();
                } else if (targetBlock instanceof SwitchStatement) {
                    stmtlist = ((SwitchStatement) targetBlock).statements();
                } else {
                    JavaLogger.error(_name + "#insertToBody Target statement not in a block " + content);
                    return;
                }
                stmtlist.add(insertIndex+writeAfter, (Statement) ASTNode.copySubtree(targetBlock.getAST(), writer));
            } else {
                JavaLogger.error(_name + "#insertToBody Target statement not in a block " + content);
                return;
            }

        }
    }

    private static void insertToBodyTailWithReturnCheck(Block targetBlock, Statement writer) {
        Statement last = (Statement) targetBlock.statements().get(targetBlock.statements().size()-1);
        if (!(last instanceof ReturnStatement)) {
            targetBlock.statements().add(writer);
        }
        // TODO: also insert before return stmt in other position
    }

    public static ASTNode findNearBlock(Statement stmt, String message) {
        ASTNode block = null;
        ASTNode parent = stmt.getParent();
        if (parent == null) {
            JavaLogger.error(_name + "#findNearBlock Target statement no parent " + message +  " : " + Constant.CURRENT_METHOD);
        } else if (parent instanceof Block) {
            block = (Block) parent;
        } else if(parent instanceof SwitchStatement) {
            // when stmt in SwitchStmt not in a block, insert writer before stmt in SwitchStmt.statements
            //block = findNearBlock((SwitchStatement) parent, message);
            block = (SwitchStatement) parent;
        }
        return block;
    }

    public static int findIndexInBlock(Statement stmt, ASTNode block, String message) {
        int index = -1;
        if (block == null) {
            JavaLogger.error(_name + "#findIndexInBlock Target statement no parent block " + message +  " : " + Constant.CURRENT_METHOD);
        } else {
            ASTNode cur = stmt.getParent();
            if (cur == null) {
                JavaLogger.error(_name + "#findNearBlock Target statement no parent " + message +  " : " + Constant.CURRENT_METHOD);
            } else if ((cur instanceof Block) && (block instanceof Block) && cur == block) {
                index = ((Block) block).statements().indexOf(stmt);
            } else if((cur instanceof SwitchStatement) && (block instanceof SwitchStatement) && cur == block) {
                // when stmt in SwitchStmt not in a block, insert writer before stmt in SwitchStmt.statements
                //index = findIndexInBlock((SwitchStatement) cur, block, message);
                index = ((SwitchStatement) block).statements().indexOf(stmt);
            }
        }
        return index;
    }

    public static int getColNo(ASTNode node) {
        int colNo = -1;
        if(node != null) {
            ASTNode cu = node.getRoot();
            if(cu instanceof CompilationUnit) {
                colNo = ((CompilationUnit) cu).getColumnNumber(node.getStartPosition());
            }
        }
        if(colNo == -1) {
            if(GenPredicate.lineColSizeByExpNode.containsKey(node)) {
                colNo = GenPredicate.lineColSizeByExpNode.get(node)[1];
            }
        }
        return colNo;
    }

    public static int getLineNo(ASTNode node) {
        int lineNo = -1;
        if(node != null) {
            ASTNode cu = node.getRoot();
            if(cu instanceof CompilationUnit) {
                lineNo = ((CompilationUnit) cu).getLineNumber(node.getStartPosition());
            }
        }
        if(lineNo == -1) {
            if(GenPredicate.lineColSizeByExpNode.containsKey(node)) {
                lineNo = GenPredicate.lineColSizeByExpNode.get(node)[0];
            }
        }
        return lineNo;
    }

    public static int getExpSize(Expression node) {
        int size = -1;
        if(node != null) {
            ASTNode cu = node.getRoot();
            if(cu instanceof CompilationUnit) {
                size = node.getLength();
            }
        }
        if(size == -1) {
            if(GenPredicate.lineColSizeByExpNode.containsKey(node)) {
                size = GenPredicate.lineColSizeByExpNode.get(node)[2];
            }
        }
        return size;
    }
}
