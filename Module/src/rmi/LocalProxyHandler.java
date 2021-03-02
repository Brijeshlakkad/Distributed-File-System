package rmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Stream;

/**
 * https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html
 *
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class LocalProxyHandler<T> extends ObjectInputStream implements InvocationHandler {
    private final ClassLoader d_classLoader;
    private final T d_target;

    /**
     * Constructs a new <code>ClassProxyHandler</code>.
     *
     * @param p_inputStream The <code>InputStream</code> to work on
     * @throws IOException              In case of an I/O error
     * @throws StreamCorruptedException If the stream is corrupted
     */
    public LocalProxyHandler(T p_Target, ClassLoader p_classLoader, InputStream p_inputStream) throws IOException {
        super(p_inputStream);
        d_target = p_Target;
        d_classLoader = p_classLoader;
    }

    private Class<?> resolveProxyClass(Class... itsInterfaces) {
        Class[] allInterfaces = Stream.concat(
                Stream.of(d_classLoader),
                Stream.of(itsInterfaces))
                .distinct()
                .toArray(Class[]::new);

        return (Class<?>) Proxy.newProxyInstance(
                d_classLoader,
                allInterfaces,
                this);
    }

    public Object handleInvocation() throws IOException, ClassNotFoundException, InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // get method name
        String l_methodName = (String) this.readObject();
        // get parameter types
        Class<?>[] l_paramTypeList = (Class<?>[]) this.readObject();

        Object[] l_argumentList = (Object[]) this.readObject();

        Class<?> l_tClass = resolveProxyClass();
        // Find method with valid and existing signature.
        Method method = l_tClass.getMethod(l_methodName, l_paramTypeList);
        return method.invoke(d_target, l_argumentList);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(d_target, args);
    }
}
