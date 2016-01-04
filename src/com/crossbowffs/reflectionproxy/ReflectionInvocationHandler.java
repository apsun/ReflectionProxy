package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/* package */ class ReflectionInvocationHandler implements InvocationHandler {
    private static final Map<Method, Method> sMethodCache = new HashMap<Method, Method>();
    private static final Map<Class<?>, Map<String, Field>> sFieldCache = new HashMap<Class<?>, Map<String, Field>>();
    private static final Method sSetFieldMethod;
    private static final Method sGetFieldMethod;
    private static final Method sGetTargetMethod;

    static {
        Class<?> proxyBaseExClass = ProxyBaseEx.class;
        try {
            sSetFieldMethod = proxyBaseExClass.getDeclaredMethod("_setField", String.class, Object.class);
            sSetFieldMethod.setAccessible(true);
            sGetFieldMethod = proxyBaseExClass.getDeclaredMethod("_getField", String.class, Class.class);
            sGetFieldMethod.setAccessible(true);
            sGetTargetMethod = proxyBaseExClass.getDeclaredMethod("_getTarget");
            sGetTargetMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    private final Class<?> mTargetClass;
    private final Object mTarget;

    public ReflectionInvocationHandler(Class<?> targetClass, Object target) {
        mTargetClass = targetClass;
        mTarget = target;
    }

    public Object getTarget() {
        return mTarget;
    }

    private static Object coerceInput(Class<?> expectedType, Object value) {
        if (value == null || expectedType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (value instanceof ProxyBase) {
            value = ProxyUtils.getProxyTarget((ProxyBase)value);
        }
        if (expectedType.isAssignableFrom(value.getClass())) {
            return value;
        }
        throw new ProxyException(value.getClass().getName() +
            " cannot be converted to " + expectedType.getName());
    }

    @SuppressWarnings("unchecked")
    private static Object coerceOutput(Class<?> expectedType, Object value) {
        if (value == null || expectedType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (ProxyBase.class.isAssignableFrom(expectedType)) {
            value = ProxyFactory.createProxy((Class<? extends ProxyBase>)expectedType, value);
        }
        if (expectedType.isAssignableFrom(value.getClass())) {
            return value;
        }
        throw new ProxyException(value.getClass().getName() +
            " cannot be converted to " + expectedType.getName());
    }

    private static Field findField(Class<?> targetClass, String fieldName) {
        try {
            return targetClass.getField(fieldName);
        } catch (NoSuchFieldException e1) {
            try {
                return targetClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e2) {
                throw new ProxyException("Could not find target field: " + fieldName, e2);
            }
        }
    }

    private static Method findMethod(Class<?> targetClass, String methodName, Class<?>[] argTypes) {
        try {
            return targetClass.getMethod(methodName, argTypes);
        } catch (NoSuchMethodException e1) {
            try {
                return targetClass.getDeclaredMethod(methodName, argTypes);
            } catch (NoSuchMethodException e2) {
                throw new ProxyException("Could not find target method: " + methodName, e2);
            }
        }
    }

    private static Field getTargetField(Class<?> targetClass, String fieldName) {
        Map<String, Field> classFieldCache = sFieldCache.get(targetClass);
        if (classFieldCache == null) {
            classFieldCache = new HashMap<String, Field>();
            sFieldCache.put(targetClass, classFieldCache);
        }

        Field targetField = classFieldCache.get(fieldName);
        if (targetField == null) {
            targetField = findField(targetClass, fieldName);
            targetField.setAccessible(true);
            classFieldCache.put(fieldName, targetField);
        }

        return targetField;
    }

    private static Method getTargetMethod(Class<?> targetClass, Method proxyMethod) {
        Method targetMethod = sMethodCache.get(proxyMethod);
        if (targetMethod == null) {
            String targetMethodName = proxyMethod.getName();
            Class<?>[] proxyMethodArgTypes = proxyMethod.getParameterTypes();
            Class<?>[] targetMethodArgTypes = new Class<?>[proxyMethodArgTypes.length];
            for (int i = 0; i < targetMethodArgTypes.length; ++i) {
                Class<?> argType = proxyMethodArgTypes[i];
                if (ProxyBase.class.isAssignableFrom(argType)) {
                    targetMethodArgTypes[i] = ProxyUtils.getTargetClass(argType);
                } else {
                    targetMethodArgTypes[i] = argType;
                }
            }

            targetMethod = findMethod(targetClass, targetMethodName, targetMethodArgTypes);
            targetMethod.setAccessible(true);
            sMethodCache.put(proxyMethod, targetMethod);
        }
        return targetMethod;
    }

    private static void unwrapArgs(Object[] args, Class<?>[] argTypes) {
        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                args[i] = coerceInput(argTypes[i], args[i]);
            }
        }
    }

    public void setField(String fieldName, Object value) {
        Field field = getTargetField(mTargetClass, fieldName);
        value = coerceInput(field.getType(), value);
        try {
            field.set(mTarget, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to set instance field on static proxy", e);
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T getField(String fieldName, Class<T> fieldType) {
        Field field = getTargetField(mTargetClass, fieldName);
        Object value;
        try {
            value = field.get(mTarget);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to get instance field on static proxy", e);
        }
        return (T)coerceOutput(fieldType, value);
    }

    private Object invokeMethod(Method targetMethod, Object[] args) {
        try {
            return targetMethod.invoke(mTarget, args);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (InvocationTargetException e) {
            throw new ProxyException("Target method threw an exception", e.getCause());
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to call instance method on static proxy", e);
        }
    }

    @Override
    public Object invoke(Object proxy, Method proxyMethod, Object[] args) {
        if (proxy instanceof ProxyBaseEx) {
            if (sSetFieldMethod.equals(proxyMethod)) {
                setField((String)args[0], args[1]);
                return null;
            } else if (sGetFieldMethod.equals(proxyMethod)) {
                return getField((String)args[0], (Class<?>)args[1]);
            } else if (sGetTargetMethod.equals(proxyMethod)) {
                return getTarget();
            }
        }

        Method targetMethod = getTargetMethod(mTargetClass, proxyMethod);
        unwrapArgs(args, targetMethod.getParameterTypes());
        Object returnValue = invokeMethod(targetMethod, args);
        Class<?> returnType = proxyMethod.getReturnType();
        return coerceOutput(returnType, returnValue);
    }
}
