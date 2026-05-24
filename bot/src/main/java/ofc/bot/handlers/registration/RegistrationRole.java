package ofc.bot.handlers.registration;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public enum RegistrationRole {
    REGISTERED(664923267601006622L),
    NON_REGISTERED(664921745777623088L),
    VERIFYING(758095503845228636L),
    REGISTRAR(740360659363168287L),
    ADULT(664918505963126814L),
    TWEEN(758095500884049960L),
    UNDERAGE(664918505400958986L),
    DESKTOP(664917764229824512L),
    MOBILE(664917765395578892L),
    FEMALE(664916190082236427L),
    MALE(664916190904320000L),
    NON_BINARY(664916189029466122L);

    private final long id;

    RegistrationRole(long id) {
        this.id = id;
    }

    public long id() {
        return this.id;
    }

    @Nullable
    public Role role(@NotNull Guild guild) {
        return guild.getRoleById(this.id);
    }

    public boolean isPresent(@Nullable Member member) {
        if (member == null) return false;

        return member.getRoles()
                .stream()
                .anyMatch(role -> role.getIdLong() == this.id);
    }

    @NotNull
    public static List<RegistrationRole> getByAge(int age) {
        if (age >= 18) {
            return List.of(ADULT);
        }

        if (age < 13) {
            return List.of(UNDERAGE, TWEEN);
        }

        return List.of(UNDERAGE);
    }
}
