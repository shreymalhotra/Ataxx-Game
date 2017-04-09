package ataxx;

import ucb.gui2.Pad;

import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Image;
import java.util.Observer;
import java.util.Observable;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.io.IOException;
import java.awt.event.MouseEvent;

/**
 * Widget for displaying an Ataxx board.
 *
 * @author Shrey Malhotra
 */
class AtaxxBoardWidget extends Pad implements Observer {

    /**
     * Length of side of one square, in pixels.
     */
    static final int SQDIM = 50;
    /**
     * Number of squares on a side.
     */
    static final int SIDE = Board.SIDE;
    /**
     * Radius of circle representing a piece.
     */
    static final int PIECE_RADIUS = 15;
    /**
     * Game board size.
     */
    static final int BOARD_SIZE = 640;
    /**
     * Color of red pieces.
     */
    private static final Color RED_COLOR = Color.RED;
    /**
     * Color of blue pieces.
     */
    private static final Color BLUE_COLOR = Color.BLUE;
    /**
     * Color of blank squares.
     */
    private static final Color BLANK_COLOR = Color.WHITE;
    /**
     * Displayed dimensions of a instruction image.
     */
    static final int INS_WIDTH = 391, INS_HEIGHT = 626;
    /**
     * Direction of free face.
     */
    private static String dirface = "freefaceL.png";
    /**
     * Displayed location of a free face.
     */
    static final int FFLOC1 = 655, FFLOC2 = 70;
    /**
     * Displayed dimensions of Freeface.
     */
    static final int FF_W = 50, FF_H = 50;
    /**
     * Displayed dimensions of a piece image.
     */
    static final int PIECE_SIZE = 80;
    /**
     * Stroke for lines.
     */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);
    /**
     * Dimension of current drawing surface in pixels.
     */
    private int dsd;

    /**
     * Model being displayed.
     */
    private static Board _model;

    /**
     * A new widget displaying MODEL.
     */
    AtaxxBoardWidget(Board model) {
        _model = model;
        setMouseHandler("click", this::readMove);
        _model.addObserver(this);
        dsd = SQDIM * SIDE;
        setPreferredSize(dsd, dsd);
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(BLANK_COLOR);
        Rectangle b = g.getClipBounds();
        g.fillRect(0, 0, dsd, dsd);
        g.drawImage(extractimage("board.png"), 0, 0,
                BOARD_SIZE, BOARD_SIZE, null);
        g.drawImage(extractimage(dirface), FFLOC1, FFLOC2,
                FF_H, FF_W, null);
        putpieceonpoint(g, _model);
    }

    /**
     * Draw a block centered at (CX, CY) on G.
     */
    void drawBlock(Graphics2D g, int cx, int cy) {
        return;
    }

    /**
     * Notify observers of mouse's current position from click event WHERE.
     */
    private void readMove(String unused, MouseEvent where) {
        int x = where.getX(), y = where.getY();
        char mouseCol, mouseRow;
        if (where.getButton() == MouseEvent.BUTTON1) {
            mouseCol = (char) (x / SQDIM + 'a');
            mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            if (mouseCol >= 'a' && mouseCol <= 'g'
                    && mouseRow >= '1' && mouseRow <= '7') {
                setChanged();
                notifyObservers("" + mouseCol + mouseRow);
            }
        }
    }

    @Override
    public synchronized void update(Observable model, Object arg) {
        repaint();
    }

    /**
     * Method for Image Extraction.
     * @param name ** Represents Name Of string.**
     * @return **Exxtracted Image**
     */
    private Image extractimage(String name) {
        InputStream input =
                getClass().getResourceAsStream("/ataxx/pictures" + name);
        try {
            return ImageIO.read(input);
        } catch (IOException exception) {
            return null;
        }
    }

    /**
     * Method for PieceColor Extraction.
     * @param p **represents piececolor**
     * @return **Exxtracted Image**
     */
    private Image getPieceColor(PieceColor p) {
        return extractimage("piececolors/" + p + ".png");
    }

    /**
     * Draw Piece at X, Y on G.
     * @param p **represents piececolor**
     * @param g **Represents 2D Graphics Package.**
     * @param x **Represents X Location**
     * @param y **Represents Y Location**
     */
    private void paintPiece(Graphics2D g, PieceColor p, int x, int y) {
        if (p == PieceColor.RED || p == PieceColor.BLUE) {
            g.drawImage(getPieceColor(p), x, y,
                    PIECE_SIZE, PIECE_SIZE, null);
        }
    }

    /**
     * Method to draw the PieceColor .
     * @param b **the board to make move on**
     * @param g **Represents 2D Graphics Package.**
     */
    void putpieceonpoint(Graphics2D g, Board b) {
        for (char c = 'a'; c <= 'g'; c++) {
            for (char r = '1'; r <= '7'; r++) {
                PieceColor p = b.get(c, r);
                if (p != PieceColor.EMPTY) {
                    paintPiece(g, p, (c - 1) * PIECE_SIZE,
                            (7 - r) * PIECE_SIZE);
                }
            }
        }
    }
}
