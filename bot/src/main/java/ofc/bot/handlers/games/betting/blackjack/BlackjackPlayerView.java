package ofc.bot.handlers.games.betting.blackjack;

import net.dv8tion.jda.api.entities.User;

public record BlackjackPlayerView(long userId, String name, String avatarUrl) {
    public static BlackjackPlayerView from(User user) {
        return new BlackjackPlayerView(user.getIdLong(), user.getEffectiveName(), user.getEffectiveAvatarUrl());
    }
}
