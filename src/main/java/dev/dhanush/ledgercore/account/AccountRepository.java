package dev.dhanush.ledgercore.account;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {
    boolean existsByAccountCode(String accountCode);
}
