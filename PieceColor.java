package ataxx;

/**
 * Describes the classes of Piece on an Ataxx board.
 *
 * @author P. N. Hilfinger
 */
enum PieceColor {

    /**
     * EMPTY: no piece.
     * BLOCKED: square contains a block.
     * RED, BLUE: piece colors.
     */
    EMPTY, BLOCKED,
    RED {
        @Override
        PieceColor opposite() {
            return BLUE;
        }

        /** Returns the character value
         * of the piece.
         */
        String pieceChar() {
            return "b";
        }

        @Override
        boolean isPiece() {
            return true;
        }
    },
    BLUE {
        @Override
        PieceColor opposite() {
            return RED;
        }

        /** Returns the character value
         * of the piece.
         */
        String pieceChar() {
            return "r";
        }

        @Override
        boolean isPiece() {
            return true;
        }
    };

    /**
     * Return player (white or black piece) for which  .fullName()
     * returns NAME.
     */
    static PieceColor playerValueOf(String name) {
        switch (name.toLowerCase()) {
        case "red":
            return RED;
        case "blue":
            return BLUE;
        default:
            throw new IllegalArgumentException("piece name unknown");
        }
    }

    /**
     * Return the piece color of my opponent, if defined.
     */
    PieceColor opposite() {
        throw new UnsupportedOperationException();
    }

    /**
     * Return true iff I denote a piece rather than an empty square or
     * block.
     */
    boolean isPiece() {
        return false;
    }

    @Override
    public String toString() {
        return capitalize(super.toString().toLowerCase());
    }

    /**
     * Return WORD with first letter capitalized.
     */
    static String capitalize(String word) {
        return Character.toUpperCase(word.charAt(0)) + word.substring(1);
    }

}
