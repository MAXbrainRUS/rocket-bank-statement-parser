package ru.maxbrainrus.parser;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Transaction {
    /**
     * Date of operation.
     * The same as operation date, if it has is source pdf.
     */
    private final LocalDate date;
    /**
     * Some additional data of transaction.
     */
    private final String description;
    /**
     * Temporal field.
     * Date of operation in description of source pdf.
     */
    private final LocalDateTime operationDate;
    private final BigDecimal amountArrival;
    private final BigDecimal amountExpenditure;
    private final String category;

    public Transaction(LocalDate date, String description, LocalDateTime operationDate, BigDecimal amountArrival, BigDecimal amountExpenditure, String category) {
        this.date = date;
        this.description = description;
        this.operationDate = operationDate;
        this.amountArrival = amountArrival;
        this.amountExpenditure = amountExpenditure;
        this.category = category;
    }

    public static TransactionBuilder builder() {
        return new TransactionBuilder();
    }

    public LocalDate getDate() {
        return this.date;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDateTime getOperationDate() {
        return this.operationDate;
    }

    public BigDecimal getAmountArrival() {
        return this.amountArrival;
    }

    public BigDecimal getAmountExpenditure() {
        return this.amountExpenditure;
    }

    public String getCategory() {
        return this.category;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "date=" + date +
                ", description='" + description + '\'' +
                ", operationDate=" + operationDate +
                ", amountArrival=" + amountArrival +
                ", amountExpenditure=" + amountExpenditure +
                ", category='" + category + '\'' +
                '}';
    }

    public TransactionBuilder toBuilder() {
        return new TransactionBuilder()
                .date(this.date)
                .description(this.description)
                .operationDate(this.operationDate)
                .amountArrival(this.amountArrival)
                .amountExpenditure(this.amountExpenditure)
                .category(this.category);
    }

    public static class TransactionBuilder {
        private LocalDate date;
        private String description;
        private LocalDateTime operationDate;
        private BigDecimal amountArrival;
        private BigDecimal amountExpenditure;
        private String category;

        TransactionBuilder() {
        }

        public Transaction.TransactionBuilder date(LocalDate date) {
            this.date = date;
            return this;
        }

        public Transaction.TransactionBuilder description(String description) {
            this.description = description;
            return this;
        }

        public Transaction.TransactionBuilder operationDate(LocalDateTime operationDate) {
            this.operationDate = operationDate;
            return this;
        }

        public Transaction.TransactionBuilder amountArrival(BigDecimal amountArrival) {
            this.amountArrival = amountArrival;
            return this;
        }

        public Transaction.TransactionBuilder amountExpenditure(BigDecimal amountExpenditure) {
            this.amountExpenditure = amountExpenditure;
            return this;
        }

        public Transaction.TransactionBuilder category(String category) {
            this.category = category;
            return this;
        }

        public Transaction build() {
            return new Transaction(date, description, operationDate, amountArrival, amountExpenditure, category);
        }

        @Override
        public String toString() {
            return "TransactionBuilder{" +
                    "date=" + date +
                    ", description='" + description + '\'' +
                    ", operationDate=" + operationDate +
                    ", amountArrival=" + amountArrival +
                    ", amountExpenditure=" + amountExpenditure +
                    ", category='" + category + '\'' +
                    '}';
        }
    }
}
