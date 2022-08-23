package academy.mindswap.board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import static academy.mindswap.game.GameMessages.DRAW;

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
    private char[][] gameState = {
            {' ', ' ', ' '},
            {' ', ' ', ' '},
            {' ', ' ', ' '}
    };

    public void createBoard() {
        boardFrame();

        gameIntro();

        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        boardTitle();

        buttonsGrid();

        disableButtons();

        Thread updateGameState = new Thread(() -> {
            while (true) {
                buttons.get(0).setText(Character.toString(gameState[0][0]));
                buttons.get(1).setText(Character.toString(gameState[0][1]));
                buttons.get(2).setText(Character.toString(gameState[0][2]));
                buttons.get(3).setText(Character.toString(gameState[1][0]));
                buttons.get(4).setText(Character.toString(gameState[1][1]));
                buttons.get(5).setText(Character.toString(gameState[1][2]));
                buttons.get(6).setText(Character.toString(gameState[2][0]));
                buttons.get(7).setText(Character.toString(gameState[2][1]));
                buttons.get(8).setText(Character.toString(gameState[2][2]));

                disableClickedButton();
            }
        });

        updateGameState.start();
    }

    private void disableClickedButton() {
        for (JButton button : buttons) {
            if (!button.getText().equals(" ")) {
                button.setEnabled(false);
            }

        }
    }

    private void boardFrame() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(BOARD_WIDTH, BOARD_HEIGHT);
        frame.getContentPane().setBackground(new Color(219, 219, 219));
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
    }

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

    private void boardTitle() {
        textField.setBackground(Color.BLACK);
        textField.setForeground(new Color(219, 219, 219));
        textField.setFont(new Font("Monospaced", Font.BOLD, 40));
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.setText("TIC TAC TOE CHALLENGE");
        textField.setOpaque(true);

        boardTitle.setLayout(new BorderLayout());
        boardTitle.setBounds(0, 0, TITLE_WIDTH, TITLE_HEIGHT);
        boardTitle.add(textField);

        frame.add(boardTitle, BorderLayout.NORTH);
    }

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

    @Override
    public void actionPerformed(ActionEvent e) {
        Object clickedButton = e.getSource();

        playerMove = Integer.toString(buttons.indexOf(clickedButton) + 1);
    }

    public void enableButtons() {
        for (JButton button : buttons) {
            if (button.getText().equals(" ")) {
                button.setEnabled(true);
            }
        }
    }

    public void disableButtons() {
        for (JButton button : buttons) {
            button.setEnabled(false);
        }
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

    public void setGameState(char[][] gameState) {
        this.gameState = gameState;
    }

    private void printWinnerPositions(int a, int b, int c) {
        buttons.get(a).setBackground(Color.GREEN);
        buttons.get(b).setBackground(Color.GREEN);
        buttons.get(c).setBackground(Color.GREEN);
    }

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


