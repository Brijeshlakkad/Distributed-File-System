package rmi;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;

class SocketClientHandler implements Runnable {
    private Socket d_clientSocket;
    private Thread d_thread;
    private boolean d_isListening;

    public SocketClientHandler(Socket p_clientSocket) {
        d_thread = new Thread(this);
        d_clientSocket = p_clientSocket;
    }

    public void start() {
        d_thread.start();
        d_isListening = true;
    }

    public void run() {
        // Thread responsive to interruption.
        while (!Thread.currentThread().isInterrupted()) {
            try {
                ObjectInputStream l_objectInputStream = new ObjectInputStream(d_clientSocket.getInputStream());
                try {
                   Object methodName = l_objectInputStream.readObject();
                } catch (ClassNotFoundException p_e) {
                    p_e.printStackTrace();
                }
            } catch (IOException p_ioException) {
                this.d_isListening = false;
            }
        }
    }
}