package JavaServer;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;


public class EchoServer extends AbstractServer {
    //Class variables *************************************************

    /**
     * The default port to listen on.
     */
    final public static int DEFAULT_PORT = 5555;
    private static final String FILE_STORAGE_DIR = "uploads";
    //Constructors ****************************************************
    /**
     * Constructs an instance of the echo server.
     *
     * @param port The port number to connect on.
     */
    public EchoServer(int port) {
        super(port);
        
        File storageDir = new File(FILE_STORAGE_DIR);
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        
        try {
            this.listen(); //Start listening for connections
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients!");
        }

    }

    //Instance methods ************************************************
    /**
     * This method handles any messages received from the client.
     *
     * @param msg The message received from the client.
     * @param client The connection from which the message originated.
     */
    public void handleMessageFromClient(Object msg, ConnectionToClient client) {
        if(msg instanceof Envelope)
        {
            handleClientCommand((Envelope)msg, client);
        }
        else
        {
            System.out.println("Message received: " + msg + " from " + client);
            String messageWithId = client.getInfo("userId") + ": " + msg;
            sendToAllClientsInRoom(messageWithId, client.getInfo("room").toString());
        }
    }
    
    public void handleClientCommand(Envelope env, ConnectionToClient client)
    {
        if (env.getName().equals("ftplist")) { // Send list of uploaded files
            File folder = new File("uploads");
            String[] fileList = folder.list();

            if (fileList == null) {
                fileList = new String[]{}; // Empty array if folder does not exist
            }

            try {
                client.sendToClient(new Envelope("ftplist", null, fileList));
            } catch (IOException e) {
                System.out.println("Error sending file list to client.");
            }
        }

        if (env.getName().equals("ftpget")) { // Send requested file to client
            String fileName = env.getArg();
            File fileToSend = new File("uploads", fileName);

            if (!fileToSend.exists()) {
                try {
                    client.sendToClient(new Envelope("ftpget", fileName, "Error: File not found."));
                } catch (IOException e) {
                    System.out.println("Error sending file not found message.");
                }
                return;
            }

            try {
                byte[] fileData = Files.readAllBytes(fileToSend.toPath());
                client.sendToClient(new Envelope("ftpget", fileName, fileData));
                System.out.println("File sent: " + fileName);
            } catch (IOException e) {
                System.out.println("Error reading or sending file: " + e.getMessage());
            }
        }
        
        if (env.getName().equals("ftpUpload")) {
            String fileName = env.getArg();
            byte[] fileData = (byte[]) env.getMsg();

            try {
                File receivedFile = new File("uploads", fileName);
                receivedFile.getParentFile().mkdirs(); // Ensure directory exists
                FileOutputStream fileOutputStream = new FileOutputStream(receivedFile);
                fileOutputStream.write(fileData);
                fileOutputStream.close();

                System.out.println("File received: " + fileName);

                client.sendToClient("File " + fileName + " saved successfully.");
            } catch (IOException e) {
                System.out.println("Error saving file: " + e.getMessage());
                try {
                    client.sendToClient("Error saving file.");
                } catch (IOException ex) {
                    System.out.println("Error sending failure message to client.");
                }
            }
        }
        
        if(env.getName().equals("login"))
        {
            client.setInfo("userId", env.getMsg());
            client.setInfo("room", "commons");
            
            String response = client.getInfo("userId")+" has joined and has been placed in room " + client.getInfo("room");
            System.out.println(response);
            
            try {
                client.sendToClient("you have joined with user id " + client.getInfo("userId") + " and been placed in room " + client.getInfo("room"));
            } catch (Exception e) {
                System.out.println("Error sending room change confirmation to client.");
            }
        }
        
        if(env.getName().equals("join"))
        {
            client.setInfo("room", env.getMsg());
            String response = client.getInfo("userId")+" has moved to room " + client.getInfo("room");
            System.out.println(response);
            
            try {
                client.sendToClient("You have moved to room " + client.getInfo("room"));
            } catch (Exception e) {
                System.out.println("Error sending room change confirmation to client.");
            }
        }
        
        if(env.getName().equals("pm"))
        {
            String target = env.getArg();
            sendToClientByUserId(env.getMsg(), target);

            try {
                client.sendToClient("You (PM to " + target + "): " + env.getMsg());
            } catch (Exception e) {
                System.out.println("Error sending PM confirmation to client.");
            }
        }
                
            if(env.getName().equals("who"))
        {
            Envelope returnEnv = new Envelope("who", null, null);
            ArrayList clientsInRoom = getAllClientsInRoom(client.getInfo("room").toString());
            returnEnv.setMsg(clientsInRoom);
            
            try {
                client.sendToClient(returnEnv);
            } catch (Exception ex) {
            }
        }

	if (env.getName().equals("ison")) {
        String location = (locateClientRoom(env.getMsg().toString()));

        try {
                client.sendToClient(location);
            } catch (Exception e) {
                System.out.println("Error sending ison response.");
            }
        }

	if (env.getName().equals("userstatus")) {
            Envelope returnEnv = new Envelope("userstatus", null, null);
            returnEnv.setMsg(printAllUsers());

            try {
                client.sendToClient("---- All Users ---\n" + printAllUsers());
            } catch (Exception ex) {}
        }

        if (env.getName().equals("joinroom")) {
            String room1 = env.getArg(); 
            String room2 = env.getMsg().toString();
            moveClientsToRoom(room1, room2);
        }
    }
    
    public void sendToAllClientsInRoom(Object msg, String room) {
        Thread[] clientThreadList = getClientConnections();

        for (int i = 0; i < clientThreadList.length; i++) {
            ConnectionToClient currentClient = (ConnectionToClient) clientThreadList[i];
            if(room.equals(currentClient.getInfo("room")))
            {
                try {
                    currentClient.sendToClient(msg);
                } catch (Exception ex) {
                }
            }
        }
    }
    
    public void sendToClientByUserId(Object msg, String userId) {
        Thread[] clientThreadList = getClientConnections();

        for (int i = 0; i < clientThreadList.length; i++) {
            ConnectionToClient currentClient = (ConnectionToClient) clientThreadList[i];
            if(userId.equals(currentClient.getInfo("userId")))
            {
                try {
                    currentClient.sendToClient(msg);
                } catch (Exception ex) {
                }
            }
        }
    }
    
    public ArrayList<String> getAllClientsInRoom(String room) {
        Thread[] clientThreadList = getClientConnections();
        ArrayList<String> results = new ArrayList<String>();
        for (int i = 0; i < clientThreadList.length; i++) {
            ConnectionToClient currentClient = (ConnectionToClient) clientThreadList[i];
            if(room.equals(currentClient.getInfo("room")))
            {
                results.add(currentClient.getInfo("userId").toString());
            }
        }
        return results;
    }

    /**
     * This method overrides the one in the superclass. Called when the server
     * starts listening for connections.
     */
    protected void serverStarted() {
        System.out.println("Server listening for connections on port " + getPort());
    }

    /**
     * This method overrides the one in the superclass. Called when the server
     * stops listening for connections.
     */
    protected void serverStopped() {
        System.out.println("Server has stopped listening for connections.");
    }

    //Class methods ***************************************************
    /**
     * This method is responsible for the creation of the server instance (there
     * is no UI in this phase).
     *
     * @param args[0] The port number to listen on. Defaults to 5555 if no
     * argument is entered.
     */
    public static void main(String[] args) {
        int port = 0; //Port to listen on
        
        try{
            port = Integer.parseInt(args[0]);
        }
        catch(ArrayIndexOutOfBoundsException aioobe)
        {
            port = DEFAULT_PORT; //Set port to 5555
        }

        EchoServer sv = new EchoServer(port);

        try {
            sv.listen(); //Start listening for connections
        } catch (Exception ex) {
            System.out.println("ERROR - Could not listen for clients!");
        }

    }

    protected void clientConnected(ConnectionToClient client) {

        System.out.println("<Client Connected:" + client + ">");
    }

    
    protected void clientDisconnected(ConnectionToClient client) {

        System.out.println("<Client disconnected>");

    }

    private String locateClientRoom(String targetUserId) 
    {
        Thread[] clientConnections = getClientConnections();            
       
        for (int i =0; i < clientConnections.length; i++)
        {
            ConnectionToClient currentConnection = (ConnectionToClient) clientConnections[i];
            if (targetUserId.equals(currentConnection.getInfo("userId")))
            {
                String room = currentConnection.getInfo("room").toString();
                return targetUserId + " is in room " + room; 
            }
        }
        return targetUserId + " is not logged on";
    }

    private Object printAllUsers() {
        Thread[] clientConnections = getClientConnections();
        String statusList = "";

        for (int i = 0; i < clientConnections.length; i++) {
            ConnectionToClient currentClient = (ConnectionToClient) clientConnections[i];
            statusList += currentClient.getInfo("userId") + " – " + currentClient.getInfo("room") + "\n";
        }

        return statusList;        
    }

    private void moveClientsToRoom(String room1, String room2) {
        Thread[] clientConnections = getClientConnections();

        for (int i = 0; i < clientConnections.length; i++) {
            ConnectionToClient currentClient = (ConnectionToClient) clientConnections[i];
            if (room1.equals(currentClient.getInfo("room"))) {
                currentClient.setInfo("room", room2); // Move client to room2
            }
        }
    }
}