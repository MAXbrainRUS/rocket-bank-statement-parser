package ru.maxbrainrus.parser;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@AutoValue
public abstract class Transaction {

    public static TransactionBuilder builder() {
        return new AutoValue_Transaction.Builder();
    }

    /**
     * Date of operation.
     * The same as operation date, if it has is source pdf.
     */
    @Nullable
    public abstract LocalDate getDate();

    @Nullable
    public abstract String getDescription();

    /**
     * Temporal field.
     * Date of operation in description of source pdf.
     */
    @Nullable
    public abstract LocalDateTime getOperationDate();

    @Nullable
    public abstract BigDecimal getAmountArrival();

    @Nullable
    public abstract BigDecimal getAmountExpenditure();

    @Nullable
    public abstract String getCategory();

    public abstract TransactionBuilder toBuilder();

    @AutoValue.Builder
    abstract static class TransactionBuilder {

        public abstract Transaction.TransactionBuilder setDate(LocalDate date);

        public abstract Transaction.TransactionBuilder setDescription(String description);

        public abstract Transaction.TransactionBuilder setOperationDate(LocalDateTime operationDate);

        public abstract Transaction.TransactionBuilder setAmountArrival(BigDecimal amountArrival);

        public abstract Transaction.TransactionBuilder setAmountExpenditure(BigDecimal amountExpenditure);

        public abstract Transaction.TransactionBuilder setCategory(String category);

        public abstract Transaction build();
    }
}
