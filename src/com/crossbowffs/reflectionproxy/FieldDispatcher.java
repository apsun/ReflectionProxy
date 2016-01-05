package com.crossbowffs.reflectionproxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/* package */ abstract class FieldDispatcher extends ProxyDispatcherBase {
    protected final Field mField;
    protected final Class<?> mExpectedType;

    protected FieldDispatcher(Field field, Class<?> expectedType) {
        mField = field;
        mExpectedType = expectedType;
    }

    private static String getFieldNameForMethod(Method proxyMethod, ProxyField annotation) {
        String fieldName = annotation.value();
        if ("".equals(fieldName)) {
            String methodName = proxyMethod.getName();
            if (methodName.startsWith("set_") || methodName.startsWith("get_")) {
                fieldName = methodName.substring(4);
            } else {
                throw new ProxyException("Could not automatically determine " +
                    "field name from method signature: " + proxyMethod.toString());
            }
        }
        return fieldName;
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

    public static FieldDispatcher create(Class<?> targetClass, Method proxyMethod, ProxyField annotation) {
        String fieldName = getFieldNameForMethod(proxyMethod, annotation);
        Field field = findField(targetClass, fieldName);
        field.setAccessible(true);
        Class<?> fieldType = field.getType();
        Class<?> returnType = proxyMethod.getReturnType();
        Class<?>[] argTypes = proxyMethod.getParameterTypes();
        Class<?> expectedFieldType;
        if (returnType == void.class && argTypes.length == 1) {
            expectedFieldType = argTypes[0];
            if (ProxyUtils.isCoercibleInput(expectedFieldType, fieldType)) {
                return new FieldSetterDispatcher(field, expectedFieldType);
            }
        } else if (returnType != void.class && argTypes.length == 0) {
            expectedFieldType = returnType;
            if (ProxyUtils.isCoercibleOutput(expectedFieldType, fieldType)) {
                return new FieldGetterDispatcher(field, expectedFieldType);
            }
        } else {
            throw new ProxyException("Invalid field accessor signature: " + proxyMethod.toString());
        }
        throw new ProxyException("Field type mismatch (expected " +
            expectedFieldType.getName() + ", got " + fieldType.getName() + ")");
    }
}
