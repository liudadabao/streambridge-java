package io.github.streambridge.core;

import io.github.streambridge.api.EngineCapability;
import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.EngineState;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamEvent;
import io.github.streambridge.api.StreamEventListener;
import io.github.streambridge.api.Subscription;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

public abstract class AbstractStreamEngine implements StreamEngine {
    private final String id;
    private final EngineConfiguration configuration;
    private final Set<EngineCapability> capabilities;
    private final AtomicReference<EngineState> state = new AtomicReference<EngineState>(EngineState.NEW);
    private final CopyOnWriteArrayList<StreamEventListener> listeners = new CopyOnWriteArrayList<StreamEventListener>();

    protected AbstractStreamEngine(String id, EngineConfiguration configuration, Set<EngineCapability> capabilities) {
        this.id = requireId(id);
        this.configuration = configuration == null ? EngineConfiguration.empty() : configuration;
        EnumSet<EngineCapability> copy = capabilities == null || capabilities.isEmpty()
            ? EnumSet.noneOf(EngineCapability.class)
            : EnumSet.copyOf(capabilities);
        this.capabilities = Collections.unmodifiableSet(copy);
    }

    @Override
    public final String id() {
        return id;
    }

    @Override
    public final EngineState state() {
        return state.get();
    }

    @Override
    public final Set<EngineCapability> capabilities() {
        return capabilities;
    }

    protected final EngineConfiguration configuration() {
        return configuration;
    }

    @Override
    public final synchronized void start() {
        EngineState current = state.get();
        if (current == EngineState.RUNNING) {
            return;
        }
        if (current == EngineState.STARTING || current == EngineState.STOPPING) {
            throw new StreamBridgeException("Engine '" + id + "' cannot start while state is " + current);
        }

        state.set(EngineState.STARTING);
        publish(StreamEvent.Type.ENGINE_STARTING, null, "Engine is starting");
        try {
            doStart();
            state.set(EngineState.RUNNING);
            publish(StreamEvent.Type.ENGINE_STARTED, null, "Engine started");
        } catch (Throwable failure) {
            state.set(EngineState.FAILED);
            publish(StreamEvent.Type.ENGINE_FAILED, null, failure.getMessage());
            try {
                doStop();
            } catch (Throwable cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            if (failure instanceof StreamBridgeException) {
                throw (StreamBridgeException) failure;
            }
            throw new StreamBridgeException("Engine '" + id + "' failed to start", failure);
        }
    }

    @Override
    public final synchronized void stop() {
        EngineState current = state.get();
        if (current == EngineState.STOPPED) {
            return;
        }
        if (current == EngineState.NEW) {
            state.set(EngineState.STOPPED);
            publish(StreamEvent.Type.ENGINE_STOPPED, null, "Engine stopped before start");
            return;
        }
        if (current == EngineState.STOPPING) {
            return;
        }

        state.set(EngineState.STOPPING);
        publish(StreamEvent.Type.ENGINE_STOPPING, null, "Engine is stopping");
        try {
            doStop();
            state.set(EngineState.STOPPED);
            publish(StreamEvent.Type.ENGINE_STOPPED, null, "Engine stopped");
        } catch (Throwable failure) {
            state.set(EngineState.FAILED);
            publish(StreamEvent.Type.ENGINE_FAILED, null, failure.getMessage());
            throw new StreamBridgeException("Engine '" + id + "' failed to stop", failure);
        }
    }

    @Override
    public final Subscription subscribe(final StreamEventListener listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener must not be null");
        }
        listeners.add(listener);
        return new Subscription() {
            private boolean closed;

            @Override
            public synchronized void close() {
                if (!closed) {
                    listeners.remove(listener);
                    closed = true;
                }
            }
        };
    }

    protected final void requireRunning() {
        if (state.get() != EngineState.RUNNING) {
            throw new StreamBridgeException("Engine '" + id + "' must be RUNNING, but was " + state.get());
        }
    }

    protected final void publish(StreamEvent.Type type, io.github.streambridge.api.StreamKey key, String message) {
        StreamEvent event = StreamEvent.now(type, id, key, message);
        for (StreamEventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (RuntimeException ignored) {
                // A consumer must not break the engine lifecycle or other consumers.
            }
        }
    }

    protected abstract void doStart();

    protected abstract void doStop();

    private static String requireId(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("engine id must not be blank");
        }
        return value.trim();
    }
}

