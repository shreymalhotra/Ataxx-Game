package ataxx;

/** Author: P. N. Hilfinger, (C) 2008. */

import java.util.Observable;
import java.util.Stack;
import java.util.ArrayList;
import java.util.Arrays;
import static ataxx.PieceColor.EMPTY;
import static ataxx.PieceColor.BLUE;
import static ataxx.PieceColor.RED;
import static ataxx.PieceColor.BLOCKED;
import static ataxx.GameException.error;

/**
 * An Ataxx board.   The squares are labeled by column (a char value between
 * 'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 * + 2) or by linearized index, an integer described below.  Values of
 * the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 * two layers of border squares, which are always blocked.
 * This artificial border (which is never actually printed) is a common
 * trick that allows one to avoid testing for edge conditions.
 * For example, to look at all the possible moves from a square, sq,
 * on the normal board (i.e., not in the border region), one can simply
 * look at all squares within two rows and columns of sq without worrying
 * about going off the board. Since squares in the border region are
 * blocked, the normal logic that prevents moving to a blocked square
 * will apply.
 * <p>
 * For some purposes, it is useful to refer to squares using a single
 * integer, which we call its "linearized index".  This is simply the
 * number of the square in row-major order (counting from 0).
 * <p>
 * Moves on this board are denoted by Moves.
 *
 * @author Shrey Malhotra
 *
 * Brainstormed and worked on ideas with Aditya Tyagi for Board.java
 */
class Board extends Observable {

    /**
     * Number of squares on a side of the board.
     */
    static final int SIDE = 7;
    /**
     * Length of a side + an artificial 2-deep border region.
     */
    static final int EXTENDED_SIDE = SIDE + 4;

    /**
     * Number of non-extending moves before game ends.
     */
    static final int JUMP_LIMIT = 25;

    /**
     * A new, cleared board at the start of the game.
     */
    static final PieceColor[] NEW_BOARD = new PieceColor[121];

    /**
     * A new, cleared board at the start of the game.
     */
    Board() {
        _board = NEW_BOARD;
        for (int k = 0; k < _board.length; k++) {
            _board[k] = EMPTY;
        }
        for (char c0 = 'a'; c0 <= 'g'; c0++) {
            for (char r0 = '1'; r0 <= '7'; r0++) {
                _board[index(c0, r0)] = EMPTY;
            }
        }
        _whoseMove = RED;
        totaljumps = 0;
        totalturns = 0;
        colorpcount = new int[2];
        colorpcount[0] = 2;
        colorpcount[1] = 2;
        oldboardstate = new Stack<PieceColor[]>();
        set(index('g', '7'), BLUE);
        set(index('g', '1'), RED);
        set(index('a', '7'), RED);
        set(index('a', '1'), BLUE);
        movedict = new ArrayList<Move>();
        oldboardstate = new Stack<PieceColor[]>();
        clear();
    }

    /**
     * A copy of B.
     */
    Board(Board b) {
        colorpcount = new int[2];
        colorpcount[0] = b.redPieces();
        colorpcount[1] = b.bluePieces();
        movedict = b.allMoves();
        oldboardstate = b.boardHistory();
        totaljumps = b.numJumps();
        totalturns = b.numMoves();
        _board = b._board.clone();
        _whoseMove = b.whoseMove();
    }

    /**
     * Return the linearized index of square COL ROW.
     */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /**
     * Return the linearized index of the square
     * with index SQ.
     * @param col **Integer of col**
     * @param row **Integer of row**
     * @param sq **Linearized sequence**
     */
    static int sidepiece(int sq, int col, int row) {
        return sq + col + row * EXTENDED_SIDE;
    }

    /**
     * Clear me to my starting state, with pieces in their initial
     * positions and no blocks.
     */
    void clear() {
        _whoseMove = RED;

        while (!(oldboardstate.isEmpty())) {
            undo();
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Return true iff the game is over: i.e., if neither side has
     * any moves, if one side has no pieces, or if there have been
     * MAX_JUMPS consecutive jumps without intervening extends.
     */
    boolean gameOver() {
        if (!(canMove(RED)) || canMove(RED)) {
            return true;
        } else if (!(canMove(BLUE))) {
            return true;
        } else if (!(canMove(RED))) {
            return true;
        } else if (numPieces(RED) == 0 || numPieces(BLUE) == 0) {
            return true;
        } else if (numPieces(BLUE) == 0) {
            return true;
        } else if (numPieces(RED) == 0) {
            return true;
        } else if (numJumps() > JUMP_LIMIT) {
            return true;
        }
        return false;
    }

    /**
     * Return number of red pieces on the board.
     */
    int redPieces() {
        return numPieces(RED);
    }

    /**
     * Return number of blue pieces on the board.
     */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /**
     * Return number of COLOR pieces on the board.
     */
    int numPieces(PieceColor color) {
        if (color == RED) {
            return colorpcount[0];
        } else if (color == BLUE) {
            return colorpcount[1];
        } else {
            throw new Error("You provided a valid"
                    + " piece color.");
        }
    }

    /**
     * Increment numPieces(COLOR) by K.
     */
    private void incrPieces(PieceColor color, int k) {
        if (color == RED) {
            colorpcount[0] += k;
            return;
        } else if (color == BLUE) {
            colorpcount[1] += k;
            return;
        } else {
            throw new Error("You provided a valid"
                    + " piece color.");
        }
    }

    /**
     * The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     * '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     * BLOCKED.  Returns the same value as get(index(C, R)).
     */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /**
     * Return the current contents of square with linearized index SQ.
     */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /**
     * Set get(C, R) to V, where 'a' <= C <= 'g', and
     * '1' <= R <= '7'.
     */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /**
     * Set square with linearized index SQ to V.  This operation is
     * undoable.
     */
    private void set(int sq, PieceColor v) {
        int blength = _board.length;
        PieceColor[] currBoard = new PieceColor[blength];
        System.arraycopy(_board, 0, currBoard, 0, blength);
        oldboardstate.push(currBoard);

        _board[sq] = v;
    }

    /**
     * Set square at C R to V (not undoable).
     */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /**
     * Set square at linearized index SQ to V (not undoable).
     */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /**
     * Return true iff MOVE is legal on the current board.
     */
    boolean legalMove(Move move) {
        char col1  = move.col1();
        char row1 = move.row1();
        if (!(move.isExtend() || move.isJump() || move.isPass())) {
            return false;
        } else if (_board[index(col1, row1)] != EMPTY) {
            return false;
        } else if (_board[index(col1, row1)] == BLOCKED) {
            return false;
        }
        return true;
    }

    /**
     * Return true iff player WHO can move, ignoring whether it is
     * that player's move and whether the game is over.
     */
    boolean canMove(PieceColor who) {
        for (char c0 = 'a'; c0 <= 'g'; c0++) {
            for (char r0 = '1'; r0 <= '7'; r0++) {
                {
                    int index = index(c0, r0);
                    if (_board[index] == who) {
                        for (int c1 = c0 - 2; c1
                                <= (int) (c0 + 2); c1++) {
                            for (int r1 = r0 - 2; r1
                                    <= (int) (r0 + 2); r1++) {
                                if (c1 == c0 || r1 == r0) {
                                    break;
                                } else if (legalMove(Move.move(c0, r0,
                                        (char) c1, (char) r1))) {
                                    return true;
                                }
                            }
                        }
                    }
                }

            }
        }
        return false;
    }


    /**
     * Return the color of the player who has the next move.  The
     * value is arbitrary if gameOver().
     */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /**
     * Return total number of moves and passes since the last
     * clear or the creation of the board.
     */
    int numMoves() {
        return totalturns;
    }

    /**
     * Return number of non-pass moves made in the current game since the
     * last extend move added a piece to the board (or since the
     * start of the game). Used to detect end-of-game.
     */
    int numJumps() {
        return totaljumps;
    }

    /** Accessor method for oldboardstate. Returns
     * a Stack of board past states.*/
    private Stack<PieceColor[]> boardHistory() {
        return oldboardstate;
    }

    /**
     * Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     * other than pass, assumes that legalMove(C0, R0, C1, R1).
     */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /**
     * Make the MOVE on this Board, assuming it is legal.
     */
    void makeMove(Move move) {
        assert legalMove(move);
        if (move.isPass()) {
            pass();
            return;
        }
        char col1  = move.col1();
        char row1 = move.row1();
        char row0 = move.row0();
        char col0 = move.col0();
        PieceColor nowcurr = _board[index(col0, row0)];
        if (move.isExtend()) {
            incrPieces(nowcurr, 1);
            set(index(col1, row1), nowcurr);
        }
        if (move.isJump()) {
            totaljumps += 1;
            set(index(col0, row0), EMPTY);
            set(index(col1, row1), nowcurr);
        }
        movedict.add(move);
        _whoseMove = _whoseMove.opposite();
        totalturns += 1;
        setChanged();
        notifyObservers();
    }

    /**
     * Update to indicate that the current player passes, assuming it
     * is legal to do so.  The only effect is to change whoseMove().
     */
    void pass() {
        assert !canMove(_whoseMove);
        if (_whoseMove == RED) {
            _whoseMove = BLUE;
        } else if (_whoseMove == BLUE) {
            _whoseMove = RED;
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Undo the last move.
     */
    void undo() {
        PieceColor[] prevBoardState = oldboardstate.pop();
        for (int len = 0; len < _board.length; len++) {
            _board[len] = prevBoardState[len];
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Indicate beginning of a move in the undo stack.
     */
    private void startUndo() {
        return;
    }

    /**
     * Add an undo action for changing SQ to NEWCOLOR on current
     * board.
     */
    private void addUndo(int sq, PieceColor newColor) {
        return;
    }

    /**
     * Return true iff it is legal to place a block at C R.
     * Assumes a Board of Actual Size 7.
     */
    boolean legalBlock(char c, char r) {
        if ((c == 'a' && r == '7')
                || (c == 'g' && r == '7') || (r == '1' && c == 'a')
                || (r == '1' && c == 'g')) {
            return false;
        }
        int currind = index(c, r);
        if (_board[currind] == BLOCKED) {
            return false;
        } else if (_board[currind] == BLUE) {
            return false;
        } else if (_board[currind] == RED) {
            return false;
        }
        return true;
    }

    /**
     * Return true iff it is legal to place a block at CR.
     */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /**
     * Set a block on the square C R and its reflections across the middle
     * row and/or column, if that square is unoccupied and not
     * in one of the corners. Has no effect if any of the squares is
     * already occupied by a block.  It is an error to place a block on a
     * piece.
     */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        _board[index(c, r)] = BLOCKED;
        int left = c - 'a';
        int bottom = Character.getNumericValue(r) - 1;
        int topright = 'g' - left;
        if (legalBlock((char) topright, r)) {
            _board[index((char) topright, r)] = BLOCKED;
        }
        int top = 7 - bottom;
        if (legalBlock(c, (char) (top + '0'))) {
            _board[index(c, (char) (top + '0'))] = BLOCKED;
        }
        if (legalBlock((char) topright,
                (char) (top + '0'))) {
            _board[index((char) topright,
                    (char) (top + '0'))] = BLOCKED;
        }
        setChanged();
        notifyObservers();
    }

    /**
     * Place a block at CR.
     */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /**
     * Return a list of all moves made since the last clear (or start of
     * game).
     */
    ArrayList<Move> allMoves() {
        return movedict;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /**
     * Return a text depiction of the board (not a dump).  If LEGEND,
     * supply row and column numbers around the edges.
     */
    String toString(boolean legend) {
        String astring = "";
        for (char r0 = '7'; r0 >= '1'; r0--) {
            astring += "\n";
            if (legend) {
                astring = astring + r0 + " ";
            }
            for (char c0 = 'a'; c0 <= 'g'; c0++) {
                PieceColor thePiecec0or = get(c0, r0);
                if (thePiecec0or == BLUE) {
                    if (c0 == 'a') {
                        astring = astring + "  b ";
                    } else {
                        astring = astring + "b ";
                    }
                } else if (thePiecec0or == RED) {
                    if (c0 == 'a') {
                        astring = astring + "  r ";
                    } else {
                        astring = astring + "r ";
                    }
                } else if (thePiecec0or == BLOCKED) {
                    if (c0 == 'a') {
                        astring = astring + "  X ";
                    } else {
                        astring = astring + "X ";
                    }
                } else if (thePiecec0or == EMPTY) {
                    if (c0 == 'a') {
                        astring = astring + "  - ";
                    } else {
                        astring = astring + "- ";
                    }
                }
            }
            if (legend) {
                astring += "\n" + "  a b c d e f g";
            }
        }
        String newstring = "===" + astring + "\n===";
        return newstring;
    }

    /**
     * For reasons of efficiency in copying the board,
     * we use a 1D array to represent it, using the usual access
     * algorithm: row r, column c => index(r, c).
     * <p>
     * Next, instead of using a 7x7 board, we use an 11x11 board in
     * which the outer two rows and columns are blocks, and
     * row 2, column 2 actually represents row 0, column 0
     * of the real board.  As a result of this trick, there is no
     * need to special-case being near the edge: we don't move
     * off the edge because it looks blocked.
     * <p>
     * Using characters as indices, it follows that if 'a' <= c <= 'g'
     * and '1' <= r <= '7', then row c, column r of the board corresponds
     * to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     * re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION].
     */
    private PieceColor[] _board;

    /**
     * Player that is on move.
     */
    private PieceColor _whoseMove;

    /** A array of the number of R and B pieces.*/
    private int[] colorpcount;

    /** A stack storing the previous states of the board.*/
    private Stack<PieceColor[]> oldboardstate;

    /** Storer of the total turns made since the start of the
    * game.*/
    private int totalturns;

    /** Stores the total number of jumps
     * since the start.*/
    private int totaljumps;

    /** Storer of the total turns made since the start of the
    * game.*/
    private ArrayList<Move> movedict;

    /** MY DECLARATIONS */

    /**
     * Checks iff SIDE's pieces are of the same color.
     * @param row **row number**
     * @param col **column number**
     * @param myPieceColor **Current Playing Color**
     */
    void setaside(PieceColor myPieceColor, char col, char row) {
        int myIndex = index(col, row);
        for (int c0 = -1; c0 <= 1; c0++) {
            for (int r0 = -1; r0 <= 1; r0++) {
                if (_board[sidepiece(myIndex, c0, r0)] != EMPTY
                        && (c0 != 0 && r0 != 0)) {
                    unrecordedSet(sidepiece(myIndex, c0, r0), myPieceColor);
                    incrPieces(myPieceColor, 1);
                }
            }
        }
    }

    /**
     * Return true iff SIDE's pieces are of the same color.
     */
    boolean samepieces(PieceColor side) {
        for (char c = 'a'; c <= 'g'; c++) {
            for (char r = '1'; r <= '7'; r++) {
                if (get(c, r) == side) {
                    return samepieceshelper(r, c, side);
                }
            }
        }
        return true;
    }

    /**
     * Return true iff the piece at this particular position is continguous.
     *
     * @param r    **row character**
     * @param c    **column character**
     * @param side **Current Playing Color**
     */
    boolean samepieceshelper(int r, int c, PieceColor side) {
        ArrayList<String> cr = new ArrayList<String>();
        PieceColor me = get((char) c, (char) r);
        int kc, kr, col, row;
        cr.add(c + "" + r);

        int i = 1;
        int count = 0;

        while (count < i) {
            c = Integer.parseInt(cr.get(count).substring(0, 1));
            r = Integer.parseInt(cr.get(count).substring(1));
            for (kc = -1; kc <= 1; kc++) {
                for (kr = -1; kr <= 1; kr++) {
                    col = c + kc;
                    row = r + kr;
                    if (col >= 'a' && col <= 'g' && row >= '1' && row <= '7') {
                        if (get((char) col, (char) row) == side) {
                            if (!cr.contains(col + "" + row)) {
                                cr.add(col + "" + row);
                                i += 1;
                            }
                        }
                    }
                }
            }
            count += 1;
        }
        for (char r2 = '1'; r2 <= '7'; r2++) {
            for (char c2 = 'a'; c2 <= 'g'; c2++) {
                if (get(c2, r2) == side && (!cr.contains(c2 + "" + r2))) {
                    return false;
                }
            }
        }
        return true;
    }


    /** Return a sequence of all legal moves.
     * @param who ** Indicates PieceColor.**
     * */
    ArrayList<Move> legalmoves(PieceColor who) {
        Move move;
        ArrayList<Move> lm = new ArrayList<Move>();
        for (char c0 = 'a'; c0 <= 'g'; c0++) {
            for (char r0 = '1'; r0 <= '7'; r0++) {
                {
                    int index = index(c0, r0);
                    if (_board[index] == who) {
                        for (int c1 = c0 - 2; c1
                                <= (int) (c0 + 2); c1++) {
                            for (int r1 = r0 - 2; r1
                                    <= (int) (r0 + 2); r1++) {
                                if (c1 == c0 || r1 == r0) {
                                    break;
                                } else if (legalMove(Move.move(c0, r0,
                                        (char) c1, (char) r1))) {
                                    move = Move.move(c0, r0, (char)
                                            c1, (char) r1);
                                    lm.add(move);
                                }
                            }
                        }
                    }
                }

            }
        }
        return lm;
    }
}
