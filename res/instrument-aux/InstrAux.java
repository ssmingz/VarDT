package auxiliary;

import org.apache.log4j.Logger;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class InstrAux {
    public static LinkedHashMap recorder = new LinkedHashMap();
    public static LinkedHashSet recorder_filterTest = new LinkedHashSet();
    public static String TEST_NAME = "";
    public static String PID = "";
    public static String BID = "";
    public static List failedTest = new ArrayList();

    public static String FAIL_TESTNAME_PATH = "";

    public static void setTestName(String ftest_path, String testname, String pid, String bid, Logger logger) {
        FAIL_TESTNAME_PATH = ftest_path;
        TEST_NAME = testname;
        PID = pid;
        BID = bid;
        recorder = new LinkedHashMap();
        recorder_filterTest = new LinkedHashSet();
    }

    /**
     * get type
     */
    public static String getType(  Object o){
        if(o==null) { return ""; }
        String result=o.getClass().toString();
        return result.substring(result.indexOf(" ") + 1);
    }
    public static String getType(  int o){
        return "int";
    }
    public static String getType(  byte o){
        return "byte";
    }
    public static String getType(  char o){
        return "char";
    }
    public static String getType(  double o){
        return "double";
    }
    public static String getType(  float o){
        return "float";
    }
    public static String getType(  long o){
        return "long";
    }
    public static String getType(  boolean o){
        return "boolean";
    }
    public static String getType(  short o){
        return "short";
    }
    /**
     * traverse element for Collection object also collect length
     */
    public static void collectElements(  Collection target,  String name,  int lineNo, int colNo){
        if (target == null) {
            return;
        }
        Iterator itr=target.iterator();
        int index=0;
        updateRecorder(name + "{PRED}.size-" + lineNo + "/" + colNo, ""+target.size());
        if (target.size() > 10 && itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}["+index+"]", lineNo, colNo); // check null
            if(obj != null) {
                getValue(obj,name + "{PRED}[" + index+ "]",lineNo,colNo); // type and attributes
                elementRecursion(obj, name + "{PRED}[" + index+ "]",lineNo,colNo);
                String content_type=name + "{PRED}[" + index++ + "].TYPE-"+ lineNo + "/" + colNo;
                updateRecorder(content_type, getType(obj));
            }
            return;
        }
        while (itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}["+index+"]", lineNo, colNo);
            if(obj != null) {
                getValue(obj,name + "{PRED}[" + index+ "]",lineNo,colNo);
                elementRecursion(obj,name+"{PRED}["+index+"]", lineNo, colNo);
                String content_type=name + "{PRED}[" + index+++ "].TYPE-"+ lineNo + "/" + colNo;
                updateRecorder(content_type, getType(obj));
            }
        }
    }

    /**
     * traverse element for Map object only output keys directly in toString() style rather than in object style output type of keys also collect length ignore values
     */
    public static void collectElements(  Map target,  String name,  int lineNo, int colNo){
        if (target == null) {
            return;
        }
        Iterator itr=target.keySet().iterator();
        int index=0;
        updateRecorder(name + "{PRED}.size-" + lineNo + "/" + colNo, ""+target.size());
        if (target.size() > 10 && itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}.keys["+index+"]", lineNo,colNo);
            if(obj != null) {
                getValue(obj,name + "{PRED}.keys[" + index+ "]",lineNo,colNo);
                elementRecursion(obj,name+"{PRED}.keys["+index+"]", lineNo, colNo);
                String content_type=name + "{PRED}.keys" + "["+ index+++ "].TYPE-"+ lineNo + "/" + colNo;
                updateRecorder(content_type, getType(obj));
            }
            return;
        }
        while (itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}.keys["+index+"]", lineNo,colNo);
            if(obj != null) {
                getValue(obj,name + "{PRED}.keys[" + index+ "]",lineNo,colNo);
                elementRecursion(obj,name+"{PRED}.keys["+index+"]", lineNo, colNo);
                String content_type=name + "{PRED}.keys" + "["+ index+++ "].TYPE-"+ lineNo + "/" + colNo;
                updateRecorder(content_type, getType(obj));
            }
        }
    }
    /**
     * traverse element for array also collect length
     */
    public static void collectElements(  Object[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            checkNull(target[ii], name+"{PRED}["+ii+"]", lineNo, colNo);
            if(target[ii] != null) {
                elementRecursion(target[ii],name + "{PRED}[" + ii+ "]", lineNo, colNo);
                String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
                getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
                updateRecorder(content_type, getType(target[ii]));
            }
        }
    }
    public static void collectElements(  int[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  byte[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  char[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  long[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  short[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  float[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  double[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  boolean[] target,  String name,  int lineNo, int colNo){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo);
            updateRecorder(content_type, getType(target[ii]));
        }
    }

    public static void checkNull(Object obj, String name, int lineNo, int colNo) {
        if(obj == null) {
            updateRecorder(name+"{PRED}.isNULL-"+lineNo + "/" + colNo, "true");
        } else {
            updateRecorder(name+"{PRED}.isNULL-"+lineNo + "/" + colNo, "false");
        }
    }

    /**
     * get object type and value, only one-level expansion consider fields of obj, include private and inheritance return "" when obj is null also collect length when obj is Collection/Map/Array/String
     */
    public static Object getValue(  Object obj,  String name,  int lineNo, int colNo){
        // null check
        checkNull(obj, name, lineNo, colNo);
        if(obj == null) {
            return null;
        }
        // object's type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(obj));
        // predicates for Collection/Map/Array/String
        Class clazz=obj.getClass();
        boolean isCollection=Collection.class.isAssignableFrom(clazz);
        boolean isArray=clazz.isArray();
        boolean isMap=Map.class.isAssignableFrom(clazz);
        if (isArray) {
            String componentType=obj.getClass().getComponentType().getName();
            if (componentType.equals("int")) {
                collectElements((int[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("byte")) {
                collectElements((byte[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("float")) {
                collectElements((float[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("double")) {
                collectElements((double[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("long")) {
                collectElements((long[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("short")) {
                collectElements((short[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("boolean")) {
                collectElements((boolean[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("char")) {
                collectElements((char[])obj,name,lineNo,colNo);
            }
            else {
                collectElements((Object[])obj,name,lineNo,colNo);
            }
            return obj;
        }
        else if (isCollection) {
            collectElements((Collection)obj,name,lineNo,colNo);
            return obj;
        }
        else if (isMap) {
            collectElements((Map)obj,name,lineNo,colNo);
            return obj;
        }
        if (obj instanceof String) {
            updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo, ""+((String)obj).length());
            updateRecorder(name + "-" + lineNo + "/" + colNo, ""+unEscapeString((String) obj));
            return obj;
        }
        // fields
        ArrayList allField=new ArrayList();
        for (; clazz != Object.class; clazz=clazz.getSuperclass()) {
            Field[] fields=clazz.getDeclaredFields();
            allField.addAll(new ArrayList(Arrays.asList(fields)));
            for (int i=0; i < fields.length; i++) {
                Field f=fields[i];
                f.setAccessible(true);
                try {
                    String fname=name+"{PRED}."+f.getName();
                    if (f.get(obj) instanceof Object) {
                        checkNull(f.get(obj),fname,lineNo,colNo);
                        if (f.get(obj) instanceof String) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+unEscapeString((String) f.get(obj)));
                        } else if (f.get(obj) instanceof Double) {
                            updateRecorder(fname + "{PRED}.isNaN-" + lineNo + "/" + colNo, ""+Double.isNaN(((Double) f.get(obj)).doubleValue()));
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+((Double) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Float) {
                            updateRecorder(fname + "{PRED}.isNaN-" + lineNo + "/" + colNo, ""+Float.isNaN(((Float) f.get(obj)).floatValue()));
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+((Float) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Character) {
                            updateRecorder(fname + "{PRED}.ASCII-" + lineNo + "/" + colNo, ""+Character.getNumericValue(((Character) f.get(obj)).charValue()));
                        } else if (f.get(obj) instanceof Integer) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+((Integer) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Long) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+((Long) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Boolean) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+((Boolean) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Byte) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+((Byte) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Short) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo, ""+((Short) f.get(obj)).toString());
                        }
                        continue;
                    }
                    else {
                        getValue(f.get(obj),fname,lineNo,colNo);
                    }
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return obj;
    }

    public static void collectElements(  Collection target,  String name,  int lineNo, int colNo, int size){
        if (target == null) {
            return;
        }
        Iterator itr=target.iterator();
        int index=0;
        updateRecorder(name + "{PRED}.size-" + lineNo + "/" + colNo + "/" + size, ""+target.size());
        //logger.debug(name + ".size-" + lineNo+ ":"+ target.size());
        if (target.size() > 10 && itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}["+index+"]", lineNo, colNo, size); // check null
            if(obj != null) {
                getValue(obj,name + "{PRED}[" + index+ "]",lineNo,colNo,size); // type and attributes
                elementRecursion(obj,name+"{PRED}["+index+"]", lineNo, colNo, size);
                String content_type=name + "{PRED}[" + index++ + "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
                updateRecorder(content_type, getType(obj));
            }
            return;
        }
        while (itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}["+index+"]", lineNo, colNo, size);
            if(obj != null) {
                getValue(obj,name + "{PRED}[" + index+ "]",lineNo,colNo,size);
                elementRecursion(obj,name+"{PRED}["+index+"]", lineNo, colNo, size);
                String content_type=name + "{PRED}[" + index+++ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
                updateRecorder(content_type, getType(obj));
            }
        }
    }

    /**
     * traverse element for Map object only output keys directly in toString() style rather than in object style output type of keys also collect length ignore values
     */
    public static void collectElements(  Map target,  String name,  int lineNo, int colNo, int size){
        if (target == null) {
            return;
        }
        Iterator itr=target.keySet().iterator();
        int index=0;
        updateRecorder(name + "{PRED}.size-" + lineNo + "/" + colNo + "/" + size, ""+target.size());
        if (target.size() > 10 && itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}.keys["+index+"]", lineNo,colNo,size);
            if(obj != null) {
                getValue(obj,name + "{PRED}.keys[" + index+ "]",lineNo,colNo,size);
                elementRecursion(obj,name+"{PRED}.keys["+index+"]", lineNo, colNo, size);
                String content_type=name + "{PRED}.keys" + "["+ index+++ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
                updateRecorder(content_type, getType(obj));
            }
            return;
        }
        while (itr.hasNext()) {
            Object obj=itr.next();
            checkNull(obj, name+"{PRED}.keys["+index+"]", lineNo,colNo,size);
            if(obj != null) {
                getValue(obj,name + "{PRED}.keys[" + index+ "]",lineNo,colNo,size);
                elementRecursion(obj,name+"{PRED}.keys["+index+"]", lineNo, colNo, size);
                String content_type=name + "{PRED}.keys" + "["+ index+++ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
                updateRecorder(content_type, getType(obj));
            }
        }
    }
    /**
     * traverse element for array also collect length
     */
    public static void collectElements(  Object[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            checkNull(target[ii], name+"{PRED}["+ii+"]", lineNo, colNo, size);
            if(target[ii] != null) {
                elementRecursion(target[ii], name+"{PRED}["+ii+"]", lineNo, colNo, size);
                String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
                getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
                updateRecorder(content_type, getType(target[ii]));
            }
        }
    }

    public static void collectElements(  int[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  byte[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  char[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  long[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  short[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  float[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  double[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }
    public static void collectElements(  boolean[] target,  String name,  int lineNo, int colNo, int size){
        updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+target.length);
        for (int ii=0; ii < target.length && ii < 10; ii++) {
            String content_type=name + "{PRED}[" + ii+ "].TYPE-"+ lineNo + "/" + colNo + "/" + size;
            getValue(target[ii],name + "{PRED}[" + ii+ "]",lineNo,colNo,size);
            updateRecorder(content_type, getType(target[ii]));
        }
    }

    public static void checkNull(Object obj, String name, int lineNo, int colNo, int size) {
        if(obj == null) {
            updateRecorder(name+"{PRED}.isNULL-"+lineNo + "/" + colNo + "/" + size, "true");
        } else {
            updateRecorder(name+"{PRED}.isNULL-"+lineNo + "/" + colNo + "/" + size, "false");
        }
    }

    /**
     * get object type and value, only one-level expansion consider fields of obj, include private and inheritance return "" when obj is null also collect length when obj is Collection/Map/Array/String
     */
    public static Object getValue(  Object obj,  String name,  int lineNo, int colNo, int size){
        // null check
        checkNull(obj, name, lineNo, colNo, size);
        if(obj == null) {
            return null;
        }
        // object's type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(obj));
        // predicates for Collection/Map/Array/String
        Class clazz=obj.getClass();
        boolean isCollection=Collection.class.isAssignableFrom(clazz);
        boolean isArray=clazz.isArray();
        boolean isMap=Map.class.isAssignableFrom(clazz);
        if (isArray) {
            String componentType=obj.getClass().getComponentType().getName();
            if (componentType.equals("int")) {
                collectElements((int[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("byte")) {
                collectElements((byte[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("float")) {
                collectElements((float[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("double")) {
                collectElements((double[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("long")) {
                collectElements((long[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("short")) {
                collectElements((short[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("boolean")) {
                collectElements((boolean[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("char")) {
                collectElements((char[])obj,name,lineNo,colNo,size);
            }
            else {
                collectElements((Object[])obj,name,lineNo,colNo,size);
            }
            return obj;
        }
        else if (isCollection) {
            collectElements((Collection)obj,name,lineNo,colNo,size);
            return obj;
        }
        else if (isMap) {
            collectElements((Map)obj,name,lineNo,colNo,size);
            return obj;
        }
        if (obj instanceof String) {
            updateRecorder(name + "{PRED}.length-" + lineNo + "/" + colNo + "/" + size, ""+((String)obj).length());
            updateRecorder(name + "-" + lineNo + "/" + colNo + "/" + size, ""+unEscapeString((String) obj));
            return obj;
        }
        // fields
        ArrayList allField=new ArrayList();
        for (; clazz != Object.class; clazz=clazz.getSuperclass()) {
            Field[] fields=clazz.getDeclaredFields();
            allField.addAll(new ArrayList(Arrays.asList(fields)));
            for (int i=0; i < fields.length; i++) {
                Field f=fields[i];
                f.setAccessible(true);
                try {
                    String fname=name+"{PRED}."+f.getName();
                    if (f.get(obj) instanceof Object) {
                        checkNull(f.get(obj),fname,lineNo,colNo,size);
                        if (f.get(obj) instanceof String) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+unEscapeString((String) f.get(obj)));
                        } else if (f.get(obj) instanceof Double) {
                            updateRecorder(fname + "{PRED}.isNaN-" + lineNo + "/" + colNo + "/" + size, ""+Double.isNaN(((Double) f.get(obj)).doubleValue()));
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+((Double) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Float) {
                            updateRecorder(fname + "{PRED}.isNaN-" + lineNo + "/" + colNo + "/" + size, ""+Float.isNaN(((Float) f.get(obj)).floatValue()));
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+((Float) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Character) {
                            updateRecorder(fname + "{PRED}.ASCII-" + lineNo + "/" + colNo + "/" + size, ""+Character.getNumericValue(((Character) f.get(obj)).charValue()));
                        } else if (f.get(obj) instanceof Integer) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+((Integer) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Long) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+((Long) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Boolean) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+((Boolean) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Byte) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+((Byte) f.get(obj)).toString());
                        } else if (f.get(obj) instanceof Short) {
                            updateRecorder(fname + "-" + lineNo + "/" + colNo + "/" + size, ""+((Short) f.get(obj)).toString());
                        }
                        continue;
                    }
                    else {
                        getValue(f.get(obj),fname,lineNo,colNo,size);
                    }
                }
                catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return obj;
    }

    public static int getValue(  int o,  String name,  int lineNo, int colNo){
        String iden = name + "-" + lineNo + "/" + colNo;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }
    public static char getValue(  char o,  String name,  int lineNo, int colNo){
        String iden = name + "{PRED}.ASCII-" + lineNo + "/" + colNo;
        String content = "" + ((int)o);
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }
    public static boolean getValue(  boolean o,  String name,  int lineNo, int colNo){
        String iden = name + "-" + lineNo + "/" + colNo;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }
    public static byte getValue(  byte o,  String name,  int lineNo, int colNo){
        String iden = name + "-" + lineNo + "/" + colNo;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }
    public static double getValue(  double o,  String name,  int lineNo, int colNo){
        String iden = name + "-" + lineNo + "/" + colNo;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }
    public static float getValue(  float o,  String name,  int lineNo, int colNo){
        String iden = name + "-" + lineNo + "/" + colNo;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }
    public static long getValue(  long o,  String name,  int lineNo, int colNo){
        String iden = name + "-" + lineNo + "/" + colNo;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }
    public static short getValue(  short o,  String name,  int lineNo, int colNo){
        String iden = name + "-" + lineNo + "/" + colNo;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo, getType(o));
        return o;
    }

    public static int getValue(  int o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }
    public static char getValue(  char o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "{PRED}.ASCII-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + ((int)o);
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }
    public static boolean getValue(  boolean o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }
    public static byte getValue(  byte o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }
    public static double getValue(  double o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }
    public static float getValue(  float o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }
    public static long getValue(  long o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }
    public static short getValue(  short o,  String name,  int lineNo, int colNo, int size){
        String iden = name + "-" + lineNo + "/" + colNo + "/" + size;
        String content = "" + o;
        updateRecorder(iden, content);
        // record type
        updateRecorder(name + "{PRED}.TYPE-" + lineNo + "/" + colNo + "/" + size, getType(o));
        return o;
    }

    private static void updateRecorder(String iden, String content) {
        if(recorder.keySet().contains(iden)) {
            recorder.replace(iden, recorder.get(iden), content);
        } else {
            recorder.put(iden, content);
        }
    }
    public static void updateRecorder_filterTest(String methodName) {
        if(recorder_filterTest.contains(methodName)) {
            // ignore
        } else {
            recorder_filterTest.add(methodName);
        }
    }
    public static void write_filterTest(Logger logger) {
        StringBuffer buf = new StringBuffer();
        buf.append(TEST_NAME);
        Iterator itr = recorder_filterTest.iterator();
        while(itr.hasNext()){
            buf.append("\n" + itr.next());
        }
        logger.debug(buf.toString());

        String ftest_path = FAIL_TESTNAME_PATH + "/" + PID + "/" + BID + ".txt";
        File ftest = new File(ftest_path);
        if(!ftest.exists()) {
            logger.error("failed_tests_file not found : " + PID + " " + BID);
            return;
        }
        try {
            FileReader reader = new FileReader(ftest);
            BufferedReader bReader = new BufferedReader(reader);
            String line = "";
            while((line = bReader.readLine()) != null) {
                line = line.trim();
                if(!line.contains("::")) {
                    logger.error("failed_tests_file illegal format : " + PID + " " + BID);
                    continue;
                }
                String fname = line.split("::")[1];
                failedTest.add(fname);
            }
            bReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(failedTest.contains(TEST_NAME)) {
            logger.debug("FAIL");
        } else {
            logger.debug("PASS");
        }
    }
    public static void write(Logger logger) {
        StringBuffer buf = new StringBuffer();
        buf.append(TEST_NAME);
        Iterator itr = recorder.entrySet().iterator();
        while(itr.hasNext()){
            Map.Entry entry = (Map.Entry) itr.next();
            buf.append("\n" + entry.getKey()+ ":" + entry.getValue());
        }
        logger.debug(buf.toString());

        String ftest_path = FAIL_TESTNAME_PATH + "/" + PID + "/" + BID + ".txt";
        File ftest = new File(ftest_path);
        if(!ftest.exists()) {
            logger.error("failed_tests_file not found : " + PID + " " + BID);
            return;
        }
        try {
            FileReader reader = new FileReader(ftest);
            BufferedReader bReader = new BufferedReader(reader);
            String line = "";
            while((line = bReader.readLine()) != null) {
                line = line.trim();
                if(!line.contains("::")) {
                    logger.error("failed_tests_file illegal format : " + PID + " " + BID);
                    continue;
                }
                String fname = line.split("::")[1];
                failedTest.add(fname);
            }
            bReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(failedTest.contains(TEST_NAME)) {
            logger.debug("FAIL");
        } else {
            logger.debug("PASS");
        }
    }
    public static String unEscapeString(String s){
        StringBuilder sb = new StringBuilder();
        for (int i=0; i<s.length(); i++)
            switch (s.charAt(i)){
                case '\n': sb.append("\\n"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\r': sb.append("\\r"); break;
                case '\f': sb.append("\\f"); break;
                default: sb.append(s.charAt(i));
            }
        return sb.toString();
    }
    public static void elementRecursion(Object obj, String name, int lineNo, int colNo) {
        Class clazz=obj.getClass();
        boolean isCollection=Collection.class.isAssignableFrom(clazz);
        boolean isArray=clazz.isArray();
        boolean isMap=Map.class.isAssignableFrom(clazz);
        if (isArray) {
            String componentType=obj.getClass().getComponentType().getName();
            if (componentType.equals("int")) {
                collectElements((int[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("byte")) {
                collectElements((byte[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("float")) {
                collectElements((float[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("double")) {
                collectElements((double[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("long")) {
                collectElements((long[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("short")) {
                collectElements((short[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("boolean")) {
                collectElements((boolean[])obj,name,lineNo,colNo);
            }
            else if (componentType.equals("char")) {
                collectElements((char[])obj,name,lineNo,colNo);
            }
            else {
                collectElements((Object[])obj,name,lineNo,colNo);
            }
        }
        else if (isCollection) {
            collectElements((Collection)obj,name,lineNo,colNo);
        }
        else if (isMap) {
            collectElements((Map)obj,name,lineNo,colNo);
        }
    }
    public static void elementRecursion(Object obj, String name, int lineNo, int colNo, int size) {
        Class clazz=obj.getClass();
        boolean isCollection=Collection.class.isAssignableFrom(clazz);
        boolean isArray=clazz.isArray();
        boolean isMap=Map.class.isAssignableFrom(clazz);
        if (isArray) {
            String componentType=obj.getClass().getComponentType().getName();
            if (componentType.equals("int")) {
                collectElements((int[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("byte")) {
                collectElements((byte[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("float")) {
                collectElements((float[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("double")) {
                collectElements((double[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("long")) {
                collectElements((long[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("short")) {
                collectElements((short[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("boolean")) {
                collectElements((boolean[])obj,name,lineNo,colNo,size);
            }
            else if (componentType.equals("char")) {
                collectElements((char[])obj,name,lineNo,colNo,size);
            }
            else {
                collectElements((Object[])obj,name,lineNo,colNo,size);
            }
        }
        else if (isCollection) {
            collectElements((Collection)obj,name,lineNo,colNo,size);
        }
        else if (isMap) {
            collectElements((Map)obj,name,lineNo,colNo,size);
        }
    }
}
