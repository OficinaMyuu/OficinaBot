package ofc.bot.handlers.giveaway;

public enum GiveawayClaimResult {
    CLAIMED,
    GIVEAWAY_NOT_FOUND,
    NOT_A_WINNER,
    NOT_CLAIMABLE,
    WRONG_PRIZE_TYPE,
    INVALID_COLOR_ROLE,
    ROLE_NOT_FOUND,
    MEMBER_NOT_FOUND,
    ALREADY_CLAIMING,
    DISCORD_FAILURE,
    ECONOMY_FAILURE
}
