package com.citrus.rewardbridge.gatekeeper.service.guard;

import com.citrus.rewardbridge.common.config.RewardBridgeProperties;
import com.citrus.rewardbridge.common.repository.BuilderConfigRepository;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.gatekeeper.dto.ConsultGuardResult;
import com.citrus.rewardbridge.gatekeeper.dto.ConsultRequest;
import com.citrus.rewardbridge.output.dto.OutputFormat;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConsultGuardService {

    private static final Logger log = LoggerFactory.getLogger(ConsultGuardService.class);
    private static final Set<String> SUPPORTED_FILE_EXTENSIONS = Set.of(
            "pdf", "doc", "docx",
            "jpg", "jpeg", "png", "webp", "gif", "bmp"
    );

    private final RewardBridgeProperties rewardBridgeProperties;
    private final BuilderConfigRepository builderConfigRepository;

    public ConsultGuardResult guard(ConsultRequest request, String clientIp) {
        Integer builderId = request.getBuilderId();
        OutputFormat outputFormat = normalizeOutputFormat(request.getOutputFormat());
        long actualFileCount = countActualFiles(request.getFiles());
        log.info(
                "Starting consult guard validation. clientIp={}, builderId={}, outputFormat={}, fileCount={}",
                clientIp,
                builderId,
                describeOutputFormat(outputFormat),
                actualFileCount
        );

        validateClientIp(clientIp);
        validateBuilder(builderId);
        validateFiles(request.getFiles());

        log.info(
                "Consult guard validation passed. clientIp={}, builderId={}, outputFormat={}",
                clientIp,
                builderId,
                describeOutputFormat(outputFormat)
        );
        return new ConsultGuardResult(builderId, outputFormat);
    }

    private void validateClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            log.warn("Consult guard rejected request because client IP is missing.");
            throw new BusinessException("CLIENT_IP_MISSING", "Client IP could not be resolved.", HttpStatus.BAD_REQUEST);
        }

        log.info("Client IP validation passed. clientIp={}", clientIp);

        // TODO: enforce IP allowlist / blocklist when PM rollout requires network-level validation.
    }

    private void validateBuilder(Integer builderId) {
        if (builderId == null) {
            log.warn("Consult guard rejected request because builderId is missing.");
            throw new BusinessException("BUILDER_ID_MISSING", "builderId is required.", HttpStatus.BAD_REQUEST);
        }

        builderConfigRepository.findById(builderId)
                .ifPresentOrElse(builderConfig -> {
                    if (!builderConfig.isActive()) {
                        log.warn("Consult guard rejected request because builder is inactive. builderId={}", builderId);
                        throw new BusinessException("BUILDER_INACTIVE", "Requested builder is inactive.", HttpStatus.FORBIDDEN);
                    }
                    log.info("Builder validation passed. builderId={}, builderCode={}", builderId, builderConfig.getBuilderCode());
                }, () -> {
                    log.warn("Consult guard rejected request because builder does not exist. builderId={}", builderId);
                    throw new BusinessException("BUILDER_NOT_FOUND", "Requested builder does not exist.", HttpStatus.BAD_REQUEST);
                });
    }

    private void validateFiles(List<MultipartFile> files) {
        long actualFileCount = countActualFiles(files);
        if (files == null || files.isEmpty() || actualFileCount == 0) {
            log.info("File validation skipped because no files were uploaded.");
            return;
        }

        if (actualFileCount > rewardBridgeProperties.getConsult().getMaxFiles()) {
            log.warn(
                    "Consult guard rejected request because file count exceeded limit. actualFileCount={}, maxFiles={}",
                    actualFileCount,
                    rewardBridgeProperties.getConsult().getMaxFiles()
            );
            throw new BusinessException(
                    "FILE_COUNT_EXCEEDED",
                    "Uploaded file count exceeds the configured limit.",
                    HttpStatus.BAD_REQUEST
            );
        }

        long totalBytes = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .mapToLong(MultipartFile::getSize)
                .sum();

        if (totalBytes > rewardBridgeProperties.getConsult().getMaxTotalSizeBytes()) {
            log.warn(
                    "Consult guard rejected request because total file size exceeded limit. totalBytes={}, maxTotalSizeBytes={}",
                    totalBytes,
                    rewardBridgeProperties.getConsult().getMaxTotalSizeBytes()
            );
            throw new BusinessException(
                    "FILE_TOTAL_SIZE_EXCEEDED",
                    "Uploaded files exceed the configured total size limit.",
                    HttpStatus.BAD_REQUEST
            );
        }

        log.info("Validating uploaded files. fileCount={}, totalBytes={}", actualFileCount, totalBytes);

        for (MultipartFile file : files) {
            validateSingleFile(file);
        }
    }

    private void validateSingleFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            log.info("Skipping empty multipart entry during file validation.");
            return;
        }

        String filename = file.getOriginalFilename();
        String extension = extractExtension(filename);
        log.info(
                "Validating uploaded file. filename={}, size={}, extension={}",
                filename,
                file.getSize(),
                extension
        );

        if (!SUPPORTED_FILE_EXTENSIONS.contains(extension)) {
            log.warn("Consult guard rejected file because extension is unsupported. filename={}, extension={}", filename, extension);
            throw new BusinessException(
                    "UNSUPPORTED_FILE_TYPE",
                    "Only PDF, DOC, DOCX, JPG, JPEG, PNG, WEBP, GIF, and BMP files are supported.",
                    HttpStatus.BAD_REQUEST
            );
        }

        if (file.getSize() > rewardBridgeProperties.getConsult().getMaxFileSizeBytes()) {
            log.warn(
                    "Consult guard rejected file because size exceeded limit. filename={}, size={}, maxFileSizeBytes={}",
                    filename,
                    file.getSize(),
                    rewardBridgeProperties.getConsult().getMaxFileSizeBytes()
            );
            throw new BusinessException(
                    "FILE_SIZE_EXCEEDED",
                    "Uploaded file exceeds the configured per-file size limit.",
                    HttpStatus.BAD_REQUEST
            );
        }

        log.info("File validation passed. filename={}, extension={}", filename, extension);

        // TODO: enforce MIME validation when PM upload policy needs a stronger trust boundary.
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            return "";
        }

        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    private long countActualFiles(List<MultipartFile> files) {
        if (files == null) {
            return 0;
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .count();
    }

    private OutputFormat normalizeOutputFormat(String outputFormat) {
        if (!StringUtils.hasText(outputFormat)) {
            return null;
        }

        return OutputFormat.from(outputFormat.trim())
                .orElseThrow(() -> new BusinessException(
                        "UNSUPPORTED_OUTPUT_FORMAT",
                        "Only markdown and xlsx output formats are supported.",
                        HttpStatus.BAD_REQUEST
                ));
    }

    private String describeOutputFormat(OutputFormat outputFormat) {
        return outputFormat == null ? "(scenario default or ignored)" : outputFormat.value();
    }
}
