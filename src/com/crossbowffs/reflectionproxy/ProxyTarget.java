package com.crossbowffs.reflectionproxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the type that this proxy delegates to.
 * If you cannot access the type directly (e.g. the type is private),
 * you can use {@link ProxyTargetName} instead.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxyTarget {
    Class<?> value();
}
