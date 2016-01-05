package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Field;

/* package */ class FieldSetterDispatcher extends FieldDispatcher {
    public FieldSetterDispatcher(Field field, Class<?> expectedType) {
        super(field, expectedType);
    }

    @Override
    public Object handle(Object target, Object[] args) {
        Object value = ProxyUtils.coerceInput(mField.getType(), args[0]);
        try {
            mField.set(target, value);
        } catch (IllegalAccessException e) {
            throw new AssertionError(e);
        } catch (NullPointerException e) {
            throw new ProxyException("Attempted to set instance field on static proxy", e);
        }
        return null;
    }
}
