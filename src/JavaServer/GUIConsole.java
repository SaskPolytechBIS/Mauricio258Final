package JavaServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class GUIConsole extends JFrame implements ChatIF {
    // GUI components
    private JButton closeB = new JButton("Logoff");
    private JButton openB = new JButton("Login");
    private JButton sendB = new JButton("Send");
    private JButton quitB = new JButton("Quit");
    private JButton browseB = new JButton("Browse");
    private JButton saveB = new JButton("Save");
    private JButton pmB = new JButton("PM");
    private JButton refreshB = new JButton("Refresh List");
    private JButton downloadB = new JButton("Download");

    private JTextField portTxF = new JTextField("5555");
    private JTextField hostTxF = new JTextField("localhost");
    private JTextField messageTxF = new JTextField("");
    private JTextField loginTxF = new JTextField("guest");
    private JTextField fileTxF = new JTextField("No file selected");

    private JLabel portLB = new JLabel("Port: ", JLabel.RIGHT);
    private JLabel hostLB = new JLabel("Host: ", JLabel.RIGHT);
    private JLabel loginLB = new JLabel("User Id: ", JLabel.RIGHT);
    private JLabel messageLB = new JLabel("Message: ", JLabel.RIGHT);
    private JLabel fileLB = new JLabel("File: ", JLabel.RIGHT);

    private JTextArea messageList = new JTextArea();
    private JComboBox<String> userList = new JComboBox<>();
    private ChatClient client; // ChatClient instance
    private File selectedFile; // Store selected file
    private JComboBox<String> fileListCombo = new JComboBox<>();

    // Constructor
    public GUIConsole() {
        super("Simple Chat GUI");
        setSize(400, 500);
        setLayout(new BorderLayout(5, 5));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        add("Center", new JScrollPane(messageList)); // Scrollable message area
        add("South", mainPanel);

        JPanel row1 = new JPanel(new GridLayout(1, 2, 5, 5));
        row1.add(hostLB); row1.add(hostTxF);
        mainPanel.add(row1);

        JPanel row2 = new JPanel(new GridLayout(1, 2, 5, 5));
        row2.add(portLB); row2.add(portTxF);
        mainPanel.add(row2);

        JPanel row3 = new JPanel(new GridLayout(1, 2, 5, 5));
        row3.add(loginLB); row3.add(loginTxF);
        mainPanel.add(row3);

        JPanel row4 = new JPanel(new GridLayout(1, 2, 5, 5));
        row4.add(messageLB); row4.add(messageTxF);
        mainPanel.add(row4);

        JPanel row5 = new JPanel(new GridLayout(1, 2, 5, 5));
        row5.add(new JLabel("User List: ", JLabel.RIGHT)); row5.add(userList);
        mainPanel.add(row5);

        JPanel row6 = new JPanel(new GridLayout(1, 2, 5, 5));
        row6.add(pmB); row6.add(sendB);
        mainPanel.add(row6);

        JPanel row7 = new JPanel(new GridLayout(1, 2, 5, 5));
        row7.add(openB); row7.add(closeB);
        mainPanel.add(row7);

        JPanel row8 = new JPanel(new GridLayout(1, 2, 5, 5));
        row8.add(browseB); row8.add(saveB);
        mainPanel.add(row8);

        // Add file list row (Row 9)
        JPanel row9 = new JPanel(new GridLayout(1, 3, 5, 5));
        row9.add(new JLabel("Files:", JLabel.RIGHT));
        row9.add(fileListCombo);
        row9.add(refreshB);
        mainPanel.add(row9);

        // Add download button row (Row 10)
        JPanel row10 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row10.add(downloadB);
        mainPanel.add(row10);

        // Add Quit button row (Row 11)
        JPanel row11 = new JPanel(new FlowLayout(FlowLayout.CENTER));
        row11.add(quitB);
        mainPanel.add(row11);
        
        setVisible(true);

        // Action Listeners
        openB.addActionListener(e -> sendLogin());
        closeB.addActionListener(e -> closeConnection());
        sendB.addActionListener(e -> sendMessage());
        quitB.addActionListener(e -> System.exit(0));
        browseB.addActionListener(e -> browseFile());
        saveB.addActionListener(e -> saveFileToServer());
        pmB.addActionListener(e -> sendPrivateMessage());
        refreshB.addActionListener(e -> requestFileList());
        downloadB.addActionListener(e -> downloadSelectedFile());
    }

    // Display method required by ChatIF
    public void display(String message) {
        messageList.append(message + "\n");
    }

    public void updateFileList(String[] files) {
        fileListCombo.removeAllItems();
        for (String file : files) {
            fileListCombo.addItem(file);
        }
        display("File list updated.");
    }

    // Browse for a file
    private void browseFile() {
        JFileChooser fileChooser = new JFileChooser();
        int returnValue = fileChooser.showOpenDialog(this);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            fileTxF.setText(selectedFile.getAbsolutePath());
        }
    }

    // Save file to server
    private void saveFileToServer() {
        if (client != null && client.isConnected() && selectedFile != null) {
            try {
                byte[] fileBytes = Files.readAllBytes(Paths.get(selectedFile.getAbsolutePath()));
                Envelope env = new Envelope("ftpUpload", selectedFile.getName(), fileBytes);
                client.sendToServer(env);
                display("File " + selectedFile.getName() + " sent to server.");
            } catch (IOException e) {
                display("Error: Could not read file.");
            }
        } else {
            display("Error: No file selected or not connected to a server.");
        }
    }
    
    private void requestFileList() {
        if (client != null && client.isConnected()) {
            try {
                Envelope env = new Envelope("ftplist", null, null);
                client.sendToServer(env);
            } catch (IOException e) {
                display("Error: Could not request file list.");
            }
        } else {
            display("Not connected to server.");
        }
    }
    
    private void downloadSelectedFile() {
        if (client != null && client.isConnected()) {
            String selectedFile = (String) fileListCombo.getSelectedItem();
            if (selectedFile != null) {
                try {
                    Envelope env = new Envelope("ftpget", selectedFile, null);
                    client.sendToServer(env);
                    display("Requesting file: " + selectedFile);
                } catch (IOException e) {
                    display("Error: Could not request file.");
                }
            } else {
                display("No file selected.");
            }
        } else {
            display("Not connected to server.");
        }
    }
    
    // Send a login command to the server
    private void sendLogin() {
        if (client == null || !client.isConnected()) {
            try {
                String host = hostTxF.getText().trim();
                int port = Integer.parseInt(portTxF.getText().trim());
                client = new ChatClient(host, port, this);
                client.openConnection();

                String userId = loginTxF.getText().trim();
                if (!userId.isEmpty()) {
                    Envelope env = new Envelope("login", null, userId);
                    client.sendToServer(env);
                    display("Logged in as: " + userId);
                } else {
                    display("Enter a username to log in.");
                }
            } catch (IOException e) {
                display("Error: Could not connect to server.");
            }
        } else {
            display("Already connected to a server.");
        }
    }

    // Close the connection to the server
    private void closeConnection() {
        if (client != null && client.isConnected()) {
            try {
                client.closeConnection();
                display("Disconnected from server.");
            } catch (IOException e) {
                display("Error: Could not disconnect.");
            }
        } else {
            display("Not connected.");
        }
    }

    // Send a message to the server
    private void sendMessage() {
        if (client != null && client.isConnected()) {
            String message = messageTxF.getText().trim();
            if (!message.isEmpty()) {
                client.handleMessageFromClientUI(message);
                messageTxF.setText("");
            }
        } else {
            display("Error: Not connected to a server.");
        }
    }

    // Send a private message
    private void sendPrivateMessage() {
        if (client != null && client.isConnected()) {
            String targetUser = (String) userList.getSelectedItem();
            String msg = messageTxF.getText().trim();
            
            if (targetUser != null && !msg.isEmpty()) {
                Envelope env = new Envelope("pm", targetUser, msg);
                try {
                    client.sendToServer(env);
                    display("You (PM to " + targetUser + "): " + msg);
                } catch (IOException e) {
                    display("Error: Could not send private message.");
                }
            } else {
                display("Select a user and enter a message to send a PM.");
            }
        } else {
            display("Error: Not connected to a server.");
        }
    }

    public static void main(String[] args) {
        new GUIConsole();
    }
}
