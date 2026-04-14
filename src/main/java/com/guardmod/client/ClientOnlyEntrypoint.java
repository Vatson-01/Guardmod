package com.guardmod.client;

public final class ClientOnlyEntrypoint {
    private static boolean initialized = false;

    private ClientOnlyEntrypoint() {
    }

    public static void init() {
        if (initialized) {
            return;
        }

        initialized = true;
    }
}