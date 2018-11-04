package ru.maxbrainrus.app;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.testng.Assert.assertEquals;

public class KeyWordsToCategoryMapJsonParserTest {

    private static Map<String, String> getConfigMapFromResources(String resourceMap) throws IOException {
        File testConfigCategoryMap = File.createTempFile("testConfigCategoryMap", ".json");
        try (FileWriter fileWriter = new FileWriter(testConfigCategoryMap)) {
            InputStream resourceAsStream = KeyWordsToCategoryMapJsonParserTest.class.getResourceAsStream(resourceMap);
            String configStringValue = IOUtils.toString(resourceAsStream, "utf-8");
            fileWriter.write(configStringValue);
        }
        return KeyWordsToCategoryMapJsonParser.parseConfigJson(testConfigCategoryMap);
    }

    @Test
    public void testParseConfigJsonSaveOrder() throws IOException {
        Map<String, String> configMap = getConfigMapFromResources("/KeyWordsToCategoryMapExample.json");

        List<Map.Entry<String, String>> expectedValues = Arrays.asList(
                new AbstractMap.SimpleEntry<>("category1", "Value1"),
                new AbstractMap.SimpleEntry<>("category2", "Value2"),
                new AbstractMap.SimpleEntry<>("category3", "Value3"),
                new AbstractMap.SimpleEntry<>("category4", "Value4")
        );
        Set<Map.Entry<String, String>> actualValues = configMap.entrySet();
        assertEquals(actualValues.size(), expectedValues.size());
        int i = 0;
        for (Map.Entry<String, String> actualValue : actualValues) {
            assertEquals(actualValue, expectedValues.get(i++));
        }
    }
}