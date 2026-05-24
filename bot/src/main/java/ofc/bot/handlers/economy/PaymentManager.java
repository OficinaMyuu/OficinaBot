package ofc.bot.handlers.economy;

public interface PaymentManager {
    // ---------- Guild Based ----------
    /**
     * Fetches the bank account of the user with the provided ID
     * in the provided guild.
     * <p>
     * This method works for both global and guild-based economies, however,
     * for global economies like {@link CurrencyType#OFICINA}, the {@code guildId}
     * parameter will be ignored.
     *
     * @param guildId the ID of the guild the bank account is scoped to.
     * @param userId the ID of the user to get the bank account.
     * @return A new {@link BankAccount} instance, or {@code null} if not found.
     */
    BankAccount get(long userId, long guildId);

    /**
     * Sets new values to the bank account of the given user in the given guild.
     * <p>
     * <b>Note:</b> For global economies like {@link CurrencyType#OFICINA},
     * the {@code guildId} parameter is ignored.
     *
     * @param guildId the ID of the guild the bank account is scoped to.
     * @param userId the ID of the user to set the new values.
     * @param cash the new cash amount to be set.
     * @param bank the new bank amount to be set.
     * @param reason the reason of the change.
     * @return the new {@link BankAccount} after the changes.
     */
    BankAccount set(long userId, long guildId, long cash, long bank, String reason);

    /**
     * Updates the bank account of the given user in the given guild.
     * <p>
     * This method will <b>UPDATE</b> the user's balance, not set new values.
     * Provide negative numbers to remove, or {@code 0} not to change a specified property.
     * <p>
     * If you want to <b>SET</b> new values to the user's balance,
     * use {@link #set(long, long, long, long, String)} instead.
     * <p>
     * <b>Note:</b> The {@code guildId} parameter is ignored for global economies like
     * {@link CurrencyType#OFICINA}.
     *
     * @param userId the ID of the user to have its balance updated.
     * @param cash the amount to update in cash.
     * @param bank the amount to update in the bank.
     * @param reason the reason of the change.
     * @return a new {@link BankAccount} instance after the changes.
     */
    BankAccount update(long userId, long guildId, long cash, long bank, String reason);

    // ---------- Global ----------
    /**
     * Fetches the bank account of the user with the provided ID globally.
     * <p>
     * <b>Note:</b> This method only works on global economies like {@link CurrencyType#OFICINA}.
     * If the economy being used is guild-based, an {@link UnsupportedOperationException}
     * will be thrown.
     *
     * @param userId the ID of the user to get the bank account.
     * @return the bank account of the user.
     * @throws UnsupportedOperationException if the economy is guild-based.
     */
    default BankAccount get(long userId) {
        return get(userId, 0);
    }

    /**
     * Sets new values to the {@link BankAccount} of the given user globally.
     * <p>
     * This method overrides any previous values set, if you want to <i>update</i>
     * their balance, use {@link #update(long, long, long, String)} instead.
     * <p>
     * <b>Note:</b> This method only works on global economies like {@link CurrencyType#OFICINA}.
     * If the economy being used is guild-based, an {@link UnsupportedOperationException}
     * will be thrown.
     *
     * @param userId the ID of the user to set the new values.
     * @param cash the new cash amount to be set.
     * @param bank the new bank amount to be set.
     * @param reason the reason of the change.
     * @return the new {@link BankAccount} after the changes.
     * @throws UnsupportedOperationException if the economy is guild-based.
     */
    default BankAccount set(long userId, long cash, long bank, String reason) {
        return set(userId, 0, cash, bank, reason);
    }

    /**
     * Updates the bank account of the given user globally.
     * <p>
     * This method will <b>UPDATE</b> the user's balance, not set new values.
     * Provide negative numbers to remove, or {@code 0} not to change a specified property.
     * <p>
     * If you want to <b>SET</b> new values to the user's balance,
     * use {@link #set(long, long, long, String)} instead.
     * <p>
     * <b>Note:</b> This method only works on global economies like {@link CurrencyType#OFICINA}.
     * If the economy being used is guild-based, an {@link UnsupportedOperationException}
     * will be thrown.
     *
     * @param userId the ID of the user to have its balance updated.
     * @param cash the amount to update in cash.
     * @param bank the amount to update in the bank.
     * @param reason the reason of the change.
     * @return a new {@link BankAccount} instance after the changes.
     * @throws UnsupportedOperationException if the economy is guild-based.
     */
    default BankAccount update(long userId, long cash, long bank, String reason) {
        return update(userId, 0, cash, bank, reason);
    }

    CurrencyType getCurrencyType();

    /**
     * Attempts to charge a member the given amount.
     * <p>
     * This is sort of a utility method, where it attemps to charge the member,
     * but if the new resulting values are negative, the process is rolled back
     * and {@code false} is returned.
     * <p>
     * Keep in mind that, while providing negative values are not allowed,
     * if {@code 0} is provided for both {@code cash} and {@code bank},
     * then {@code true} will be returned immediately.
     *
     * @param userId the id of the user to have its balance updated.
     * @param guildId the id of the guild this economy is scoped to, if not global.
     * @param cash the amount to be removed from the {@code cash}.
     * @param bank the amount to be removed from the {@code bank}.
     * @param reason the reason of the amount charged.
     * @return {@code true} if we are able to charge the member,
     *         {@code false} if they don't have enough money for this.
     * @throws IllegalArgumentException if {@code cash} or {@code bank} are negative.
     * @implNote The real implementation of this method may differ, some economies may
     *           first fetch the bank account before trying to update,
     *           while others may first try to update and then revert it
     *           if negative values are encoutered.
     *           Implementations shall never check the total, instead,
     *           if either {@code cash} OR {@code bank} end up negative, the operation should be reverted.
     */
    BankAction charge(long userId, long guildId, long cash, long bank, String reason);

    /**
     * Attempts to charge a member the given amount.
     * <p>
     * This is sort of a utility method, where it attemps to charge the member,
     * but if the new resulting values are negative, the process is rolled back
     * and {@code false} is returned.
     * <p>
     * Keep in mind that, while providing negative values are not allowed,
     * if {@code 0} is provided for both {@code cash} and {@code bank},
     * then {@code true} will be returned immediately.
     *
     * @param userId the id of the user to have its balance updated.
     * @param guildId the id of the guild this economy is scoped to, if not global.
     * @param cash the amount to be removed from the {@code cash}.
     * @param bank the amount to be removed from the {@code bank}.
     * @return {@code true} if we are able to charge the member,
     *         {@code false} if they don't have enough money for this.
     * @throws IllegalArgumentException if {@code cash} or {@code bank} are negative.
     * @implNote The real implementation of this method may differ, for instance,
     *           economies like {@link CurrencyType#OFICINA} will first fetch
     *           the bank account before trying to update.
     *           While economies like {@link CurrencyType#UNBELIEVABOAT} will
     *           send a PATCH request and wait for the response, if a negative
     *           balance is returned, another request is sent to revert the action
     *           (we don't check the total, that is, if either {@code cash} OR {@code bank}
     *           end up negative, the action is reverted).
     */
    default BankAction charge(long userId, long guildId, long cash, long bank) {
        return charge(userId, guildId, cash, bank, null);
    }

    /**
     * Attempts to charge a member the given amount.
     * <p>
     * This is sort of a utility method, where it attemps to charge the member,
     * but if the new resulting values are negative, the process is rolled back
     * and {@code false} is returned.
     * <p>
     * Keep in mind that, while providing negative values are not allowed,
     * if {@code 0} is provided for both {@code cash} and {@code bank},
     * then {@code true} will be returned immediately.
     *
     * @param userId the id of the user to have its balance updated.
     * @param cash the amount to be removed from the {@code cash}.
     * @param bank the amount to be removed from the {@code bank}.
     * @param reason the reason of the amount charged.
     * @return {@code true} if we are able to charge the member,
     *         {@code false} if they don't have enough money for this.
     * @throws IllegalArgumentException if {@code cash} or {@code bank} are negative.
     * @throws UnsupportedOperationException if the economy is guild-based.
     * @implNote The real implementation of this method may differ, for instance,
     *           economies like {@link CurrencyType#OFICINA} will first fetch
     *           the bank account before trying to update.
     *           While economies like {@link CurrencyType#UNBELIEVABOAT} will
     *           send a PATCH request and wait for the response, if a negative
     *           balance is returned, another request is sent to revert the action
     *           (we don't check the total, that is, if either {@code cash} OR {@code bank}
     *           end up negative, the action is reverted).
     */
    default BankAction charge(long userId, long cash, long bank, String reason) {
        return charge(userId, 0, cash, bank, reason);
    }

    /**
     * Attempts to charge a member the given amount.
     * <p>
     * This is sort of a utility method, where it attemps to charge the member,
     * but if the new resulting values are negative, the process is rolled back
     * and {@code false} is returned.
     * <p>
     * Keep in mind that, while providing negative values are not allowed,
     * if {@code 0} is provided for both {@code cash} and {@code bank},
     * then {@code true} will be returned immediately.
     *
     * @param userId the id of the user to have its balance updated.
     * @param cash the amount to be removed from the {@code cash}.
     * @param bank the amount to be removed from the {@code bank}.
     * @return {@code true} if we are able to charge the member,
     *         {@code false} if they don't have enough money for this.
     * @throws IllegalArgumentException if {@code cash} or {@code bank} are negative.
     * @throws UnsupportedOperationException if the economy is guild-based.
     * @implNote The real implementation of this method may differ, for instance,
     *           economies like {@link CurrencyType#OFICINA} will first fetch
     *           the bank account before trying to update.
     *           While economies like {@link CurrencyType#UNBELIEVABOAT} will
     *           send a PATCH request and wait for the response, if a negative
     *           balance is returned, another request is sent to revert the action
     *           (we don't check the total, that is, if either {@code cash} OR {@code bank}
     *           end up negative, the action is reverted).
     */
    default BankAction charge(long userId, long cash, long bank) {
        return charge(userId, 0, cash, bank, null);
    }

    // ---------- Utils ----------
    /**
     * Sets new values to the provided {@link BankAccount}.
     * <p>
     * If the economy does not allow you to provide a reason for this change,
     * then the {@code reason} parameter is ignored.
     *
     * @param newVal the new values to be set.
     * @param reason the reason of the change.
     * @return the new {@link BankAccount} after the changes.
     */
    default BankAccount save(BankAccount newVal, String reason) {
        if (newVal.isDummy())
            throw new IllegalArgumentException("Cannot modify a dummy account");

        return set(newVal.getGuildId(), newVal.getUserId(), newVal.getCash(), newVal.getBank(), reason);
    }

    /**
     * Sets new values to the provided {@link BankAccount}.
     * <p>
     * If the economy allows you to provide a reason for this call,
     * use {@link #save(BankAccount, String)} instead.
     * If it does not, then the {@code reason} parameter is ignored.
     *
     * @param newVal the new values to be set.
     * @return the new {@link BankAccount} after the changes.
     */
    default BankAccount save(BankAccount newVal) {
        if (newVal.isDummy())
            throw new IllegalArgumentException("Cannot modify a dummy account");

        return save(newVal, null);
    }

    /**
     * Returns an <b>IMMUTABLE</b> dummy bank account with maxed values.
     * <p>
     * Keep in mind that this account cannot be used on methods such as
     * {@link #save(BankAccount)}, as it does not exist.
     * <p>
     * Calling {@link BankAccount#getRank()} on this account will always return {@code 1}.
     *
     * @return A dummy account with all bank/cash/total values maxed.
     */
    default BankAccount max() {
        return new BankAccount() {
            public long getUserId() { return 0; }
            public long getGuildId() { return 0; }
            public long getCash() { return Long.MAX_VALUE; }
            public long getBank() { return Long.MAX_VALUE; }
            public long getTotal() { return Long.MAX_VALUE; }
            public int getRank() { return 1; }
            public CurrencyType getType() { return getCurrencyType(); }
            public BankAccount setCash(long cash) { return this; }
            public BankAccount modifyCash(long cash) { return this; }
            public BankAccount setBank(long bank) { return this; }
            public BankAccount modifyBank(long bank) { return this; }
            public boolean isDummy() { return true; }
        };
    }

    /**
     * Returns an <b>IMMUTABLE</b> dummy bank account with minimum values.
     * <p>
     * Keep in mind that this account cannot be used on methods such as
     * {@link #save(BankAccount)}, as it does not exist.
     * <p>
     * Calling {@link BankAccount#getRank()} on this account will always
     * return {@link Integer#MAX_VALUE}.
     *
     * @return A dummy account with the smallest bank/cash/total values.
     */
    default BankAccount min() {
        return new BankAccount() {
            public long getUserId() { return 0; }
            public long getGuildId() { return 0; }
            public long getCash() { return Long.MIN_VALUE; }
            public long getBank() { return Long.MIN_VALUE; }
            public long getTotal() { return Long.MIN_VALUE; }
            public int getRank() { return Integer.MAX_VALUE; }
            public CurrencyType getType() { return getCurrencyType(); }
            public BankAccount setCash(long cash) { return this; }
            public BankAccount modifyCash(long cash) { return this; }
            public BankAccount setBank(long bank) { return this; }
            public BankAccount modifyBank(long bank) { return this; }
            public boolean isDummy() { return true; }
        };
    }
}