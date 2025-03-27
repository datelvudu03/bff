package cz.kb.leon.bff.servicing.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;

@Data
@Configuration
@ConfigurationProperties("leon.servicing.zone-id")
@NoArgsConstructor
public class ZoneIdProperties {

    private ZoneId prague;
}
