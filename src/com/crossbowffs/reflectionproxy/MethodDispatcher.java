package com.crossbowffs.reflectionproxy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* package */ class MethodDispatcher extends ProxyDispatcherBase {
    private final Method mMethod;
    private final Class<?>[] mArgTypes;
    private final Class<?> mExpectedReturnType;

    private MethodDispatcher(Method method, Class<?> expectedReturnType) {
        mMethod = method;
        mArgTypes = method.getParameterTypes();
        mExpectedReturnType = expectedReturnType;
    }

    @Override
    public Object handle(ProxyBase proxy, Object target, Object[] args) {
        ProxyUtils.coerceArgs(mArgTypes, args);
        Object returnValue;
        try {
            returnValue = mMethod.invoke(target, args);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new ProxyException("Target method threw an exception", e.getCause());
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to call instance method on static proxy", e);
        }
        return ProxyUtils.coerceOutput(mExpectedReturnType, returnValue, proxy);
    }

    private static Method findMethod(Class<?> targetClass, String methodName, Method proxyMethod) {
        Class<?>[] argTypes = proxyMethod.getParameterTypes();
        ProxyUtils.coerceArgTypes(argTypes);
        try {
            return targetClass.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException e1) {
            try {
                return targetClass.getDeclaredMethod(methodName, argTypes);
            } catch (NoSuchMethodException e2) {
                throw new ProxyException("Could not find target method from signature: " + proxyMethod.toString(), e2);
            }
        }
    }

    public static MethodDispatcher create(Class<?> targetClass, Method proxyMethod, ProxyMethod annotation) {
        String targetMethodName = "";
        if (annotation != null) {
            targetMethodName = annotation.value();
        }
        if ("".equals(targetMethodName)) {
            targetMethodName = proxyMethod.getName();
        }
        Method targetMethod = findMethod(targetClass, targetMethodName, proxyMethod);
        Class<?> actualReturnType = targetMethod.getReturnType();
        Class<?> expectedReturnType = proxyMethod.getReturnType();
        if (!ProxyUtils.isCoercibleOutput(expectedReturnType, actualReturnType)) {
            throw new ProxyException("Target method return type (" + actualReturnType.getName() +
                ") cannot be converted to proxy method return type (" + expectedReturnType.getName() + ")");
        }
        targetMethod.setAccessible(true);
        return new MethodDispatcher(targetMethod, expectedReturnType);
    }
}
