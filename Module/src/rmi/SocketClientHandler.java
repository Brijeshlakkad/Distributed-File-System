package rmi;

import rmi.enums.ResponseStatus;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

/**
 * Thread to handle client socket request for remote method invocations.
 */
class SocketClientHandler implements Runnable {
    private final Socket d_clientSocket;
    private final Skeleton<?> d_skeleton;
    private final Thread d_thread;

    /**
     * Parameterized constructor to initialise the thread with this class.
     *
     * @param p_clientSocket Client socket.
     * @param p_skeleton     Skeleton which provides the target (local) class and interface.
     */
    public SocketClientHandler(Socket p_clientSocket, Skeleton<?> p_skeleton) {
        d_thread = new Thread(this);
        d_clientSocket = p_clientSocket;
        d_skeleton = p_skeleton;
    }

    /**
     * Starts the thread.
     */
    public void start() {
        d_thread.start();
    }


    public void run() {
        // Sets the service error handler. If exception not handled manually, ServiceErrorHandler will handle it.
        Thread.setDefaultUncaughtExceptionHandler(new ServiceErrorHandler(d_skeleton, d_clientSocket));
        try {
            // The local proxy handler is a custom proxy handler to invoke a method on the local object.
            LocalProxyHandler<?> l_localProxyHandler = new LocalProxyHandler<>(d_skeleton, d_clientSocket.getInputStream());
            try {
                Object returnObject = l_localProxyHandler.invoke();
                write(ResponseStatus.Ok, returnObject);
            } catch (ClassNotFoundException p_e) {
                write(ResponseStatus.InternalServerErrorException, p_e.getCause());
            } catch (InvocationTargetException p_e) {
                // When the called method throws an exception
                write(ResponseStatus.BadRequestException, p_e.getCause());
            } catch (IllegalAccessException p_e) {
                write(ResponseStatus.UnauthorizedException, p_e.getCause());
            } catch (NoSuchMethodException p_e) {
                write(ResponseStatus.NotFoundException, p_e.getCause());
            }
        } catch (IOException p_ioException) {
            this.d_skeleton.service_error(new RMIException(p_ioException));
        } finally {
            try {
                d_clientSocket.close();
            } catch (IOException p_ioException) {
                this.d_skeleton.service_error(new RMIException(p_ioException.getCause()));
            }
        }
    }

    /**
     * Writes response status and returned value (from invoking the method on local object) on the
     * <code>ObjectOutputStream</code>.
     *
     * @param p_responseStatus Response status from enum <code>ResponseStatus</code>.
     * @param returnValue      Value returned from invoking the method.
     * @see ResponseStatus To know the types of response status.
     */
    public void write(ResponseStatus p_responseStatus, Object returnValue) {
        // Get output stream to unmarshal message
        ObjectOutputStream l_objectOutputStream;
        try {
            l_objectOutputStream = new ObjectOutputStream(d_clientSocket.getOutputStream());
            l_objectOutputStream.flush();
            l_objectOutputStream.writeObject(p_responseStatus);
            l_objectOutputStream.writeObject(returnValue);
        } catch (IOException p_ioException) {
            this.d_skeleton.service_error(new RMIException(p_ioException));
        }
    }

    public Thread getThread() {
        return this.d_thread;
    }

    /**
     * To check if thread is alive or not.
     *
     * @return True if the thread is running.
     */
    public boolean isThreadAlive() {
        return this.d_thread.isAlive();
    }

    /**
     * Terminate thread gracefully.
     */
    public void terminate() {
        if (this.d_thread.isAlive())
            this.d_thread.interrupt();
    }
}