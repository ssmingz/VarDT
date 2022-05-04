package fl.weka;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import java.io.Serializable;
import java.util.*;

import weka.attributeSelection.CorrelationAttributeEval;
import weka.attributeSelection.GainRatioAttributeEval;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.ContingencyTables;
import weka.core.Drawable;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.PartitionGenerator;
import weka.core.Randomizable;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.gui.ProgrammaticProperty;

public class RandomTree2 extends AbstractClassifier implements OptionHandler, WeightedInstancesHandler, Randomizable, Drawable, PartitionGenerator {
    private static final long serialVersionUID = -9051119597407396024L;
    protected RandomTree2.Tree m_Tree = null;
    protected Instances m_Info = null;
    protected double m_MinNum = 1.0D;
    protected int m_KValue = 0;
    protected int m_randomSeed = 1;
    protected int m_MaxDepth = 0;
    protected int m_NumFolds = 0;
    protected boolean m_AllowUnclassifiedInstances = false;
    protected boolean m_BreakTiesRandomly = false;
    protected Classifier m_zeroR;
    protected double m_MinVarianceProp = 0.001D;
    protected boolean m_computeImpurityDecreases;
    protected double[][] m_impurityDecreasees;

    protected Map<String, Double> _attrScoreByName = null;
    protected LinkedHashMap<String, String> _attrNameMap = null;
    protected Map<String, Double> _reorderedAttrs = new LinkedHashMap<>();
    protected int _testScale = 1;

    public RandomTree2() {
    }

    public RandomTree2(Map<String, Double> attrScoreByName, LinkedHashMap<String, String> attrNameMap) {
        _attrScoreByName = attrScoreByName;
        _attrNameMap = attrNameMap;
    }

    public String globalInfo() {
        return "Class for constructing a tree that considers K randomly  chosen attributes at each node. Performs no pruning. Also has an option to allow estimation of class probabilities (or target mean in the regression case) based on a hold-out set (backfitting).";
    }

    public double[][] getImpurityDecreases() {
        return this.m_impurityDecreasees;
    }

    @ProgrammaticProperty
    public void setComputeImpurityDecreases(boolean computeImpurityDecreases) {
        this.m_computeImpurityDecreases = computeImpurityDecreases;
    }

    public boolean getComputeImpurityDecreases() {
        return this.m_computeImpurityDecreases;
    }

    public String minNumTipText() {
        return "The minimum total weight of the instances in a leaf.";
    }

    public double getMinNum() {
        return this.m_MinNum;
    }

    public void setMinNum(double newMinNum) {
        this.m_MinNum = newMinNum;
    }

    public String minVariancePropTipText() {
        return "The minimum proportion of the variance on all the data that needs to be present at a node in order for splitting to be performed in regression trees.";
    }

    public double getMinVarianceProp() {
        return this.m_MinVarianceProp;
    }

    public void setMinVarianceProp(double newMinVarianceProp) {
        this.m_MinVarianceProp = newMinVarianceProp;
    }

    public String KValueTipText() {
        return "Sets the number of randomly chosen attributes. If 0, int(log_2(#predictors) + 1) is used.";
    }

    public int getKValue() {
        return this.m_KValue;
    }

    public void setKValue(int k) {
        this.m_KValue = k;
    }

    public String seedTipText() {
        return "The random number seed used for selecting attributes.";
    }

    public void setSeed(int seed) {
        this.m_randomSeed = seed;
    }

    public int getSeed() {
        return this.m_randomSeed;
    }

    public String maxDepthTipText() {
        return "The maximum depth of the tree, 0 for unlimited.";
    }

    public int getMaxDepth() {
        return this.m_MaxDepth;
    }

    public void setMaxDepth(int value) {
        this.m_MaxDepth = value;
    }

    public String numFoldsTipText() {
        return "Determines the amount of data used for backfitting. One fold is used for backfitting, the rest for growing the tree. (Default: 0, no backfitting)";
    }

    public int getNumFolds() {
        return this.m_NumFolds;
    }

    public void setNumFolds(int newNumFolds) {
        this.m_NumFolds = newNumFolds;
    }

    public String allowUnclassifiedInstancesTipText() {
        return "Whether to allow unclassified instances.";
    }

    public boolean getAllowUnclassifiedInstances() {
        return this.m_AllowUnclassifiedInstances;
    }

    public void setAllowUnclassifiedInstances(boolean newAllowUnclassifiedInstances) {
        this.m_AllowUnclassifiedInstances = newAllowUnclassifiedInstances;
    }

    public String breakTiesRandomlyTipText() {
        return "Break ties randomly when several attributes look equally good.";
    }

    public boolean getBreakTiesRandomly() {
        return this.m_BreakTiesRandomly;
    }

    public void setBreakTiesRandomly(boolean newBreakTiesRandomly) {
        this.m_BreakTiesRandomly = newBreakTiesRandomly;
    }

    public Enumeration<Option> listOptions() {
        Vector<Option> newVector = new Vector();
        newVector.addElement(new Option("\tNumber of attributes to randomly investigate.\t(default 0)\n\t(<1 = int(log_2(#predictors)+1)).", "K", 1, "-K <number of attributes>"));
        newVector.addElement(new Option("\tSet minimum number of instances per leaf.\n\t(default 1)", "M", 1, "-M <minimum number of instances>"));
        newVector.addElement(new Option("\tSet minimum numeric class variance proportion\n\tof train variance for split (default 1e-3).", "V", 1, "-V <minimum variance for split>"));
        newVector.addElement(new Option("\tSeed for random number generator.\n\t(default 1)", "S", 1, "-S <num>"));
        newVector.addElement(new Option("\tThe maximum depth of the tree, 0 for unlimited.\n\t(default 0)", "depth", 1, "-depth <num>"));
        newVector.addElement(new Option("\tNumber of folds for backfitting (default 0, no backfitting).", "N", 1, "-N <num>"));
        newVector.addElement(new Option("\tAllow unclassified instances.", "U", 0, "-U"));
        newVector.addElement(new Option("\t" + this.breakTiesRandomlyTipText(), "B", 0, "-B"));
        newVector.addAll(Collections.list(super.listOptions()));
        return newVector.elements();
    }

    public String[] getOptions() {
        Vector<String> result = new Vector();
        result.add("-K");
        result.add("" + this.getKValue());
        result.add("-M");
        result.add("" + this.getMinNum());
        result.add("-V");
        result.add("" + this.getMinVarianceProp());
        result.add("-S");
        result.add("" + this.getSeed());
        if (this.getMaxDepth() > 0) {
            result.add("-depth");
            result.add("" + this.getMaxDepth());
        }

        if (this.getNumFolds() > 0) {
            result.add("-N");
            result.add("" + this.getNumFolds());
        }

        if (this.getAllowUnclassifiedInstances()) {
            result.add("-U");
        }

        if (this.getBreakTiesRandomly()) {
            result.add("-B");
        }

        Collections.addAll(result, super.getOptions());
        return (String[])result.toArray(new String[result.size()]);
    }

    public void setOptions(String[] options) throws Exception {
        String tmpStr = Utils.getOption('K', options);
        if (tmpStr.length() != 0) {
            this.m_KValue = Integer.parseInt(tmpStr);
        } else {
            this.m_KValue = 0;
        }

        tmpStr = Utils.getOption('M', options);
        if (tmpStr.length() != 0) {
            this.m_MinNum = Double.parseDouble(tmpStr);
        } else {
            this.m_MinNum = 1.0D;
        }

        String minVarString = Utils.getOption('V', options);
        if (minVarString.length() != 0) {
            this.m_MinVarianceProp = Double.parseDouble(minVarString);
        } else {
            this.m_MinVarianceProp = 0.001D;
        }

        tmpStr = Utils.getOption('S', options);
        if (tmpStr.length() != 0) {
            this.setSeed(Integer.parseInt(tmpStr));
        } else {
            this.setSeed(1);
        }

        tmpStr = Utils.getOption("depth", options);
        if (tmpStr.length() != 0) {
            this.setMaxDepth(Integer.parseInt(tmpStr));
        } else {
            this.setMaxDepth(0);
        }

        String numFoldsString = Utils.getOption('N', options);
        if (numFoldsString.length() != 0) {
            this.m_NumFolds = Integer.parseInt(numFoldsString);
        } else {
            this.m_NumFolds = 0;
        }

        this.setAllowUnclassifiedInstances(Utils.getFlag('U', options));
        this.setBreakTiesRandomly(Utils.getFlag('B', options));
        super.setOptions(options);
    }

    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();
        result.disableAll();
        result.enable(Capability.NOMINAL_ATTRIBUTES);
        result.enable(Capability.NUMERIC_ATTRIBUTES);
        result.enable(Capability.DATE_ATTRIBUTES);
        result.enable(Capability.MISSING_VALUES);
        result.enable(Capability.NOMINAL_CLASS);
        result.enable(Capability.NUMERIC_CLASS);
        result.enable(Capability.MISSING_CLASS_VALUES);
        return result;
    }

    public void buildClassifier(Instances data) throws Exception {
        // set number of test cases for this method
        this._testScale = data.numInstances();

        if (this.m_computeImpurityDecreases) {
            this.m_impurityDecreasees = new double[data.numAttributes()][2];
        }

        // KValue: the number of randomly chosen attributes
        this.m_KValue = data.numAttributes() - 1;
        //if (this.m_KValue > data.numAttributes() - 1) {
        //    this.m_KValue = data.numAttributes() - 1;
        //}

        //if (this.m_KValue < 1) {
        //    this.m_KValue = (int)Utils.log2((double)(data.numAttributes() - 1)) + 1;
        //}

        this.getCapabilities().testWithFail(data);
        data = new Instances(data);
        data.deleteWithMissingClass();
        if (data.numAttributes() == 1) {
            System.err.println("Cannot build model (only class attribute present in data!), using ZeroR model instead!");
            this.m_zeroR = new ZeroR();
            this.m_zeroR.buildClassifier(data);
        } else {
            this.m_zeroR = null;
            Instances train = null;
            Instances backfit = null;
            Random rand = data.getRandomNumberGenerator((long)this.m_randomSeed);
            if (this.m_NumFolds <= 0) {
                train = data;
            } else {
                data.randomize(rand);
                data.stratify(this.m_NumFolds);
                train = data.trainCV(this.m_NumFolds, 1, rand);
                backfit = data.testCV(this.m_NumFolds, 1);
            }

            int[] attIndicesWindow = new int[data.numAttributes() - 1];
            int j = 0;

            for(int i = 0; i < attIndicesWindow.length; ++i) {
                if (j == data.classIndex()) {
                    ++j;
                }

                attIndicesWindow[i] = j++;
            }

            double totalWeight = 0.0D;
            double totalSumSquared = 0.0D;
            double[] classProbs = new double[train.numClasses()];

            for(int i = 0; i < train.numInstances(); ++i) {
                Instance inst = train.instance(i);
                if (data.classAttribute().isNominal()) {
                    int var10001 = (int)inst.classValue();
                    classProbs[var10001] += inst.weight();
                    totalWeight += inst.weight();
                } else {
                    classProbs[0] += inst.classValue() * inst.weight();
                    totalSumSquared += inst.classValue() * inst.classValue() * inst.weight();
                    totalWeight += inst.weight();
                }
            }

            double trainVariance = 0.0D;
            if (data.classAttribute().isNumeric()) {
                trainVariance = singleVariance(classProbs[0], totalSumSquared, totalWeight) / totalWeight;
                classProbs[0] /= totalWeight;
            }

            this.m_Tree = new RandomTree2.Tree();
            this.m_Info = new Instances(data, 0);
            this.m_Tree.buildTree(train, classProbs, attIndicesWindow, totalWeight, rand, 0, this.m_MinVarianceProp * trainVariance, 0, new HashSet<>());
            if (backfit != null) {
                this.m_Tree.backfitData(backfit);
            }

        }
    }

    public double[] distributionForInstance(Instance instance) throws Exception {
        return this.m_zeroR != null ? this.m_zeroR.distributionForInstance(instance) : this.m_Tree.distributionForInstance(instance);
    }

    public String toString() {
        if (this.m_zeroR != null) {
            StringBuffer buf = new StringBuffer();
            buf.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
            buf.append(this.getClass().getName().replaceAll(".*\\.", "").replaceAll(".", "=") + "\n\n");
            buf.append("Warning: No model could be built, hence ZeroR model is used:\n\n");
            buf.append(this.m_zeroR.toString());
            return buf.toString();
        } else {
            return this.m_Tree == null ? "RandomTree: no model has been built yet." : "\nRandomTree\n==========\n" + this.m_Tree.toString(0) + "\n\nSize of the tree : " + this.m_Tree.numNodes() + (this.getMaxDepth() > 0 ? "\nMax depth of tree: " + this.getMaxDepth() : "");
        }
    }

    public String graph() throws Exception {
        if (this.m_Tree == null) {
            throw new Exception("RandomTree: No model built yet.");
        } else {
            StringBuffer resultBuff = new StringBuffer();
            this.m_Tree.toGraph(resultBuff, 0, (RandomTree2.Tree)null);
            String result = "digraph RandomTree {\nedge [style=bold]\n" + resultBuff.toString() + "\n}\n";
            return result;
        }
    }

    public int graphType() {
        return 1;
    }

    public void generatePartition(Instances data) throws Exception {
        this.buildClassifier(data);
    }

    public double[] getMembershipValues(Instance instance) throws Exception {
        double[] a;
        if (this.m_zeroR != null) {
            a = new double[]{instance.weight()};
            return a;
        } else {
            a = new double[this.numElements()];
            Queue<Double> queueOfWeights = new LinkedList();
            Queue<RandomTree2.Tree> queueOfNodes = new LinkedList();
            queueOfWeights.add(instance.weight());
            queueOfNodes.add(this.m_Tree);
            int index = 0;

            while(true) {
                RandomTree2.Tree node;
                do {
                    if (queueOfNodes.isEmpty()) {
                        return a;
                    }

                    a[index++] = (Double)queueOfWeights.poll();
                    node = (RandomTree2.Tree)queueOfNodes.poll();
                } while(node.m_Attribute <= -1);

                double[] weights = new double[node.m_Successors.length];
                if (instance.isMissing(node.m_Attribute)) {
                    System.arraycopy(node.m_Prop, 0, weights, 0, node.m_Prop.length);
                } else if (this.m_Info.attribute(node.m_Attribute).isNominal()) {
                    weights[(int)instance.value(node.m_Attribute)] = 1.0D;
                } else if (instance.value(node.m_Attribute) < node.m_SplitPoint) {
                    weights[0] = 1.0D;
                } else {
                    weights[1] = 1.0D;
                }

                for(int i = 0; i < node.m_Successors.length; ++i) {
                    queueOfNodes.add(node.m_Successors[i]);
                    queueOfWeights.add(a[index - 1] * weights[i]);
                }
            }
        }
    }

    public int numElements() throws Exception {
        return this.m_zeroR != null ? 1 : this.m_Tree.numNodes();
    }

    protected static double variance(double[] s, double[] sS, double[] sumOfWeights) {
        double var = 0.0D;

        for(int i = 0; i < s.length; ++i) {
            if (sumOfWeights[i] > 0.0D) {
                var += singleVariance(s[i], sS[i], sumOfWeights[i]);
            }
        }

        return var;
    }

    protected static double singleVariance(double s, double sS, double weight) {
        return sS - s * s / weight;
    }

    public static void main(String[] argv) {
        runClassifier(new RandomTree2(), argv);
    }

    protected class Tree extends DT implements Serializable {
        private static final long serialVersionUID = 3549573538656522569L;
        protected RandomTree2.Tree[] m_Successors;
        protected int m_Attribute = -1;
        protected double m_SplitPoint = 0.0D / 0.0;
        protected double[] m_Prop = null;
        protected double[] m_ClassDistribution = null;
        protected double[] m_Distribution = null;

        protected Tree() {
        }

        public Tree(Map<String, Double> attrScoreByName, LinkedHashMap<String, String> attrNameMap) {
        }

        public void backfitData(Instances data) throws Exception {
            double totalWeight = 0.0D;
            double totalSumSquared = 0.0D;
            double[] classProbs = new double[data.numClasses()];

            for(int i = 0; i < data.numInstances(); ++i) {
                Instance inst = data.instance(i);
                if (data.classAttribute().isNominal()) {
                    int var10001 = (int)inst.classValue();
                    classProbs[var10001] += inst.weight();
                    totalWeight += inst.weight();
                } else {
                    classProbs[0] += inst.classValue() * inst.weight();
                    totalSumSquared += inst.classValue() * inst.classValue() * inst.weight();
                    totalWeight += inst.weight();
                }
            }

            double trainVariance = 0.0D;
            if (data.classAttribute().isNumeric()) {
                trainVariance = RandomTree2.singleVariance(classProbs[0], totalSumSquared, totalWeight) / totalWeight;
                classProbs[0] /= totalWeight;
            }

            this.backfitData(data, classProbs, totalWeight);
        }

        public double[] distributionForInstance(Instance instance) throws Exception {
            double[] returnedDist = null;
            if (this.m_Attribute > -1) {
                if (instance.isMissing(this.m_Attribute)) {
                    returnedDist = new double[RandomTree2.this.m_Info.numClasses()];

                    for(int i = 0; i < this.m_Successors.length; ++i) {
                        double[] help = this.m_Successors[i].distributionForInstance(instance);
                        if (help != null) {
                            for(int j = 0; j < help.length; ++j) {
                                returnedDist[j] += this.m_Prop[i] * help[j];
                            }
                        }
                    }
                } else if (RandomTree2.this.m_Info.attribute(this.m_Attribute).isNominal()) {
                    returnedDist = this.m_Successors[(int)instance.value(this.m_Attribute)].distributionForInstance(instance);
                } else if (instance.value(this.m_Attribute) < this.m_SplitPoint) {
                    returnedDist = this.m_Successors[0].distributionForInstance(instance);
                } else {
                    returnedDist = this.m_Successors[1].distributionForInstance(instance);
                }
            }

            if (this.m_Attribute != -1 && returnedDist != null) {
                return returnedDist;
            } else {
                double[] result;
                if (this.m_ClassDistribution == null) {
                    if (RandomTree2.this.getAllowUnclassifiedInstances()) {
                        result = new double[RandomTree2.this.m_Info.numClasses()];
                        if (RandomTree2.this.m_Info.classAttribute().isNumeric()) {
                            result[0] = Utils.missingValue();
                        }

                        return result;
                    } else {
                        return null;
                    }
                } else {
                    result = (double[])this.m_ClassDistribution.clone();
                    if (RandomTree2.this.m_Info.classAttribute().isNominal()) {
                        Utils.normalize(result);
                    }

                    return result;
                }
            }
        }

        public int toGraph(StringBuffer text, int num) throws Exception {
            int maxIndex = Utils.maxIndex(this.m_ClassDistribution);
            String classValue = RandomTree2.this.m_Info.classAttribute().isNominal() ? RandomTree2.this.m_Info.classAttribute().value(maxIndex) : Utils.doubleToString(this.m_ClassDistribution[0], RandomTree2.this.getNumDecimalPlaces());
            ++num;
            if (this.m_Attribute == -1) {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + ": " + classValue + "\"shape=box]\n");
            } else {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + ": " + classValue + "\"]\n");

                for(int i = 0; i < this.m_Successors.length; ++i) {
                    text.append("N" + Integer.toHexString(this.hashCode()) + "->N" + Integer.toHexString(this.m_Successors[i].hashCode()) + " [label=\"" + RandomTree2.this.m_Info.attribute(this.m_Attribute).name());
                    if (RandomTree2.this.m_Info.attribute(this.m_Attribute).isNumeric()) {
                        if (i == 0) {
                            text.append(" < " + Utils.doubleToString(this.m_SplitPoint, RandomTree2.this.getNumDecimalPlaces()));
                        } else {
                            text.append(" >= " + Utils.doubleToString(this.m_SplitPoint, RandomTree2.this.getNumDecimalPlaces()));
                        }
                    } else {
                        text.append(" = " + RandomTree2.this.m_Info.attribute(this.m_Attribute).value(i));
                    }

                    text.append("\"]\n");
                    num = this.m_Successors[i].toGraph(text, num);
                }
            }

            return num;
        }

        protected String leafString() throws Exception {
            double sum = 0.0D;
            double maxCount = 0.0D;
            int maxIndex = 0;
            double classMean = 0.0D;
            double avgError = 0.0D;
            if (this.m_ClassDistribution != null) {
                if (RandomTree2.this.m_Info.classAttribute().isNominal()) {
                    sum = Utils.sum(this.m_ClassDistribution);
                    maxIndex = Utils.maxIndex(this.m_ClassDistribution);
                    maxCount = this.m_ClassDistribution[maxIndex];
                } else {
                    classMean = this.m_ClassDistribution[0];
                    if (this.m_Distribution[1] > 0.0D) {
                        avgError = this.m_Distribution[0] / this.m_Distribution[1];
                    }
                }
            }

            return RandomTree2.this.m_Info.classAttribute().isNumeric() ? " : "
                    + Utils.doubleToString(classMean, RandomTree2.this.getNumDecimalPlaces()) + " (" + Utils.doubleToString(this.m_Distribution[1], RandomTree2.this.getNumDecimalPlaces()) + "/" + Utils.doubleToString(avgError, RandomTree2.this.getNumDecimalPlaces()) + ")" : " : "
                    + RandomTree2.this.m_Info.classAttribute().value(maxIndex) + " (" + Utils.doubleToString(sum, RandomTree2.this.getNumDecimalPlaces()) + "/" + Utils.doubleToString(sum - maxCount, RandomTree2.this.getNumDecimalPlaces()) + ")";
        }

        protected String toString(int level) {
            try {
                StringBuffer text = new StringBuffer();
                if (this.m_Attribute == -1) {
                    return this.leafString();
                } else {
                    int i;
                    if (RandomTree2.this.m_Info.attribute(this.m_Attribute).isNominal()) {
                        for(i = 0; i < this.m_Successors.length; ++i) {
                            text.append("\n");

                            for(int j = 0; j < level; ++j) {
                                text.append("|   ");
                            }

                            text.append(RandomTree2.this.m_Info.attribute(this.m_Attribute).name() + " = " + RandomTree2.this.m_Info.attribute(this.m_Attribute).value(i));
                            text.append(this.m_Successors[i].toString(level + 1));
                        }
                    } else {
                        text.append("\n");

                        for(i = 0; i < level; ++i) {
                            text.append("|   ");
                        }

                        text.append(RandomTree2.this.m_Info.attribute(this.m_Attribute).name() + " < " + Utils.doubleToString(this.m_SplitPoint, RandomTree2.this.getNumDecimalPlaces()));
                        text.append(this.m_Successors[0].toString(level + 1));
                        text.append("\n");

                        for(i = 0; i < level; ++i) {
                            text.append("|   ");
                        }

                        text.append(RandomTree2.this.m_Info.attribute(this.m_Attribute).name() + " >= " + Utils.doubleToString(this.m_SplitPoint, RandomTree2.this.getNumDecimalPlaces()));
                        text.append(this.m_Successors[1].toString(level + 1));
                    }

                    return text.toString();
                }
            } catch (Exception var5) {
                var5.printStackTrace();
                return "RandomTree: tree can't be printed";
            }
        }

        protected void backfitData(Instances data, double[] classProbs, double totalWeight) throws Exception {
            if (data.numInstances() == 0) {
                this.m_Attribute = -1;
                this.m_ClassDistribution = null;
                if (data.classAttribute().isNumeric()) {
                    this.m_Distribution = new double[2];
                }

                this.m_Prop = null;
            } else {
                double priorVar = 0.0D;
                if (data.classAttribute().isNumeric()) {
                    double totalSum = 0.0D;
                    double totalSumSquared = 0.0D;
                    double totalSumOfWeights = 0.0D;

                    for(int i = 0; i < data.numInstances(); ++i) {
                        Instance inst = data.instance(i);
                        totalSum += inst.classValue() * inst.weight();
                        totalSumSquared += inst.classValue() * inst.classValue() * inst.weight();
                        totalSumOfWeights += inst.weight();
                    }

                    priorVar = RandomTree2.singleVariance(totalSum, totalSumSquared, totalSumOfWeights);
                }

                this.m_ClassDistribution = (double[])classProbs.clone();
                if (this.m_Attribute > -1) {
                    this.m_Prop = new double[this.m_Successors.length];

                    int var10001;
                    for(int ix = 0; ix < data.numInstances(); ++ix) {
                        Instance instx = data.instance(ix);
                        if (!instx.isMissing(this.m_Attribute)) {
                            double[] var10000;
                            if (data.attribute(this.m_Attribute).isNominal()) {
                                var10000 = this.m_Prop;
                                var10001 = (int)instx.value(this.m_Attribute);
                                var10000[var10001] += instx.weight();
                            } else {
                                var10000 = this.m_Prop;
                                var10001 = instx.value(this.m_Attribute) < this.m_SplitPoint ? 0 : 1;
                                var10000[var10001] += instx.weight();
                            }
                        }
                    }

                    if (Utils.sum(this.m_Prop) <= 0.0D) {
                        this.m_Attribute = -1;
                        this.m_Prop = null;
                        if (data.classAttribute().isNumeric()) {
                            this.m_Distribution = new double[2];
                            this.m_Distribution[0] = priorVar;
                            this.m_Distribution[1] = totalWeight;
                        }

                        return;
                    }

                    Utils.normalize(this.m_Prop);
                    Instances[] subsets = this.splitData(data);

                    int ixx;
                    for(ixx = 0; ixx < subsets.length; ++ixx) {
                        double[] dist = new double[data.numClasses()];
                        double sumOfWeights = 0.0D;

                        for(int j = 0; j < subsets[ixx].numInstances(); ++j) {
                            if (data.classAttribute().isNominal()) {
                                var10001 = (int)subsets[ixx].instance(j).classValue();
                                dist[var10001] += subsets[ixx].instance(j).weight();
                            } else {
                                dist[0] += subsets[ixx].instance(j).classValue() * subsets[ixx].instance(j).weight();
                                sumOfWeights += subsets[ixx].instance(j).weight();
                            }
                        }

                        if (sumOfWeights > 0.0D) {
                            dist[0] /= sumOfWeights;
                        }

                        this.m_Successors[ixx].backfitData(subsets[ixx], dist, totalWeight);
                    }

                    if (RandomTree2.this.getAllowUnclassifiedInstances()) {
                        this.m_ClassDistribution = null;
                        return;
                    }

                    for(ixx = 0; ixx < subsets.length; ++ixx) {
                        if (this.m_Successors[ixx].m_ClassDistribution == null) {
                            return;
                        }
                    }

                    this.m_ClassDistribution = null;
                }

            }
        }

        protected void buildTree(Instances data, double[] classProbs, int[] attIndicesWindow, double totalWeight, Random random, int depth, double minVariance, int attrCounter, Set<String> exceptAttrs) throws Exception {
            if (data.numInstances() == 0) {
            //if (data.numInstances() == 0 || data.numAttributes()-1 == attrCounter) {
                this.m_Attribute = -1;
                this.m_ClassDistribution = null;
                this.m_Prop = null;
                if (data.classAttribute().isNumeric()) {
                    this.m_Distribution = new double[2];
                }

            } else {
                double priorVar = 0.0D;
                double val;
                double split;
                int bestIndex;
                if (data.classAttribute().isNumeric()) {
                    val = 0.0D;
                    split = 0.0D;
                    double totalSumOfWeights = 0.0D;

                    for(bestIndex = 0; bestIndex < data.numInstances(); ++bestIndex) {
                        Instance inst = data.instance(bestIndex);
                        val += inst.classValue() * inst.weight();
                        split += inst.classValue() * inst.classValue() * inst.weight();
                        totalSumOfWeights += inst.weight();
                    }

                    priorVar = RandomTree2.singleVariance(val, split, totalSumOfWeights);
                }

                if (data.classAttribute().isNominal()) {
                    totalWeight = Utils.sum(classProbs);
                }

                if (totalWeight < 2.0D * RandomTree2.this.m_MinNum || data.classAttribute().isNominal() && Utils.eq(classProbs[Utils.maxIndex(classProbs)], Utils.sum(classProbs)) || data.classAttribute().isNumeric() && priorVar / totalWeight < minVariance || RandomTree2.this.getMaxDepth() > 0 && depth >= RandomTree2.this.getMaxDepth()) {
                    this.m_Attribute = -1;
                    this.m_ClassDistribution = (double[])classProbs.clone();
                    if (data.classAttribute().isNumeric()) {
                        this.m_Distribution = new double[2];
                        this.m_Distribution[0] = priorVar;
                        this.m_Distribution[1] = totalWeight;
                    }

                    this.m_Prop = null;
                } else {
                    val = -1.7976931348623157E308D;
                    split = -1.7976931348623157E308D;
                    double[][] bestDists = (double[][])null;
                    double[] bestProps = null;
                    bestIndex = 0;
                    double[][] props = new double[1][0];
                    double[][][] dists = new double[1][0][0];
                    double[][] totalSubsetWeights = new double[data.numAttributes()][0];
                    boolean attIndex = false;
                    int windowSize = attIndicesWindow.length;
                    int k = RandomTree2.this.m_KValue;
                    boolean gainFound = false;
                    double[] tempNumericVals = new double[data.numAttributes()];

                    // compute gainRation and correlation for all attrs with current records
                    Map<String, Double> attScoreByName = new HashMap<>();
                    GainRatioAttributeEval eval_gr = new GainRatioAttributeEval();
                    CorrelationAttributeEval eval_co = new CorrelationAttributeEval();
                    eval_gr.buildEvaluator(data);
                    eval_co.buildEvaluator(data);
                    for(int i=0; i<data.numAttributes()-1; i++) {
                        String attName = _attrNameMap.get(data.attribute(i).name());
                        double val_gr = eval_gr.evaluateAttribute(i);
                        double val_co = eval_co.evaluateAttribute(i);
                        if(_attrScoreByName.containsKey(attName)) {
                            double newScore = (val_gr + val_co) * _attrScoreByName.get(attName);
                            attScoreByName.put(attName, newScore);
                        }
                    }

                    List<Map.Entry<String, Double>> tmp = new ArrayList<>(attScoreByName.entrySet());
                    Collections.sort(tmp, new Comparator<Map.Entry<String, Double>>() {
                        @Override
                        public int compare(Map.Entry<String, Double> o1, Map.Entry<String, Double> o2) {
                            return o2.getValue().compareTo(o1.getValue());
                        }
                    });

                    while(windowSize > 0 && (k-- > 0 || !gainFound)) {
                        int chosenIndex = random.nextInt(windowSize);
                        int attIndexx = attIndicesWindow[chosenIndex]; // after setting m_KValue, attIndex will traverse all attributes rather than randomly selecting
                        attIndicesWindow[chosenIndex] = attIndicesWindow[windowSize - 1];
                        attIndicesWindow[windowSize - 1] = attIndexx;
                        --windowSize;
                        double currSplit = data.classAttribute().isNominal() ? this.distribution(props, dists, attIndexx, data) : this.numericDistribution(props, dists, attIndexx, totalSubsetWeights, data, tempNumericVals);

                        double currVal = data.classAttribute().isNominal() ? this.gain(dists[0], this.priorVal(dists[0])) : tempNumericVals[attIndexx];
                        //double currVal;
                        //if(data.classAttribute().isNominal()) {
                        //    currVal = _attrScoreByName.get(_attrNameMap.get(data.attribute(attIndexx).name()));
                        //} else {
                        //    currVal = tempNumericVals[attIndexx];
                        //}
                        if (Utils.gr(currVal, 0.0D)) {
                            gainFound = true;
                        }

                        if(exceptAttrs.contains(_attrNameMap.get(data.attribute(attIndexx).name()))) {
                            continue;
                        }

                        double currVal_comp = attScoreByName.get(_attrNameMap.get(data.attribute(attIndexx).name()));
                        if (currVal_comp > val || !RandomTree2.this.getBreakTiesRandomly() && currVal_comp == val && attIndexx < bestIndex) {
                        //if (currVal > val || !RandomTree2.this.getBreakTiesRandomly() && currVal == val && attIndexx < bestIndex) {
                            val = currVal_comp;
                            //val = currVal;
                            bestIndex = attIndexx;
                            split = currSplit;
                            bestProps = props[0];
                            bestDists = dists[0];
                        }
                    }


                    this.m_Attribute = bestIndex;
                    if (Utils.gr(val, 0.0D)) {
                        if (RandomTree2.this.m_computeImpurityDecreases) {
                            double[] var32 = RandomTree2.this.m_impurityDecreasees[this.m_Attribute];
                            var32[0] += val;
                            double var10002 = RandomTree2.this.m_impurityDecreasees[this.m_Attribute][1]++;
                        }

                        this.m_SplitPoint = split;
                        this.m_Prop = bestProps;
                        Instances[] subsets = this.splitData(data);
                        this.m_Successors = new RandomTree2.Tree[bestDists.length];
                        double[] attTotalSubsetWeights = totalSubsetWeights[bestIndex];

                        for(int i = 0; i < bestDists.length; ++i) {
                            this.m_Successors[i] = RandomTree2.this.new Tree(_attrScoreByName, _attrNameMap);

                            // handle exit, e.g. there are only "2,pass" and "2,fail" left
                            //if(subsets[i].numInstances() == 0) {
                            //    continue;
                            //}

                            // need to handle (0,n), the same attribute will be selected in next turn and never out of the loop
                            Set<String> exceptA = new HashSet<>();
                            if(subsets[i].numInstances() == data.numInstances()) {
                                boolean allOneClass = true;
                                for(int j=0; j<subsets[i].numInstances()-2; j++) {
                                    if(subsets[i].instance(j).classValue() != subsets[i].instance(j+1).classValue()) {
                                        allOneClass = false;
                                        break;
                                    }
                                }
                                if(!allOneClass) {
                                    exceptA.add(_attrNameMap.get(data.attribute(bestIndex).name()));
                                }
                            }
                            if(data.attribute(this.m_Attribute).isNominal()||!Double.isNaN(this.m_SplitPoint)) {
                                this.m_Successors[i].buildTree(subsets[i], bestDists[i], attIndicesWindow, data.classAttribute().isNominal() ? 0.0D : attTotalSubsetWeights[i], random, depth + 1, minVariance, attrCounter, exceptA);
                            }
                            //this.m_Successors[i].buildTree(subsets[i], bestDists[i], attIndicesWindow, data.classAttribute().isNominal() ? 0.0D : attTotalSubsetWeights[i], random, depth + 1, minVariance, attrCounter, exceptA);
                        }

                        boolean emptySuccessor = false;

                        for(int ix = 0; ix < subsets.length; ++ix) {
                            if (this.m_Successors[ix].m_ClassDistribution == null) {
                                emptySuccessor = true;
                                break;
                            }
                        }

                        if (emptySuccessor) {
                            this.m_ClassDistribution = (double[])classProbs.clone();
                        }
                    } else {
                        this.m_Attribute = -1;
                        this.m_ClassDistribution = (double[])classProbs.clone();
                        if (data.classAttribute().isNumeric()) {
                            this.m_Distribution = new double[2];
                            this.m_Distribution[0] = priorVar;
                            this.m_Distribution[1] = totalWeight;
                        }
                    }

                }
            }
        }

        public int numNodes() {
            if (this.m_Attribute == -1) {
                return 1;
            } else {
                int size = 1;
                RandomTree2.Tree[] var2 = this.m_Successors;
                int var3 = var2.length;

                for(int var4 = 0; var4 < var3; ++var4) {
                    RandomTree2.Tree m_Successor = var2[var4];
                    size += m_Successor.numNodes();
                }

                return size;
            }
        }

        protected Instances[] splitData(Instances data) throws Exception {
            Instances[] subsets = new Instances[this.m_Prop.length];

            int i;
            for(i = 0; i < this.m_Prop.length; ++i) {
                subsets[i] = new Instances(data, data.numInstances());
            }

            for(i = 0; i < data.numInstances(); ++i) {
                Instance inst = data.instance(i);
                if (inst.isMissing(this.m_Attribute)) {
                    for(int k = 0; k < this.m_Prop.length; ++k) {
                        if (this.m_Prop[k] > 0.0D) {
                            Instance copy = (Instance)inst.copy();
                            //copy.setWeight(this.m_Prop[k] * inst.weight());
                            subsets[k].add(copy);
                        }
                    }
                } else if (data.attribute(this.m_Attribute).isNominal()) {
                    subsets[(int)inst.value(this.m_Attribute)].add(inst);
                } else {
                    if (!data.attribute(this.m_Attribute).isNumeric()) {
                        throw new IllegalArgumentException("Unknown attribute type");
                    }

                    subsets[inst.value(this.m_Attribute) < this.m_SplitPoint ? 0 : 1].add(inst);
                }
            }

            for(i = 0; i < this.m_Prop.length; ++i) {
                subsets[i].compactify();
            }

            return subsets;
        }

        protected double numericDistribution(double[][] props, double[][][] dists, int att, double[][] subsetWeights, Instances data, double[] vals) throws Exception {
            double splitPoint = 0.0D / 0.0;
            Attribute attribute = data.attribute(att);
            double[][] dist = (double[][])null;
            double[] sums = null;
            double[] sumSquared = null;
            double[] sumOfWeights = null;
            double totalSum = 0.0D;
            double totalSumSquared = 0.0D;
            double totalSumOfWeights = 0.0D;
            int indexOfFirstMissingValue = data.numInstances();
            int jxxx;
            double[] sumsx;
            double[] sumSquaredx;
            double[] sumOfWeightsx;
            if (attribute.isNominal()) {
                sumsx = new double[attribute.numValues()];
                sumSquaredx = new double[attribute.numValues()];
                sumOfWeightsx = new double[attribute.numValues()];

                for(int i = 0; i < data.numInstances(); ++i) {
                    Instance inst = data.instance(i);
                    if (inst.isMissing(att)) {
                        if (indexOfFirstMissingValue == data.numInstances()) {
                            indexOfFirstMissingValue = i;
                        }
                    } else {
                        jxxx = (int)inst.value(att);
                        sumsx[jxxx] += inst.classValue() * inst.weight();
                        sumSquaredx[jxxx] += inst.classValue() * inst.classValue() * inst.weight();
                        sumOfWeightsx[jxxx] += inst.weight();
                    }
                }

                totalSum = Utils.sum(sumsx);
                totalSumSquared = Utils.sum(sumSquaredx);
                totalSumOfWeights = Utils.sum(sumOfWeightsx);
            } else {
                sumsx = new double[2];
                sumSquaredx = new double[2];
                sumOfWeightsx = new double[2];
                double[] currSums = new double[2];
                double[] currSumSquared = new double[2];
                double[] currSumOfWeights = new double[2];
                data.sort(att);

                for(int jx = 0; jx < data.numInstances(); ++jx) {
                    Instance instx = data.instance(jx);
                    if (instx.isMissing(att)) {
                        indexOfFirstMissingValue = jx;
                        break;
                    }

                    currSums[1] += instx.classValue() * instx.weight();
                    currSumSquared[1] += instx.classValue() * instx.classValue() * instx.weight();
                    currSumOfWeights[1] += instx.weight();
                }

                totalSum = currSums[1];
                totalSumSquared = currSumSquared[1];
                totalSumOfWeights = currSumOfWeights[1];
                sumsx[1] = currSums[1];
                sumSquaredx[1] = currSumSquared[1];
                sumOfWeightsx[1] = currSumOfWeights[1];
                double currSplit = data.instance(0).value(att);
                double bestVal = 1.7976931348623157E308D;

                for(int ix = 0; ix < indexOfFirstMissingValue; ++ix) {
                    Instance instxx = data.instance(ix);
                    if (instxx.value(att) > currSplit) {
                        double currVal = RandomTree2.variance(currSums, currSumSquared, currSumOfWeights);
                        if (currVal < bestVal) {
                            bestVal = currVal;
                            splitPoint = (instxx.value(att) + currSplit) / 2.0D;
                            if (splitPoint <= currSplit) {
                                splitPoint = instxx.value(att);
                            }

                            for(int jxx = 0; jxx < 2; ++jxx) {
                                sumsx[jxx] = currSums[jxx];
                                sumSquaredx[jxx] = currSumSquared[jxx];
                                sumOfWeightsx[jxx] = currSumOfWeights[jxx];
                            }
                        }
                    }

                    currSplit = instxx.value(att);
                    double classVal = instxx.classValue() * instxx.weight();
                    double classValSquared = instxx.classValue() * classVal;
                    currSums[0] += classVal;
                    currSumSquared[0] += classValSquared;
                    currSumOfWeights[0] += instxx.weight();
                    currSums[1] -= classVal;
                    currSumSquared[1] -= classValSquared;
                    currSumOfWeights[1] -= instxx.weight();
                }
            }

            props[0] = new double[sumsx.length];

            for(jxxx = 0; jxxx < props[0].length; ++jxxx) {
                props[0][jxxx] = sumOfWeightsx[jxxx];
            }

            if (!(Utils.sum(props[0]) > 0.0D)) {
                for(jxxx = 0; jxxx < props[0].length; ++jxxx) {
                    props[0][jxxx] = 1.0D / (double)props[0].length;
                }
            } else {
                Utils.normalize(props[0]);
            }

            for(jxxx = indexOfFirstMissingValue; jxxx < data.numInstances(); ++jxxx) {
                Instance instxxx = data.instance(jxxx);

                for(int j = 0; j < sumsx.length; ++j) {
                    sumsx[j] += props[0][j] * instxxx.classValue() * instxxx.weight();
                    sumSquaredx[j] += props[0][j] * instxxx.classValue() * instxxx.classValue() * instxxx.weight();
                    sumOfWeightsx[j] += props[0][j] * instxxx.weight();
                }

                totalSum += instxxx.classValue() * instxxx.weight();
                totalSumSquared += instxxx.classValue() * instxxx.classValue() * instxxx.weight();
                totalSumOfWeights += instxxx.weight();
            }

            dist = new double[sumsx.length][data.numClasses()];

            for(jxxx = 0; jxxx < sumsx.length; ++jxxx) {
                if (sumOfWeightsx[jxxx] > 0.0D) {
                    dist[jxxx][0] = sumsx[jxxx] / sumOfWeightsx[jxxx];
                } else {
                    dist[jxxx][0] = totalSum / totalSumOfWeights;
                }
            }

            double priorVar = RandomTree2.singleVariance(totalSum, totalSumSquared, totalSumOfWeights);
            double var = RandomTree2.variance(sumsx, sumSquaredx, sumOfWeightsx);
            double gain = priorVar - var;
            subsetWeights[att] = sumOfWeightsx;
            dists[0] = dist;
            vals[att] = gain;
            return splitPoint;
        }

        protected double distribution(double[][] props, double[][][] dists, int att, Instances data) throws Exception {
            double splitPoint = 0.0D / 0.0;
            Attribute attribute = data.attribute(att);
            double[][] dist = (double[][])null;
            int indexOfFirstMissingValue = data.numInstances();
            int i;
            Instance inst;
            double[] var10000;
            int var10001;
            if (attribute.isNominal()) {
                dist = new double[attribute.numValues()][data.numClasses()];

                for(i = 0; i < data.numInstances(); ++i) {
                    inst = data.instance(i);
                    if (inst.isMissing(att)) {
                        if (indexOfFirstMissingValue == data.numInstances()) {
                            indexOfFirstMissingValue = i;
                        }
                    } else {
                        var10000 = dist[(int)inst.value(att)];
                        var10001 = (int)inst.classValue();
                        var10000[var10001] += inst.weight();
                    }
                }
            } else {
                double[][] currDist = new double[2][data.numClasses()];
                dist = new double[2][data.numClasses()];
                data.sort(att); // sort data by attr value in increasing order

                for(int jx = 0; jx < data.numInstances(); ++jx) {
                    Instance instx = data.instance(jx);
                    if (instx.isMissing(att)) {
                        indexOfFirstMissingValue = jx;
                        break;
                    }

                    var10000 = currDist[1];
                    var10001 = (int)instx.classValue();
                    var10000[var10001] += instx.weight();
                }

                double priorVal = this.priorVal(currDist);

                for(int jxxx = 0; jxxx < currDist.length; ++jxxx) {
                    System.arraycopy(currDist[jxxx], 0, dist[jxxx], 0, dist[jxxx].length);
                }

                double currSplit = data.instance(0).value(att);
                double bestVal = -1.7976931348623157E308D;

                for(int ix = 0; ix < data.numInstances(); ++ix) { // f**k, causing a lot NaN......
                    if(data.instance(ix).isMissing(att)) {
                        continue;
                    }
                //for(int ix = 0; ix < indexOfFirstMissingValue; ++ix) {
                    Instance instxx = data.instance(ix);
                    double attVal = instxx.value(att);
                    int j;
                    if (attVal > currSplit) {
                        double currVal = this.gain(currDist, priorVal); // currVal represents the gain
                        if (currVal > bestVal) {
                            bestVal = currVal;
                            splitPoint = (attVal + currSplit) / 2.0D;
                            if (splitPoint <= currSplit) {
                                splitPoint = attVal;
                            }

                            for(j = 0; j < currDist.length; ++j) {
                                System.arraycopy(currDist[j], 0, dist[j], 0, dist[j].length);
                            }
                        }

                        currSplit = attVal;
                    }

                    j = (int)instxx.classValue();
                    currDist[0][j] += instxx.weight();
                    currDist[1][j] -= instxx.weight();
                }
            }

            props[0] = new double[dist.length];

            for(i = 0; i < props[0].length; ++i) {
                props[0][i] = Utils.sum(dist[i]);
            }

            if (Utils.eq(Utils.sum(props[0]), 0.0D)) {
                for(i = 0; i < props[0].length; ++i) {
                    props[0][i] = 1.0D / (double)props[0].length;
                }
            } else {
                Utils.normalize(props[0]);
            }

            for(i = indexOfFirstMissingValue; i < data.numInstances(); ++i) {
                inst = data.instance(i);
                int jxx;
                if (attribute.isNominal()) {
                    if (inst.isMissing(att)) {
                        for(jxx = 0; jxx < dist.length; ++jxx) {
                            var10000 = dist[jxx];
                            var10001 = (int)inst.classValue();
                            var10000[var10001] += props[0][jxx] * inst.weight();
                        }
                    }
                } else {
                    for(jxx = 0; jxx < dist.length; ++jxx) {
                        var10000 = dist[jxx];
                        var10001 = (int)inst.classValue();
                        var10000[var10001] += props[0][jxx] * inst.weight();
                    }
                }
            }

            dists[0] = dist;
            return splitPoint;
        }

        protected double priorVal(double[][] dist) {
            return ContingencyTables.entropyOverColumns(dist);
        }

        protected double gain(double[][] dist, double priorVal) {
            return priorVal - ContingencyTables.entropyConditionedOnRows(dist);
        }

        public String getRevision() {
            return RevisionUtils.extract("$Revision: 15520 $");
        }

        protected int toGraph(StringBuffer text, int num, RandomTree2.Tree parent) throws Exception {
            ++num;
            if (this.m_Attribute == -1) {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + Utils.backQuoteChars(this.leafString()) + "\" shape=box]\n");
            } else {
                text.append("N" + Integer.toHexString(this.hashCode()) + " [label=\"" + num + ": " + Utils.backQuoteChars(RandomTree2.this.m_Info.attribute(this.m_Attribute).name()) + "\"]\n");

                for(int i = 0; i < this.m_Successors.length; ++i) {
                    text.append("N" + Integer.toHexString(this.hashCode()) + "->N" + Integer.toHexString(this.m_Successors[i].hashCode()) + " [label=\"");
                    if (RandomTree2.this.m_Info.attribute(this.m_Attribute).isNumeric()) {
                        if (i == 0) {
                            text.append(" < " + Utils.doubleToString(this.m_SplitPoint, RandomTree2.this.getNumDecimalPlaces()));
                        } else {
                            text.append(" >= " + Utils.doubleToString(this.m_SplitPoint, RandomTree2.this.getNumDecimalPlaces()));
                        }
                    } else {
                        text.append(" = " + Utils.backQuoteChars(RandomTree2.this.m_Info.attribute(this.m_Attribute).value(i)));
                    }

                    text.append("\"]\n");
                    num = this.m_Successors[i].toGraph(text, num, this);
                }
            }

            return num;
        }

        @Override
        public double[] leafScore(int rootdist) {

            //new
            double[] info = new double[7];
            info[0] = 0.0;
            info[1] = 0.0;
            info[2] = 0.0;
            info[3] = 0.0;
            info[4] = 0.0;
            info[5] = rootdist;
            info[6] = Double.MAX_VALUE;// fail distance
            if(this.m_Successors == null) { // leaf node, PASS/FAIL
                if (this.m_ClassDistribution != null) {
                    double dist_pass = this.m_ClassDistribution[0];
                    double dist_fail = this.m_ClassDistribution[1];
                    double total = dist_fail + dist_pass;
                    info[0] = dist_fail;
                    info[1] = dist_pass;
                    info[2] = total;
                    info[3] = 1 - (dist_fail/total) * (dist_fail/total) + (dist_pass/total) * (dist_pass/total); // calculate gini
                    info[4] = dist_fail > dist_pass ? 1.0 : 0.0;
                    info[5] = rootdist;
                    if(info[4]==1.0){
                        info[6] = 0;
                    }
                }
                return info;
            }
            for (int i=0; i<this.m_Successors.length; i++) {
                double[] childinfo = this.m_Successors[i].leafScore(rootdist+1);
                info[0] += childinfo[0];
                info[1] += childinfo[1];
                info[2] += childinfo[2];
            }
            for (int i=0; i<this.m_Successors.length; i++) {
                double[] childinfo = this.m_Successors[i].leafScore(rootdist+1);
                if (childinfo[2] == 0.0) {
                    continue;
                }
                info[3] += (childinfo[2] / info[2]) * (1 - (childinfo[0]/childinfo[2]) * (childinfo[0]/childinfo[2]) - (childinfo[1]/childinfo[2]) * (childinfo[1]/childinfo[2])); // calculate gini
                info[4] += childinfo[4];
                if (childinfo[6]==0) {
                    double newdist = Math.abs(info[5]-childinfo[5]);
                    if (info[6]>newdist) {
                        info[6] = newdist;
                    }
                }
            }

            String aName = RandomTree2.this.m_Info.attribute(this.m_Attribute).name();

            // gini
            double aScore = (1 - info[3]);

            // consider test number
            aScore *= Math.pow(info[2],0.5);

            // consider "FAIL" node
            //aScore *= (1 + info[4]);

            // consider branch number
            int branchNum = this.m_Successors.length;
            for (int i=0; i<this.m_Successors.length; i++) {
                double[] childinfo = this.m_Successors[i].leafScore(rootdist+1);
                if (childinfo[2] == 0.0) {
                    branchNum--;
                }
            }
            if (RandomTree2.this.m_Info.attribute(this.m_Attribute).isNominal() && branchNum > 2) {
                //aScore /= branchNum;
            }
            
            // consider fail node distance
            aScore /= info[6];

            // consider pdg score
            //aScore *= _attrScoreByName.get(_attrNameMap.get(aName));
            aScore += _attrScoreByName.get(_attrNameMap.get(aName));

            if(_reorderedAttrs.containsKey(aName)) {
                double oldscore = _reorderedAttrs.get(aName);
                if(oldscore<aScore){
                    _reorderedAttrs.put(aName, aScore);
                }
            } else {
                _reorderedAttrs.put(aName, aScore);
            }

            return info;

/*
            // original
            double[] result = new double[2];
            result[0] = 0.0;
            result[1] = 0.0;
            if(Double.isNaN(this.m_SplitPoint) && this.m_Attribute != -1 && RandomTree2.this.m_Info.attribute(this.m_Attribute).isNumeric()) {
                return result;
            }
            if(this.m_Attribute == -1 && this.m_ClassDistribution != null) {
                result[0] = Utils.sum(this.m_ClassDistribution);
                result[1] = result[0] - this.m_ClassDistribution[Utils.maxIndex(this.m_ClassDistribution)];
            } else {
                if(this.m_ClassDistribution != null) {
                    result[0] += this.m_ClassDistribution[0];
                    result[1] += this.m_ClassDistribution[1];
                }
                if(this.m_Successors != null) {
                    for(int i=0; i<this.m_Successors.length; i++) {
                        double[] tmp = this.m_Successors[i].leafScore(0);
                        //result[0] *= tmp[0] == 0 ? 1 : tmp[0];
                        //result[1] *= tmp[1] == 0 ? 1 : tmp[1];
                        result[0] += tmp[0];
                        result[1] += tmp[1];
                    }
                }
            }
            if(this.m_Attribute != -1) {
                String aName = RandomTree2.this.m_Info.attribute(this.m_Attribute).name();
                double aScore;
                if(result[0] == 0) {
                    aScore = 1;
                } else {
                    aScore = result[1] / result[0];
                }
                aScore = 0.5 * aScore + 0.5 * _attrScoreByName.get(_attrNameMap.get(aName));

                // consider number of covered test for each method
                //aScore *= Math.pow(_testScale, 0.5);

                _reorderedAttrs.put(aName, aScore);
            }
            return result;
*/
        }

        @Override
        public Map<String, Double> reorder() {
            if(this == null) {
                return _reorderedAttrs;
            }
            leafScore(0);
            return _reorderedAttrs;
        }
    }

    private abstract class DT {
        public abstract double[] leafScore(int rootDist);
        public abstract Map<String, Double> reorder();
    }
}

