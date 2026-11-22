package dev.dhanush.ledgercore.account;

public enum AccountType {
    ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE;

    /** Debit-normal accounts grow with debits; credit-normal accounts grow with credits. */
    public boolean isDebitNormal() {
        return this == ASSET || this == EXPENSE;
    }
}
