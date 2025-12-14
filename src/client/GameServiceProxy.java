package client;

import server.ServerDispatcher;

/**
 * Client-side proxy for the Tic-Tac-Toe game service.
 * Acts as the RMI stub, providing transparent remote method calls.
 */
public class GameServiceProxy {
    private ServiceReference serviceRef;

    public GameServiceProxy(ServiceReference serviceRef) {
        this.serviceRef = serviceRef;
    }

    /**
     * Makes a move in the game.
     * @param player the player making the move
     * @param position the board position (0-8)
     * @return result message
     */
    public Object makeMove(char player, int position) {
        ServerDispatcher dispatcher = serviceRef.getDispatcher();
        return dispatcher.handleRequest(
            serviceRef.getServiceName(),
            "makeMove",
            player,
            position
        );
    }

    /**
     * Gets the current board state.
     * @return board array
     */
    public Object getBoard() {
        ServerDispatcher dispatcher = serviceRef.getDispatcher();
        return dispatcher.handleRequest(
            serviceRef.getServiceName(),
            "getBoard"
        );
    }

    /**
     * Gets the current game status.
     * @return status string
     */
    public Object getStatus() {
        ServerDispatcher dispatcher = serviceRef.getDispatcher();
        return dispatcher.handleRequest(
            serviceRef.getServiceName(),
            "getStatus"
        );
    }

    /**
     * Gets the current player.
     * @return current player character
     */
    public Object getCurrentPlayer() {
        ServerDispatcher dispatcher = serviceRef.getDispatcher();
        return dispatcher.handleRequest(
            serviceRef.getServiceName(),
            "getCurrentPlayer"
        );
    }

    /**
     * Resets the game to initial state.
     * @return result message
     */
    public Object resetGame() {
        ServerDispatcher dispatcher = serviceRef.getDispatcher();
        return dispatcher.handleRequest(
            serviceRef.getServiceName(),
            "resetGame"
        );
    }
}