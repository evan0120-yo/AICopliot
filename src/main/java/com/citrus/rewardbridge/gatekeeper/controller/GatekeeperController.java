package com.citrus.rewardbridge.gatekeeper.controller;

import com.citrus.rewardbridge.common.response.ApiResponse;
import com.citrus.rewardbridge.gatekeeper.dto.ConsultRequest;
import com.citrus.rewardbridge.gatekeeper.service.guard.ClientIpResolver;
import com.citrus.rewardbridge.gatekeeper.usecase.command.GatekeeperCommandUseCase;
import com.citrus.rewardbridge.output.dto.RenderedOutput;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class GatekeeperController {

    private static final Logger log = LoggerFactory.getLogger(GatekeeperController.class);

    private final GatekeeperCommandUseCase gatekeeperCommandUseCase;
    private final ClientIpResolver clientIpResolver;

    @PostMapping(value = "/consult", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> consult(
            @Valid @ModelAttribute ConsultRequest request,
            HttpServletRequest httpServletRequest
    ) {
        log.info(
                "Received consult request. method={}, uri={}, group={}, type={}, outputFormat={}, textLength={}, file={}",
                httpServletRequest.getMethod(),
                httpServletRequest.getRequestURI(),
                request.getGroup(),
                request.getType(),
                request.getOutputFormat(),
                request.getText() == null ? 0 : request.getText().length(),
                describeFiles(request.getFiles())
        );

        String clientIp = clientIpResolver.resolve(httpServletRequest);
        log.info("Resolved client IP for consult request. clientIp={}", clientIp);

        RenderedOutput response = gatekeeperCommandUseCase.consult(request, clientIp);
        log.info("Consult request completed in Gatekeeper controller. clientIp={}, type={}", clientIp, request.getType());
        return toHttpResponse(response);
    }

    private String describeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return "none (0 files)";
        }

        long actualFileCount = files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .count();

        if (actualFileCount == 0) {
            return "none (0 files)";
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .map(this::describeSingleFile)
                .reduce((left, right) -> left + ", " + right)
                .orElse("none (0 files)");
    }

    private String describeSingleFile(MultipartFile file) {
        return "%s (%d bytes)".formatted(file.getOriginalFilename(), file.getSize());
    }

    private ResponseEntity<?> toHttpResponse(RenderedOutput renderedOutput) {
        return ResponseEntity.ok(ApiResponse.success(renderedOutput.body()));
    }
}
