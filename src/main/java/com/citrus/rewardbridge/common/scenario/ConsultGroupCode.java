package com.citrus.rewardbridge.common.scenario;

import java.util.Arrays;
import java.util.Optional;

public enum ConsultGroupCode {
    PM(1, "產品經理", "pm"),
    QA(2, "測試團隊", "qa"),
    ;

    private final int code;
    private final String label;
    private final String slug;

    ConsultGroupCode(int code, String label, String slug) {
        this.code = code;
        this.label = label;
        this.slug = slug;
    }

    public int code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String slug() {
        return slug;
    }

    public static Optional<ConsultGroupCode> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst();
    }
}
