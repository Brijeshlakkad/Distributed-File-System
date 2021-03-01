package example;

import java.rmi.AlreadyBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class RMIServerExample extends UnicastRemoteObject implements HelloInterface {
    protected RMIServerExample() throws RemoteException {
        super();
    }

    public static void main(String[] args) {
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        RMIServerExample l_rmiExample = null;
        try {
            l_rmiExample = new RMIServerExample();
            l_rmiExample.run();
        } catch (RemoteException p_remoteException) {
            p_remoteException.printStackTrace();
        }
        System.out.print("Exit");
    }

    public void run() {
        try {
            Registry l_registry = LocateRegistry.createRegistry(1099);
            HelloInterface l_helloInterface = this;
            l_registry.bind("HelloWorld", l_helloInterface);
            System.out.println("Server running...");
        } catch (RemoteException p_remoteException) {
            p_remoteException.printStackTrace();
        } catch (AlreadyBoundException p_e) {
            p_e.printStackTrace();
        }
        while (true) {

        }
    }
    public void print(String message) throws RemoteException {
        System.out.println(message);
    }
}
