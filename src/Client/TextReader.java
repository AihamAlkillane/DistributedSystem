package Client;

import Common.ClientCallback;
import Common.Coordinator;

import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;

public class TextReader extends JFrame {

    private Coordinator coordinator;
    private String token;
    private String fileName;
    private ClientCallback callback;
    private JTextArea textArea;
    private JLabel notificationLabel;
    private String department;

    public TextReader(String fileName,String department ,java.util.List<String>fileLines, Coordinator coordinator, String token) {
        this.fileName = fileName;
        this.coordinator = coordinator;
        this.token = token;
        textArea = new JTextArea();
        this.department = department;

        setTitle("Text Reader – " + fileName);
        setSize(700, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        initUI(fileLines);
        registerCallback();
        setVisible(true);
    }

    private void initUI(java.util.List<String>fileLines) {
        textArea.setText(String.join("\n", fileLines));
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
        textArea.setEditable(false);

        JScrollPane scrollPane = new JScrollPane(textArea);

        // Notification banner
        notificationLabel = new JLabel(" ");
        notificationLabel.setForeground(Color.WHITE);
        notificationLabel.setOpaque(true);
        notificationLabel.setBackground(new Color(255, 140, 0)); // Orange
        notificationLabel.setHorizontalAlignment(SwingConstants.CENTER);
        notificationLabel.setVisible(false);

        // Refresh button
        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(e -> {
            System.out.println("Refresh");
            refreshFile();
        });

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(notificationLabel, BorderLayout.CENTER);
        topPanel.add(refreshButton, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    private void registerCallback() {
        try {
            callback = new ClientCallbackImpl(() -> {
                notificationLabel.setText("This file has been updated. Click Refresh to reload.");
                notificationLabel.setVisible(true);
            });
            coordinator.registerFileListener(token, fileName, callback);
        } catch (RemoteException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to register for updates.");
        }
    }

    private void refreshFile() {
        try {
            java.util.List<String> fileLines = coordinator.readFile(token, fileName, department);
            textArea.setText(String.join("\n", fileLines));
            notificationLabel.setVisible(false);
        } catch (RemoteException e) {
            JOptionPane.showMessageDialog(this, "Failed to refresh file: " + e.getMessage());
        }
    }

    @Override
    public void dispose() {
        try {
            if (callback != null) {
                coordinator.unregisterFileListener(token, fileName, callback);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        super.dispose();
    }
}
