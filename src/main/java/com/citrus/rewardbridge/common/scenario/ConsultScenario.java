package com.citrus.rewardbridge.common.scenario;

import java.util.Arrays;
import java.util.Optional;

public enum ConsultScenario {
    PM_ESTIMATE(
            ConsultGroupCode.PM,
            ConsultTypeCode.ESTIMATE,
            false,
            null,
            "pm-estimate"
    ),
    QA_SMOKE_DOC(
            ConsultGroupCode.QA,
            ConsultTypeCode.SMOKE_DOC,
            true,
            "xlsx",
            "qa-smoke-doc"
    ),
    ;

    private final ConsultGroupCode groupCode;
    private final ConsultTypeCode typeCode;
    private final boolean includeFile;
    private final String defaultOutputFormatValue;
    private final String filePrefix;

    ConsultScenario(
            ConsultGroupCode groupCode,
            ConsultTypeCode typeCode,
            boolean includeFile,
            String defaultOutputFormatValue,
            String filePrefix
    ) {
        this.groupCode = groupCode;
        this.typeCode = typeCode;
        this.includeFile = includeFile;
        this.defaultOutputFormatValue = defaultOutputFormatValue;
        this.filePrefix = filePrefix;
    }

    public int groupCode() {
        return groupCode.code();
    }

    public int typeCode() {
        return typeCode.code();
    }

    public String groupLabel() {
        return groupCode.label();
    }

    public String typeLabel() {
        return typeCode.label();
    }

    public boolean includeFile() {
        return includeFile;
    }

    public String defaultOutputFormatValue() {
        return defaultOutputFormatValue;
    }

    public String filePrefix() {
        return filePrefix;
    }

    public String displayName() {
        return "%s / %s".formatted(groupLabel(), typeLabel());
    }

    public boolean matches(Integer groupCode, Integer typeCode) {
        return this.groupCode.code() == groupCode && this.typeCode.code() == typeCode;
    }

    public static Optional<ConsultScenario> fromCodes(Integer groupCode, Integer typeCode) {
        if (groupCode == null || typeCode == null) {
            return Optional.empty();
        }

        return Arrays.stream(values())
                .filter(value -> value.matches(groupCode, typeCode))
                .findFirst();
    }
}
