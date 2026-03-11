package com.citrus.rewardbridge.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.util.Locale;

public final class RagRetrievalModeNormalizer {

    public static final String FULL_CONTEXT = "full_context";

    private static final Logger log = LoggerFactory.getLogger(RagRetrievalModeNormalizer.class);

    private RagRetrievalModeNormalizer() {
    }

    public static String normalizeForRead(String rawRetrievalMode, Long sourceId, Long ragId, String ragType) {
        if (!StringUtils.hasText(rawRetrievalMode)) {
            return FULL_CONTEXT;
        }

        String normalized = rawRetrievalMode.trim().toLowerCase(Locale.ROOT);
        if (FULL_CONTEXT.equals(normalized)) {
            return FULL_CONTEXT;
        }

        log.warn(
                "Falling back unsupported RAG retrieval mode to full_context. sourceId={}, ragId={}, ragType={}, rawMode={}",
                sourceId,
                ragId,
                ragType,
                rawRetrievalMode
        );
        return FULL_CONTEXT;
    }
}
