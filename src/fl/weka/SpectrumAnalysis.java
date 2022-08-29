package fl.weka;

import fl.utils.Constant;
import fl.utils.JavaFile;
import org.apache.commons.cli.*;

import java.io.File;

public class SpectrumAnalysis {
    public static void main(String[] args) {
        String[] pros = {"lang"};
        //String[] pros = Constant.PROJECT_BUG_IDS.keySet().toArray(new String[0]);

        for(String pro : pros) {
            int[] bugs = Constant.PROJECT_BUG_IDS.get(pro);
            //int[] bugs = {30};
            for(int j : bugs) {
                String values = String.format("./collected_values/%s/%s_%d/original_delSpace.txt",pro,pro,j);
                String matrix = String.format("./spectrum/%s/%s_%d",pro,pro,j);
                File input = new File(values);
                File output = new File(matrix);
                if(!input.exists()) {
                    continue;
                }
                if(!output.exists()) {
                    output.mkdirs();
                }
                matrix += "/coveredInfo.txt";
                output = new File(matrix);
                JavaFile.spectrum_analysis(input, output);
            }
        }
    }
}
