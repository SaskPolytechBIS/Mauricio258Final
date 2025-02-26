package JavaServer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class GUIConsole extends JFrame implements ChatIF {
    // GUI components
    private JButton closeB = new JButton("Close");
    private JButton openB = new JButton("Open");
    private JButton sendB = new JButton("Send");
    private JButton quitB = new JButton("Quit");
    private JButton loginB = new JButton("Login");

    private JTextField portTxF = new JTextField("5555");
    private JTextField hostTxF = new JTextField("127.0.0.1");
    private JTextField messageTxF = new JTextField("");
    private JTextField loginTxF = new JTextField("");

    private JLabel portLB = new JLabel("Port: ", JLabel.RIGHT);
    private JLabel hostLB = new JLabel("Host: ", JLabel.RIGHT);
    private JLabel messageLB = new JLabel("Message: ", JLabel.RIGHT);
    private JLabel loginLB = new JLabel("Username: ", JLabel.RIGHT);

    private JTextArea messageList = new JTextArea();
    private ChatClient client; // ChatClient instance

    // Constructor
    public GUIConsole() {
        super("Simple Chat GUI");
        setSize(400, 500);
        setLayout(new BorderLayout(5, 5));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel bottom = new JPanel();
        add("Center", new JScrollPane(messageList)); // Scrollable message area
        add("South", bottom);

        bottom.setLayout(new GridLayout(6, 2, 5, 5));
        bottom.add(hostLB); bottom.add(hostTxF);
        bottom.add(portLB); bottom.add(portTxF);
        bottom.add(loginLB); bottom.add(loginTxF);
        bottom.add(messageLB); bottom.add(messageTxF);
        bottom.add(openB); bottom.add(sendB);
        bottom.add(closeB); bottom.add(quitB);
        bottom.add(loginB);

        setVisible(true);

        // Action Listeners
        openB.addActionListener(e -> openConnection());
        closeB.addActionListener(e -> closeConnection());
        sendB.addActionListener(e -> sendMessage());
        quitB.addActionListener(e -> System.exit(0));
        loginB.addActionListener(e -> sendLogin());
    }

    // Display method required by ChatIF
    public void display(String message) {
        messageList.append(message + "\n");
    }

    // Open a connection to the server
    private void openConnection() {
        if (client == null || !client.isConnected()) {
            try {
                String host = hostTxF.getText();
                int port = Integer.parseInt(portTxF.getText());
                client = new ChatClient(host, port, this); // Pass GUIConsole (ChatIF)
                client.openConnection();
                display("Connected to server at " + host + ":" + port);
            } catch (IOException e) {
                display("Error: Could not connect to server.");
            }
        } else {
            display("Already connected to a server.");
        }
    }

    // Close the connection
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

    // Send a login command to the server
    private void sendLogin() {
        if (client != null && client.isConnected()) {
            String userId = loginTxF.getText().trim();
            if (!userId.isEmpty()) {
                Envelope env = new Envelope("login", null, userId);
                try {
                    client.sendToServer(env);
                } catch (IOException e) {
                    display("Error: Could not send login request.");
                }
            } else {
                display("Enter a username to log in.");
            }
        } else {
            display("Error: Not connected to a server.");
        }
    }

    public static void main(String[] args) {
        new GUIConsole();
    }
}
