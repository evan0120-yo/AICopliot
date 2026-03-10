package com.citrus.rewardbridge.builder.usecase.command;

import com.citrus.rewardbridge.builder.dto.BuilderConsultCommand;
import com.citrus.rewardbridge.builder.service.command.BuilderCommandService;
import com.citrus.rewardbridge.common.config.RewardBridgeProperties;
import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedOutput;
import com.citrus.rewardbridge.output.usecase.command.OutputCommandUseCase;
import com.citrus.rewardbridge.rag.dto.RagDocumentDto;
import com.citrus.rewardbridge.rag.usecase.query.RagQueryUseCase;
import com.citrus.rewardbridge.source.dto.SourceResult;
import com.citrus.rewardbridge.source.usecase.query.SourceQueryUseCase;
import com.citrus.rewardbridge.aiclient.usecase.command.AiClientCommandUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class BuilderCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(BuilderCommandUseCase.class);
    private static final String SAFETY_CATEGORY = "SAFETY";

    private final BuilderCommandService builderCommandService;
    private final RagQueryUseCase ragQueryUseCase;
    private final SourceQueryUseCase sourceQueryUseCase;
    private final AiClientCommandUseCase aiClientCommandUseCase;
    private final OutputCommandUseCase outputCommandUseCase;
    private final RewardBridgeProperties rewardBridgeProperties;
    private final ExecutorService rewardBridgeExecutorService;

    public RenderedOutput consult(BuilderConsultCommand command) {
        log.info(
                "Builder consult started. group={}, type={}, outputFormat={}, clientIp={}, textLength={}, fileCount={}",
                command.group(),
                command.type(),
                describeOutputFormat(command.outputFormat()),
                command.clientIp(),
                command.text() == null ? 0 : command.text().length(),
                command.files() == null ? 0 : command.files().stream().filter(file -> file != null && !file.isEmpty()).count()
        );

        CompletableFuture<List<RagDocumentDto>> safetyRagFuture = CompletableFuture.supplyAsync(
                () -> ragQueryUseCase.queryByScenarioAndCategory(command.group(), command.type(), SAFETY_CATEGORY),
                rewardBridgeExecutorService
        );

        CompletableFuture<List<RagDocumentDto>> typeRagFuture = CompletableFuture.supplyAsync(
                () -> ragQueryUseCase.queryByScenario(command.group(), command.type()),
                rewardBridgeExecutorService
        );

        CompletableFuture<SourceResult> sourceFuture = CompletableFuture.supplyAsync(
                () -> sourceQueryUseCase.query(command.group(), command.type()),
                rewardBridgeExecutorService
        );

        CompletableFuture.allOf(safetyRagFuture, typeRagFuture, sourceFuture).join();

        List<RagDocumentDto> safetyDocuments = safetyRagFuture.join();
        List<RagDocumentDto> typeDocuments = typeRagFuture.join();
        SourceResult sourceResult = sourceFuture.join();
        List<RagDocumentDto> extraDocuments = ragQueryUseCase.queryByKeys(sourceResult.ragKeys());
        List<RagDocumentDto> businessDocuments = new java.util.ArrayList<>(typeDocuments);
        businessDocuments.addAll(extraDocuments);
        String analysisInstructions = builderCommandService.buildAnalysisInstructions(
                command,
                sourceResult,
                safetyDocuments,
                businessDocuments
        );

        ConsultBusinessResponse response = aiClientCommandUseCase.analyze(
                rewardBridgeProperties.getAi().getModels().getConsult(),
                command.text(),
                analysisInstructions,
                command.files()
        );

        log.info(
                "Builder consult completed. clientIp={}, group={}, type={}, outputFormat={}, status={}",
                command.clientIp(),
                command.group(),
                command.type(),
                describeOutputFormat(command.outputFormat()),
                response.isStatus()
        );
        return outputCommandUseCase.render(new OutputRenderCommand(
                command.group(),
                command.type(),
                command.outputFormat(),
                response
        ));
    }

    private String describeOutputFormat(com.citrus.rewardbridge.output.dto.OutputFormat outputFormat) {
        return outputFormat == null ? "(scenario default or ignored)" : outputFormat.value();
    }
}
