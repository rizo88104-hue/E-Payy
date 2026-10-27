package uz.epay.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

final class AfterTransaction {

    private AfterTransaction() {
    }

    static void onCommit(Runnable action) {
        register(true, action);
    }

    static void onRollback(Runnable action) {
        register(false, action);
    }

    private static void register(boolean onCommit, Runnable action) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if ((status == STATUS_COMMITTED) == onCommit) {
                    action.run();
                }
            }
        });
    }
}
