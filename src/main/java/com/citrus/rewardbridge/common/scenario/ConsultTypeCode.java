package com.citrus.rewardbridge.common.scenario;

import java.util.Arrays;
import java.util.Optional;

public enum ConsultTypeCode {
    ESTIMATE(1, "工時估算", "estimate"),
    SMOKE_DOC(2, "生成冒煙測試", "smoke-doc"),
    ;

    private final int code;
    private final String label;
    private final String slug;

    ConsultTypeCode(int code, String label, String slug) {
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

    public static Optional<ConsultTypeCode> fromCode(Integer code) {
        if (code == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(value -> value.code == code)
                .findFirst();
    }
}
