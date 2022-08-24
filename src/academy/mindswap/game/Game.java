package academy.mindswap.game;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static academy.mindswap.ConstantMessages.*;
import static academy.mindswap.game.GameMessages.*;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.ERROR;
import static academy.mindswap.utils.logger.LoggerType.SUCCESS;

/**
 * Class representing the game logic.
 * It manages the information between players.
 */
public class Game {
    private List<PlayerHandler> playerConnections;
    private ExecutorService playerThreads;
    private volatile boolean isGameFinished;
    private boolean isGameStarted;
    private boolean isFirstTurn;
    private PlayerHandler playerTurn;
    private List<List> winningConditions = new ArrayList<>();

    private String[][] gameState = {
            {" ", " ", " "},
            {" ", " ", " "},
            {" ", " ", " "}};

    /**
     * Game constructor that initializes playerconnections arraylist and fixedthreadpool.
     */
    public Game() {
        playerConnections = new CopyOnWriteArrayList<>();

        playerThreads = Executors.newFixedThreadPool(MAX_PLAYERS);
    }

    /**
     * Broadcasts the information for the players to initializes their own boards.
     * Indicates players that the game begins.
     * Initializes the variables isGameStarted and isFirstTurn.
     * Creates the winning conditions (to be checked later on).
     */
    public void start() {
        broadcast(CREATE_BOARD);

        isGameStarted = true;

        broadcast(GAME_START_MESSAGE);

        isFirstTurn = true;

        createWinningConditions();
    }

    /**
     * Chooses the first playing player.
     * Starts the mechanism to change player each turn.
     * Broadcasts the name of the player that will play in each turn.
     * Indicates the player that it's his/her turn.
     * Receives and saves each play from player.
     * Updates internal game state and sends it to the players via broadcast.
     * Checks if there is any winner.
     */
    public void nextTurn() throws IOException {
        String playerMove;

        StringBuilder gameStateToSend = new StringBuilder();

        if (isFirstTurn) {
            playerTurn = chooseStartingPlayer();

            isFirstTurn = false;
        } else {
            playerTurn = switchPlayer(playerTurn);

            broadcast(PLAYER_TURN.concat(playerTurn.nickname.toUpperCase()).concat(TURN));

            playerTurn.sendMessage(YOUR_TURN);
        }

        if (playerTurn.playerSocket.getInputStream().read() != -1) {
            playerMove = playerTurn.getPlayerMessage();

            playerTurn.playerMoves.add(playerMove);

            sendGameState(playerMove, gameStateToSend);

            checkWinner(playerTurn);
        }
    }

    /**
     * Sends the game state to the players after updating the internal one.
     *
     * @param playerMove      String containing the player move.
     * @param gameStateToSend String containing the updated game state to send.
     */
    private void sendGameState(String playerMove, StringBuilder gameStateToSend) {
        updateGameState(playerTurn, playerMove);

        gameStateToSend.append(GAME_STATE);

        for (String[] row : gameState) {
            for (String s : row) {
                gameStateToSend.append(s);
            }
        }

        broadcast(gameStateToSend.toString());
    }

    /**
     * Decides the next player by knowing the previous one.
     *
     * @param previousPlayer
     * @return
     */
    private PlayerHandler switchPlayer(PlayerHandler previousPlayer) {
        Optional<PlayerHandler> nextPlayer = playerConnections.stream()
                .filter(ph -> ph != previousPlayer)
                .findFirst();

        if (nextPlayer.isPresent()) {
            return nextPlayer.get();
        }

        log(ERROR, PLAYER_LEFT, true);

        finishGame(0);

        return null;
    }

    /**
     * Updates the internal game state, to send to the players.
     *
     * @param playerTurn Player that played.
     * @param playerMove The move that was played.
     */
    private void updateGameState(PlayerHandler playerTurn, String playerMove) {
        switch (playerMove) {
            case "1" -> gameState[0][0] = playerTurn.symbol;
            case "2" -> gameState[0][1] = playerTurn.symbol;
            case "3" -> gameState[0][2] = playerTurn.symbol;
            case "4" -> gameState[1][0] = playerTurn.symbol;
            case "5" -> gameState[1][1] = playerTurn.symbol;
            case "6" -> gameState[1][2] = playerTurn.symbol;
            case "7" -> gameState[2][0] = playerTurn.symbol;
            case "8" -> gameState[2][1] = playerTurn.symbol;
            case "9" -> gameState[2][2] = playerTurn.symbol;
        }
    }

    /**
     * Chooses the starting player randomly.
     * It assigns the first player to the starting symbol and second player to the other one.
     * Broadcasts the name of the player that will start.
     * Indicates the first player that it's his/her turn.
     * Terminates the game if the second player disconnects.
     *
     * @return first player.
     */
    private PlayerHandler chooseStartingPlayer() {
        PlayerHandler firstPlayer = playerConnections.get(new Random().nextInt(2));

        firstPlayer.symbol = X;

        Optional<PlayerHandler> secondPlayer = playerConnections.stream()
                .filter(ph -> ph != firstPlayer)
                .findFirst();

        if (secondPlayer.isPresent()) {
            secondPlayer.get().symbol = O;

            broadcast(PLAYER_TURN.concat(firstPlayer.nickname.toUpperCase()).concat(TURN));

            firstPlayer.sendMessage(YOUR_TURN);

            return firstPlayer;
        }

        log(ERROR, PLAYER_LEFT, true);

        finishGame(0);

        return null;
    }

    /**
     * Checks if the player has a winning condition in his playerMoves.
     * If all positions of the board are filled, it broadcasts a message indicating a DRAW.
     *
     * @param playerTurn Player that played.
     */
    private void checkWinner(PlayerHandler playerTurn) {
        for (List winningCondition : winningConditions) {
            if (playerTurn.playerMoves.containsAll(winningCondition)) {
                log(SUCCESS, String.format(WINNER, playerTurn.nickname), false);

                StringBuilder winnerPositions = new StringBuilder();

                winningCondition.forEach(winnerPositions::append);

                broadcast(GAME_OVER.concat(String.format(WINNER, playerTurn.nickname)).concat(winnerPositions.toString()));

                finishGame(4000);
            }
        }

        if (playerConnections.get(0).playerMoves.size() + playerConnections.get(1).playerMoves.size() == 9) {
            log(SUCCESS, DRAW, false);

            broadcast(GAME_OVER.concat(DRAW));

            finishGame(4000);
        }
    }

    /**
     * Creates winning conditions and saves them in the winningConditions arrayList.
     */
    private void createWinningConditions() {
        List<String> topRow = Arrays.asList("1", "2", "3");
        List<String> midRow = Arrays.asList("4", "5", "6");
        List<String> bottomRow = Arrays.asList("7", "8", "9");

        List<String> leftColumn = Arrays.asList("1", "4", "7");
        List<String> midColumn = Arrays.asList("2", "5", "8");
        List<String> bottomColumn = Arrays.asList("3", "6", "9");

        List<String> diagonal1 = Arrays.asList("1", "5", "9");
        List<String> diagonal2 = Arrays.asList("7", "5", "3");

        winningConditions.add(topRow);
        winningConditions.add(midRow);
        winningConditions.add(bottomRow);
        winningConditions.add(leftColumn);
        winningConditions.add(midColumn);
        winningConditions.add(bottomColumn);
        winningConditions.add(diagonal1);
        winningConditions.add(diagonal2);
    }

    /**
     * Broadcasts the message to all players.
     *
     * @param message String to send.
     */
    private synchronized void broadcast(String message) {
        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .forEach(ph -> ph.sendMessage(message));
    }

    /**
     * Broadcasts the message to all players, except a selected one.
     *
     * @param message    String to send.
     * @param notSending Player that does not receive the message.
     */
    private synchronized void broadcast(String message, PlayerHandler notSending) {
        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .filter(ph -> !ph.equals(notSending))
                .forEach(ph -> ph.sendMessage(message));
    }

    /**
     * Adds to the threadpool a new player handler.
     *
     * @param playerSocket connection between server and player.
     */
    public void acceptPlayer(Socket playerSocket) {
        playerThreads.submit(new PlayerHandler(playerSocket));
    }

    /**
     * Checks if the game can accept more players and
     *
     * @return true or false.
     */
    private boolean isAcceptingPlayers() {
        return playerConnections.size() < MAX_PLAYERS && !isGameStarted;
    }

    /**
     * Check if all necessary players are connected and verifies if each player has nicknames.
     *
     * @return true or false.
     */
    public boolean canGameStart() {
        return !isAcceptingPlayers()
                && playerConnections.stream().filter(ph -> !ph.hasDisconnected)
                .noneMatch(ph -> ph.nickname.equals(""));
    }

    /**
     * Close all player connections and finishes the game.
     *
     * @param delay
     */
    public void finishGame(int delay) {
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            log(ERROR, e.getMessage(), true);

            System.exit(0);
        }

        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .forEach(PlayerHandler::shutdownPlayerSocket);

        isGameFinished = true;
    }

    /**
     * Gives the information if the game is finished.
     *
     * @return true or false.
     */
    public boolean isGameFinished() {
        return isGameFinished;
    }

    /**
     * Gives the information if the game is finished.
     *
     * @return true or false.
     */
    public boolean isGameStarted() {
        return isGameStarted;
    }

    /**
     * @link Inner class handling playerSockets
     */
    private class PlayerHandler implements Runnable {
        private Socket playerSocket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String nickname = "";
        private boolean hasDisconnected;
        private String symbol;
        private List<String> playerMoves = new ArrayList<>();

        /**
         * Constructor method that accepts the playerSocket, initializes bufferedReader and bufferedWriter.
         *
         * @param playerSocket
         */
        private PlayerHandler(Socket playerSocket) {
            this.playerSocket = playerSocket;

            try {
                bufferedReader = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));

                bufferedWriter = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));
            } catch (IOException e) {
                log(ERROR, e.getMessage(), true);

                finishGame(0);
            }
        }

        /**
         * PlayerHandler adds himself to the PlayerHandler list.
         * Asks the player for a nickname.
         * Sends to the player a welcome message after receiving a correct nickname
         * Informs the other player of his arrival.
         * The while loop ensures that the thread remains alive.
         */
        @Override
        public void run() {
            playerConnections.add(this);

            sendMessage(ENTER_NICKNAME);

            nickname = getPlayerMessage();

            this.sendMessage(String.format(WELCOME_MESSAGE, nickname));

            log(SUCCESS, String.format(WELCOME_MESSAGE, " (" + nickname + ")"), false);

            while (true) {
                if (playersHaveNickname()) {
                    broadcast(String.format(NEW_PLAYER, nickname), this);

                    break;
                }
            }

            while (!isGameFinished) {
                try {
                    if (this.playerSocket.getInputStream().read() == -1) {
                        this.hasDisconnected = true;

                        log(ERROR, PLAYER_LEFT, false);

                        broadcast(PLAYER_LEFT, this);

                        finishGame(0);
                    }
                } catch (IOException e) {
                    log(ERROR, e.getMessage(), true);
                }

                if (Thread.interrupted()) {
                    shutdownPlayerSocket();
                }
            }
        }

        /**
         * Send message to the player.
         *
         * @param message
         */
        private void sendMessage(String message) {
            try {
                bufferedWriter.write(message);

                bufferedWriter.newLine();

                bufferedWriter.flush();
            } catch (IOException e) {
                log(ERROR, e.getMessage(), true);
            }
        }

        /**
         * Receives message from the player.
         *
         * @return
         */
        private String getPlayerMessage() {
            String playerMessage = null;

            try {
                playerMessage = bufferedReader.readLine();
            } catch (IOException | NullPointerException e) {
                log(ERROR, e.getMessage(), true);
            } finally {
                if (playerMessage == null) {
                    finishGame(0);
                }
            }

            return playerMessage;
        }

        /**
         * Check if all players have nicknames.
         *
         * @return
         */
        private boolean playersHaveNickname() {
            for (PlayerHandler playerConnection : playerConnections) {
                if (playerConnection.nickname.equals("")) {
                    return false;
                }
            }

            return true;
        }

        /**
         * Closes bufferedWriter/reader stream and player socket.
         */
        private void shutdownPlayerSocket() {
            hasDisconnected = true;

            if (!playerSocket.isClosed()) {
                try {
                    bufferedWriter.close();

                    bufferedReader.close();

                    playerSocket.close();

                    log(ERROR, PLAYER_LEFT, false);
                } catch (IOException e) {
                    log(ERROR, PLAYER_SOCKET_CLOSED, true);
                }
            }
        }
    }
}
