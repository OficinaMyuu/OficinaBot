package ofc.bot.utils;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import ofc.bot.RegisterMaster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public enum Roles {
    REGISTERED(    664923267601006622L, "Registrado"),
    NON_REGISTERED(664921745777623088L, "Não Registrado"),
    VERIFYING(     758095503845228636L, "Em Verificação"),
    REGISTRAR(     740360659363168287L, "Registrador"),

    // Age
    ADULT(         664918505963126814L, "Maior de Idade"),
    TWEEN(         758095500884049960L, "Menor de 13 anos"),
    UNDERAGE(      664918505400958986L, "Menor do Idade"),

    // Device
    DESKTOP(       664917764229824512L, "PC"),
    MOBILE(        664917765395578892L, "Mobile"),

    // Gender
    FEMALE(        664916190082236427L, "Feminino"),
    MALE(          664916190904320000L, "Masculino"),
    NON_BINARY(    664916189029466122L, "Não Binário");

    private final long roleId;
    private final String name;

    Roles(long roleId, String name) {
        this.roleId = roleId;
        this.name = name;
    }

    public long getRoleId() {
        return this.roleId;
    }

    public String getName() {
        return this.name;
    }

    public static boolean isRegistered(Member member) {
        return REGISTERED.isPresent(member);
    }

    public static List<Roles> getByAge(int age) {
        List<Roles> roles = new ArrayList<>(2);

        if (age >= 18) roles.add(ADULT);
        if (age < 18) roles.add(UNDERAGE);
        if (age < 13) roles.add(TWEEN);

        return Collections.unmodifiableList(roles);
    }

    public static List<Roles> getRegisteredRoles() {
        return List.of(
                REGISTERED,
                ADULT,
                TWEEN,
                UNDERAGE,
                DESKTOP,
                MOBILE,
                FEMALE,
                MALE,
                NON_BINARY
        );
    }

    public static List<Roles> getNonRegisteredRoles() {
        return List.of(
                NON_REGISTERED
        );
    }

    public Role getRole() {
        JDA api = RegisterMaster.getApi();
        return api.getRoleById(this.roleId);
    }

    public boolean isPresent(Member member) {

        if (member == null)
            return false;

        return member.getRoles()
                .stream()
                .anyMatch(r -> r.getIdLong() == this.roleId);
    }
}