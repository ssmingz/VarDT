package fl.weka;

import fl.utils.Constant;
import fl.utils.JavaFile;
import fl.utils.JavaLogger;
import org.apache.commons.lang.StringUtils;
import pda.common.java.D4jSubject;
import pda.core.dependency.Config;
import pda.core.dependency.DependencyParser;
import pda.core.dependency.dependencyGraph.*;
import weka.attributeSelection.CorrelationAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.attributeSelection.Ranker;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.*;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @description: std.log --> values.csv and attrMap --> tree
 * @author:
 * @time: 2021/8/4 11:46
 */
public class IntraGenTree {
    private static String _name = "@GenTree ";
    private static Set<String> _checkedComponents = new HashSet<>();
    private static Map<String, Set<String>> _checkedComponentsMap = new HashMap<>();
    private static String PID, BID, MID;
    private static String CLAZZ = null;
    private static String METHOD = null;
    private static String SRC_BASE, TRACE_DIR;
    private static boolean SLICING = true;

    private static String LINESCORE_BASE;
    private static Map<String, String> methodByMID;

    private static double DEPENDENCY_FACTOR = 0.8;
    //private static double DEPENDENCY_FACTOR = 1.0;

    private static DependencyParser dependencyParser;

    private static int TREE_MAX_DEPTH = 5;

    public static void main(String[] args) {
        // log to csv and attrMap

        SLICING = true;
        DEPENDENCY_FACTOR = 0.8;

        String collectDir = args[0];
        String outputDir = args[1];
        String pro = args[2];
        String j = args[3];

        dependencyParser = null;
        String values = String.format("%s/%s/%s_%d",collectDir,pro,pro,j);
        File valuesDir = new File(values);
        int ms = 0;
        for(File f : valuesDir.listFiles()) {
            if(f.isDirectory()) {
                ms++;
            }
        }
        for(int k=0; k<ms; k++) {
            String log_path = String.format("%s/%s/%s_%d/%d/std.log",collectDir,pro,pro,j,k);
            String output_path = String.format("%s/%s/%s_%d/%d",outputDir,pro,pro,j,k);
            if (SLICING) {
                log_path = String.format("%s/%s/%s_%d/%d/std_slicing.log",collectDir,pro,pro,j,k);
                output_path = String.format("%s/%s/%s_%d/%d",outputDir,pro,pro,j,k);
            }
            File output = new File(output_path);
            if(output.exists()) {
                //continue;
            }
            File treesDir = new File(output_path);
            if(!treesDir.exists()) {
                treesDir.mkdirs();
            }
            PID = pro;
            BID = ""+j;
            MID = ""+k;
            SRC_BASE = args[4];
            TRACE_DIR = args[5];
            LINESCORE_BASE = args[6];
            //if(pro.equals("lang")||pro.equals("chart")||pro.equals("math")||pro.equals("time")||pro.equals("closure")) {
            //    LINESCORE_BASE = String.format("",pro,pro,j);
            //}
            methodByMID = loadMID(String.format("%s/instrumented_method_id.txt",values));
            parseClazzMethod(String.format("%s/instrumented_method_id.txt",values));

            log2tree(log_path, output_path);
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
                //locs.add(linecolsize);
                locs.add(line);
                //if(name.startsWith("(") && name.endsWith(")")) {
                //    continue;
                //}

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
                //loc = ((VariableVertex) vertex).getLineNo() + "/" + ((VariableVertex) vertex).getColNo();
                loc = "" + ((VariableVertex) vertex).getLineNo();
            } else if(vertex instanceof TempVertex) {
                //loc = ((TempVertex) vertex).getLineNo() + "/" + ((TempVertex) vertex).getColNo() + "/" + ((TempVertex) vertex).getSize();
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
        Set<DependencyGraphVertex> checked = new HashSet<>();
        for(DependencyGraphVertex vertex : vertexInRange) {
            double score = computeScoreByEdge(equisByVertex, vertex);
            scoreByVariable.put(vertex.getVertexId(), score);
        }
        for(DependencyGraphVertex vertex : equisByVertex.keySet()) {
            double newScore = scoreByVariable.get(vertex.getVertexId());
            for(DependencyGraphVertex equi : equisByVertex.get(vertex)) {
                double otherscore = scoreByVariable.get(equi.getVertexId());
                if (otherscore < newScore) {
                    scoreByVariable.replace(vertex.getVertexId(), scoreByVariable.get(vertex.getVertexId()), newScore);
                }
            }
        }

        // csv to tree
        try {
            String srcPath = csvPath;
            String outputPath = output_path + "/tree";
            DataSource src = new DataSource(srcPath);
            Instances data = src.getDataSet();
            if(data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes()-1);
            }
            // 1.delete attr-test_name
            Remove remove_testname = new Remove();
            String[] options1 = Utils.splitOptions("-R 1");
            remove_testname.setOptions(options1);
            remove_testname.setInputFormat(data);
            Instances data1 = Filter.useFilter(data, remove_testname);
            // 2.change attr-P/F type: Numeric to Nominal
            NumericToNominal changeFormat_pf = new NumericToNominal();
            String[] options2 = Utils.splitOptions("-R last");
            changeFormat_pf.setOptions(options2);
            changeFormat_pf.setInputFormat(data1);
            Instances data2 = Filter.useFilter(data1, changeFormat_pf);
            // 3.change String attrs: String to Nominal
            StringToNominal changeFormat_string = new StringToNominal();
            String[] options3 = Utils.splitOptions("-R first-last");
            changeFormat_string.setOptions(options3);
            changeFormat_string.setInputFormat(data2);
            Instances data3 = Filter.useFilter(data2, changeFormat_string);
            // 4.delete instances with all NaN
            for(int i=data3.numInstances()-1; i>=0; i--) {
                Instance inst = data3.get(i);
                boolean isAllNaN = true;
                for(int j=0; j<data3.numAttributes()-1; j++) {
                    if(!inst.toString(j).equals("?")) {
                        isAllNaN = false;
                        break;
                    }
                }
                if(isAllNaN) {
                    data3.delete(i);
                }
            }
            // if less than 2 instances left after deleting all-NaN lines, skip building tree
            int instancesNum = data3.numInstances();
            if(instancesNum <= 2) {
                String content = "There are only " + instancesNum + " instances, too few to build tree";
                JavaFile.writeTreeToFile(outputPath, content);
                JavaLogger.info("Failed write tree to file, too few instances : " + outputPath);
                return;
            }
            // replace missing
            Instances data4 = data3;
            ReplaceMissingWithUserConstant replaceMissing = new ReplaceMissingWithUserConstant();
            String range = "?";
            for(int i=0; i<data3.numAttributes()-1; i++) {
                Attribute a = data3.attribute(i);
                if(a.isNominal()) {
                    if(!attrMapRes.get(a.name()).contains("{PRED}")) {
                        if(a.toString().endsWith("{true}") || a.toString().endsWith("{true,false}") || a.toString().endsWith("{false,true}")) {
                            int ii = i+1;
                            range += "," + ii;
                        }
                    }
                } else if(a.isNumeric() && !attrMapRes.get(a.name()).contains("{PRED}")) {
                    int ii = i+1;
                    range += "," + ii;
                }
            }
            range = range.replaceAll("\\?,","");
            if(!range.equals("?")) {
                replaceMissing.setAttributes(range);
                replaceMissing.setInputFormat(data3);
                replaceMissing.setNominalStringReplacementValue("false");
                replaceMissing.setNumericReplacementValue("0");
                data4 = Filter.useFilter(data3, replaceMissing);
            }

            // 5.remove useless attributes
            RemoveUseless removeUseless = new RemoveUseless();
            removeUseless.setInputFormat(data4);
            Instances data5 = Filter.useFilter(data4, removeUseless);

            // build decision tree
            buildTreeRecursive(data5, outputPath, attrMapRes, equisByVertex, scoreByVariable);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    /**
     * build tree with the given data, in a recursive style
     * @param data4
     * @param outputPath
     */
    private static void buildTreeRecursive(Instances data4, String outputPath, LinkedHashMap<String, String> attrMapRes,
                                           Map<DependencyGraphVertex, Set<DependencyGraphVertex>> equisByVertex,
                                           Map<String, Double> scoreByPDG) {
        Instances data = new Instances(data4);
        // check whether all fail/pass instances
        if(data.numClasses()==1) {
            JavaFile.writeTreeToFile(outputPath, "Data has unary class!");
            JavaLogger.info("Has unary class, failed write trees to file : " + outputPath);
            return;
        }

        int round = 1;
        StringBuffer treeBuf = new StringBuffer();
        Set<AttributeStatistic> attrResult = new LinkedHashSet<>();
        Map<String, Double> reorderedAttrResult = new LinkedHashMap<>();
        // init isCorrelated-value map
        LinkedHashMap<String, Double> isCorrelatedMap = new LinkedHashMap<>();
        for(String attr : attrMapRes.keySet()) {
            isCorrelatedMap.put(attr, Double.valueOf(1));
        }
        // attribute evaluating
        GainRatioAttributeEval eval_gain = new GainRatioAttributeEval();
        CorrelationAttributeEval eval_correlation = new CorrelationAttributeEval();
        try {
            eval_gain.buildEvaluator(data4);
            eval_correlation.buildEvaluator(data4);
            for(int i=0; i<data.numAttributes()-1; i++) {
                String attr = data.attribute(i).name();
                String attrContent = attrMapRes.get(attr);
                double val_gain = eval_gain.evaluateAttribute(i);
                double val_correlation = eval_correlation.evaluateAttribute(i);
                val_correlation = Math.abs(val_correlation);
                CorrelationAttributeEval eval_correlation2 = new CorrelationAttributeEval();
                data4.setClassIndex(i);
                try {
                    eval_correlation2.buildEvaluator(data4);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for(int j=0; j<data.numAttributes()-1; j++) {
                    if(isCorrelatedMap.get(data.attribute(j).name()) == 0) {
                        continue;
                    }
                    Ranker ranker = new Ranker();
                    ranker.search(eval_correlation2, data4);
                    ranker.rankedAttributes();
                    double isCorrelated = eval_correlation2.evaluateAttribute(i);
                    if(isCorrelated == 1) {
                        isCorrelated = 0;
                    }
                    else if(isCorrelated == 0) {
                        isCorrelated = 1;
                    }
                    isCorrelatedMap.put(data.attribute(i).name(), Double.valueOf(isCorrelatedMap.get(data.attribute(j).name())*isCorrelated));
                }
                data4.setClassIndex(data4.numAttributes()-1);
                double val_isCorrelated = isCorrelatedMap.get(attr);
                attrResult.add(new AttributeStatistic(attr, attrContent, val_gain, val_correlation, val_isCorrelated));
                //data.deleteAttributeAt(i);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        List<AttributeStatistic> tmp0 = new ArrayList<>(attrResult);
        for(AttributeStatistic attr : tmp0) {
            attr.computeScore();
        }
        Collections.sort(tmp0, new AttrComparator());

        // transform to name-score map
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
            String iden_pdg = "", iden_pdg3 = "";
            if (pos.split("/").length == 2) {
                iden_pdg = "Variable#" + CLAZZ + "." + METHOD + ":" + line + "|" + col + "-" + iden;
            } else if (pos.split("/").length == 3) {
                String len = pos.split("/")[2];
                iden_pdg = "Temp#" + CLAZZ + "." + METHOD + ":" + line + "|" + col + "|" + len + "-TEMP135241";
                iden_pdg3 = "Variable#" + CLAZZ + "." + METHOD + ":" + line + "|" + col + "-" + iden.substring(1,iden.length()-1);
            }
            String iden_pdg2 = CLAZZ + "." + METHOD + ":" + line + "-" + iden;
            Double score_pdg = 1.0;
            if(scoreByPDG.containsKey(iden_pdg)) {
                score_pdg = scoreByPDG.get(iden_pdg);
            } else if(scoreByPDG.containsKey(iden_pdg3)) {
                score_pdg = scoreByPDG.get(iden_pdg3);
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
            //ascore = ascore * score_pdg;

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
        Set<String> selected = new HashSet<>();
        for(int i=data.numAttributes()-2; i>=0; i--) {
            Attribute attr = data.attribute(i);
            String aname = attrMapRes.get(attr.name());
            if(newNameByOld.containsKey(aname)) {
                String newname = newNameByOld.get(aname);
                if(selected.contains(newname)) {
                    data.deleteAttributeAt(i);
                } else {
                    selected.add(newname);
                }
            }
        }
        for(Map.Entry entry : attrMapRes.entrySet()) {
            String value = (String) entry.getValue();
            if(newNameByOld.containsKey(value)) {
                attrMapRes.replace((String) entry.getKey(), value, newNameByOld.get(value));
            } else {
                if (value.contains("-")) {
                    String iden = value.substring(0,value.lastIndexOf("-"));
                    String pos = value.substring(value.lastIndexOf("-")+1);
                    String pred = "";
                    if(iden.contains("{PRED}")) {
                        pred = iden.substring(iden.indexOf("{PRED}"));
                        iden = iden.substring(0, iden.indexOf("{PRED}"));
                    }
                    if(newNameByOld.containsKey(iden+"-"+pos)) {
                        String newiden = iden+pred+"-"+newNameByOld.get(iden+"-"+pos);
                        attrMapRes.replace((String) entry.getKey(), value, newiden);
                    }
                }
            }
        }

        while(data.numAttributes()-1 > 0) {
            RandomTree2 tree = new RandomTree2(scoreByName_afterAggre, attrMapRes);
            tree.setMaxDepth(TREE_MAX_DEPTH);
            try {
                // build tree
                tree.buildClassifier(data);
                // get attrs in this round
                Set<String> attrs = getAllAttributes(tree);
                // stop building when no attr is in the current tree
                if(attrs.size() == 0) {
                    break;
                }
                // save the tree
                //System.out.println(tree.toString());
                String tmp = tree.toString().replace("\nRandomTree\n==========\n\n", "Round" + round + "\n");
                tmp = tmp.replace(tmp.substring(tmp.indexOf("\nSize of the tree")), "\n");
                treeBuf.append(tmp);

                // reorder for this tree in this round
                // TODO: optimize reorder algorithm
                reorderedAttrResult.putAll(tree.m_Tree.reorder());

                // delete attrs in this round
                for(String attr : attrs) {
                    int attrIndex = data.attribute(attr).index();
                    data.deleteAttributeAt(attrIndex);
                }
                JavaLogger.info("Successfully build tree in round : " + round++);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // write all attrs
        treeBuf.append("All attributes get:\n");
        List<AttributeStatistic> nan = new ArrayList<>();
        List<AttributeStatistic> finalresult = new ArrayList<>();
        for(AttributeStatistic attr : tmp0) {
            if(Double.isNaN(attr._score)) {
                nan.add(attr);
            } else {
                finalresult.add(attr);
            }
        }
        if(!nan.isEmpty()) {
            finalresult.addAll(nan);
        }
        Collections.sort(finalresult, new AttrComparator());
        for(AttributeStatistic attr : finalresult) {
            //treeBuf.append(attr.attr2line());
        }

        // after reorder
        treeBuf.append("\nReorder:\n");
        List<Map.Entry<String, Double>> reordered = new ArrayList<>(reorderedAttrResult.entrySet());
        reordered.sort(new Comparator<Map.Entry<String, Double>>() {
            @Override
            public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                return o2.getValue().compareTo(o1.getValue());
            }
        });
        Iterator it = reordered.iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            
            String vname = attrMapRes.get(entry.getKey());
            Double pdgscore = 1.0;
            if (scoreByName_afterAggre.containsKey(vname)) {
                pdgscore = scoreByName_afterAggre.get(vname);
            }
            if (vname.startsWith("(") && vname.endsWith(")")) {
                vname = vname.substring(1, vname.lastIndexOf(")"));
                if (scoreByName_afterAggre.containsKey(vname)) {
                    Double ns = scoreByName_afterAggre.get(vname);
                    if (ns<pdgscore) {
                        pdgscore = ns;
                    }
                }
            }
            
            treeBuf.append(entry.getKey() + " "
                    + vname + " "
                    + entry.getValue() + " "
                    + pdgscore + " "
                    + ((Double)entry.getValue())*scoreByName_afterAggre.get(attrMapRes.get(entry.getKey())).doubleValue() + "\n");
        }

        JavaFile.writeTreeToFile(outputPath, treeBuf.toString());

        JavaLogger.info("Successfully write trees to file : " + outputPath + " #TotalRound : " + --round);
    }

    private static Map<String, Map<String, Double>> loadLineScore(String linescoreBase) {
        Map<String, Map<String, Double>> scoreByLines = new LinkedHashMap<>();
        File file = new File(linescoreBase);
        int index = 3;
        try{
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String l;
            while((l = reader.readLine()) != null) {
                l = l.trim();
                if(!l.contains("#")) {
                    String[] tmp = l.split("\t");
                    for(int i=0; i<tmp.length; i++) {
                        if(tmp[i].equals("total")) {
                            index = i;
                            break;
                        }
                    }
                } else {
                    String name = l.split("\t")[0];
                    String line = name.substring(name.lastIndexOf("#")+1);
                    name = name.substring(0,name.lastIndexOf("#"));
                    String score = l.split("\t")[index];
                    if(!scoreByLines.containsKey(name)) {
                        Map<String, Double> inner = new LinkedHashMap<>();
                        scoreByLines.put(name, inner);
                    }
                    scoreByLines.get(name).put(line,Double.valueOf(score));
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return scoreByLines;
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

    private static Map<String, Set<String>> adjustDefUse(Map<String, Map<String, Set<String>>> defUseByLine) {
        Map<String, Set<String>> result = new HashMap<>();
        if(defUseByLine == null) {
            return result;
        }
        Iterator it = defUseByLine.entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry entry = (Map.Entry) it.next();
            String lineNo = (String) entry.getKey();
            Map<String, Set<String>> defUseMap = (Map<String, Set<String>>) entry.getValue();
            Set<String> defs = defUseMap.get("def");
            Set<String> uses = defUseMap.get("use");
            if(defs.isEmpty() || defs.size()>1) {
                continue;
            }
            String def_new = "";
            for(String d : defs) {
                def_new += d + "-" + lineNo;
            }
            Set<String> uses_new = new HashSet<>();
            for(String u : uses) {
                String use_new = u + "-" + lineNo;
                uses_new.add(use_new);
            }
            result.put(def_new, uses_new);
        }
        return result;
    }

    private static Map<String, Set<String>> adjustEquiLocals(Map<Set<String>, Set<String>> aggreEquiLocalsByRD) {
        Map<String, Set<String>> result = new HashMap<>();
        if(aggreEquiLocalsByRD == null) {
            return result;
        }
        Iterator equiSetIt = aggreEquiLocalsByRD.entrySet().iterator();
        while(equiSetIt.hasNext()) {
            Map.Entry entry = (Map.Entry) equiSetIt.next();
            Set<String> equiSet = (Set<String>) entry.getValue();
            for(String local : equiSet) {
                Set<String> equis = new HashSet<>();
                equis.addAll(equiSet);
                equis.remove(local);
                equis.addAll((Set<String>) entry.getKey());
                result.put(local, equis);
            }
        }
        Iterator rdIt = aggreEquiLocalsByRD.keySet().iterator();
        while(rdIt.hasNext()) {
            Set<String> rds = (Set<String>) rdIt.next();
            for(String local : rds) {
                if(!result.containsKey(local)) {
                    Set<String> equis = new HashSet<>();
                    if(rds.size() == 1) {
                        equis.addAll(aggreEquiLocalsByRD.get(rds));
                    }
                    result.put(local, equis);
                }
            }
        }
        return result;
    }

    private static void computeScoreConsiderComponents(List<AttributeStatistic> tmp, Map<String, Set<String>> defRelationByName, Map<String, Set<String>> aggreEquiLocalsByName) {
        _checkedComponents.clear();
        if(defRelationByName.isEmpty()) {
            return;
        }
        for(int i=0; i<tmp.size(); i++) {
            AttributeStatistic as = tmp.get(i);
            if(!as.IS_FIRST_IN_EQUIVALENCE) {
                continue;
            }
            String format_name = getFormatName(as._content);
            Set<String> components = new HashSet<>();
            Set<String> tmp_cp = checkComponents(format_name, defRelationByName, aggreEquiLocalsByName);
            if(tmp_cp.size() == 0 && _checkedComponentsMap.containsKey(format_name)) {
                tmp_cp = _checkedComponentsMap.get(format_name);
            }
            components.addAll(tmp_cp);
            // check all candidates in tmp
            if(!components.isEmpty()) {
                // as long as current candidate is a component of a local, change its score
                for(int j=0; j<tmp.size(); j++) {
                    AttributeStatistic as2 = tmp.get(j);
                    String format_name2 = getFormatName(as2._content);
                    // also need to check components' predicates
                    if(isComponent(components, format_name2)) {
                        as2._score *= 0.4; // focus on result of an error revealed by a var
                        //as2._importance *= 1.01; // focus on influence of an error caused by a var
                    }
                }
            }
        }
    }

    private static boolean isComponent(Set<String> components, String format_name) {
        if(components == null) { return false; }
        // directly check name
        if(components.contains(format_name)) { return true; }
        // also check predicates, by traversing and extracting
        // para name and line
        String p_name = format_name.substring(0, format_name.indexOf("-"));
        String p_line = format_name.substring(format_name.indexOf("-")+1);
        for(String c : components) {
            // c-name
            String c_name = c.substring(0, c.indexOf("-"));
            // c-line
            String c_line = c.substring(c.indexOf("-")+1);
            if(p_line.equals(c_line)) {
                if(p_name.contains(".") && p_name.substring(0, p_name.indexOf(".")).equals(c_name)) {
                    // field
                    return true;
                }
                else if(p_name.contains("[") && p_name.contains("]") && p_name.substring(0, p_name.indexOf("[")).equals(c_name)) {
                    // array component
                    return true;
                }
            }
        }
        return false;
    }

    private static Set<String> checkComponents(String equi, Map<String, Set<String>> defRelationByName, Map<String, Set<String>> aggreEquiLocalsByName) {
        Set<String> result = new HashSet<>();
        if(_checkedComponents.contains(equi)) {
            return result;
        }
        // both target and its equis need to be checked
        Set<String> checkList = new HashSet<>();
        checkList.add(equi);
        if(aggreEquiLocalsByName.containsKey(equi)) {
            checkList.addAll(aggreEquiLocalsByName.get(equi));
        }
        _checkedComponents.addAll(checkList);
        for(String e : checkList) {
            //_checkedComponents.add(e);
            if(defRelationByName.containsKey(e)) {
                Set<String> cAll = defRelationByName.get(e);
                result.addAll(cAll);
                // equivalents of component
                for(String c : cAll) {
                    Set<String> equi_of_c = aggreEquiLocalsByName.get(c);
                    if(equi_of_c != null && !equi_of_c.isEmpty()) {
                        result.addAll(equi_of_c);
                    }
                }
                // components of component
                for(String c : cAll) {
                    Set<String> c_of_c = checkComponents(c, defRelationByName, aggreEquiLocalsByName);
                    if(!c_of_c.isEmpty()) {
                        result.addAll(c_of_c);
                    }
                }
            }
        }
        for(String e : checkList) {
            _checkedComponentsMap.put(e, result);
        }
        return result;
    }

    private static int getImplicitDependenceNum(DependencyGraphVertex start) {
        // vertex is TEMP and outEdge.type is DD
        // TEMP --DD--> TEMP --CD--> VAR
        int num = 1; // init
        for (DependencyGraphEdge outEdge : start.getStartEdges()) {
            if (outEdge.getEdgeType() == EdgeType.CONTROL_DEPENDENCY) {
                num++;
            } else if (outEdge.getEdgeType() == EdgeType.DATA_DEPENDENCY && outEdge.getEndVertex() instanceof TempVertex && outEdge.getEndVertex().getToVertexes().size() != 0) {
                num += getImplicitDependenceNum(outEdge.getEndVertex());
            }
        }
        return num == 1 ? 0 : num;
    }

    private static void computeScoreConsiderEquivalence(List<AttributeStatistic> tmp, Map<String, Set<String>> aggreEquiLocalsByName) {
        if(aggreEquiLocalsByName.isEmpty()) {
            return;
        }
        for(int i=0; i<tmp.size(); i++) {
            AttributeStatistic as = tmp.get(i);
            if(as.CONSIDER_EQUIVALENCE) {
                // already consider its equivalents
                continue;
            }
            as.IS_FIRST_IN_EQUIVALENCE = true;
            String format_name = getFormatName(as._content);
            // get all equivalences
            if(!aggreEquiLocalsByName.containsKey(format_name)) {
                continue;
            }
            Set<String> equis = aggreEquiLocalsByName.get(format_name);
            if(equis.size() == 0) {
                continue;
            }
            // check candidates after current as
            for(int j=i+1; j<tmp.size(); j++) {
                AttributeStatistic as2 = tmp.get(j);
                String format_namt2 = getFormatName(as2._content);
                if(equis.contains(format_namt2)) {
                    as2._score *= 0.1;
                    as2.CONSIDER_EQUIVALENCE = true;
                }
            }
        }
    }

    private static String getFormatName(String fullName) {
        // x.x-line --> x-line
        String name = fullName.substring(0, fullName.indexOf("-"));
        if(fullName.contains(".")) {
            name = fullName.substring(0, fullName.indexOf("."));
        }
        String lineNo = fullName.substring(fullName.indexOf("-")+1);
        return name + "-" + lineNo;
    }

    /**
     * get all attributes appeared in this tree
     * @param tree
     * @return
     */
    private static Set<String> getAllAttributes(RandomTree2 tree) {
        Set<String> result = new LinkedHashSet<>();
        try {
            String treeStr = tree.graph();
            // match like [label="1: A6"]
            String labelPattern = "\\[(.*?)]";
            Pattern p = Pattern.compile(labelPattern);
            Matcher m = p.matcher(treeStr);
            while(m.find()){
                String param = m.group(0);
                param = param.substring(1,param.length()-1);
                // match like A6
                String attrPattern = "[0-9]+: A[0-9]+";
                Pattern p2 = Pattern.compile(attrPattern);
                Matcher m2 = p2.matcher(param);
                while(m2.find()) {
                    String a = m2.group(0).trim();
                    a = a.substring(a.indexOf("A"));
                    result.add(a);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static class AttrComparator2 implements Comparator<AttributeStatistic> {
        @Override
        public int compare(AttributeStatistic o1, AttributeStatistic o2) {
            double diff = o2._gainRatio - o1._gainRatio;
            if(diff >= 0 ) {
                return 1;
            }
            else {
                return -1;
            }
        }
    }
}
