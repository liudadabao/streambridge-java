package io.github.streambridge.engine.mock;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamEnginePlugin;

public final class MockStreamEnginePlugin implements StreamEnginePlugin {
    public static final String ID = "mock";

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "In-memory engine for tests and local development";
    }

    @Override
    public StreamEngine create(EngineConfiguration configuration) {
        return new MockStreamEngine(configuration);
    }
}

