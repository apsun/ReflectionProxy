package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Field;

/* package */ class FieldGetterDispatcher extends FieldDispatcher {
    public FieldGetterDispatcher(Field field, Class<?> expectedType) {
        super(field, expectedType);
    }

    @Override
    public Object handle(Object target, Object[] args) {
        Object value;
        try {
            value = mField.get(target);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to get instance field on static proxy", e);
        }
        return ProxyUtils.coerceOutput(mExpectedType, value);
    }
}
