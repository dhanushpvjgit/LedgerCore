package dev.dhanush.ledgercore.journal;

import dev.dhanush.ledgercore.common.BaseEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** An immutable, balanced accounting event. Rows are append-only (enforced by a database trigger). */
@Entity
@Table(name = "journal_entries")
public class JournalEntry extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String description;

    @Column(length = 80)
    private String reference;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "posted_at", nullable = false)
    private Instant postedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @OrderBy("lineNumber ASC")
    private List<JournalLine> lines = new ArrayList<>();

    protected JournalEntry() {
    }

    public JournalEntry(String description, String reference, String currency, String createdBy) {
        super(UUID.randomUUID());
        this.description = description;
        this.reference = reference;
        this.currency = currency;
        this.status = "POSTED";
        // PostgreSQL stores microseconds; truncate so in-memory and persisted values agree.
        this.postedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        this.createdBy = createdBy;
    }

    public void addLine(JournalLine line) {
        lines.add(line);
        line.attach(this);
    }

    public String getDescription() { return description; }
    public String getReference() { return reference; }
    public String getCurrency() { return currency; }
    public String getStatus() { return status; }
    public Instant getPostedAt() { return postedAt; }
    public String getCreatedBy() { return createdBy; }
    public List<JournalLine> getLines() { return Collections.unmodifiableList(lines); }
}
