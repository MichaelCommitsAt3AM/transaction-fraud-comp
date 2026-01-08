// IFraudRule.java
package io.github.michaelcommitsat3am.transactionfraudcomp.component.api;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.core.TransactionContext;

public interface IFraudRule {
    boolean isFraudulent(TransactionContext context);
    String getRuleName();
}