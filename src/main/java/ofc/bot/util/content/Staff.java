package ofc.bot.util.content;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import ofc.bot.Main;

import java.util.List;
import java.util.stream.Stream;

public enum Staff {
    GENERAL(         "691178135596695593",  Scope.NONE, -1),
    ALMIRANTES_FROTA("1048808588375773234", Scope.NONE, -1),

    /* Mov Call */
    MOV_CALL_CO_LEADER(  "691167801783877653", Scope.MOV_CALL, 4),
    MOV_CALL_VICE_LEADER("740360644645093437", Scope.MOV_CALL, 3),
    MOV_CALL_SUPERIOR(   "691167797270806538", Scope.MOV_CALL, 2),
    MOV_CALL_MAIN(       "691173151400263732", Scope.MOV_CALL, 1),
    MOV_CALL_TRAINEE(    "691173142969712640", Scope.MOV_CALL, 0),
    
    /* Support */
    AJUDANTES_CO_LEADER(  "648444762852163588", Scope.SUPPORT, 4),
    AJUDANTES_VICE_LEADER("740360642032173156", Scope.SUPPORT, 3),
    AJUDANTES_SUPERIOR(   "691167798474440775", Scope.SUPPORT, 2),
    AJUDANTES_MAIN(       "592427681727905792", Scope.SUPPORT, 1),
    AJUDANTES_TRAINEE(    "648408508219260928", Scope.SUPPORT, 0);

    private final String id;
    private final Scope field;
    private final int seniority;

    Staff(String id, Scope scope, int seniority) {
        this.id = id;
        this.field = scope;
        this.seniority = seniority;
    }

    public String getId() {
        return this.id;
    }

    public Scope getField() {
        return this.field;
    }

    public int getSeniority() {
        return this.seniority;
    }

    public Role role() {
        return Main.getApi().getRoleById(this.id);
    }

    public static boolean isStaff(Member member) {
        return isStaff(member.getRoles());
    }

    public static boolean isStaff(List<Role> roles) {
        return roles
                .stream()
                .anyMatch(r -> r.getId().equals(GENERAL.id));
    }

    public static List<Staff> getByScope(Scope scope) {
        return Stream.of(values())
                .filter(s -> s.field == scope)
                .toList();
    }

    public static List<String> getIdsByScope(Scope scope) {
        return getByScope(scope)
                .stream()
                .map(Staff::getId)
                .toList();
    }

    public enum Scope {
        SUPPORT,
        MOV_CALL,
        NONE
    }
}