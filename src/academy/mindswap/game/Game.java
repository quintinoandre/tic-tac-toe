package academy.mindswap.game;

import academy.mindswap.server.Server;

import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static academy.mindswap.game.GameMessages.*;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.*;

public class Game {
    private List<PlayerHandler> playerConnections;
    ExecutorService playerThreads;
    private final int MAX_PLAYERS = 2;
    Server server;

    public Game(Server server) {
        this.server = server;
        playerConnections = new CopyOnWriteArrayList<>();
        playerThreads = Executors.newFixedThreadPool(MAX_PLAYERS);
    }

    public synchronized void broadcast(String message, PlayerHandler notSending) {
        playerConnections.stream()
                .filter(ph -> !ph.equals(notSending))
                .forEach(ph -> ph.sendMessage(message));
    }

    public void acceptPlayer(Socket playerSocket) {
        playerThreads.submit(new PlayerHandler(playerSocket));
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
        private String nickname;//TODO: add method to initialize this variable


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
            this.sendMessage(String.format(WELCOME_MESSAGE, nickname));
            log(SUCCESS, String.format(WELCOME_MESSAGE, " (" + nickname + ")"), false);
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

        public void shutdownPlayerSocket() {
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
