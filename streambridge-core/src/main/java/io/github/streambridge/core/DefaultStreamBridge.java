package io.github.streambridge.core;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.StreamBridge;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamEnginePlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class DefaultStreamBridge implements StreamBridge {
    private final Map<String, StreamEnginePlugin> plugins = new LinkedHashMap<String, StreamEnginePlugin>();
    private final List<StreamEngine> ownedEngines = new ArrayList<StreamEngine>();
    private boolean closed;

    @Override
    public synchronized Set<String> availablePlugins() {
        return Collections.unmodifiableSet(new LinkedHashSet<String>(plugins.keySet()));
    }

    @Override
    public synchronized void register(StreamEnginePlugin plugin) {
        ensureOpen();
        if (plugin == null) {
            throw new IllegalArgumentException("plugin must not be null");
        }
        String id = plugin.id() == null ? "" : plugin.id().trim();
        if (id.isEmpty()) {
            throw new IllegalArgumentException("plugin id must not be blank");
        }
        if (plugins.containsKey(id)) {
            throw new StreamBridgeException("A stream engine plugin with id '" + id + "' is already registered");
        }
        plugins.put(id, plugin);
    }

    @Override
    public synchronized StreamEngine create(String pluginId, EngineConfiguration configuration) {
        ensureOpen();
        String id = pluginId == null ? "" : pluginId.trim();
        StreamEnginePlugin plugin = plugins.get(id);
        if (plugin == null) {
            throw new StreamBridgeException("No stream engine plugin named '" + id + "'. Available plugins: " + plugins.keySet());
        }
        StreamEngine engine = plugin.create(configuration == null ? EngineConfiguration.empty() : configuration);
        if (engine == null) {
            throw new StreamBridgeException("Plugin '" + id + "' returned a null engine");
        }
        ownedEngines.add(engine);
        return engine;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        StreamBridgeException aggregate = null;
        for (int index = ownedEngines.size() - 1; index >= 0; index--) {
            try {
                ownedEngines.get(index).close();
            } catch (RuntimeException failure) {
                if (aggregate == null) {
                    aggregate = new StreamBridgeException("One or more stream engines failed to close");
                }
                aggregate.addSuppressed(failure);
            }
        }
        ownedEngines.clear();
        closed = true;
        if (aggregate != null) {
            throw aggregate;
        }
    }

    private void ensureOpen() {
        if (closed) {
            throw new StreamBridgeException("StreamBridge is already closed");
        }
    }
}

