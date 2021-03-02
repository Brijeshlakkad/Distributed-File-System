package rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class SocketConnectionListener implements Runnable {
    private final Skeleton<?> d_skeleton;
    private final Thread d_thread;
    private boolean d_isListening;
    private final ServerSocket d_serverSocket;
    private final List<SocketClientHandler> d_socketClientHandlers;

    public SocketConnectionListener(ServerSocket p_serverSocket, Skeleton<?> p_skeleton) {
        d_thread = new Thread(this);
        d_serverSocket = p_serverSocket;
        d_socketClientHandlers = new ArrayList<>();
        d_skeleton = p_skeleton;
    }

    public void start() {
        d_isListening = true;
        d_thread.start();
    }

    public void run() {
        // Sets the listen error handler. If exception not handled manually, ListenErrorHandler will handle it.
        Thread.setDefaultUncaughtExceptionHandler(new ListenErrorHandler(d_skeleton));
        try {
            // Thread responsive to interruption.
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = d_serverSocket.accept();
                    this.createClientHandler(clientSocket);
                } catch (IOException p_ioException) {
                    this.d_isListening = false;
                    d_skeleton.listen_error(p_ioException);
                }
            }
        } finally {
            try {
                d_serverSocket.close();
            } catch (IOException p_ioException) {
                d_skeleton.listen_error(p_ioException);
            }
            // If server socket closed explicitly stop the server.
            // If not done this, then the conformance test of SkeletonTest will fail because of time out.
            synchronized (d_skeleton) {
                d_skeleton.stopped(null);
            }
            this.d_isListening = false;
        }
    }

    private void createClientHandler(Socket clientSocket) {
        SocketClientHandler l_clientHandler = new SocketClientHandler(clientSocket, d_skeleton);
        l_clientHandler.start();
        d_socketClientHandlers.add(l_clientHandler);
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
        if (this.d_thread.isAlive())
            this.d_thread.interrupt();
    }
}