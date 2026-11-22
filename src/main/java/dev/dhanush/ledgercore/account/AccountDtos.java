package dev.dhanush.ledgercore.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class AccountDtos {
    private AccountDtos() {
    }

    public record CreateRequest(
            @NotBlank @Size(max = 40) String accountCode,
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter upper-case ISO currency code") String currency,
            @NotNull AccountType accountType) {
    }

    public record Response(UUID id, String accountCode, String name, String currency,
                           AccountType accountType, AccountStatus status, Instant createdAt) {
    }
}
