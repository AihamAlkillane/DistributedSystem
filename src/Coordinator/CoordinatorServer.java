package Coordinator;

import Common.Coordinator;
import Utils.Operation;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CoordinatorServer {

    public static void main(String[] args) {
        try {
            try {
                LocateRegistry.createRegistry(1099);
                System.out.println("Coordinator RMI Registry started.");
            } catch (Exception e) {
                System.out.println("Registry already exists.");
            }
            CoordinatorImpl coordinator = new CoordinatorImpl();
            Naming.rebind("rmi://localhost:1099/Coordinator", coordinator);
            System.out.println("Coordinator ready on port 1099");
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(() -> {
                syncFiles(coordinator.connectedNodes, coordinator);

            }, 0, 2, TimeUnit.MINUTES);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void syncFiles(List<NodeRecord> connectedNodes, CoordinatorImpl coordinator) {
        for (NodeRecord sourceNode : connectedNodes) {
            List<Operation> operations;
            operations = coordinator.logger.getActivity(sourceNode.getNodeName());

            for (NodeRecord targetNode : connectedNodes) {
                if (!sourceNode.equals(targetNode)) {
                    try {
                        int targetPort = targetNode.getSyncPort();
                        sourceNode.getNode().triggerSync(operations, targetPort);
                    } catch (Exception e) {
                        System.err.println("Failed to sync from " + sourceNode.getNodeName()
                                + " to " + targetNode.getNodeName());
                        e.printStackTrace();
                    }
                }
            }
        }
        coordinator.logger.clearActivity();
    }
}
