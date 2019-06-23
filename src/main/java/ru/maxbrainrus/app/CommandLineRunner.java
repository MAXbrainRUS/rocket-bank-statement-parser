package ru.maxbrainrus.app;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import picocli.CommandLine;
import ru.maxbrainrus.config.KeyWordsToCategoryMapJsonParser;
import ru.maxbrainrus.parser.RocketParserController;

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
    public static final String DATE_PATTERN = "dd-mm-yyyy";

    @CommandLine.Option(names = {"-m", "--custom-category-map"},
            description = customCategoryMapOptionDescription,
            defaultValue = DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH)
    private File pathToKeyWordToMapConfig;

    @CommandLine.Option(names = {"-c", "--cut-date"},
            description = "Remove all transactions with date less or equals this date from report. Date format is " + DATE_PATTERN)
    private String cutDateStringValue;

    @CommandLine.Parameters(index = "0", descriptionKey = "source.pdf", description = "rocket statement pdf file")
    private String sourceStatementFilename;

    @CommandLine.Parameters(index = "1", descriptionKey = "report.csv", description = "result report filename",
            defaultValue = "report.csv")
    private String reportFilename;


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
        Map<String, String> keyWordsToCategoryMap = getConfigFileMapKeyWordsToCategory()
                .map(KeyWordsToCategoryMapJsonParser::parseConfigJson)
                .orElse(Collections.emptyMap());

        LocalDate cutDate = parseCutDate(cutDateStringValue);
        RocketParserController.makeReport(sourceStatementFilename, reportFilename, keyWordsToCategoryMap);
    }

    private Optional<File> getConfigFileMapKeyWordsToCategory() {
        if (pathToKeyWordToMapConfig.isFile() && pathToKeyWordToMapConfig.exists()) {
            return Optional.of(pathToKeyWordToMapConfig);
        }
        log.warn("File config with category mapping is not found. Transactions won't be categorized");
        return Optional.empty();
    }
}
