package io.github.streambridge.spring.boot;

import io.github.streambridge.api.EngineState;
import io.github.streambridge.api.StreamEngine;
import org.springframework.context.SmartLifecycle;

final class StreamEngineLifecycle implements SmartLifecycle {
    private final StreamEngine engine;
    private final boolean autoStartup;

    StreamEngineLifecycle(StreamEngine engine, boolean autoStartup) {
        this.engine = engine;
        this.autoStartup = autoStartup;
    }

    @Override
    public void start() {
        engine.start();
    }

    @Override
    public void stop() {
        engine.stop();
    }

    @Override
    public boolean isRunning() {
        return engine.state() == EngineState.RUNNING;
    }

    @Override
    public boolean isAutoStartup() {
        return autoStartup;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE - 100;
    }
}

