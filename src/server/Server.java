// src/server/Server.java
package server;

import client.ServiceReference;

/**
 * Server that hosts the Tic-Tac-Toe game service.
 * Provides service references to clients on request.
 */
public class Server {
    private ServerDispatcher dispatcher;
    private TicTacToeService gameService;

    public Server() {
        this.dispatcher = new ServerDispatcher();
        this.gameService = new TicTacToeService();
        
        // Register the game service with the dispatcher
        dispatcher.registerService("TicTacToeGame", gameService);
        
        System.out.println("[Server] Tic-Tac-Toe server initialized.");
    }

    /**
     * Provides a service reference to clients.
     * @param serviceName the name of the requested service
     * @return ServiceReference if service exists, null otherwise
     */
    public ServiceReference requestService(String serviceName) {
        System.out.println("[Server] Service request received for: " + serviceName);
        
        // Create and return a service reference
        ServiceReference ref = new ServiceReference(serviceName, dispatcher);
        return ref;
    }

    public ServerDispatcher getDispatcher() {
        return dispatcher;
    }

    public TicTacToeService getGameService() {
        return gameService;
    }
}