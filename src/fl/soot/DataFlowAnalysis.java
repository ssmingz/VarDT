package fl.soot;

import java.lang.reflect.Field;
import java.util.*;

import fl.instr.visitor.LocalNameVisitor;
import fl.instr.visitor.MethodDecVisitor;
import fl.utils.Constant;
import fl.utils.DataFlowParaPack;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import soot.*;
import soot.Timer;
import soot.jimple.FieldRef;
import soot.jimple.ParameterRef;
import soot.jimple.internal.JAssignStmt;
import soot.jimple.internal.JIdentityStmt;
import soot.toolkits.graph.ExceptionalUnitGraph;
import soot.toolkits.graph.UnitGraph;
import soot.toolkits.scalar.SimpleLocalDefs;

/**
 * @description: apply data flow analysis (e.g. reaching definition) to the target buggy method
 * notice that this analysis must be applied before instrumentation
 * @author:
 * @time: 2021/8/20 21:38
 */
public class DataFlowAnalysis {
    private String _name= "@DataFlowAnalysis ";

    private String _classpath;
    private String _process_dir;
    private String _className;
    private String _methodName;
    private List<String> _methodParaTypes;
    private CompilationUnit _cu;
    private MethodDeclaration _methodNode;

    public Map<String, Map<String, Set<String>>> _defUse;
    public Map<Set<String>, Set<String>> _aggreEquiLocals;
    public Map<String, String> _localNameMap = new HashMap<>();

    public DataFlowAnalysis(String classpath, String processdir, String className, String methodName, List<String> paras, CompilationUnit cu) {
        _classpath = classpath;
        _process_dir = processdir;
        _className = className;
        _methodName = methodName;
        _methodParaTypes = new ArrayList<>();
        // add paras, notice ""
        for(String p : paras) {
            if(!p.equals("")) {
                _methodParaTypes.add(p);
            }
        }
        _cu = cu;
        MethodDecVisitor mdVisitor = new MethodDecVisitor(_cu, _methodName, _methodParaTypes);
        _methodNode = mdVisitor.traverse();
    }

    public void doAnalysis(){
        //String classpath = "E:/java/FaultLocalization/outputs/Lang/Lang_1_buggy/target/classes/";
        //String dir = "E:/java/FaultLocalization/outputs/Lang/Lang_1_buggy/target/classes/";
        //String className = "org.apache.commons.lang3.math.NumberUtils";
        //String methodName = "createNumber";
        //List<String> paras = new ArrayList<>();
        //paras.add("java.lang.String");
        run(_classpath, _process_dir, _className, _methodName, _methodParaTypes);
    }

    public void run(String classpath, String dir, String className, final String methodName, final List<String> paras){
        Transform t1 = new Transform("jtp.Printer", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String string, Map map) {
                SootMethod method = body.getMethod();
                if(!checkMethod(method, methodName, paras)){
                    return;
                }
                UnitGraph g = new ExceptionalUnitGraph(body);
                Map<String, Set<String>> equiLocals = new HashMap<>();
                _defUse = fromLineToDefUseAndRD(g, equiLocals);
                _defUse = sortDefUseByLine(_defUse);
                outputDefUse(_defUse);
                //outputEquiLocals(equiLocals);
                _aggreEquiLocals = aggregateEquiLocals(equiLocals);
                _aggreEquiLocals = sortEquiLocalsByLine(_aggreEquiLocals);
                outputAggreEquiLocals(_aggreEquiLocals);
            }

            private boolean checkMethod(SootMethod method, String methodName, List<String> paras) {
                // check class, notice outer/inner
                String clazz = method.getDeclaringClass().getName();
                clazz = clazz.replaceAll("\\$", ".");
                clazz = clazz.replaceAll("#", ".");
                if(!clazz.equals(_className)) { return false; }
                if(!method.getName().equals(methodName)) { return false; }
                List<String> parasToCheck = new ArrayList<>();
                for(Object type : method.getParameterTypes()) {
                    parasToCheck.add(type.toString());
                }
                if(parasToCheck.size() != paras.size()) { return false; }
                for(int i=0; i<paras.size(); i++) {
                    if(!parasToCheck.get(i).equals(paras.get(i))) {
                        return false;
                    }
                }
                return true;
            }
        });
        PackManager.v().getPack("jtp").add(t1);

        //if(G.v().Timer_outstandingTimers.contains(Timers.v().totalTimer)) {
        //    Timers.v().totalTimer.end();
        //}
        soot.Main.main(new String[]{
                "-cp", classpath,
                "-process-dir", dir,
                "-main-class", className,
                "-pp", "-allow-phantom-refs",
                "--keep-line-number",
                "-p", "jb", "use-original-names:true"
        });
    }

    private Map<Set<String>, Set<String>> sortEquiLocalsByLine(Map<Set<String>, Set<String>> aggreEquiLocals) {
        Map<Set<String>, Set<String>> result = new HashMap<>();
        Iterator it = aggreEquiLocals.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Set<String> key_new = new LinkedHashSet<>();
            Set<String> value_new = new LinkedHashSet<>();
            List<String> sortedKey = new ArrayList<>((Set<String>) entry.getKey());
            List<String> sortedValue = new ArrayList<>((Set<String>) entry.getValue());
            Collections.sort(sortedKey);
            Collections.sort(sortedValue);
            key_new.addAll(sortedKey);
            value_new.addAll(sortedValue);
            result.put(key_new, value_new);
        }
        return result;
    }

    private Map<String, Map<String, Set<String>>> sortDefUseByLine(Map<String, Map<String, Set<String>>> defUse) {
        Map<String, Map<String, Set<String>>> result = new LinkedHashMap<>();
        List<String> sortedKey = new ArrayList<>(defUse.keySet());
        Collections.sort(sortedKey);
        for(int i=0; i<sortedKey.size(); i++) {
            String key = sortedKey.get(i);
            result.put(key, defUse.get(key));
        }
        return result;
    }

    private void outputAggreEquiLocals(Map<Set<String>, Set<String>> aggreEquiLocals) {
        System.out.println("---------- Output aggregated equivalent locals: ----------");
        Iterator it = aggreEquiLocals.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            Set<String> reachingdefSet = (Set<String>) entry.getKey();
            Set<String> aggreEquiSet = (Set<String>) entry.getValue();
            Set<String> tmp = new LinkedHashSet<>();
            tmp.addAll(reachingdefSet);
            tmp.addAll(aggreEquiSet);
            String content = "";
            for (String local : tmp) {
                content += local + " ";
            }
            content = content.trim();
            System.out.println(content);
        }
    }

    private Map<Set<String>, Set<String>> aggregateEquiLocals(Map<String, Set<String>> equiLocals) {
        Iterator it = equiLocals.entrySet().iterator();
        // key: reaching definitions, value: equivalent locals
        Map<Set<String>, Set<String>> aggregatedEquiLocals = new HashMap<>();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String cur_local = (String) entry.getKey();
            Set<String> cur_reachingDefs = (Set<String>) entry.getValue();
            Iterator aggre_it = aggregatedEquiLocals.entrySet().iterator();
            boolean exist = false;
            while(aggre_it.hasNext()) {
                Map.Entry aggre_entry = (Map.Entry) aggre_it.next();
                Set<String> aggre_reachingDefs = (Set<String>) aggre_entry.getKey();
                if(aggre_reachingDefs.size() != cur_reachingDefs.size()) {
                    continue;
                }
                boolean isSame = true;
                for(String reachingDef : cur_reachingDefs) {
                    if(!aggre_reachingDefs.contains(reachingDef)) {
                        isSame = false;
                        break;
                    }
                }
                if(isSame) {
                    exist =true;
                    aggregatedEquiLocals.get(cur_reachingDefs).add(cur_local);
                    break;
                }
            }
            if(!exist) {
                Set<String> new_aggreEquiLocals = new HashSet<>();
                new_aggreEquiLocals.add(cur_local);
                aggregatedEquiLocals.put(cur_reachingDefs, new_aggreEquiLocals);
            }
        }
        return aggregatedEquiLocals;
    }

    private void outputEquiLocals(Map<String, Set<String>> equiLocals) {
        Iterator it = equiLocals.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String local = (String) entry.getKey();
            Set<String> equiSets = (Set<String>) entry.getValue();
            String equis = "";
            for(String equi : equiSets) {
                equis += " " + equi;
            }
            System.out.println(local + ":" + equis);
        }
    }

    private void outputDefUse(Map<String, Map<String, Set<String>>> defUse) {
        System.out.println("---------- Output defUseByLine: ----------");
        Iterator it = defUse.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String lineNo = (String) entry.getKey();
            Set<String> defSet = ((Map<String, Set<String>>) entry.getValue()).get("def");
            Set<String> useSet = ((Map<String, Set<String>>) entry.getValue()).get("use");
            // output def
            String defs = lineNo + "-def:";
            for(String d : defSet) {
                defs += " " + d;
            }
            // output use
            String uses = lineNo + "-use:";
            for(String u : useSet) {
                uses += " " + u;
            }
            System.out.println(defs);
            System.out.println(uses);
        }
    }

    private Map<String, Map<String, Set<String>>> fromLineToDefUseAndRD(UnitGraph g, Map<String, Set<String>> equiLocals) {
        Map<String, Map<String, Set<String>>> result = new HashMap<>();
        SimpleLocalDefs sld = new SimpleLocalDefs(g);
        Iterator it = g.iterator();
        while(it.hasNext()) {
            Unit u = (Unit) it.next();
            // compute key
            if(!u.hasTag("LineNumberTag") && !(u instanceof JIdentityStmt) && !(u instanceof JAssignStmt)) {
                continue;
            }
            // get -xx
            //String lineNo = u.getTag("LineNumberTag").toString();
            String lineNo = "";
            if(u.hasTag("LineNumberTag")) {
                lineNo = u.getTag("LineNumberTag").toString();
            } else if(u instanceof JIdentityStmt) {
                if(((JIdentityStmt) u).getRightOp() instanceof ParameterRef) {
                    lineNo = "para";
                } else {
                    continue;
                }
            } else if(u instanceof JAssignStmt) {
                if(((JAssignStmt) u).getRightOp() instanceof FieldRef) {
                    lineNo = "field";
                } else {
                    continue;
                }
            }
            // compute value
            List<ValueBox> defBoxes = u.getDefBoxes(), useBoxes = u.getUseBoxes();
            // traverse value name
            for(ValueBox v : defBoxes) {
                String name = v.getValue().toString();
                if(!checkName(name)) {
                    continue;
                }
                LocalNameVisitor nameVisitor = new LocalNameVisitor(_cu, _methodNode, lineNo);
                String original_name = nameVisitor.traverse();
                if(original_name == null) {
                    original_name = name;
                }
                _localNameMap.put(name, original_name);
                if(!name.equals(original_name)) {
                    name = original_name;
                }
                if(result.containsKey(lineNo)) {
                    result.get(lineNo).get("def").add(name);
                } else {
                    Map<String, Set<String>> defUseInit = new HashMap<>();
                    Set<String> defInit = new HashSet<>(), useInit = new HashSet<>();
                    defInit.add(name);
                    defUseInit.put("def", defInit);
                    defUseInit.put("use", useInit);
                    result.put(lineNo, defUseInit);
                }

            }
            for(ValueBox v : useBoxes) {
                String name = "";
                if (v.getValue() instanceof FieldRef) {
                    name = ((FieldRef) v.getValue()).getField().getName();
                    if(!checkName(name)) {
                        continue;
                    }
                    if(_localNameMap.containsKey(name)) {
                        name = _localNameMap.get(name);
                    }
                    // make up defUse
                    if(result.containsKey(lineNo)) {
                        result.get(lineNo).get("use").add(name);
                    } else {
                        Map<String, Set<String>> defUseInit = new HashMap<>();
                        Set<String> defInit = new HashSet<>(), useInit = new HashSet<>();
                        useInit.add(name);
                        defUseInit.put("use", useInit);
                        defUseInit.put("def", defInit);
                        result.put(lineNo, defUseInit);
                    }
                    // make up reachingDefs
                    Set<String> defSet = new HashSet<>();
                    defSet.add(name + "-" + "field");
                    String v_key = name + "-" + lineNo;
                    if(equiLocals.containsKey(v_key)) {
                        equiLocals.get(v_key).addAll(defSet);
                    } else {
                        equiLocals.put(v_key, defSet);
                    }
                }
                else if(v.getValue() instanceof Local) {
                    name = ((Local) v.getValue()).getName();
                    if(!checkName(name)) {
                        continue;
                    }
                    if(_localNameMap.containsKey(name)) {
                        name = _localNameMap.get(name);
                    }
                    // make up defUse
                    if(result.containsKey(lineNo)) {
                        result.get(lineNo).get("use").add(name);
                    } else {
                        Map<String, Set<String>> defUseInit = new HashMap<>();
                        Set<String> defInit = new HashSet<>(), useInit = new HashSet<>();
                        useInit.add(name);
                        defUseInit.put("use", useInit);
                        defUseInit.put("def", defInit);
                        result.put(lineNo, defUseInit);
                    }
                    // make up reachingDefs
                    List<Unit> v_defs = sld.getDefsOfAt((Local) v.getValue(), u);
                    Set<String> defSet = new HashSet<>();
                    for(Unit v_def : v_defs) {
                        if(!v_def.hasTag("LineNumberTag") && !(v_def instanceof JIdentityStmt) && !(v_def instanceof JAssignStmt)) {
                            continue;
                        }
                        String v_def_line = "";
                        if(v_def.hasTag("LineNumberTag")) {
                            v_def_line = v_def.getTag("LineNumberTag").toString();
                        } else if(v_def instanceof JIdentityStmt) {
                            if(((JIdentityStmt) v_def).getRightOp() instanceof ParameterRef) {
                                v_def_line = "para";
                            } else {
                                continue;
                            }
                        } else if(v_def instanceof JAssignStmt) {
                            if(((JAssignStmt) v_def).getRightOp() instanceof FieldRef) {
                                v_def_line = "field";
                            } else {
                                continue;
                            }
                        }
                        defSet.add(name + "-" + v_def_line);
                    }
                    String v_key = name + "-" + lineNo;
                    if(equiLocals.containsKey(v_key)) {
                        equiLocals.get(v_key).addAll(defSet);
                    } else {
                        equiLocals.put(v_key, defSet);
                    }
                }
            }
        }
        return result;
    }

    private boolean checkName(String name) {
        if(name.contains(" ")) { return false; }
        if(name.startsWith("$")) { return false; }
        if(!Character.isJavaIdentifierStart(name.charAt(0))) { return false; }
        if(isKeyword(name)) { return false; }
        return true;
    }

    private boolean isKeyword(String name) {
        if(Constant.KEYWORDS.contains(name)) { return true; }
        return false;
    }

}
