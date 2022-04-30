package fl.weka.gentree;

public class NodeInfo {
    public double dist_fail;
    public double dist_pass;
    public double total;
    public double giniScore;
    public double failNodeNum;   // represent number of "FAIL" node

    public NodeInfo(double fail, double pass, double total, double giniScore, double failSize) {
        this.dist_fail = fail;
        this.dist_pass = pass;
        this.total = total;
        this.giniScore = giniScore;
        this.failNodeNum = failSize;
    }
}