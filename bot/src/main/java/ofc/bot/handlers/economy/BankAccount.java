package ofc.bot.handlers.economy;

import ofc.bot.util.Bot;

public interface BankAccount {

    /**
     * The id of the user responsible for this account.
     *
     * @return the id of the user responsible for this account.
     */
    long getUserId();

    /**
     * The id of the guild this bank account is scoped to.
     * <p>
     * This method returns {@code 0} if the economy is global.
     *
     * @return the id of the {@link net.dv8tion.jda.api.entities.Guild Guild},
     * {@code 0} otherwise.
     */
    long getGuildId();

    /**
     * The amount in cash the user has, that is, the money you have
     * in your pocket or wallet.
     * <p>
     * Depending on the {@link CurrencyType}, you can be stolen
     * if your money is left on the cash.
     * <p>
     * On global economies like {@link CurrencyType#OFICINA}, this method will return {@code 0}.
     *
     * @return the amount the user has in cash.
     */
    long getCash();

    /**
     * The amount of money the user has deposited in their bank account.
     * <p>
     * In economies of type {@link CurrencyType#UNBELIEVABOAT}, this is the
     * money you are safe with. Users cannot rob it.
     *
     * @return the amount the user has in the bank.
     */
    long getBank();

    /**
     * The total amount of money, that is, a sum of {@code getBank() + getCash()}.
     * <p>
     * In the {@link CurrencyType#OFICINA} economy, this method returns the same
     * value as {@link #getBank()}.
     *
     * @return the total amount of money the user has.
     */
    long getTotal();

    /**
     * Finds the rank of the user.
     * <p>
     * In economies like {@link CurrencyType#OFICINA}, this is a heavier method to call,
     * since it has to lookup in the database for the user's rank.
     * Additionally, for this economy, if the rank of the user cannot be resolved
     * (maybe they do not exist in the database), then {@link Integer#MAX_VALUE} is returned.
     * <p>
     * Whereas accounts from {@link CurrencyType#UNBELIEVABOAT} will return the value
     * immediately, since this property is loaded when the request is received.
     *
     * @return the user's rank in the given economy.
     */
    int getRank();

    /**
     * Returns the "pretty" rank of the current user.
     * For instance, if the user's rank is {@code 1582}, this method will return:
     * {@code #1.582}.
     * <p>
     * Keep in mind that this method relies on {@link #getRank()},
     * so all the rules and limitations also apply here.
     *
     * @return the formatted rank of the user.
     */
    default String getRankf() {
        return String.format("#%s", Bot.fmtNum(this.getRank()));
    }

    /**
     * Returns the underlying economy implementation being used.
     *
     * @return the type of the economy currently in use for this account.
     */
    CurrencyType getType();

    /**
     * Defines a new amount of cash for this current bank account.
     * <p>
     * In economies like {@link CurrencyType#OFICINA}, this is a no-op method.
     *
     * @param cash the new amount of cash you want to <b><u>set</u></b>.
     * @return the current bank account, for chaining convenience.
     * @see #modifyCash(long)
     */
    BankAccount setCash(long cash);

    /**
     * Modifies the amount of cash for this current bank account.
     * <p>
     * In economies like {@link CurrencyType#OFICINA}, this is a no-op method.
     *
     * @param cash the amount of cash you want to <b><u>update</u></b>.
     * @return the current bank account, for chaining convenience.
     * @see #setCash(long)
     */
    BankAccount modifyCash(long cash);

    /**
     * Defines a new amount of money in the bank for this current bank account.
     *
     * @param bank the new amount of money in the bank you want to <b><u>set</u></b>.
     * @return the current bank account, for chaining convenience.
     */
    BankAccount setBank(long bank);

    /**
     * Modifies the amount of money in the bank for this current bank account.
     *
     * @param bank the amount of money in the bank you want to <b><u>update</u></b>.
     * @return the current bank account, for chaining convenience.
     */
    BankAccount modifyBank(long bank);

    boolean isDummy();
}