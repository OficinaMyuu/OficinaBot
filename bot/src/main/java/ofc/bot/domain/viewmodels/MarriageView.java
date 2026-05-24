package ofc.bot.domain.viewmodels;

import ofc.bot.domain.entity.AppUser;

public record MarriageView(
        AppUser requester,
        AppUser target,
        int id,
        long marriedAt,
        long createdAt,
        long updatedAt
) {
    /**
     * Returns the partner of the user with the provided ID.
     * This method assumes that the provided ID belongs to either
     * the requester or the target.
     * <p>
     * Note: This method does not check if the provided id exists
     * in the relationship.
     * Instead, it simply returns the other user following this logic:
     * <pre>
     * {@code
     * if (requester.getId() == userId) {
     *     return target;
     * } else {
     *     return requester;
     * }
     * }
     * </pre>
     *
     * @param userId the ID of the user whose partner is to be returned.
     * @return The partner of the user with the specified ID.
     */
    public AppUser partner(long userId) {
        return requester.getId() == userId ? target : requester;
    }
}