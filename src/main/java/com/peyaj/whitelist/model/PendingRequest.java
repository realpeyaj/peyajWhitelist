package com.peyaj.whitelist.model;

import java.util.UUID;

public class PendingRequest {
    private final String name;
    private final UUID uuid;
    private final String xuid;
    private final String platform;
    private final long timestamp;

    public PendingRequest(String name, UUID uuid, String xuid, String platform) {
        this.name = name;
        this.uuid = uuid;
        this.xuid = xuid;
        this.platform = platform;
        this.timestamp = System.currentTimeMillis();
    }

    public String getName() {
        return name;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getXuid() {
        return xuid;
    }

    public String getPlatform() {
        return platform;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
