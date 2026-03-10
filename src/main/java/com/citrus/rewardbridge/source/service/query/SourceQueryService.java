package com.citrus.rewardbridge.source.service.query;

import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.source.dto.SourceEntryDto;
import com.citrus.rewardbridge.source.dto.SourceLoadResult;
import com.citrus.rewardbridge.source.entity.SourceEntity;
import com.citrus.rewardbridge.source.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SourceQueryService {

    private static final Logger log = LoggerFactory.getLogger(SourceQueryService.class);

    private final SourceRepository sourceRepository;

    public SourceLoadResult loadByBuilderId(Integer builderId) {
        List<SourceEntity> entities = sourceRepository.findAllByBuilderIdOrdered(builderId);
        if (entities.isEmpty()) {
            throw new BusinessException(
                    "SOURCE_ENTRIES_NOT_FOUND",
                    "No source prompt entries were found for the requested builder.",
                    HttpStatus.INTERNAL_SERVER_ERROR
            );
        }

        List<SourceEntryDto> entries = entities.stream()
                .map(entity -> new SourceEntryDto(
                        entity.getSourceId(),
                        entity.getSourceType().getTypeCode(),
                        entity.getPrompts(),
                        entity.getOrderNo(),
                        entity.isNeedsRagSupplement()
                ))
                .toList();

        log.info("Loaded source entries for builder. builderId={}, entryCount={}", builderId, entries.size());
        return new SourceLoadResult(builderId, entries);
    }
}
