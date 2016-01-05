package com.crossbowffs.reflectionproxy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a proxy method as a constructor delegate. This method
 * is implicitly {@link ProxyStatic}, and may be called on static
 * proxy objects. The target type of the containing proxy interface
 * must either be convertible to the return value (either the same
 * class, a superclass, or a proxy of the target type).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxyConstructor {

}
