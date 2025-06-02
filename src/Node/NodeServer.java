package Node;

import Common.Coordinator;
import Node.Sync.SyncServer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

public class NodeServer {
    public static void main(String[] args) {
        try {
            String nodeName = args.length > 0 ? args[0] : "Node1";
            int rmiPort = args.length > 1 ? Integer.parseInt(args[1]) : 1100;
            int syncPort = args.length > 2 ? Integer.parseInt(args[2]) : 9001;

            Path nodePath = Paths.get("files", nodeName);
            try {
                LocateRegistry.createRegistry(rmiPort);
                System.out.println("RMI registry started on port " + rmiPort);
            } catch (Exception e) {
                System.out.println("RMI registry already running.");
            }
            NodeImpl nodeImpl = new NodeImpl(nodeName, nodePath, syncPort);
            Naming.rebind("rmi://localhost:" + rmiPort + "/" + nodeName, nodeImpl);

            SyncServer syncServer = new SyncServer(syncPort, nodeImpl);
            syncServer.start();
            System.out.println("SyncServer started on port " + syncPort);

            Coordinator coordinator = (Coordinator) Naming.lookup("rmi://localhost:1099/Coordinator");
            coordinator.registerNode(nodeImpl);

            System.out.println("Node Registered :" + nodeName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
