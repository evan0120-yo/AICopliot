package com.citrus.rewardbridge.builder.service.command.override;

public record BuilderOverrideContext(
        String ragType,
        String originalContent,
        String userText
) {
}
