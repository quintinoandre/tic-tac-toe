package academy.mindswap.player;

import academy.mindswap.board.Board;

import java.io.*;
import java.net.Socket;

import static academy.mindswap.EnvironmentVariables.HOST;
import static academy.mindswap.EnvironmentVariables.PORT;
import static academy.mindswap.player.PlayerMessages.*;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.ERROR;
import static academy.mindswap.utils.logger.LoggerType.WARNING;

public class Player {
    private Socket playerSocket;
    private static Board playerBoard;
    private boolean isPlayerTurn;
    private final String GAME_STATE = "gamestate";
    private final String YOUR_TURN = "yourturn";

    /**
     * constructor method of the class Player
     * initializes player properties
     */
    public Player() {
        playerSocket = null;
        playerBoard = null;
        isPlayerTurn = false;
    }

    /**
     * main method of the class Player
     * creates a new player instance and call the startPlay method of the player
     */
    public static void main(String[] args) {
        Player player = new Player();

        playerBoard = new Board();

        playerBoard.createBoard();

        try {
            player.startPlay(HOST, PORT);
        } catch (IOException e) {
            log(ERROR, DEAD_SERVER, true);
        }
    }

    /**
     * starts the player in a specific port and host
     * create a new thread to send the player moves to the game
     *
     * @param host the string that represents the host of the server
     * @param port the integer that represents the port of the server
     * @throws IOException when it's not possible to connect with the server
     */
    private void startPlay(String host, int port) throws IOException {
        playerSocket = new Socket(host, port);

        new Thread(new SendMove()).start();

        receiveGameMassage();

        playerSocket.close();
    }

    /**
     * read the message from de game
     *
     * @throws IOException if client socket closed
     */
    private void receiveGameMassage() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));

        String line;

        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith(GAME_STATE)) {
                playerBoard.setGameState(extractGameState(line));
            }

            if (line.equals(YOUR_TURN)){
                isPlayerTurn = true;
            }

            System.out.println(line);
        }

        bufferedReader.close();

        closeSocket();
    }

    private char[][] extractGameState(String gameMessage) {
        String[] gamePositions = gameMessage.replace(GAME_STATE, "").split("");

        char[][] gameState = {
                {' ', ' ', ' '},
                {' ', ' ', ' '},
                {' ', ' ', ' '}};

        for (int i = 0; i < gameState.length; i++) {
            for (int j = 0; j < gameState[i].length; j++) {
                gameState[i][j] = gamePositions[j].toCharArray()[0];
            }
        }

        return gameState;
    }

    /**
     * closes player's socket after server closes his
     */
    private void closeSocket() {
        log(WARNING, SERVER_CLOSE_CONNECTION, true);

        try {
            playerSocket.close();
        } catch (IOException e) {
            log(WARNING, PLAYER_SOCKET_CLOSED, true);
        }

        System.exit(0);
    }

    /**
     * inner class SendMove that implements the interface Runnable
     */
    private class SendMove implements Runnable {
        /**
         * read the player move from the player board if it's his turn
         */
        @Override
        public void run() {
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));

                while (!playerSocket.isClosed()) {
                    if (isPlayerTurn) {
                        String playerMove = playerBoard.getPlayerMove();

                        if (!playerMove.equals("")) {
                            bufferedWriter.write(playerMove);

                            bufferedWriter.newLine();

                            bufferedWriter.flush();

                            playerBoard.setPlayerMove("");
                        }

                        isPlayerTurn = false;
                    }
                }

                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
