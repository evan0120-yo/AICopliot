package com.citrus.rewardbridge.builder.service.command.override;

import com.citrus.rewardbridge.rag.dto.RagSupplementDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BuilderOverrideFactory {

    private final List<BuilderOverrideStrategy> builderOverrideStrategies;

    public String resolveContent(RagSupplementDto supplement, String userText) {
        if (!supplement.overridable() || !StringUtils.hasText(userText)) {
            return supplement.content();
        }

        BuilderOverrideContext context = new BuilderOverrideContext(
                supplement.ragType(),
                supplement.content(),
                userText.trim()
        );

        return builderOverrideStrategies.stream()
                .filter(strategy -> strategy.supports(context))
                .findFirst()
                .map(strategy -> strategy.override(context))
                .orElse(supplement.content());
    }
}
