package Utils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Operation implements Serializable {
    public enum OpType {UPDATE, DELETE, ADD}

    private final OpType type;
    private final String department;
    private final String fileName;
    private final Map<Integer, String> updatedLines;
    List<String> fileContents = new ArrayList<>();

    public Operation(OpType type, String department, String fileName, Map<Integer, String> updatedLines) {
        //this.nodeName = nodeName;
        this.type = type;
        this.fileName = fileName;
        this.updatedLines = updatedLines;
        this.department = department;
    }
    public OpType getType() {
        return type;
    }

    public String getDepartment() {
        return department;
    }

    public String getFileName() {
        return fileName;
    }

    public Map<Integer, String> getUpdatedLines() {
        return updatedLines;
    }
    public void setFileContents(List<String> fileContents) {
        this.fileContents = fileContents;
    }
    public List<String> getFileContents() {
        return fileContents;
    }
}
