package ofc.bot.handlers.requests.exceptions;

public class HttpRequestException extends RuntimeException {

    public HttpRequestException(Exception e) {
        super(e);
    }

    public HttpRequestException(String message) {
        super(message);
    }
}