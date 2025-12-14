# RMI Tic-Tac-Toe Game

A distributed Tic-Tac-Toe game demonstrating Remote Method Invocation (RMI) architecture patterns including Proxy, Registry, and Dispatcher components.

## 📋 Table of Contents

- [Overview](#-overview)
- [Installation](#-installation)
- [Features](#-features)
- [Architecture](#-architecture)
- [Project Structure](#-project-structure)
- [Requirements](#-requirements)
- [Usage](#-usage)
- [Use Cases](#-use-cases)
- [RMI Components](#-rmi-components)
- [Game Flow](#-game-flow)
- [Design Patterns](#-design-patterns)
- [Limitations](#-limitations)
- [Future Enhancements](#-future-enhancements)
- [Performance Considerations](#-performance-considerations)
- [Contributing](#-contributing)
- [License](#-license)
- [Authors](#-authors)
- [Acknowledgments](#-acknowledgments)

## 🎯 Overview

This project implements a **Tic-Tac-Toe game** using **RMI (Remote Method Invocation) architectural patterns** to demonstrate distributed systems concepts. The implementation simulates a client-server architecture where:

- **Clients** interact with the game through a graphical user interface
- **Proxy** handles client-side remote method calls
- **Registry** manages service discovery and caching
- **Dispatcher** routes method calls to appropriate services
- **Service** contains the actual game logic

The system supports both **single-player** (vs AI) and **two-player** (local multiplayer) modes.

## 🚀 Installation

### Option 1: Using Command Line

```bash
# 1. Clone the repository
git clone <repository-url>
cd RMI-Tic-Tac-Toe-V1

# 2. Compile all Java files
javac -d bin src/**/*.java
or
javac -d bin src\registry\*.java src\client\*.java src\server\*.java src\ui\*.java


# 3. Run single-player mode
java -cp bin ui.GameUI

# 4. Or run two-player mode
java -cp bin ui.TwoGameUI
```

## ✨ Features

### Game Features
- ✅ Classic 3x3 Tic-Tac-Toe gameplay
- ✅ Turn-based player switching
- ✅ Win detection (rows, columns, diagonals)
- ✅ Draw detection
- ✅ Real-time board updates

### UI Features
- 🎨 Modern GUI with color-coded players
- ⏱️ Game timer
- 📊 Round counter
- 📈 Match statistics (wins, losses, draws)
- 📝 Game event logging
- ⏸️ Pause/Resume functionality

### RMI Features
- 🔄 Service discovery and caching
- 🔌 Service renewal mechanism
- 🎯 Remote method invocation
- 📦 Registry-based service management
- 🔀 Request dispatching
- 🔒 Thread-safe operations

### Multiplayer Features (Two-Player Mode)
- 👥 Separate windows for each player
- 🔄 Synchronized game state
- 🎮 Turn enforcement
- 📊 Shared statistics
- ⏸️ Synchronized pause/resume
- 🔄 Synchronized round/match reset

## 🏗️ Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    CLIENT LAYER                         │
│  ┌──────────────┐              ┌──────────────┐         │
│  │   GameUI     │              │  TwoGameUI   │         │
│  │ (Single Play)│              │ (Two Players)│         │
│  └──────┬───────┘              └──────┬───────┘         │
│         │                             │                 │
│         └─────────────┬───────────────┘                 │
│                       ↓                                 │
│              ┌─────────────────┐                        │
│              │ GameServiceProxy│                        │
│              │   (RMI Stub)    │                        │
│              └────────┬────────┘                        │
└───────────────────────┼─────────────────────────────────┘
                        │
┌───────────────────────┼─────────────────────────────────┐
│                       ↓      REGISTRY LAYER             │
│              ┌─────────────────┐                        │
│              │    Register     │                        │
│              │  (Cache + Bind) │                        │
│              └────────┬────────┘                        │
└───────────────────────┼─────────────────────────────────┘
                        │
┌───────────────────────┼─────────────────────────────────┐
│                       ↓      SERVER LAYER               │
│              ┌─────────────────┐                        │
│              │     Server      │                        │
│              │ (Service Mgmt)  │                        │
│              └────────┬────────┘                        │
│                       ↓                                 │
│              ┌─────────────────┐                        │
│              │ServerDispatcher │                        │
│              │ (RMI Skeleton)  │                        │
│              └────────┬────────┘                        │
│                       ↓                                 │
│              ┌─────────────────┐                        │
│              │TicTacToeService │                        │
│              │  (Game Logic)   │                        │
│              └─────────────────┘                        │
└─────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

```
1. SERVICE DISCOVERY
   GameUI → Register.lookup() → ServiceReference
                    ↓ (if not cached)
   Register → Server.requestService() → TicTacToeService
   
2. METHOD INVOCATION
   GameUI → GameServiceProxy.makeMove()
            ↓
   Proxy → ServerDispatcher.handleRequest()
            ↓
   Dispatcher → TicTacToeService.makeMove()
            ↓
   Result ← ← ← (returns back through chain)
```

## 📁 Project Structure

```
RMI-Tic-Tac-Toe-V1/
│
├── src/
│   ├── client/                    # Client-side components
│   │   ├── GameServiceProxy.java  # RMI stub (proxy pattern)
│   │   └── ServiceReference.java  # Service metadata holder
│   │
│   ├── registry/                  # Service registry
│   │   └── Register.java          # Service discovery & caching
│   │
│   ├── server/                    # Server-side components
│   │   ├── Server.java            # Service provider
│   │   ├── ServerDispatcher.java  # RMI skeleton (request router)
│   │   └── TicTacToeService.java  # Game logic implementation
│   │
│   └── ui/                        # User interface
│       ├── GameUI.java            # Single-player interface
│       └── TwoGameUI.java         # Two-player interface
│
├── README.md                      # This file
└── .gitignore
```

## 📋 Requirements

### System Requirements
- **Java Development Kit (JDK)**: 8 or higher
- **Operating System**: Windows, macOS, or Linux
- **Memory**: Minimum 512 MB RAM
- **Display**: 1024x768 or higher resolution

### Java Libraries
- `javax.swing.*` - GUI components
- `java.awt.*` - UI rendering
- `java.util.*` - Data structures
- `java.time.*` - Timestamps

### Development Tools (Optional)
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code
- **Build Tool**: Maven or Gradle (optional)
- **Version Control**: Git

### Option 2: Using IDE (IntelliJ IDEA)

```
1. Open IntelliJ IDEA
2. File → Open → Select project folder
3. Wait for indexing to complete
4. Right-click on GameUI.java or TwoGameUI.java
5. Select "Run 'GameUI.main()'" or "Run 'TwoGameUI.main()'"
```

### Option 3: Using VS Code

```
1. Open VS Code
2. Install "Extension Pack for Java"
3. File → Open Folder → Select project folder
4. Open GameUI.java or TwoGameUI.java
5. Click "Run" button above main method
```

## 🎮 Usage

### Single-Player Mode

```bash
java -cp bin ui.GameUI
```

**Features:**
- Play against yourself or test game logic
- Practice turn-based gameplay
- Full statistics tracking
- Service renewal and caching demonstration

**Controls:**
- Click board cells to make moves
- **New Round**: Start a new game (keeps statistics)
- **Reset Match**: Clear all statistics
- **Renew Service**: Request fresh service instance
- **Pause**: Pause the game timer

### Two-Player Mode

```bash
java -cp bin ui.TwoGameUI
```

**Features:**
- Two separate windows (Player X and Player O)
- Synchronized game state
- Turn enforcement
- Shared statistics across both windows
- Synchronized controls (pause, reset, etc.)

**Controls:**
- Each player clicks cells in their own window
- Controls affect both players simultaneously
- Only the player whose turn it is can make moves

### Game Rules

1. **Starting**: Player X always goes first
2. **Turns**: Players alternate turns
3. **Winning**: Get 3 in a row (horizontal, vertical, or diagonal)
4. **Draw**: All cells filled with no winner
5. **New Round**: Keeps match statistics
6. **Reset Match**: Clears all statistics and starts fresh

## 📖 Use Cases

### Use Case 1: Playing a Single Game

```
Actor: Player
Preconditions: Application launched
Flow:
1. Player launches GameUI
2. System initializes game service
3. System displays empty board
4. Player clicks a cell
5. System validates move
6. System updates board
7. System checks for win/draw
8. If game continues, switch turns (steps 4-7 repeat)
9. If game ends, display result
Postconditions: Game statistics updated
```

### Use Case 2: Two-Player Match

```
Actor: Two Players (X and O)
Preconditions: TwoGameUI launched
Flow:
1. System creates two windows
2. Both players connect to same service
3. Player X makes first move
4. System validates turn
5. System updates both displays
6. Player O makes move
7. Steps 4-6 repeat until game ends
8. System displays result in both windows
9. Statistics synchronized across both players
Postconditions: Both windows show same state
```

### Use Case 3: Service Renewal

```
Actor: Player
Preconditions: Game in progress
Flow:
1. Player clicks "Renew Service"
2. System requests new service from server
3. System updates registry cache
4. System creates new proxy connection
5. System confirms renewal to player
6. Game continues with fresh service
Postconditions: New service instance cached
```

### Use Case 4: Registry Caching

```
Actor: System
Preconditions: Service request made
Flow:
1. Client requests service
2. Registry checks cache
3a. If cached: Return cached reference
3b. If not cached:
    - Request service from server
    - Server creates service instance
    - Cache service reference
    - Return reference
4. Client receives service reference
Postconditions: Service available for use
```

## 🔧 RMI Components

### 1. GameServiceProxy (Client-Side Stub)

**Purpose**: Represents the remote service on the client side

**Key Methods:**
```java
Object makeMove(char player, int position)
Object getBoard()
Object getStatus()
Object getCurrentPlayer()
Object resetGame()
```

**Responsibilities:**
- Hides network complexity from client
- Forwards method calls to dispatcher
- Provides location transparency

### 2. Register (Service Registry)

**Purpose**: Manages service discovery and caching

**Key Methods:**
```java
void bind(String serviceName, ServiceReference ref)
void rebind(String serviceName, ServiceReference ref)
ServiceReference lookup(String serviceName)
void displayCache()
```

**Responsibilities:**
- Cache service references
- Provide service lookup
- Manage service lifecycle

### 3. ServerDispatcher (Server-Side Skeleton)

**Purpose**: Routes client requests to appropriate services

**Key Methods:**
```java
void registerService(String name, Object service)
Object handleRequest(String serviceName, String methodName, Object... params)
```

**Responsibilities:**
- Register services
- Route method calls
- Handle parameters
- Return results

### 4. TicTacToeService (Service Implementation)

**Purpose**: Contains actual game logic

**Key Methods:**
```java
synchronized String makeMove(char player, int position)
synchronized char[] getBoard()
synchronized String getStatus()
synchronized char getCurrentPlayer()
synchronized String resetGame()
```

**Responsibilities:**
- Maintain game state
- Validate moves
- Check win conditions
- Thread-safe operations

## 🎯 Game Flow

### Standard Game Flow

```
1. INITIALIZATION
   ├── Create Registry
   ├── Create Server
   └── Create GameServiceProxy

2. SERVICE DISCOVERY
   ├── Lookup service in registry
   ├── If not cached: Request from server
   └── Cache service reference

3. GAME LOOP
   ├── Display board
   ├── Wait for player input
   ├── Validate move (RMI call)
   ├── Update board (RMI call)
   ├── Check game status (RMI call)
   └── If game not over, repeat

4. GAME END
   ├── Display result
   ├── Update statistics
   └── Offer new round/reset
```

### RMI Method Call Flow

```
Client Action: Player clicks cell 5
    ↓
1. GameUI.handleCellClick(1, 2) // row=1, col=2, pos=5
    ↓
2. gameService.makeMove('X', 5)
    ↓
3. GameServiceProxy.makeMove('X', 5)
    ↓
4. dispatcher.handleRequest("TicTacToeGame", "makeMove", 'X', 5)
    ↓
5. ServerDispatcher routes to service
    ↓
6. TicTacToeService.makeMove('X', 5)
    ↓
7. Validates: turn, position, game status
    ↓
8. Updates: board[5] = 'X', currentPlayer = 'O'
    ↓
9. Returns: "Move accepted. Player O's turn."
    ↓
10. Result propagates back through chain
    ↓
11. GameUI updates display
```

## 🎨 Design Patterns

### 1. Proxy Pattern

**Location**: `GameServiceProxy.java`

**Purpose**: Provide a surrogate for the remote service

```java
// Client calls proxy as if it's local
gameService.makeMove('X', 0);

// Proxy forwards to remote service
dispatcher.handleRequest("TicTacToeGame", "makeMove", 'X', 0);
```

**Benefits:**
- Location transparency
- Simplified client code
- Easy to add caching/logging

### 2. Registry Pattern

**Location**: `Register.java`

**Purpose**: Centralized service discovery

```java
// Lookup service
ServiceReference ref = registry.lookup("TicTacToeGame");

// If not found, request and cache
if (ref == null) {
    ref = server.requestService("TicTacToeGame");
    registry.rebind("TicTacToeGame", ref);
}
```

**Benefits:**
- Service reusability
- Reduced server load
- Fast service access

### 3. Dispatcher Pattern

**Location**: `ServerDispatcher.java`

**Purpose**: Route requests to appropriate handlers

```java
switch (methodName) {
    case "makeMove":
        return gameService.makeMove(player, position);
    case "getBoard":
        return gameService.getBoard();
    // ... more methods
}
```

**Benefits:**
- Centralized routing logic
- Easy to add new methods
- Clean separation of concerns

### 4. Singleton Pattern

**Location**: Static variables in `TwoGameUI.java`

**Purpose**: Share state across multiple instances

```java
private static int currentRound = 1;
private static int playerXWins = 0;
private static volatile boolean needsRoundReset = false;
```

**Benefits:**
- Synchronized state
- Shared statistics
- Global control flags

## ⚠️ Limitations

### Current Implementation

1. **Single JVM Only**
   - Both clients run in same process
   - Cannot run on different machines
   - Shared memory dependencies

2. **No Network Communication**
   - No TCP/IP sockets
   - No port usage
   - No serialization

3. **Simulated RMI**
   - Demonstrates RMI patterns
   - Not true distributed system
   - Direct method calls

4. **Shared State Dependencies**
   - Static variables require same JVM
   - Cannot scale across processes
   - Memory-based synchronization

### What's Missing for True Distribution

```markdown
❌ Network Layer
   • Socket connections
   • Port management
   • Message serialization

❌ Remote Communication
   • Java RMI (java.rmi.*)
   • Protocol handling
   • Network error handling

❌ Service Isolation
   • Separate server process
   • Independent client processes
   • Cross-machine support

❌ Security
   • Authentication
   • Authorization
   • Encryption
```

## 🚀 Future Enhancements

### Phase 1: True RMI Implementation

```java
// Add Java RMI support
import java.rmi.*;
import java.rmi.registry.*;
import java.rmi.server.*;

// Create remote interface
public interface TicTacToeRemote extends Remote {
    String makeMove(char player, int position) throws RemoteException;
    char[] getBoard() throws RemoteException;
    // ...
}
```

### Phase 2: Network Distribution

- [ ] Separate server process (port 1099)
- [ ] Network-based clients
- [ ] Support for different machines
- [ ] IP address configuration

### Phase 3: Advanced Features

- [ ] AI opponent (Minimax algorithm)
- [ ] Online matchmaking
- [ ] Game replay system
- [ ] Move history and undo
- [ ] Custom board sizes (4x4, 5x5)
- [ ] Tournament mode
- [ ] Leaderboards
- [ ] Chat system

### Phase 4: Production Readiness

- [ ] Security (SSL/TLS)
- [ ] User authentication
- [ ] Database persistence
- [ ] Load balancing
- [ ] Error recovery
- [ ] Logging framework
- [ ] Unit tests
- [ ] Documentation

## 📊 Performance Considerations

### Registry Caching Benefits

```
WITHOUT CACHE:
Request 1: Lookup → Server → Create service → 50ms
Request 2: Lookup → Server → Create service → 50ms
Request 3: Lookup → Server → Create service → 50ms
Total: 150ms

WITH CACHE:
Request 1: Lookup → Server → Create service → Cache → 50ms
Request 2: Lookup → Cache hit → 1ms
Request 3: Lookup → Cache hit → 1ms
Total: 52ms (65% faster!)
```

### Thread Safety

All service methods use `synchronized` keyword:
```java
public synchronized String makeMove(char player, int position)
```

**Prevents:**
- Race conditions
- Concurrent move conflicts
- Inconsistent game state

**Performance Impact:**
- Slight overhead per method call
- Ensures correctness
- Worth the tradeoff for reliability

## 🤝 Contributing

Contributions are welcome! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📝 License

This project is created for educational purposes as part of a Distributed Systems course.

## 👥 Authors

- **SOK KIMHENG** - Initial work - TP-I4 2025

## 🙏 Acknowledgments

- Distributed Systems Course - DS-I4 2025
- Java Swing Documentation
- RMI Architecture Patterns
- Design Patterns: Elements of Reusable Object-Oriented Software
---

**Note**: This is a prototype implementation designed to demonstrate RMI architectural patterns. For production use, implement true Java RMI with network communication and proper security measures.