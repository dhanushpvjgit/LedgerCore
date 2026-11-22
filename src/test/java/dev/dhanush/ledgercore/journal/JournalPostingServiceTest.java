package dev.dhanush.ledgercore.journal;

import dev.dhanush.ledgercore.account.Account;
import dev.dhanush.ledgercore.account.AccountRepository;
import dev.dhanush.ledgercore.account.AccountType;
import dev.dhanush.ledgercore.common.BusinessRuleException;
import dev.dhanush.ledgercore.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalPostingServiceTest {

    @Mock AccountRepository accounts;
    @Mock JournalEntryRepository entries;
    @InjectMocks JournalPostingService service;

    private Account cash() { return new Account("CASH-1", "Cash", "EUR", AccountType.ASSET); }
    private Account equity() { return new Account("EQ-1", "Equity", "EUR", AccountType.EQUITY); }

    private JournalDtos.PostRequest request(Account debit, String debitAmount, Account credit, String creditAmount) {
        return new JournalDtos.PostRequest("Test entry", "REF-1", List.of(
                new JournalDtos.LineRequest(debit.getId(), PostingDirection.DEBIT, new BigDecimal(debitAmount), "debit"),
                new JournalDtos.LineRequest(credit.getId(), PostingDirection.CREDIT, new BigDecimal(creditAmount), "credit")));
    }

    @Test
    void postsBalancedEntry() {
        Account a = cash(), b = equity();
        when(accounts.findById(a.getId())).thenReturn(Optional.of(a));
        when(accounts.findById(b.getId())).thenReturn(Optional.of(b));
        when(entries.save(any(JournalEntry.class))).thenAnswer(i -> i.getArgument(0));

        JournalDtos.EntryResponse result = service.post(request(a, "20.00", b, "20.00"), "tester");

        assertEquals(2, result.lines().size());
        assertEquals("EUR", result.currency());
        assertEquals("POSTED", result.status());
        assertEquals(PostingDirection.DEBIT, result.lines().get(0).direction());
        assertEquals(PostingDirection.CREDIT, result.lines().get(1).direction());
    }

    @Test
    void rejectsUnbalancedEntry() {
        Account a = cash(), b = equity();
        when(accounts.findById(a.getId())).thenReturn(Optional.of(a));
        when(accounts.findById(b.getId())).thenReturn(Optional.of(b));

        assertThrows(BusinessRuleException.class, () -> service.post(request(a, "20.00", b, "19.99"), "tester"));
        verify(entries, never()).save(any());
    }

    @Test
    void rejectsSingleLineEntry() {
        Account a = cash();
        var single = new JournalDtos.PostRequest("One line", null, List.of(
                new JournalDtos.LineRequest(a.getId(), PostingDirection.DEBIT, new BigDecimal("1.00"), null)));
        assertThrows(BusinessRuleException.class, () -> service.post(single, "tester"));
    }

    @Test
    void rejectsInactiveAccount() {
        Account a = cash(), b = equity();
        a.deactivate();
        when(accounts.findById(a.getId())).thenReturn(Optional.of(a));
        assertThrows(BusinessRuleException.class, () -> service.post(request(a, "5.00", b, "5.00"), "tester"));
    }

    @Test
    void rejectsMixedCurrencies() {
        Account a = cash();
        Account usd = new Account("EQ-USD", "Equity USD", "USD", AccountType.EQUITY);
        when(accounts.findById(a.getId())).thenReturn(Optional.of(a));
        when(accounts.findById(usd.getId())).thenReturn(Optional.of(usd));
        assertThrows(BusinessRuleException.class, () -> service.post(request(a, "5.00", usd, "5.00"), "tester"));
    }

    @Test
    void rejectsMoreThanFourDecimalPlaces() {
        Account a = cash(), b = equity();
        assertThrows(BusinessRuleException.class, () -> service.post(request(a, "1.00001", b, "1.00001"), "tester"));
    }

    @Test
    void rejectsUnknownAccount() {
        Account a = cash(), b = equity();
        when(accounts.findById(a.getId())).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.post(request(a, "5.00", b, "5.00"), "tester"));
    }

    @Test
    void getThrowsWhenEntryMissing() {
        UUID id = UUID.randomUUID();
        when(entries.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(id));
    }
}
