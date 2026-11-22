package dev.dhanush.ledgercore.journal;

import dev.dhanush.ledgercore.account.Account;
import dev.dhanush.ledgercore.account.AccountRepository;
import dev.dhanush.ledgercore.account.AccountType;
import dev.dhanush.ledgercore.support.PostgresIntegrationTestBase;
import dev.dhanush.ledgercore.support.TestData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Proves that the ledger invariants hold in PostgreSQL itself, even if the service layer is bypassed. */
@Testcontainers(disabledWithoutDocker = true)
class LedgerInvariantsPostgresTest extends PostgresIntegrationTestBase {

    @Autowired AccountRepository accounts;
    @Autowired JournalPostingService posting;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager txManager;

    private void insertEntry(UUID entryId, String currency) {
        jdbc.update("insert into journal_entries(id, description, currency, status, posted_at) values (?, ?, ?, 'POSTED', now())",
                entryId, "raw insert", currency);
    }

    private void insertLine(UUID entryId, UUID accountId, int lineNo, String direction, String amount) {
        jdbc.update("insert into journal_lines(id, journal_entry_id, account_id, line_number, direction, amount) values (?, ?, ?, ?, ?, ?)",
                UUID.randomUUID(), entryId, accountId, lineNo, direction, new BigDecimal(amount));
    }

    @Test
    void postsBalancedEntryAndReadsItBack() {
        Account cash = TestData.account(accounts, "CASH", AccountType.ASSET, "EUR");
        Account equity = TestData.account(accounts, "EQ", AccountType.EQUITY, "EUR");

        JournalDtos.EntryResponse posted = TestData.fund(posting, cash, equity, "125.00");
        JournalDtos.EntryResponse loaded = posting.get(posted.id());

        assertThat(loaded.lines()).hasSize(2);
        assertThat(loaded.lines().get(0).direction()).isEqualTo(PostingDirection.DEBIT);
        assertThat(loaded.lines().get(0).amount()).isEqualByComparingTo("125.00");
        assertThat(loaded.currency()).isEqualTo("EUR");
    }

    @Test
    void databaseRejectsUnbalancedEntryEvenWhenServiceIsBypassed() {
        Account cash = TestData.account(accounts, "CASH", AccountType.ASSET, "EUR");
        Account equity = TestData.account(accounts, "EQ", AccountType.EQUITY, "EUR");
        UUID entryId = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            insertEntry(entryId, "EUR");
            insertLine(entryId, cash.getId(), 1, "DEBIT", "10.00");
            insertLine(entryId, equity.getId(), 2, "CREDIT", "9.00");
            jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        })).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("not balanced");

        assertThat(jdbc.queryForObject("select count(*) from journal_entries where id = ?", Integer.class, entryId)).isZero();
    }

    @Test
    void databaseRejectsEntryWithoutLines() {
        UUID entryId = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            insertEntry(entryId, "EUR");
            jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        })).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("has no lines");
    }

    @Test
    void databaseRejectsMixedCurrencyEntry() {
        Account cash = TestData.account(accounts, "CASH", AccountType.ASSET, "EUR");
        Account usdEquity = TestData.account(accounts, "EQ", AccountType.EQUITY, "USD");
        UUID entryId = UUID.randomUUID();
        TransactionTemplate tx = new TransactionTemplate(txManager);

        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
            insertEntry(entryId, "EUR");
            insertLine(entryId, cash.getId(), 1, "DEBIT", "10.00");
            insertLine(entryId, usdEquity.getId(), 2, "CREDIT", "10.00");
            jdbc.execute("SET CONSTRAINTS ALL IMMEDIATE");
        })).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("mixes currencies");
    }

    @Test
    void postedJournalRowsAreAppendOnly() {
        Account cash = TestData.account(accounts, "CASH", AccountType.ASSET, "EUR");
        Account equity = TestData.account(accounts, "EQ", AccountType.EQUITY, "EUR");
        UUID entryId = TestData.fund(posting, cash, equity, "50.00").id();

        assertThatThrownBy(() -> jdbc.update("update journal_lines set amount = amount + 1 where journal_entry_id = ?", entryId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from journal_lines where journal_entry_id = ?", entryId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("update journal_entries set description = 'tampered' where id = ?", entryId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from journal_entries where id = ?", entryId))
                .isInstanceOf(DataIntegrityViolationException.class).hasMessageContaining("append-only");
    }
}
