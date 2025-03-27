package cz.kb.leon.bff.servicing.infra;

import org.junit.jupiter.api.Test;
import cz.kb.leon.lib.test.utils.contract.PreAuthorizedCheckUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PreAuthorizedCheckerTest {

    @Test
    void checkPreAuthorizedAnnotation() {
        assertDoesNotThrow(() ->  PreAuthorizedCheckUtils.scanServices("cz.kb.leon.bff.servicing.infra.ui"));
    }
}