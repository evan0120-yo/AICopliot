package com.citrus.rewardbridge.rag.usecase.query;

import com.citrus.rewardbridge.rag.dto.RagSupplementDto;
import com.citrus.rewardbridge.rag.service.query.RagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagQueryUseCase {

    private final RagQueryService ragQueryService;

    public List<RagSupplementDto> queryBySourceId(Long sourceId) {
        return ragQueryService.queryBySourceId(sourceId);
    }
}
