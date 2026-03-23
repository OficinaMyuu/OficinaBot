package ofc.bot.handlers.games.mafia.models;

import ofc.bot.handlers.games.mafia.enums.Role;

public class Player {
    private final long userId;
    private boolean isAlive;
    private long previousTargetId;
    private Role role;

    public Player(long userId) {
        this.userId = userId;
        this.isAlive = true;
        this.previousTargetId = 0;
        this.role = null;
    }

    public long getUserId() {
        return this.userId;
    }

    public String getMention() {
        return String.format("<@%d>", this.userId);
    }

    public boolean isAlive() {
        return this.isAlive;
    }

    public long getPreviousTargetId() {
        return this.previousTargetId;
    }

    public Role getRole() {
        return this.role;
    }

    public void setAlive(boolean alive) {
        isAlive = alive;
    }

    public void setPreviousTargetId(long previousTargetId) {
        this.previousTargetId = previousTargetId;
    }

    public void setRole(Role role) {
        this.role = role;
    }
}