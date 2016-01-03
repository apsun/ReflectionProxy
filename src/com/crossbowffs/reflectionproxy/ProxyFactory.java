package com.crossbowffs.reflectionproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.*;

public final class ProxyFactory {
    private static class MethodWrapper {
        private final Method mMethod;
        private final boolean mNeedsUnwrapping;

        public MethodWrapper(Method method, boolean needsUnwrapping) {
            mMethod = method;
            mNeedsUnwrapping = needsUnwrapping;
        }

        public Method getMethod() {
            return mMethod;
        }

        public boolean needsUnwrapping() {
            return mNeedsUnwrapping;
        }
    }

    private static class ReflectionInvocationHandler implements InvocationHandler {
        private static final Map<Method, MethodWrapper> sMethodCache = new HashMap<Method, MethodWrapper>();
        private final Object mTarget;
        private final Class<?> mTargetClass;

        public ReflectionInvocationHandler(Class<?> targetClass, Object target) {
            mTargetClass = targetClass;
            mTarget = target;
        }

        public Object getTarget() {
            return mTarget;
        }

        private Method findRawMethod(String methodName, Class<?>[] argTypes) {
            try {
                return mTargetClass.getMethod(methodName, argTypes);
            } catch (NoSuchMethodException e1) {
                try {
                    return mTargetClass.getDeclaredMethod(methodName, argTypes);
                } catch (NoSuchMethodException e2) {
                    throw new ProxyException("Could not find target method: " + methodName, e2);
                }
            }
        }

        private MethodWrapper getTargetMethod(Method proxyMethod) {
            MethodWrapper targetMethod = sMethodCache.get(proxyMethod);
            if (targetMethod == null) {
                String targetMethodName = proxyMethod.getName();
                Class<?>[] proxyMethodArgTypes = proxyMethod.getParameterTypes();
                Class<?>[] targetMethodArgTypes = new Class<?>[proxyMethodArgTypes.length];
                boolean needsUnwrapping = false;
                for (int i = 0; i < targetMethodArgTypes.length; ++i) {
                    Class<?> argType = proxyMethodArgTypes[i];
                    if (ProxyBase.class.isAssignableFrom(argType)) {
                        needsUnwrapping = true;
                        targetMethodArgTypes[i] = getTargetClass(argType);
                    } else {
                        targetMethodArgTypes[i] = argType;
                    }
                }
                Method rawTargetMethod = findRawMethod(targetMethodName, targetMethodArgTypes);
                rawTargetMethod.setAccessible(true);
                targetMethod = new MethodWrapper(rawTargetMethod, needsUnwrapping);
                sMethodCache.put(proxyMethod, targetMethod);
            }
            return targetMethod;
        }

        private Object[] unwrapArgs(Object[] args) {
            Object[] unwrappedArgs = new Object[args.length];
            for (int i = 0; i < args.length; ++i) {
                Object arg = args[i];
                if (arg instanceof ProxyBase) {
                    unwrappedArgs[i] = getProxyTarget((ProxyBase)arg);
                } else {
                    unwrappedArgs[i] = arg;
                }
            }
            return unwrappedArgs;
        }

        private Object invokeMethod(Method targetMethod, Object[] args) {
            try {
                return targetMethod.invoke(mTarget, args);
            } catch (IllegalAccessException e) {
                throw new AssertionError(e);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e.getCause());
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public Object invoke(Object proxy, Method proxyMethod, Object[] args) {
            MethodWrapper targetMethod = getTargetMethod(proxyMethod);
            if (targetMethod.needsUnwrapping()) {
                args = unwrapArgs(args);
            }
            Object returnValue = invokeMethod(targetMethod.getMethod(), args);
            Class<?> returnType = proxyMethod.getReturnType();
            if (ProxyBase.class.isAssignableFrom(returnType)) {
                return createProxy((Class<? extends ProxyBase>)returnType, returnValue);
            } else {
                return returnValue;
            }
        }
    }

    private ProxyFactory() { }

    @SuppressWarnings("unchecked")
    public static <T extends ProxyBase> T createProxy(Class<T> proxyClass, Object target) {
        Class<?> targetClass = getTargetClass(proxyClass);
        if (!targetClass.isAssignableFrom(target.getClass())) {
            throw new ProxyException("Proxy cannot be applied to target (expected " +
                targetClass.getName() + ", got " + target.getClass().getName() + ")");
        }

        ReflectionInvocationHandler invocationHandler = new ReflectionInvocationHandler(targetClass, target);
        return (T)Proxy.newProxyInstance(proxyClass.getClassLoader(), new Class<?>[] {proxyClass}, invocationHandler);
    }

    public static Object getProxyTarget(ProxyBase proxy) {
        ReflectionInvocationHandler invocationHandler = (ReflectionInvocationHandler)Proxy.getInvocationHandler(proxy);
        return invocationHandler.getTarget();
    }

    private static Class<?> getTargetClass(Class<?> proxyClass) {
        ProxyTarget annotation = proxyClass.getAnnotation(ProxyTarget.class);
        if (annotation != null) {
            return annotation.value();
        }

        ProxyTargetName altAnnotation = proxyClass.getAnnotation(ProxyTargetName.class);
        if (altAnnotation != null) {
            String className = altAnnotation.value();
            try {
                return Class.forName(className, true, proxyClass.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new ProxyException("No proxy target found with name: " + className, e);
            }
        }

        throw new ProxyException("No proxy target annotation found on class: " + proxyClass.getName());
    }
}
