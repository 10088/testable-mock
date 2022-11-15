package com.alibaba.testable.core.util;

import com.alibaba.testable.core.compile.InMemoryJavaCompiler;
import com.alibaba.testable.core.tool.OmniConstructor;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import static com.alibaba.testable.core.constant.ConstPool.DOLLAR;
import static com.alibaba.testable.core.constant.ConstPool.DOT;

public class ConstructionUtil {

    private static final String TESTABLE_IMPL = "$TestableImpl";

    public static <T> T generateSubClassOf(Class<T> clazz) throws InstantiationException {
        StringBuilder sourceCode = new StringBuilder();
        sourceCode.append("package ").append(clazz.getPackage().getName()).append(";\n")
                .append("public class ").append(getSubclassName(clazz));
        appendTypeParameters(sourceCode, clazz.getTypeParameters(), true);
        sourceCode.append(clazz.isInterface() ? " implements " : " extends ")
                .append(getClassName(clazz));
        appendTypeParameters(sourceCode, clazz.getTypeParameters(), false);
        sourceCode.append(" {\n");
        for (Method m : clazz.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers()) && !Modifier.isFinal(m.getModifiers())) {
                sourceCode.append("\tpublic ");
                appendTypeParameters(sourceCode, m.getTypeParameters(), true);
                sourceCode.append(getClassName(m.getGenericReturnType())).append(" ")
                        .append(m.getName()).append("(");
                Type[] parameters = m.getGenericParameterTypes();
                for (int i = 0; i < parameters.length; i++) {
                    sourceCode.append(getParameterName(parameters[i])).append(" p").append(i);
                    if (i < parameters.length - 1) {
                        sourceCode.append(", ");
                    }
                }
                sourceCode.append(") {\n");
                if (!m.getReturnType().equals(void.class)) {
                    sourceCode.append("\t\treturn (").append(getClassName(m.getGenericReturnType())).append(") ")
                            .append(getClassName(OmniConstructor.class)).append(".")
                            .append("newInstance(").append(getClassName(m.getReturnType())).append(".class);\n");
                }
                sourceCode.append("\t}\n");
            }
        }
        sourceCode.append("}");

        try {
            return (T) InMemoryJavaCompiler.newInstance()
                    .useParentClassLoader(clazz.getClassLoader())
                    .useOptions("-Xlint:unchecked")
                    .ignoreWarnings()
                    .compile(clazz.getPackage().getName() + DOT + getSubclassName(clazz), sourceCode.toString())
                    .newInstance();
        } catch (Exception e) {
            throw new InstantiationException(e.toString());
        }
    }

    private static void appendTypeParameters(StringBuilder sourceCode, TypeVariable<?>[] typeParameters, boolean withScope) {
        if (typeParameters.length > 0) {
            sourceCode.append("<");
            for (int i = 0; i < typeParameters.length; i++) {
                sourceCode.append(typeParameters[i].getName());
                if (withScope) {
                    sourceCode.append(" extends ");
                    Type[] bounds = typeParameters[i].getBounds();
                    for (int j = 0; j < bounds.length; j++) {
                        sourceCode.append(getClassName(bounds[j]));
                        if (j < bounds.length - 1) {
                            sourceCode.append(" & ");
                        }
                    }
                }
                if (i < typeParameters.length - 1) {
                    sourceCode.append(", ");
                }
            }
            sourceCode.append("> ");
        }
    }

    private static String getClassName(Type clazz) {
        if (clazz instanceof Class) {
            return ((Class<?>)clazz).getName().replace(DOLLAR, DOT);
        }
        return clazz.toString();
    }

    private static String getParameterName(Type parameter) {
        if (parameter instanceof Class && ((Class<?>)parameter).isArray()) {
            return getParameterName(((Class<?>)parameter).getComponentType()) + "[]";
        }
        return getClassName(parameter);
    }

    private static String getSubclassName(Class<?> clazz) {
        return clazz.getSimpleName() + TESTABLE_IMPL;
    }

}
