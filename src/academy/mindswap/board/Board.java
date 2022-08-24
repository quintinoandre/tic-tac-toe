package academy.mindswap.board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import static academy.mindswap.ConstantMessages.GAME_TITLE;
import static academy.mindswap.game.GameMessages.DRAW;
import static academy.mindswap.utils.logger.Logger.log;
import static academy.mindswap.utils.logger.LoggerType.ERROR;

/**
 * Class representing the board and implements the ActionListener interface.
 */
public class Board implements ActionListener {
    JFrame frame = new JFrame();
    JPanel boardTitle = new JPanel();
    JPanel boardButtons = new JPanel();
    JLabel textField = new JLabel();
    ArrayList<JButton> buttons = new ArrayList<>(9);
    private final int BOARD_HEIGHT = 800;
    private final int BOARD_WIDTH = 800;
    private final int TITLE_HEIGHT = 200;
    private final int TITLE_WIDTH = 800;
    private String playerMove = "";
    private String[][] gameState = {
            {" ", " ", " "},
            {" ", " ", " "},
            {" ", " ", " "}};

    /**
     * Call all methods to initialize the game board.
     * Starts with the board frame.
     * Opens a screen splash before the board display.
     * Both boards start with disabled buttons (until a player is chosen).
     * Instantiates a new thread that stays constantly updating the game state and disables all clicked buttons.
     */
    public void createBoard() {
        boardFrame();

        gameIntro();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            log(ERROR, e.getMessage(), false);
        }

        boardTitle();

        buttonsGrid();

        disableButtons();

        Thread updateGameState = new Thread(() -> {
            while (true) {
                buttons.get(0).setText(gameState[0][0]);
                buttons.get(1).setText(gameState[0][1]);
                buttons.get(2).setText(gameState[0][2]);
                buttons.get(3).setText(gameState[1][0]);
                buttons.get(4).setText(gameState[1][1]);
                buttons.get(5).setText(gameState[1][2]);
                buttons.get(6).setText(gameState[2][0]);
                buttons.get(7).setText(gameState[2][1]);
                buttons.get(8).setText(gameState[2][2]);

                disableClickedButtons();
            }
        });

        updateGameState.start();
    }

    /**
     * Disables all the clicked buttons.
     */
    private void disableClickedButtons() {
        for (JButton button : buttons) {
            if (!button.getText().equals(" ")) {
                button.setEnabled(false);
            }

        }
    }

    /**
     * Exit on close option.
     * Sets the size of the board and its features.
     */
    private void boardFrame() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(BOARD_WIDTH, BOARD_HEIGHT);
        frame.getContentPane().setBackground(new Color(219, 219, 219));
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
    }

    /**
     * Creates screen splash and its features.
     */
    private void gameIntro() {
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.setText(
                "<html><img src=\""
                        + Board.class.getResource("screensplash.png")
                        + "\"></html>"
        );

        boardTitle.setLayout(new BorderLayout());
        boardTitle.setBounds(0, 0, BOARD_WIDTH, BOARD_HEIGHT);
        boardTitle.add(textField);

        frame.add(boardTitle, BorderLayout.CENTER);
    }

    /**
     * Creates board title bar and his styles.
     */
    private void boardTitle() {
        textField.setBackground(Color.BLACK);
        textField.setForeground(new Color(219, 219, 219));
        textField.setFont(new Font("Monospaced", Font.BOLD, 40));
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.setText(GAME_TITLE);
        textField.setOpaque(true);

        boardTitle.setLayout(new BorderLayout());
        boardTitle.setBounds(0, 0, TITLE_WIDTH, TITLE_HEIGHT);
        boardTitle.add(textField);

        frame.add(boardTitle, BorderLayout.NORTH);
    }

    /**
     * Creates a 3x3 grid.
     * Adds the grid to the frame.
     * Fills the grid with the buttons.
     */
    private void buttonsGrid() {
        boardButtons.setLayout(new GridLayout(3, 3));
        boardButtons.setBackground(new Color(150, 150, 150));

        frame.add(boardButtons);

        for (int i = 0; i < 9; i++) {
            buttons.add(new JButton());

            boardButtons.add(buttons.get(i));

            buttons.get(i).setOpaque(true);
            buttons.get(i).setFont(new Font("Monospaced", Font.BOLD, 50));
            buttons.get(i).setFocusable(false);
            buttons.get(i).addActionListener(this);
        }
    }

    /**
     * Verifies which button was clicked, gets its index plus one
     * and saves that value as a string in the playerMove variable.
     *
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        Object clickedButton = e.getSource();

        playerMove = Integer.toString(buttons.indexOf(clickedButton) + 1);
    }

    /**
     * Enables all buttons that weren't clicked.
     */
    public void enableButtons() {
        for (JButton button : buttons) {
            if (button.getText().equals(" ")) {
                button.setEnabled(true);
            }
        }
    }

    /**
     * Disables all buttons.
     */
    public void disableButtons() {
        for (JButton button : buttons) {
            button.setEnabled(false);
        }
    }

    /**
     * When the winning conditions are fulfilled the winner positions turn green.
     *
     * @param a first winner position.
     * @param b second winner position.
     * @param c third winner position.
     */
    private void printWinnerPositions(int a, int b, int c) {
        buttons.get(a).setBackground(Color.GREEN);
        buttons.get(b).setBackground(Color.GREEN);
        buttons.get(c).setBackground(Color.GREEN);
    }

    public String getPlayerMove() {
        return playerMove;
    }

    public void setPlayerTurn(String playerTurn) {
        textField.setText(playerTurn);
    }

    public void setPlayerMove(String playerMove) {
        this.playerMove = playerMove;
    }

    public void setGameState(String[][] gameState) {
        this.gameState = gameState;
    }

    /**
     * When the game ends, disable all buttons.
     * If it's not a draw, extracts the winner positions from a message sent by the game.
     * Afterwards, calls the printWinnerPositions method to assign its winning colors.
     * It prints the winner name in the title bar.
     * In case of a draw, turns all buttons yellow and, also, prints DRAW to the title bar.
     *
     * @param gameResult
     */
    public void setGameResult(String gameResult) {
        disableButtons();

        if (!gameResult.equals(DRAW)) {
            String[] winnerPositions = gameResult.replaceAll("\\D", "").split("");

            int[] winnerButtonsPosition = {Integer.parseInt(winnerPositions[0]) - 1,
                    Integer.parseInt(winnerPositions[1]) - 1,
                    Integer.parseInt(winnerPositions[2]) - 1};

            printWinnerPositions(winnerButtonsPosition[0], winnerButtonsPosition[1], winnerButtonsPosition[2]);

            textField.setText(gameResult.replaceAll("\\d", ""));

            return;
        }

        buttons.forEach(button -> button.setBackground(Color.YELLOW));

        textField.setText(gameResult);
    }
}


