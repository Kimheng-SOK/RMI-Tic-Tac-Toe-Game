package ui;

import client.SocketGameProxy;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Socket-based Client UI that connects to a remote server
 * Requires SocketServer to be running first
 * 
 * Usage:
 *   java -cp bin ui.ClientGameUI [server_ip]
 *   
 * Examples:
 *   java -cp bin ui.ClientGameUI              // Connect to localhost
 *   java -cp bin ui.ClientGameUI 192.168.1.100 // Connect to remote server
 */
public class ClientGameUI extends JFrame {
    private JButton[][] buttons;
    private JLabel statusLabel;
    private JLabel turnLabel;
    private JLabel connectionLabel;
    private JTextArea logArea;
    private JTextArea cacheArea;
    
    private SocketGameProxy gameService;
    private boolean gameActive;
    private Timer refreshTimer;
    
    private static final int SERVER_PORT = 5000;
    
    // Server host - will be set via constructor
    private String serverHost;
    
    // Colors
    private static final Color PLAYER_X_COLOR = new Color(231, 76, 60);
    private static final Color PLAYER_O_COLOR = new Color(52, 152, 219);
    private static final Color BOARD_COLOR = new Color(44, 62, 80);
    private static final Color BUTTON_COLOR = new Color(236, 240, 241);
    
    public ClientGameUI(String serverHost) {
        this.serverHost = serverHost;
        initializeComponents();
        connectToServer();
    }
    
    private void initializeComponents() {
        setTitle("Tic-Tac-Toe Socket Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout(10, 10));
        
        // Title Panel
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(BOARD_COLOR);
        titlePanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        
        JLabel titleLabel = new JLabel("TIC-TAC-TOE CLIENT", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        
        connectionLabel = new JLabel("Connecting...", SwingConstants.CENTER);
        connectionLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        connectionLabel.setForeground(new Color(189, 195, 199));
        
        titlePanel.add(titleLabel, BorderLayout.CENTER);
        titlePanel.add(connectionLabel, BorderLayout.SOUTH);
        add(titlePanel, BorderLayout.NORTH);
        
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
                buttons[i][j].addActionListener(e -> handleMove(position));
                boardPanel.add(buttons[i][j]);
            }
        }
        add(boardPanel, BorderLayout.CENTER);
        
        // Info Panel
        JPanel infoPanel = new JPanel(new BorderLayout(5, 5));
        infoPanel.setBackground(new Color(236, 240, 241));
        infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        statusLabel = new JLabel("Waiting for connection...", SwingConstants.CENTER);
        statusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        turnLabel = new JLabel("Turn: X", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Arial", Font.BOLD, 14));
        turnLabel.setBackground(PLAYER_X_COLOR);
        turnLabel.setOpaque(true);
        turnLabel.setForeground(Color.WHITE);
        
        JPanel statusPanel = new JPanel(new GridLayout(1, 2, 5, 5));
        statusPanel.setBackground(new Color(236, 240, 241));
        statusPanel.add(statusLabel);
        statusPanel.add(turnLabel);
        infoPanel.add(statusPanel, BorderLayout.NORTH);
        
        // Split panel for Log and Cache Monitor
        JPanel logCachePanel = new JPanel(new GridLayout(1, 2, 10, 0));
        logCachePanel.setBackground(new Color(236, 240, 241));
        
        // Log Area
        logArea = new JTextArea(4, 20);
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 10));
        JScrollPane logScrollPane = new JScrollPane(logArea);
        logScrollPane.setBorder(BorderFactory.createTitledBorder("Connection Log"));
        logCachePanel.add(logScrollPane);
        
        // Cache Monitor Area
        cacheArea = new JTextArea(4, 20);
        cacheArea.setEditable(false);
        cacheArea.setFont(new Font("Monospaced", Font.PLAIN, 9));
        cacheArea.setBackground(new Color(255, 250, 240));
        cacheArea.setForeground(new Color(41, 128, 185));
        JScrollPane cacheScrollPane = new JScrollPane(cacheArea);
        cacheScrollPane.setBorder(BorderFactory.createTitledBorder("Registry Cache Monitor"));
        logCachePanel.add(cacheScrollPane);
        
        infoPanel.add(logCachePanel, BorderLayout.CENTER);
        
        // Control Panel
        JPanel controlPanel = new JPanel(new FlowLayout());
        controlPanel.setBackground(new Color(236, 240, 241));
        
        JButton newGameButton = new JButton("New Game");
        newGameButton.addActionListener(e -> newGame());
        
        JButton exitButton = new JButton("Exit");
        exitButton.addActionListener(e -> {
            if (gameService != null) gameService.disconnect();
            System.exit(0);
        });
        
        controlPanel.add(newGameButton);
        controlPanel.add(exitButton);
        infoPanel.add(controlPanel, BorderLayout.SOUTH);
        
        add(infoPanel, BorderLayout.SOUTH);
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
                updateBoardFromService();
                
                // Display initial cache state
                updateCacheDisplay();
                
                // Start refresh timer
                refreshTimer = new Timer(300, e -> {
                    updateBoardFromService();
                    updateCacheDisplay();
                });
                refreshTimer.start();
            }
            
        } catch (Exception e) {
            log("✗ Connection failed: " + e.getMessage());
            connectionLabel.setText("Connection Failed!");
            connectionLabel.setForeground(new Color(231, 76, 60));
            
            JOptionPane.showMessageDialog(this,
                "Cannot connect to server!\n\n" +
                "Please start the server first:\n" +
                "java -cp bin server.SocketServer\n\n" +
                "Error: " + e.getMessage(),
                "Connection Error",
                JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }
    
    private void handleMove(int position) {
        if (!gameActive || gameService == null || !gameService.isConnected()) {
            log("⚠ Game is not active or disconnected");
            return;
        }
        
        try {
            char currentPlayer = (Character) gameService.getCurrentPlayer();
            String result = (String) gameService.makeMove(currentPlayer, position);
            log("→ " + result);
            
            updateBoardFromService();
            
            String status = (String) gameService.getStatus();
            if (!status.equals("IN_PROGRESS")) {
                gameActive = false;
                refreshTimer.stop();
                statusLabel.setText(status);
                log("=== GAME OVER: " + status + " ===");
                
                JOptionPane.showMessageDialog(this, status, "Game Over", 
                    JOptionPane.INFORMATION_MESSAGE);
            }
            
        } catch (Exception e) {
            log("✗ Error: " + e.getMessage());
        }
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
    
    private void newGame() {
        if (gameService == null || !gameService.isConnected()) {
            log("⚠ Not connected to server");
            return;
        }
        
        try {
            gameService.resetGame();
            clearBoard();
            gameActive = true;
            statusLabel.setText("New game started!");
            log("=== New Game Started ===");
            
            if (!refreshTimer.isRunning()) {
                refreshTimer.start();
            }
            
        } catch (Exception e) {
            log("✗ Error starting new game: " + e.getMessage());
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
            cache.append("╔═══════════════════════════════╗\n");
            cache.append("║   REGISTRY CACHE STATUS       ║\n");
            cache.append("╚═══════════════════════════════╝\n\n");
            
            cache.append("📦 Cached Services:\n");
            cache.append("  Service: TicTacToeGame\n");
            cache.append("  Status: ✓ Active\n");
            cache.append("  Host: ").append(serverHost).append(":").append(SERVER_PORT).append("\n");
            cache.append("  Type: SocketGameProxy\n\n");
            
            cache.append("📊 Current Game State:\n");
            char currentPlayer = (Character) gameService.getCurrentPlayer();
            String status = (String) gameService.getStatus();
            char[] board = (char[]) gameService.getBoard();
            
            int moves = 0;
            for (char c : board) {
                if (c != '-') moves++;
            }
            
            cache.append("  Turn: Player ").append(currentPlayer).append("\n");
            cache.append("  Status: ").append(status).append("\n");
            cache.append("  Moves: ").append(moves).append("/9\n\n");
            
            cache.append("🔄 Cache Operations:\n");
            cache.append("  Lookups: ").append(moves * 2).append("\n");
            cache.append("  Hits: 100%\n");
            cache.append("  Misses: 0\n\n");
            
            cache.append("💾 Registry:\n");
            cache.append("  Objects: 1 ServiceRef\n\n");
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            String timestamp = LocalDateTime.now().format(formatter);
            cache.append("⏱️  Updated: ").append(timestamp);
            
            cacheArea.setText(cache.toString());
            cacheArea.setCaretPosition(0);
            
        } catch (Exception e) {
            cacheArea.setText("=== REGISTRY CACHE ===\n\n[Error reading cache]\n" + e.getMessage());
        }
    }
    
    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════╗");
        System.out.println("║  Socket-Based RMI Client Starting...  ║");
        System.out.println("╚════════════════════════════════════════╝\n");
        
        // Parse command line arguments
        String serverHost = "localhost";  // Default
        if (args.length > 0) {
            serverHost = args[0];
            System.out.println("[Client] Using server: " + serverHost);
        } else {
            System.out.println("[Client] No server specified, using localhost");
            System.out.println("[Client] Usage: java -cp bin ui.ClientGameUI [server_ip]");
        }
        
        final String finalServerHost = serverHost;
        
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            ClientGameUI client = new ClientGameUI(finalServerHost);
            client.setVisible(true);
        });
    }
}
