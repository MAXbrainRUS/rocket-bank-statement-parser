package ru.maxbrainrus.parser.statement;

public class BankStatementParserFactory {
    public static BankStatementParser createBankStatementParser(BankFormatType bankFormatType) {
        switch (bankFormatType) {
            case RAIF:
                return new RaiffeisenStatementParser();
            case ALFA:
                return new AlfaStatementParser();
            default:
                throw new IllegalArgumentException(String.format("Unsupported BankFormatType: %s", bankFormatType));
        }
    }
}
