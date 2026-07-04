package com.peyaj.whitelist.hook;

import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;
import java.util.UUID;

public class ActiveFloodgateHook implements IFloodgateHook {
    @Override
    public boolean isBedrockPlayer(UUID uuid) {
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(uuid);
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public String getXuid(UUID uuid) {
        try {
            FloodgatePlayer player = FloodgateApi.getInstance().getPlayer(uuid);
            return player != null ? player.getXuid() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getRawUsername(UUID uuid) {
        try {
            FloodgatePlayer player = FloodgateApi.getInstance().getPlayer(uuid);
            return player != null ? player.getUsername() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getJavaUsername(UUID uuid) {
        try {
            FloodgatePlayer player = FloodgateApi.getInstance().getPlayer(uuid);
            return player != null ? player.getJavaUsername() : null;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public String getCorrectUsername(UUID uuid) {
        try {
            FloodgatePlayer player = FloodgateApi.getInstance().getPlayer(uuid);
            return player != null ? player.getCorrectUsername() : null;
        } catch (Throwable t) {
            return null;
        }
    }
}
