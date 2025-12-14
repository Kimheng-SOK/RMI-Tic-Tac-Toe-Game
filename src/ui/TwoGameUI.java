// src/ui/TwoGameUI.java
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
 * Two-player UI with separate windows for each player.
 * Features: Timer, round counter, service renewal, and match statistics.
 */
public class TwoGameUI extends JFrame implements Runnable {
    private char player;
    private GameServiceProxy gameService;
    private Register registry;
    private Server server;
    private String serviceName;
    
    private JButton[][] buttons;
    private JLabel statusLabel;
    private JLabel playerLabel;
    private JLabel timerLabel;
    private JLabel roundLabel;
    private JLabel statsLabel;
    private JTextArea logArea;
    private Timer refreshTimer;
    private Timer gameTimer;
    private boolean gameActive;
    
    // Statistics
    private static int currentRound = 1;
    private static int playerXWins = 0;
    private static int playerOWins = 0;
    private static int draws = 0;
    private int elapsedSeconds = 0;
    
    // Shared control flags
    private static volatile boolean needsRoundReset = false;
    private static volatile boolean needsMatchReset = false;
    private static volatile boolean isPaused = false;
    private static volatile long lastResetTime = 0;

    private static final Color PLAYER_X_COLOR = new Color(231, 76, 60);
    private static final Color PLAYER_O_COLOR = new Color(52, 152, 219);
    private static final Color BOARD_COLOR = new Color(44, 62, 80);
    private static final Color BUTTON_COLOR = new Color(236, 240, 241);
    private static final Color WIN_COLOR = new Color(46, 204, 113);

    public TwoGameUI(char player, Register registry, Server server, String serviceName) {
        this.player = player;
        this.registry = registry;
        this.server = server;
        this.serviceName = serviceName;
        
        initializeUI();
    }

    private void initializeUI() {
        setTitle("Player " + player + " - Round " + currentRound);
        setSize(420, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        getContentPane().setBackground(new Color(236, 240, 241));

        setLocation(player == 'X' ? 100 : 550, 100);

        // Header Panel
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(player == 'X' ? PLAYER_X_COLOR : PLAYER_O_COLOR);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        playerLabel = new JLabel("PLAYER " + player, SwingConstants.CENTER);
        playerLabel.setFont(new Font("Arial", Font.BOLD, 28));
        playerLabel.setForeground(Color.WHITE);
        
        headerPanel.add(playerLabel, BorderLayout.CENTER);

        // Stats Panel
        JPanel statsPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        statsPanel.setBackground(new Color(236, 240, 241));
        statsPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        roundLabel = createStatLabel("Round: " + currentRound, new Color(52, 73, 94));
        timerLabel = createStatLabel("Time: 00:00", new Color(230, 126, 34));
        statsLabel = createStatLabel("X:0 O:0 D:0", new Color(142, 68, 173));
        statusLabel = createStatLabel("Connecting...", new Color(52, 152, 219));

        statsPanel.add(roundLabel);
        statsPanel.add(timerLabel);
        statsPanel.add(statsLabel);
        statsPanel.add(statusLabel);

        // Combined North Panel (Header + Stats)
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(236, 240, 241));
        northPanel.add(headerPanel, BorderLayout.NORTH);
        northPanel.add(statsPanel, BorderLayout.SOUTH);
        
        add(northPanel, BorderLayout.NORTH);

        // Board Panel
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 8, 8));
        boardPanel.setBackground(BOARD_COLOR);
        boardPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        buttons = new JButton[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                final int position = i * 3 + j;
                buttons[i][j] = new JButton("");
                buttons[i][j].setFont(new Font("Arial", Font.BOLD, 48));
                buttons[i][j].setFocusPainted(false);
                buttons[i][j].setBackground(BUTTON_COLOR);
                buttons[i][j].setCursor(new Cursor(Cursor.HAND_CURSOR));
                buttons[i][j].setBorder(BorderFactory.createLineBorder(BOARD_COLOR, 2));

                buttons[i][j].addActionListener(e -> makeMove(position));
                boardPanel.add(buttons[i][j]);
            }
        }
        add(boardPanel, BorderLayout.CENTER);

        // Info Panel
        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBackground(new Color(236, 240, 241));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        logArea = new JTextArea(5, 30);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 9));
        logArea.setBackground(new Color(250, 250, 250));
        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Game Log"));
        infoPanel.add(scrollPane, BorderLayout.CENTER);

        // Control Panel
        JPanel controlPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        controlPanel.setBackground(new Color(236, 240, 241));
        controlPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JButton newRoundButton = createControlButton("New Round", new Color(46, 204, 113));
        newRoundButton.addActionListener(e -> requestNewRound());
        
        JButton resetButton = createControlButton("Reset Match", new Color(231, 76, 60));
        resetButton.addActionListener(e -> resetMatch());

        JButton renewButton = createControlButton("Renew", new Color(230, 126, 34));
        renewButton.addActionListener(e -> renewService());

        JButton pauseButton = createControlButton("Pause", new Color(142, 68, 173));
        pauseButton.addActionListener(e -> togglePause());


        controlPanel.add(newRoundButton);
        controlPanel.add(renewButton);
        controlPanel.add(pauseButton);
        controlPanel.add(resetButton);

        infoPanel.add(controlPanel, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.SOUTH);
    }

    private JLabel createStatLabel(String text, Color bgColor) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 12));
        label.setForeground(Color.WHITE);
        label.setBackground(bgColor);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return label;
    }

    private JButton createControlButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 10));
        button.setBackground(bgColor);
        button.setForeground(Color.BLACK);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return button;
    }

    @Override
    public void run() {
        try {
            setupGame();
        } catch (Exception e) {
            log("Error: " + e.getMessage());
            updateStatus("Failed!");
        }
    }

    private void setupGame() {
        try {
            log("Initializing game service...");
            
            log("Checking registry cache...");
            ServiceReference ref = registry.lookup(serviceName);
            
            if (ref == null) {
                log("Service not cached. Requesting from server...");
                ref = server.requestService(serviceName);
                registry.rebind(serviceName, ref);
                log("✓ New service instance created and cached.");
            } else {
                log("✓ Service found in registry cache!");
            }

            gameService = new GameServiceProxy(ref);
            
            // Reset the game board (keeps same service instance)
            log("Resetting game board...");
            gameService.resetGame();
            log("✓ Game board reset successfully.");
            
            log("✓ Connected to game service via proxy.");
            
            gameActive = true;
            elapsedSeconds = 0;
            
            updateStatus("Game Ready!");
            log("=== Round " + currentRound + " started ===");

            // Start timers
            startTimers();

        } catch (Exception e) {
            log("✗ Error initializing game: " + e.getMessage());
            updateStatus("Failed to initialize game!");
        }
    }

    private void startTimers() {
        // Stop existing timers first
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();
        
        refreshTimer = new Timer(300, e -> {
            updateBoard();
            checkSharedState(); // Check if other player triggered an action
        });
        refreshTimer.start();

        gameTimer = new Timer(1000, e -> {
            if (gameActive && !isPaused) {
                elapsedSeconds++;
                updateTimerDisplay();
            }
        });
        gameTimer.start();
    }
    
    /**
     * Checks if the other player triggered a shared action (new round, reset, pause).
     */
    private void checkSharedState() {
        // Check for new round request
        if (needsRoundReset && System.currentTimeMillis() - lastResetTime < 1000) {
            needsRoundReset = false;
            performNewRoundSync(); // Sync without incrementing
        }
        
        // Check for match reset request
        if (needsMatchReset && System.currentTimeMillis() - lastResetTime < 1000) {
            needsMatchReset = false;
            performMatchResetSync(); // Sync without resetting stats
        }
    }

    private void updateTimerDisplay() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }

    private void makeMove(int position) {
        if (!gameActive) {
            log("⚠ Game over!");
            return;
        }

        try {
            char currentTurn = (Character) gameService.getCurrentPlayer();
            
            if (currentTurn != player) {
                log("Not your turn!");
                return;
            }

            log("Move at position " + position);
            String result = (String) gameService.makeMove(player, position);
            log("→ " + result);

            updateBoard();

            String status = (String) gameService.getStatus();
            if (!status.equals("IN_PROGRESS")) {
                handleGameEnd(status);
            }

        } catch (Exception e) {
            log("Error: " + e.getMessage());
        }
    }

    private void handleGameEnd(String status) {
        gameActive = false;
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        updateStatus("Game Over!");
        log("=== " + status + " ===");

        // Update statistics (synchronized for both windows)
        synchronized (TwoGameUI.class) {
            if (status.contains("Player X wins")) {
                playerXWins++;
            } else if (status.contains("Player O wins")) {
                playerOWins++;
            } else if (status.contains("Draw")) {
                draws++;
            }
            updateStatsDisplay();
        }

        JOptionPane.showMessageDialog(this, 
            status + "\n\nTime: " + formatTime(elapsedSeconds) + 
            "\nRound: " + currentRound, 
            "Game Over", 
            JOptionPane.INFORMATION_MESSAGE);
    }

    private void updateBoard() {
        try {
            char[] board = (char[]) gameService.getBoard();
            
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int pos = i * 3 + j;
                    char cell = board[pos];
                    
                    if (cell != '-') {
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
                boolean isMyTurn = (currentTurn == player);
                updateStatus(isMyTurn ? "YOUR TURN!" : "Wait...");
                
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        if (buttons[i][j].getText().isEmpty()) {
                            buttons[i][j].setEnabled(isMyTurn);
                        }
                    }
                }
            }

        } catch (Exception e) {
            // Ignore refresh errors
        }
    }

    private void requestNewRound() {
        synchronized (TwoGameUI.class) {
            needsRoundReset = true;
            lastResetTime = System.currentTimeMillis();
        }
        performNewRound();
    }
    
    private void performNewRound() {
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        clearBoard();
        
        synchronized (TwoGameUI.class) {
            currentRound++;
            roundLabel.setText("Round: " + currentRound);
            setTitle("Player " + player + " - Round " + currentRound);
        }
        
        updateStatus("Starting new round...");
        
        log("\n" + "=".repeat(40));
        log("Starting Round " + currentRound);
        log("=".repeat(40));
        
        // Display cache before starting new round
        System.out.println("\n>>> BEFORE NEW ROUND (Player " + player + "):");
        registry.displayCache();
        
        // Setup game (reuses cached service + resets)
        setupGame();
        
        System.out.println("\n>>> AFTER NEW ROUND (Player " + player + "):");
        registry.displayCache();
    }
    
    /**
     * Syncs with other player's new round request without incrementing.
     */
    private void performNewRoundSync() {
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        clearBoard();
        
        // Just update display, don't increment (already incremented by other player)
        synchronized (TwoGameUI.class) {
            roundLabel.setText("Round: " + currentRound);
            setTitle("Player " + player + " - Round " + currentRound);
        }
        
        updateStatus("Starting new round...");
        
        log("\n" + "=".repeat(40));
        log("Syncing to Round " + currentRound);
        log("=".repeat(40));
        
        // Setup game (reuses cached service + resets)
        setupGame();
    }

    private void resetMatch() {
        synchronized (TwoGameUI.class) {
            needsMatchReset = true;
            lastResetTime = System.currentTimeMillis();
        }
        performMatchReset();
    }
    
    private void performMatchReset() {
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        clearBoard();
        
        synchronized (TwoGameUI.class) {
            currentRound = 1;
            playerXWins = 0;
            playerOWins = 0;
            draws = 0;
            
            roundLabel.setText("Round: 1");
            updateStatsDisplay();
            setTitle("Player " + player + " - Round 1");
        }
        
        elapsedSeconds = 0;
        logArea.setText("");
        
        updateStatus("New match started!");
        
        log("=".repeat(40));
        log("NEW MATCH STARTED");
        log("=".repeat(40));
        
        // Display cache before reset
        System.out.println("\n>>> BEFORE RESET MATCH (Player " + player + "):");
        registry.displayCache();
        
        // Setup game (creates new game instance)
        setupGame();
        
        System.out.println("\n>>> AFTER RESET MATCH (Player " + player + "):");
        registry.displayCache();
    }
    
    /**
     * Syncs with other player's match reset request without resetting stats.
     */
    private void performMatchResetSync() {
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();

        clearBoard();
        
        // Just update display, stats already reset by other player
        synchronized (TwoGameUI.class) {
            roundLabel.setText("Round: 1");
            updateStatsDisplay();
            setTitle("Player " + player + " - Round 1");
        }
        
        elapsedSeconds = 0;
        logArea.setText("");
        
        updateStatus("New match started!");
        
        log("=".repeat(40));
        log("NEW MATCH STARTED (Synced)");
        log("=".repeat(40));
        
        // Setup game (creates new game instance)
        setupGame();
    }

    private void renewService() {
        log("\n--- Renewing Service Connection ---");
        
        try {
            System.out.println("\n>>> BEFORE RENEWAL (Player " + player + "):");
            registry.displayCache();

            log("Requesting fresh service from server...");
            ServiceReference ref = server.requestService(serviceName);
            registry.rebind(serviceName, ref);
            log("✓ New service instance cached in registry.");
            
            System.out.println("\n>>> AFTER RENEWAL (Player " + player + "):");
            registry.displayCache();

            // Create new proxy
            gameService = new GameServiceProxy(ref);
            
            log("✓ Service successfully renewed!");
            log("✓ New proxy connection established.");
            updateStatus("Service renewed!");
            
            JOptionPane.showMessageDialog(this, 
                "Service connection renewed!\n" +
                "Fresh game instance created.", 
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

    private void togglePause() {
        synchronized (TwoGameUI.class) {
            isPaused = !isPaused;
        }
        
        if (gameTimer != null && refreshTimer != null) {
            if (isPaused) {
                updateStatus("Paused");
                log("⏸ Paused by Player " + player);
            } else {
                updateStatus("Resumed");
                log("▶ Resumed by Player " + player);
            }
        }
    }

    private void clearBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].setBackground(BUTTON_COLOR);
                buttons[i][j].setForeground(Color.BLACK);
            }
        }
        gameActive = false;
    }

    private void updateStatsDisplay() {
        String stats = String.format("X:%d O:%d D:%d", playerXWins, playerOWins, draws);
        statsLabel.setText(stats);
    }

    private void updateStatus(String text) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(text));
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }

    private void log(String message) {
        SwingUtilities.invokeLater(() -> {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Launch two player windows.
     */
    public static void launchTwoPlayers() {
        // Reset shared statistics for new game
        currentRound = 1;
        playerXWins = 0;
        playerOWins = 0;
        draws = 0;

        Register registry = new Register();
        Server server = new Server();
        String serviceName = "TicTacToeGame";

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }

            TwoGameUI playerX = new TwoGameUI('X', registry, server, serviceName);
            TwoGameUI playerO = new TwoGameUI('O', registry, server, serviceName);

            playerX.setVisible(true);
            playerO.setVisible(true);

            new Thread(playerX).start();
            new Thread(playerO).start();
        });
    }

    /**
     * Main method to launch the two-player UI.
     */
    public static void main(String[] args) {
        launchTwoPlayers();
    }
}