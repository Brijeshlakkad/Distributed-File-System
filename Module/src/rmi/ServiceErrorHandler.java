package rmi;


import java.net.Socket;

/**
 * Catches the exception occurred in the Thread execution.
 */
public class ServiceErrorHandler implements Thread.UncaughtExceptionHandler {
    private final Skeleton<?> d_skeleton;
    private final Socket d_clientSocket;

    public ServiceErrorHandler(Skeleton<?> p_skeleton, Socket p_clientSocket) {
        d_skeleton = p_skeleton;
        d_clientSocket = p_clientSocket;
    }

    /**
     * {@inheritDoc}
     */
    public void uncaughtException(Thread t, Throwable e) {
        d_skeleton.service_error(
                new RMIException(String.format("Exception occurred in server thread while processing a request from %s", d_clientSocket.getRemoteSocketAddress())));
    }
}