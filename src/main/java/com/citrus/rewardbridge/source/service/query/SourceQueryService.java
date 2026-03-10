package com.citrus.rewardbridge.source.service.query;

import com.citrus.rewardbridge.common.exception.BusinessException;
import com.citrus.rewardbridge.source.dto.SourceResult;
import com.citrus.rewardbridge.source.service.query.strategy.SourceStrategy;
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

    private final List<SourceStrategy> sourceStrategies;

    public SourceResult query(Integer group, Integer type) {
        log.info("Querying source data. group={}, type={}, strategyCount={}", group, type, sourceStrategies.size());
        return sourceStrategies.stream()
                .filter(strategy -> strategy.supports(group, type))
                .findFirst()
                .map(strategy -> strategy.query(group, type))
                .orElseThrow(() -> new BusinessException(
                        "SOURCE_STRATEGY_NOT_FOUND",
                        "No source strategy supports the requested consult scenario.",
                        HttpStatus.BAD_REQUEST
                ));
    }
}
