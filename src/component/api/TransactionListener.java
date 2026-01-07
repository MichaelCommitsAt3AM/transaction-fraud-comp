// TransactionListener.java
package component.api;

import component.events.*;

public interface TransactionListener {
    void onApproved(TransactionApprovedEvent event);
    void onDeclined(TransactionDeclinedEvent event);
    void onFraudDetected(FraudDetectedEvent event);
}