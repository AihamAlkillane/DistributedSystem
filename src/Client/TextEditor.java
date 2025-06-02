package Client;

import Common.Coordinator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;

public class TextEditor extends JFrame {

    private JTextArea textArea;
    private Coordinator coordinator;
    private String token;
    private String fileName;
    private String[] originalLines;

    public TextEditor(String title, List<String>fileLines, Coordinator coordinator, String token, String fileName) {
        this.coordinator = coordinator;
        this.token = token;
        this.fileName = fileName;
        this.textArea = new JTextArea();
        setTitle("Text Editor – " + title);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI(fileLines);
        setVisible(true);
    }

    private void initUI(List<String> fileLines) {
        textArea.setText(String.join("\n", fileLines));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JScrollPane scrollPane = new JScrollPane(textArea);
        JButton saveButton = new JButton("Save");
        JButton cancelButton = new JButton("Cancel");

        saveButton.addActionListener(this::onSave);
        cancelButton.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        originalLines = textArea.getText().split("\r?\n", -1);
    }

    private void onSave(ActionEvent e) {
        String[] currentLines = textArea.getText().split("\r?\n", -1);
        Map<Integer, String> changes = new HashMap<>();

        for (int i = 0; i < Math.max(originalLines.length, currentLines.length); i++) {
            String original = i < originalLines.length ? originalLines[i] : "";
            String current = i < currentLines.length ? currentLines[i] : "";
            if (!original.equals(current)) {
                changes.put(i + 1, current);
            }
        }

        try {
            boolean success = coordinator.updateFile(token, fileName, changes);
            JOptionPane.showMessageDialog(this, success ? "Saved successfully!" : "Failed to save.");
        } catch (RemoteException ex) {
            JOptionPane.showMessageDialog(this, "Server error: " + ex.getMessage());
        }
    }
}
