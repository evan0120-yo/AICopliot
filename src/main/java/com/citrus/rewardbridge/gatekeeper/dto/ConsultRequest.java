package com.citrus.rewardbridge.gatekeeper.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ConsultRequest {

    private String text;

    private Integer group;

    @NotNull(message = "type is required")
    private Integer type;

    private String outputFormat;

    private List<MultipartFile> files;

}
