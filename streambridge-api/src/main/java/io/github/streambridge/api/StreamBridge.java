package io.github.streambridge.api;

import java.util.Set;

public interface StreamBridge extends AutoCloseable {
    Set<String> availablePlugins();

    void register(StreamEnginePlugin plugin);

    StreamEngine create(String pluginId, EngineConfiguration configuration);

    default StreamEngine open(String pluginId, EngineConfiguration configuration) {
        StreamEngine engine = create(pluginId, configuration);
        engine.start();
        return engine;
    }

    @Override
    void close();
}

