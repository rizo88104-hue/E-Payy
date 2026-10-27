package uz.epay.service;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Fayl amallari baza tranzaksiyasi natijasiga bog'lanadi: diskda yetim fayl yoki yo'qolgan rasm qolmasin. */
final class AfterTransaction {

    private AfterTransaction() {
    }

    /** Tranzaksiya commit bo'lgandan keyin bajaradi (masalan, o'chirilgan rasm faylini o'chirish). */
    static void onCommit(Runnable action) {
        register(true, action);
    }

    /** Tranzaksiya bekor bo'lsa bajaradi (masalan, yangi saqlangan faylni o'chirish). */
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
