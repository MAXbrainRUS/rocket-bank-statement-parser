package ru.maxbrainrus.parser;

import ru.maxbrainrus.transaction.MoneyTransaction;

import java.util.List;

public interface StatementParser {
    List<MoneyTransaction> parseBankStatement(String inputDataFileName, String sourceWallet);
}
