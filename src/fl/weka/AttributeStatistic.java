package fl.weka;

import java.util.Comparator;

/**
 * @description:
 * @author:
 * @time: 2021/8/15 13:40
 */
public class AttributeStatistic {
    String _name;
    String _content;
    double _gainRatio;
    double _correlation;
    double _isCorrelated;
    int _round;
    double _score;
    double _importance;
    int _totalRounds;

    boolean CONSIDER_EQUIVALENCE = false;
    boolean IS_FIRST_IN_EQUIVALENCE = false;

    public AttributeStatistic(String name, String content, double gainRatio, double correlation, double isCorrelated) {
        _name = name;
        _content = content;
        _gainRatio = gainRatio;
        _correlation = correlation;
        _isCorrelated = isCorrelated;
    }

    public AttributeStatistic(String name, String content, double score) {
        _name = name;
        _content = content;
        _score = score;
    }

    public String attr2line() {
        //return _name + " " + _content + " " + _score + " " + _gainRatio + " " + _correlation + " " + _isCorrelated + " " + "\n";
        //return _name + " " + _content + " " + _gainRatio + "\n";
        return _name + " " + _content + " " + _score +" ";
    }

    public void computeScore() {
        _importance = 1;

        // init
        _score = 1.0D;

        if(!Double.isNaN(_gainRatio) && !Double.isNaN(_correlation)) {
            _score = (0.8 * _gainRatio + 0.2 * _correlation);
        } else {
            _score = Double.MIN_VALUE;
        }
    }
}
