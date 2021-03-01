package example;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author Brijesh Lakkad
 * @version 1.0
 */
public interface HelloInterface extends Remote {
    void print(String message) throws RemoteException;
}
