package com.citrus.rewardbridge.gatekeeper.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ConsultRequest {

    @NotNull(message = "builderId is required")
    private Integer builderId;

    private String text;

    private String outputFormat;

    private List<MultipartFile> files;

}
