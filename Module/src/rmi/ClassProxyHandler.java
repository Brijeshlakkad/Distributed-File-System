package rmi;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.stream.Stream;

/**
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class ClassProxyHandler<T> {
    public T resolveProxyClass(Class iface, InvocationHandler handler, Class... otherIfaces) {
        Class[] allInterfaces = Stream.concat(
                Stream.of(iface),
                Stream.of(otherIfaces))
                .distinct()
                .toArray(Class[]::new);

        return (T) Proxy.newProxyInstance(
                iface.getClassLoader(),
                allInterfaces,
                handler);
    }

    public T passThroughProxy(Class iface, T target) {
        return resolveProxyClass(iface, new PassThroughInvocationHandler(target), Remote.class);
    }
}

class PassThroughInvocationHandler implements InvocationHandler {
    private final Object target;

    public PassThroughInvocationHandler(Object target) {
        this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(target, args);
    }
}
