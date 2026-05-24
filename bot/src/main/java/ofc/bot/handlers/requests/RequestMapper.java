package ofc.bot.handlers.requests;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.dv8tion.jda.api.utils.data.DataObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class RequestMapper {
    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();
    private final byte[] bytes;
    private final boolean isOk;
    private final int statusCode;

    public RequestMapper(byte[] bytes, boolean isOk, int code) {
        this.bytes = bytes;
        this.isOk = isOk;
        this.statusCode = code;
    }

    public DataObject asDataObject() {
        return DataObject.fromJson(this.bytes);
    }

    public String asString() {
        return new String(this.bytes);
    }

    public InputStream asInputStream() {
        return new ByteArrayInputStream(this.bytes);
    }

    public boolean isOk() {
        return this.isOk;
    }

    public int getStatusCode() {
        return this.statusCode;
    }

    /**
     * This method uses {@link Gson} to parse the response body,
     * expecting it to be a valid JSON.
     *
     * @param type the object type to parse the response.
     * @return the instance of the class of type {@code type}.
     */
    public <T> T json(Class<T> type) {
        return GSON.fromJson(new String(this.bytes), type);
    }

    public <T> T json(Class<T> type, int expectedCode) {
        return expectedCode == this.statusCode ? json(type) : null;
    }
}