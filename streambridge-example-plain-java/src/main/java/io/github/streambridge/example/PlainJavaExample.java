package io.github.streambridge.example;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.StreamBridge;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.core.StreamBridges;

public final class PlainJavaExample {
    private PlainJavaExample() {
    }

    public static void main(String[] args) {
        try (StreamBridge bridge = StreamBridges.builder().build();
             StreamEngine engine = bridge.open("mock", EngineConfiguration.empty())) {
            StreamHandle handle = engine.pull(PullRequest.builder()
                .sourceUri("rtsp://example.test/live/camera-1")
                .target(StreamKey.of("__defaultVhost__", "live", "camera-1"))
                .build());
            System.out.println("Stream is available: " + handle.key());
            handle.close();
        }
    }
}

