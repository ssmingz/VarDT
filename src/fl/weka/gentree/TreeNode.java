package fl.weka.gentree;

import java.util.ArrayList;
import java.util.List;

public class TreeNode {
    public int id;
    public int pid = -1; // parent id, root if -1
    public int rid; // root id
    public String name;
    public double score;
    public NodeInfo info;
    public List<TreeNode> children = new ArrayList<>();

    public TreeNode(int id, int pid, String name, double[] info, double score) {
        this.id = id;
        this.pid = pid;
        this.name = name;
        this.info = new NodeInfo(info[0],info[1],info[2],info[3],info[4]);
        this.score = score;
    }

    public void setChild(TreeNode child) {
        this.children.add(child);
    }

    public void setChildren(List<TreeNode> children) {
        this.children.addAll(children);
    }

    public void setRoot(int rid) {
        this.rid = rid;
    }

    public boolean isRoot() {
        if (this.pid == -1) {
            return true;
        }
        return false;
    }

    public int getParentId() {
        return this.pid;
    }

    public int getId() {
        return this.id;
    }
}