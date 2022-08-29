package fl.weka;

import java.util.Comparator;

/**
 * @description:
 * @author:
 * @time: 2021/8/16 0:32
 */
public class EquiComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        int l1 = 0;
        int c1 = 0;
        int l2 = 0;
        int c2 = 0;
        if (o1.contains("/")) {
            l1 = Integer.parseInt(o1.substring(o1.lastIndexOf('-')+1).split("/")[0]);
            c1 = Integer.parseInt(o1.substring(o1.lastIndexOf('-')+1).split("/")[1]);
        }
        if (o2.contains("/")) {
            l2 = Integer.parseInt(o2.substring(o2.lastIndexOf('-')+1).split("/")[0]);
            c2 = Integer.parseInt(o2.substring(o2.lastIndexOf('-')+1).split("/")[1]);
        }
        double diffL= l1 - l2;
        double diffC = c1 - c2;
        if(diffL == 0) {
            if(diffC == 0) {
                return 0;
            }
            if(diffC > 0 ) {
                return 1;
            }
            else {
                return -1;
            }
        }
        if(diffL > 0 ) {
            return 1;
        }
        else {
            return -1;
        }
    }
}
