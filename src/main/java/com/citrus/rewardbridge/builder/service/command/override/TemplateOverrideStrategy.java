package com.citrus.rewardbridge.builder.service.command.override;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Order(0)
public class TemplateOverrideStrategy implements BuilderOverrideStrategy {

    private static final String PLACEHOLDER = "{{userText}}";

    @Override
    public boolean supports(BuilderOverrideContext context) {
        return StringUtils.hasText(context.userText()) && context.originalContent() != null && context.originalContent().contains(PLACEHOLDER);
    }

    @Override
    public String override(BuilderOverrideContext context) {
        return context.originalContent().replace(PLACEHOLDER, context.userText());
    }
}
