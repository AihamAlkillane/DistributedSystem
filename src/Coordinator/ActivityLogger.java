package Coordinator;

import Utils.Operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static Utils.Operation.OpType.ADD;

public class ActivityLogger {
    private final Map<String, List<Operation>> nodeActivity = new HashMap<>();
    public Map<String, String> deletedFiles = new HashMap<>(); // file key : fileName/department , NodeName
    public Map<String, String> addedFiles = new HashMap<>(); // file key : fileName/department  , NodeName
    public Map<String, String> updatedFiles = new HashMap<>();

    public void logOperation(String nodeName, Operation op) {
        if (!nodeActivity.containsKey(nodeName)) {
            nodeActivity.put(nodeName, new ArrayList<>());
            nodeActivity.get(nodeName).add(op);
        } else {
            nodeActivity.get(nodeName).add(op);
        }
        switch (op.getType()) {
            case DELETE:
                deletedFiles.put(op.getFileName() + "/" + op.getDepartment(), nodeName);
            case ADD:
                addedFiles.put( op.getFileName() + "/" + op.getDepartment(),nodeName);
        }
    }

    public List<Operation> getActivity(String name) {
        return nodeActivity.getOrDefault(name, new ArrayList<>());
    }

    public void clearActivity() {
        nodeActivity.clear();
    }

    public void removeOperation(Operation.OpType type, String fileKey, String nodeName) {
        String fileName = fileKey.split("/")[0];
        String department = fileKey.split("/")[1];

        List<Operation> ops = nodeActivity.get(nodeName);
        if (ops == null) return;

        ops.removeIf(op ->
                op.getType() == type &&
                        op.getFileName().equals(fileName) &&
                        op.getDepartment().equals(department)
        );

        if (type == Operation.OpType.ADD) {
            deletedFiles.remove(fileKey);
        }
    }

    public boolean hasActivity(String name) {
        return nodeActivity.containsKey(name) && !nodeActivity.get(name).isEmpty();
    }
}

