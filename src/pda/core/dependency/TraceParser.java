package pda.core.dependency;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import pda.common.conf.Constant;
import pda.common.java.D4jSubject;
import pda.common.utils.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.sql.SQLOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TraceParser {
    private String resultFile;

    public TraceParser(String resultFile){
        this.resultFile = resultFile;
    }

    private Map<Integer, String> parseIdentifier(String identifierFilePath){
        Map<Integer, String> result = new HashMap<>();
        File identifierFile = new File(identifierFilePath);
        if (!identifierFile.exists()) {
            System.out.println("Can not find IdentifierFile in " + identifierFilePath);
            return null;
        }
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(identifierFile));
            String line = "";
            while ((line = bufferedReader.readLine()) != null){
                String[] array = line.split("\t");
                if (array.length != 2) {
                    System.out.println("Error Identifier Line: " + line);
                    continue;
                }
                String id = array[0];
                String method = array[1];
                result.put(Integer.parseInt(id), method);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return result;
    }

    public List<Pair<MethodDeclaration, String>> parse(D4jSubject subject){
        List<String> result = new ArrayList<>();
        String traceFilePath = resultFile + File.separator + "trace.out";
        String identifierFilePath = resultFile + File.separator + "identifier.txt";
        Map<Integer, String> identifiers = parseIdentifier(identifierFilePath);
        if (identifiers == null) return null;
        File traceFile = new File(traceFilePath);
        if (!traceFile.exists()) {
            System.out.println("Can not find traceFile in " + traceFilePath);
            return null;
        }
        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(traceFile));
            String line = "";
            while((line = bufferedReader.readLine()) != null){
                if (!line.contains("#")) continue;
                String [] array = line.split("#");
                if (array.length != 2){
                    System.out.println("Error trace line: " + line);
                    continue;
                }
                int methodId = Integer.parseInt(array[0]);
                String lineNo = array[1];
                if (identifiers.containsKey(methodId)){
                    String method = identifiers.get(methodId);
                    result.add(method + ":" + lineNo);
                }else {
                    System.err.println(line + " is not in identifier.txt!");
                    return null;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        System.out.println("Parsing method.... total: " + result.size());
        return ParseMethodFromString.parseMethodString(subject, result);
    }
}
