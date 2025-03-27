package cz.kb.leon.bff.servicing;

import cz.kb.leon.featureflags.FeatureFlagService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class TestFeatureFlagConfiguration {
    @Bean
    @Primary
    public FeatureFlagService featureFlagService() {
        return new TestFeatureFlagService();
    }
}
