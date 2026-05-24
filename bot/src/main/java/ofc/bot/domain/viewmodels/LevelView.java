package ofc.bot.domain.viewmodels;

public record LevelView(
        String username,
        long userId,
        int level,
        int rank,
        int xp
) {
    public static LevelView empty(long userId) {
        return new LevelView(null, userId, 0, 0, 0);
    }

    public String displayIdentifier() {
        return username == null || username.isBlank()
                ? String.valueOf(userId)
                : username;
    }
}