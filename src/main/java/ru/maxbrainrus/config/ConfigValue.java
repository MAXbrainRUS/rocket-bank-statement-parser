package ru.maxbrainrus.config;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ConfigValue {
    String category;
    String additionalDescription;
}
