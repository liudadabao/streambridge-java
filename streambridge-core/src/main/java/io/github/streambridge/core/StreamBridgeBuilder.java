package io.github.streambridge.core;

import io.github.streambridge.api.StreamBridge;
import io.github.streambridge.api.StreamEnginePlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public final class StreamBridgeBuilder {
    private final List<StreamEnginePlugin> plugins = new ArrayList<StreamEnginePlugin>();
    private boolean discoverPlugins = true;
    private ClassLoader classLoader;

    StreamBridgeBuilder() {
    }

    public StreamBridgeBuilder discoverPlugins(boolean discoverPlugins) {
        this.discoverPlugins = discoverPlugins;
        return this;
    }

    public StreamBridgeBuilder classLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    public StreamBridgeBuilder plugin(StreamEnginePlugin plugin) {
        this.plugins.add(plugin);
        return this;
    }

    public StreamBridge build() {
        DefaultStreamBridge bridge = new DefaultStreamBridge();
        if (discoverPlugins) {
            ClassLoader loader = classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
            ServiceLoader<StreamEnginePlugin> discovered = ServiceLoader.load(StreamEnginePlugin.class, loader);
            for (StreamEnginePlugin plugin : discovered) {
                bridge.register(plugin);
            }
        }
        for (StreamEnginePlugin plugin : plugins) {
            bridge.register(plugin);
        }
        return bridge;
    }
}

