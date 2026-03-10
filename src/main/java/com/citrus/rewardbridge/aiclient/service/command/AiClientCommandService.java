package com.citrus.rewardbridge.aiclient.service.command;

import com.citrus.rewardbridge.aiclient.dto.AiConsultResponse;
import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;
import com.citrus.rewardbridge.common.exception.BusinessException;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIServiceException;
import com.openai.errors.PermissionDeniedException;
import com.openai.errors.UnauthorizedException;
import com.openai.models.files.FileCreateParams;
import com.openai.models.files.FileObject;
import com.openai.models.files.FilePurpose;
import com.openai.models.responses.EasyInputMessage;
import com.openai.models.responses.ResponseInputContent;
import com.openai.models.responses.ResponseInputFile;
import com.openai.models.responses.ResponseInputImage;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseInputText;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.StructuredResponseOutputItem;
import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class AiClientCommandService {

    private static final Logger log = LoggerFactory.getLogger(AiClientCommandService.class);

    private final ObjectProvider<OpenAIClient> openAIClientProvider;

    public ConsultBusinessResponse analyze(
            String model,
            String text,
            String instructions,
            @Nullable List<MultipartFile> attachments
    ) {
        String normalizedText = normalizeText(text);
        List<MultipartFile> safeAttachments = attachments == null ? List.of() : attachments.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();

        log.info(
                "Calling OpenAI consult analysis. model={}, textLength={}, attachmentCount={}",
                model,
                normalizedText.length(),
                safeAttachments.size()
        );
        log.info("OpenAI consult raw text payload:\n{}", normalizedText);
        log.info("OpenAI consult instructions payload:\n{}", instructions);
        log.info("OpenAI consult attachment payloads={}", describeAttachments(safeAttachments));

        try {
            List<ResponseInputContent> contents = new ArrayList<>();
            contents.add(ResponseInputContent.ofInputText(
                    ResponseInputText.builder()
                            .text(normalizedText)
                            .build()
            ));

            for (MultipartFile attachment : safeAttachments) {
                contents.add(uploadAttachmentAsContent(attachment));
            }

            StructuredResponse<AiConsultResponse> response = client().responses().create(
                    StructuredResponseCreateParams.<AiConsultResponse>builder()
                            .model(model)
                            .instructions(instructions)
                            .store(false)
                            .text(AiConsultResponse.class)
                            .inputOfResponse(List.of(
                                    ResponseInputItem.ofEasyInputMessage(
                                            EasyInputMessage.builder()
                                                    .role(EasyInputMessage.Role.USER)
                                                    .contentOfResponseInputMessageContentList(contents)
                                                    .build()
                                    )
                            ))
                            .build()
            );

            AiConsultResponse parsed = extractStructuredOutput(response);
            log.info(
                    "OpenAI consult analysis completed. status={}, statusAns={}, responseLength={}, responsePreview={}",
                    parsed.isStatus(),
                    parsed.getStatusAns(),
                    parsed.getResponse() == null ? 0 : parsed.getResponse().length(),
                    preview(parsed.getResponse())
            );
            return toBusinessResponse(parsed);
        } catch (BusinessException ex) {
            throw ex;
        } catch (UnauthorizedException | PermissionDeniedException ex) {
            log.warn("OpenAI authorization failed during consult analysis. model={}", model, ex);
            throw new BusinessException(
                    "OPENAI_AUTH_FAILED",
                    "OpenAI authorization failed. Please verify OPENAI_API_KEY / OPENAI_BASE_URL configuration.",
                    HttpStatus.BAD_GATEWAY
            );
        } catch (Exception ex) {
            log.error("OpenAI consult analysis failed. model={}, attachmentCount={}", model, safeAttachments.size(), ex);
            throw new BusinessException(
                    "OPENAI_ANALYSIS_FAILED",
                    "OpenAI consult analysis failed: " + ex.getMessage(),
                    HttpStatus.BAD_GATEWAY
            );
        }
    }

    private ResponseInputContent uploadAttachmentAsContent(MultipartFile attachment) throws IOException {
        String originalFilename = attachment.getOriginalFilename() == null ? "upload.bin" : attachment.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        boolean imageFile = isImage(extension);
        FilePurpose purpose = imageFile ? FilePurpose.VISION : FilePurpose.USER_DATA;

        Path tempFile = Files.createTempFile("rewardbridge-", sanitizeSuffix(originalFilename));
        try {
            attachment.transferTo(tempFile);
            FileObject uploadedFile;
            try {
                uploadedFile = client().files().create(
                        FileCreateParams.builder()
                                .file(tempFile)
                                .purpose(purpose)
                                .build()
                );
            } catch (UnauthorizedException | PermissionDeniedException ex) {
                log.warn("OpenAI authorization failed during attachment upload. filename={}", originalFilename, ex);
                throw new BusinessException(
                        "OPENAI_AUTH_FAILED",
                        "OpenAI authorization failed while uploading attachment. Please verify OPENAI_API_KEY / OPENAI_BASE_URL configuration.",
                        HttpStatus.BAD_GATEWAY
                );
            } catch (OpenAIServiceException ex) {
                log.warn("OpenAI rejected attachment upload. filename={}, purpose={}", originalFilename, purpose.asString(), ex);
                throw new BusinessException(
                        "ATTACHMENT_UPLOAD_REJECTED",
                        "OpenAI rejected the uploaded attachment. Please verify the file can be accepted by the configured model/API.",
                        HttpStatus.BAD_GATEWAY
                );
            }

            log.info(
                    "Uploaded attachment to OpenAI. filename={}, openAiFileId={}, purpose={}",
                    originalFilename,
                    uploadedFile.id(),
                    purpose.asString()
            );

            if (imageFile) {
                return ResponseInputContent.ofInputImage(
                        ResponseInputImage.builder()
                                .fileId(uploadedFile.id())
                                .detail(ResponseInputImage.Detail.AUTO)
                                .build()
                );
            }

            return ResponseInputContent.ofInputFile(
                    ResponseInputFile.builder()
                            .fileId(uploadedFile.id())
                            .build()
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (IOException ex) {
            log.warn("Attachment transfer failed before OpenAI upload. filename={}", originalFilename, ex);
            throw new BusinessException(
                    "ATTACHMENT_UPLOAD_FAILED",
                    "Attachment could not be prepared for OpenAI upload.",
                    HttpStatus.BAD_GATEWAY
            );
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    private ResponseInputItem buildUserMessage(String text, List<ResponseInputContent> attachments) {
        List<ResponseInputContent> contents = new ArrayList<>();
        contents.add(ResponseInputContent.ofInputText(
                ResponseInputText.builder()
                        .text(text)
                        .build()
        ));
        contents.addAll(attachments);

        return ResponseInputItem.ofEasyInputMessage(
                EasyInputMessage.builder()
                        .role(EasyInputMessage.Role.USER)
                        .contentOfResponseInputMessageContentList(contents)
                        .build()
        );
    }

    private AiConsultResponse extractStructuredOutput(StructuredResponse<AiConsultResponse> response) {
        for (StructuredResponseOutputItem<AiConsultResponse> outputItem : response.output()) {
            if (!outputItem.isMessage()) {
                continue;
            }

            return outputItem.asMessage().content().stream()
                    .filter(content -> content.isOutputText() && content.outputText().isPresent())
                    .map(content -> content.asOutputText())
                    .findFirst()
                    .orElseThrow(() -> new BusinessException(
                            "OPENAI_EMPTY_OUTPUT",
                            "OpenAI returned a message without structured output text.",
                            HttpStatus.BAD_GATEWAY
                    ));
        }

        throw new BusinessException(
                "OPENAI_EMPTY_OUTPUT",
                "OpenAI returned no structured message output.",
                HttpStatus.BAD_GATEWAY
        );
    }

    private ConsultBusinessResponse toBusinessResponse(AiConsultResponse aiResponse) {
        return new ConsultBusinessResponse(
                aiResponse.isStatus(),
                aiResponse.getStatusAns(),
                aiResponse.getResponse(),
                null
        );
    }

    private OpenAIClient client() {
        OpenAIClient client = openAIClientProvider.getIfAvailable();
        if (client == null) {
            throw new BusinessException(
                    "OPENAI_CLIENT_UNAVAILABLE",
                    "OpenAI client is not configured. Please provide OPENAI_API_KEY before running consult flow.",
                    HttpStatus.SERVICE_UNAVAILABLE
            );
        }
        return client;
    }

    private boolean isImage(String extension) {
        return switch (extension) {
            case "jpg", "jpeg", "png", "webp", "gif", "bmp" -> true;
            default -> false;
        };
    }

    private String extractExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex < 0) {
            return "";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private String sanitizeSuffix(String filename) {
        String safe = filename.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!safe.contains(".")) {
            return ".bin";
        }
        return "-" + safe;
    }

    private String normalizeText(String text) {
        if (text == null || text.isBlank()) {
            return "用戶沒有額外需求";
        }
        return text;
    }

    private String describeAttachments(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return "[]";
        }

        return attachments.stream()
                .map(file -> "%s(%d bytes)".formatted(file.getOriginalFilename(), file.getSize()))
                .toList()
                .toString();
    }

    private String preview(String response) {
        if (response == null || response.isBlank()) {
            return "(empty)";
        }

        String normalized = response.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= 300) {
            return normalized;
        }
        return normalized.substring(0, 300) + "...";
    }
}
