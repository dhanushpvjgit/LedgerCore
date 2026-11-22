package dev.dhanush.ledgercore.journal;

import dev.dhanush.ledgercore.account.Account;
import dev.dhanush.ledgercore.common.BaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "journal_lines")
public class JournalLine extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry entry;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id")
    private Account account;

    @Column(name = "line_number", nullable = false)
    private int lineNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PostingDirection direction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    protected JournalLine() {
    }

    public JournalLine(Account account, int lineNumber, PostingDirection direction, BigDecimal amount, String description) {
        super(UUID.randomUUID());
        this.account = account;
        this.lineNumber = lineNumber;
        this.direction = direction;
        this.amount = amount;
        this.description = description;
    }

    void attach(JournalEntry entry) {
        this.entry = entry;
    }

    public JournalEntry getEntry() { return entry; }
    public Account getAccount() { return account; }
    public int getLineNumber() { return lineNumber; }
    public PostingDirection getDirection() { return direction; }
    public BigDecimal getAmount() { return amount; }
    public String getDescription() { return description; }
}
