-- Week 1: accounts and the double-entry journal.

CREATE TABLE ledger_accounts (
    id           UUID PRIMARY KEY,
    account_code VARCHAR(40)  NOT NULL UNIQUE,
    name         VARCHAR(120) NOT NULL,
    currency     VARCHAR(3)   NOT NULL,
    account_type VARCHAR(20)  NOT NULL,
    status       VARCHAR(20)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    CONSTRAINT chk_account_type     CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT chk_account_status   CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT chk_account_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE journal_entries (
    id          UUID PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    reference   VARCHAR(80),
    currency    VARCHAR(3)   NOT NULL,
    status      VARCHAR(20)  NOT NULL,
    posted_at   TIMESTAMPTZ  NOT NULL,
    created_by  VARCHAR(100),
    CONSTRAINT chk_journal_status   CHECK (status = 'POSTED'),
    CONSTRAINT chk_journal_currency CHECK (currency ~ '^[A-Z]{3}$')
);

CREATE TABLE journal_lines (
    id               UUID PRIMARY KEY,
    journal_entry_id UUID           NOT NULL REFERENCES journal_entries (id),
    account_id       UUID           NOT NULL REFERENCES ledger_accounts (id),
    line_number      INTEGER        NOT NULL,
    direction        VARCHAR(10)    NOT NULL,
    amount           NUMERIC(19, 4) NOT NULL,
    description      VARCHAR(255),
    CONSTRAINT chk_line_direction CHECK (direction IN ('DEBIT', 'CREDIT')),
    CONSTRAINT chk_line_amount    CHECK (amount > 0),
    CONSTRAINT uq_line_number     UNIQUE (journal_entry_id, line_number)
);

CREATE INDEX idx_journal_entries_posted_at ON journal_entries (posted_at);
CREATE INDEX idx_journal_lines_account_id  ON journal_lines (account_id);
