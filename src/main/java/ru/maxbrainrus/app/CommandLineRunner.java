package ru.maxbrainrus.app;

import picocli.CommandLine;
import ru.maxbrainrus.parser.RocketParserController;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@CommandLine.Command(name = "rocketParser", mixinStandardHelpOptions = true, version = "1.0")
public class CommandLineRunner implements Runnable {
    public static final String DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH = "KeyWordsToCategoryMap.json";
    public static final String customCategoryMapOptionDescription = "Specific path to json file with map {\"key word\":\"category\"} for auto filling of category for transactions. By default try to use " + DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH + " in the app working directory";

    @CommandLine.Option(names = {"-m", "--custom-category-map"},
            description = customCategoryMapOptionDescription,
            defaultValue = DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH)
    private File pathToKeyWordToMapConfig;

    @CommandLine.Parameters(index = "0", descriptionKey = "source.pdf", description = "rocket statement pdf file")
    private String sourceStatementFilename;

    @CommandLine.Parameters(index = "1", descriptionKey = "report.csv", description = "result report filename",
            defaultValue = "report.csv")
    private String reportFilename;


    public static void main(String[] args) {
        CommandLine.run(new CommandLineRunner(), args);
    }

    @Override
    public void run() {
        Map<String, String> keyWordsToCategoryMap = getConfigFileMapKeyWordsToCategory()
                .map(KeyWordsToCategoryMapJsonParser::parseConfigJson)
                .orElse(Collections.emptyMap());

        RocketParserController.makeReport(sourceStatementFilename, reportFilename, keyWordsToCategoryMap);
    }

    private Optional<File> getConfigFileMapKeyWordsToCategory() {
        if (pathToKeyWordToMapConfig.isFile() && pathToKeyWordToMapConfig.exists()) {
            return Optional.of(pathToKeyWordToMapConfig);
        }
        return Optional.empty();
    }
}
