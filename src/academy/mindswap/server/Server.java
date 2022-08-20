package academy.mindswap.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {

    public static void main(String[] args) {
        Server server = new Server();
        server.startServer();
    }

    // private List<Game> gamesList; //TODO : add a gameslist

    private ServerSocket server; //is the server socket accepting playerSockets
    private boolean done; //this allows the server to keep on receiving clients until is true

    private ExecutorService gamesThreads; // we need to create a threadpool for different games that are going to connect

    public Server() {
        //gamesList = new CopyOnWriteArrayList<>();
        this.done = false;
    }


    public void startServer() {
        try {
            server = new ServerSocket(8080);
            gamesThreads = Executors.newCachedThreadPool(); //cachedthreadPools create the necessary threads, while using the already existing ones
            while (!done) { //!done is true, so while true. When we change done to true, !done turn out to be false and stops the while loop

                /*if(!isGameCreated()){
                    createGame();
                }
                if (getOpenGame().isPresent()) {
                  getOpenGame.acceptPlayer(Socket gameSocket = server.accept());
                }*/
                //game.acceptPlayer(Socket playerSocket = server.accept());
                Socket playerSocket = server.accept();

            }
        } catch (IOException e) { //catch the exception here instead of throwing it to the main
            shutdown();
        }
    }


    // private boolean isGameCreated() {}


    /**
     * @link this method creates a game, adds the server to it and
     * puts it on a thread with the gameThreads.execute method
     */

    /*private void createGame() {
        Game game = GameFactory.create(this);
        //gamesList.add(game);
        gamesThreads.execute(game);
    }
     */

    /**
     * this method shuts down the server and the individual playerSockets connected to the server
     */
    public void shutdown() {
        done = true;
        if (!server.isClosed()) {
            try {
                server.close();

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

