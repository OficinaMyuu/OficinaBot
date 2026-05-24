package ofc.bot.domain.viewmodels;

public record LeaderboardUser (
        String username,
        long userId,
        long balance
) {
    public String displayIdentifier() {
        return username == null || username.isBlank()
                ? String.valueOf(userId)
                : username;
    }
}