package io.github.michaelcommitsat3am.transactionfraudcomp.component.exceptions;

public class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}