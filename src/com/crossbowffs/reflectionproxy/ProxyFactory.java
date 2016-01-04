package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Proxy;

public final class ProxyFactory {
    private ProxyFactory() { }

    /**
     * Creates a new reflection proxy using the specified proxy type and target object.
     *
     * @param proxyClass The proxy type. Must be annotated with {@link ProxyTarget} or {@link ProxyTargetName}.
     * @param target The target object. Cannot be null. If you want a static proxy, use {@link #createStaticProxy(Class)}.
     */
    public static <T extends ProxyBase> T createProxy(Class<T> proxyClass, Object target) {
        Class<?> targetClass = ProxyUtils.getTargetClass(proxyClass);
        if (!targetClass.isAssignableFrom(target.getClass())) {
            throw new ProxyException("Proxy cannot be applied to target (expected " +
                targetClass.getName() + ", got " + target.getClass().getName() + ")");
        }
        return createProxyInternal(proxyClass, targetClass, target);
    }

    /**
     * Creates a new static reflection proxy using the specified proxy type. The proxy object
     * returned by this method may only be used to call static methods and get/set static fields.
     *
     * @param proxyClass The proxy type. Must be annotated with {@link ProxyTarget} or {@link ProxyTargetName}.
     */
    public static <T extends ProxyBase> T createStaticProxy(Class<T> proxyClass) {
        Class<?> targetClass = ProxyUtils.getTargetClass(proxyClass);
        return createProxyInternal(proxyClass, targetClass, null);
    }

    @SuppressWarnings("unchecked")
    private static <T> T createProxyInternal(Class<T> proxyClass, Class<?> targetClass, Object target) {
        ReflectionInvocationHandler invocationHandler = new ReflectionInvocationHandler(targetClass, target);
        return (T)Proxy.newProxyInstance(proxyClass.getClassLoader(), new Class<?>[] {proxyClass}, invocationHandler);
    }
}
