# ReflectionProxy

Automatic Java reflection proxy binding library, for all your 
reflection needs!

## Usage

Let's say you have the following library code:
```Java
package com.example.api;

public class Target {
    /* package */ TargetData getData();
    /* package */ void setData(TargetData data);
}

/* package */ class TargetData {
    /* package */ int getId();
}
```

Now let's say you have an instance of `Target`, and you want to use the 
library's internal API. Just create the proxy interfaces like so:
```Java
@ProxyTarget(Target.class)
public interface TargetProxy extends ProxyBase {
    // Automatic parameter and return type conversion!
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
TargetProxy proxy = ProxyFactory.createProxy(TargetProxy.class, target);
TargetDataProxy dataProxy = proxy.getData();
int id = dataProxy.getId();
proxy.setData(dataProxy);
```

## License

Distributed under the [MIT License](http://opensource.org/licenses/MIT).
