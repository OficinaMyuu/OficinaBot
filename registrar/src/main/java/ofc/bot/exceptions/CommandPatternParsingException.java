package ofc.bot.exceptions;

public class CommandPatternParsingException extends IllegalArgumentException {

    public CommandPatternParsingException(String message) {
        super(message);
    }

    public CommandPatternParsingException(Throwable cause) {
        super(cause);
    }
}