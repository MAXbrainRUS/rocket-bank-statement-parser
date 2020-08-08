package ru.maxbrainrus.parser.statement;

import ru.maxbrainrus.transaction.MoneyTransaction;

import java.util.List;

public interface BankStatementParser {
    List<MoneyTransaction> parseBankStatement(String inputDataFileName, String sourceWallet);
}
