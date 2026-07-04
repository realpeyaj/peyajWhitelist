package com.peyaj.whitelist.hook;

import java.util.UUID;

public interface ILuckPermsHook {
    /**
     * Checks if a player has permission to bypass the whitelist check.
     */
    boolean hasBypassPermission(UUID uuid);
}
