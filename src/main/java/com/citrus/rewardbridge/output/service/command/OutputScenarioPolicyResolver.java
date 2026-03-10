package com.citrus.rewardbridge.output.service.command;

import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.ScenarioOutputPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OutputScenarioPolicyResolver {

    public ScenarioOutputPolicy resolve(BuilderConfigEntity builderConfig) {
        if (builderConfig == null || !builderConfig.isIncludeFile()) {
            return ScenarioOutputPolicy.textOnly();
        }

        if (!StringUtils.hasText(builderConfig.getDefaultOutputFormat())) {
            throw new BusinessException(
                    "BUILDER_DEFAULT_OUTPUT_FORMAT_MISSING",
                    "Builder requires file output but no default output format is configured.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        OutputFormat defaultOutputFormat = OutputFormat.from(builderConfig.getDefaultOutputFormat())
                .orElseThrow(() -> new BusinessException(
                        "BUILDER_DEFAULT_OUTPUT_FORMAT_INVALID",
                        "Builder default output format is invalid.",
                        HttpStatus.INTERNAL_SERVER_ERROR
                ));

        return ScenarioOutputPolicy.withFile(defaultOutputFormat);
    }
}
