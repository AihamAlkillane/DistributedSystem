package Coordinator;

import Common.ClientCallback;
import Common.Coordinator;
import Common.Models.User;
import Common.Node;
import Common.Status;
import Utils.Operation;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class CoordinatorImpl extends UnicastRemoteObject implements Coordinator {

    private int nextNodeIndex = 0;
    public List<NodeRecord> connectedNodes = new ArrayList<>();
    private final Map<String, Set<ClientCallback>> fileListeners = new HashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ActivityLogger logger;
    private final Auth auth;
    private int attempts = 0;


    public CoordinatorImpl() throws RemoteException {
        super();
        auth = new Auth();
        logger = new ActivityLogger();
    }

    private NodeRecord getNextAvailableNodeRecord() {
        while (attempts > 0) {
            NodeRecord record = connectedNodes.get(nextNodeIndex);
            nextNodeIndex = (nextNodeIndex + 1) % connectedNodes.size();
            if (record.isAlive()) {
                return record;
            }
            attempts--;
        }
        return null;
    }

    @Override
    public void registerNode(Node node) throws RemoteException {

        for (NodeRecord record : connectedNodes) {

            String recordNodeName = record.getNodeName();
            String nodeName = node.getNodeName();
            if (recordNodeName.equals(nodeName)) {
                record.setNode(node);
                record.setAlive(true);
                System.out.println("Node Rejoined: " + node.getNodeName());
                return;
            }
        }


        String name = node.getNodeName();
        int syncPort = node.getSyncPort();
        NodeRecord record = new NodeRecord(name, node, syncPort);
        connectedNodes.add(record);
        attempts = connectedNodes.size();
        System.out.println("Node Rejoined: " + name);
    }

    @Override
    public User login(String username, String password) throws RemoteException {
        User user = auth.login(username, password);
        if (user == null)
            throw new RemoteException("Invalid credentials");
        return user;
    }

    @Override
    public boolean addFile(String token, String fileName, List<String> lines) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("Not authorized");

        String department = auth.getDepartment(token);
        String fileKey = fileName + "/" + department;

        Lock lock = locks.computeIfAbsent(fileKey, k -> new ReentrantLock());
        lock.lock();
        try {
            if (logger.addedFiles.containsKey(fileKey))
                throw new RemoteException("File already exists");

            if (logger.deletedFiles.containsKey(fileKey)) {
                logger.deletedFiles.remove(fileKey);
            }
            for (NodeRecord record : connectedNodes) {
                if (!record.isAlive()) continue;
                try {
                    Status status = record.getNode().addFile(department, fileName, lines);
                    if (status == Status.success) {
                        Operation op = new Operation(Operation.OpType.ADD, department, fileName, null);
                        logger.logOperation(record.getNodeName(), op);
                        logger.deletedFiles.remove(fileKey);
                        return true;
                    } else if (status == Status.already_exists)
                        throw new RemoteException("File already exists");
                } catch (RemoteException e) {
                    record.setAlive(false);
                }
            }

        } finally {
            lock.unlock();
        }
        throw new RemoteException("failed to add file :: all nodes have failed");
    }

    @Override
    public boolean deleteFile(String token, String fileName) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("Not authorized");

        String department = auth.getDepartment(token);
        String fileKey = fileName + "/" + department;

        Lock lock = locks.computeIfAbsent(fileKey, k -> new ReentrantLock());
        lock.lock();
        try {
            if (logger.deletedFiles.containsKey(fileKey))
                throw new RemoteException("File not found");

            int notFoundCount = 0;
            for (NodeRecord record : connectedNodes) {
                if (!record.isAlive()) continue;
                try {
                    Status status = record.getNode().deleteFile(department, fileName);
                    if (status == Status.success) {
                        Operation op = new Operation(Operation.OpType.DELETE, department, fileName, null);
                        logger.logOperation(record.getNodeName(), op);
                        logger.updatedFiles.remove(fileKey);
                        logger.addedFiles.remove(fileKey);
                        return true;

                    } else if (status == Status.not_found)
                        notFoundCount++;
                } catch (RemoteException e) {
                    record.setAlive(false);
                }
            }
            if (notFoundCount == connectedNodes.size())
                throw new RemoteException("file not found");
        } finally {
            lock.unlock();
        }

        throw new RemoteException("failed to delete file :: all nodes have failed");
    }

    @Override
    public boolean updateFile(String token, String fileName, Map<Integer, String> updatedLines) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("invalid credentials");

        String department = auth.getDepartment(token);
        String fileKey = fileName + "/" + department;

        Lock lock = locks.computeIfAbsent(fileKey, k -> new ReentrantLock());
        lock.lock();
        try {
            if (logger.deletedFiles.containsKey(fileKey))
                throw new RemoteException("File not found");

            NodeRecord nodeRecord = null;
            String nodeName = null;

            if (logger.addedFiles.containsKey(fileKey)) {
                nodeName = logger.updatedFiles.get(fileKey);
                for (NodeRecord temp : connectedNodes) {
                    if (temp.getNode().getNodeName().equals(nodeName)) {
                        nodeRecord = temp;
                        break;
                    }
                }
                logger.addedFiles.remove(fileKey);
            } else if (logger.updatedFiles.containsKey(fileKey)) {
                nodeName = logger.updatedFiles.get(fileKey);
                for (NodeRecord temp : connectedNodes) {
                    if (temp.getNode().getNodeName().equals(nodeName)) {
                        nodeRecord = temp;
                        break;
                    }
                }
            }
            if (nodeName != null) {
                try {
                    for (NodeRecord temp : connectedNodes) {
                        if (temp.getNodeName().equals(nodeName)) {
                            nodeRecord = temp;
                            break;
                        }
                    }
                    Status status = nodeRecord.getNode().updateFile(department, fileName, updatedLines);
                    if (status == Status.success) {
                        notifyListeners(fileName);
                        Operation op = new Operation(Operation.OpType.UPDATE, department, fileName, updatedLines);
                        logger.logOperation(nodeRecord.getNodeName(), op);
                        return true;
                    }

                } catch (RemoteException e) {
                    nodeRecord.setAlive(false);
                }
            }
            for (NodeRecord record : connectedNodes) {
                try {
                    Status status = record.getNode().updateFile(department, fileName, updatedLines);
                    if (status == Status.success) {
                        notifyListeners(fileName);
                        Operation op = new Operation(Operation.OpType.UPDATE, department, fileName, updatedLines);
                        logger.logOperation(record.getNodeName(), op);
                        return true;
                    }

                } catch (RemoteException e) {
                    record.setAlive(false);
                }
            }
        } finally {
            lock.unlock();
        }
        throw new RemoteException("all nodes have failed");
    }

    @Override
    public Set<String> getFiles(String token, String department) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("Not authorized");

        Set<String> fileNames = new HashSet<>();
        for (NodeRecord record : connectedNodes) {
            if (!record.isAlive()) continue;
            try {
                fileNames.addAll(record.getNode().getFiles(department));
            } catch (Exception e) {
                System.err.println("Failed to fetch files from node: " + e.getMessage());
                record.setAlive(false);
            }
        }
        for (String fileName : fileNames) {
            if (logger.deletedFiles.containsKey(fileName + "/" + department))
                fileNames.remove(fileName);
        }

        return fileNames;
    }

    @Override
    public List<String> readFile(String token, String fileName, String department) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("Not authorized");

        ReentrantLock lock = locks.get(fileName + "/" + department);
        if (lock != null && lock.isLocked()) {
            throw new RemoteException("File is currently being modified.");
        }

        if (logger.updatedFiles.containsKey(fileName + "/" + department)) {
            for (NodeRecord record : connectedNodes) {
                if (record.getNode().getNodeName().equals(logger.updatedFiles.get(fileName + "/" + department))) {
                    return record.getNode().readFile(fileName, department);
                }
            }
        }
        int attempts = connectedNodes.size();
        while (attempts-- > 0) {
            try {
                NodeRecord record = getNextAvailableNodeRecord();
                return record.getNode().readFile(fileName, department);
            } catch (Exception e) {
                System.err.println("Failed to fetch files from node: " + e.getMessage());
            }
        }
        throw new RemoteException("Failed to read file :: all nodes have failed");

    }

    private void notifyListeners(String fileName) {
        Set<ClientCallback> listeners = fileListeners.get(fileName);
        if (listeners != null) {
            for (ClientCallback client : new HashSet<>(listeners)) {
                try {
                    client.notifyFileUpdated(fileName);
                } catch (RemoteException e) {
                    System.err.println("Failed to notify client: " + e.getMessage());
                    listeners.remove(client);
                }
            }
        }
    }

    @Override
    public void registerFileListener(String token, String fileName, ClientCallback callback) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("Not authorized");
        fileListeners.computeIfAbsent(fileName, k -> new HashSet<>()).add(callback);
    }

    @Override
    public void unregisterFileListener(String token, String fileName, ClientCallback callback) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("Not authorized");
        Set<ClientCallback> listeners = fileListeners.get(fileName);
        if (listeners != null) {
            listeners.remove(callback);
            if (listeners.isEmpty()) {
                fileListeners.remove(fileName);
            }
        }
    }

    @Override
    public List<NodeRecord> getConnectedNodes()throws RemoteException{
        return connectedNodes;
    }

    @Override
    public ActivityLogger getActivityLogger() throws RemoteException{
        return this.logger;
    }
}
