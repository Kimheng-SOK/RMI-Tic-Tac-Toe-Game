package server;

import registry.Register;
import client.ServiceReference;

import java.io.*;
import java.net.*;

/**
 * Socket-based RMI server that listens for client connections
 * Provides true client/server separation using TCP sockets
 */
public class SocketServer {
    private static final int PORT = 5000;
    private static Register registry;
    private static Server server;
    private static ServerSocket serverSocket;
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║   Socket-Based RMI Server Starting    ║");
        System.out.println("╚════════════════════════════════════════╝");
        
        try {
            // Initialize server components
            registry = new Register();
            server = new Server();
            
            // Pre-register TicTacToe service
            String serviceName = "TicTacToeGame";
            ServiceReference ref = server.requestService(serviceName);
            registry.rebind(serviceName, ref);
            
            System.out.println("[Server] ✓ Registry initialized");
            System.out.println("[Server] ✓ Server initialized");
            System.out.println("[Server] ✓ Service '" + serviceName + "' registered");
            
            // Start socket server
            serverSocket = new ServerSocket(PORT);
            
            // Get and display server IP address
            String serverIP = getLocalIPAddress();
            
            System.out.println("\n╔════════════════════════════════════════╗");
            System.out.println("║   SERVER READY on port " + PORT + "           ║");
            System.out.println("║   Server IP: " + serverIP + "            ║");
            System.out.println("║   Waiting for client connections...   ║");
            System.out.println("║                                        ║");
            System.out.println("║   Clients should connect to:          ║");
            System.out.println("║   java -cp bin ui.ClientGameUI " + serverIP + " ║");
            System.out.println("║                                        ║");
            System.out.println("║   Press Ctrl+C to shutdown            ║");
            System.out.println("╚════════════════════════════════════════╝\n");
            
            // Accept client connections
            int clientCount = 0;
            while (true) {
                Socket clientSocket = serverSocket.accept();
                clientCount++;
                System.out.println("[Server] ✓ Client #" + clientCount + " connected from " + 
                    clientSocket.getInetAddress().getHostAddress());
                
                // Handle each client in a separate thread
                new ClientHandler(clientSocket, clientCount, registry, server).start();
            }
            
        } catch (IOException e) {
            System.err.println("[Server] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }
    
    private static void cleanup() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
                System.out.println("\n[Server] Server socket closed");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Gets the local IP address of the server
     * @return IP address as String
     */
    private static String getLocalIPAddress() {
        try {
            // Try to get the actual network IP (not localhost)
            InetAddress localHost = InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();
            
            // If we got localhost, try to find a real network interface
            if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
                java.util.Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface iface = interfaces.nextElement();
                    if (iface.isLoopback() || !iface.isUp()) continue;
                    
                    java.util.Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        // Get IPv4 address (skip IPv6)
                        if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
            return ip;
        } catch (Exception e) {
            return "localhost";
        }
    }
    
    /**
     * Handles individual client connections
     */
    static class ClientHandler extends Thread {
        private Socket socket;
        private int clientId;
        private Register registry;
        private Server server;
        private ObjectOutputStream out;
        private ObjectInputStream in;
        
        public ClientHandler(Socket socket, int clientId, Register registry, Server server) {
            this.socket = socket;
            this.clientId = clientId;
            this.registry = registry;
            this.server = server;
        }
        
        @Override
        public void run() {
            try {
                // Setup streams (order matters: out before in!)
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(socket.getInputStream());
                
                System.out.println("[Client #" + clientId + "] Ready to receive requests");
                
                // Handle requests
                while (!socket.isClosed()) {
                    try {
                        // Read request
                        String methodName = (String) in.readObject();
                        Object[] args = (Object[]) in.readObject();
                        
                        System.out.println("[Client #" + clientId + "] Request: " + methodName);
                        
                        // Process request
                        Object result = processRequest(methodName, args);
                        
                        // Send response
                        out.writeObject(result);
                        out.flush();
                        
                    } catch (EOFException e) {
                        // Client disconnected
                        break;
                    }
                }
                
            } catch (Exception e) {
                System.err.println("[Client #" + clientId + "] Error: " + e.getMessage());
            } finally {
                cleanup();
            }
        }
        
        private Object processRequest(String methodName, Object[] args) {
            try {
                String serviceName = "TicTacToeGame";
                ServiceReference ref = registry.lookup(serviceName);
                
                if (ref == null) {
                    ref = server.requestService(serviceName);
                    registry.rebind(serviceName, ref);
                }
                
                ServerDispatcher dispatcher = ref.getDispatcher();
                return dispatcher.handleRequest(serviceName, methodName, args);
                
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        
        private void cleanup() {
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                if (socket != null) socket.close();
                System.out.println("[Client #" + clientId + "] Disconnected");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
