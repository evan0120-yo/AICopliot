package com.citrus.rewardbridge.source.usecase.query;

import com.citrus.rewardbridge.source.dto.SourceResult;
import com.citrus.rewardbridge.source.service.query.SourceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SourceQueryUseCase {

    private final SourceQueryService sourceQueryService;

    public SourceResult query(Integer group, Integer type) {
        return sourceQueryService.query(group, type);
    }
}
