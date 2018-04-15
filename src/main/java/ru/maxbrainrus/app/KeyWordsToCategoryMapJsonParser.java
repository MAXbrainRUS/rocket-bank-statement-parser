package ru.maxbrainrus.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class KeyWordsToCategoryMapJsonParser {
    @SneakyThrows
    public static Map<String, String> parseConfigJson(File config) {
        return new ObjectMapper().readValue(config, HashMap.class);
    }
}
