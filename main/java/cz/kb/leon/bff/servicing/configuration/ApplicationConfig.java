package cz.kb.leon.bff.servicing.configuration;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.retry.annotation.EnableRetry;

/**
 * XML free configuration of the base component scan.
 *
 * <p>It registers all application components.
 *
 * @author here comes a real name
 */
@Configuration
@EnableRetry
public class ApplicationConfig {

    @Bean
    public MessageSource phraseMessages() {
        final ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("phrase/locales");
        source.setAlwaysUseMessageFormat(true);
        return source;
    }

}
