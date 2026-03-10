package com.citrus.rewardbridge.builder.service.command.override;

public interface BuilderOverrideStrategy {

    boolean supports(BuilderOverrideContext context);

    String override(BuilderOverrideContext context);
}
