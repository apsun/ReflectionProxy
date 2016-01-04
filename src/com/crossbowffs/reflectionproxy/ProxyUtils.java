package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Proxy;

public final class ProxyUtils {
    private ProxyUtils() { }

    /**
     * Gets the object that the specified proxy delegates to.
     *
     * @param proxy The proxy object.
     */
    public static Object getProxyTarget(ProxyBase proxy) {
        return getInvocationHandler(proxy).getTarget();
    }

    private static ReflectionInvocationHandler getInvocationHandler(ProxyBase proxy) {
        return (ReflectionInvocationHandler)Proxy.getInvocationHandler(proxy);
    }

    /* package */ static Class<?> getTargetClass(Class<?> proxyClass) {
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
