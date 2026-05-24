package ofc.bot.util.content;

import net.dv8tion.jda.api.entities.Role;
import ofc.bot.Main;

public enum Roles {
    AMONG_US(  "596784802150088704"),
    SALADA(    "693973471989858714"),
    STUDY(     "1249474433278677022"),
    PENALIZED( "1186192823008116746"),
    NOTIFY_BOT("1334324077271453808");

    final String id;

    Roles(String id) {
        this.id = id;
    }

    public String id() {
        return this.id;
    }

    public Role role() {
        return Main.getApi().getRoleById(this.id);
    }
}