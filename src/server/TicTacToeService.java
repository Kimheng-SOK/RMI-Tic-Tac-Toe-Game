// src/server/TicTacToeService.java
package server;

/**
 * Core game logic for Tic-Tac-Toe.
 * Thread-safe implementation with synchronized methods.
 */
public class TicTacToeService {
    private char[] board;
    private char currentPlayer;
    private String status;

    public TicTacToeService() {
        this.board = new char[9];
        for (int i = 0; i < 9; i++) {
            board[i] = '-';
        }
        this.currentPlayer = 'X';
        this.status = "IN_PROGRESS";
    }

    /**
     * Makes a move on the board.
     * @param player the player making the move
     * @param position the position (0-8)
     * @return result message
     */
    public synchronized String makeMove(char player, int position) {
        // Check if game is over
        if (!status.equals("IN_PROGRESS")) {
            return "Game is already over. Status: " + status;
        }

        // Validate turn
        if (player != currentPlayer) {
            return "Not your turn! Current player: " + currentPlayer;
        }

        // Validate position
        if (position < 0 || position > 8) {
            return "Invalid position! Must be between 0 and 8.";
        }

        // Check if position is occupied
        if (board[position] != '-') {
            return "Position already occupied! Choose another.";
        }

        // Make the move
        board[position] = player;
        System.out.println("[Game] Player " + player + " moved to position " + position);

        // Check for win
        if (checkWin(player)) {
            status = "Player " + player + " wins!";
            System.out.println("[Game] " + status);
            return status;
        }

        // Check for draw
        if (checkDraw()) {
            status = "Draw!";
            System.out.println("[Game] " + status);
            return status;
        }

        // Switch player
        currentPlayer = (currentPlayer == 'X') ? 'O' : 'X';
        return "Move accepted. Next player: " + currentPlayer;
    }

    /**
     * Gets the current board state.
     * @return copy of the board array
     */
    public synchronized char[] getBoard() {
        return board.clone();
    }

    /**
     * Gets the current game status.
     * @return status string
     */
    public synchronized String getStatus() {
        return status;
    }

    /**
     * Gets the current player.
     * @return current player character
     */
    public synchronized char getCurrentPlayer() {
        return currentPlayer;
    }

    /**
     * Resets the game to initial state.
     * @return confirmation message
     */    
    public synchronized String resetGame() {
        for (int i = 0; i < 9; i++) {
            board[i] = '-';
        }
        currentPlayer = 'X';
        status = "IN_PROGRESS";
        System.out.println("[Game] Game has been reset.");
        return "Game reset successfully.";
    }
    /**
     * Checks if a player has won.
     * @param player the player to check
     * @return true if player won, false otherwise
     */
    private boolean checkWin(char player) {
        // Check rows
        for (int i = 0; i < 9; i += 3) {
            if (board[i] == player && board[i + 1] == player && board[i + 2] == player) {
                return true;
            }
        }

        // Check columns
        for (int i = 0; i < 3; i++) {
            if (board[i] == player && board[i + 3] == player && board[i + 6] == player) {
                return true;
            }
        }

        // Check diagonals
        if (board[0] == player && board[4] == player && board[8] == player) {
            return true;
        }
        if (board[2] == player && board[4] == player && board[6] == player) {
            return true;
        }

        return false;
    }

    /**
     * Checks if the game is a draw.
     * @return true if board is full, false otherwise
     */
    private boolean checkDraw() {
        for (char cell : board) {
            if (cell == '-') {
                return false;
            }
        }
        return true;
    }
}