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

public class RaiffeisenStatementParserTest {
    @Test
    public void testCsvExample() {
        RaiffeisenStatementParser raifParser = new RaiffeisenStatementParser();
        InputStream testData = RaiffeisenStatementParserTest.class.getResourceAsStream("/raiff_example.csv");
        List<MoneyTransaction> actualTransactions = raifParser.parseBankStatement(testData, "testWallet");
        List<MoneyTransaction> expectedTransactions = Arrays.asList(
                getTransactionExample(OperationType.EXPENDITURE, LocalDate.of(2000, 2, 1), "some old transaction", "1000.01"),
                getTransactionExample(OperationType.EXPENDITURE, LocalDate.of(2003, 2, 1), "SOME TRANSACTION DESCRIPTION", "1234.56"),
                getTransactionExample(OperationType.EXPENDITURE, LocalDate.of(2004, 3, 2), "\"some transaction with quotes\" and other text", "234.00"),
                getTransactionExample(OperationType.INCOME, LocalDate.of(2005, 4, 3), "transfer money from one account to another", "40000.00"),
                getTransactionExample(OperationType.INCOME, LocalDate.of(2020, 6, 5), "rollback money", "2499.00")
        );
        assertEquals(actualTransactions, expectedTransactions);

    }

    private MoneyTransaction getTransactionExample(OperationType operationType, LocalDate date, String description, String amount) {
        return MoneyTransaction.builder()
                .operationType(operationType)
                .date(date)
                .description(description)
                .amounts(
                        Amounts.builder().sourceAmount(AmountWithCcy.builder().
                                amount(new BigDecimal(amount)).build()).build())
                .sourceWallet("testWallet")
                .build();
    }

    private MoneyTransaction getTransaction() {
        return MoneyTransaction.builder()
                .build();
    }
}