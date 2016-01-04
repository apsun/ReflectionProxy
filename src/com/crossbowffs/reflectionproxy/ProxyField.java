package com.crossbowffs.reflectionproxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a proxy method as a field accessor. The method must have
 * either of the following signatures:
 * <ul>
 *     <li>Setter: {@code void set_X(T)}</li>
 *     <li>Getter: {@code T get_X()}</li>
 * </ul>
 *
 * <p>
 * If you do not want to use the set_X/get_X naming convention,
 * you may also specify the name of the field in the annotation.
 *
 * <p>
 * If the parameter/return type is a proxy interface, the value
 * will automatically be bridged at runtime.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxyField {
    String value() default "";
}
