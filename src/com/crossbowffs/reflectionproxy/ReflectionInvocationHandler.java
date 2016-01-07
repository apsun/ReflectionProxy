package com.crossbowffs.reflectionproxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/* package */ class ReflectionInvocationHandler implements InvocationHandler {
    private final Class<?> mTargetClass;
    private final Object mTarget;

    public ReflectionInvocationHandler(Class<?> targetClass, Object target) {
        mTargetClass = targetClass;
        mTarget = target;
    }

    public Object getTarget() {
        return mTarget;
    }

    @Override
    public Object invoke(Object proxy, Method proxyMethod, Object[] args) {
        ProxyDispatcherBase dispatcher = ProxyDispatcherBase.get(mTargetClass, proxyMethod);
        return dispatcher.handle((ProxyBase)proxy, mTarget, args);
    }
}
