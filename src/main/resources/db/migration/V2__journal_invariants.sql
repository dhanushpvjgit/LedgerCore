-- Week 1: database-enforced ledger invariants.
--   1. Journal rows are append-only (no UPDATE / DELETE / TRUNCATE).
--   2. Every journal entry has >= 2 lines, is balanced (debits = credits) and
--      every line's account currency matches the entry currency.
-- The balance checks are DEFERRED constraint triggers: they run at COMMIT, after all
-- lines of the entry were inserted, so an unbalanced entry can never be committed.

CREATE OR REPLACE FUNCTION ledger_reject_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'Table % is append-only: % is not allowed', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'integrity_constraint_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_entries_append_only
    BEFORE UPDATE OR DELETE ON journal_entries
    FOR EACH ROW EXECUTE FUNCTION ledger_reject_mutation();

CREATE TRIGGER trg_journal_lines_append_only
    BEFORE UPDATE OR DELETE ON journal_lines
    FOR EACH ROW EXECUTE FUNCTION ledger_reject_mutation();

CREATE TRIGGER trg_journal_entries_no_truncate
    BEFORE TRUNCATE ON journal_entries
    FOR EACH STATEMENT EXECUTE FUNCTION ledger_reject_mutation();

CREATE TRIGGER trg_journal_lines_no_truncate
    BEFORE TRUNCATE ON journal_lines
    FOR EACH STATEMENT EXECUTE FUNCTION ledger_reject_mutation();

CREATE OR REPLACE FUNCTION ledger_assert_entry_balanced() RETURNS trigger AS $$
DECLARE
    diff       NUMERIC;
    line_count INTEGER;
BEGIN
    SELECT COALESCE(SUM(CASE direction WHEN 'DEBIT' THEN amount ELSE -amount END), 0), COUNT(*)
      INTO diff, line_count
      FROM journal_lines
     WHERE journal_entry_id = NEW.journal_entry_id;

    IF line_count < 2 THEN
        RAISE EXCEPTION 'Journal entry % must have at least two lines', NEW.journal_entry_id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF diff <> 0 THEN
        RAISE EXCEPTION 'Journal entry % is not balanced (debits - credits = %)', NEW.journal_entry_id, diff
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    IF EXISTS (SELECT 1
                 FROM journal_lines jl
                 JOIN ledger_accounts a ON a.id = jl.account_id
                 JOIN journal_entries e ON e.id = jl.journal_entry_id
                WHERE jl.journal_entry_id = NEW.journal_entry_id
                  AND a.currency <> e.currency) THEN
        RAISE EXCEPTION 'Journal entry % mixes currencies', NEW.journal_entry_id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_journal_lines_balanced
    AFTER INSERT ON journal_lines
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger_assert_entry_balanced();

CREATE OR REPLACE FUNCTION ledger_assert_entry_has_lines() RETURNS trigger AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM journal_lines WHERE journal_entry_id = NEW.id) THEN
        RAISE EXCEPTION 'Journal entry % has no lines', NEW.id
            USING ERRCODE = 'integrity_constraint_violation';
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_journal_entries_have_lines
    AFTER INSERT ON journal_entries
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW EXECUTE FUNCTION ledger_assert_entry_has_lines();
