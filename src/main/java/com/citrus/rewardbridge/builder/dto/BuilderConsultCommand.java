package com.citrus.rewardbridge.builder.dto;

import com.citrus.rewardbridge.output.dto.OutputFormat;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public record BuilderConsultCommand(
        String text,
        Integer builderId,
        OutputFormat outputFormat,
        List<MultipartFile> files,
        String clientIp
) {
}
