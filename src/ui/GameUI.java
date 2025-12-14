// src/ui/GameUI.java
package ui;

import client.GameServiceProxy;
import client.ServiceReference;
import registry.Register;
import server.Server;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced Graphical User Interface for the Tic-Tac-Toe game.
 * Features: Game timer, round counter, service renewal, and improved controls.
 */
public class GameUI extends JFrame {
    private JButton[][] buttons;
    private JLabel statusLabel;
    private JLabel turnLabel;
    private JLabel timerLabel;
    private JLabel roundLabel;
    private JLabel statsLabel;
    private JTextArea logArea;
    private JPanel boardPanel;
    private JPanel controlPanel;
    
    private GameServiceProxy gameService;
    private Register registry;
    private Server server;
    private boolean gameActive;
    private Timer refreshTimer;
    private Timer gameTimer;
    
    // Game statistics
    private int currentRound;
    private int playerXWins;
    private int playerOWins;
    private int draws;
    private long gameStartTime;
    private int elapsedSeconds;

    // Colors
    private static final Color PLAYER_X_COLOR = new Color(231, 76, 60);
    private static final Color PLAYER_O_COLOR = new Color(52, 152, 219);
    private static final Color BOARD_COLOR = new Color(44, 62, 80);
    private static final Color BUTTON_COLOR = new Color(236, 240, 241);
    private static final Color HOVER_COLOR = new Color(189, 195, 199);
    private static final Color WIN_COLOR = new Color(46, 204, 113);

    public GameUI() {
        initializeStats();
        initializeComponents();
        setupGame();
    }

    /**
     * Initializes game statistics.
     */
    private void initializeStats() {
        currentRound = 1;
        playerXWins = 0;
        playerOWins = 0;
        draws = 0;
        elapsedSeconds = 0;
    }

    /**
     * Initializes all UI components.
     */
    private void initializeComponents() {
        setTitle("Custom RMI Tic-Tac-Toe - Enhanced Edition");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(550, 700);
        setLocationRelativeTo(null);
        setResizable(false);

        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(236, 240, 241));

        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(BOARD_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("TIC-TAC-TOE", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 32));
        titleLabel.setForeground(Color.WHITE);
        
        JLabel subtitleLabel = new JLabel("Custom RMI Architecture", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitleLabel.setForeground(new Color(189, 195, 199));
        
        JPanel titleContent = new JPanel(new BorderLayout());
        titleContent.setBackground(BOARD_COLOR);
        titleContent.add(titleLabel, BorderLayout.CENTER);
        titleContent.add(subtitleLabel, BorderLayout.SOUTH);
        
        titlePanel.add(titleContent, BorderLayout.CENTER);
        add(titlePanel, BorderLayout.NORTH);

        // Stats Panel
        JPanel statsPanel = new JPanel(new GridLayout(1, 4, 10, 0));
        statsPanel.setBackground(new Color(236, 240, 241));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        roundLabel = createStatLabel("Round: 1", BOARD_COLOR);
        timerLabel = createStatLabel("Time: 00:00", new Color(230, 126, 34));
        statsLabel = createStatLabel("X:0 O:0 D:0", new Color(142, 68, 173));
        turnLabel = createStatLabel("Turn: X", PLAYER_X_COLOR);

        statsPanel.add(roundLabel);
        statsPanel.add(timerLabel);
        statsPanel.add(statsLabel);
        statsPanel.add(turnLabel);

        // Combined North Panel (Title + Stats)
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(236, 240, 241));
        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(statsPanel, BorderLayout.SOUTH);
        
        // Replace previous titlePanel add with combined panel
        getContentPane().removeAll();
        add(northPanel, BorderLayout.NORTH);

        // Board Panel
        boardPanel = new JPanel();
        boardPanel.setLayout(new GridLayout(3, 3, 10, 10));
        boardPanel.setBackground(BOARD_COLOR);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        buttons = new JButton[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int position = i * 3 + j;
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 60));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setBackground(BUTTON_COLOR);
                buttons[i][j].setBorder(BorderFactory.createLineBorder(BOARD_COLOR, 2));
                buttons[i][j].setCursor(new Cursor(Cursor.HAND_CURSOR));
                
                buttons[i][j].addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent evt) {
                        JButton btn = (JButton) evt.getSource();
                        if (btn.getText().isEmpty() && gameActive) {
                            btn.setBackground(HOVER_COLOR);
                        }
                    }
                    public void mouseExited(java.awt.event.MouseEvent evt) {
                        JButton btn = (JButton) evt.getSource();
                        if (btn.getText().isEmpty()) {
                            btn.setBackground(BUTTON_COLOR);
                        }
                    }
                });

                buttons[i][j].addActionListener(e -> handleMove(position));
                boardPanel.add(buttons[i][j]);
            }
        }

        add(boardPanel, BorderLayout.CENTER);

        // Info Panel
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BorderLayout(5, 5));
        infoPanel.setBackground(new Color(236, 240, 241));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));

        // Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(1, 1, 5, 5));
        statusPanel.setBackground(new Color(236, 240, 241));

        statusLabel = new JLabel("Initializing game...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusPanel.add(statusLabel);

        infoPanel.add(statusPanel, BorderLayout.NORTH);

        // Log Area
        logArea = new JTextArea(4, 40);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        logArea.setBackground(new Color(250, 250, 250));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Game Log"));
        infoPanel.add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        controlPanel.setBackground(new Color(236, 240, 241));

        JButton newGameButton = createControlButton("New Round", new Color(46, 204, 113));
        newGameButton.addActionListener(e -> newRound());

        JButton newMatchButton = createControlButton("New Match", new Color(52, 152, 219));
        newMatchButton.addActionListener(e -> resetMatch());

        JButton renewServiceButton = createControlButton("Renew Service", new Color(230, 126, 34));
        renewServiceButton.addActionListener(e -> renewService());

        JButton pauseButton = createControlButton("Pause", new Color(142, 68, 173));
        pauseButton.addActionListener(e -> togglePause());

        JButton exitButton = createControlButton("Exit", new Color(231, 76, 60));
        exitButton.addActionListener(e -> System.exit(0));

        controlPanel.add(newGameButton);
        controlPanel.add(newMatchButton);
        controlPanel.add(renewServiceButton);
        controlPanel.add(pauseButton);
        controlPanel.add(exitButton);

        infoPanel.add(controlPanel, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.SOUTH);
    }

    /**
     * Creates a stat label with consistent styling.
     */
    private JLabel createStatLabel(String text, Color bgColor) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        label.setBackground(bgColor);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return label;
    }

    /**
     * Creates a control button with consistent styling.
     */
    private JButton createControlButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return button;
    }

    /**
     * Sets up the game service and connections.
     */
    private void setupGame() {
        try {
            log("Initializing game service...");
            
            // only create new instance if not already created
            if (registry == null) {
                registry = new Register();
            }
            if (server == null) {
                server = new Server();
            }
            String serviceName = "TicTacToeGame";

            log("Performing service discovery...");
            ServiceReference ref = registry.lookup(serviceName);
            
            if (ref == null) {
                log("Service not in registry. Requesting from server...");
                ref = server.requestService(serviceName);
                registry.rebind(serviceName, ref);
                log("✓ Service cached in registry.");
            } else {
                log("✓ Service found in registry.");
            }

            gameService = new GameServiceProxy(ref);
            log("✓ Connected to game service via proxy.");
            
            gameService.resetGame();
            clearBoard();

            gameActive = true;
            gameStartTime = System.currentTimeMillis();
            
            statusLabel.setText("Game Ready - Player X starts!");
            log("=== Round " + currentRound + " started ===");

            // Start timers
            startTimers();

        } catch (Exception e) {
            log("✗ Error initializing game: " + e.getMessage());
            statusLabel.setText("Failed to initialize game!");
        }
    }

    /**
     * Starts the game and refresh timers.
     */
    private void startTimers() {
        // Refresh timer for board updates
        refreshTimer = new Timer(500, e -> updateBoardFromService());
        refreshTimer.start();

        // Game timer for elapsed time
        gameTimer = new Timer(1000, e -> {
            elapsedSeconds++;
            updateTimerDisplay();
        });
        gameTimer.start();
    }

    /**
     * Updates the timer display.
     */
    private void updateTimerDisplay() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    /**
     * Handles a player's move.
     */
    private void handleMove(int position) {
        if (!gameActive) {
            log("⚠ Game is over. Start a new round.");
            return;
        }

        try {
            char serviceCurrentPlayer = (Character) gameService.getCurrentPlayer();
            
            log("Player " + serviceCurrentPlayer + " attempting move at position " + position);
            String result = (String) gameService.makeMove(serviceCurrentPlayer, position);
            log("→ " + result);

            updateBoardFromService();

            String status = (String) gameService.getStatus();
            if (!status.equals("IN_PROGRESS")) {
                gameActive = false;
                refreshTimer.stop();
                gameTimer.stop();
                
                handleGameEnd(status);
            }

        } catch (Exception e) {
            log("✗ Error making move: " + e.getMessage());
        }
    }

    /**
     * Handles game end scenario.
     */
    private void handleGameEnd(String status) {
        statusLabel.setText(status);
        statusLabel.setForeground(WIN_COLOR);
        log("=== GAME OVER: " + status + " ===");
        
        // Update statistics
        if (status.contains("Player X wins")) {
            playerXWins++;
        } else if (status.contains("Player O wins")) {
            playerOWins++;
        } else if (status.contains("Draw")) {
            draws++;
        }
        
        updateStatsDisplay();
        
        JOptionPane.showMessageDialog(this, 
            status + "\n\nTime: " + formatTime(elapsedSeconds) + 
            "\nRound: " + currentRound, 
            "Game Over", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Updates the board display from the service.
     */
    private void updateBoardFromService() {
        try {
            char[] board = (char[]) gameService.getBoard();
            
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int position = i * 3 + j;
                    char cell = board[position];
                    
                    if (cell != '-' && buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText(String.valueOf(cell));
                        buttons[i][j].setForeground(
                            cell == 'X' ? PLAYER_X_COLOR : PLAYER_O_COLOR
                        );
                        buttons[i][j].setEnabled(false);
                        buttons[i][j].setBackground(new Color(220, 220, 220));
                    }
                }
            }

            if (gameActive) {
                char currentTurn = (Character) gameService.getCurrentPlayer();
                turnLabel.setText("Turn: " + currentTurn);
                turnLabel.setBackground(
                    currentTurn == 'X' ? PLAYER_X_COLOR : PLAYER_O_COLOR
                );
            }

        } catch (Exception e) {
            // Silently fail for refresh operations
        }
    }

    /**
     * Starts a new round (keeps statistics).
     */
    private void newRound() {
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        clearBoard();
        
        currentRound++;
        elapsedSeconds = 0;
        roundLabel.setText("Round: " + currentRound);
        
        statusLabel.setText("Starting new round...\n");
        statusLabel.setForeground(Color.BLACK);
        
        log("=".repeat(40));
        log("Starting Round " + currentRound);
        log("=".repeat(40));
        
        // Display cache before starting new round
        System.out.println("\n>>> BEFORE NEW ROUND:");
        registry.displayCache();
        
        // Reset the game service (creates new game instance)
        setupGame();
        
        System.out.println("\n>>> AFTER NEW ROUND:");
        registry.displayCache();
    }

    /**
     * Resets the entire match (clears all statistics).
     */
    private void resetMatch() {
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        clearBoard();
        
        // Reset all statistics
        currentRound = 1;
        playerXWins = 0;
        playerOWins = 0;
        draws = 0;
        elapsedSeconds = 0;
        
        roundLabel.setText("Round: 1");
        updateStatsDisplay();
        
        logArea.setText("");
        
        statusLabel.setText("New match started!");
        statusLabel.setForeground(Color.BLACK);
        
        log("=".repeat(40));
        log("NEW MATCH STARTED");
        log("=".repeat(40));
        
        // Display cache before reset
        System.out.println("\n>>> BEFORE RESET MATCH:");
        registry.displayCache();
        
        // Reset the game service (creates new game instance)
        setupGame();
        
        System.out.println("\n>>> AFTER RESET MATCH:");
        registry.displayCache();
    }

    /**
     * Renews the service connection.
     */
    private void renewService() {
        log("\n--- Renewing Service Connection ---");
        
        try {
            String serviceName = "TicTacToeGame";

            System.out.println("\n>>>> Before RENEWAL: ");
            registry.displayCache();

            // Clear registry cache
            log("Clearing registry for cached service...");
            ServiceReference ref = registry.lookup(serviceName);

            if (ref == null) {
                log("Service not found in registry. Requesting from server...");
                ref = server.requestService(serviceName);
                registry.rebind(serviceName, ref);
                log("✓ Service cached in registry.");
            } else {
                log("✓ Service found in registry cache!");
                log("reusing cached service reference.");
            }
            
            System.out.println("\n>>>> After LOOKUP: ");
            registry.displayCache();

            // Create new proxy
            gameService = new GameServiceProxy(ref);
            
            log("✓ Service successfully renewed!");
            log("✓ New proxy connection established.");
            statusLabel.setText("Service renewed successfully!");
            
            JOptionPane.showMessageDialog(this, 
            "Service connection renewed!\n" +
            (ref != null ? "Using cached service." : "Fetched from server."), 
            "Service Renewed", 
            JOptionPane.INFORMATION_MESSAGE);
            
        } catch (Exception e) {
            log("✗ Error renewing service: " + e.getMessage());
            JOptionPane.showMessageDialog(this, 
                "Failed to renew service: " + e.getMessage(), 
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Toggles pause/resume of the game timer.
     */
    private void togglePause() {
        if (gameTimer != null) {
            if (gameTimer.isRunning()) {
                gameTimer.stop();
                refreshTimer.stop();
                statusLabel.setText("Game Paused");
                log("⏸ Game paused");
            } else {
                gameTimer.start();
                refreshTimer.start();
                statusLabel.setText("Game Resumed");
                log("▶ Game resumed");
            }
        }
    }

    /**
     * Clears the board display.
     */
    private void clearBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].setBackground(new Color(236, 240, 241));
                buttons[i][j].setForeground(Color.BLACK);
            }
        }
        gameActive = false;
    }

    /**
     * Updates the statistics display.
     */
    private void updateStatsDisplay() {
        statsLabel.setText(String.format("X:%d O:%d D:%d", playerXWins, playerOWins, draws));
    }

    /**
     * Formats time in MM:SS format.
     */
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    /**
     * Logs a message to the log area.
     */
    private void log(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Main method to launch the UI.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            GameUI ui = new GameUI();
            ui.setVisible(true);
        });
    }
}