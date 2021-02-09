package com.alibaba.testable.core.util;

import java.util.Set;

/**
 * @author flin
 */
public class TestableUtil {

    /**
     * [0]Thread.getStackTrace() → [1]currentSourceMethodName() → [2]MockMethod -> [3]SourceMethod
     */
    private static final int INDEX_OF_SOURCE_METHOD = 3;
    /**
     * [0]Thread.getStackTrace() → [1]previousStackLocation() → [2]Invoker -> [3]Caller of invoker
     */
    private static final int INDEX_OF_CALLER_METHOD = 3;

    /**
     * Get the last visit method in source file
     * @return method name
     */
    public static String currentSourceMethodName() {
        return Thread.currentThread().getStackTrace()[INDEX_OF_SOURCE_METHOD].getMethodName();
    }

    /**
     * Get current test case method
     * @param testClassName name of current test class
     * @return method name
     */
    public static String currentTestCaseName(String testClassName) {
        // try current thread
        String testCaseName = findFirstMethodFromTestClass(testClassName, Thread.currentThread().getStackTrace());
        if (testCaseName.isEmpty()) {
            Set<Thread> threads = Thread.getAllStackTraces().keySet();
            // travel all possible threads
            for (Thread t : threads) {
                testCaseName = findFirstMethodFromTestClass(testClassName, t.getStackTrace());
                if (!testCaseName.isEmpty()) {
                    return testCaseName;
                }
            }
        }
        return testCaseName;
    }

    /**
     * Get file name and line number of where current method was called
     * @return in "filename:linenumber" format
     */
    public static String previousStackLocation() {
        StackTraceElement stack = Thread.currentThread().getStackTrace()[INDEX_OF_CALLER_METHOD];
        return stack.getFileName() + ":" + stack.getLineNumber();
    }

    private static String findFirstMethodFromTestClass(String testClassName, StackTraceElement[] stack) {
        for (int i = stack.length - 1; i >= 0; i--) {
            if (getOuterClassName(stack[i].getClassName()).equals(testClassName)) {
                return stack[i].getClassName().indexOf('$') > 0 ?
                    // test case using async call
                    getMethodNameFromLambda(stack[i].getClassName()) :
                    // in case of lambda method
                    getMethodNameFromLambda(stack[i].getMethodName());
            }
        }
        return "";
    }

    private static String getMethodNameFromLambda(String originName) {
        int beginOfMethodName = originName.indexOf('$');
        if (beginOfMethodName < 0) {
            return originName;
        }
        int endOfMethodName = originName.indexOf('$', beginOfMethodName + 1);
        return originName.substring(beginOfMethodName + 1, endOfMethodName);
    }

    private static String getOuterClassName(String className) {
        int posOfInnerClass = className.indexOf('$');
        return posOfInnerClass > 0 ? className.substring(0, posOfInnerClass) : className;
    }

}
