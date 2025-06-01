package Client;

import Common.ClientCallback;

import javax.swing.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ClientCallbackImpl extends UnicastRemoteObject implements ClientCallback {

    private Runnable onFileUpdated;

    public ClientCallbackImpl(Runnable onFileUpdated) throws RemoteException {
        super();
        this.onFileUpdated = onFileUpdated;
    }

    @Override
    public void notifyFileUpdated(String fileName) throws RemoteException {
        SwingUtilities.invokeLater(onFileUpdated);
    }
}
