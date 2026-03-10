package com.citrus.rewardbridge.output.dto;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

public enum OutputFormat {
    MARKDOWN("markdown", "text/markdown; charset=UTF-8"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    ;

    private final String value;
    private final String contentType;

    OutputFormat(String value, String contentType) {
        this.value = value;
        this.contentType = contentType;
    }

    public String value() {
        return value;
    }

    public String contentType() {
        return contentType;
    }

    public static Optional<OutputFormat> from(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(format -> format.value.equals(normalized))
                .findFirst();
    }
}
