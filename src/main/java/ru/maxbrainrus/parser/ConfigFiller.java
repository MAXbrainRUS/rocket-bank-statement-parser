package ru.maxbrainrus.parser;

import lombok.extern.slf4j.Slf4j;
import ru.maxbrainrus.config.ConfigValue;
import ru.maxbrainrus.transaction.MoneyTransaction;
import ru.maxbrainrus.transaction.OperationType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ConfigFiller {
    private final Map<String, ConfigValue> keyWordsToConfigMap;

    public ConfigFiller(Map<String, ConfigValue> keyWordsToConfigMap) {
        this.keyWordsToConfigMap = keyWordsToConfigMap;
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
        // One of wallet already filled with default wallet value
        // Fill other (not filled) wallet
        if (transaction.getSourceWallet() == null) {
            return transaction.toBuilder()
                    .sourceWallet(categoryOrWallet)
                    .build();
        }
        if (transaction.getTargetWallet() == null) {
            return transaction.toBuilder()
                    .targetWallet(categoryOrWallet)
                    .build();
        }
        return transaction;
    }

    private MoneyTransaction fillTransaction(MoneyTransaction transaction) {
        return keyWordsToConfigMap.entrySet().stream()
                .filter(entry -> transaction.getDescription().toLowerCase().contains(entry.getKey().toLowerCase()))
                .findFirst()
                .map(entry -> enrichCategoryOrWallet(transaction, entry.getValue().getCategory()))
                .orElse(transaction);
    }

    public List<MoneyTransaction> fill(List<MoneyTransaction> transactionList) {
        return transactionList.stream()
                .map(this::fillTransaction)
                .collect(Collectors.toList());
    }


}
