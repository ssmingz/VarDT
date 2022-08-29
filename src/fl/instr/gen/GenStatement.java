package fl.instr.gen;

import fl.instr.visitor.BlockTransVisitor;
import fl.instr.visitor.MethodDeclVisitor;
import fl.utils.Constant;
import fl.utils.JavaLogger;
import org.eclipse.jdt.core.dom.*;

import java.util.*;

/**
 * @description: generate new statement node in the given compilation unit
 * @author:
 * @time: 2021/7/23 15:55
 */
public class GenStatement {
    private static final String _name = "@GenStatement ";
    private static AST _ast = AST.newAST(AST.JLS8);

    static MethodDeclaration _curMethodDec = null;

    /**
     * generate writers according to different Statement types
     * @param cu : target AST
     * @param stmtList : sliced Statement nodes
     */
    public static void generate(CompilationUnit cu, LinkedHashMap<Statement, Integer> stmtList, String projectPath) {
        GenWriter.writerInitImports(cu);
        Iterator itr = stmtList.keySet().iterator();
        Set<MethodDeclaration> mds = new HashSet<>();
        Map<Statement, MethodDeclaration> mdByStmt = new HashMap<>();
        while(itr.hasNext()) {
            ASTNode cur = (Statement) itr.next();
            Statement cur_stmt = (Statement) cur;
            while(!(cur instanceof MethodDeclaration) && !(cur instanceof CompilationUnit)) {
                cur = cur.getParent();
            }
            if(cur instanceof MethodDeclaration && !mds.contains(cur)) {
                mds.add((MethodDeclaration) cur);
            }
            if(cur instanceof MethodDeclaration) {
                mdByStmt.put(cur_stmt, (MethodDeclaration) cur);
            }
            if(cur instanceof CompilationUnit){
                JavaLogger.error(_name + "#generate Target method declaration cannot find");
            }
        }
        // fill the blocks without {}
        itr = mds.iterator();
        while(itr.hasNext()) {
            ASTNode cur = (MethodDeclaration) itr.next();
            // change single-line to block
            BlockTransVisitor btVisitor = new BlockTransVisitor(cur);
            btVisitor.traverse();
        }
        // instrumentation
        Constant.INSTRUMENT_COUNTER = 0;
        itr = stmtList.entrySet().iterator();
        while(itr.hasNext()) {
            Map.Entry entry = (Map.Entry) itr.next();
            Statement aStmt = (Statement) entry.getKey();
            if(Constant.nodeBuffer.containsKey(aStmt)) {
                aStmt = (Statement) Constant.nodeBuffer.get(aStmt);
            }
            _curMethodDec = mdByStmt.get(aStmt);
            int lineNo = (int) entry.getValue();
            if(aStmt instanceof IfStatement) {
                // IfStatement --> operate on its expression
                extractExpressions(cu, (IfStatement) aStmt, lineNo);
            } else if(aStmt instanceof ForStatement) {
                extractExpressions(cu, (ForStatement) aStmt, lineNo);
            } else if(aStmt instanceof EnhancedForStatement) {
                extractExpressions(cu, (EnhancedForStatement) aStmt, lineNo);
            } else if(aStmt instanceof WhileStatement) {
                extractExpressions(cu, (WhileStatement) aStmt, lineNo);
            } else if(aStmt instanceof DoStatement) {
                extractExpressions(cu, (DoStatement) aStmt, lineNo);
            } else if(aStmt instanceof TryStatement) {
                // TODO: consider after encountering examples
            } else if(aStmt instanceof SwitchStatement) {
                extractExpressions(cu, (SwitchStatement) aStmt, lineNo);
            } else if(aStmt instanceof SwitchCase) {
                extractExpressions(cu, (SwitchCase) aStmt, lineNo);
            } else if(aStmt instanceof SynchronizedStatement) {
                extractExpressions(cu, (SynchronizedStatement) aStmt, lineNo);
            } else if(aStmt instanceof ReturnStatement) {
                extractExpressions(cu, (ReturnStatement) aStmt, lineNo);
            } else if(aStmt instanceof ThrowStatement) {
                extractExpressions(cu, (ThrowStatement) aStmt, lineNo);
            } else if(aStmt instanceof ExpressionStatement) {
                extractExpressions(cu, (ExpressionStatement) aStmt, lineNo);
            } else if(aStmt instanceof LabeledStatement) {
                extractExpressions(cu, (LabeledStatement) aStmt, lineNo);
            } else if(aStmt instanceof AssertStatement) {
                extractExpressions(cu, (AssertStatement) aStmt, lineNo);
            } else if(aStmt instanceof ConstructorInvocation) {
                extractExpressions(cu, (ConstructorInvocation) aStmt, lineNo);
            } else if(aStmt instanceof SuperConstructorInvocation) {
                extractExpressions(cu, (SuperConstructorInvocation) aStmt, lineNo);
            } else if(aStmt instanceof VariableDeclarationStatement) {
                extractExpressions(cu, (VariableDeclarationStatement) aStmt, lineNo);
            }
            // skip for Block, BreakStmt, ContinueStmt, EmptyStmt,
            // VariableDeclarationStmt(see as VDExp in ExpStmt), TypeDeclarationStmt
        }
        // writer init
        itr = mds.iterator();
        while(itr.hasNext()) {
            ASTNode cur = (MethodDeclaration) itr.next();
            GenWriter.writerInit(cu, (MethodDeclaration) cur, projectPath);
        }
    }

    /**
     * generate writers for test-number-filter checking verison
     * @param cu : target AST
     */
    public static void generateForFilterVersion(CompilationUnit cu, String projectPath) {
        GenWriter.writerInitImports(cu);
        // get target MethodDeclaration
        MethodDeclVisitor mdvisitor = new MethodDeclVisitor(cu, Constant.CURRENT_METHOD);
        MethodDeclaration md = mdvisitor.traverse();
        if(md == null) {
            JavaLogger.error(_name + "#generateForFilterVersion Target MethodDeclaration not find : " + Constant.CURRENT_METHOD);
            return;
        }
        // writer init
        GenWriter.writerInitForFilterVersion(cu, md, projectPath);
    }

    private static void extractExpressions(CompilationUnit cu, VariableDeclarationStatement stmt, int lineNo) {
        for(Object frag : stmt.fragments()) {
            if(((VariableDeclarationFragment) frag).getInitializer() != null) {
                Expression right = ((VariableDeclarationFragment) frag).getInitializer();
                SimpleName left = ((VariableDeclarationFragment) frag).getName();
                generateStmt(right, stmt, lineNo, 0);
                generateStmt(left, stmt, lineNo, 1);
            }
        }
    }

    /**
     * extract expressions required to be recorded
     * @param cu
     * @param stmt
     * @param lineNo
     */
    protected static void extractExpressions(CompilationUnit cu, IfStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, ForStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 1);
    }

    protected static void extractExpressions(CompilationUnit cu, EnhancedForStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, WhileStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, DoStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 1);
    }

    protected static void extractExpressions(CompilationUnit cu, SwitchStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, SwitchCase stmt, int lineNo) {
        if(!stmt.isDefault()) {
            generateStmt(stmt.getExpression(), stmt, lineNo, 0);
        }
    }

    protected static void extractExpressions(CompilationUnit cu, SynchronizedStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, ReturnStatement stmt, int lineNo) {
        GenPredicate.genReturnValue(stmt, lineNo);
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, ThrowStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, ExpressionStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, LabeledStatement stmt, int lineNo) {
        generateStmt(stmt.getLabel(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, AssertStatement stmt, int lineNo) {
        generateStmt(stmt.getExpression(), stmt, lineNo, 0);
    }

    protected static void extractExpressions(CompilationUnit cu, ConstructorInvocation stmt, int lineNo) {
        for(Object arg : stmt.arguments()) {
            GenPredicate.genArgumentPred((Expression) arg, stmt, stmt, stmt.arguments().indexOf(arg));
            generateStmt((Expression) arg, stmt, lineNo, 0);
        }
        ASTNode cur = stmt;
        while(!(cur instanceof MethodDeclaration)) {
            cur = cur.getParent();
        }
        if(cur instanceof MethodDeclaration) {
            if(((MethodDeclaration) cur).isConstructor()) {
                stmt.delete();
                ((MethodDeclaration) cur).getBody().statements().add(0, stmt);
            }
        }
    }

    protected static void extractExpressions(CompilationUnit cu, SuperConstructorInvocation stmt, int lineNo) {
        for(Object arg : stmt.arguments()) {
            GenPredicate.genArgumentPred((Expression) arg, stmt, stmt, stmt.arguments().indexOf(arg));
            generateStmt((Expression) arg, stmt, lineNo, 0);
        }
        ASTNode cur = stmt;
        while(!(cur instanceof MethodDeclaration)) {
            cur = cur.getParent();
        }
        if(cur instanceof MethodDeclaration) {
            if(((MethodDeclaration) cur).isConstructor()) {
                stmt.delete();
                ((MethodDeclaration) cur).getBody().statements().add(0, stmt);
            }
        }
    }

    /**
     * generate statement for the given expression
     * @param exp
     * @param stmt
     * @param lineNo
     * @param genAfter : indicating whether insert after the given expression
     */
    protected static void generateStmt(Expression exp, Statement stmt, int lineNo, int genAfter) {
        String content = "";
        // check for boolean value of the given expression
        if(exp != null) {
            GenPredicate.genBooleanPred(exp, stmt, _curMethodDec);
        }
        if (exp instanceof SuperFieldAccess) {
            content = "super." + ((SuperFieldAccess) exp).getName().getIdentifier();
            GenWriter.write(content, exp, stmt, genAfter);
        } else if (exp instanceof FieldAccess) {
            content = ((FieldAccess) exp).getExpression().toString() + "." + ((FieldAccess) exp).getName().getIdentifier();
            GenWriter.write(content, exp, stmt, genAfter);
        } else if (exp instanceof Assignment) {
            generateStmt(((Assignment) exp).getLeftHandSide(), stmt, lineNo, 1);
            generateStmt(((Assignment) exp).getRightHandSide(), stmt, lineNo, genAfter);
        } else if (exp instanceof ParenthesizedExpression) {
            generateStmt(((ParenthesizedExpression) exp).getExpression(), stmt, lineNo, genAfter);
        } else if (exp instanceof PrefixExpression) {
            generateStmt(((PrefixExpression) exp).getOperand(), stmt, lineNo, genAfter);
            // note that -1 is also PrefixExpression, its operand is NumberLiteral
        } else if (exp instanceof PostfixExpression) {
            generateStmt(((PostfixExpression) exp).getOperand(), stmt, lineNo, genAfter);
        } else if (exp instanceof InfixExpression) {
            generateStmt(((InfixExpression) exp).getLeftOperand(), stmt, lineNo, genAfter);
            generateStmt(((InfixExpression) exp).getRightOperand(), stmt, lineNo, genAfter);
            for(int i=0; i<((InfixExpression) exp).extendedOperands().size(); i++) {
                generateStmt((Expression) ((InfixExpression) exp).extendedOperands().get(i), stmt, lineNo, genAfter);
            }
        } else if (exp instanceof ClassInstanceCreation) {
            // creation paras
            for(Object arg : ((ClassInstanceCreation) exp).arguments()) {
                generateStmt((Expression) arg, stmt, lineNo, genAfter);
            }
        } else if (exp instanceof ArrayCreation) {
            // creation dimensions
            for(Object dim : ((ArrayCreation) exp).dimensions()) {
                generateStmt((Expression) dim, stmt, lineNo, genAfter);
            }
        } else if (exp instanceof ArrayInitializer) {
            // e.g. int guests[] = {1, 4, 2}
            for(Object ele : ((ArrayInitializer) exp).expressions()) {
                generateStmt((Expression) ele, stmt, lineNo, genAfter);
            }
        } else if (exp instanceof MethodInvocation) {
            generateStmt(((MethodInvocation) exp).getExpression(), stmt, lineNo, genAfter);
            for(Object arg : ((MethodInvocation) exp).arguments()) {
                GenPredicate.genArgumentPred((Expression) arg, stmt, exp, ((MethodInvocation) exp).arguments().indexOf(arg));
                generateStmt((Expression) arg, stmt, lineNo, genAfter);
            }
        } else if (exp instanceof SuperMethodInvocation) {
            for(Object arg : ((SuperMethodInvocation) exp).arguments()) {
                GenPredicate.genArgumentPred((Expression) arg, stmt, exp, ((SuperMethodInvocation) exp).arguments().indexOf(arg));
                generateStmt((Expression) arg, stmt, lineNo, genAfter);
            }
        } else if (exp instanceof ArrayAccess) {
            generateStmt(((ArrayAccess) exp).getIndex(), stmt, lineNo, 0);
            generateStmt(((ArrayAccess) exp).getArray(), stmt, lineNo, genAfter);
        } else if (exp instanceof InfixExpression) {
            generateStmt(((InfixExpression) exp).getLeftOperand(), stmt, lineNo, genAfter);
            generateStmt(((InfixExpression) exp).getRightOperand(), stmt, lineNo, genAfter);
            if(((InfixExpression) exp).hasExtendedOperands()) {
                for(Object extendOpr : ((InfixExpression) exp).extendedOperands()) {
                    generateStmt((Expression) extendOpr, stmt, lineNo, genAfter);
                }
            }
        } else if (exp instanceof InstanceofExpression) {
            generateStmt(((InstanceofExpression) exp).getLeftOperand(), stmt, lineNo, genAfter);
        } else if (exp instanceof ConditionalExpression) {
            // e1 ? e2 : e3
            generateStmt(((ConditionalExpression) exp).getExpression(), stmt, lineNo, genAfter);
            generateStmt(((ConditionalExpression) exp).getThenExpression(), stmt, lineNo, genAfter);
            generateStmt(((ConditionalExpression) exp).getElseExpression(), stmt, lineNo, genAfter);
        } else if (exp instanceof CastExpression) {
            // ( Type ) exp
            generateStmt(((CastExpression) exp).getExpression(), stmt, lineNo, genAfter);
        } else if (exp instanceof VariableDeclarationExpression) {
            // just declaration, so skip
            for(Object frag : ((VariableDeclarationExpression) exp).fragments()) {
                if(((VariableDeclarationFragment) frag).getInitializer() != null) {
                    generateStmt(((VariableDeclarationFragment) frag).getInitializer(), stmt, lineNo, genAfter);
                    generateStmt(((VariableDeclarationFragment) frag).getName(), stmt, lineNo, 1);
                }
            }
        } else if (exp instanceof ThisExpression) {
            // TODO: consider after encountering examples
            JavaLogger.warn(_name + "#generateStmt Skip creation for ThisExpression in current implementation L" + lineNo);
        } else if (exp instanceof Name) {
            LinkedHashMap<String, Expression> contents = GenPredicate.genPred((Name) exp);
            Iterator itr = contents.entrySet().iterator();
            while(itr.hasNext()) {
                Map.Entry<String, Expression> entry = (Map.Entry<String, Expression>) itr.next();
                // insert logger.info(content+exp)
                GenWriter.write(entry.getKey(), entry.getValue(), stmt, genAfter);
            }
        }
        // check for boolean value of the given expression
        if(exp != null) {
            //GenPredicate.genBooleanPred(exp, stmt, lineNo, _curMethodDec);
            //GenPredicate.genArgumentPred(exp, stmt, lineNo, _curMethodDec);
            //GenPredicate.genLength(exp, stmt, lineNo, genAfter);
            //GenPredicate.genType(exp, stmt, lineNo, genAfter);
            //GenPredicate.genElementValue(exp, stmt, lineNo, genAfter);
            //GenPredicate.genASCIIForChar(exp, stmt, lineNo, genAfter);
        }
    }

    /**
     * generate try/catch for test methods to record PASS/FAIL results and test name
     * @param methods
     */
    public static void genForTestMethod(Set<MethodDeclaration> methods) {
        Iterator itr = methods.iterator();
        while(itr.hasNext()) {
            MethodDeclaration md = (MethodDeclaration) itr.next();
            // change single-line to block
            BlockTransVisitor btVistor = new BlockTransVisitor(md);
            btVistor.traverse();
            // check if already instrument
            boolean alreadyInstr = false;
            for(Object s : md.getBody().statements()) {
                if(((Statement) s).toString().startsWith("auxiliary.InstrAux.setTestName")) {
                    alreadyInstr = true;
                    break;
                }
            }
            if(alreadyInstr) {
                continue;
            }
            // record current test name: logger.debug(Thread.currentThread().getStackTrace()[1].getMethodName())
            MethodInvocation getMethodName = _ast.newMethodInvocation();
            getMethodName.setName(_ast.newSimpleName("getMethodName"));
            MethodInvocation getStackTrace = _ast.newMethodInvocation();
            getStackTrace.setName(_ast.newSimpleName("getStackTrace"));
            MethodInvocation currentThread = _ast.newMethodInvocation();
            currentThread.setName(_ast.newSimpleName("currentThread"));
            currentThread.setExpression(_ast.newSimpleName("Thread"));
            getStackTrace.setExpression(currentThread);
            ArrayAccess aa = _ast.newArrayAccess();
            aa.setIndex(_ast.newNumberLiteral("1"));
            aa.setArray(getStackTrace);
            getMethodName.setExpression(aa);

            // TODO: set InstrAux.TEST_NAME
            MethodInvocation init = _ast.newMethodInvocation();
            init.setName(_ast.newSimpleName("setTestName"));
            init.setExpression(_ast.newName("auxiliary.InstrAux"));
            StringLiteral ftestpath = _ast.newStringLiteral();
            ftestpath.setLiteralValue(Constant.FAIL_TESTS);
            StringLiteral pid = _ast.newStringLiteral();
            pid.setLiteralValue(Constant.PROJECT_ID);
            StringLiteral bid = _ast.newStringLiteral();
            bid.setLiteralValue(Constant.BUG_ID);
            init.arguments().add(ftestpath);
            init.arguments().add(getMethodName);
            init.arguments().add(pid);
            init.arguments().add(bid);
            init.arguments().add(_ast.newSimpleName("logger_log4j"));
            ExpressionStatement recordTestName = _ast.newExpressionStatement(init);

            TryStatement tryStmt = _ast.newTryStatement();
            tryStmt.setBody((Block) ASTNode.copySubtree(_ast, md.getBody()));
            int trySize= tryStmt.getBody().statements().size();
            //if(trySize == 0) {
            //    // do not modify if this method is empty
            //    continue;
            //}
            // new method body
            Block newBody = _ast.newBlock();
            newBody.statements().add(recordTestName);

            // TODO: add finally block to write all values to file
            Block finalBlock = _ast.newBlock();
            MethodInvocation writer = _ast.newMethodInvocation();
            writer.setExpression(_ast.newName("auxiliary.InstrAux"));
            writer.setName(_ast.newSimpleName("write"));
            writer.arguments().add(_ast.newSimpleName("logger_log4j"));
            ExpressionStatement writer_stmt = _ast.newExpressionStatement(writer);
            finalBlock.statements().add(writer_stmt);
            tryStmt.setFinally(finalBlock);

            newBody.statements().add(tryStmt);
            // insert to target test method
            md.setBody((Block) ASTNode.copySubtree(md.getAST(), newBody));
        }
    }

    public static void genForTestMethod_filterVersion(HashSet<MethodDeclaration> methods) {
        Iterator itr = methods.iterator();
        while(itr.hasNext()) {
            MethodDeclaration md = (MethodDeclaration) itr.next();
            // change single-line to block
            BlockTransVisitor btVistor = new BlockTransVisitor(md);
            btVistor.traverse();
            // record current test name: logger.debug(Thread.currentThread().getStackTrace()[1].getMethodName())
            MethodInvocation getMethodName = _ast.newMethodInvocation();
            getMethodName.setName(_ast.newSimpleName("getMethodName"));
            MethodInvocation getStackTrace = _ast.newMethodInvocation();
            getStackTrace.setName(_ast.newSimpleName("getStackTrace"));
            MethodInvocation currentThread = _ast.newMethodInvocation();
            currentThread.setName(_ast.newSimpleName("currentThread"));
            currentThread.setExpression(_ast.newSimpleName("Thread"));
            getStackTrace.setExpression(currentThread);
            ArrayAccess aa = _ast.newArrayAccess();
            aa.setIndex(_ast.newNumberLiteral("1"));
            aa.setArray(getStackTrace);
            getMethodName.setExpression(aa);

            // TODO: set InstrAux.TEST_NAME
            MethodInvocation init = _ast.newMethodInvocation();
            init.setName(_ast.newSimpleName("setTestName"));
            init.setExpression(_ast.newName("auxiliary.InstrAux"));
            StringLiteral ftestpath = _ast.newStringLiteral();
            ftestpath.setLiteralValue(Constant.FAIL_TESTS);
            StringLiteral pid = _ast.newStringLiteral();
            pid.setLiteralValue(Constant.PROJECT_ID);
            StringLiteral bid = _ast.newStringLiteral();
            bid.setLiteralValue(Constant.BUG_ID);
            init.arguments().add(ftestpath);
            init.arguments().add(getMethodName);
            init.arguments().add(pid);
            init.arguments().add(bid);
            init.arguments().add(_ast.newSimpleName("logger_log4j"));
            ExpressionStatement recordTestName = _ast.newExpressionStatement(init);

            TryStatement tryStmt = _ast.newTryStatement();
            tryStmt.setBody((Block) ASTNode.copySubtree(_ast, md.getBody()));
            int trySize= tryStmt.getBody().statements().size();
            if(trySize == 0) {
                // do not modify if this method is empty
                continue;
            }
            // new method body
            Block newBody = _ast.newBlock();
            newBody.statements().add(recordTestName);

            // TODO: add finally block to write all values to file
            Block finalBlock = _ast.newBlock();
            MethodInvocation writer = _ast.newMethodInvocation();
            writer.setExpression(_ast.newName("auxiliary.InstrAux"));
            writer.setName(_ast.newSimpleName("write_filterTest"));
            writer.arguments().add(_ast.newSimpleName("logger_log4j"));
            ExpressionStatement writer_stmt = _ast.newExpressionStatement(writer);
            finalBlock.statements().add(writer_stmt);
            tryStmt.setFinally(finalBlock);

            newBody.statements().add(tryStmt);
            // insert to target test method
            md.setBody((Block) ASTNode.copySubtree(md.getAST(), newBody));
        }
    }

    public static void genForSuperClazzTestMethod(Set<MethodDeclaration> methods) {
        Iterator itr = methods.iterator();
        while(itr.hasNext()) {
            MethodDeclaration md = (MethodDeclaration) itr.next();
            // change single-line to block
            BlockTransVisitor btVistor = new BlockTransVisitor(md);
            btVistor.traverse();
            // add "throws Throwable"
            boolean alreadyExist = false;
            for(Object thrown : md.thrownExceptionTypes()) {
                if(thrown.toString().equals("Exception")) {
                    md.thrownExceptionTypes().remove(thrown);
                    break;
                }
                if(thrown.toString().equals("Throwable")) {
                    alreadyExist = true;
                    break;
                }
            }
            if(!alreadyExist){
                md.thrownExceptionTypes().add(md.getAST().newSimpleType(md.getAST().newSimpleName("Throwable")));
            }
        }
    }
}
