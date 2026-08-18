package io.github.streambridge.api;

public interface StreamEnginePlugin {
    String id();

    String description();

    StreamEngine create(EngineConfiguration configuration);
}

