# ReflectionProxy

Automatic Java reflection proxy binding library, for all your reflection needs!

## Features

- Pure Java library, no external dependencies
- Automatic method and field binding (no boilerplate code required)
- Automatic proxy <-> object coercion

## Usage

Suppose you have the following library code:
```Java
package com.example.api;

public class WidgetFactory {
    /* package */ static Widget create(int id);
}

public class Widget {
    /* package */ WidgetData getData();
    public int getId();
}

/* package */ class WidgetData {
    private int mId;
}
```

Let's say you want to use the library's internal API. Just create the proxy interfaces:
```Java
@ProxyTarget(WidgetFactory.class)
interface WidgetFactoryProxy extends ProxyBase {
    Widget create(int id);
}

@ProxyTarget(Widget.class)
interface WidgetProxy extends ProxyBase {
    // Automatic parameter and return type conversion
    WidgetDataProxy getData();

    // Note that we don't need to declare all the methods, 
    // just the ones that we need
}

// Can't access the class? Use the fully-qualified class name instead
@ProxyTargetName("com.example.api.WidgetData")
interface WidgetDataProxy extends ProxyBase {
    // You can auto-bind to a field by name...
    @ProxyField
    int get_mId();

    // ... or you can explicitly specify the field name
    @ProxyField("mId")
    void setId(int value);
}
```

Now, your code is as simple as:
```Java
// Create a static proxy
WidgetFactoryProxy factoryProxy = ProxyFactory.createStaticProxy(WidgetFactoryProxy.class);

// Call a static proxy method
Widget widget = factoryProxy.create(1);

// Create an instance proxy
WidgetProxy proxy = ProxyFactory.createProxy(WidgetProxy.class, widget);

// Call an instance proxy method
WidgetDataProxy dataProxy = proxy.getData();

// Get and set object fields through the proxy
dataProxy.setId(dataProxy.get_mId() + 42);

// Done!
int newId = widget.getId();
System.out.println(newId); // 43
```

Note that there is no distinction between static and instance methods in the 
proxy interface. Calling a static method on an instance proxy is perfectly fine, but 
calling an instance method on a static proxy will throw an exception.

## License

Distributed under the [MIT License](http://opensource.org/licenses/MIT).
