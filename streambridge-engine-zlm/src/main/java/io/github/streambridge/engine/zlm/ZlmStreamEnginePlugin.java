package io.github.streambridge.engine.zlm;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamEnginePlugin;

import java.util.Objects;

public final class ZlmStreamEnginePlugin implements StreamEnginePlugin {
    public static final String ID = "zlm-embedded";

    private final ZlmNativeClientFactory nativeClientFactory;

    public ZlmStreamEnginePlugin() {
        this(new ZlmNativeClientFactory() {
            @Override
            public ZlmNativeClient create(EngineConfiguration configuration) {
                return new JnaZlmNativeClient(configuration);
            }
        });
    }

    public ZlmStreamEnginePlugin(ZlmNativeClientFactory nativeClientFactory) {
        this.nativeClientFactory = Objects.requireNonNull(nativeClientFactory, "nativeClientFactory");
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public String description() {
        return "Embedded ZLMediaKit engine using the official public C ABI";
    }

    @Override
    public StreamEngine create(EngineConfiguration configuration) {
        EngineConfiguration effective = configuration == null ? EngineConfiguration.empty() : configuration;
        return new ZlmStreamEngine(effective, nativeClientFactory.create(effective));
    }
}

