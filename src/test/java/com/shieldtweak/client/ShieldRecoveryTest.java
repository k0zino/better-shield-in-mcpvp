package com.shieldtweak.client;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.shieldtweak.client.ShieldRecovery.Note.*;

class ShieldRecoveryTest {
    @Test
    void recoveryPlaysExactlyTwoNotesAtTheConfiguredTickSpacing() {
        ShieldRecovery recovery = new ShieldRecovery();
        assertEquals(NONE, recovery.tick(false, true, 3, true));
        assertEquals(NONE, recovery.tick(true, true, 3, true));
        assertEquals(FIRST, recovery.tick(false, true, 3, true));
        assertEquals(NONE, recovery.tick(false, true, 3, true));
        assertEquals(NONE, recovery.tick(false, true, 3, true));
        assertEquals(SECOND, recovery.tick(false, true, 3, true));
        assertEquals(NONE, recovery.tick(false, true, 3, true));
    }

    @Test
    void switchingTheSoundOffCancelsThePendingSecondNote() {
        ShieldRecovery recovery = new ShieldRecovery();
        recovery.reset(true);
        assertEquals(FIRST, recovery.tick(false, true, 3, true));
        assertEquals(NONE, recovery.tick(false, false, 3, true));
        for (int i = 0; i < 5; i++) {
            assertEquals(NONE, recovery.tick(false, true, 3, true));
        }
    }

    @Test
    void anotherDisableCancelsTheOldSecondNoteAndStartsANewRecovery() {
        ShieldRecovery recovery = new ShieldRecovery();
        recovery.reset(true);
        assertEquals(FIRST, recovery.tick(false, true, 1, true));
        assertEquals(NONE, recovery.tick(true, true, 1, true));
        assertEquals(NONE, recovery.tick(true, true, 1, true));
        assertEquals(FIRST, recovery.tick(false, true, 1, true));
        assertEquals(SECOND, recovery.tick(false, true, 1, true));
    }

    @Test
    void worldOrPlayerResetDoesNotProduceAFalseRecovery() {
        ShieldRecovery recovery = new ShieldRecovery();
        recovery.reset(true);
        assertEquals(FIRST, recovery.tick(false, true, 3, true));
        recovery.reset(false);
        for (int i = 0; i < 5; i++) {
            assertEquals(NONE, recovery.tick(false, true, 3, true));
        }
    }

    @Test
    void zeroDelayOrBlankSecondSoundProducesASingleNote() {
        for (int delay : new int[]{0, -1, 3}) {
            ShieldRecovery recovery = new ShieldRecovery();
            recovery.reset(true);
            assertEquals(FIRST, recovery.tick(false, true, delay, false));
            for (int i = 0; i < 5; i++) {
                assertEquals(NONE, recovery.tick(false, true, delay, false));
            }
        }
        ShieldRecovery recovery = new ShieldRecovery();
        recovery.reset(true);
        assertEquals(FIRST, recovery.tick(false, true, 0, true));
        assertEquals(NONE, recovery.tick(false, true, 0, true));
    }

    @Test
    void enablingSoundAfterASilentRecoveryDoesNotReplayIt() {
        ShieldRecovery recovery = new ShieldRecovery();
        recovery.reset(true);
        assertEquals(NONE, recovery.tick(false, false, 3, true));
        assertEquals(NONE, recovery.tick(false, true, 3, true));
    }
}
