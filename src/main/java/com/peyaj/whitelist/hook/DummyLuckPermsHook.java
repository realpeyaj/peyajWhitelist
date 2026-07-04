package com.peyaj.whitelist.hook;

import java.util.UUID;

public class DummyLuckPermsHook implements ILuckPermsHook {
    @Override
    public boolean hasBypassPermission(UUID uuid) {
        return false;
    }
}
