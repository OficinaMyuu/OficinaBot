package ofc.bot.commands.impl.slash;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RoleMembersCommandTest {
    @Test
    void shouldFormatMembersAlphabeticallyByUsername() {
        List<Member> members = List.of(
                member("20", "beta"),
                member("2", "alpha"),
                member("100", "Alpha")
        );

        String formatted = RoleMembersCommand.format(members);

        assertEquals("""
                100  ->  Alpha
                2    ->  alpha
                20   ->  beta""", formatted);
    }

    private static Member member(String id, String username) {
        User user = proxy(User.class, (proxy, method, args) -> switch (method.getName()) {
            case "getName" -> username;
            case "toString" -> username;
            default -> throw new UnsupportedOperationException(method.getName());
        });

        return proxy(Member.class, (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            case "getUser" -> user;
            case "toString" -> username + " (" + id + ")";
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        Object instance = Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                handler
        );

        return type.cast(instance);
    }
}
