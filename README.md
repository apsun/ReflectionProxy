# ReflectionProxy

Automatic Java reflection proxy binding library, for all your 
reflection needs!

## Usage

Suppose you have the following library code:
```Java
package com.example.api;

public class TargetFactory {
    /* package */ static Target create();
}

public class Target {
    /* package */ static Target create();
    /* package */ TargetData getData();
    /* package */ void setData(TargetData data);
}

/* package */ class TargetData {
    /* package */ int getId();
}
```

Now let's say you want to use the library's internal API.
Just create the proxy interfaces like so:
```Java
@ProxyTarget(TargetFactory.class)
public interface TargetFactoryProxy extends ProxyBase {
    Target create();
}

@ProxyTarget(Target.class)
public interface TargetProxy extends ProxyBase {
    // Automatic parameter and return type conversion!
    Target create();
    TargetDataProxy getData();
    void setData(TargetDataProxy data);
}

// Can't access the class? Use the fully-qualified class name instead!
@ProxyTargetName("com.example.api.TargetData")
public interface TargetDataProxy extends ProxyBase {
    int getId();
}
```

Now, your code is as simple as:
```Java
TargetFactoryProxy factoryProxy = ProxyFactory.createStaticProxy(TargetFactoryProxy.class);
Target target = factoryProxy.create();
// Note that I could have made `TargetFactoryProxy#create()` directly
// return `TargetProxy`, which would let me skip this next step; this 
// is for demonstration purposes only.
TargetProxy proxy = ProxyFactory.createProxy(TargetProxy.class, target);
TargetDataProxy dataProxy = proxy.getData();
int id = dataProxy.getId();
proxy.setData(dataProxy);
```

## License

Distributed under the [MIT License](http://opensource.org/licenses/MIT).
