package com.citrus.rewardbridge.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConsultBusinessResponse {

    private boolean status;
    private String statusAns;
    private String response;
    private ConsultFilePayload file;

    public static ConsultBusinessResponse attachmentRejected() {
        return new ConsultBusinessResponse(false, "串入檔案格式錯誤", "", null);
    }
}
