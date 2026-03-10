package com.citrus.rewardbridge.aiclient.usecase.command;

import com.citrus.rewardbridge.aiclient.service.command.AiClientCommandService;
import com.citrus.rewardbridge.common.dto.ConsultBusinessResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AiClientCommandUseCase {

    private final AiClientCommandService aiClientCommandService;

    public ConsultBusinessResponse analyze(String model, String text, String instructions, List<MultipartFile> attachments) {
        return aiClientCommandService.analyze(model, text, instructions, attachments);
    }
}
