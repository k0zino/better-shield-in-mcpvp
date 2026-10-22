package com.shieldtweak;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.shieldtweak.ConfigEditorValues.NumberKind.*;

class ConfigEditorValuesTest {
    private static final ConfigEditorValues.NumberRule DECIMALS =
            new ConfigEditorValues.NumberRule(DECIMAL, 0, Double.MAX_VALUE, 0.05, 0.25);
    private static final ConfigEditorValues.NumberRule VOLUME =
            new ConfigEditorValues.NumberRule(FLOAT, 0, 1, 0.05, 0.6f);
    private static final ConfigEditorValues.NumberRule TICKS =
            new ConfigEditorValues.NumberRule(INTEGER, 0, Integer.MAX_VALUE, 1, 3);

    @Test
    void colorsAcceptPrefixesAndCaseButRejectPartialOrOverflowValues() {
        for (String value : new String[]{"#aBcDeF", "ABCDEF", "0xABCDEF", " 0Xabcdef "}) {
            assertEquals(0xABCDEF, ConfigEditorValues.color(value).value());
        }
        for (String value : new String[]{"", "#", "#FFF", "#12345", "#1234567", "-12345", "#GG5555"}) {
            assertFalse(ConfigEditorValues.color(value).valid(), value);
        }
        assertEquals("#000007", ConfigEditorValues.hex(7));
    }

    @Test
    void incompleteDraftPreservesPreviewAndCannotOverwriteConfiguration() {
        AtomicInteger config = new AtomicInteger(0x55FF55);
        var draft = new ConfigEditorValues.Draft<>("#55FF55", ConfigEditorValues::color);
        draft.edit("#FF5555");
        assertEquals(0x55FF55, config.get(), "Typing must not save settings");
        assertEquals(0xFF5555, draft.preview());
        draft.edit("#FF");
        assertEquals("#FF", draft.text(), "Keep the user's incomplete text through widget rebuilds");
        assertEquals(0xFF5555, draft.preview());
        assertFalse(draft.apply(config::set));
        assertEquals(0x55FF55, config.get());
        draft.edit("0x123456");
        assertTrue(draft.apply(config::set));
        assertEquals(0x123456, config.get());
    }

    @Test
    void numericDraftDoesNotCommitUntilApplied() {
        double[] config = {0.137};
        var draft = new ConfigEditorValues.Draft<>("0.137", DECIMALS::parse);
        draft.edit("0.123456789");
        assertEquals(0.137, config[0]);
        draft.edit("-");
        assertFalse(draft.apply(value -> config[0] = value));
        assertEquals(0.137, config[0]);
        draft.edit("0.123456789");
        assertTrue(draft.apply(value -> config[0] = value));
        assertEquals(0.123456789, config[0]);
    }

    @Test
    void rejectsNonFiniteValuesAndOutOfRangeAudioWithoutSilentlyClampingInput() {
        for (String text : new String[]{"NaN", "Infinity", "1e9999", "-0.01", "1.01", ""}) {
            assertFalse(VOLUME.parse(text).valid(), text);
        }
        var pitch = new ConfigEditorValues.NumberRule(FLOAT, 0.5, 2, 0.05, 1.5);
        assertFalse(pitch.parse("0.49").valid());
        assertFalse(pitch.parse("2.01").valid());
        assertTrue(pitch.parse("0.5").valid());
        assertTrue(pitch.parse("2").valid());
    }

    @Test
    void tickInputRejectsFractionsAndOverflowBeforeNarrowingToInt() {
        assertFalse(TICKS.parse("1.5").valid());
        assertFalse(TICKS.parse("2147483648").valid());
        assertFalse(TICKS.parse("-1").valid());
        assertEquals(3.0, TICKS.parse("3.0").value());
        assertEquals((double) Integer.MAX_VALUE, TICKS.parse("2147483647").value());
    }

    @Test
    void openingAnExistingValuePreservesCustomPrecisionAndFloatFormatting() {
        for (double value : new double[]{0.137, 0.123456789012345, Double.MIN_VALUE, Double.MAX_VALUE}) {
            String text = DECIMALS.format(value);
            assertTrue(text.length() < 512);
            assertEquals(value, DECIMALS.parse(text).value());
        }
        assertEquals("0.6", VOLUME.format(0.6f));
        assertEquals("100", TICKS.format(100));
    }

    @Test
    void steppingKeepsOffPresetValuesAndAvoidsBinaryFloatingPointNoise() {
        assertEquals("0.187", DECIMALS.step("0.137", 1));
        assertEquals("0.087", DECIMALS.step("0.137", -1));
        assertEquals("0.65", VOLUME.step("0.6", 1));
        assertEquals("0.6", VOLUME.step("0.65", -1));
        assertEquals("-", DECIMALS.step("-", 1));
    }

    @Test
    void stepButtonsStopAtBoundsWithoutOverflow() {
        assertEquals("0", VOLUME.step("0", -1));
        assertEquals("1", VOLUME.step("1", 1));
        assertEquals("0", DECIMALS.step("0.01", -1));
        assertEquals("2147483647", TICKS.step("2147483647", 1));
        assertEquals("0", TICKS.step("0", -1));
    }
}
