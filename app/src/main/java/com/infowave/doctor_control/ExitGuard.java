package com.infowave.doctor_control;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * One-shot suppression for exit prompts fired by onUserLeaveHint().
 * Call suppressNextPrompt() immediately BEFORE startActivity(...).
 * MainActivity will consult consumeIfSuppressed() and skip the popup once.
 */
public final class ExitGuard {
    private static final AtomicBoolean SUPPRESS_NEXT = new AtomicBoolean(false);

    private ExitGuard() {}

    /** Call this right BEFORE any startActivity(...) that navigates within your app. */
    public static void suppressNextPrompt() {
        SUPPRESS_NEXT.set(true);
    }

    /** Called by MainActivity.onUserLeaveHint(); returns true exactly once if suppressed. */
    public static boolean consumeIfSuppressed() {
        return SUPPRESS_NEXT.getAndSet(false);
    }
}
