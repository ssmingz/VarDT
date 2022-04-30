package fl.weka.gentree;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TreeUtil {
    public static List<TreeNode> buildTree(List<TreeNode> nodes) {
        //获取所有pid=-1的根节点
        List<TreeNode> list = nodes.stream().filter(item -> item.getParentId() == -1).collect(Collectors.toList());
        //根据pid进行分组
        Map<Integer, List<TreeNode>> map = nodes.stream().collect(Collectors.groupingBy(TreeNode::getParentId));
        recursionBuildTree(list, map);
        return list;
    }

    public static void recursionBuildTree(List<TreeNode> roots, Map<Integer, List<TreeNode>> map) {
        for (TreeNode root : roots) {
            List<TreeNode> childList = map.get(root.getId());
            root.setChildren(childList);
            if (childList!=null && childList.size()>0) {
                recursionBuildTree(childList, map);
            }
        }
    }
}