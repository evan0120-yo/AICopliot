package com.citrus.rewardbridge.output.dto;

public record RenderedFile(
        String fileName,
        String contentType,
        byte[] fileBytes
) {
}
