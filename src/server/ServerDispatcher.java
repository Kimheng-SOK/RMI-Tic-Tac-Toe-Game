// src/server/ServerDispatcher.java
package server;

import java.util.HashMap;
import java.util.Map;

/**
 * Dispatches client requests to appropriate service methods.
 * Acts as the RMI skeleton, routing method calls.
 */
public class ServerDispatcher {
    private Map<String, Object> services;

    public ServerDispatcher() {
        this.services = new HashMap<>();
    }

    /**
     * Registers a service with the dispatcher.
     * @param serviceName the name of the service
     * @param serviceObject the service implementation
     */
    public void registerService(String serviceName, Object serviceObject) {
        services.put(serviceName, serviceObject);
        System.out.println("[Dispatcher] Service '" + serviceName + "' registered.");
    }

    /**
     * Handles a remote method call request.
     * @param serviceName the service to call
     * @param methodName the method to invoke
     * @param params method parameters
     * @return the result of the method call
     */
    public Object handleRequest(String serviceName, String methodName, Object... params) {
        Object service = services.get(serviceName);
        
        if (service == null) {
            return "Error: Service '" + serviceName + "' not found.";
        }

        // For TicTacToeService
        if (service instanceof TicTacToeService) {
            TicTacToeService gameService = (TicTacToeService) service;
            
            switch (methodName) {
                case "makeMove":
                    if (params.length >= 2) {
                        char player = (Character) params[0];
                        int position = (Integer) params[1];
                        return gameService.makeMove(player, position);
                    }
                    return "Error: Invalid parameters for makeMove.";
                    
                case "getBoard":
                    return gameService.getBoard();
                    
                case "getStatus":
                    return gameService.getStatus();
                    
                case "getCurrentPlayer":
                    return gameService.getCurrentPlayer();
                    
                case "resetGame":
                    gameService.resetGame();
                    return "Game reset successfully";
                    
                default:
                    return "Error: Unknown method '" + methodName + "'.";
            }
        }

        return "Error: Unknown service type.";
    }
}