package academy.mindswap.game;

public final class GameFactory {

    private GameFactory() {
    }

    public static Game create() {
        return new Game();
    }
}
