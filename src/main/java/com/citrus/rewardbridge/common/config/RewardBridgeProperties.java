package com.citrus.rewardbridge.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "rewardbridge")
@Getter
public class RewardBridgeProperties {

    private final Consult consult = new Consult();
    private final Ai ai = new Ai();

    @Getter
    @Setter
    public static class Consult {
        private int maxFiles = 10;
        private long maxFileSizeBytes = 20L * 1024 * 1024;
        private long maxTotalSizeBytes = 50L * 1024 * 1024;
    }

    @Getter
    public static class Ai {
        private final Models models = new Models();
        
        @Getter
        @Setter
        public static class Models {
            private String consult = "gpt-4o";
        }
    }
}
