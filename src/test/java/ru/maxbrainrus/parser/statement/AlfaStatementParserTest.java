package ru.maxbrainrus.parser.statement;

import org.testng.annotations.Test;
import ru.maxbrainrus.transaction.AmountWithCcy;
import ru.maxbrainrus.transaction.Amounts;
import ru.maxbrainrus.transaction.MoneyTransaction;
import ru.maxbrainrus.transaction.OperationType;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.testng.Assert.assertEquals;

public class AlfaStatementParserTest {
    @Test
    public void testCsvExample() {
        AlfaStatementParser alfaParser = new AlfaStatementParser();
        InputStream testData = AlfaStatementParserTest.class.getResourceAsStream("/alfa_example.csv");
        List<MoneyTransaction> actualTransactions = alfaParser.parseBankStatement(testData, "testWalletA");
        List<MoneyTransaction> expectedTransactions = Arrays.asList(
                getTransactionExample(OperationType.EXPENDITURE, LocalDate.of(2001, 1, 1), "Some old operation", "1234.5"),
                getTransactionExample(OperationType.EXPENDITURE, LocalDate.of(2003, 2, 1), "RU SOME.SERVICE", "100"),
                getTransactionExample(OperationType.EXPENDITURE, LocalDate.of(2003, 2, 2), "SOME SRVS2", "200.4"),
                getTransactionExample(OperationType.TRANSFER, LocalDate.of(2004, 2, 3), "Внутрибанковский перевод между счетами, ИВАНОВ И. И.", "500"),
                getTransactionExample(OperationType.TRANSFER, LocalDate.of(2004, 2, 4), "CARD2CARD ALFA_MOBILE", "600", true),
                getTransactionExample(OperationType.EXPENDITURE, LocalDate.of(2004, 3, 5), "Some other operation", "700.7"),
                getTransactionExample(OperationType.INCOME, LocalDate.of(2004, 3, 6), "Some income with 'quotes'", "800"),
                getTransactionExample(OperationType.INCOME, LocalDate.of(2005, 4, 7), "Some income with % and some other description 900.12RUR and'04 (1.23% gg 01-30.01)", "1234.56")
        );
        assertEquals(actualTransactions, expectedTransactions);

    }

    private MoneyTransaction getTransactionExample(OperationType operationType, LocalDate date, String description, String amount, boolean isTargetWallet) {
        MoneyTransaction.MoneyTransactionBuilder builder = MoneyTransaction.builder()
                .operationType(operationType)
                .date(date)
                .description(description)
                .amounts(
                        Amounts.builder().sourceAmount(AmountWithCcy.builder().
                                amount(new BigDecimal(amount)).build()).build());
        if (isTargetWallet) {
            builder.targetWallet("testWalletA");
        } else {
            builder.sourceWallet("testWalletA");
        }
        return builder.build();
    }

    private MoneyTransaction getTransactionExample(OperationType operationType, LocalDate date, String description, String amount) {
        return getTransactionExample(operationType, date, description, amount, false);
    }
}