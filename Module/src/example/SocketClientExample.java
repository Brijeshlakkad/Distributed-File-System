package example;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class SocketClientExample {
    public static void main(String[] args) {
        SocketClientExample l_rmiExample = new SocketClientExample();
        l_rmiExample.connectToRMI();
    }

    public void connectToRMI() {
        Socket l_socket = new Socket();
        InetSocketAddress l_inetSocketAddress = new InetSocketAddress(1099);
        try {
            l_socket.connect(l_inetSocketAddress);
            System.out.print("Connected");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
