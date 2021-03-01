package example;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author Brijesh Lakkad
 * @version 1.0
 */
public class RMIClientExample {
    public static void main(String[] args){
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        RMIClientExample l_rmiExample = new RMIClientExample();
        l_rmiExample.connectToRMI();
    }
    public void connectToRMI(){
        try {
            Registry l_registry = LocateRegistry.getRegistry(1099);
            try {
                HelloInterface l_helloInterface = (HelloInterface) l_registry.lookup("HelloWorld");
                l_helloInterface.print("Brijesh!");
            } catch (NotBoundException p_e) {
                p_e.printStackTrace();
            }
        } catch (RemoteException p_remoteException) {
            p_remoteException.printStackTrace();
        }
    }
}
