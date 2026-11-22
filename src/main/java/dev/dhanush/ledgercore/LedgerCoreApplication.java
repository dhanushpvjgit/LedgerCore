package dev.dhanush.ledgercore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * LedgerCore - double-entry ledger with idempotent APIs, a transactional outbox
 * and a saga-based transfer workflow.
 *
 * @author Dhanush
 */
@SpringBootApplication
@EnableScheduling
public class LedgerCoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(LedgerCoreApplication.class, args);
    }
}
