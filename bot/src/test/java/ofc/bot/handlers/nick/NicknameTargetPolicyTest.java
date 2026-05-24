package ofc.bot.handlers.nick;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import ofc.bot.util.content.Staff;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NicknameTargetPolicyTest {
    private final NicknameTargetPolicy policy = new NicknameTargetPolicy();

    @Test
    void shouldRejectBotTargets() {
        Member issuer = member(false, List.of(), true);
        Member target = member(true, List.of(), true);

        NicknameTargetPolicy.TargetValidation validation = policy.validate(issuer, target);

        assertFalse(validation.accepted());
        assertEquals("Bots n\u00e3o podem receber pedidos de altera\u00e7\u00e3o de apelido.", validation.rejectionReason());
    }

    @Test
    void shouldRejectStaffTargets() {
        Member issuer = member(false, List.of(), true);
        Member target = member(false, List.of(role(Staff.GENERAL.getId())), true);

        NicknameTargetPolicy.TargetValidation validation = policy.validate(issuer, target);

        assertFalse(validation.accepted());
        assertEquals("Staffs n\u00e3o podem solicitar altera\u00e7\u00e3o de apelido para outros staffs.", validation.rejectionReason());
    }

    @Test
    void shouldRejectTargetsAboveIssuerHierarchy() {
        Member issuer = member(false, List.of(), false);
        Member target = member(false, List.of(), true);

        NicknameTargetPolicy.TargetValidation validation = policy.validate(issuer, target);

        assertFalse(validation.accepted());
        assertEquals(
                "Voc\u00ea n\u00e3o pode solicitar altera\u00e7\u00e3o de apelido para esse membro pela hierarquia atual de cargos.",
                validation.rejectionReason()
        );
    }

    @Test
    void shouldAcceptRegularTargetsBelowIssuerHierarchy() {
        Member issuer = member(false, List.of(), true);
        Member target = member(false, List.of(), true);

        NicknameTargetPolicy.TargetValidation validation = policy.validate(issuer, target);

        assertTrue(validation.accepted());
        assertNull(validation.rejectionReason());
    }

    private static Member member(boolean bot, List<Role> roles, boolean canInteract) {
        User user = user(bot);
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getUser" -> user;
            case "getRoles" -> roles;
            case "canInteract" -> args != null && args.length == 1 && args[0] instanceof Member && canInteract;
            default -> defaultValue(proxy, method.getName(), method.getReturnType(), args);
        };

        return (Member) Proxy.newProxyInstance(Member.class.getClassLoader(), new Class<?>[]{Member.class}, handler);
    }

    private static User user(boolean bot) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "isBot" -> bot;
            default -> defaultValue(proxy, method.getName(), method.getReturnType(), args);
        };

        return (User) Proxy.newProxyInstance(User.class.getClassLoader(), new Class<?>[]{User.class}, handler);
    }

    private static Role role(String id) {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "getId" -> id;
            default -> defaultValue(proxy, method.getName(), method.getReturnType(), args);
        };

        return (Role) Proxy.newProxyInstance(Role.class.getClassLoader(), new Class<?>[]{Role.class}, handler);
    }

    private static Object defaultValue(Object proxy, String methodName, Class<?> returnType, Object[] args) {
        if ("equals".equals(methodName)) {
            return args != null && args.length == 1 && proxy == args[0];
        }
        if ("hashCode".equals(methodName)) {
            return System.identityHashCode(proxy);
        }
        if ("toString".equals(methodName)) {
            return proxy.getClass().getInterfaces()[0].getSimpleName() + "Proxy";
        }
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
