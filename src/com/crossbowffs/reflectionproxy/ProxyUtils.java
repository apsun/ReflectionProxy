package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Proxy;

/* package */ final class ProxyUtils {
    private ProxyUtils() { }

    public static Object getProxyTarget(ProxyBase proxy) {
        return getInvocationHandler(proxy).getTarget();
    }

    public static Class<?> getTargetClass(Class<?> proxyClass) {
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

    public static Object coerceInput(Class<?> expectedType, Object value) {
        if (value == null || expectedType.isAssignableFrom(value.getClass())) {
            return value;
        } else if (expectedType.isPrimitive()) {
            checkPrimitiveType(expectedType, value);
            return value;
        } else if (value instanceof ProxyBase) {
            Object rawValue = getProxyTarget((ProxyBase)value);
            if (expectedType.isAssignableFrom(rawValue.getClass())) {
                return rawValue;
            }
        }
        throw new ProxyException(value.getClass().getName() +
            " cannot be converted to " + expectedType.getName());
    }

    @SuppressWarnings("unchecked")
    public static Object coerceOutput(Class<?> expectedType, Object value, ProxyBase inputProxy) {
        if (value == null || expectedType.isAssignableFrom(value.getClass())) {
            return value;
        } else if (expectedType.isPrimitive()) {
            checkPrimitiveType(expectedType, value);
            return value;
        } else if (ProxyBase.class.isAssignableFrom(expectedType)) {
            ProxyBase proxy;
            // Reuse the current proxy object if the target method uses `return this;`
            // This reduces object allocations when performing method cascading (builder pattern)
            if (inputProxy != null && value == getProxyTarget(inputProxy)) {
                proxy = inputProxy;
            } else {
                proxy = ProxyFactory.createProxy((Class<? extends ProxyBase>)expectedType, value);
            }
            if (expectedType.isAssignableFrom(proxy.getClass())) {
                return proxy;
            }
        }
        throw new ProxyException(value.getClass().getName() +
            " cannot be converted to " + expectedType.getName());
    }

    public static void coerceArgs(Class<?>[] argTypes, Object[] args) {
        if (args == null) {
            return;
        }
        for (int i = 0; i < args.length; ++i) {
            args[i] = coerceInput(argTypes[i], args[i]);
        }
    }

    public static void coerceArgTypes(Class<?>[] argTypes) {
        for (int i = 0; i < argTypes.length; ++i) {
            Class<?> argType = argTypes[i];
            if (ProxyBase.class.isAssignableFrom(argType)) {
                argTypes[i] = getTargetClass(argType);
            }
        }
    }

    public static boolean isCoercibleInput(Class<?> maybeProxyType, Class<?> actualType) {
        if (actualType.isAssignableFrom(maybeProxyType)) {
            return true;
        }
        if (ProxyBase.class.isAssignableFrom(maybeProxyType)) {
            Class<?> rawType = getTargetClass(maybeProxyType);
            if (actualType.isAssignableFrom(rawType)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isCoercibleOutput(Class<?> maybeProxyType, Class<?> actualType) {
        if (maybeProxyType.isAssignableFrom(actualType)) {
            return true;
        }
        if (ProxyBase.class.isAssignableFrom(maybeProxyType)) {
            Class<?> rawType = getTargetClass(maybeProxyType);
            if (rawType.isAssignableFrom(actualType)) {
                return true;
            }
        }
        return false;
    }

    private static ReflectionInvocationHandler getInvocationHandler(ProxyBase proxy) {
        return (ReflectionInvocationHandler)Proxy.getInvocationHandler(proxy);
    }

    private static void throwIfPrimitiveTypeMismatch(Class<?> primitiveType, Class<?> expectedType, Object value) {
        if (expectedType != value.getClass()) {
            throw new ProxyException("Cannot convert primitive value of type " +
                value.getClass().getName() + " to " + primitiveType.getName());
        }
    }

    private static void checkPrimitiveType(Class<?> primitiveType, Object value) {
        if (int.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Integer.class, value);
        } else if (boolean.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Boolean.class, value);
        } else if (double.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Double.class, value);
        } else if (float.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Float.class, value);
        } else if (long.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Long.class, value);
        } else if (byte.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Byte.class, value);
        } else if (char.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Character.class, value);
        } else if (short.class == primitiveType) {
            throwIfPrimitiveTypeMismatch(primitiveType, Short.class, value);
        }
    }
}
