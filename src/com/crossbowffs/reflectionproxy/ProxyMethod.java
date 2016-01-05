package com.crossbowffs.reflectionproxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a proxy method as a method delegate. This is the default
 * behavior, so this annotation is only needed if you need to
 * explicitly specify a target method name.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxyMethod {
    String value() default "";
}
