# Week 1: schema, accounts, journal posting, balanced-entry invariants

**Goal:** a correct accounting core before any distributed-systems machinery is added.

## Delivered
- Flyway `V1`: accounts, journal entries, journal lines with CHECK constraints (positive amounts, valid direction, ISO currency).
- Flyway `V2`: database-enforced invariants
  - journal rows are append-only (UPDATE / DELETE / TRUNCATE rejected by triggers);
  - deferred constraint trigger: every entry has at least two lines, debits = credits, and each line's account currency matches the entry currency.
- Account API (create / get / list / deactivate) and journal API (post / get).
- Service-level validation: at least 2 lines, positive amounts with at most 4 decimals, single currency, active accounts, balanced entry.
- Uniform JSON error model (400 / 404 / 409 / 422).

## Why the invariants live in the database too
Application validation can be bypassed by a bug, a second writer or a manual SQL session. A deferred constraint trigger runs at COMMIT, after all lines of an entry are inserted, so an unbalanced entry can never become visible. `LedgerInvariantsPostgresTest` proves this by bypassing the service entirely.

## Verify
```bash
mvn clean verify            # unit tests + Testcontainers tests (needs Docker for the latter)
```

## Interview prompts
- Why `NUMERIC(19,4)` and `BigDecimal` rather than `double`?
- Why a *deferred* constraint trigger instead of a CHECK constraint?
- Why is the journal append-only, and how are mistakes corrected? (reversing entries, see Week 4)
