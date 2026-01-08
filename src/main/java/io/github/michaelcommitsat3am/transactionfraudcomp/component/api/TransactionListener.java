// TransactionListener.java
package io.github.michaelcommitsat3am.transactionfraudcomp.component.api;

import io.github.michaelcommitsat3am.transactionfraudcomp.component.events.*;

public interface TransactionListener {
    void onApproved(TransactionApprovedEvent event);
    void onDeclined(TransactionDeclinedEvent event);
    void onFraudDetected(FraudDetectedEvent event);
}