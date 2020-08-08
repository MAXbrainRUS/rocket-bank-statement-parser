package ru.maxbrainrus.app;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;
import ru.maxbrainrus.config.ConfigValue;
import ru.maxbrainrus.config.KeyWordsToCategoryMapJsonParser;
import ru.maxbrainrus.parser.ReportGeneratorFacade;
import ru.maxbrainrus.parser.statement.BankFormatType;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Slf4j
@CommandLine.Command(name = "rocketParser", mixinStandardHelpOptions = true, version = "1.0")
public class CommandLineRunner implements Runnable {
    public static final String DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH = "KeyWordsToCategoryMap.json";
    public static final String customCategoryMapOptionDescription = "Specific path to json file with map {\"key word\":\"category\"} for auto filling of category for transactions. By default try to use " + DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH + " in the app working directory";
    public static final String DATE_PATTERN = "dd-MM-yyyy";

    @CommandLine.Option(names = {"-m", "--custom-category-map"},
            description = customCategoryMapOptionDescription,
            defaultValue = DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH)
    private File pathToKeyWordToMapConfig;

    @CommandLine.Option(names = {"-c", "--cut-date"},
            description = "Remove all transactions with date less or equals this date from report. Date format is " + DATE_PATTERN)
    private String cutDateStringValue;

    @CommandLine.Option(names = {"-w", "--wallet"},
            description = "Name of wallet (source of transactions)",
            defaultValue = "Рокет карта")
    private String sourceWallet;

    @CommandLine.Parameters(index = "0", descriptionKey = "source.scv", description = "bank statement file")
    private String sourceStatementFilename;

    @CommandLine.Parameters(index = "1", descriptionKey = "report.csv", description = "result report filename",
            defaultValue = "report.csv")
    private String reportFilename;

    @CommandLine.Option(names = {"-b", "--bank_format"},
            description = "Bank statement format type",
            defaultValue = "RAIF")
    private BankFormatType bankFormatType;

    @CommandLine.Option(names = {"-q", "--quiet"},
            description = "Quiet output - only show errors")
    private boolean isQuiet;


    public static void main(String[] args) {
        CommandLine.run(new CommandLineRunner(), args);
    }

    private static LocalDate parseCutDate(String cutDateStringValue) {
        LocalDate cutDate = null;
        if (!StringUtils.isBlank(cutDateStringValue)) {
            cutDate = LocalDate.parse(cutDateStringValue, DateTimeFormatter.ofPattern(DATE_PATTERN));
        }
        return cutDate;
    }

    @Override
    public void run() {
        setLoggerLevel();
        Map<String, ConfigValue> keyWordsToCategoryOrWalletMap = getKeyWordsToCategoryOrWalletMapping();
        LocalDate cutDate = parseCutDate(cutDateStringValue);
        ReportGeneratorFacade.makeReport(sourceStatementFilename, reportFilename, keyWordsToCategoryOrWalletMap, cutDate, sourceWallet, bankFormatType);
    }

    private void setLoggerLevel() {
        if (isQuiet) {
            Logger root = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
            root.setLevel(Level.ERROR);
        }
    }

    private Map<String, ConfigValue> getKeyWordsToCategoryOrWalletMapping() {
        return getConfigFileMapKeyWordsToCategory()
                .map(KeyWordsToCategoryMapJsonParser::parseConfigJson)
                .orElse(Collections.emptyMap());
    }

    private Optional<File> getConfigFileMapKeyWordsToCategory() {
        if (pathToKeyWordToMapConfig.isFile() && pathToKeyWordToMapConfig.exists()) {
            return Optional.of(pathToKeyWordToMapConfig);
        }
        log.warn("File config with category mapping is not found. Transactions won't be categorized");
        return Optional.empty();
    }
}
