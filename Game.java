package ataxx;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;
import java.util.Stack;
import static ataxx.PieceColor.*;
import static ataxx.Game.State.*;
import static ataxx.Command.Type.*;
import static ataxx.GameException.error;

/**
 * Controls the play of the game.
 *
 * @author P. N. Hilfinger
 *
 * Thought about ideas and brainstormed with Aditya Tyagi for Game.java
 */
class Game {

    /**
     * States of play.
     */
    static enum State {
        SETUP, PLAYING, FINISHED;
    }

    /**
     * A new Game, using BOARD to play on, reading initially from
     * BASESOURCE and using REPORTER for error and informational messages.
     */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = new Board();
        _reporter = reporter;
        redplayer = new Manual(this, RED);
        blueplayer = new AI(this, BLUE);
    }

    /**
     * Run a session of Ataxx gaming.  Use an AtaxxGUI iff USEGUI.
     */
    void process(boolean useGUI) {
        Player red, blue;

        red = blue = null;

        GameLoop:
        while (true) {
            doClear(null);

            SetupLoop:
            while (_state == SETUP) {
                doCommand();
            }

            _state = PLAYING;
            red = redplayer;
            blue = blueplayer;


            while (_state != SETUP && !_board.gameOver()) {
                Move move;
                if (_board.whoseMove() == RED) {
                    move = red.myMove();
                } else {
                    move = blue.myMove();
                }
                if (_state == PLAYING) {
                    _board.makeMove(move);
                }
            }

            if (_state != SETUP) {
                reportWinner();
            }

            if (_state == PLAYING) {
                _state = FINISHED;
            }

            while (_state == FINISHED) {
                doCommand();
            }
        }

    }

    /**
     * Return a view of my game board that should not be modified by
     * the caller.
     */
    Board board() {
        return _board;
    }

    /**
     * Perform the next command from our input source.
     */
    void doCommand() {
        try {
            Command act =
                    Command.parseCommand(_inputs.getLine("ataxx: "));
            _commands.get(act.commandType()).accept(act.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /**
     * Read and execute commands until encountering a move or until
     * the game leaves playing state due to one of the commands. Return
     * the terminating move command, or null if the game first drops out
     * of playing mode. If appropriate to the current input source, use
     * PROMPT to prompt for input.
     */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command act = Command.parseCommand(_inputs.getLine(prompt));
                if (act.commandType() == Command.Type.PIECEMOVE) {
                    return act;
                } else {
                    _commands.get(act.commandType()).accept(act.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /**
     * Return random integer between 0 (inclusive) and MAX>0 (exclusive).
     */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /**
     * Report a move, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /**
     * Report an error, using a message formed from FORMAT and ARGS as
     * for String.format.
     */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /**
     * Perform the command 'auto OPERANDS[0]'.
     */
    void doAuto(String[] operands) {
        try {
            _state = SETUP;
            if (operands[0].equals("RED")) {
                redplayer = new AI(this, BLUE);
            }
        } catch (IllegalArgumentException excp) {
            error("unknown player: %s", operands[0]);
        }

    }

    /**
     * Perform a 'help' command.
     */
    void doHelp(String[] unused) {
        InputStream helpIn =
                Game.class.getClassLoader().getResourceAsStream("ataxx/"
                        + "help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                        = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /**
     * Perform the command 'load OPERANDS[0]'.
     */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            bufferread(new BufferedReader(reader));
            System.out.println("File loading");
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /**
     * Perform the reading of the bufferstream.
     * @param stream ** A BufferReader.**
     */

    private void bufferread(BufferedReader stream) {
        inputs.push(stream);
    }

    /**
     * Perform the command 'manual OPERANDS[0]'.
     */
    void doManual(String[] operands) {
        if (operands[0].equals("BLUE")) {
            blueplayer = new Manual(this, BLUE);
        }
    }

    /**
     * Exit the program.
     */
    void doQuit(String[] unused) {
        System.exit(0);
    }

    /**
     * Perform the command 'start'.
     */
    void doStart(String[] unused) {
        checkState("start", SETUP);
        _state = PLAYING;
    }

    /**
     * Perform the move OPERANDS[0].
     */
    void doMove(String[] operands) {
        if (_state == SETUP) {
            Move newmove = Move.move(operands[0].charAt(0),
                    operands[1].charAt(0), operands[2].charAt(0),
                    operands[3].charAt(0));
            if (_board.legalMove(newmove)) {
                _board.makeMove(newmove);
            }
        } else {
            throw new Error("Illegal Move");
        }
    }

    /**
     * Cause current player to pass.
     */
    void doPass(String[] unused) {
        _board.pass();
    }

    /**
     * Perform the command 'clear'.
     */
    void doClear(String[] unused) {
        _board.clear();
        _state = SETUP;
    }


    /**
     * Perform the command 'dump'.
     */
    void doDump(String[] unused) {
        System.out.println(_board);
    }

    /**
     * Execute 'seed OPERANDS[0]' command, where the operand is a string
     * of decimal digits. Silently substitutes another value if
     * too large.
     */
    void doSeed(String[] operands) {
        try {
            _randoms.setSeed(Long.parseLong(operands[0]));
        } catch (NumberFormatException excp) {
            error("Invalid number: %s", operands[0]);
        }
    }

    /**
     * Execute the command 'block OPERANDS[0]'.
     */
    void doBlock(String[] operands) {
        _state = SETUP;
        if (_state == SETUP) {
            String checkblock = operands[0];
            if (_board.legalBlock(checkblock.charAt(0), checkblock.charAt(1))) {
                _board.setBlock(checkblock.charAt(0), checkblock.charAt(1));
            } else {
                throw new Error("Not Legal to place Block here");
            }
        } else {
            throw new Error("Game is not in SETUP state ");
        }

    }

    /**
     * Execute the artificial 'error' command.
     */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /**
     * Report the outcome of the current game.
     */
    void reportWinner() {
        _state = FINISHED;
        String msg;
        msg = "Game over.";
        if (_board.whoseMove() == BLUE) {
            System.out.println("Blue wins.");
        } else if (_board.whoseMove() == RED) {
            System.out.println("Red wins.");
        }
        _reporter.outcomeMsg(msg);
    }

    /**
     * Check that game is currently in one of the states STATES, assuming
     * CMND is the command to be executed.
     */
    private void checkState(Command cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd.commandType());
    }

    /**
     * Check that game is currently in one of the states STATES, using
     * CMND in error messages as the name of the command to be executed.
     */
    private void checkState(String cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd);
    }

    /**
     * Mapping of command types to methods that process them.
     */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
            new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(BLOCK, this::doBlock);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PASS, this::doPass);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }


    /**
     * Input source.
     */
    private final CommandSources _inputs = new CommandSources();
    /**
     * My board.
     */
    private Board _board;
    /**
     * Current game state.
     */
    private State _state;
    /**
     * Used to send messages to the user.
     */
    private Reporter _reporter;
    /**
     * Source of pseudo-random numbers (used by AIs).
     */
    private Random _randoms = new Random();

    /**
     * A red Player.
     */
    private Player redplayer;
    /**
     * A Blue Player.
     */
    private Player blueplayer;
    /**
     * An Input BufferReader Stack.
     */
    private final Stack<BufferedReader> inputs = new Stack<BufferedReader>();

}
