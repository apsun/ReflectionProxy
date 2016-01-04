package com.crossbowffs.reflectionproxy;

/**
 * An extension to {@link ProxyBase} that provides convenience
 * methods for getting the target object and getting/setting field
 * values on the target object.
 *
 * You should generally implement this interface instead of {@link ProxyBase}.
 * Only use {@link ProxyBase} if a naming conflict occurs (e.g. you want to call
 * a method named {@code _getTarget} on the target class). The equivalent
 * functionality is provided through {@link ProxyUtils}.
 */
public interface ProxyBaseEx extends ProxyBase {
    /**
     * Gets the object that this proxy delegates to.
     *
     * @see ProxyUtils#getProxyTarget(ProxyBase)
     */
    Object _getTarget();

    /**
     * Sets the value of the specified field on the proxy target. If {@code value} is a proxy
     * object and the target field cannot be directly assigned to {@code value}, the field will
     * be set to the proxy's delegate object (see {@link #_getTarget()}).
     *
     * @param fieldName The name of the field to set.
     * @param value The value to set the field to.
     * @see ProxyUtils#setProxyField(ProxyBase, String, Object)
     */
    void _setField(String fieldName, Object value);

    /**
     * Gets the value of the specified field on the proxy target. If the value of the field
     * cannot be directly assigned to {@code fieldType} and {@code fieldType} extends
     * {@link ProxyBase}, a proxy object will be returned (see {@link ProxyFactory#createProxy(Class, Object)}).
     *
     * @param fieldName The name of the field to get.
     * @param fieldType The expected type of the field.
     * @see ProxyUtils#getProxyField(ProxyBase, String, Class)
     */
    <T> T _getField(String fieldName, Class<T> fieldType);
}
