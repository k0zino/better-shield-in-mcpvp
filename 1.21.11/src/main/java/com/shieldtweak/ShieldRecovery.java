package com.shieldtweak;

/** 只按世界内的 tick 推进提示音，禁用功能或再次破盾会取消待播的第二声。 */
final class ShieldRecovery {
    enum Note { NONE, FIRST, SECOND }

    private boolean wasOnCooldown;
    private int secondNoteIn = -1;

    void reset(final boolean onCooldown) {
        wasOnCooldown = onCooldown;
        secondNoteIn = -1;
    }

    Note tick(final boolean onCooldown, final boolean enabled, final int delayTicks, final boolean hasSecondNote) {
        boolean recovered = wasOnCooldown && !onCooldown;
        wasOnCooldown = onCooldown;
        if (!enabled || onCooldown) {
            secondNoteIn = -1;
            return Note.NONE;
        }
        if (recovered) {
            secondNoteIn = delayTicks > 0 && hasSecondNote ? delayTicks : -1;
            return Note.FIRST;
        }
        if (secondNoteIn > 0 && --secondNoteIn == 0) {
            return Note.SECOND;
        }
        return Note.NONE;
    }
}
