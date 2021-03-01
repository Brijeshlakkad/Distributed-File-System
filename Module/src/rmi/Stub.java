package rmi;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.Arrays;
import java.util.List;

/**
 * RMI stub factory.
 *
 * <p>
 * RMI stubs hide network communication with the remote server and provide a simple object-like interface to their
 * users. This class provides methods for creating stub objects dynamically, when given pre-defined interfaces.
 *
 * <p>
 * The network address of the remote server is set when a stub is created, and may not be modified afterwards. Two stubs
 * are equal if they implement the same interface and carry the same remote server address - and would therefore connect
 * to the same skeleton. Stubs are serializable.
 */
public abstract class Stub {
    /**
     * Creates a stub, given a skeleton with an assigned address.
     *
     * <p>
     * The stub is assigned the address of the skeleton. The skeleton must either have been created with a fixed
     * address, or else it must have already been started.
     *
     * <p>
     * This method should be used when the stub is created together with the skeleton. The stub may then be transmitted
     * over the network to enable communication with the skeleton.
     *
     * @param c        A <code>Class</code> object representing the interface implemented by the remote object.
     * @param skeleton The skeleton whose network address is to be used.
     * @return The stub created.
     * @throws IllegalStateException If the skeleton has not been assigned an address by the user and has not yet been
     *                               started.
     * @throws UnknownHostException  When the skeleton address is a wildcard and a port is assigned, but no address can
     *                               be found for the local host.
     * @throws NullPointerException  If any argument is <code>null</code>.
     * @throws Error                 If <code>c</code> does not represent a remote interface - an interface in which
     *                               each method is marked as throwing
     *                               <code>RMIException</code>, or if an object implementing
     *                               this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton)
            throws UnknownHostException {
        if (c == null || skeleton == null) {
            throw new NullPointerException("Null parameter(s).");
        }
        // Checks if c represents a remote interface.
        if (!c.isInterface()) {
            throw new Error();
        }
        // Checks if c represents a remote interface.
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            List<Class<?>> exceptionTypes = Arrays.asList(declaredMethod.getExceptionTypes());
            if (!exceptionTypes.contains(RMIException.class)) {
                throw new Error();
            }
        }
        try {
            Registry registry = LocateRegistry.getRegistry(skeleton.getAddress().getHostName());
            return (T) registry.lookup(skeleton.getInterface().getName());
        } catch (NotBoundException | RemoteException p_e) {
            throw new UnknownHostException();
        }
    }

    /**
     * Creates a stub, given a skeleton with an assigned address and a hostname which overrides the skeleton's
     * hostname.
     *
     * <p>
     * The stub is assigned the port of the skeleton and the given hostname. The skeleton must either have been started
     * with a fixed port, or else it must have been started to receive a system-assigned port, for this method to
     * succeed.
     *
     * <p>
     * This method should be used when the stub is created together with the skeleton, but firewalls or private networks
     * prevent the system from automatically assigning a valid externally-routable address to the skeleton. In this
     * case, the creator of the stub has the option of obtaining an externally-routable address by other means, and
     * specifying this hostname to this method.
     *
     * @param c        A <code>Class</code> object representing the interface implemented by the remote object.
     * @param skeleton The skeleton whose port is to be used.
     * @param hostname The hostname with which the stub will be created.
     * @return The stub created.
     * @throws IllegalStateException If the skeleton has not been assigned a port.
     * @throws NullPointerException  If any argument is <code>null</code>.
     * @throws Error                 If <code>c</code> does not represent a remote interface - an interface in which
     *                               each method is marked as throwing
     *                               <code>RMIException</code>, or if an object implementing
     *                               this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, Skeleton<T> skeleton,
                               String hostname) {
        if (c == null || skeleton == null || hostname == null) {
            throw new NullPointerException("Null parameter(s).");
        }
        // Checks if c represents a remote interface.
        if (!c.isAssignableFrom(Remote.class)) {
            throw new Error();
        }
        try {
            Registry registry = LocateRegistry.getRegistry(hostname);
            return (T) registry.lookup(skeleton.getInterface().getName());
        } catch (NotBoundException | RemoteException p_e) {
            throw new Error();
        }
    }

    /**
     * Creates a stub, given the address of a remote server.
     *
     * <p>
     * This method should be used primarily when bootstrapping RMI. In this case, the server is already running on a
     * remote host but there is not necessarily a direct way to obtain an associated stub.
     *
     * @param c       A <code>Class</code> object representing the interface implemented by the remote object.
     * @param address The network address of the remote skeleton.
     * @return The stub created.
     * @throws NullPointerException If any argument is <code>null</code>.
     * @throws Error                If <code>c</code> does not represent a remote interface - an interface in which each
     *                              method is marked as throwing
     *                              <code>RMIException</code>, or if an object implementing
     *                              this interface cannot be dynamically created.
     */
    public static <T> T create(Class<T> c, InetSocketAddress address) {
        if (c == null || address == null) {
            throw new NullPointerException("Null parameter(s).");
        }
        // Checks if c represents a remote interface.
        if (!c.isInterface()) {
            throw new Error();
        }
        // Checks if c represents a remote interface.
        Method[] declaredMethods = c.getDeclaredMethods();
        for (Method declaredMethod : declaredMethods) {
            List<Class<?>> exceptionTypes = Arrays.asList(declaredMethod.getExceptionTypes());
            if (!exceptionTypes.contains(RMIException.class)) {
                throw new Error();
            }
        }
        try {
            Registry registry = LocateRegistry.getRegistry(address.getAddress().getHostName());
            return (T) registry.lookup(address.toString());
        } catch (NotBoundException | RemoteException p_e) {
            throw new Error();
        }
    }
}
