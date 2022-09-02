/**
 * Copyright (C) . - All Rights Reserved.
 * Unauthorized copying of this file via any medium is
 * strictly prohibited Proprietary and Confidential.
 * Written by .
 */

package pda.common.utils;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * @author:
 * @date: 2021/11/2
 */
public class Method {

    private boolean _isTest = false;
    private String _clazz;
    private String _retType;
    private String _name = "DUMMY";
    private List<String> _argTypes;
    private MethodDeclaration _methodDecl;

    public Method(String clazz, String retType, String name, List<String> args) {
        _clazz = clazz;
        _retType = retType;
        _name = name;
        _argTypes = args;
    }

    public Method(MethodDeclaration method) {
        _methodDecl = method;
        List<SingleVariableDeclaration> args = new LinkedList<>();
        if (method != null) {
            _retType = method.getReturnType2() == null ? null : method.getReturnType2().toString();
            _name = method.getName().getIdentifier();
            args = method.parameters();
        }
        _argTypes = new ArrayList<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            _argTypes.add(args.get(i).getType().toString());
        }
    }

    public MethodDeclaration getMethodDecl() {
        return _methodDecl;
    }

    public void setIsTest(boolean isTest) {
        _isTest = isTest;
    }

    public boolean isTest() {
        return _isTest;
    }

    public void setClazz(String clazz) {
        _clazz = clazz;
    }

    public String getClazz() {
        return _clazz;
    }

    public String getRetType() {
        return _retType;
    }

    public String getName() {
        return _name;
    }

    public List<String> getArgTypes() {
        return _argTypes;
    }

    public boolean argTypeSame(MethodDeclaration method) {
        List<SingleVariableDeclaration> args = method.parameters();
        if (args.size() != _argTypes.size()) {
            return false;
        }
        for (int i = 0; i < _argTypes.size(); i++) {
            if (!_argTypes.get(i).equals(args.get(i).getType().toString())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return _retType + " " + _name + _argTypes.toString();
    }

    public static Method parse(String string) {
        String[] info = string.split("#");
        if (info.length != 4) {
            LevelLogger.error("Method info format error : " + string);
            return null;
        }
        String clazz = info[0].trim();
        String name = info[2].trim();
        String retType = "null".equals(info[1].trim()) ? null : info[1].trim();
        List<String> args = new LinkedList<>();
        String[] arguments = info[3].split(",");
        for (int i = 1; i < arguments.length; i++) {
            args.add(arguments[i].trim());
        }
        return new Method(clazz, retType, name, args);

    }

    public boolean same(MethodDeclaration method) {
        String retType = method.getReturnType2() == null ? null : method.getReturnType2().toString();
        String name = method.getName().getIdentifier();
        if (Utils.safeStringEqual(_retType, retType) && _name.equals(name)) {
            List<SingleVariableDeclaration> args = method.parameters();
            if (args.size() != _argTypes.size()) {
                return false;
            }
            for (int i = 0; i < _argTypes.size(); i++) {
                if (!_argTypes.get(i).equals(args.get(i).getType().toString())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return _name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof Method)) {
            return false;
        }
        Method m = (Method) obj;
        List<String> args = m.getArgTypes();
        if (Utils.safeStringEqual(_retType, m.getRetType()) && _name.equals(m.getName())
                && _argTypes.size() == args.size()) {
            for (int i = 0; i < _argTypes.size(); i++) {
                if (!Utils.safeStringEqual(_argTypes.get(i), args.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
}
