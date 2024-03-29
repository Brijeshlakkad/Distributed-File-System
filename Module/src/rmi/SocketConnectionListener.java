package rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Thread to handle a request for a new connection. This thread will create child threads for every new connection. It
 * also manages the child threads.
 */
class SocketConnectionListener implements Runnable {
    private final Skeleton<?> d_skeleton;
    private final Thread d_thread;
    private boolean d_isListening;
    private final ServerSocket d_serverSocket;
    private final List<SocketClientHandler> d_socketClientHandlers;

    /**
     * Creates a <code>SocketConnectionListener</code> and initialises the thread for this class implementing
     * <code>Runnable</code> interface.
     *
     * @param p_serverSocket Server socket.
     * @param p_skeleton     Skeleton which had the target (local) object.
     */
    public SocketConnectionListener(ServerSocket p_serverSocket, Skeleton<?> p_skeleton) {
        d_thread = new Thread(this);
        d_serverSocket = p_serverSocket;
        d_socketClientHandlers = new ArrayList<>();
        d_skeleton = p_skeleton;
    }

    /**
     * Starts the thread.
     */
    public void start() {
        d_isListening = true;
        d_thread.start();
    }

    public void run() {
        // Sets the listen error handler. If exception not handled manually, ListenErrorHandler will handle it.
        Thread.setDefaultUncaughtExceptionHandler(new ListenErrorHandler(d_skeleton));

        // Thread responsive to interruption.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = d_serverSocket.accept();
                this.createClientHandler(clientSocket);
            } catch (IOException p_ioException) {
                this.d_isListening = false;
                if (d_skeleton.isAlive())
                    d_skeleton.listen_error(p_ioException);
            }
            // If explicitly stopped the skeleton server.
            // If not done this, then the conformance test of SkeletonTest will fail because of time out.
            // isAlive is synchronized method
            if (!d_skeleton.isAlive()) {
                d_skeleton.stopped(null);
                break;
            }
        }
    }

    /**
     * Creates and starts a thread representing the client's new connection.
     *
     * @param clientSocket Client socket.
     */
    private void createClientHandler(Socket clientSocket) {
        SocketClientHandler l_clientHandler = new SocketClientHandler(clientSocket, d_skeleton);
        l_clientHandler.start();
        d_socketClientHandlers.add(l_clientHandler);
    }

    /**
     * Gets the thread representing this implementing class of <code>Runnable</code> interface.
     *
     * @return Value of the thread.
     */
    public Thread getThread() {
        return this.d_thread;
    }

    /**
     * To check if thread is alive or not.
     *
     * @return True if the thread is running.
     */
    public boolean isThreadAlive() {
        return this.d_isListening;
    }

    /**
     * Terminate thread gracefully.
     */
    public void terminate() {
        // TODO Let SocketClientHandler complete their execution.
//        for (SocketClientHandler l_clientHandler : d_socketClientHandlers) {
//            try {
//                l_clientHandler.getThread().join();
//            } catch (InterruptedException l_ignored) {
//            }
//        }
        if (!this.d_isListening) {
            return;
        }
        if (this.d_thread.isAlive())
            this.d_thread.interrupt();
        try {
            d_serverSocket.close();
        } catch (IOException p_ioException) {
        } finally {
            d_skeleton.stopped(null);
            this.d_isListening = false;
        }
    }
}