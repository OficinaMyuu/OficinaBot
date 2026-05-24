package ofc.bot.handlers.economy.oficina;

import ofc.bot.domain.entity.UserEconomy;
import ofc.bot.domain.sqlite.repository.Repositories;
import ofc.bot.domain.sqlite.repository.UserEconomyRepository;
import ofc.bot.handlers.economy.BankAccount;
import ofc.bot.handlers.economy.CurrencyType;

public class OficinaAccount implements BankAccount {
    private final long userId;
    private int wallet;
    private int bank;

    protected OficinaAccount(long userId, int wallet, int bank) {
        this.userId = userId;
        this.wallet = wallet;
        this.bank = bank;
    }

    protected static OficinaAccount from(UserEconomy eco) {
        return new OficinaAccount(eco.getUserId(), eco.getWallet(), eco.getBank());
    }

    @Override
    public long getUserId() {
        return this.userId;
    }

    @Override
    public long getGuildId() {
        return 0L;
    }

    @Override
    public long getCash() {
        return this.wallet;
    }

    @Override
    public long getBank() {
        return this.bank;
    }

    @Override
    public long getTotal() {
        return this.wallet + this.bank;
    }

    @Override
    public int getRank() {
        UserEconomyRepository ecoRepo = Repositories.getUserEconomyRepository();
        return ecoRepo.findRankByUserId(userId);
    }

    @Override
    public CurrencyType getType() {
        return CurrencyType.OFICINA;
    }

    @Override
    public BankAccount setCash(long cash) {
        if (cash < Integer.MIN_VALUE || cash > Integer.MAX_VALUE)
            throw new IllegalArgumentException("This economy only supports 32-bits values, provided: " + cash);

        this.wallet = (int) cash;
        return this;
    }

    @Override
    public BankAccount modifyCash(long cash) {
        long result = this.wallet + cash;

        if (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE)
            throw new UnsupportedOperationException(String.format(
                    "This economy only supports 32-bits values, but the result of (%d + %d) is %d",
                    this.wallet, cash, result));

        this.wallet = (int) result;
        return this;
    }

    @Override
    public BankAccount setBank(long bank) {
        if (bank < Integer.MIN_VALUE || bank > Integer.MAX_VALUE)
            throw new IllegalArgumentException("This economy only supports 32-bits values, provided: " + bank);

        this.bank = (int) bank;
        return this;
    }

    @Override
    public BankAccount modifyBank(long bank) {
        long result = this.bank + bank;

        if (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE)
            throw new UnsupportedOperationException(String.format(
                    "This economy only supports 32-bits values, but the result of (%d + %d) is %d",
                    this.bank, bank, result));

        this.bank = (int) result;
        return this;
    }

    @Override
    public boolean isDummy() {
        return false;
    }
}