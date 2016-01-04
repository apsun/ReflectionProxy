package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Proxy;

public final class ProxyUtils {
    private ProxyUtils() { }

    /**
     * Gets the object that the specified proxy delegates to.
     *
     * If using {@link ProxyBaseEx}, this is equivalent to calling {@link ProxyBaseEx#_getTarget()}.
     * Generally, you should use {@link ProxyBaseEx}; this method is only provided in case there
     * is a naming conflict with the target class.
     *
     * @param proxy The proxy object.
     * @see ProxyBaseEx#_getTarget()
     */
    public static Object getProxyTarget(ProxyBase proxy) {
        return getInvocationHandler(proxy).getTarget();
    }

    /**
     * Sets the value of the specified field on the proxy target. If {@code value} is a proxy
     * object and the target field cannot be directly assigned to {@code value}, the field will
     * be set to the proxy's delegate object (see {@link #getProxyTarget(ProxyBase)}).
     *
     * If using {@link ProxyBaseEx}, this is equivalent to calling {@link ProxyBaseEx#_setField(String, Object)}.
     * Generally, you should use {@link ProxyBaseEx}; this method is only provided in case there
     * is a naming conflict with the target class.
     *
     * @param proxy The proxy object.
     * @param fieldName The name of the field to set.
     * @param value The value to set the field to.
     * @see ProxyBaseEx#_setField(String, Object)
     */
    public static void setProxyField(ProxyBase proxy, String fieldName, Object value) {
        getInvocationHandler(proxy).setField(fieldName, value);
    }

    /**
     * Gets the value of the specified field on the proxy target. If the value of the field
     * cannot be directly assigned to {@code fieldType} and {@code fieldType} extends
     * {@link ProxyBase}, a proxy object will be returned (see {@link ProxyFactory#createProxy(Class, Object)}).
     *
     * If using {@link ProxyBaseEx}, this is equivalent to calling {@link ProxyBaseEx#_getField(String, Class)}.
     * Generally, you should use {@link ProxyBaseEx}; this method is only provided in case there
     * is a naming conflict with the target class.
     *
     * @param proxy The proxy object.
     * @param fieldName The name of the field to get.
     * @param fieldType The expected type of the field.
     * @see ProxyBaseEx#_getField(String, Class)
     */
    public static <T> T getProxyField(ProxyBase proxy, String fieldName, Class<T> fieldType) {
        return getInvocationHandler(proxy).getField(fieldName, fieldType);
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
