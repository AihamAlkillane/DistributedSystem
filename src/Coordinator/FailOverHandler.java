package Coordinator;

import Utils.Operation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FailOverHandler {

    private final Map<String, List<Operation>> nodeFailures = new HashMap<>();

    public void logFailure(String name, Operation op) {
        if (!nodeFailures.containsKey(name)) {
            nodeFailures.put(name, new ArrayList<>());
            nodeFailures.get(name).add(op);
        } else {
            nodeFailures.get(name).add(op);
        }
    }

    public List<Operation> getFailures(String name) {
        return nodeFailures.getOrDefault(name, new ArrayList<>());
    }

    public void clearFailures(String name) {
        nodeFailures.remove(name);
    }

    public boolean hasFailures(String name) {
        return nodeFailures.containsKey(name) && !nodeFailures.get(name).isEmpty();
    }
}

