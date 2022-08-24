package academy.mindswap.player;

import academy.mindswap.board.Board;

import java.io.*;
import java.net.Socket;

import static academy.mindswap.ConstantMessages.*;
import static academy.mindswap.EnvironmentVariables.HOST;
import static academy.mindswap.EnvironmentVariables.PORT;
import static academy.mindswap.player.PlayerMessages.*;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.ERROR;

/**
 * Class that connects to server, sends and receives messages from game/server,
 * creates a new board and gives instructions to it.
 */
public class Player {
    private Socket playerSocket;
    private Board playerBoard;
    private boolean isPlayerTurn;

    /**
     * Player's constructor method.
     * Initializes player properties.
     */
    public Player() {
        playerSocket = null;

        playerBoard = null;

        isPlayerTurn = false;
    }

    /**
     * Main method of the class Player
     * Creates a new player instance and call the startPlay method of the player
     */
    public static void main(String[] args) {
        Player player = new Player();

        try {
            player.startPlay(HOST, PORT);
        } catch (IOException e) {
            log(ERROR, DEAD_SERVER, true);
        }
    }

    /**
     * Starts the player in the specified port and host that are referenced in the EnvironmentVariables Class.
     * Create a new thread to send the player moves to the game
     * Initializes receiveGameMessage method that uses the main thread to keep listening the game/server information.
     *
     * @param host the string that represents the host of the server
     * @param port the integer that represents the port of the server
     * @throws IOException to main, when it's not possible to connect with the server
     */
    private void startPlay(String host, int port) throws IOException {
        playerSocket = new Socket(host, port);

        new Thread(new SendInformation()).start();

        receiveGameMessage();
    }

    /**
     * Read the information from the game/server.
     * Allows player to know how to interact with the game and board.
     * Creates board.
     * Updates the state of the game.
     * Receive the instruction to be able to play and the end game result.
     *
     * @throws IOException if client socket closed
     */
    private void receiveGameMessage() throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(playerSocket.getInputStream()));

        String line;

        while ((line = bufferedReader.readLine()) != null) {
            if (line.equals(CREATE_BOARD)) {
                playerBoard = new Board();

                playerBoard.createBoard();

                continue;
            }

            if (line.startsWith(GAME_STATE)) {
                playerBoard.setGameState(extractGameState(line));

                continue;
            }

            if (line.startsWith(YOUR_TURN)) {
                isPlayerTurn = true;

                continue;
            }

            if (line.startsWith(GAME_OVER)) {
                playerBoard.setGameResult(line.replace(GAME_OVER, ""));

                continue;
            }


            if (line.startsWith(PLAYER_TURN)) {
                playerBoard.setPlayerTurn(line.replace(PLAYER_TURN, ""));

                continue;
            }

            System.out.println(line);
        }

        bufferedReader.close();

        closeSocket();
    }

    /**
     * Extract the game state information from the game class received string.
     *
     * @param gameMessage the game class received string.
     * @return a two-dimensional array of strings.
     */
    private String[][] extractGameState(String gameMessage) {
        String[] gamePositions = gameMessage.replace(GAME_STATE, "").split("");

        String[][] gameState = {
                {" ", " ", " "},
                {" ", " ", " "},
                {" ", " ", " "}};

        updateGameState(gamePositions, gameState);

        return gameState;
    }

    /**
     * Updates the game state through the game positions that comes from game.
     *
     * @param gamePositions String array of the game positions
     * @param gameState     two-dimensional string array that will be updated.
     */
    private void updateGameState(String[] gamePositions, String[][] gameState) {
        gameState[0][0] = gamePositions[0];
        gameState[0][1] = gamePositions[1];
        gameState[0][2] = gamePositions[2];
        gameState[1][0] = gamePositions[3];
        gameState[1][1] = gamePositions[4];
        gameState[1][2] = gamePositions[5];
        gameState[2][0] = gamePositions[6];
        gameState[2][1] = gamePositions[7];
        gameState[2][2] = gamePositions[8];
    }

    /**
     * Closes the player socket and terminates the process.
     */
    private void closeSocket() {
        log(ERROR, SERVER_CLOSE_CONNECTION, true);

        try {
            playerSocket.close();
        } catch (IOException e) {
            log(ERROR, PLAYER_SOCKET_CLOSED, true);
        }

        System.exit(0);
    }

    /**
     * Inner class SendMove that implements the interface Runnable
     */
    private class SendInformation implements Runnable {
        /**
         * Sends the asked (by the game/server) nickname and validates (before sending it) if it's correctly written.
         * Reads the player move from the player board (each turn) and sends it to the game/server.
         */

        @Override
        public void run() {
            try {
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));

                sendNickname(bufferedWriter);

                while (!playerSocket.isClosed()) {
                    if (isPlayerTurn) {
                        sendMove(bufferedWriter);
                    }
                }

                bufferedWriter.close();
            } catch (IOException e) {
                log(ERROR, e.getMessage(), true);
            }
        }

        /**
         * Sends the player move to the game/server.
         *
         * @param bufferedWriter
         * @throws IOException if occurs an error with bufferedWriter.
         */
        private void sendMove(BufferedWriter bufferedWriter) throws IOException {
            playerBoard.enableButtons();

            String playerMove = playerBoard.getPlayerMove();

            if (!playerMove.equals("")) {
                bufferedWriter.write(playerMove);

                bufferedWriter.newLine();

                bufferedWriter.flush();

                playerBoard.setPlayerMove("");

                playerBoard.disableButtons();

                isPlayerTurn = false;
            }
        }

        /**
         * Sends the player nickname to the game/server
         *
         * @param bufferedWriter
         * @throws IOException if occurs an error with bufferedWriter/reader.
         */
        private void sendNickname(BufferedWriter bufferedWriter) throws IOException {
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

            String playerMessage = bufferedReader.readLine();

            while (!playerMessage.matches("[A-Za-zÀ-ü]+")) {
                log(ERROR, INCORRECT_NICKNAME, true);

                playerMessage = bufferedReader.readLine();
            }

            bufferedWriter.write(playerMessage);

            bufferedWriter.newLine();

            bufferedWriter.flush();

            bufferedReader.close();
        }
    }
}
