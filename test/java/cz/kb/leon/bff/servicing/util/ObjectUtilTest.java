package cz.kb.leon.bff.servicing.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ObjectUtilTest {

    @Test
    void toStringTest() {
        assertEquals("1", ObjectUtil.toString(1));
    }

    @Test
    void toStringTest_null() {
        assertEquals("null", ObjectUtil.toString(null));
    }

    @Test
    void toUpperCaseTest() {
        assertEquals("THIS IS STRANGELY FORMATTED TEXT.", ObjectUtil.toUpperCase("ThIs Is sTrAnGeLy FoRmAtTeD tExT."));
    }

    @Test
    void toUpperCaseTest_nullValue() {
        assertEquals(null, ObjectUtil.toUpperCase(null));
    }

    @Test
    void evaluateMessageTest() {
        assertEquals("This is a test message 1.", ObjectUtil.evaluateMessage("This is a test message {}.", 1));
    }

}
