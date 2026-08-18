package io.github.streambridge.core;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.EngineState;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamDescriptor;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractStreamEngineTest {
    @Test
    void startFailureIsVisibleAndCleanupRuns() {
        FailingEngine engine = new FailingEngine();
        assertThrows(StreamBridgeException.class, engine::start);
        assertEquals(EngineState.FAILED, engine.state());
        assertEquals(1, engine.cleanupCalls);
    }

    private static final class FailingEngine extends AbstractStreamEngine {
        private int cleanupCalls;

        private FailingEngine() {
            super("failing", EngineConfiguration.empty(), Collections.emptySet());
        }

        @Override
        protected void doStart() {
            throw new IllegalStateException("boom");
        }

        @Override
        protected void doStop() {
            cleanupCalls++;
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
    }
}

