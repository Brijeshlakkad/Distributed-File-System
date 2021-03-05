package rmi;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.StreamCorruptedException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html
 *
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class LocalProxyHandler<T> extends ObjectInputStream {
    private final Class<?> d_class;
    private final T d_target;

    /**
     * Constructs a new <code>ClassProxyHandler</code>.
     *
     * @param p_inputStream The <code>InputStream</code> to work on
     * @throws IOException              In case of an I/O error
     * @throws StreamCorruptedException If the stream is corrupted
     */
    public LocalProxyHandler(T p_Target, Class<?> p_class, InputStream p_inputStream) throws IOException {
        super(p_inputStream);
        d_target = p_Target;
        d_class = p_class;
    }

    public Object invoke() throws IOException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // * Invoke method on local object

        // 1. Unmarshal the data
        // 1.1 Get the method name
        String l_methodName = (String) this.readObject();
        // 1.2 Get the parameter types to locate the specified method
        Class<?>[] l_paramTypeList = (Class<?>[]) this.readObject();
        // 1.3 Get the arguments to be passed to method call.
        Object[] l_argumentList = (Object[]) this.readObject();

        // Find method with valid and existing signature.
        Method method = d_class.getMethod(l_methodName, l_paramTypeList);

        // Invoke the method and return the results.
        return method.invoke(d_target, l_argumentList);
    }
}
