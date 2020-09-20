package ru.maxbrainrus.parser.statement;

import lombok.SneakyThrows;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import ru.maxbrainrus.transaction.MoneyTransaction;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public abstract class CsvStatementParser implements BankStatementParser {
    public static Charset getCharset1251() {
        return Charset.forName("windows-1251");
    }

    public static Charset getCharsetUtf8() {
        return StandardCharsets.UTF_8;
    }


    @SneakyThrows
    private static <T> T withOpenFile(String fileName, Function<InputStream, T> consumer) {
        try (FileInputStream in = new FileInputStream(fileName)) {
            return consumer.apply(in);
        }
    }

    @SneakyThrows
    private static <T> T withParseInputStream(InputStream in, Charset charset, CSVFormat csvFormat, Function<CSVParser, T> consumer) {
        try (InputStreamReader isr = new InputStreamReader(in, charset)) {
            try (CSVParser csvParser = csvFormat.parse(isr)) {
                return consumer.apply(csvParser);
            }
        }
    }

    @Override
    public List<MoneyTransaction> parseBankStatement(String inputDataFileName, String sourceWallet) {
        return withOpenFile(inputDataFileName, inputStream ->
                parseBankStatement(inputStream, sourceWallet));
    }

    public List<MoneyTransaction> parseBankStatement(InputStream inputData, String sourceWallet) {
        return withParseInputStream(inputData, getCharset(), getCsvFormat(),
                csvParser ->
                        StreamSupport.stream(csvParser.spliterator(), false)
                                .map(record -> readTransaction(record, sourceWallet))
                                .collect(Collectors.toList()));
    }

    protected Charset getCharset() {
        return getCharset1251();
    }

    protected CSVFormat getCsvFormat() {
        return CSVFormat.DEFAULT
                .withDelimiter(';')
                .withFirstRecordAsHeader();
    }

    protected abstract MoneyTransaction readTransaction(CSVRecord record, String sourceWallet);
}
