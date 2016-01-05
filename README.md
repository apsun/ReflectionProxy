# ReflectionProxy

Automatic Java reflection proxy binding library.

## Features

- Pure Java library, no external dependencies
- Automatic method and field binding (no boilerplate code required)
- Automatic proxy <-> object coercion

## Note

ReflectionProxy was designed for _convenience_, not _performance_.
It should only be used if the following conditions are met:
- You are using a large number of private API's
- You aren't using it in performance critical code (i.e., in tight loops)
- You don't want an external tool dependency

If you are looking for high-performance reflection, please look elsewhere:
- [AspectJ](https://blogs.vmware.com/vfabric/2012/04/using-aspectj-for-accessing-private-members-without-reflection.html)
- [PatchLib](https://github.com/mariotaku/PatchLib)

## Example

Suppose you have the following library code:
```Java
package com.example.api;

public class WidgetFactory {
    private static int sGlobalId = 0;

    /* package */ static Widget create() {
        return new Widget(sGlobalId++);
    }
}

public class Widget {
    private WidgetData mData;

    /* package */ Widget(int id) {
        mData = new WidgetData(id);
    }

    /* package */ WidgetData getData() {
        return mData;
    }
}

/* package */ class WidgetData {
    /* package */ int mId;

    /* package */ WidgetData(int id) {
        mId = id;
    }
}
```

Let's say you want to use the library's internal API.
Just create the corresponding proxy interfaces:
```Java
@ProxyTarget(WidgetFactory.class)
interface WidgetFactoryProxy extends ProxyBase {
    @ProxyStatic Widget create();
}

@ProxyTarget(Widget.class)
interface WidgetProxy extends ProxyBase {
    @ProxyField void set_mData(WidgetDataProxy data);
    WidgetDataProxy getData();
}

@ProxyTargetName("com.example.api.WidgetData")
interface WidgetDataProxy extends ProxyBase {
    @ProxyConstructor WidgetDataProxy newInstance(int id);
    @ProxyField int get_mId();
}
```

Now, your code is as simple as:
```Java
// Create a static proxy
WidgetFactoryProxy factory = ProxyFactory.createStaticProxy(WidgetFactoryProxy.class);

// Call a static proxy method
Widget widget = factory.create();

// Create an instance proxy
WidgetProxy widgetProxy = ProxyFactory.createProxy(WidgetProxy.class, widget);

// Call an instance proxy method
WidgetDataProxy oldData = widgetProxy.getData();

// Get the value of a field
int oldId = oldData.get_mId();

// Construct a new object (see note below)
WidgetDataProxy newData = oldData.newInstance(oldId + 42);

// Set the value of a field
widgetProxy.set_mData(newData);
```

Note that there is no distinction between static and instance methods in the proxy
interface. Calling a static method on an instance proxy is valid, but calling
an instance method on a static proxy will throw an exception. Constructors are
implicitly static, hence we can create new objects through references to proxies
of the same type (`WidgetDataProxy newData = oldData.newInstance(oldId + 42)`).

## API

### `ProxyBase`
- The base interface for all reflection proxies.
- You must annotate your proxy interface with either `@ProxyTarget` or `@ProxyTargetName`

### `@ProxyTarget(targetClass)`
- Declares the target class for a proxy interface

### `@ProxyTargetName(targetClassName)`
- Same as `@ProxyTarget`, but using the class name
- Use this if you do not have access to the target class directly
- The name must be fully qualified (see `Class.forName(String)`)

### `@ProxyMethod([methodName])`
- Marks a proxy member as a method delegate (optionally specifying the target method name)
- This is the default behavior; you do not have to explicitly specify it
- The signature of the proxy method must match that of the target method
- If the target method has an inaccessible return/parameter type, use a proxy type instead

### `@ProxyField([fieldName])`
- Marks a proxy member as a field accessor (optionally specifying the target field name)
- Whether the method is a getter or a setter is automatically determined by its signature
  - Getters: `T get_X()`
  - Setters: `void set_X(T value)`
- If you do not explicitly specify the field name, the method must be named `set_X()` or `get_X()`
  - `X` is the name of your field (case sensitive)
- For example, a setter for a field `int mData` could be declared in the following ways:
  - `@ProxyField void set_mData(int value)`
  - `@ProxyField("mData") void setData(int value)`

### `@ProxyConstructor`
- Marks a proxy member as a constructor delegate
- Proxy constructor methods are implicitly `@ProxyStatic`
- Declare and call the constructor as if it were a factory method in the proxy interface
  - Original constructor: `private MyClass(String a, int b) { ... }`
  - Proxy method: `@ProxyConstructor MyClass newInstance(String a, int b)`

### `@ProxyStatic`
- Declares a proxy member as a static member of the target class
- This currently has no runtime effect; you can use it for static analysis

## What sorcery is this?!

~~Witchcraft~~ `java.lang.reflect.Proxy`.

## License

Distributed under the [MIT License](http://opensource.org/licenses/MIT).
