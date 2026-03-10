package com.citrus.rewardbridge.output.service.command;

import com.citrus.rewardbridge.common.scenario.ConsultScenario;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import com.citrus.rewardbridge.output.dto.ScenarioOutputPolicy;
import org.springframework.stereotype.Component;

@Component
public class OutputScenarioPolicyResolver {

    public ScenarioOutputPolicy resolve(Integer group, Integer type) {
        return ConsultScenario.fromCodes(group, type)
                .map(scenario -> scenario.includeFile()
                        ? ScenarioOutputPolicy.withFile(
                                OutputFormat.from(scenario.defaultOutputFormatValue()).orElse(OutputFormat.XLSX)
                        )
                        : ScenarioOutputPolicy.textOnly())
                .orElseGet(ScenarioOutputPolicy::textOnly);
    }
}
