package ru.maxbrainrus.app;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import ru.maxbrainrus.parser.RocketParserController;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

public class CommandLineRunner {
    public static final String DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH = "KeyWordsToCategoryMap.json";
    public static final String customCategoryMapOptionDescription = "Specific path to json file with map {\"key word\":\"category\"} for auto filling of category for transactions. By default try to use " + DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH + " in the app working directory";
    @Option(name = "--help", usage = "Show help", help = true)
    private boolean help;

    @Option(name = "-m", aliases = "--custom-category-map", usage = customCategoryMapOptionDescription, help = true)
    private File pathToKeyWordToMapConfig;

    @Argument
    private List<String> arguments = new ArrayList<String>();

    public static void main(String[] args) {
        new CommandLineRunner().doMain(args);
    }

    private void doMain(String[] args) {
        CmdLineParser parser = new CmdLineParser(this);

        try {
            parser.parseArgument(args);

            if (help) {
                printUsage(parser, System.out);
                return;
            }

            if (arguments.isEmpty()) {
                throw new CmdLineException(parser, "No argument is given", new IllegalStateException());
            }

            if (arguments.size() > 2) {
                throw new CmdLineException(parser, "Too many arguments. Should be one or two", new IllegalStateException());
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            printUsage(parser, System.err);
            System.err.println();
            return;
        }

        String inputDataFileName = arguments.get(0);
        String resultFileName = arguments.size() >= 2 ? arguments.get(1) : "report.xls";
        Map<String, String> keyWordsToCategoryMap = getConfigFileMapKeyWordsToCategory()
                .map(KeyWordsToCategoryMapJsonParser::parseConfigJson)
                .orElse(Collections.emptyMap());

        RocketParserController.makeReport(inputDataFileName, resultFileName, keyWordsToCategoryMap);
    }

    private Optional<File> getConfigFileMapKeyWordsToCategory() {
        if (pathToKeyWordToMapConfig == null) {
            File config = new File(DEFAULT_KEY_WORDS_TO_CATEGORY_MAP_JSON_PATH);
            if (config.isFile() && config.exists()) {
                return Optional.of(config);
            }
            return Optional.empty();
        }
        return Optional.of(pathToKeyWordToMapConfig);
    }

    private void printUsage(CmdLineParser parser, PrintStream printStream) {
        printStream.println("usage: rocketParser [options] source.pdf [result.xls]");
        printStream.println("Options:");
        parser.printUsage(printStream);
    }
}
