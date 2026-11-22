package dev.dhanush.ledgercore.account;

import dev.dhanush.ledgercore.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ledger_accounts")
public class Account extends BaseEntity {

    @Column(name = "account_code", nullable = false, unique = true, length = 40)
    private String accountCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Account() {
    }

    public Account(String accountCode, String name, String currency, AccountType accountType) {
        super(UUID.randomUUID());
        this.accountCode = accountCode;
        this.name = name;
        this.currency = currency;
        this.accountType = accountType;
        this.status = AccountStatus.ACTIVE;
        this.createdAt = Instant.now();
    }

    public String getAccountCode() { return accountCode; }
    public String getName() { return name; }
    public String getCurrency() { return currency; }
    public AccountType getAccountType() { return accountType; }
    public AccountStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void deactivate() {
        this.status = AccountStatus.INACTIVE;
    }
}
