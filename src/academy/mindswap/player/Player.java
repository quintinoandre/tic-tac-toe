package academy.mindswap.player;

import academy.mindswap.board.Board;

import java.io.*;
import java.net.Socket;

import static academy.mindswap.ConstantMessages.GAME_STATE;
import static academy.mindswap.ConstantMessages.YOUR_TURN;
import static academy.mindswap.EnvironmentVariables.HOST;
import static academy.mindswap.EnvironmentVariables.PORT;
import static academy.mindswap.player.PlayerMessages.*;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.ERROR;
import static academy.mindswap.utils.logger.LoggerType.WARNING;

public class Player {
    private Socket playerSocket;
    private Board playerBoard;
    private boolean isPlayerTurn;

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

                continue;
            }

            if (line.equals(YOUR_TURN)) {
                isPlayerTurn = true;

                continue;
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

        updateGameState(gamePositions, gameState);
        
        return gameState;
    }

    private void updateGameState(String[] gamePositions, char[][] gameState) {

        gameState[0][0] = gamePositions[0].toCharArray()[0];
        gameState[0][1] = gamePositions[1].toCharArray()[0];
        gameState[0][2] = gamePositions[2].toCharArray()[0];
        gameState[1][0] = gamePositions[3].toCharArray()[0];
        gameState[1][1] = gamePositions[4].toCharArray()[0];
        gameState[1][2] = gamePositions[5].toCharArray()[0];
        gameState[2][0] = gamePositions[6].toCharArray()[0];
        gameState[2][1] = gamePositions[7].toCharArray()[0];
        gameState[2][2] = gamePositions[8].toCharArray()[0];

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
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(playerSocket.getOutputStream()));

                String playerMessage = bufferedReader.readLine();

                while (!playerMessage.matches("[a-zA-Z]+")) {
                    log(ERROR, INCORRECT_NICKNAME, true);

                    playerMessage = bufferedReader.readLine();
                }

                bufferedWriter.write(playerMessage);

                bufferedWriter.newLine();

                bufferedWriter.flush();

                // TODO: don't allow to open the board without two players on the game
                playerBoard = new Board();

                playerBoard.createBoard();

                bufferedReader.close();

                while (!playerSocket.isClosed()) {
                    if (isPlayerTurn) {
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
                }

                bufferedWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
