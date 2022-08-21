package academy.mindswap.game;

import academy.mindswap.server.Server;

public final class GameFactory {

    private GameFactory() {
    }

    public static Game create(Server server) {

        return new Game(server);

    }
}
