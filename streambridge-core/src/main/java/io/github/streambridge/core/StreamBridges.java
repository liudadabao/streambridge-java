package io.github.streambridge.core;

public final class StreamBridges {
    private StreamBridges() {
    }

    public static StreamBridgeBuilder builder() {
        return new StreamBridgeBuilder();
    }
}

