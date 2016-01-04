package com.crossbowffs.reflectionproxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the type that this proxy delegates to.
 * This annotation should be used instead of {@link ProxyTarget}
 * when you do not have access to the target type.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxyTargetName {
    String value();
}
