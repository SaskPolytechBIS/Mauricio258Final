package JavaServer;


import java.io.*;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

/**
 * This class overrides some of the methods defined in the abstract superclass
 * in order to give more functionality to the client.
 */
public class ChatClient extends AbstractClient {
    //Instance variables **********************************************

    /**
     * The interface type variable. It allows the implementation of the display
     * method in the client.
     */
    ChatIF clientUI;

    //Constructors ****************************************************
    /**
     * Constructs an instance of the chat client.
     *
     * @param host The server to connect to.
     * @param port The port number to connect on.
     * @param clientUI The interface type variable.
     */
    public ChatClient(String host, int port, ChatIF clientUI)
            throws IOException {
        super(host, port); //Call the superclass constructor
        this.clientUI = clientUI;
        //openConnection();
    }

    //Instance methods ************************************************
    /**
     * This method handles all data that comes in from the server.
     *
     * @param msg The message from the server.
     */
    public void handleMessageFromServer(Object msg) {
        if(msg instanceof Envelope)
        {
            handleServerCommand((Envelope) msg);
        }
        else
        {
            clientUI.display(msg.toString());
        }
    }

    
    public void handleServerCommand(Envelope env)
    {
        if (env.getName().equals("ftplist")) {
            String[] files = (String[]) env.getMsg();
            
            // Populate the combo box in GUIConsole
            SwingUtilities.invokeLater(() -> {
                GUIConsole gui = (GUIConsole) clientUI;
                gui.updateFileList(files);
            });
        }
        
        if (env.getName().equals("ftpget")) {
            String fileName = env.getArg();
            Object data = env.getMsg();

            if (data instanceof String) {
                clientUI.display((String) data); // Display error message if any
                return;
            }

            byte[] fileData = (byte[]) data;
            File downloadFolder = new File("downloads");
            downloadFolder.mkdirs(); // Ensure downloads folder exists

            File downloadedFile = new File(downloadFolder, fileName);
            try (FileOutputStream fos = new FileOutputStream(downloadedFile)) {
                fos.write(fileData);
                clientUI.display("File downloaded: " + fileName);
            } catch (IOException e) {
                clientUI.display("Error saving downloaded file.");
            }
        }
            
        if(env.getName().equals("who"))
        {
            ArrayList<String> clientsInRoom = (ArrayList<String>) env.getMsg();
            
            clientUI.display("---Users in room---");
            for(int i = 0; i < clientsInRoom.size(); i++)
            {
                clientUI.display(clientsInRoom.get(i));
            }
        }
    }

    /**
     * This method handles all data coming from the UI
     *
     * @param message The message from the UI.
     */
    public void handleMessageFromClientUI(String message) {

        //if the first character in our message is a # treat it as a command
        if (message.charAt(0) == '#') {

            handleClientCommand(message);

        } else {
            try {
                sendToServer(message);
            } catch (IOException e) {
                clientUI.display("Could not send message to server.  Terminating client.......");
                quit();
            }
        }
    }

    /**
     * This method terminates the client.
     */
    public void quit() {
        try {
            closeConnection();
        } catch (IOException e) {
        }
        System.exit(0);
    }

    public void connectionClosed() {

        System.out.println("Connection closed");

    }
    
    protected void connectionException(Exception exception) {

        System.out.println("The server has shut down");

    }

    //compare message to premade command strings
    //if the message matches perform that command
    public void handleClientCommand(String message) {
        
        //disconnects from server and shuts down client
        if (message.equals("#quit")) {
            clientUI.display("Shutting Down Client");
            quit();

        }

        //disconnect from server
        if (message.equals("#logoff")) {
            clientUI.display("Disconnecting from server");
            try {
                closeConnection();
            } catch (IOException e) {
            };

        }

        //changes the host that the socket attempts to connect to
        //format: #setHost hostName
        if (message.indexOf("#setHost") >= 0) {

            if (isConnected()) {
                clientUI.display("Cannot change host while connected");
            } else {
                //#setHost localhost
                //localhost
                String newHost = message.substring(9, message.length());
                setHost(newHost);
            }

        }

        //changes the port that the socket attempts to connect to
        //format: #setPort 5555
        if (message.indexOf("#setPort") >= 0) {

            if (isConnected()) {
                clientUI.display("Cannot change port while connected");
            } else {
                //#setPort 5555
                // 5555
                int newPort = Integer.parseInt(message.substring(9, message.length()));
                setPort(newPort);
            }

        }

        //if we are not already logged in connect us to a server
        if (message.indexOf("#login") >= 0) {

            if (isConnected()) {
                clientUI.display("already connected");
            } else {

                try {

                    openConnection();
                } catch (IOException e) {
                    clientUI.display("failed to connect to server.");
                }
                
                String userId = message.substring(7, message.length()).trim();
                Envelope env = new Envelope("login", null, userId);
                
                try {
                    sendToServer(env);
                } catch (IOException e) {
                    clientUI.display("Could not send message to server.  Terminating client.......");
                    quit();
                }
            }
        }
        
        if (message.indexOf("#join") >= 0) {

            if (isConnected()) {
                String room = message.substring(6, message.length()).trim();
                Envelope env = new Envelope("join", null, room);
                
                try {
                    sendToServer(env);
                } catch (IOException e) {
                    clientUI.display("Could not send message to server.  Terminating client.......");
                    quit();
                }
            } else {
                clientUI.display("You must be connected to a server to perform this command");
            }
        }
        
        if (message.indexOf("#pm") >= 0) {

            if (isConnected()) {
                String userAndMessage = message.substring(4, message.length()).trim();
                String user = userAndMessage.substring(0, userAndMessage.indexOf(" ")).trim();
                String msg = userAndMessage.substring(userAndMessage.indexOf(" "), userAndMessage.length()).trim();
                Envelope env = new Envelope("pm", user, msg);
                
                try {
                    sendToServer(env);
                } catch (IOException e) {
                    clientUI.display("Could not send message to server.  Terminating client.......");
                    quit();
                }
            } else {
                clientUI.display("You must be connected to a server to perform this command");
            }
        }
        
        if (message.indexOf("#who") >= 0) {

            if (isConnected()) {
                
                Envelope env = new Envelope("who", null, null);
                
                try {
                    sendToServer(env);
                } catch (IOException e) {
                    clientUI.display("Could not send message to server.  Terminating client.......");
                    quit();
                }
            } else {
                clientUI.display("You must be connected to a server to perform this command");
            }
        }
        
        if (message.indexOf("#ison") >= 0) {

            if (isConnected()) {
                String userId = message.substring(6).trim(); 
 
                Envelope env = new Envelope("ison", null, userId);
                
                try {
                    sendToServer(env);
                } catch (IOException e) {
                    clientUI.display("Could not send message to server.  Terminating client.......");
                    quit();
                }
            } else {
                clientUI.display("You must be connected to a server to perform this command");
            }
        }
        
        if (message.indexOf("#userstatus") >= 0) {

            if (isConnected()) {
                
                Envelope env = new Envelope("userstatus", null, null);
                
                try {
                    sendToServer(env);
                } catch (IOException e) {
                    clientUI.display("Could not send message to server.  Terminating client.......");
                    quit();
                }
            } else {
                clientUI.display("You must be connected to a server to perform this command");
            }
        }
        
        if (message.indexOf("#joinroom") >= 0) {
            if (isConnected()) 
            {
                String rooms = message.substring(10).trim(); 

                String[] roomArray = rooms.split(" "); 

                String room1 = roomArray[0].trim(); 
                String room2 = roomArray[1].trim();

                Envelope env = new Envelope("joinroom", room1,room2);
                
                try {
                    sendToServer(env);
                } catch (IOException e) {
                    clientUI.display("Could not send message to server.  Terminating client.......");
                    quit();
                }
            } else 
                clientUI.display("You must be connected to a server to perform this command");
        }
    }
}
//End of ChatClient class
