package com.citrus.rewardbridge.gatekeeper.usecase.command;

import com.citrus.rewardbridge.builder.dto.BuilderConsultCommand;
import com.citrus.rewardbridge.builder.usecase.command.BuilderCommandUseCase;
import com.citrus.rewardbridge.gatekeeper.dto.ConsultGuardResult;
import com.citrus.rewardbridge.gatekeeper.dto.ConsultRequest;
import com.citrus.rewardbridge.gatekeeper.service.guard.ConsultGuardService;
import com.citrus.rewardbridge.output.dto.RenderedOutput;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GatekeeperCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(GatekeeperCommandUseCase.class);
    private final ConsultGuardService consultGuardService;
    private final BuilderCommandUseCase builderCommandUseCase;

    public RenderedOutput consult(ConsultRequest request, String clientIp) {
        log.info(
                "Gatekeeper use case started. clientIp={}, builderId={}, outputFormat={}",
                clientIp,
                request.getBuilderId(),
                request.getOutputFormat()
        );
        ConsultGuardResult guardResult = consultGuardService.guard(request, clientIp);
        log.info(
                "Gatekeeper use case forwarding request to Builder. clientIp={}, builderId={}, outputFormat={}",
                clientIp,
                guardResult.builderId(),
                describeOutputFormat(guardResult.outputFormat())
        );

        RenderedOutput response = builderCommandUseCase.consult(
                new BuilderConsultCommand(
                        request.getText(),
                        guardResult.builderId(),
                        guardResult.outputFormat(),
                        request.getFiles(),
                        clientIp
                )
        );

        log.info(
                "Builder returned response to Gatekeeper use case. clientIp={}, builderId={}, outputFormat={}",
                clientIp,
                guardResult.builderId(),
                describeOutputFormat(guardResult.outputFormat())
        );
        return response;
    }

    private String describeOutputFormat(com.citrus.rewardbridge.output.dto.OutputFormat outputFormat) {
        return outputFormat == null ? "(scenario default or ignored)" : outputFormat.value();
    }
}
