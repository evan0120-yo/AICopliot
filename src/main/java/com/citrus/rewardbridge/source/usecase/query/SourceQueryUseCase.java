package com.citrus.rewardbridge.source.usecase.query;

import com.citrus.rewardbridge.source.dto.SourceLoadResult;
import com.citrus.rewardbridge.source.service.query.SourceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SourceQueryUseCase {

    private final SourceQueryService sourceQueryService;

    public SourceLoadResult loadByBuilderId(Integer builderId) {
        return sourceQueryService.loadByBuilderId(builderId);
    }
}
