package ofc.bot.handlers.economy.unb;

import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.CurrencyType;

// The values are assigned by Gson when an HTTP request is done
public class UnbelievaAccount implements BankAccount {
    private final String user_id;
    private String rank;
    private long guildId;
    private long cash;
    private long bank;
    private long total;

    public UnbelievaAccount(String userId, long guildId, long cash, long bank) {
        this.user_id = userId;
        this.guildId = guildId;
        this.cash = cash;
        this.bank = bank;
    }

    protected void setGuildId(long guildId) {
        this.guildId = guildId;
    }

    @Override
    public long getUserId() {
        return Long.parseLong(this.user_id);
    }

    @Override
    public long getGuildId() {
        return this.guildId;
    }

    @Override
    public long getCash() {
        return this.cash;
    }

    @Override
    public long getBank() {
        return this.bank;
    }

    @Override
    public long getTotal() {
        return this.total;
    }

    @Override
    public int getRank() {
        return Integer.parseInt(this.rank);
    }

    @Override
    public CurrencyType getType() {
        return CurrencyType.UNBELIEVABOAT;
    }

    @Override
    public BankAccount setCash(long cash) {
        this.cash = cash;
        return this;
    }

    @Override
    public BankAccount modifyCash(long cash) {
        this.cash += cash;
        return this;
    }

    @Override
    public BankAccount setBank(long bank) {
        this.bank = bank;
        return this;
    }

    @Override
    public BankAccount modifyBank(long bank) {
        this.bank += bank;
        return this;
    }

    @Override
    public boolean isDummy() {
        return false;
    }
}