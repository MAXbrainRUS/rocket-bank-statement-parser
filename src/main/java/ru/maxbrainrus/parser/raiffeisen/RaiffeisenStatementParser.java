package ru.maxbrainrus.parser.raiffeisen;

import ru.maxbrainrus.parser.StatementParser;
import ru.maxbrainrus.transaction.MoneyTransaction;

import java.util.Collections;
import java.util.List;

public class RaiffeisenStatementParser implements StatementParser {
    @Override
    public List<MoneyTransaction> parseBankStatement(String inputDataFileName, String sourceWallet) {
        return Collections.emptyList();
    }
}
