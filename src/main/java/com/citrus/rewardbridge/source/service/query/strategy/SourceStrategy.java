package com.citrus.rewardbridge.source.service.query.strategy;

import com.citrus.rewardbridge.source.dto.SourceResult;

public interface SourceStrategy {

    boolean supports(Integer group, Integer type);

    SourceResult query(Integer group, Integer type);
}
