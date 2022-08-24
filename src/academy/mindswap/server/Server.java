package academy.mindswap.server;


import academy.mindswap.game.Game;
import academy.mindswap.game.GameFactory;

import java.io.IOException;
import java.net.ServerSocket;

import static academy.mindswap.ConstantMessages.MAX_PLAYERS;
import static academy.mindswap.EnvironmentVariables.PORT;
import static academy.mindswap.server.ServerMessages.SERVER_START;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.ERROR;
import static academy.mindswap.utils.logger.LoggerType.SUCCESS;

/**
 * @link class that creates the server, listens to players and starts the game.
 */
public class Server {
    /**
     * Main method of the class Server that creates a new server instance and starts it.
     *
     * @param args
     */
    public static void main(String[] args) {
        Server server = new Server();

        server.startServer();
    }

    private Game game;
    private ServerSocket server;
    private boolean isServerDown;
    private int playersConnected;

    /**
     * Server's constructor method.
     * Initialize the variable isServerDown as false.
     */
    private Server() {
        this.isServerDown = false;
    }

    /**
     * Starts the server in the specified port that is referenced in the EnvironmentVariables Class.
     * This method accepts a player connection, creates a new game,
     * starts the game and allows it to continue until finished.
     */

    public void startServer() {
        log(SUCCESS, String.format(SERVER_START, PORT), true);

        try {
            game = GameFactory.create();

            server = new ServerSocket(PORT);

            while (!isServerDown) {
                if (!game.canGameStart()) {
                    if (playersConnected < MAX_PLAYERS) {
                        game.acceptPlayer(server.accept());

                        playersConnected++;
                    }

                    continue;
                }

                if (!game.isGameFinished()) {
                    if (!game.isGameStarted()) {
                        game.start();
                    }

                    if (game.isGameStarted() && !game.isGameFinished()) {
                        game.nextTurn();
                    }

                    continue;
                }

                shutdown();
            }
        } catch (IOException e) {
            log(ERROR, e.getMessage(), true);

            shutdown();
        }
    }

    /**
     * This method shuts down the server.
     */
    public void shutdown() {
        isServerDown = true;

        if (!server.isClosed()) {
            try {
                server.close();

                System.exit(0);
            } catch (IOException e) {
                log(ERROR, e.getMessage(), true);
            }
        }
    }
}

