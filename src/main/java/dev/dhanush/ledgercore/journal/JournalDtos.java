package dev.dhanush.ledgercore.journal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class JournalDtos {
    private JournalDtos() {
    }

    public record LineRequest(
            @NotNull UUID accountId,
            @NotNull PostingDirection direction,
            @NotNull @DecimalMin("0.0001") @Digits(integer = 15, fraction = 4) BigDecimal amount,
            @Size(max = 255) String description) {
    }

    public record PostRequest(
            @NotBlank @Size(max = 255) String description,
            @Size(max = 80) String reference,
            @NotEmpty List<@Valid LineRequest> lines) {
    }

    public record LineResponse(UUID id, UUID accountId, String accountCode, String accountName,
                               PostingDirection direction, BigDecimal amount, String description) {
    }

    public record EntryResponse(UUID id, String description, String reference, String currency,
                                String status, Instant postedAt, List<LineResponse> lines) {
    }
}
