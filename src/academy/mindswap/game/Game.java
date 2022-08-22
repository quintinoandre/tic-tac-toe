package academy.mindswap.game;

import academy.mindswap.server.Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
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
    }

    public void nextTurn() {
        if (isFirstTurn) {
            chooseStartingPlayer();

            isFirstTurn = false;
        }
    }

    public PlayerHandler chooseStartingPlayer() {
        PlayerHandler chosenOne = playerConnections.get(new Random().nextInt(2));

        chosenOne.symbol = X;

        chosenOne.sendMessage(YOUR_TURN);

        return chosenOne;
    }

    public synchronized void broadcast(String message) {
        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .forEach(ph -> ph.sendMessage(message));
    }

    public synchronized void broadcast(String message, PlayerHandler notSending) {
        playerConnections.stream()
                .filter(ph -> !ph.hasDisconnected)
                .filter(ph -> !ph.equals(notSending))
                .forEach(ph -> ph.sendMessage(message));
    }

    public void acceptPlayer(Socket playerSocket) {
        playerThreads.submit(new PlayerHandler(playerSocket));
    }

    public synchronized boolean playersHaveNickname() {
        for (PlayerHandler playerConnection : playerConnections) {
            if (playerConnection.nickname.equals("")) {
                return false;
            }
        }

        return true;
    }

    public boolean isAcceptingPlayers() {
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
    public class PlayerHandler implements Runnable {
        private Socket playerSocket;
        private BufferedReader bufferedReader;
        private BufferedWriter bufferedWriter;
        private String nickname = "";
        private boolean hasDisconnected;
        private char symbol;

        public PlayerHandler(Socket playerSocket) {
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

        public void sendMessage(String message) {
            try {
                bufferedWriter.write(message);
                bufferedWriter.newLine();
                bufferedWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public String getPlayerMessage() {
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

        public void shutdownPlayerSocket() {
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
