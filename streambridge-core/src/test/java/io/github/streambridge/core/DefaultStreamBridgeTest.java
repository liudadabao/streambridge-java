package io.github.streambridge.core;

import io.github.streambridge.api.EngineCapability;
import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.StreamBridge;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamDescriptor;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamEnginePlugin;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.PullRequest;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultStreamBridgeTest {
    @Test
    void rejectsDuplicatePluginIds() {
        StreamBridge bridge = StreamBridges.builder().discoverPlugins(false).plugin(new TestPlugin()).build();
        assertThrows(StreamBridgeException.class, () -> bridge.register(new TestPlugin()));
    }

    @Test
    void reportsAvailablePluginsWhenRequestedPluginIsMissing() {
        StreamBridge bridge = StreamBridges.builder().discoverPlugins(false).plugin(new TestPlugin()).build();
        StreamBridgeException failure = assertThrows(StreamBridgeException.class,
            () -> bridge.create("missing", EngineConfiguration.empty()));
        assertTrue(failure.getMessage().contains("test"));
    }

    @Test
    void closesOwnedEngines() {
        StreamBridge bridge = StreamBridges.builder().discoverPlugins(false).plugin(new TestPlugin()).build();
        StreamEngine engine = bridge.open("test", EngineConfiguration.empty());
        bridge.close();
        assertEquals(io.github.streambridge.api.EngineState.STOPPED, engine.state());
    }

    private static final class TestPlugin implements StreamEnginePlugin {
        @Override
        public String id() {
            return "test";
        }

        @Override
        public String description() {
            return "test";
        }

        @Override
        public StreamEngine create(EngineConfiguration configuration) {
            return new AbstractStreamEngine("test", configuration, Collections.singleton(EngineCapability.SERVER_LIFECYCLE)) {
                @Override
                protected void doStart() {
                }

                @Override
                protected void doStop() {
                }

                @Override
                public List<StreamDescriptor> listStreams() {
                    return Collections.emptyList();
                }

                @Override
                public StreamHandle pull(PullRequest request) {
                    throw new UnsupportedOperationException();
                }

                @Override
                public boolean closeStream(StreamKey key) {
                    return false;
                }
            };
        }
    }
}

