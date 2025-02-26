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

    // Constructor
    public GUIConsole() {
        super("Simple Chat GUI");
        setSize(400, 500);
        setLayout(new BorderLayout(5, 5));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel bottom = new JPanel();
        add("Center", new JScrollPane(messageList)); // Scrollable message area
        add("South", bottom);

        bottom.setLayout(new GridLayout(8, 2, 5, 5));
        bottom.add(hostLB); bottom.add(hostTxF);
        bottom.add(portLB); bottom.add(portTxF);
        bottom.add(loginLB); bottom.add(loginTxF);
        bottom.add(messageLB); bottom.add(messageTxF);
        bottom.add(new JLabel("User List: ", JLabel.RIGHT)); bottom.add(userList);
        bottom.add(pmB); bottom.add(sendB);
        bottom.add(openB); bottom.add(closeB);
        bottom.add(browseB); bottom.add(saveB);
        bottom.add(quitB);

        setVisible(true);

        // Action Listeners
        openB.addActionListener(e -> sendLogin());
        closeB.addActionListener(e -> closeConnection());
        sendB.addActionListener(e -> sendMessage());
        quitB.addActionListener(e -> System.exit(0));
        browseB.addActionListener(e -> browseFile());
        saveB.addActionListener(e -> saveFileToServer());
        pmB.addActionListener(e -> sendPrivateMessage());
    }

    // Display method required by ChatIF
    public void display(String message) {
        messageList.append(message + "\n");
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
                Envelope env = new Envelope("saveFile", selectedFile.getName(), fileBytes);
                client.sendToServer(env);
                display("File " + selectedFile.getName() + " sent to server.");
            } catch (IOException e) {
                display("Error: Could not read file.");
            }
        } else {
            display("Error: No file selected or not connected to a server.");
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
