package rmi;

import rmi.enums.ResponseStatus;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;

class SocketClientHandler implements Runnable {
    private final Socket d_clientSocket;
    private final Skeleton<?> d_skeleton;
    private final Thread d_thread;

    public SocketClientHandler(Socket p_clientSocket, Skeleton<?> p_skeleton) {
        d_thread = new Thread(this);
        d_clientSocket = p_clientSocket;
        d_skeleton = p_skeleton;
    }

    public void start() {
        d_thread.start();
    }

    public void run() {
        // Sets the service error handler. If exception not handled manually, ServiceErrorHandler will handle it.
        Thread.setDefaultUncaughtExceptionHandler(new ServiceErrorHandler(d_skeleton, d_clientSocket));
        try {
            ObjectOutputStream l_objectOutputStream = new ObjectOutputStream(d_clientSocket.getOutputStream());
            LocalProxyHandler<Object> l_localProxyHandler =
                    new LocalProxyHandler<>(d_skeleton.getTarget(), d_skeleton.getClassLoader(), d_clientSocket.getInputStream());
            try {
                Object returnObject = l_localProxyHandler.handleInvocation();
                if (returnObject != null) {
                    l_objectOutputStream.writeObject(ResponseStatus.Ok);
                    l_objectOutputStream.writeObject(returnObject);
                }
            } catch (ClassNotFoundException p_e) {
                l_objectOutputStream.writeObject(ResponseStatus.InternalServerErrorException);
                l_objectOutputStream.writeObject(p_e.getCause());
            } catch (InvocationTargetException p_e) {
                // When the called method throws an exception
                l_objectOutputStream.writeObject(ResponseStatus.BadRequestException);
                l_objectOutputStream.writeObject(p_e.getCause());
            } catch (IllegalAccessException p_e) {
                l_objectOutputStream.writeObject(ResponseStatus.UnauthorizedException);
                l_objectOutputStream.writeObject(p_e.getCause());
            } catch (NoSuchMethodException p_e) {
                l_objectOutputStream.writeObject(ResponseStatus.NotFoundException);
                l_objectOutputStream.writeObject(p_e.getCause());
            }
        } catch (IOException p_ioException) {
            this.d_skeleton.service_error(new RMIException(p_ioException.getMessage()));
        } finally {
            try {
                d_clientSocket.close();
            } catch (IOException p_ioException) {
                this.d_skeleton.service_error(new RMIException(p_ioException.getCause()));
            }
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