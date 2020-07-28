package ru.maxbrainrus.app;

import org.testng.annotations.Test;
import ru.maxbrainrus.config.ConfigValue;
import ru.maxbrainrus.config.KeyWordsToCategoryMapJsonParser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

import static org.testng.Assert.assertEquals;

public class KeyWordsToCategoryMapJsonParserTest {

    private static Map<String, ConfigValue> getConfigMapFromResources(String resourcePath) throws IOException {
        URL resourceUrl = Thread.currentThread().getContextClassLoader().getResource(resourcePath);
        File configFile = new File(resourceUrl.getPath());
        return KeyWordsToCategoryMapJsonParser.parseConfigJson(configFile);
    }

    @Test
    public void testParseConfigJsonSaveOrder() throws IOException {
        Map<String, ConfigValue> configMap = getConfigMapFromResources("KeyWordsToCategoryMapExample.json");

        List<Map.Entry<String, ConfigValue>> expectedValues = Arrays.asList(
                new AbstractMap.SimpleEntry<>("category1", configValue("Value1")),
                new AbstractMap.SimpleEntry<>("category2", configValue("Value2")),
                new AbstractMap.SimpleEntry<>("category3", configValue("Value3", "Additional description")),
                new AbstractMap.SimpleEntry<>("category4", configValue("Value4"))
        );
        Set<Map.Entry<String, ConfigValue>> actualValues = configMap.entrySet();
        assertEquals(actualValues.size(), expectedValues.size());
        int i = 0;
        for (Map.Entry<String, ConfigValue> actualValue : actualValues) {
            assertEquals(actualValue, expectedValues.get(i++));
        }
    }

    private ConfigValue configValue(String category) {
        return ConfigValue.builder().category(category).build();
    }

    private ConfigValue configValue(String category, String additionalDescription) {
        return ConfigValue.builder()
                .category(category)
                .additionalDescription(additionalDescription)
                .build();
    }
}