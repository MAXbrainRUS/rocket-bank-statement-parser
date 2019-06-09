package ru.maxbrainrus.parser;

import ru.maxbrainrus.transaction.MoneyTransaction;
import ru.maxbrainrus.transaction.OperationType;

import java.util.Map;

public class KeyWordCategoryFiller {
    private final Map<String, String> keyWordsToCategoryMap;

    public KeyWordCategoryFiller(Map<String, String> keyWordsToCategoryMap) {
        this.keyWordsToCategoryMap = keyWordsToCategoryMap;
    }

    private static MoneyTransaction enrichCategoryOrWallet(MoneyTransaction transaction, String categoryOrWallet) {
        if (transaction.getOperationType() == OperationType.TRANSFER) {
            return enrichWallet(transaction, categoryOrWallet);
        }
        return enrichCategory(transaction, categoryOrWallet);
    }

    private static MoneyTransaction enrichCategory(MoneyTransaction transaction, String categoryOrWallet) {
        return transaction.toBuilder()
                .category(categoryOrWallet)
                .build();
    }

    private static MoneyTransaction enrichWallet(MoneyTransaction transaction, String categoryOrWallet) {
        MoneyTransaction.MoneyTransactionBuilder builder = transaction.toBuilder();
        if (transaction.getSourceWallet() == null) {
            builder.sourceWallet(categoryOrWallet);
        } else if (transaction.getTargetWallet() == null) {
            builder.targetWallet(categoryOrWallet);
        }
        return builder.build();
    }

    public MoneyTransaction fillCategory(MoneyTransaction transaction) {
        return keyWordsToCategoryMap.entrySet().stream()
                .filter(entry -> transaction.getDescription().contains(entry.getKey()))
                .findFirst()
                .map(entry -> enrichCategoryOrWallet(transaction, entry.getValue()))
                .orElse(transaction);
    }


}
