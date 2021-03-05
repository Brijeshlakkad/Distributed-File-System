package rmi;

import rmi.enums.ResponseStatus;
import rmi.utils.RemoteUtil;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * https://docs.oracle.com/javase/8/docs/api/java/io/ObjectInputStream.html
 *
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class RemoteProxyHandler implements InvocationHandler {
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
    public Object invoke(Object proxy, Method method, Object[] argumentList) throws Throwable {
        if (RemoteUtil.isRemoteMethod(d_class, method)) {
            try (Socket l_clientSocket = new Socket(d_address.getAddress(), d_address.getPort())) {

                // * Remote Method Invocation steps

                ObjectOutputStream outputStream = new ObjectOutputStream(l_clientSocket.getOutputStream());
                // 1 Flush the stream
                outputStream.flush();

                // 2 Get the subroutine data and marshals the data
                // 2.1 Method name to invoke
                outputStream.writeObject(method.getName());
                // 2.1 Signature of the method
                outputStream.writeObject(method.getParameterTypes());
                // 2.3 Parameters to pass
                outputStream.writeObject(argumentList);

                // 3 Read the received data
                ObjectInputStream inputStream = new ObjectInputStream(l_clientSocket.getInputStream());

                // 3.1 Unmarshal the data and check the response status
                ResponseStatus l_responseStatus = (ResponseStatus) inputStream.readObject();
                // 3.2 Return value if the response status value is 200.
                Object responseObject = inputStream.readObject();

                if (l_responseStatus == ResponseStatus.Ok) {
                    return responseObject;
                } else {
                    throw (Throwable) responseObject;
                }
            } catch (Exception e) {
                throw new RMIException(e);
            }
            // close the client socket
        } else {
            return new RMIException(new NoSuchMethodException());
        }
    }
}
