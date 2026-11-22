package dev.dhanush.ledgercore.account;

import dev.dhanush.ledgercore.common.ConflictException;
import dev.dhanush.ledgercore.common.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock AccountRepository repository;
    @InjectMocks AccountService service;

    @Test
    void createNormalisesAccountCode() {
        when(repository.existsByAccountCode("CASH-001")).thenReturn(false);
        when(repository.saveAndFlush(any(Account.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.create(new AccountDtos.CreateRequest(" cash-001 ", " Operating Cash ", "EUR", AccountType.ASSET));

        assertEquals("CASH-001", response.accountCode());
        assertEquals("Operating Cash", response.name());
        assertEquals(AccountStatus.ACTIVE, response.status());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(repository.existsByAccountCode("CASH-001")).thenReturn(true);
        assertThrows(ConflictException.class,
                () -> service.create(new AccountDtos.CreateRequest("cash-001", "Cash", "EUR", AccountType.ASSET)));
        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void deactivateMarksAccountInactive() {
        Account account = new Account("A-1", "A", "EUR", AccountType.ASSET);
        when(repository.findById(account.getId())).thenReturn(Optional.of(account));
        assertEquals(AccountStatus.INACTIVE, service.deactivate(account.getId()).status());
    }

    @Test
    void getThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.get(id));
    }
}
