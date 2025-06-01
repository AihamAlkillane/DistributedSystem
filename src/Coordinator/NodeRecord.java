package Coordinator;

import Common.Node;

import java.io.Serializable;

public class NodeRecord implements Serializable {
    private String nodeName;
    private Boolean isAlive;
    private Node node;
    private int syncPort;
    //public List<FailedOp> failureLog = new ArrayList<FailedOp>();

    public NodeRecord() {
    }

    public NodeRecord(String nodeName, Node node, int syncPort) {
        this.nodeName = nodeName;
        this.node = node;
        this.isAlive = true;
        this.syncPort = syncPort;
    }

    public int getSyncPort() {
        return syncPort;
    }
    public Node getNode() {
        return node;
    }

    public String getNodeName() {
        return nodeName;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean isAlive) {
        this.isAlive = isAlive;
    }
}


//    public void setNodeName(String nodeName) {
//        this.nodeName = nodeName;
//    }
//
//    public int getNodeID() {
//        return nodeID;
//    }
//
//    public void setNodeID(int nodeID) {
//        this.nodeID = nodeID;
//    }
//
//    public int getRmiPort() {
//        return rmiPort;
//    }
//
//    public void setRmiPort(int rmiPort) {
//        this.rmiPort = rmiPort;
//    }
//
//    public int getSyncPort() {
//        return syncPort;
//    }
//
//    public void setSyncPort(int syncPort) {
//        this.syncPort = syncPort;
//    }
