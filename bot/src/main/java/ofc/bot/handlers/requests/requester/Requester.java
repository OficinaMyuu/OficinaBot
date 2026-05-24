package ofc.bot.handlers.requests.requester;

import ofc.bot.handlers.requests.RequestMapper;
import ofc.bot.handlers.requests.exceptions.HttpRequestException;
import okhttp3.Request;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface Requester {

    @NotNull
    RequestMapper makeRequest(@NotNull Supplier<Request> requestSupplier) throws HttpRequestException;
}