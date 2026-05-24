package ofc.bot.util;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UrlBuilderTest {
    @Test
    void shouldParseExistingQueryParameters() {
        UrlBuilder builder = new UrlBuilder("https://example.com/search?q=discord+bot&lang=pt-BR");

        assertEquals("discord bot", builder.get("q"));
        assertEquals("pt-BR", builder.get("lang"));
        assertTrue(builder.contains("q"));
    }

    @Test
    void shouldPreservePathAndFragmentWhenReplacingQueryParameters() {
        UrlBuilder builder = new UrlBuilder("https://example.com/api/v1/items?page=1#section");

        builder.set("page", "2")
                .set("sort", "name asc");

        assertEquals(
                "https://example.com/api/v1/items?page=2&sort=name+asc#section",
                builder.build()
        );
    }

    @Test
    void shouldRemoveParameterWhenValueIsNull() {
        UrlBuilder builder = new UrlBuilder("https://example.com?keep=yes&drop=no");

        builder.set("drop", null);

        assertEquals("https://example.com?keep=yes", builder.toString());
        assertFalse(builder.contains("drop"));
    }

    @Test
    void shouldEncodeEmptyValuesExplicitly() {
        UrlBuilder builder = new UrlBuilder("https://example.com");

        builder.set("empty", "");

        assertEquals("https://example.com?empty=", builder.build());
    }

    @Test
    void shouldReturnImmutableParameterSnapshot() {
        UrlBuilder builder = new UrlBuilder("https://example.com?foo=bar");

        Map<String, String> snapshot = builder.parameters();

        assertEquals(Map.of("foo", "bar"), snapshot);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("bar", "baz"));
    }

    @Test
    void shouldClearAllParameters() {
        UrlBuilder builder = new UrlBuilder("https://example.com?a=1&b=2");

        builder.clear();

        assertTrue(builder.isEmpty());
        assertEquals("https://example.com", builder.build());
    }

    @Test
    void shouldBuildUriInstance() {
        UrlBuilder builder = UrlBuilder.from(URI.create("https://example.com/base"));

        URI uri = builder.add("name", "Oficina Bot").toUri();

        assertEquals(URI.create("https://example.com/base?name=Oficina+Bot"), uri);
    }
}
