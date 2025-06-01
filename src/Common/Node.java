package Common;

import Utils.Operation;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

public interface Node extends Remote {
    Status addFile(String department, String fileName,  List<String> lines) throws RemoteException;

    Status deleteFile(String department, String fileName) throws RemoteException;

    Status updateFile(String department, String fileName, Map<Integer, String> updatedLines) throws RemoteException;

    List<String> readFile(String fileName, String department) throws RemoteException;

    List<String> getFiles(String department) throws RemoteException;

  //  byte[] findFile(String fileName) throws RemoteException;

    String getNodeName() throws RemoteException;

    int getSyncPort() throws RemoteException;

    void triggerSync(List<Operation> operations,int port) throws RemoteException;
}
