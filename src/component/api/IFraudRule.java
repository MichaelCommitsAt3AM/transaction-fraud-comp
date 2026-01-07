// IFraudRule.java
package component.api;

import component.core.TransactionContext;

public interface IFraudRule {
    boolean isFraudulent(TransactionContext context);
    String getRuleName();
}