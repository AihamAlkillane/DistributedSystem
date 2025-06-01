package Client;

import Client.UI.TextEditor;
import Client.UI.TextReader;
import Common.Coordinator;
import Common.Models.User;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.rmi.Naming;
import java.util.List;
import java.util.*;

public class ClientUI extends JFrame {

    private Coordinator coordinator;
    private User user;
    private String token;

    private JComboBox<String> departmentBox;
    private JList<String> fileList;

    public ClientUI() {
        setTitle("RMI File Client");
        setSize(400, 300);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initLogin();
    }

    private void initLogin() {
        JTextField usernameField = new JTextField();
        JPasswordField passwordField = new JPasswordField();
        Object[] message = {"Username:", usernameField, "Password:", passwordField};

        int option = JOptionPane.showConfirmDialog(null, message, "Login", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            try {
                coordinator = (Coordinator) Naming.lookup("rmi://localhost:1099/Coordinator");
                user = coordinator.login(usernameField.getText(), new String(passwordField.getPassword()));
                token = user.getToken();
                initMainMenu();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Login failed: " + e.getMessage());
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    private void initMainMenu() {
        JPanel panel = new JPanel(new GridLayout(5, 1, 10, 10));
        JButton uploadBtn = new JButton("Upload File");
        JButton deleteBtn = new JButton("Delete File");
        JButton readBtn = new JButton("Read File");
        JButton editBtn = new JButton("Edit File");
        JButton exitBtn = new JButton("Exit");

        uploadBtn.addActionListener(e -> uploadFile());
        deleteBtn.addActionListener(e -> deleteFile());
        readBtn.addActionListener(e -> readFile());
        editBtn.addActionListener(e -> editFile());
        exitBtn.addActionListener(e -> System.exit(0));

        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        panel.add(uploadBtn);
        panel.add(deleteBtn);
        panel.add(readBtn);
        panel.add(editBtn);
        panel.add(exitBtn);

        setContentPane(panel);
        setVisible(true);
    }

    private void uploadFile() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            java.io.File file = fileChooser.getSelectedFile();
            try {
                List<String> lines = Files.readAllLines(file.toPath());
                boolean requestResult = coordinator.addFile(token, file.getName(), lines);
                if (requestResult)
                    JOptionPane.showMessageDialog(this, "file successfully uploaded");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Upload failed: " + e.getMessage());
            }
        }
    }


    private void deleteFile() {
        String fileName = JOptionPane.showInputDialog(this, "Enter file name to delete:");
        if (fileName != null && !fileName.isBlank()) {
            try {
                boolean result = coordinator.deleteFile(token, fileName);
                if (result)
                    JOptionPane.showMessageDialog(this, "file successfully deleted");
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
            }
        }
    }

    private void readFile() {
        String[] departments = {"development", "graphic design", "QA"};
        String department = (String) JOptionPane.showInputDialog(this, "Choose department:", "Department",
                JOptionPane.QUESTION_MESSAGE, null, departments, departments[0]);

        if (department != null) {
            try {
                Set<String> files = coordinator.getFiles(token, department);
                if (files.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "No files in selected department.");
                    return;
                }

                String selectedFile = (String) JOptionPane.showInputDialog(this, "Choose file:", "Files",
                        JOptionPane.QUESTION_MESSAGE, null, files.toArray(), files.iterator().next());

                if (selectedFile != null) {
                    List<String> fileData = coordinator.readFile(token, selectedFile, department);
                    SwingUtilities.invokeLater(() -> new TextReader(selectedFile, fileData, coordinator, token));
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Read failed: " + e.getMessage());
            }
        }
    }

    private void editFile() {
        try {
            Set<String> files = coordinator.getFiles(token, null);
            if (files.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No files found.");
                return;
            }

            String selectedFile = (String) JOptionPane.showInputDialog(this, "Choose file to edit:", "Edit File",
                    JOptionPane.QUESTION_MESSAGE, null, files.toArray(), files.iterator().next());

            if (selectedFile != null) {
                List<String> fileLines = coordinator.readFile(token, selectedFile, user.getRole());
                SwingUtilities.invokeLater(() ->
                        new TextEditor(selectedFile, fileLines, coordinator, token, selectedFile));
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Edit failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientUI::new);
    }
}
