package Node;

import Common.Node;
import Common.Status;
import Node.Sync.SyncClient;
import Utils.Operation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NodeImpl extends UnicastRemoteObject implements Node {
    private Path nodePath;
    private String nodeName;
    private int port;
    private int syncPort;

    @Override
    public String getNodeName() {
        return nodeName;
    }

    @Override
    public int getSyncPort() {
        return syncPort;
    }

    public void setSyncPort(int syncPort) {
        this.syncPort = syncPort;
    }
    public NodeImpl(String nodeName, Path nodePath, int syncPort) throws RemoteException {
        super();
        this.nodeName = nodeName;
        this.nodePath = nodePath;
        this.syncPort = syncPort;
    }

    @Override
    public Status addFile(String department, String fileName, List<String> lines) throws RemoteException {
        Path outputPath = Paths.get(nodePath.toString(), department, fileName);

        try {
            if (Files.exists(outputPath)) {
                return Status.already_exists;
            }

            Files.write(outputPath, lines, StandardOpenOption.CREATE_NEW);
            return Status.success;
        } catch (IOException e) {
            return Status.failed;
        }
    }


    @Override
    public Status deleteFile(String department, String fileName) throws RemoteException {
        Path outputPath = Paths.get(nodePath.toString(), department, fileName);
        try {
            if (Files.notExists(outputPath)) {
                return Status.not_found;
            }
            Files.delete(outputPath);
            return Status.success;
        } catch (IOException e) {
            return Status.failed;
        }
    }

    @Override
    public Status updateFile(String department, String fileName, Map<Integer, String> updatedLines) throws RemoteException {
        Path filePath = nodePath.resolve(Paths.get(department, fileName));

        if (!Files.exists(filePath)) {
            return Status.not_found;
        }
        try {
            List<String> lines = Files.readAllLines(filePath);
            for (Map.Entry<Integer, String> entry : updatedLines.entrySet()) {
                int lineNumber = entry.getKey();
                String newContent = entry.getValue();
                while (lines.size() < lineNumber) {
                    lines.add("");
                }
                lines.set(lineNumber - 1, newContent);
            }
            Files.write(filePath, lines, StandardOpenOption.TRUNCATE_EXISTING);
            return Status.success;

        } catch (IOException e) {
            e.printStackTrace();
            return Status.failed;
        }
    }

    @Override
    public List<String> getFiles(String department) throws RemoteException {
        Path folderPath = nodePath.resolve(department); // safer than manual concatenation
        List<String> fileList = new ArrayList<>();

        File folder = folderPath.toFile(); // convert Path to File
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                fileList.add(file.getName());
            }
        }

        return fileList;
    }

    @Override
    public List<String> readFile(String fileName, String department) throws RemoteException {
        Path filePath = nodePath.resolve(Paths.get(department, fileName));

        if (!Files.exists(filePath)) {
            throw new RemoteException("File not found");
        }
        try {
            return Files.readAllLines(filePath);

        }
        catch (IOException e) {
            throw new RuntimeException("Failed to read file: " + fileName, e);
        }
    }

//    @Override
//    public byte[] findFile(String fileName) throws RemoteException {
//        try {
//            Path baseDir = Paths.get(this.nodeFilePath);
//
//            try (var paths = java.nio.file.Files.walk(baseDir)) {
//                return paths
//                        .filter(java.nio.file.Files::isRegularFile)
//                        .filter(path -> path.getFileName().toString().equals(fileName))
//                        .findFirst()
//                        .map(path -> {
//                            try {
//                                return java.nio.file.Files.readAllBytes(path);
//                            } catch (Exception e) {
//                                e.printStackTrace();
//                                return null;
//                            }
//                        })
//                        .orElse(null);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }

    @Override
    public void triggerSync(List<Operation> operations,int targetPort) throws RemoteException {
        new Thread(() -> {
            SyncClient client = new SyncClient(operations,nodePath); // customize this constructor
            client.sync(targetPort);
        }).start();
    }
}
