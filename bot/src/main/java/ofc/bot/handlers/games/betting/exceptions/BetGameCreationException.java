package ofc.bot.handlers.games.betting.exceptions;

public class BetGameCreationException extends RuntimeException {

    public BetGameCreationException(String message) {
        super(message);
    }

    public BetGameCreationException(String format, Object... args) {
        this(String.format(format, args));
    }
}