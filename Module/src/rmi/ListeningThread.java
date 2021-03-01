package rmi;

import java.rmi.AlreadyBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

public class ListeningThread<T extends Remote> implements Runnable {
    private Registry d_registry;
    private String d_name;
    private Thread d_thread;
    private T d_proxyClass;
    private int d_port;
    private boolean d_listening;

    public ListeningThread(Registry p_registry, T p_proxyClass, String p_name, int p_port) {
        d_registry = p_registry;
        d_name = p_name;
        d_thread = new Thread(this, p_name);
        d_proxyClass = p_proxyClass;
        d_port = p_port;
    }

    public void start() {
        d_thread.start();
        d_listening = true;
    }

    public void run() {
        try {
//            Remote stub = UnicastRemoteObject.exportObject(this.d_proxyClass, d_port);
            d_registry.bind(d_name, this.d_proxyClass);
            while (!Thread.currentThread().isInterrupted()) {
            }
//            System.out.print("Done!");
        } catch (RemoteException p_remoteException) {
            p_remoteException.printStackTrace();
        } catch (AlreadyBoundException p_e) {
            p_e.printStackTrace();
        }
    }

    public void terminate() {
        if (d_listening) {
            this.d_thread.interrupt();
            try {
                if (d_registry != null) {
                    if (UnicastRemoteObject.unexportObject(d_registry, true)) {
//                        System.out.println("Registry removed!");
                    } else {
//                        System.out.println("Registry can not be removed.");
                    }
                }
            } catch (RemoteException p_e) {
                p_e.printStackTrace();
            } finally {
                d_listening = false;
            }
        }
    }
}