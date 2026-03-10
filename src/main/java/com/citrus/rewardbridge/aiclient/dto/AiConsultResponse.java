package com.citrus.rewardbridge.aiclient.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiConsultResponse {

    private boolean status;
    private String statusAns;
    private String response;
}
