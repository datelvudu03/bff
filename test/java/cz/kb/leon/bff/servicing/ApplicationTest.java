package cz.kb.leon.bff.servicing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test startu aplikace.
 *
 * @author here comes a real name
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(SpringExtension.class)
@Import(TestFeatureFlagConfiguration.class)
class ApplicationTest {
    @Test
    void contextLoads() {
        assertTrue(true);
    }
}
