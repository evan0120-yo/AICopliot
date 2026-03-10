package com.citrus.rewardbridge.output.dto;

public record ScenarioOutputPolicy(
        boolean includeFile,
        OutputFormat defaultOutputFormat
) {

    public static ScenarioOutputPolicy textOnly() {
        return new ScenarioOutputPolicy(false, null);
    }

    public static ScenarioOutputPolicy withFile(OutputFormat defaultOutputFormat) {
        return new ScenarioOutputPolicy(true, defaultOutputFormat);
    }
}
