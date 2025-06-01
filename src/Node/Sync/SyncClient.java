package Node.Sync;

import Utils.Operation;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class SyncClient {

    List<Operation> operations;  Path nodePath;
    SyncClient (List<Operation> operations, Path nodePath) {
        this.operations = operations;
        this.nodePath = nodePath;
    }
    public void sync(int targetPort) {
        try (
                Socket socket = new Socket("localhost", targetPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream())
        ) {
            for (Operation op : operations) {
                switch (op.getType()) {
                    case ADD:
                        List<String> content = Files.readAllLines(Paths.get(nodePath.toString(),op.getDepartment(),op.getFileName()));
                        op.setFileContents(content);
                        break;
                    case UPDATE:
                    case DELETE:
                        break;
                }
            }

            out.writeObject(operations);
            out.flush();
            String response = in.readUTF();

            if ("SYNC_COMPLETE".equals(response)) {
                System.out.println("Sync to "  + ":" + targetPort + " completed successfully.");
            } else {
                System.err.println("Unexpected response: " + response);
            }

        } catch (Exception e) {
            System.err.println("Sync failed to " + ":" + targetPort);
            e.printStackTrace();
        }
    }
}
