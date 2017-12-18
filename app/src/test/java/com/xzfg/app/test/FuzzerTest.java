package com.xzfg.app.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import com.xzfg.app.security.Fuzz;

import org.junit.Test;

/**
 * Test the "fuzzer".
 */
public class FuzzerTest {
    private static final String message = "The rain in Spain stays mainly in the plains.";
    private static final String key = "SOMEKEY";

    @Test
    public void fuzzerTest() throws Exception {
        String fuzzedResult = Fuzz.en(message, key);
        assertNotSame(fuzzedResult,message);
        String defuzzedResult = Fuzz.de(fuzzedResult, key);
        assertEquals(defuzzedResult, message);
    }

}
