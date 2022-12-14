package academy.mindswap.player;

public final class PlayerMessages {
    private PlayerMessages() {
    }

    /**
     * Messages sent from player.
     */

    public static final String DEAD_SERVER = "It's not possible to connect to server";
    public static final String SERVER_CLOSE_CONNECTION = "Server closed the connection";
    public static final String PLAYER_SOCKET_CLOSED = "Player socket is already closed";
    public static final String INCORRECT_NICKNAME = "Incorrect nickname. Please enter a nickname with letters only:";
}
