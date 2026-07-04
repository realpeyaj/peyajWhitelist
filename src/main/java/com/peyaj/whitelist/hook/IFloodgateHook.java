package com.peyaj.whitelist.hook;

import java.util.UUID;

public interface IFloodgateHook {
    boolean isBedrockPlayer(UUID uuid);
    String getXuid(UUID uuid);
    String getRawUsername(UUID uuid);
    String getJavaUsername(UUID uuid);
    String getCorrectUsername(UUID uuid);
}
