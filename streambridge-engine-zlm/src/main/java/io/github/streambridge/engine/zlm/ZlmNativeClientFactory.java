package io.github.streambridge.engine.zlm;

import io.github.streambridge.api.EngineConfiguration;

@FunctionalInterface
public interface ZlmNativeClientFactory {
    ZlmNativeClient create(EngineConfiguration configuration);
}

