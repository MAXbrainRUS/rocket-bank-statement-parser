package ru.maxbrainrus.parser;

import java.util.Map;

public class KeyWordCategoryFiller {
    private final Map<String, String> keyWordsToCategoryMap;

    public KeyWordCategoryFiller(Map<String, String> keyWordsToCategoryMap) {
        this.keyWordsToCategoryMap = keyWordsToCategoryMap;
    }

    public Transaction fillCategory(Transaction transaction) {
        for (Map.Entry<String, String> entry : keyWordsToCategoryMap.entrySet()) {
            if (transaction.getDescription().contains(entry.getKey())) {
                return transaction.toBuilder().category(entry.getValue()).build();
            }
        }
        return transaction;
    }


}
