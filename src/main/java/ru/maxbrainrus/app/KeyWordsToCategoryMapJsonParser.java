package ru.maxbrainrus.app;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public class KeyWordsToCategoryMapJsonParser {
    @SuppressWarnings("unchecked")
    public static Map<String, String> parseConfigJson(File config) {
        try {
            return new ObjectMapper().readValue(config, LinkedHashMap.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
