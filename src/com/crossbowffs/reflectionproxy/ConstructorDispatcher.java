package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/* package */ class ConstructorDispatcher extends ProxyDispatcherBase {
    private final Constructor<?> mConstructor;
    private final Class<?>[] mArgTypes;
    private final Class<?> mExpectedType;

    private ConstructorDispatcher(Constructor<?> constructor, Class<?> expectedType) {
        mConstructor = constructor;
        mArgTypes = constructor.getParameterTypes();
        mExpectedType = expectedType;
    }

    @Override
    public Object handle(Object target, Object[] args) {
        ProxyUtils.coerceArgs(mArgTypes, args);
        Object newObject;
        try {
            newObject = mConstructor.newInstance(args);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InstantiationException e) {
            throw new ProxyException("Target class is an abstract class", e);
        } catch (InvocationTargetException e) {
            throw new ProxyException("Target method threw an exception", e.getCause());
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to call instance method on static proxy", e);
        }
        return ProxyUtils.coerceOutput(mExpectedType, newObject);
    }

    private static Constructor<?> findConstructor(Class<?> targetClass, Method proxyMethod) {
        Class<?>[] argTypes = proxyMethod.getParameterTypes();
        ProxyUtils.coerceArgTypes(argTypes);
        try {
            return targetClass.getConstructor(argTypes);
        } catch (NoSuchMethodException e1) {
            try {
                return targetClass.getDeclaredConstructor(argTypes);
            } catch (NoSuchMethodException e2) {
                throw new ProxyException("Could not find target constructor from signature: " + proxyMethod.toString(), e2);
            }
        } catch (SecurityException e) {
            throw new ProxyException("Cannot access constructor of class " + targetClass.getName());
        }
    }

    public static ConstructorDispatcher create(Class<?> targetClass, Method proxyMethod, ProxyConstructor annotation) {
        if (proxyMethod.getReturnType() == void.class) {
            throw new ProxyException("Proxy constructor cannot return void");
        }
        Class<?> expectedType = proxyMethod.getReturnType();
        if (!ProxyUtils.isCoercibleOutput(expectedType, targetClass)) {
            throw new ProxyException("Target type (" + targetClass.getName() +
                ") cannot be converted to proxy constructor return type (" + expectedType.getName() + ")");
        }
        Constructor<?> targetConstructor = findConstructor(targetClass, proxyMethod);
        targetConstructor.setAccessible(true);
        return new ConstructorDispatcher(targetConstructor, expectedType);
    }
}
