package academy.mindswap.game;

import academy.mindswap.server.Server;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static academy.mindswap.ConstantMessages.*;
import static academy.mindswap.game.GameMessages.*;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.*;

public class Game {
    private List<PlayerHandler> playerConnections;
    ExecutorService playerThreads;
    Server server;
    private volatile boolean isGameFinished;
    private boolean isGameStarted;
    private boolean isFirstTurn;
    PlayerHandler playerTurn;
    List<List> winningConditions = new ArrayList<>();

    char[][] gameState = {
            {' ', ' ', ' '},
            {' ', ' ', ' '},
            {' ', ' ', ' '}};

    public Game(Server server) {
        this.server = server;
        playerConnections = new ArrayList<>();
        playerThreads = Executors.newFixedThreadPool(MAX_PLAYERS);
    }

    public void start() {
        isGameStarted = true;
        broadcast(GAME_START_MESSAGE);
        isFirstTurn = true;
        createWinningConditions();
    }

    public void nextTurn() {
        String playerMove;

        StringBuilder gameStateToSend = new StringBuilder();

        if (isFirstTurn) {
            playerTurn = chooseStartingPlayer();
            isFirstTurn = false;
        } else {
            playerTurn = switchPlayer(playerTurn);
            playerTurn.sendMessage(YOUR_TURN);
        }

        playerMove = playerTurn.getPlayerMessage();
        playerTurn.playerMoves.add(playerMove);

        sendGameState(playerMove, gameStateToSend);
        System.out.println(checkWinner(playerTurn));
    }

    private void sendGameState(String playerMove, StringBuilder gameStateToSend) {
        updateGameState(playerTurn, playerMove);
        gameStateToSend.append(GAME_STATE);
        for (char[] row : gameState) {
            for (char c : row) {
                gameStateToSend.append(c);
            }

        }
        broadcast(gameStateToSend.toString());
    }

    private PlayerHandler switchPlayer(PlayerHandler previousPlayer) {

        Optional<PlayerHandler> nextPlayer = playerConnections.stream()
                .filter(ph -> ph != previousPlayer)
                .findFirst();

        return nextPlayer.get();

    }

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

    private PlayerHandler chooseStartingPlayer() {
        PlayerHandler firstPlayer = playerConnections.get(new Random().nextInt(2));

        firstPlayer.symbol = X;

        Optional<PlayerHandler> secondPlayer = playerConnections.stream()
                .filter(ph -> ph != firstPlayer)
                .findFirst();

        secondPlayer.get().symbol = O;

        firstPlayer.sendMessage(YOUR_TURN);

        return firstPlayer;
    }

    private String checkWinner(PlayerHandler playerTurn) {
        if (playerConnections.get(0).playerMoves.size() + playerConnections.get(1).playerMoves.size() == 9) {
            log(WARNING, DRAW, false);
            return DRAW;
        }

        for (List winningCondition : winningConditions) {
            if (playerTurn.playerMoves.containsAll(winningCondition)) {
                log(SUCCESS, String.format(WINNER, playerTurn.nickname), false);
                return String.format(WINNER, playerTurn.nickname);
            }
        }
        return "";
    }


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

    private synchronized void broadcast(String message) {
        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .forEach(ph -> ph.sendMessage(message));
    }

    private synchronized void broadcast(String message, PlayerHandler notSending) {
        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .filter(ph -> !ph.equals(notSending))
                .forEach(ph -> ph.sendMessage(message));
    }

    public void acceptPlayer(Socket playerSocket) {
        playerThreads.submit(new PlayerHandler(playerSocket));
    }

    private synchronized boolean playersHaveNickname() {
        for (PlayerHandler playerConnection : playerConnections) {
            if (playerConnection.nickname.equals("")) {
                return false;
            }
        }

        return true;
    }

    private boolean isAcceptingPlayers() {
        return playerConnections.size() < MAX_PLAYERS && !isGameStarted;
    }

    public boolean canGameStart() {
        return !isAcceptingPlayers()
                && playerConnections.stream().filter(ph -> !ph.hasDisconnected)
                .noneMatch(ph -> ph.nickname.equals(""));
    }

    public void finishGame() {
        broadcast(GAME_OVER);

        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .forEach(PlayerHandler::shutdownPlayerSocket);

        isGameFinished = true;
    }

    public boolean isGameFinished() {
        return isGameFinished;
    }

    public boolean isGameStarted() {
        return isGameStarted;
    }

    /* private void removeFromServerList() {
    server.removeGameFromList(this);}
     */

    /**
     * @link inner class handling playerSockets
     */
    private class PlayerHandler implements Runnable {
        private Socket playerSocket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String nickname = "";
        private boolean hasDisconnected;
        private char symbol;
        private List<String> playerMoves = new ArrayList<>();

        private PlayerHandler(Socket playerSocket) {
            this.playerSocket = playerSocket;

            try {
                bufferedReader = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));
                bufferedWriter = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));
            } catch (IOException e) {
                shutdownPlayerSocket();
            }
        }

        @Override
        public void run() {
            playerConnections.add(this);

            sendMessage(ENTER_NICKNAME);

            nickname = getPlayerMessage();

            this.sendMessage(String.format(WELCOME_MESSAGE, nickname));

            while (true) {
                if (playersHaveNickname()) {
                    broadcast(String.format(NEW_PLAYER, nickname), this);

                    break;
                }
            }

            log(SUCCESS, String.format(WELCOME_MESSAGE, " (" + nickname + ")"), false);

            while (!isGameFinished) {

                if (Thread.interrupted()) {
                    return;
                }
            }

            shutdownPlayerSocket();
        }

        private void sendMessage(String message) {
            try {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private String getPlayerMessage() {
            String playerMessage = null;

            try {
                playerMessage = bufferedReader.readLine();
            } catch (IOException | NullPointerException e) {
                shutdownPlayerSocket();
            } finally {
                if (playerMessage == null) {
                    shutdownPlayerSocket();
                }
            }

            return playerMessage;
        }

        private void shutdownPlayerSocket() {
            hasDisconnected = true;

            if (!playerSocket.isClosed()) {
                try {
                    bufferedWriter.close();
                    bufferedReader.close();
                    playerSocket.close();
                } catch (IOException e) {
                    log(ERROR, PLAYER_SOCKET_CLOSED, true);
                } finally {
                    //areStillPlayersPlaying();

                    log(WARNING, PLAYER_LEFT, false);

                    broadcast(PLAYER_LEFT, this);
                }
            }

        }

    }
}
