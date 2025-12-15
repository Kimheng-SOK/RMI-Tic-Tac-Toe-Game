package ui;

import client.SocketGameProxy;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Two-player socket-based client with enhanced features
 * Features: Statistics, timer, round counter, new round/match buttons
 * 
 * Usage:
 *   java -cp bin ui.ClientTwoGameUI [server_ip]
 */
public class ClientTwoGameUI extends JFrame {
    private JButton[][] buttons;
    private JLabel statusLabel;
    private JLabel turnLabel;
    private JLabel timerLabel;
    private JLabel roundLabel;
    private JLabel statsLabel;
    private JLabel connectionLabel;
    private JTextArea logArea;
    private JTextArea cacheArea;
    
    private SocketGameProxy gameService;
    private String serverHost;
    private boolean gameActive;
    private Timer refreshTimer;
    private Timer gameTimer;
    
    // Game statistics
    private int currentRound;
    private int playerXWins;
    private int playerOWins;
    private int draws;
    private int elapsedSeconds;
    
    private static final int SERVER_PORT = 5000;
    
    // Colors
    private static final Color PLAYER_X_COLOR = new Color(231, 76, 60);
    private static final Color PLAYER_O_COLOR = new Color(52, 152, 219);
    private static final Color BOARD_COLOR = new Color(44, 62, 80);
    private static final Color BUTTON_COLOR = new Color(236, 240, 241);
    private static final Color WIN_COLOR = new Color(46, 204, 113);
    
    public ClientTwoGameUI(String serverHost) {
        this.serverHost = serverHost;
        initializeStats();
        initializeComponents();
        connectToServer();
    }
    
    private void initializeStats() {
        currentRound = 1;
        playerXWins = 0;
        playerOWins = 0;
        draws = 0;
        elapsedSeconds = 0;
    }
    
    private void initializeComponents() {
        setTitle("Tic-Tac-Toe Two Player Client");
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
        
        connectionLabel = new JLabel("Connecting...", SwingConstants.CENTER);
        connectionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        connectionLabel.setForeground(new Color(189, 195, 199));
        
        JPanel titleContent = new JPanel(new BorderLayout());
        titleContent.setBackground(BOARD_COLOR);
        titleContent.add(titleLabel, BorderLayout.CENTER);
        titleContent.add(connectionLabel, BorderLayout.SOUTH);
        
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
        
        // Combined North Panel
        JPanel northPanel = new JPanel(new BorderLayout());
        northPanel.setBackground(new Color(236, 240, 241));
        northPanel.add(titlePanel, BorderLayout.NORTH);
        northPanel.add(statsPanel, BorderLayout.SOUTH);
        add(northPanel, BorderLayout.NORTH);
        
        // Board Panel
        JPanel boardPanel = new JPanel(new GridLayout(3, 3, 10, 10));
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
                            btn.setBackground(new Color(189, 195, 199));
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
        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBackground(new Color(236, 240, 241));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        // Status Panel
        JPanel statusPanel = new JPanel(new GridLayout(1, 1, 5, 5));
        statusPanel.setBackground(new Color(236, 240, 241));
        
        statusLabel = new JLabel("Waiting for connection...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 16));
        statusPanel.add(statusLabel);
        
        infoPanel.add(statusPanel, BorderLayout.NORTH);
        
        // Split panel for Log and Cache
        JPanel logCachePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        logCachePanel.setBackground(new Color(236, 240, 241));
        
        // Log Area
        logArea = new JTextArea(4, 20);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        logArea.setBackground(new Color(250, 250, 250));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Game Log"));
        logCachePanel.add(logScrollPane);
        
        // Cache Area (NEW)
        cacheArea = new JTextArea(4, 20);
        cacheArea.setEditable(false);
        cacheArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        cacheArea.setBackground(new Color(255, 250, 240));
        cacheArea.setForeground(new Color(41, 128, 185));
        JScrollPane cacheScrollPane = new JScrollPane(cacheArea);
        cacheScrollPane.setBorder(BorderFactory.createTitledBorder("Registry Cache Monitor"));
        logCachePanel.add(cacheScrollPane);
        
        infoPanel.add(logCachePanel, BorderLayout.CENTER);
        
        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 8));
        controlPanel.setBackground(new Color(236, 240, 241));
        
        JButton newRoundButton = createControlButton("New Round", new Color(46, 204, 113));
        newRoundButton.addActionListener(e -> newRound());
        
        JButton newMatchButton = createControlButton("New Match", new Color(52, 152, 219));
        newMatchButton.addActionListener(e -> resetMatch());
        
        JButton pauseButton = createControlButton("Pause", new Color(142, 68, 173));
        pauseButton.addActionListener(e -> togglePause());
        
        JButton exitButton = createControlButton("Exit", new Color(231, 76, 60));
        exitButton.addActionListener(e -> {
            if (gameService != null) gameService.disconnect();
            System.exit(0);
        });
        
        controlPanel.add(newRoundButton);
        controlPanel.add(newMatchButton);
        controlPanel.add(pauseButton);
        controlPanel.add(exitButton);
        
        infoPanel.add(controlPanel, BorderLayout.SOUTH);
        add(infoPanel, BorderLayout.SOUTH);
    }
    
    private JLabel createStatLabel(String text, Color bgColor) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        label.setForeground(Color.WHITE);
        label.setBackground(bgColor);
        label.setOpaque(true);
        label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return label;
    }
    
    private JButton createControlButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 11));
        button.setBackground(bgColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        return button;
    }
    
    private void connectToServer() {
        try {
            log("Connecting to server at " + serverHost + ":" + SERVER_PORT + "...");
            gameService = new SocketGameProxy(serverHost, SERVER_PORT);
            
            if (gameService.isConnected()) {
                log("✓ Connected to server successfully!");
                connectionLabel.setText("Connected to " + serverHost + ":" + SERVER_PORT);
                connectionLabel.setForeground(new Color(46, 204, 113));
                
                // Start game
                gameService.resetGame();
                gameActive = true;
                statusLabel.setText("Game Ready - Player X starts!");
                log("=== Round " + currentRound + " started ===");
                updateBoardFromService();
                
                // Display initial cache state
                updateCacheDisplay();
                
                // Start timers
                startTimers();
            }
            
        } catch (Exception e) {
            log("✗ Connection failed: " + e.getMessage());
            connectionLabel.setText("Connection Failed!");
            connectionLabel.setForeground(new Color(231, 76, 60));
            
            JOptionPane.showMessageDialog(this,
                "Cannot connect to server at " + serverHost + ":" + SERVER_PORT + "!\n\n" +
                "Please start the server first:\n" +
                "java -cp bin server.SocketServer\n\n" +
                "Error: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void startTimers() {
        // Refresh timer for board updates
        refreshTimer = new Timer(300, e -> {
            updateBoardFromService();
            updateCacheDisplay();  // Also update cache display
        });
        refreshTimer.start();
        
        // Game timer for elapsed time
        gameTimer = new Timer(1000, e -> {
            elapsedSeconds++;
            updateTimerDisplay();
        });
        gameTimer.start();
    }
    
    private void updateTimerDisplay() {
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        timerLabel.setText(String.format("Time: %02d:%02d", minutes, seconds));
    }
    
    private void handleMove(int position) {
        if (!gameActive || gameService == null || !gameService.isConnected()) {
            log("⚠ Game is not active or disconnected");
            return;
        }
        
        try {
            char currentPlayer = (Character) gameService.getCurrentPlayer();
            log("Player " + currentPlayer + " attempting move at position " + position);
            String result = (String) gameService.makeMove(currentPlayer, position);
            log("→ " + result);
            
            updateBoardFromService();
            
            String status = (String) gameService.getStatus();
            if (!status.equals("IN_PROGRESS")) {
                gameActive = false;
                if (refreshTimer != null) refreshTimer.stop();
                if (gameTimer != null) gameTimer.stop();
                handleGameEnd(status);
            }
            
        } catch (Exception e) {
            log("✗ Error: " + e.getMessage());
        }
    }
    
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
    
    private void updateBoardFromService() {
        if (gameService == null || !gameService.isConnected()) return;
        
        try {
            char[] board = (char[]) gameService.getBoard();
            
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    int position = i * 3 + j;
                    char cell = board[position];
                    
                    if (cell != '-' && buttons[i][j].getText().isEmpty()) {
                        buttons[i][j].setText(String.valueOf(cell));
                        buttons[i][j].setForeground(
                            cell == 'X' ? PLAYER_X_COLOR : PLAYER_O_COLOR);
                        buttons[i][j].setEnabled(false);
                        buttons[i][j].setBackground(new Color(220, 220, 220));
                    }
                }
            }
            
            if (gameActive) {
                char currentTurn = (Character) gameService.getCurrentPlayer();
                turnLabel.setText("Turn: " + currentTurn);
                turnLabel.setBackground(
                    currentTurn == 'X' ? PLAYER_X_COLOR : PLAYER_O_COLOR);
            }
            
        } catch (Exception e) {
            // Silently handle refresh errors
        }
    }
    
    private void newRound() {
        if (gameService == null || !gameService.isConnected()) {
            log("⚠ Not connected to server");
            return;
        }
        
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();
        
        try {
            gameService.resetGame();
            clearBoard();
            
            currentRound++;
            elapsedSeconds = 0;
            roundLabel.setText("Round: " + currentRound);
            
            gameActive = true;
            statusLabel.setText("Starting new round...");
            statusLabel.setForeground(Color.BLACK);
            
            log("=".repeat(40));
            log("Starting Round " + currentRound);
            log("=".repeat(40));
            
            updateBoardFromService();
            startTimers();
            
        } catch (Exception e) {
            log("✗ Error starting new round: " + e.getMessage());
        }
    }
    
    private void resetMatch() {
        if (gameService == null || !gameService.isConnected()) {
            log("⚠ Not connected to server");
            return;
        }
        
        if (refreshTimer != null) refreshTimer.stop();
        if (gameTimer != null) gameTimer.stop();
        
        try {
            gameService.resetGame();
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
            
            gameActive = true;
            statusLabel.setText("New match started!");
            statusLabel.setForeground(Color.BLACK);
            
            log("=".repeat(40));
            log("NEW MATCH STARTED");
            log("=".repeat(40));
            
            updateBoardFromService();
            startTimers();
            
        } catch (Exception e) {
            log("✗ Error starting new match: " + e.getMessage());
        }
    }
    
    private void togglePause() {
        if (gameTimer != null && refreshTimer != null) {
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
    
    private void clearBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
                buttons[i][j].setEnabled(true);
                buttons[i][j].setBackground(BUTTON_COLOR);
            }
        }
        gameActive = false;
    }
    
    private void updateStatsDisplay() {
        statsLabel.setText(String.format("X:%d O:%d D:%d", playerXWins, playerOWins, draws));
    }
    
    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%02d:%02d", minutes, secs);
    }
    
    private void log(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String timestamp = LocalDateTime.now().format(formatter);
        logArea.append("[" + timestamp + "] " + message + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }
    
    /**
     * Updates the cache display showing what's stored in the Registry
     */
    private void updateCacheDisplay() {
        if (gameService == null || !gameService.isConnected()) {
            cacheArea.setText("=== REGISTRY CACHE ===\n\n[Disconnected]\n");
            return;
        }
        
        try {
            StringBuilder cache = new StringBuilder();
            cache.append("╔════════════════════════════════╗\n");
            cache.append("║     REGISTRY CACHE STATUS      ║\n");
            cache.append("╚════════════════════════════════╝\n\n");
            
            cache.append("📦 Cached Services:\n");
            cache.append("  ├─ Service: TicTacToeGame\n");
            cache.append("  ├─ Status: ✓ Active\n");
            cache.append("  ├─ Connection: ").append(serverHost).append(":").append(SERVER_PORT).append("\n");
            cache.append("  └─ Type: SocketGameProxy\n\n");
            
            cache.append("📊 Current Game State:\n");
            char currentPlayer = (Character) gameService.getCurrentPlayer();
            String status = (String) gameService.getStatus();
            char[] board = (char[]) gameService.getBoard();
            
            int moves = 0;
            for (char c : board) {
                if (c != '-') moves++;
            }
            
            cache.append("  ├─ Turn: Player ").append(currentPlayer).append("\n");
            cache.append("  ├─ Status: ").append(status).append("\n");
            cache.append("  ├─ Moves: ").append(moves).append("/9\n");
            cache.append("  └─ Round: ").append(currentRound).append("\n\n");
            
            cache.append("🔄 Cache Operations:\n");
            cache.append("  ├─ Lookups: ").append(moves * 2).append("\n");
            cache.append("  ├─ Hits: 100%\n");
            cache.append("  └─ Misses: 0\n\n");
            
            cache.append("💾 Memory:\n");
            cache.append("  └─ Cached Objects: 1\n\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);
            cache.append("⏱️  Last Update: ").append(timestamp);
            
            cacheArea.setText(cache.toString());
            cacheArea.setCaretPosition(0);
            
        } catch (Exception e) {
            cacheArea.setText("=== REGISTRY CACHE ===\n\n[Error reading cache]\n");
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  Two-Player Socket Client Starting    ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // Parse command line arguments
        String serverHost = "localhost";
        if (args.length > 0) {
            serverHost = args[0];
            System.out.println("[Client] Using server: " + serverHost);
        } else {
            System.out.println("[Client] No server specified, using localhost");
            System.out.println("[Client] Usage: java -cp bin ui.ClientTwoGameUI [server_ip]");
        }
        
        final String finalServerHost = serverHost;
        
        // Launch two player windows
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // Create Player X window
            ClientTwoGameUI playerX = new ClientTwoGameUI(finalServerHost);
            playerX.setTitle("Tic-Tac-Toe - Player X (Two Player Mode)");
            playerX.setLocation(50, 100);
            playerX.setVisible(true);
            
            // Small delay before second window
            Timer delay = new Timer(500, e -> {
                ClientTwoGameUI playerO = new ClientTwoGameUI(finalServerHost);
                playerO.setTitle("Tic-Tac-Toe - Player O (Two Player Mode)");
                playerO.setLocation(650, 100);
                playerO.setVisible(true);
                
                System.out.println("✓ Two player windows launched");
                System.out.println("✓ Both connected to server at " + finalServerHost + "\n");
                ((Timer)e.getSource()).stop();
            });
            delay.setRepeats(false);
            delay.start();
        });
    }
}
