package com.peyaj.whitelist.hook;

import java.util.UUID;

public class DummyFloodgateHook implements IFloodgateHook {
    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        return false;
    }

    @Override
    public String getXuid(UUID uuid) {
        return null;
    }

    @Override
    public String getRawUsername(UUID uuid) {
        return null;
    }

    @Override
    public String getJavaUsername(UUID uuid) {
        return null;
    }

    @Override
    public String getCorrectUsername(UUID uuid) {
        return null;
    }
}
