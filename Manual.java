package ataxx;

import static ataxx.PieceColor.*;

/**
 * A Player that receives its moves from its Game's getMoveCmnd method.
 *
 * @author Shrey Malhotra
 */
class Manual extends Player {

    /**
     * A Player that will play MYCOLOR on GAME, taking its moves from
     * GAME.
     */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        Move move;
        Command cmnd = game().getMoveCmnd(myColor() + ": ");
        String[] operands = cmnd.operands();
        move = Move.move(operands[0].charAt(0), operands[1].charAt(0),
                operands[2].charAt(0), operands[3].charAt(0));
        if (game().board().legalMove(move)) {
            return move;
        } else {
            throw new Error("Illegal Move Attempted");
        }
    }
}

