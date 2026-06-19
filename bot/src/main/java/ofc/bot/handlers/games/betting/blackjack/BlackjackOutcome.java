package ofc.bot.handlers.games.betting.blackjack;

public enum BlackjackOutcome {
    WIN("Vitória"),
    LOSS("Derrota"),
    PUSH("Empate");

    private final String displayName;

    BlackjackOutcome(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
