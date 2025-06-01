package Common;

import Common.Models.User;
import Coordinator.NodeRecord;
import Coordinator.ActivityLogger;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Coordinator extends Remote {
    void registerNode(Node node) throws RemoteException;

    User login(String userName, String password) throws RemoteException;

    boolean addFile(String token, String fileName,  List<String> lines) throws RemoteException;

    boolean deleteFile(String token, String fileName) throws RemoteException;

    boolean updateFile(String token, String fileName, Map<Integer, String> updatedLines) throws RemoteException;

    Set<String> getFiles(String token, String department) throws RemoteException;

    List<String> readFile(String token, String fileName, String department) throws RemoteException;

    void registerFileListener(String token, String fileName, ClientCallback callback) throws RemoteException;

    void unregisterFileListener(String token, String fileName, ClientCallback callback) throws RemoteException;

    List<NodeRecord> getConnectedNodes()throws RemoteException;

    ActivityLogger getActivityLogger() throws RemoteException;
}
