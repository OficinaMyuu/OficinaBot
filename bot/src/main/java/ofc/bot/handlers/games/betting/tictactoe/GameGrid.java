package ofc.bot.handlers.games.betting.tictactoe;

import net.dv8tion.jda.api.entities.emoji.Emoji;
import ofc.bot.util.Bot;
import org.jetbrains.annotations.Contract;

import java.util.Arrays;
import java.util.Map;

public class GameGrid {
    public static final Map<Character, Emoji> EMOJIS;
    private static final int MIN_GRID_SIZE = 3;
    private static final int MAX_GRID_SIZE = 5;
    private final char[][] board;

    public GameGrid(int gridSize) {
        if (gridSize < MIN_GRID_SIZE || gridSize > MAX_GRID_SIZE)
            failGridSize(gridSize);

        this.board = new char[gridSize][gridSize];
        for (int i = 0; i < gridSize; i++) {
            Arrays.fill(board[i], '\0');
        }
    }

    public void set(int row, int col, char symbol) {
        if (symbol != 'X' && symbol != 'O')
            throw new IllegalArgumentException("Invalid symbol: " + symbol);

        this.board[row][col] = symbol;
    }

    public int size() {
        return board.length;
    }

    public char[][] getBoard() {
        return board;
    }

    public char get(int row, int col) {
        return board[row][col];
    }

    public char getWinner() {
        char diag = checkDiags();
        if (diag != '\0') return diag; // We have a winner in one of the diagonals

        for (int i = 0; i < board.length; i++) {
            char col = checkCol(i);
            if (col != '\0') return col; // We have a winner at the ith column

            char row = checkRow(i);
            if (row != '\0') return row; // We have a winner at the ith row
        }
        return '\0'; // No winners
    }

    public boolean hasSlotAvailable() {
        for (char[] row : board) {
            for (char cell : row) {
                if (cell == '\0') {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        for (char[] row : board) {
            String els = String.valueOf(row).replaceAll("(?i)[^XO]", " ");
            builder.append(String.join("", els));
        }
        return builder.toString();
    }

    private char checkCol(int col) {
        char firstChar = board[0][col];
        if (firstChar != '\0') { // Ensure the first cell is occupied
            for (int i = 1; i < board.length; i++) {
                if (board[i][col] != firstChar) {
                    return '\0'; // No winners in this column
                }
            }
            return firstChar; // Winner found in this column
        }
        return '\0'; // No winners
    }


    private char checkRow(int row) {
        char firstChar = board[row][0];
        if (firstChar != '\0') { // Ensure the first cell is occupied
            for (int i = 1; i < board.length; i++) {
                if (board[row][i] != firstChar) {
                    return '\0'; // No winners in this row
                }
            }
            return firstChar; // Winner found in this row
        }
        return '\0'; // No winners
    }

    // We have to check both diagonals
    private char checkDiags() {
        char firstChar = board[0][0];
        boolean firstValid = true;

        // Checking the first diagonal
        for (int i = 1; i < board.length; i++) {
            if (board[i][i] != firstChar) {
                // This diagonal has already divergent characters
                firstValid = false;
                break;
            }
        }
        if (firstValid) return firstChar;

        firstChar = board[0][board.length - 1];
        // Checking the second diagonal
        for (int i = 1; i < board.length; i++) {
            if (board[i][board.length - 1 - i] != firstChar) {
                // Both tests failed
                return '\0';
            }
        }
        return firstChar;
    }

    @Contract("_ -> fail")
    private void failGridSize(int size) {
        throw new IllegalArgumentException(String.format("Grid size must be in range of %d - %d, provided: %d",
                MIN_GRID_SIZE, MAX_GRID_SIZE, size));
    }

    static {
        EMOJIS = Map.of(
                'X', Emoji.fromUnicode("❌"),
                'O', Emoji.fromUnicode("⭕"),
                '\0', Bot.Emojis.INV
        );
    }
}