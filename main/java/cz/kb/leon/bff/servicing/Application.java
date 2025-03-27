package cz.kb.leon.bff.servicing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Vychozi trida aplikace.
 */
@SpringBootApplication
public class Application {

    /**
     * Vychozi bod aplikace.
     *
     * @param args Argumenty aplikace.
     */
    @SuppressWarnings( {"squid:S1118", "squid:S2095", "checkstyle:regexpsinglelinejava", "checkstyle:com.puppycrawl.tools.checkstyle.checks.UncommentedMainCheck"})
    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
