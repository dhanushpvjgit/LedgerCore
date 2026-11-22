package dev.dhanush.ledgercore.journal;

import dev.dhanush.ledgercore.account.Account;
import dev.dhanush.ledgercore.account.AccountRepository;
import dev.dhanush.ledgercore.account.AccountStatus;
import dev.dhanush.ledgercore.common.BusinessRuleException;
import dev.dhanush.ledgercore.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Validates and persists balanced journal entries.
 * Rules: at least two lines, positive amounts (max 4 decimals), one currency, active accounts,
 * and total debits equal to total credits. The database re-checks balance and immutability.
 */
@Service
@Transactional
public class JournalPostingService {
    private final AccountRepository accounts;
    private final JournalEntryRepository entries;

    public JournalPostingService(AccountRepository accounts, JournalEntryRepository entries) {
        this.accounts = accounts;
        this.entries = entries;
    }

    public JournalDtos.EntryResponse post(JournalDtos.PostRequest request, String actor) {
        if (request.lines() == null || request.lines().size() < 2) {
            throw new BusinessRuleException("At least two journal lines are required");
        }

        BigDecimal debits = BigDecimal.ZERO;
        BigDecimal credits = BigDecimal.ZERO;
        String currency = null;
        List<Account> resolved = new ArrayList<>();

        for (JournalDtos.LineRequest line : request.lines()) {
            if (line.amount() == null || line.amount().signum() <= 0) {
                throw new BusinessRuleException("Amounts must be positive");
            }
            if (line.amount().stripTrailingZeros().scale() > 4) {
                throw new BusinessRuleException("Amounts support at most 4 decimal places");
            }
            if (line.direction() == null) {
                throw new BusinessRuleException("Posting direction is required");
            }
            Account account = accounts.findById(line.accountId())
                    .orElseThrow(() -> new NotFoundException("Account not found: " + line.accountId()));
            if (account.getStatus() != AccountStatus.ACTIVE) {
                throw new BusinessRuleException("Inactive account cannot be posted: " + account.getAccountCode());
            }
            if (currency == null) {
                currency = account.getCurrency();
            } else if (!currency.equals(account.getCurrency())) {
                throw new BusinessRuleException("All lines must use one currency");
            }
            if (line.direction() == PostingDirection.DEBIT) {
                debits = debits.add(line.amount());
            } else {
                credits = credits.add(line.amount());
            }
            resolved.add(account);
        }

        if (debits.compareTo(credits) != 0) {
            throw new BusinessRuleException("Debits must equal credits");
        }

        JournalEntry entry = new JournalEntry(request.description().trim(), request.reference(), currency, actor);
        for (int i = 0; i < request.lines().size(); i++) {
            JournalDtos.LineRequest l = request.lines().get(i);
            entry.addLine(new JournalLine(resolved.get(i), i + 1, l.direction(), l.amount(), l.description()));
        }
        return map(entries.save(entry));
    }

    @Transactional(readOnly = true)
    public JournalDtos.EntryResponse get(UUID id) {
        return entries.findById(id).map(this::map)
                .orElseThrow(() -> new NotFoundException("Journal entry not found: " + id));
    }

    private JournalDtos.EntryResponse map(JournalEntry e) {
        List<JournalDtos.LineResponse> lines = e.getLines().stream()
                .map(l -> new JournalDtos.LineResponse(l.getId(), l.getAccount().getId(),
                        l.getAccount().getAccountCode(), l.getAccount().getName(),
                        l.getDirection(), l.getAmount(), l.getDescription()))
                .toList();
        return new JournalDtos.EntryResponse(e.getId(), e.getDescription(), e.getReference(),
                e.getCurrency(), e.getStatus(), e.getPostedAt(), lines);
    }
}
