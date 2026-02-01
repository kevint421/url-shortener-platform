package com.urlshortener.common.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShortCodeGeneratorTest {

    @Test
    void generateFromId_withValidId_returnsShortCode() {
        String shortCode = ShortCodeGenerator.generateFromId(1L);

        assertNotNull(shortCode);
        assertTrue(shortCode.length() >= 7);
        assertTrue(ShortCodeGenerator.isValidShortCode(shortCode));
    }

    @Test
    void generateFromId_withLargeId_returnsValidShortCode() {
        String shortCode = ShortCodeGenerator.generateFromId(999999L);

        assertNotNull(shortCode);
        assertTrue(ShortCodeGenerator.isValidShortCode(shortCode));
    }

    @Test
    void generateFromId_withNullId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ShortCodeGenerator.generateFromId(null);
        });
    }

    @Test
    void generateFromId_withZeroId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ShortCodeGenerator.generateFromId(0L);
        });
    }

    @Test
    void generateFromId_withNegativeId_throwsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            ShortCodeGenerator.generateFromId(-1L);
        });
    }

    @Test
    void generateFromId_producesUniqueCodesForDifferentIds() {
        String code1 = ShortCodeGenerator.generateFromId(1L);
        String code2 = ShortCodeGenerator.generateFromId(2L);
        String code3 = ShortCodeGenerator.generateFromId(100L);

        assertNotEquals(code1, code2);
        assertNotEquals(code1, code3);
        assertNotEquals(code2, code3);
    }

    @Test
    void generateRandom_returnsValidShortCode() {
        String shortCode = ShortCodeGenerator.generateRandom();

        assertNotNull(shortCode);
        assertEquals(7, shortCode.length());
        assertTrue(ShortCodeGenerator.isValidShortCode(shortCode));
    }

    @Test
    void generateRandom_producesRandomCodes() {
        String code1 = ShortCodeGenerator.generateRandom();
        String code2 = ShortCodeGenerator.generateRandom();
        String code3 = ShortCodeGenerator.generateRandom();

        // Extremely unlikely all three are the same
        assertFalse(code1.equals(code2) && code2.equals(code3));
    }

    @Test
    void isValidShortCode_withValidCode_returnsTrue() {
        assertTrue(ShortCodeGenerator.isValidShortCode("abc123"));
        assertTrue(ShortCodeGenerator.isValidShortCode("XyZ789"));
        assertTrue(ShortCodeGenerator.isValidShortCode("A"));
    }

    @Test
    void isValidShortCode_withNull_returnsFalse() {
        assertFalse(ShortCodeGenerator.isValidShortCode(null));
    }

    @Test
    void isValidShortCode_withEmptyString_returnsFalse() {
        assertFalse(ShortCodeGenerator.isValidShortCode(""));
    }

    @Test
    void isValidShortCode_withTooLongCode_returnsFalse() {
        assertFalse(ShortCodeGenerator.isValidShortCode("12345678901")); // 11 chars
    }

    @Test
    void isValidShortCode_withInvalidCharacters_returnsFalse() {
        assertFalse(ShortCodeGenerator.isValidShortCode("abc#123"));
        assertFalse(ShortCodeGenerator.isValidShortCode("test@url"));
        assertFalse(ShortCodeGenerator.isValidShortCode("hello world"));
    }
}
