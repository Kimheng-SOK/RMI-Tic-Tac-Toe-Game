package client;

import java.io.*;
import java.net.*;

/**
 * Socket-based proxy that connects to remote server
 * Implements the same interface as GameServiceProxy but uses sockets
 */
public class SocketGameProxy {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String serverHost;
    private int serverPort;
    private boolean connected;
    
    public SocketGameProxy(String host, int port) throws IOException {
        this.serverHost = host;
        this.serverPort = port;
        connect();
    }
    
    private void connect() throws IOException {
        try {
            socket = new Socket(serverHost, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            System.out.println("[Proxy] ✓ Connected to server at " + serverHost + ":" + serverPort);
        } catch (IOException e) {
            connected = false;
            throw new IOException("Failed to connect to server: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return connected && socket != null && !socket.isClosed();
    }
    
    public void disconnect() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            connected = false;
            System.out.println("[Proxy] Disconnected from server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Invokes a remote method on the server
     */
    private Object invokeRemote(String methodName, Object... args) throws Exception {
        if (!isConnected()) {
            throw new Exception("Not connected to server");
        }
        
        try {
            // Send request
            out.writeObject(methodName);
            out.writeObject(args);
            out.flush();
            
            // Receive response
            Object result = in.readObject();
            return result;
            
        } catch (IOException | ClassNotFoundException e) {
            connected = false;
            throw new Exception("Communication error: " + e.getMessage());
        }
    }
    
    // Game service methods
    
    public Object makeMove(char player, int position) {
        try {
            return invokeRemote("makeMove", player, position);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    public Object getBoard() {
        try {
            return invokeRemote("getBoard");
        } catch (Exception e) {
            return new char[9];
        }
    }
    
    public Object getCurrentPlayer() {
        try {
            return invokeRemote("getCurrentPlayer");
        } catch (Exception e) {
            return 'X';
        }
    }
    
    public Object getStatus() {
        try {
            return invokeRemote("getStatus");
        } catch (Exception e) {
            return "ERROR";
        }
    }
    
    public Object resetGame() {
        try {
            return invokeRemote("resetGame");
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
    
    public Object getWinner() {
        try {
            return invokeRemote("getWinner");
        } catch (Exception e) {
            return '-';
        }
    }
}
