package client;

import registry.Register;
import server.Server;
import java.util.Random;

/**
 * ⚠️ NOTE: This class is NOT used in the final implementation.
 * 
 * This was an early command-line client for testing purposes.
 * The actual implementation uses GameUI.java and TwoGameUI.java
 * with graphical user interfaces.
 * 
 * Kept for reference and potential automated testing.
 */
public class Client implements Runnable {
    private char player;
    private GameServiceProxy gameService;
    private Register registry;
    private Server server;
    private String serviceName;

    public Client(char player, Register registry, Server server, String serviceName) {
        this.player = player;
        this.registry = registry;
        this.server = server;
        this.serviceName = serviceName;
    }

    @Override
    public void run() {
        try {
            // Step 1: Service Discovery
            System.out.println("[Client " + player + "] Starting service discovery...");
            
            // Try to lookup service in registry
            ServiceReference ref = registry.lookup(serviceName);
            
            if (ref == null) {
                // Service not in registry, request from server
                System.out.println("[Client " + player + "] Service not in registry. Requesting from server...");
                ref = server.requestService(serviceName);
                
                if (ref != null) {
                    // Cache the service reference in registry
                    registry.rebind(serviceName, ref);
                    System.out.println("[Client " + player + "] Service reference cached in registry.");
                }
            } else {
                System.out.println("[Client " + player + "] Service found in registry.");
            }

            // Step 2: Create proxy
            gameService = new GameServiceProxy(ref);
            System.out.println("[Client " + player + "] Connected to game service via proxy.");

            // Step 3: Play the game
            playGame();

        } catch (Exception e) {
            System.err.println("[Client " + player + "] Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main game loop for the client.
     */
    private void playGame() throws InterruptedException {
        Random random = new Random();
        
        while (true) {
            // Check game status
            String status = (String) gameService.getStatus();
            
            if (!status.equals("IN_PROGRESS")) {
                System.out.println("\n[Client " + player + "] Game Over! " + status);
                displayBoard();
                break;
            }

            // Check if it's this player's turn
            char currentPlayer = (Character) gameService.getCurrentPlayer();
            
            if (currentPlayer == player) {
                // Make a move
                int position = findAvailablePosition();
                
                if (position == -1) {
                    System.out.println("[Client " + player + "] No available positions!");
                    break;
                }

                System.out.println("\n[Client " + player + "] Making move at position " + position);
                String result = (String) gameService.makeMove(player, position);
                System.out.println("[Client " + player + "] Result: " + result);
                
                displayBoard();
                
                // Small delay for readability
                Thread.sleep(500);
            } else {
                // Wait for opponent's turn
                Thread.sleep(300);
            }
        }
    }

    /**
     * Finds an available position on the board.
     * @return position index or -1 if board is full
     */
    private int findAvailablePosition() {
        char[] board = (char[]) gameService.getBoard();
        Random random = new Random();
        
        // Collect available positions
        int[] available = new int[9];
        int count = 0;
        
        for (int i = 0; i < board.length; i++) {
            if (board[i] == '-') {
                available[count++] = i;
            }
        }

        if (count == 0) {
            return -1;
        }

        // Return random available position
        return available[random.nextInt(count)];
    }

    /**
     * Displays the current board state.
     */
    private void displayBoard() {
        char[] board = (char[]) gameService.getBoard();
        
        System.out.println("\n[Client " + player + "] Current Board:");
        System.out.println("  " + board[0] + " | " + board[1] + " | " + board[2]);
        System.out.println(" -----------");
        System.out.println("  " + board[3] + " | " + board[4] + " | " + board[5]);
        System.out.println(" -----------");
        System.out.println("  " + board[6] + " | " + board[7] + " | " + board[8]);
        System.out.println();
    }
}