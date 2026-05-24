package ofc.bot.handlers.games;

public enum GameType {
    TIC_TAC_TOE(false, true),
    ROULETTE(   true,  false),
    UNO(        false, true);

    private final boolean isMultiWiner;
    private final boolean isRandomizable;

    GameType(boolean isMultiWiner, boolean isRandomizable) {
        this.isMultiWiner = isMultiWiner;
        this.isRandomizable = isRandomizable;
    }

    public boolean isMultiWiner() {
        return this.isMultiWiner;
    }

    public boolean isRandomizable() {
        return this.isRandomizable;
    }
}