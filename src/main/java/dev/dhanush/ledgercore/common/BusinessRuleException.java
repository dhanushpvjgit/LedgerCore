package dev.dhanush.ledgercore.common;

/** A well-formed request that violates a domain rule (maps to HTTP 422). */
public class BusinessRuleException extends RuntimeException {
    public BusinessRuleException(String message) { super(message); }
}
