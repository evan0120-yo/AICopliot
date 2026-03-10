package com.citrus.rewardbridge.rag.usecase.query;

import com.citrus.rewardbridge.rag.dto.RagDocumentDto;
import com.citrus.rewardbridge.rag.service.query.RagQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RagQueryUseCase {

    private final RagQueryService ragQueryService;

    public List<RagDocumentDto> queryByKeys(List<String> keys) {
        return ragQueryService.queryByKeys(keys);
    }

    public List<RagDocumentDto> queryByScenarioAndCategory(Integer group, Integer type, String category) {
        return ragQueryService.queryByScenarioAndCategory(group, type, category);
    }

    public List<RagDocumentDto> queryByScenario(Integer group, Integer type) {
        return ragQueryService.queryByScenario(group, type);
    }
}
