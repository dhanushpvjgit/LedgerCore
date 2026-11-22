package dev.dhanush.ledgercore.account;

import dev.dhanush.ledgercore.common.ConflictException;
import dev.dhanush.ledgercore.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AccountService {
    private final AccountRepository repository;

    public AccountService(AccountRepository repository) {
        this.repository = repository;
    }

    public AccountDtos.Response create(AccountDtos.CreateRequest request) {
        String code = request.accountCode().trim().toUpperCase(Locale.ROOT);
        if (repository.existsByAccountCode(code)) {
            throw new ConflictException("Account code already exists: " + code);
        }
        Account account = new Account(code, request.name().trim(), request.currency(), request.accountType());
        return map(repository.saveAndFlush(account));
    }

    public AccountDtos.Response deactivate(UUID id) {
        Account account = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
        account.deactivate();
        repository.flush();
        return map(account);
    }

    @Transactional(readOnly = true)
    public AccountDtos.Response get(UUID id) {
        return repository.findById(id).map(this::map)
                .orElseThrow(() -> new NotFoundException("Account not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<AccountDtos.Response> list() {
        return repository.findAll().stream().map(this::map).toList();
    }

    private AccountDtos.Response map(Account a) {
        return new AccountDtos.Response(a.getId(), a.getAccountCode(), a.getName(), a.getCurrency(),
                a.getAccountType(), a.getStatus(), a.getCreatedAt());
    }
}
