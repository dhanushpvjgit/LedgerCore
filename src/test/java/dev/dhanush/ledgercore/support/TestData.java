package dev.dhanush.ledgercore.support;

import dev.dhanush.ledgercore.account.Account;
import dev.dhanush.ledgercore.account.AccountRepository;
import dev.dhanush.ledgercore.account.AccountType;
import dev.dhanush.ledgercore.journal.JournalDtos;
import dev.dhanush.ledgercore.journal.JournalPostingService;
import dev.dhanush.ledgercore.journal.PostingDirection;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Small helpers shared by integration tests. */
public final class TestData {
    private TestData() {
    }

    public static Account account(AccountRepository repository, String prefix, AccountType type, String currency) {
        String code = (prefix + "-" + UUID.randomUUID().toString().substring(0, 8)).toUpperCase();
        return repository.saveAndFlush(new Account(code, prefix + " account", currency, type));
    }

    /** Debit {@code asset} / credit {@code equity}, i.e. put money into an asset account. */
    public static JournalDtos.EntryResponse fund(JournalPostingService posting, Account asset, Account equity, String amount) {
        return posting.post(new JournalDtos.PostRequest("Funding", null, List.of(
                new JournalDtos.LineRequest(asset.getId(), PostingDirection.DEBIT, new BigDecimal(amount), "funding"),
                new JournalDtos.LineRequest(equity.getId(), PostingDirection.CREDIT, new BigDecimal(amount), "capital"))),
                "test");
    }
}
