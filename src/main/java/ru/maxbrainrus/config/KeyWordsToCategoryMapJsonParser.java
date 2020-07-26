package ru.maxbrainrus.config;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class KeyWordsToCategoryMapJsonParser {
    @SuppressWarnings("unchecked")
    public static Map<String, ConfigValue> parseConfigJson(File config) {
        try {
            LinkedHashMap<String, Object> configFromFile = new ObjectMapper().readValue(config, LinkedHashMap.class);
            LinkedHashMap<String, ConfigValue> result = new LinkedHashMap<>();
            configFromFile.forEach((keyWord, configValue) -> {
                if (configValue instanceof String) {
                    result.put(keyWord, ConfigValue.builder()
                            .category((String) configValue)
                            .build());
                } else if (configValue instanceof List) {
                    List<String> listOfValues = (List<String>) configValue;
                    result.put(keyWord, ConfigValue.builder()
                            .category(listOfValues.get(0))
                            .additionalDescription(listOfValues.get(1))
                            .build());
                } else {
                    throw new IllegalArgumentException(String.format("Can't parse config value: %s -> %s", keyWord, configValue));
                }
            });
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
