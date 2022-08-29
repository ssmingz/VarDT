package fl.weka;

import fl.utils.Constant;
import fl.utils.JavaFile;
import fl.utils.JavaLogger;
import org.apache.commons.lang.StringUtils;
import pda.common.java.D4jSubject;
import pda.core.dependency.Config;
import pda.core.dependency.DependencyParser;
import pda.core.dependency.dependencyGraph.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;


/**
 * @description: std.log --> values.csv and attrMap --> tree
 * @author:
 * @time: 2021/8/4 11:46
 */
public class WithoutGenTree {
    private static String _name = "@WithoutGenTree ";
    private static String PID, BID, MID;
    private static String CLAZZ = null;
    private static String METHOD = null;
    private static String SRC_BASE, TRACE_DIR;

    private static String LINESCORE_BASE;
    private static Map<String, String> methodByMID;

    private static double DEPENDENCY_FACTOR = 0.8;
    //private static double DEPENDENCY_FACTOR = 0.2;

    private static boolean SLICING = true;

    private static DependencyParser dependencyParser;

    public static void main(String[] args) {
        if(args.length > 9) {
            JavaLogger.error(_name + "#main Wrong number of arguments for fl-gentree.jar");
            return;
        }

        String[] pros = {"lang"};

        for(String pro : pros) {
            int[] bugs3 = {32};
            for(int j : bugs3) {
                String values = String.format("",pro,pro,j);
                File valuesDir = new File(values);
                int ms = 0;
                for(File f : valuesDir.listFiles()) {
                    if(f.isDirectory()) {
                        ms++;
                    }
                }
                dependencyParser = null;
                for(int k=0; k<ms; k++) {
                    String log_path = String.format("",pro,pro,j,k);
                    String output_path = String.format("", pro,pro,j,k);
                    if (SLICING) {
                        log_path = String.format("",pro,pro,j,k);
                        output_path = String.format("", pro,pro,j,k);
                    }
                    File output = new File(output_path);
                    if(output.exists()) {
                        continue;
                    }
                    File treesDir = new File(output_path);
                    if(!treesDir.exists()) {
                        treesDir.mkdirs();
                    }
                    PID = pro;
                    BID = ""+j;
                    MID = ""+k;
                    SRC_BASE = String.format("");
                    TRACE_DIR = String.format("",pro,pro,j);
                    LINESCORE_BASE = String.format("",pro,pro,j);
                    if(pro.equals("lang")||pro.equals("chart")||pro.equals("math")||pro.equals("time")||pro.equals("closure")) {
                        LINESCORE_BASE = String.format("",pro,pro,j);
                    }
                    methodByMID = loadMID(String.format("",pro,pro,j));
                    parseClazzMethod(String.format("",pro,pro,j));

                    log2tree(log_path, output_path);
                }
            }
        }
    }


    private static Map<String, String> loadMID(String format) {
        Map<String, String> result = new LinkedHashMap<>();
        try{
            File file = new File(format);
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String l;
            while((l = reader.readLine()) != null) {
                l = l.trim();
                if(l.contains(":")) {
                    String mid = l.substring(0,l.indexOf(":"));
                    String m = l.substring(l.indexOf(":")+1);
                    result.put(mid,m);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }


    private static void log2tree(String logPath, String output_path) {
        // log to csv and attrMap
        File logFile = new File(logPath);
        LinkedHashMap<String, String> attrMapRes;

        String csvPath = output_path + "/values.csv";
        File csvFile = new File(csvPath);
        if((attrMapRes = JavaFile.log2csv(logFile, csvFile)) == null) {
            JavaLogger.error(_name + "#run Failing at log to csv transformation : " + logFile.getAbsolutePath());
            System.out.println("Log2Csv failed at " + logFile.getAbsolutePath());
            return;
        }
        System.out.println("Log2Csv successful at " + logFile.getAbsolutePath());

        // extract all vars, excluded field or array-element
        Set<String> vars = new LinkedHashSet<>();
        Set<String> locs = new LinkedHashSet<>();
        for(String s : attrMapRes.values()) {
            if(s.contains("-") && s.substring(s.lastIndexOf("-")+1).contains("/")) {
            //if(s.contains("-") && StringUtils.isNumeric(s.substring(s.lastIndexOf("-")+1))) {
                String name = s.substring(0, s.lastIndexOf("-"));
                String linecolsize = s.substring(s.lastIndexOf("-")+1);
                String line = linecolsize.split("/")[0];
                String col = linecolsize.split("/")[1];
                if(!StringUtils.isNumeric(line) || !StringUtils.isNumeric(col)) {
                    continue;
                }
                locs.add(line);

                // TODO: check correctness for special case like predicates by human added
                // so need to recollected values
                if(name.contains("{PRED}")) {
                    name = name.substring(0,name.indexOf("{PRED}"));
                    vars.add(name+"-"+linecolsize);
                    continue;
                }
                vars.add(name+"-"+linecolsize);
            }
        }

        // TODO: load data dependency analysis results
        D4jSubject subject = new D4jSubject(SRC_BASE, PID, Integer.parseInt(BID));

        // build dependency parser
        if(dependencyParser==null){
            if(PID.equals("closure")||PID.equals("mockito")) {
                dependencyParser = new DependencyParser(true);
            } else if(PID.equals("jacksoncore") && BID.equals("10")) {
                dependencyParser = new DependencyParser(true);
            } else if(PID.equals("math") && BID.equals("32")) {
                dependencyParser = new DependencyParser(true);
            } else {
                dependencyParser = new DependencyParser();
            }
            dependencyParser.parse(TRACE_DIR, subject);
        }

        Map<String, Double> scoreByVariable = new HashMap<>();

        // load or build graph
        String graphPath = Config.graphPath + "/" + PID + "_" + BID + ".ser";
        DependencyGraph graph = dependencyParser.getDependencyGraph();

        // get all vertexes in range
        Set<DependencyGraphVertex> vertexInRange = new LinkedHashSet<>();
        Map<DependencyGraphVertex, Set<DependencyGraphVertex>> equisByVertex = new IdentityHashMap<>();
        for(Map.Entry entry : graph.getVertexes().entrySet()) {
            DependencyGraphVertex vertex = (DependencyGraphVertex) entry.getValue();
            String loc = "";
            if(vertex instanceof VariableVertex) {
                loc = "" + ((VariableVertex) vertex).getLineNo();
            } else if(vertex instanceof TempVertex) {
                loc = "" + ((TempVertex) vertex).getLineNo();
            } else if(vertex instanceof MethodInvocationVertex) {
                loc = "" + ((MethodInvocationVertex) vertex).getLineNo();
            } else {
                continue;
            }
            boolean inflag = false;
            for(String loc0 : locs) {
                if(loc0.equals(loc)) {
                    inflag = true;
                    break;
                }
            }
            if(inflag) {
                vertexInRange.add(vertex);
            } else {
                continue;
            }
            
        }

        // get all equis
        for(DependencyGraphVertex vertex : vertexInRange) {
            // check equis by def-use
            Set<DependencyGraphVertex> equis = new LinkedHashSet<>();
            for(DependencyGraphVertex v : vertexInRange) {
                if(v==vertex) { continue; }
                if(v.getVertexId().equals(vertex.getVertexId())) { continue; }
                if(dependencyParser.isEquivalent(vertex, v)) {
                    equis.add(v);
                }
            }
            if(!equis.isEmpty()) {
                equisByVertex.put(vertex, equis);
            }
        }

        // compute score
        for(DependencyGraphVertex vertex : vertexInRange) {
            double score = computeScoreByEdge(equisByVertex, vertex);
            scoreByVariable.put(vertex.getVertexId(), score);
        }
        for(DependencyGraphVertex vertex : equisByVertex.keySet()) {
            double newScore = scoreByVariable.get(vertex.getVertexId());
            for(DependencyGraphVertex equi : equisByVertex.get(vertex)) {
                newScore *= scoreByVariable.get(equi.getVertexId());
            }
            scoreByVariable.replace(vertex.getVertexId(), scoreByVariable.get(vertex.getVertexId()), newScore);
        }

        String outputPath = output_path + "/tree";
        orderByDependency(outputPath, attrMapRes, equisByVertex, scoreByVariable);
    }

    private static double computeScoreByEdge(Map<DependencyGraphVertex, Set<DependencyGraphVertex>> equisByVertex, DependencyGraphVertex vertex) {
        double score = 1.0;
        for (DependencyGraphEdge outEdge : vertex.getStartEdges()) {
            // vertex --outEdge--> x
            if (outEdge.getEdgeType() == EdgeType.CONTROL_DEPENDENCY) {
                score *= DEPENDENCY_FACTOR;
            }
            if (outEdge.getEdgeType() == EdgeType.DATA_DEPENDENCY || outEdge.getEdgeType() == EdgeType.DEF_USE) {
                if (vertex instanceof TempVertex && outEdge.getEndVertex() instanceof TempVertex) {
                    int implicit = getImplicitDependenceNum(outEdge.getEndVertex());
                    score *= Math.pow(DEPENDENCY_FACTOR, implicit);
                } else {
                    score *= DEPENDENCY_FACTOR;
                    if (vertex instanceof VariableVertex && outEdge.getEndVertex() instanceof TempVertex) {
                        score *= computeScoreByEdge(equisByVertex, outEdge.getEndVertex());
                    }
                }
            }
        }
        // also the equis
        //if(equisByVertex.containsKey(vertex)) {
        //    for (DependencyGraphVertex equi : equisByVertex.get(vertex)) {
        //        for(DependencyGraphEdge outEdge : equi.getStartEdges()) {
        //            if (outEdge.getEdgeType() == EdgeType.DATA_DEPENDENCY || outEdge.getEdgeType() == EdgeType.CONTROL_DEPENDENCY) {
        //                score *= DEPENDENCY_FACTOR;
        //            }
        //        }
        //    }
        //}
        return score;
    }

    private static int getImplicitDependenceNum(DependencyGraphVertex start) {
        // vertex is TEMP and outEdge.type is DD
        // TEMP --DD--> TEMP --CD--> VAR
        int num = 1; // init
        for (DependencyGraphEdge outEdge : start.getStartEdges()) {
            if (outEdge.getEdgeType() == EdgeType.CONTROL_DEPENDENCY) {
                num++;
            } else if (outEdge.getEdgeType() == EdgeType.DATA_DEPENDENCY && outEdge.getEndVertex() instanceof TempVertex) {
                num += getImplicitDependenceNum(outEdge.getEndVertex());
            }
        }
        return num;
    }


    /**
     * build tree with the given data, in a recursive style
     */
    private static void orderByDependency( String outputPath, LinkedHashMap<String, String> attrMapRes,
                                           Map<DependencyGraphVertex, Set<DependencyGraphVertex>> equisByVertex,
                                           Map<String, Double> scoreByPDG) {
        StringBuffer treeBuf = new StringBuffer();
        Set<AttributeStatistic> attrResult = new LinkedHashSet<>();

        for(String attr : attrMapRes.keySet()) {
            if(attrMapRes.get(attr).equals("test_name") || attrMapRes.get(attr).equals("P/F")) {
                continue;
            }
            attrResult.add(new AttributeStatistic(attr, attrMapRes.get(attr), 0.0, 0.0, 0.0));
        }


        List<AttributeStatistic> tmp0 = new ArrayList<>(attrResult);
        for(AttributeStatistic attr : tmp0) {
            attr.computeScore();
        }
        Collections.sort(tmp0, new AttrComparator());

        Map<String, Double> scoreByName = new LinkedHashMap<>();
        // scoreByPDG --> scoreByName
        for(AttributeStatistic a : tmp0) {
            String aname = a._content;
            Double ascore = a._score;

            String iden = aname.substring(0, aname.lastIndexOf("-"));
            if(aname.contains("{PRED}")) {
                iden = aname.substring(0, aname.indexOf("{PRED}"));
            }
            String pos = aname.substring(aname.lastIndexOf('-')+1);
            String line = pos.split("/")[0];
            String col = pos.split("/")[1];
            String iden_pdg = "";
            if (pos.split("/").length == 2) {
                iden_pdg = "Variable#" + CLAZZ + "." + METHOD + ":" + line + "|" + col + "-" + iden;
            } else if (pos.split("/").length == 3) {
                String len = pos.split("/")[2];
                iden_pdg = "Temp#" + CLAZZ + "." + METHOD + ":" + line + "|" + col + "|" + len + "-TEMP135241";
            }
            String iden_pdg2 = CLAZZ + "." + METHOD + ":" + line + "-" + iden;
            Double score_pdg = 1.0;
            if(scoreByPDG.containsKey(iden_pdg)) {
                score_pdg = scoreByPDG.get(iden_pdg);
            }
            for(String k : scoreByPDG.keySet()) {
                if(k.startsWith("MethodArgument") && k.endsWith(iden_pdg2)) {
                    score_pdg = scoreByPDG.get(k);

                    VariableVertex vv = null;
                    for(DependencyGraphVertex v : equisByVertex.keySet()) {
                        if(v.getVertexId().startsWith("MethodArgument") && v.getVertexId().endsWith(iden_pdg2)) {
                            vv = new VariableVertex(((MethodInvocationVertex) v).getClazz(),((MethodInvocationVertex) v).getMethodName(), Integer.valueOf(line), ((MethodInvocationVertex) v).getSimpleName(), Integer.valueOf(col));
                            Set<DependencyGraphVertex> newequis2 = new LinkedHashSet<>();
                            newequis2.addAll(equisByVertex.get(v));
                            newequis2.add(v);
                            for(DependencyGraphVertex v2 : equisByVertex.get(v)) {
                                Set<DependencyGraphVertex> newequis = new LinkedHashSet<>();
                                newequis.addAll(equisByVertex.get(v2));
                                newequis.add(vv);
                                equisByVertex.replace(v2, equisByVertex.get(v2), newequis);
                            }
                            Set<DependencyGraphVertex> newequis = new LinkedHashSet<>();
                            newequis.addAll(equisByVertex.get(v));
                            newequis.add(vv);
                            equisByVertex.replace(v, equisByVertex.get(v), newequis);
                            equisByVertex.put(vv, newequis2);
                            break;
                        }
                    }

                    break;
                }
            }
            ascore = ascore * score_pdg;

            scoreByName.put(aname, score_pdg);
        }

        // aggregate different lines to a single node
        Map<String, String> newAggreByName = new LinkedHashMap<>();
        for(Map.Entry entry : equisByVertex.entrySet()) {
            List<String> equiUnit = new ArrayList<>();
            DependencyGraphVertex key = (DependencyGraphVertex) entry.getKey();
            Set<DependencyGraphVertex> value = (Set<DependencyGraphVertex>) entry.getValue();
            equiUnit.add(resolveID(key));
            equiUnit.addAll(resolveIDs(value));
            Collections.sort(equiUnit, new EquiComparator());
            String equiStr = "{";
            for(String equi : equiUnit) {
                if(equiStr.endsWith("{")) {
                    equiStr += equi.substring(equi.lastIndexOf('-')+1);
                } else {
                    equiStr += ";" + equi.substring(equi.lastIndexOf('-')+1);
                }
            }
            equiStr += "}";
            newAggreByName.put(resolveID(key), equiStr);
        }
        // need to change scoreByName, attrMapRes, data.attributeName
        Map<String, Double> scoreByName_afterAggre = new LinkedHashMap<>();
        Map<String, String> newNameByOld = new LinkedHashMap<>();
        for(Map.Entry entry : scoreByName.entrySet()) {
            String key = (String) entry.getKey();
            String iden = key.substring(0,key.lastIndexOf("-"));
            String pos = key.substring(key.lastIndexOf("-")+1);
            String pred = "";
            if(iden.contains("{PRED}")) {
                pred = iden.substring(iden.indexOf("{PRED}"));
                iden = iden.substring(0, iden.indexOf("{PRED}"));
            }
            if(newAggreByName.containsKey(iden+"-"+pos)) {
                String keyStr = iden+pred+"-"+newAggreByName.get(iden+"-"+pos);
                Double newScore = (Double) entry.getValue();
                if(!scoreByName_afterAggre.containsKey(keyStr)) {
                    scoreByName_afterAggre.put(keyStr, newScore);
                } else {
                    if(scoreByName_afterAggre.get(keyStr)>newScore) {
                        scoreByName_afterAggre.put(keyStr, newScore);
                    }
                }
                newNameByOld.put((String) entry.getKey(), iden+pred+"-"+newAggreByName.get(iden+"-"+pos));
            } else {
                scoreByName_afterAggre.put((String) entry.getKey(), (Double) entry.getValue());
            }
        }

        // write all attrs
        treeBuf.append("Reorder:\n");
        List<AttributeStatistic> finalresult = new ArrayList<>();
        for(String attr : scoreByName_afterAggre.keySet()) {
            finalresult.add(new AttributeStatistic("A?", attr, scoreByName_afterAggre.get(attr)));
        }
        Collections.sort(finalresult, new AttrComparator());
        for(AttributeStatistic attr : finalresult) {
            treeBuf.append(attr.attr2line()+"\n");
        }

        JavaFile.writeTreeToFile(outputPath, treeBuf.toString());
    }

    private static Set<String> resolveIDs(Set<DependencyGraphVertex> id) {
        Set<String> result = new HashSet<>();
        for (DependencyGraphVertex v : id) {
            String idstr = v.getVertexId();

            int nstart = -1;
            Matcher m = Constant.NODE_REGEX_3.matcher(idstr);
            if(m.find()) {
                nstart = m.end();
            } else {
                m = Constant.NODE_REGEX_2.matcher(idstr);
            if(m.find()) {
                nstart = m.end();
            } else {
                m = Constant.NODE_REGEX_1.matcher(idstr);
                if(m.find()) {
                    nstart = m.end();
                }
            }
            }

            String name = idstr.substring(nstart);
            String pos = m.group().substring(1,m.group().length()-1);
            pos = pos.replaceAll("\\|", "/");
            String var = name + "-" + pos;
            result.add(var);
        }
        return result;
    }

    private static String resolveID(DependencyGraphVertex id) {
        String idstr = id.getVertexId();

        int nstart = -1;
        Matcher m = Constant.NODE_REGEX_3.matcher(idstr);
        if(m.find()) {
            nstart = m.end();
        } else {
            m = Constant.NODE_REGEX_2.matcher(idstr);
            if(m.find()) {
                nstart = m.end();
            } else {
                m = Constant.NODE_REGEX_1.matcher(idstr);
                if(m.find()) {
                    nstart = m.end();
                }
            }
        } 

        String name = idstr.substring(nstart);
        String pos = m.group().substring(1,m.group().length()-1);
        pos = pos.replaceAll("\\|", "/");
        String var = name + "-" + pos;
        return var;
    }

    private static void computeTotalScore(List<AttributeStatistic> tmp) {
        for(AttributeStatistic attr : tmp) {
            attr._score = attr._importance*Math.pow(1-Math.pow(1/attr._importance,2),2) + Math.sqrt(attr._score);
        }
    }

    private static void parseClazzMethod(String mids_path) {
        File mids = new File(mids_path);
        if(mids.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(mids));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    if(line.startsWith(MID+":")) {
                        String tmp = line.substring(line.indexOf(":")+1).trim();
                        String[] elements = tmp.split("#");
                        CLAZZ = elements[0];
                        METHOD = elements[2];
                        break;
                    }
                }
                reader.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
