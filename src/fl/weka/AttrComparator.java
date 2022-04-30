package fl.weka;

import java.util.Comparator;

/**
 * @description:
 * @author:
 * @time: 2021/8/16 0:32
 */
public class AttrComparator implements Comparator<AttributeStatistic> {
    @Override
    public int compare(AttributeStatistic o1, AttributeStatistic o2) {
        double diff = o2._score - o1._score;
        if(diff == 0) {
            return 0;
        }
        if(diff > 0 ) {
            return 1;
        }
        else {
            return -1;
        }
    }
}
