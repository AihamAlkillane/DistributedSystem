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

public class CoordinatorImpl extends UnicastRemoteObject implements Coordinator {

    private int nextNodeIndex = 0;
    public List<NodeRecord> connectedNodes = new ArrayList<>();
    private final Map<String, Set<ClientCallback>> fileListeners = new HashMap<>();
    private final Map<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    private final ActivityLogger logger;
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
        if (logger.addedFiles.containsKey(fileKey))
            throw new RemoteException("File already exists");

        if (logger.deletedFiles.containsKey(fileKey)) {
            String nodeName = logger.deletedFiles.get(fileKey);
            logger.removeOperation(Operation.OpType.ADD, fileKey, nodeName);
        }

        Lock lock = locks.computeIfAbsent(department, k -> new ReentrantLock());
        lock.lock();
        try {
            int attempts = connectedNodes.size();
            while (attempts > 0) {
                NodeRecord nodeRecord = getNextAvailableNodeRecord();
                Node node = nodeRecord.getNode();

                if (node.addFile(department, fileName, lines) == Status.success) {
                    Operation op = new Operation(Operation.OpType.ADD, department, fileName, null);
                    logger.logOperation(nodeRecord.getNodeName(), op);
                    return true;
                } else if (node.addFile(department, fileName, lines) == Status.already_exists) {
                    throw new RemoteException("File already exists");
                }
                attempts--;
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

        if (logger.deletedFiles.containsKey(fileName + "/" + department))
            throw new RemoteException("File not found");

        if (logger.addedFiles.containsKey(fileName + "/" + department)) {
            String nodeName = logger.addedFiles.get(fileName + "/" + department);
            logger.removeOperation(Operation.OpType.DELETE, fileName + "/" + department, nodeName);
        }
        int notFoundCount = 0;
        int attempts = connectedNodes.size();

        while (attempts > 0) {
            attempts--;

            NodeRecord nodeRecord = getNextAvailableNodeRecord();
            Node node = nodeRecord.getNode();
            Status status = node.deleteFile(department, fileName);

            switch (status) {
                case success:
                    Operation op = new Operation(Operation.OpType.DELETE, department, fileName, null);
                    logger.logOperation(nodeRecord.getNodeName(), op);
                    return true;
                case failed:
                    continue;
                case not_found:
                    notFoundCount++;
            }
        }
        if (notFoundCount == connectedNodes.size())
            throw new RemoteException("file not found");

        throw new RemoteException("failed to delete file :: all nodes have failed");
    }

    @Override
    public boolean updateFile(String token, String fileName, Map<Integer, String> updatedLines) throws RemoteException {
        if (!auth.authenticate(token))
            return false;

        String department = auth.getDepartment(token);
        String fileKey = fileName + "/" + department;

        if (logger.deletedFiles.containsKey(fileKey))
            throw new RemoteException("File not found");

        NodeRecord nodeRecord = null;

        if (logger.updatedFiles.containsKey(fileKey)) {
            String nodeName = logger.updatedFiles.get(fileKey);
            for (NodeRecord temp : connectedNodes) {
                if (temp.getNode().getNodeName().equals(nodeName)) {
                    nodeRecord = temp;
                    break;
                }
            }

            Node node = nodeRecord.getNode();
            Status status = node.updateFile(department, fileName, updatedLines);
            if (status == Status.success) {
                Operation op = new Operation(Operation.OpType.UPDATE, department, fileName, updatedLines);
                logger.logOperation(nodeRecord.getNodeName(), op);
                notifyListeners(fileName);
                return true;
            }
            throw new RemoteException("file is currently unavailable");
        }
        int attempts = connectedNodes.size();
        while (attempts-- > 0) {
            nodeRecord = getNextAvailableNodeRecord();
            Node node = nodeRecord.getNode();
            Status status;

            Lock lock = locks.computeIfAbsent(fileKey, k -> new ReentrantLock());
            lock.lock();
            try {
                status = node.updateFile(department, fileName, updatedLines);
            } finally {
                lock.unlock();
            }

            if (status == Status.success) {
                Operation op = new Operation(Operation.OpType.UPDATE, department, fileName, updatedLines);
                logger.logOperation(nodeRecord.getNodeName(), op);
                logger.updatedFiles.put(fileKey, nodeRecord.getNodeName());
                notifyListeners(fileName);
                return true;
            }
        }
        throw new RemoteException("Failed to update file :: all nodes have failed");
    }

    @Override
    public Set<String> getFiles(String token, String department) throws RemoteException {
        if (!auth.authenticate(token))
            throw new RemoteException("Not authorized");

        if (department == null) {
            department = auth.getDepartment(token);
        }

        Set<String> fileNames = new HashSet<>();
        for (NodeRecord record : connectedNodes) {
            if (!record.isAlive()) continue;
            try {
                fileNames.addAll(record.getNode().getFiles(department));
            } catch (Exception e) {
                System.err.println("Failed to fetch files from node: " + e.getMessage());
            }
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
        getNextAvailableNodeRecord().getNode().readFile(fileName, department);

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

    public List<NodeRecord> getConnectedNodes() throws RemoteException {
        return connectedNodes;
    }

    public ActivityLogger getActivityLogger() throws RemoteException {
        return logger;
    }
}
