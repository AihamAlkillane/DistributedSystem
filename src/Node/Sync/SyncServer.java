package Node.Sync;
import Node.NodeImpl;
import Utils.Operation;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

public class SyncServer extends Thread {
    private int port;
    private NodeImpl nodeImpl;

    public SyncServer(int port, NodeImpl nodeImpl) {
        this.port = port;
        this.nodeImpl = nodeImpl;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket socket) {
        try (
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())
        ) {
            List<Operation> operations = (List<Operation>) in.readObject();

            for (Operation op : operations) {
                switch (op.getType()) {
                    case ADD:
                        nodeImpl.addFile(op.getDepartment(), op.getFileName(), op.getFileContents());
                        break;
                    case DELETE:
                        nodeImpl.deleteFile(op.getDepartment(), op.getFileName());
                        break;
                    case UPDATE:
                        nodeImpl.updateFile(op.getDepartment(), op.getFileName(), op.getUpdatedLines());
                        break;
                }
            }

            out.writeUTF("SYNC_COMPLETE");
            out.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
