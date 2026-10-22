package com.shieldtweak;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ShieldTweakConfigTest {
    private static ShieldTweakConfig read(String json) {
        return ShieldTweakConfig.fromJson(JsonParser.parseString(json).getAsJsonObject());
    }

    @Test
    void acceptsEveryDocumentedColorFormat() {
        ShieldTweakConfig cfg = read("""
                {"colorIdle":"#123456","colorRaising":"0xABCDEF","colorRaised":"0X55FF55","colorDisabled":16733525}
                """);
        assertEquals(0x123456, cfg.colorIdle);
        assertEquals(0xABCDEF, cfg.colorRaising);
        assertEquals(0x55FF55, cfg.colorRaised);
        assertEquals(0xFF5555, cfg.colorDisabled);
    }

    @Test
    void badFieldsDoNotDiscardValidLaterSettings() {
        ShieldTweakConfig cfg = read("""
                {"lowShieldEnabled":null,"lowShieldOffset":{},"tintEnabled":"wrong",
                 "colorIdle":[],"recoverSoundId":null,"recoverSoundId2":42,
                 "tintOthers":false,"observedDisableTicks":42,"observedDisableRadius":2.5}
                """);
        assertTrue(cfg.lowShieldEnabled);
        assertEquals(0.25, cfg.lowShieldOffset);
        assertTrue(cfg.tintEnabled);
        assertEquals(0xFFFFFF, cfg.colorIdle);
        assertEquals("minecraft:block.note_block.pling", cfg.recoverSoundId);
        assertEquals("minecraft:block.note_block.pling", cfg.recoverSoundId2);
        assertFalse(cfg.tintOthers);
        assertEquals(42, cfg.observedDisableTicks);
        assertEquals(2.5, cfg.observedDisableRadius);
    }

    @Test
    void rejectsNonFiniteNumbersAndClampsUnsafeRanges() {
        JsonObject json = JsonParser.parseString("""
                {"lowShieldOffset":-10,"observedDisableTicks":-5,"recoverSoundDelayTicks":-3,
                 "recoverSoundVolume":1e300,"recoverSoundPitch":-2,"recoverSoundPitch2":10}
                """).getAsJsonObject();
        json.addProperty("observedDisableRadius", Double.NaN);
        json.addProperty("recoverSoundVolume2", Double.POSITIVE_INFINITY);
        ShieldTweakConfig cfg = ShieldTweakConfig.fromJson(json);
        assertEquals(0.0, cfg.lowShieldOffset);
        assertEquals(0, cfg.observedDisableTicks);
        assertEquals(0, cfg.recoverSoundDelayTicks);
        assertEquals(1.0f, cfg.recoverSoundVolume);
        assertEquals(0.5f, cfg.recoverSoundPitch);
        assertEquals(2.0f, cfg.recoverSoundPitch2);
        assertEquals(3.0, cfg.observedDisableRadius);
        assertEquals(0.6f, cfg.recoverSoundVolume2);
    }

    @Test
    void preservesCustomPrecisionAndTheSingleNoteOption() {
        ShieldTweakConfig cfg = read("""
                {"lowShieldOffset":0.137,"recoverSoundId2":"   ","observedDisableRadius":0.75}
                """);
        assertEquals(0.137, cfg.lowShieldOffset);
        assertTrue(cfg.recoverSoundId2.isEmpty());
        assertEquals(0.75, cfg.observedDisableRadius);
    }
}
