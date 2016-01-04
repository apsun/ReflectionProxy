package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/* package */ class ReflectionInvocationHandler implements InvocationHandler {
    private static class FieldAccessor {
        private final Field mField;
        private final boolean mIsGetter;
        private final Class<?> mExpectedType;

        private FieldAccessor(Field field, boolean isGetter, Class<?> expectedType) {
            mField = field;
            mIsGetter = isGetter;
            mExpectedType = expectedType;
        }
    }

    private static final Map<Method, FieldAccessor> sFieldAccessorCache = new HashMap<Method, FieldAccessor>();
    private static final Map<Method, Method> sMethodCache = new HashMap<Method, Method>();

    private final Class<?> mTargetClass;
    private final Object mTarget;

    /* package */ ReflectionInvocationHandler(Class<?> targetClass, Object target) {
        mTargetClass = targetClass;
        mTarget = target;
    }

    public Object getTarget() {
        return mTarget;
    }

    private static void throwIfTypeMismatch(Class<?> primitiveType, Class<?> expectedType, Object value) {
        if (expectedType != value.getClass()) {
            throw new ProxyException("Cannot convert primitive value of type " +
                value.getClass().getName() + " to " + primitiveType.getName());
        }
    }

    private static void checkPrimitiveType(Class<?> primitiveType, Object value) {
        if (int.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Integer.class, value);
        } else if (boolean.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Boolean.class, value);
        } else if (double.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Double.class, value);
        } else if (float.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Float.class, value);
        } else if (long.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Long.class, value);
        } else if (byte.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Byte.class, value);
        } else if (char.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Character.class, value);
        } else if (short.class == primitiveType) {
            throwIfTypeMismatch(primitiveType, Short.class, value);
        }
    }

    private static Object coerceInput(Class<?> expectedType, Object value) {
        if (value == null || expectedType.isAssignableFrom(value.getClass())) {
            return value;
        }
        if (expectedType.isPrimitive()) {
            checkPrimitiveType(expectedType, value);
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
        if (expectedType.isPrimitive()) {
            checkPrimitiveType(expectedType, value);
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

    private static void unwrapArgs(Object[] args, Class<?>[] argTypes) {
        if (args != null) {
            for (int i = 0; i < args.length; ++i) {
                args[i] = coerceInput(argTypes[i], args[i]);
            }
        }
    }

    private static boolean isCoercibleInput(Class<?> maybeProxyType, Class<?> actualType) {
        if (actualType.isAssignableFrom(maybeProxyType)) {
            return true;
        }
        if (ProxyBase.class.isAssignableFrom(maybeProxyType)) {
            Class<?> rawType = ProxyUtils.getTargetClass(maybeProxyType);
            if (actualType.isAssignableFrom(rawType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isCoercibleOutput(Class<?> maybeProxyType, Class<?> actualType) {
        if (maybeProxyType.isAssignableFrom(actualType)) {
            return true;
        }
        if (ProxyBase.class.isAssignableFrom(maybeProxyType)) {
            Class<?> rawType = ProxyUtils.getTargetClass(maybeProxyType);
            if (rawType.isAssignableFrom(actualType)) {
                return true;
            }
        }
        return false;
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

    private static FieldAccessor createAccessorInfo(Class<?> targetClass, Method proxyMethod, ProxyField fieldAnnotation) {
        String fieldName = fieldAnnotation.value();
        if ("".equals(fieldName)) {
            String methodName = proxyMethod.getName();
            if (methodName.startsWith("set_") || methodName.startsWith("get_")) {
                fieldName = methodName.substring(4);
            } else {
                throw new ProxyException("Could not automatically determine " +
                    "field name from method signature: " + proxyMethod.toString());
            }
        }

        Field field = findField(targetClass, fieldName);
        field.setAccessible(true);
        Class<?> fieldType = field.getType();
        Class<?> returnType = proxyMethod.getReturnType();
        Class<?>[] argTypes = proxyMethod.getParameterTypes();
        Class<?> expectedFieldType;

        if (returnType == void.class && argTypes.length == 1) {
            expectedFieldType = argTypes[0];
            if (isCoercibleInput(expectedFieldType, fieldType)) {
                return new FieldAccessor(field, false, expectedFieldType);
            }
        } else if (returnType != void.class && argTypes.length == 0) {
            expectedFieldType = returnType;
            if (isCoercibleOutput(expectedFieldType, fieldType)) {
                return new FieldAccessor(field, true, expectedFieldType);
            }
        } else {
            throw new ProxyException("Invalid field accessor signature: " + proxyMethod.toString());
        }

        throw new ProxyException("Field type mismatch (expected " +
            expectedFieldType.getName() + ", got " + field.getType().getName() + ")");
    }

    private static FieldAccessor getAccessorInfo(Class<?> targetClass, Method proxymethod) {
        FieldAccessor accessor = sFieldAccessorCache.get(proxymethod);
        if (accessor == null) {
            ProxyField fieldAnnotation = proxymethod.getAnnotation(ProxyField.class);
            if (fieldAnnotation != null) {
                accessor = createAccessorInfo(targetClass, proxymethod, fieldAnnotation);
                sFieldAccessorCache.put(proxymethod, accessor);
            }
        }
        return accessor;
    }

    private void setFieldValue(Field field, Object value) {
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
    private <T> T getFieldValue(Field field, Class<T> expectedType) {
        Object value;
        try {
            value = field.get(mTarget);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to get instance field on static proxy", e);
        }
        return (T)coerceOutput(expectedType, value);
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
        FieldAccessor accessorInfo = getAccessorInfo(mTargetClass, proxyMethod);
        if (accessorInfo != null) {
            if (accessorInfo.mIsGetter) {
                return getFieldValue(accessorInfo.mField, accessorInfo.mExpectedType);
            } else {
                setFieldValue(accessorInfo.mField, args[0]);
                return null;
            }
        }

        Method targetMethod = getTargetMethod(mTargetClass, proxyMethod);
        unwrapArgs(args, targetMethod.getParameterTypes());
        Object returnValue = invokeMethod(targetMethod, args);
        Class<?> returnType = proxyMethod.getReturnType();
        return coerceOutput(returnType, returnValue);
    }
}
