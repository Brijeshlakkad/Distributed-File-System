package rmi;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.Arrays;
import java.util.List;

/**
 * RMI skeleton
 *
 * <p>
 * A skeleton encapsulates a multithreaded TCP server. The server's clients are intended to be RMI stubs created using
 * the <code>Stub</code> class.
 *
 * <p>
 * The skeleton class is parametrized by a type variable. This type variable should be instantiated with an interface.
 * The skeleton will accept from the stub requests for calls to the methods of this interface. It will then forward
 * those requests to an object. The object is specified when the skeleton is constructed, and must implement the remote
 * interface. Each method in the interface should be marked as throwing
 * <code>RMIException</code>, in addition to any other exceptions that the user
 * desires.
 *
 * <p>
 * Exceptions may occur at the top level in the listening and service threads. The skeleton's response to these
 * exceptions can be customized by deriving a class from <code>Skeleton</code> and overriding <code>listen_error</code>
 * or <code>service_error</code>.
 */
public class Skeleton<T> {
    private Class<T> c;
    private T server;
    private InetSocketAddress address;
    private Registry registry;
    private ProxyHandler<T> d_proxyHandler;
    private T proxyClass;
    private ListeningThread<Remote> d_listeningThread;

    /**
     * Creates a <code>Skeleton</code> with no initial server address. The address will be determined by the system
     * when
     * <code>start</code> is called. Equivalent to using <code>Skeleton(null)</code>.
     *
     * <p>
     * This constructor is for skeletons that will not be used for bootstrapping RMI - those that therefore do not
     * require a well-known port.
     *
     * @param c      An object representing the class of the interface for which the skeleton server is to handle method
     *               call requests.
     * @param server An object implementing said interface. Requests for method calls are forwarded by the skeleton to
     *               this object.
     * @throws Error                If <code>c</code> does not represent a remote interface - an interface whose methods
     *                              are all marked as throwing
     *                              <code>RMIException</code>.
     * @throws NullPointerException If either of <code>c</code> or
     *                              <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server) {
        if (c == null || server == null) {
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
        this.c = c;
        this.server = server;
        this.d_proxyHandler = new ProxyHandler<>();
        this.proxyClass = this.d_proxyHandler.passthroughProxy(this.c, this.server);
    }

    /**
     * Creates a <code>Skeleton</code> with the given initial server address.
     *
     * <p>
     * This constructor should be used when the port number is significant.
     *
     * @param c       An object representing the class of the interface for which the skeleton server is to handle
     *                method call requests.
     * @param server  An object implementing said interface. Requests for method calls are forwarded by the skeleton to
     *                this object.
     * @param address The address at which the skeleton is to run. If
     *                <code>null</code>, the address will be chosen by the
     *                system when <code>start</code> is called.
     * @throws Error                If <code>c</code> does not represent a remote interface - an interface whose methods
     *                              are all marked as throwing
     *                              <code>RMIException</code>.
     * @throws NullPointerException If either of <code>c</code> or
     *                              <code>server</code> is <code>null</code>.
     */
    public Skeleton(Class<T> c, T server, InetSocketAddress address) {
        if (c == null || server == null || address == null) {
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
        this.c = c;
        this.server = server;
        this.address = address;
        this.d_proxyHandler = new ProxyHandler<>();
        this.proxyClass = this.d_proxyHandler.passthroughProxy(this.c, this.server);
    }

    /**
     * Called when the listening thread exits.
     *
     * <p>
     * The listening thread may exit due to a top-level exception, or due to a call to <code>stop</code>.
     *
     * <p>
     * When this method is called, the calling thread owns the lock on the
     * <code>Skeleton</code> object. Care must be taken to avoid deadlocks when
     * calling <code>start</code> or <code>stop</code> from different threads during this call.
     *
     * <p>
     * The default implementation does nothing.
     *
     * @param cause The exception that stopped the skeleton, or
     *              <code>null</code> if the skeleton stopped normally.
     */
    protected void stopped(Throwable cause) {
    }

    /**
     * Called when an exception occurs at the top level in the listening thread.
     *
     * <p>
     * The intent of this method is to allow the user to report exceptions in the listening thread to another thread, by
     * a mechanism of the user's choosing. The user may also ignore the exceptions. The default implementation simply
     * stops the server. The user should not use this method to stop the skeleton. The exception will again be provided
     * as the argument to <code>stopped</code>, which will be called later.
     *
     * @param exception The exception that occurred.
     * @return <code>true</code> if the server is to resume accepting
     * connections, <code>false</code> if the server is to shut down.
     */
    protected boolean listen_error(Exception exception) {
        return exception != null;
    }

    /**
     * Called when an exception occurs at the top level in a service thread.
     *
     * <p>
     * The default implementation does nothing.
     *
     * @param exception The exception that occurred.
     */
    protected void service_error(RMIException exception) {
    }

    /**
     * Starts the skeleton server.
     *
     * <p>
     * A thread is created to listen for connection requests, and the method returns immediately. Additional threads are
     * created when connections are accepted. The network address used for the server is determined by which constructor
     * was used to create the <code>Skeleton</code> object.
     *
     * @throws RMIException When the listening socket cannot be created or bound, when the listening thread cannot be
     *                      created, or when the server has already been started and has not since stopped.
     */
    public synchronized void start() throws RMIException {
        synchronized (this) {
            try {
                if (System.getSecurityManager() == null) {
                    System.setSecurityManager(new SecurityManager());
                }
//            byte pattern = (byte) 0xAC;
//            RMIClientSocketFactory csf = new XorClientSocketFactory(pattern);
//            RMIServerSocketFactory ssf = new XorServerSocketFactory(pattern);
                String name = this.c.getName();
                int port = address != null ? address.getPort() : 0;
                try {
                    registry = LocateRegistry.createRegistry(port);
                } catch (RemoteException p_e) {
                    registry = LocateRegistry.getRegistry(port);
                }
                this.d_listeningThread = new ListeningThread<>(registry, (Remote) this.proxyClass, name, port);
                this.d_listeningThread.start();
//            Remote stub = UnicastRemoteObject.exportObject((Remote) this.c.cast(server), 0, csf, ssf);
            } catch (ExportException p_ignoredExportException) {
                // If the second time, the same object exported, this will likely to throw below exception
                // <code>java.rmi.server.ExportException: object already exported</code>
            } catch (RemoteException p_remoteException) {
                throw new RMIException("Listening socket can not be created!");
            }
        }
    }

    /**
     * Stops the skeleton server, if it is already running.
     *
     * <p>
     * The listening thread terminates. Threads created to service connections may continue running until their
     * invocations of the <code>service</code> method return. The server stops at some later time; the method
     * <code>stopped</code> is called at that point. The server may then be
     * restarted.
     */
    public synchronized void stop() {
        synchronized (this) {
            try {
                if (this.d_listeningThread != null)
                    this.d_listeningThread.terminate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public Class<?> getInterface() {
        return this.c;
    }

    /**
     * Gets the assigned at which skeleton is running.
     *
     * @return Value of the address.
     */
    public InetSocketAddress getAddress() {
        return address;
    }
}
