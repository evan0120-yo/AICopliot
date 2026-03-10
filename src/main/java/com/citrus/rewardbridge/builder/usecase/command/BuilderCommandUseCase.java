package com.citrus.rewardbridge.builder.usecase.command;

import com.citrus.rewardbridge.aiclient.usecase.command.AiClientCommandUseCase;
import com.citrus.rewardbridge.builder.dto.BuilderConsultCommand;
import com.citrus.rewardbridge.builder.dto.PromptAssemblyResult;
import com.citrus.rewardbridge.builder.service.command.BuilderCommandService;
import com.citrus.rewardbridge.common.config.RewardBridgeProperties;
import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;
import com.citrus.rewardbridge.common.entity.BuilderConfigEntity;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import com.citrus.rewardbridge.output.dto.OutputRenderCommand;
import com.citrus.rewardbridge.output.dto.RenderedOutput;
import com.citrus.rewardbridge.output.usecase.command.OutputCommandUseCase;
import com.citrus.rewardbridge.rag.dto.RagSupplementDto;
import com.citrus.rewardbridge.rag.usecase.query.RagQueryUseCase;
import com.citrus.rewardbridge.source.dto.SourceEntryDto;
import com.citrus.rewardbridge.source.dto.SourceLoadResult;
import com.citrus.rewardbridge.source.usecase.query.SourceQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
public class BuilderCommandUseCase {

    private static final Logger log = LoggerFactory.getLogger(BuilderCommandUseCase.class);

    private final BuilderCommandService builderCommandService;
    private final BuilderConfigRepository builderConfigRepository;
    private final RagQueryUseCase ragQueryUseCase;
    private final SourceQueryUseCase sourceQueryUseCase;
    private final AiClientCommandUseCase aiClientCommandUseCase;
    private final OutputCommandUseCase outputCommandUseCase;
    private final RewardBridgeProperties rewardBridgeProperties;
    private final ExecutorService rewardBridgeExecutorService;

    public RenderedOutput consult(BuilderConsultCommand command) {
        log.info(
                "Builder consult started. builderId={}, outputFormat={}, clientIp={}, textLength={}, fileCount={}",
                command.builderId(),
                describeOutputFormat(command.outputFormat()),
                command.clientIp(),
                command.text() == null ? 0 : command.text().length(),
                command.files() == null ? 0 : command.files().stream().filter(file -> file != null && !file.isEmpty()).count()
        );

        CompletableFuture<BuilderConfigEntity> builderConfigFuture = CompletableFuture.supplyAsync(
                () -> loadBuilderConfig(command.builderId()),
                rewardBridgeExecutorService
        );
        CompletableFuture<SourceLoadResult> sourceFuture = CompletableFuture.supplyAsync(
                () -> sourceQueryUseCase.loadByBuilderId(command.builderId()),
                rewardBridgeExecutorService
        );

        CompletableFuture.allOf(builderConfigFuture, sourceFuture).join();

        BuilderConfigEntity builderConfig = builderConfigFuture.join();
        SourceLoadResult sourceLoadResult = sourceFuture.join();
        Map<Long, List<RagSupplementDto>> ragSupplementsBySourceId = loadRagSupplements(sourceLoadResult.entries());
        PromptAssemblyResult promptAssemblyResult = builderCommandService.assemblePrompt(
                builderConfig,
                sourceLoadResult.entries(),
                ragSupplementsBySourceId,
                command.text()
        );

        ConsultBusinessResponse response = aiClientCommandUseCase.analyze(
                rewardBridgeProperties.getAi().getModels().getConsult(),
                promptAssemblyResult.userMessageText(),
                promptAssemblyResult.instructions(),
                command.files()
        );

        log.info(
                "Builder consult completed. clientIp={}, builderId={}, outputFormat={}, status={}",
                command.clientIp(),
                command.builderId(),
                describeOutputFormat(command.outputFormat()),
                response.isStatus()
        );
        return outputCommandUseCase.render(new OutputRenderCommand(
                builderConfig,
                command.outputFormat(),
                response
        ));
    }

    private BuilderConfigEntity loadBuilderConfig(Integer builderId) {
        return builderConfigRepository.findById(builderId)
                .orElseThrow(() -> new BusinessException(
                        "BUILDER_NOT_FOUND",
                        "Requested builder does not exist.",
                        HttpStatus.BAD_REQUEST
                ));
    }

    private Map<Long, List<RagSupplementDto>> loadRagSupplements(List<SourceEntryDto> sourceEntries) {
        Map<Long, CompletableFuture<List<RagSupplementDto>>> futures = new LinkedHashMap<>();
        for (SourceEntryDto sourceEntry : sourceEntries) {
            if (!sourceEntry.needsRagSupplement()) {
                continue;
            }

            futures.put(
                    sourceEntry.sourceId(),
                    CompletableFuture.supplyAsync(
                            () -> ragQueryUseCase.queryBySourceId(sourceEntry.sourceId()),
                            rewardBridgeExecutorService
                    )
            );
        }

        CompletableFuture.allOf(futures.values().toArray(CompletableFuture[]::new)).join();

        Map<Long, List<RagSupplementDto>> supplementsBySourceId = new LinkedHashMap<>();
        futures.forEach((sourceId, future) -> supplementsBySourceId.put(sourceId, future.join()));
        return supplementsBySourceId;
    }

    private String describeOutputFormat(com.citrus.rewardbridge.output.dto.OutputFormat outputFormat) {
        return outputFormat == null ? "(builder default or ignored)" : outputFormat.value();
    }
}
