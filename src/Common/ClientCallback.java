package Common;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ClientCallback extends Remote {
    void notifyFileUpdated(String fileName) throws RemoteException;
}
