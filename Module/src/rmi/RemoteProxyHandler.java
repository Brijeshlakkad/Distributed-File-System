package rmi;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Objects;

/**
 * This class handles invocation made on the stub which will be directed to the server stub (Skeleton) residing at the
 * server.
 * <p>
 * If this class not implemented as
 * <code>Serializable</code>, JVM will throw java.io.NotSerializableException when writing on
 * <code>ObjectOutputStream</code>.
 * <p>
 * See https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html
 *
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class RemoteProxyHandler implements InvocationHandler, Serializable {
    private final Class<?> d_class;
    private final InetSocketAddress d_address;

    /**
     * Constructs a new <code>RemoteProxyHandler</code>.
     *
     * @param p_class   The class object.
     * @param p_address Value of address of server socket.
     */
    public RemoteProxyHandler(Class<?> p_class, InetSocketAddress p_address) {
        d_class = p_class;
        d_address = p_address;
    }

    @Override
    public Object invoke(Object p_proxyTarget, Method p_method, Object[] p_argumentList) throws Throwable {
        if (RemoteUtil.isLocalMethod(this.getClass(), p_method, p_argumentList)) {
            return p_method.invoke(this, p_argumentList);
        } else if (RemoteUtil.isRemoteMethod(d_class, p_method)) {
            try (Socket l_clientSocket = new Socket(d_address.getAddress(), d_address.getPort())) {
                /* Remote Method Invocation steps
                /* If the call cannot be complete due to a network error, this block should throw RMIException.
                 */
                try {
                    ObjectOutputStream outputStream = new ObjectOutputStream(l_clientSocket.getOutputStream());
                    // 1 Flush the stream
                    outputStream.flush();

                    // 2 Get the subroutine data and marshals the data
                    // 2.1 Method name to invoke
                    outputStream.writeObject(p_method.getName());
                    // 2.1 Signature of the method
                    outputStream.writeObject(p_method.getParameterTypes());
                    // 2.3 Parameters to pass
                    outputStream.writeObject(p_argumentList);
                } catch (Exception e) {
                    throw new RMIException(e);
                }

                // 3 Read the received data
                ObjectInputStream inputStream = new ObjectInputStream(l_clientSocket.getInputStream());
                if (inputStream.read() < 0) {
                    // 3.1 Unmarshal the data and check the response status
                    ResponseStatus l_responseStatus = (ResponseStatus) inputStream.readObject();

                    if (inputStream.read() < 0) {
                        // 3.2 Return value if the response status value is 200.
                        Object responseObject = inputStream.readObject();

                        if (l_responseStatus == ResponseStatus.Ok) {
                            return responseObject;
                        } else {
                            throw (Throwable) responseObject;
                        }
                    }
                }

                // If method was expected to return an object and input stream was empty.
                if (p_method.getReturnType() != void.class && inputStream.read() < -1) {
                    throw new RMIException("Connection failed!");
                } else {
                    return null;
                }
            } catch (ConnectException p_connectException) {
                throw new RMIException("Connection refused!");
            }
            // close the client socket
        } else {
            return new RMIException(new NoSuchMethodException());
        }
    }

    @Override
    public boolean equals(Object l_p_o) {
        if (this == l_p_o) return true;
        if (l_p_o == null) return false;
        // Get RemoteProxyHandler of proxy object to compare.
        RemoteProxyHandler l_that = (RemoteProxyHandler) Proxy.getInvocationHandler(l_p_o);
        return Objects.equals(d_class, l_that.d_class) &&
                Objects.equals(d_address, l_that.d_address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(d_class, d_address);
    }

    @Override
    public String toString() {
        return "RemoteProxyHandler{" +
                "d_class=" + d_class +
                ", d_address=" + d_address +
                '}';
    }
}
