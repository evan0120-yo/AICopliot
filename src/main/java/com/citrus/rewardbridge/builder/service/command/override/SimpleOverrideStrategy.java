package com.citrus.rewardbridge.builder.service.command.override;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(1)
public class SimpleOverrideStrategy implements BuilderOverrideStrategy {

    @Override
    public boolean supports(BuilderOverrideContext context) {
        return StringUtils.hasText(context.userText());
    }

    @Override
    public String override(BuilderOverrideContext context) {
        return context.userText();
    }
}
