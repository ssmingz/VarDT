package pda.core.slice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import fl.utils.Constant;
import pda.common.java.D4jSubject;
import pda.common.utils.JavaFile;

public class SlicerMain {
    public static MethodDeclaration _CURRENT_METHOD_DECL = null;
    public static void main(String[] args) {
        String base = args[0];
        String pid = args[1];
        int bid = Integer.valueOf(args[2]).intValue();
        String traceDir = args[3];
        String tracelineList = args[4];
        String output = args[5];
        D4jSubject subject = new D4jSubject(base, pid, bid);
        Slicer slicer = new Slicer(subject, traceDir);
        List<String> newlinelist = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(tracelineList));
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String newline = "";
                String prefix = line.substring(0, line.lastIndexOf(":"));
                String clazz = line.substring(line.indexOf(":") + 1, line.indexOf("#"));
                if (clazz.contains("$"))
                    clazz = clazz.substring(0, clazz.indexOf("$"));
                String linestr = line.substring(line.lastIndexOf(":"));
                if (linestr.equals("?")) {
                    newline = newline + prefix + ":" + linestr;
                } else {
                    String[] linelist = linestr.split(",");
                    newline = newline + prefix + ":?";
                    String criterion = null;
                    _CURRENT_METHOD_DECL = null;
                    for (int i = linelist.length - 1; i > 0; i--) {
                        int line2check = Integer.valueOf(linelist[i]).intValue();
                        String projectpath = base + "/" + pid + "/" + pid + "_" + bid + "_buggy";
                        String varcounter_arg1 = projectpath;
                        String varcounter_arg2 = JavaFile.getSrcClasses(projectpath) + "/" + clazz.replaceAll("\\.", "/") + ".java";
                        int varcount = VarCounter.countVars(varcounter_arg1, varcounter_arg2, line2check);
                        if (varcount != 0) {
                            criterion = clazz + ":" + line2check;
                            break;
                        }
                    }
                    if(criterion == null) {
                        if(linelist.length-1>0) {
                            int lastline = Integer.valueOf(linelist[linelist.length-1]).intValue();
                            criterion = clazz + ":" + lastline;
                        }
                    }
                    if (criterion != null) {
                        List<String> result = slicer.slice(criterion);

                        // get paras lines
                        LinkedHashSet<Integer> plines = new LinkedHashSet();
                        if (_CURRENT_METHOD_DECL != null) {
                            CompilationUnit cu = (CompilationUnit) _CURRENT_METHOD_DECL.getRoot();
                            for(Object para : _CURRENT_METHOD_DECL.parameters()) {
                                int pline = cu.getLineNumber(((SingleVariableDeclaration) para).getStartPosition());
                                plines.add(pline);
                            }
                            for(Integer pline : plines) {
                                newline = newline + "," + pline;
                            }
                        } else {
                            System.out.println("[ERROR] no para-lines find in method : " + line + " " + pid + " " + bid);
                        }

                        Set<String> slices = new HashSet<>();
                        slices.addAll(plines.stream().map(x->x.toString()).collect(Collectors.toList()));
                        for (String l : result) {
                            if (l.trim().split(":")[0].equals(clazz))
                                slices.add(l.trim().split(":")[1]);
                        }
                        String crtline = criterion.substring(criterion.indexOf(':')+1);
                        if (!slices.contains(crtline)) {
                            slices.add(crtline);
                        }
                        for (String l : linestr.split(",")) {
                            if (slices.contains(l) && !newline.contains(l))
                                newline = newline + "," + l;
                        }
                    }
                }
                newlinelist.add(newline);
            }
        } catch (FileNotFoundException e) {
            System.out.println("[ERROR] traceLineByTopN.txt does not exist : " + pid + " " + bid);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        String resultStr = "";
        for (String nl : newlinelist) {
            resultStr = resultStr + nl + "\n";
            System.out.println(nl);
        }
        JavaFile.writeStringToFile(output, resultStr);
    }
}
