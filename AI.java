package ataxx;

import java.util.Random;


/**
 * A Player that computes its own moves.
 *
 * @author Shrey Malhotra
 */
class AI extends Player {

    /**
     * Maximum minimax search depth before going to static evaluation.
     */
    private static final int MAX_DEPTH = 4;
    /**
     * A position magnitude indicating a win (for red if positive, blue
     * if negative).
     */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /**
     * A magnitude greater than a normal value.
     */
    private static final int INFTY = Integer.MAX_VALUE;
    /**
     * Value of losing on a board.
     */
    private static final double LOSS_VALUE = -1000.0;


    /**
     * A new AI for GAME that will play MYCOLOR.
     */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    /**
     * Return a move for me from the current position, assuming there
     * is a move.
     */
    @Override
    Move myMove() {
        Move move = findMove(myColor(), game().board(), MAX_DEPTH);
        return move;
    }

    /**
     * Used to communicate best moves found by findMove, when asked for.
     */
    private Move _lastFoundMove;


    /**
     * Return a heuristic value for BOARD.
     */
    private int staticScore(Board board) {
        if (board.samepieces(board.whoseMove())) {
            return Integer.MAX_VALUE;
        } else if (board.samepieces(board.whoseMove().opposite())) {
            return Integer.MIN_VALUE;
        } else {
            Random r = new Random();
            return r.nextInt();
        }
    }

    /**
     * Find a move from position BOARD and return its value, recording
     * the move found in _lastFoundMove. Searches up to depth
     * levels before using a static estimate.
     * @param side **represents piececolor**
     * @param board **the board to make move on**
     * @param depth **the depth of search**
     */
    private Move findMove(PieceColor side, Board board, int depth) {
        guessMove(side, board, depth, Double.POSITIVE_INFINITY);
        return _lastFoundMove;
    }

    /**
     * Helper Function. Find a move from position BOARD
     * and return its value, recording
     * the move found in _lastFoundMove. Searches up to
     * DEPTH levels before using a static estimate.
     * @param side **represents piececolor**
     * @param cutoff **the cutoff value to stop the tree search**
     * @param board **the board to make move on**
     * @param depth **the depth of search**
     */
    private double guessMove(PieceColor side, Board board, int depth,
                             double cutoff) {
        double bestValue = Double.NEGATIVE_INFINITY;
        Move bestSoFar = null;
        if (board.samepieces(side)) {
            return Double.POSITIVE_INFINITY;
        } else if (board.samepieces(side.opposite())) {
            return Double.NEGATIVE_INFINITY;
        }
        if (depth == 0) {
            return movevalue(side, board);
        }
        for (Move move : board.legalmoves(side)) {
            Board newboard = new Board(board);
            newboard.makeMove(move);
            double value = -guessMove(side.opposite(), newboard,
                    depth - 1, -bestValue);
            if (bestSoFar == null || value > bestValue) {
                bestSoFar = move;
                bestValue = value;
                if (value > cutoff) {
                    break;
                }
            }
        }
        _lastFoundMove = bestSoFar;
        return bestValue;
    }

    /**
     * Returns a heuristic value of the Board.
     * @param side **represents piececolor**
     * @param board **the board to make move on**
     */
    private static double movevalue(PieceColor side, Board board) {
        if (board.samepieces(side)) {
            return Double.POSITIVE_INFINITY;
        } else if (board.samepieces(side.opposite())) {
            return Double.NEGATIVE_INFINITY;
        }
        return treemideval(side, board);
    }

    /**
     * Helper Function to Function that returns Heuristic
     * Value, Divides Game Tree.
     * @param side **represents piececolor**
     * @param board **the board to make move on**
     */
    private static Double treemideval(PieceColor side, Board board) {
        double value = 0;
        for (char c = 'a'; c <= 'g'; c++) {
            for (char r = '1'; r <= '7'; r++) {
                if (board.get(c, r) == side) {
                    value -= nodevalue(c, r);
                } else if (board.get(c, r) == side.opposite()) {
                    value += nodevalue(c, r);
                }
            }
        }
        return value;
    }

    /**
     * Helper Function to Function that returns Heuristic Value.
     * Finds value of node on Game Tree.
     * @param c **the column character**
     * @param r **the row character**
     */
    static Double nodevalue(char c, char r) {
        return Math.abs(c - (double) (Board.SIDE / 2))
                + Math.abs(r - (double) (Board.SIDE / 2));
    }
}

