package com.crossbowffs.reflectionproxy;

/**
 * The base interface for all reflection proxy interfaces.
 *
 * <p>
 * To create a proxy interface, create a new instance that extends
 * this interface, and annotate it with either {@link ProxyTarget}
 * or {@link ProxyTargetName}. Then, declare the methods that you
 * wish to access on the target object, using the same signature
 * as the target method.
 *
 * <p>
 * To access fields, annotate a proxy method with {@link ProxyField}.
 * To call constructors, use {@link ProxyConstructor}. See the
 * corresponding annotations for more information.
 *
 * <p>
 * If an argument or return value is not directly accessible by your
 * code, you may use a proxy interface in the signature instead;
 * the types will be automatically bridged at runtime.
 */
public interface ProxyBase {

}
