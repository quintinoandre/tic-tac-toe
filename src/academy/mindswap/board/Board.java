package academy.mindswap.board;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
    private String playerMove;
    private char[][] gameState;

    public void createBoard() {
        //gameIntro();
        boardFrame();
        boardTitle();
        buttonsGrid();
    }

    private void boardFrame() {
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(BOARD_WIDTH, BOARD_HEIGHT);
        frame.getContentPane().setBackground(new Color(219, 219, 219));
        frame.setLayout(new BorderLayout());
        frame.setVisible(true);
    }

    /*private void gameIntro(){
        textField.setBackground(Color.BLACK);
        textField.setForeground(Color.BLACK);
        textField.setFont(new Font("Monospaced", Font.BOLD, 40));
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.setText("TIC TAC TOE on steroids");
        textField.setOpaque(true);

        try{
            Thread.sleep(3000);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }*/

    private void boardTitle() {
        textField.setBackground(Color.BLACK);
        textField.setForeground(new Color(219, 219, 219));
        textField.setFont(new Font("Monospaced", Font.BOLD, 40));
        textField.setHorizontalAlignment(SwingConstants.CENTER);
        textField.setText("TIC TAC TOE CHALLENGE");
        textField.setOpaque(true);

        //o text field anterior vai estar displayed neste titlePanel
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

    public String getPlayerMove() {
        return playerMove;
    }

    public void setGameState(char[][] gameState) {
        this.gameState = gameState;
    }
}


