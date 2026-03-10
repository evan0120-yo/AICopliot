package com.citrus.rewardbridge.source.service.query.strategy;

import com.citrus.rewardbridge.common.scenario.ConsultScenario;
import com.citrus.rewardbridge.source.repository.SourceRagMappingRepository;
import com.citrus.rewardbridge.source.repository.SourceReferenceItemRepository;
import com.citrus.rewardbridge.source.repository.SourceScenarioConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class QaSmokeDocStrategy extends BaseScenarioSourceStrategy {

    private static final Logger log = LoggerFactory.getLogger(QaSmokeDocStrategy.class);

    public QaSmokeDocStrategy(
            SourceScenarioConfigRepository sourceScenarioConfigRepository,
            SourceReferenceItemRepository sourceReferenceItemRepository,
            SourceRagMappingRepository sourceRagMappingRepository
    ) {
        super(sourceScenarioConfigRepository, sourceReferenceItemRepository, sourceRagMappingRepository);
    }

    @Override
    protected ConsultScenario scenario() {
        return ConsultScenario.QA_SMOKE_DOC;
    }

    @Override
    protected Logger log() {
        return log;
    }
}
