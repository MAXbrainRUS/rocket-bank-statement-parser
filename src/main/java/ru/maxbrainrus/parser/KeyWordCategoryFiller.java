package ru.maxbrainrus.parser;

import ru.maxbrainrus.migrate.MoneyTransaction;
import ru.maxbrainrus.migrate.OperationType;

import java.util.Map;

public class KeyWordCategoryFiller {
    private final Map<String, String> keyWordsToCategoryMap;

    public KeyWordCategoryFiller(Map<String, String> keyWordsToCategoryMap) {
        this.keyWordsToCategoryMap = keyWordsToCategoryMap;
    }

    public MoneyTransaction fillCategory(MoneyTransaction transaction) {
        for (Map.Entry<String, String> entry : keyWordsToCategoryMap.entrySet()) {
            if (transaction.getDescription().contains(entry.getKey())) {
                MoneyTransaction.MoneyTransactionBuilder builder = transaction.toBuilder();
                String categoryOrWallet = entry.getValue();
                if (transaction.getOperationType() == OperationType.TRANSFER) {
                    if (transaction.getSourceWallet() == null) {
                        builder.sourceWallet(categoryOrWallet);
                    } else if (transaction.getTargetWallet() == null) {
                        builder.targetWallet(categoryOrWallet);
                    }
                } else {
                    builder.category(categoryOrWallet);
                }
                return builder.build();
            }
        }
        return transaction;
    }


}
