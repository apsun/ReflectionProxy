package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/* package */ abstract class ProxyDispatcherBase {
    private static final Map<Method, ProxyDispatcherBase> sDispatcherCache = new HashMap<Method, ProxyDispatcherBase>();

    public abstract Object handle(Object target, Object[] args);

    private static ProxyDispatcherBase create(Class<?> targetClass, Method proxyMethod) {
        ProxyConstructor constructorAnnotation = proxyMethod.getAnnotation(ProxyConstructor.class);
        if (constructorAnnotation != null) {
            return ConstructorDispatcher.create(targetClass, proxyMethod, constructorAnnotation);
        }

        ProxyField fieldAnnotation = proxyMethod.getAnnotation(ProxyField.class);
        if (fieldAnnotation != null) {
            return FieldDispatcher.create(targetClass, proxyMethod, fieldAnnotation);
        }

        ProxyMethod methodAnnotation = proxyMethod.getAnnotation(ProxyMethod.class);
        return MethodDispatcher.create(targetClass, proxyMethod, methodAnnotation);
    }

    public static ProxyDispatcherBase get(Class<?> targetClass, Method proxyMethod) {
        ProxyDispatcherBase dispatcher = sDispatcherCache.get(proxyMethod);
        if (dispatcher == null) {
            dispatcher = create(targetClass, proxyMethod);
            sDispatcherCache.put(proxyMethod, dispatcher);
        }
        return dispatcher;
    }
}
