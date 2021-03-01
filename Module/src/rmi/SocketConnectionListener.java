package rmi;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

class SocketConnectionListener implements Runnable {
    private String d_name;
    private Thread d_thread;
    private boolean d_isListening;
    private ServerSocket d_serverSocket;
    private List<SocketClientHandler> d_socketClientHandlers;

    public SocketConnectionListener(ServerSocket p_serverSocket) {
        d_thread = new Thread(this);
        d_serverSocket = p_serverSocket;
        d_socketClientHandlers = new ArrayList<>();
    }

    public void start() {
        d_isListening = true;
        d_thread.start();
    }

    public void run() {
        // Thread responsive to interruption.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Socket clientSocket = d_serverSocket.accept();
                this.createClientHandler(clientSocket);
            } catch (IOException p_ioException) {
                this.d_isListening = false;
            }
        }
    }

    private void createClientHandler(Socket clientSocket) {
        SocketClientHandler l_clientHandler = new SocketClientHandler(clientSocket);
        l_clientHandler.start();
        d_socketClientHandlers.add(l_clientHandler);
    }
}