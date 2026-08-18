package io.github.streambridge.api;

import java.util.Objects;

public final class StreamEvent {
    public enum Type {
        ENGINE_STARTING,
        ENGINE_STARTED,
        ENGINE_STOPPING,
        ENGINE_STOPPED,
        ENGINE_FAILED,
        STREAM_AVAILABLE,
        STREAM_UNAVAILABLE
    }

    private final Type type;
    private final String engineId;
    private final StreamKey streamKey;
    private final String message;
    private final long occurredAtEpochMillis;

    public StreamEvent(Type type, String engineId, StreamKey streamKey, String message, long occurredAtEpochMillis) {
        this.type = Objects.requireNonNull(type, "type");
        this.engineId = Objects.requireNonNull(engineId, "engineId");
        this.streamKey = streamKey;
        this.message = message;
        this.occurredAtEpochMillis = occurredAtEpochMillis;
    }

    public static StreamEvent now(Type type, String engineId, StreamKey streamKey, String message) {
        return new StreamEvent(type, engineId, streamKey, message, System.currentTimeMillis());
    }

    public Type type() {
        return type;
    }

    public String engineId() {
        return engineId;
    }

    public StreamKey streamKey() {
        return streamKey;
    }

    public String message() {
        return message;
    }

    public long occurredAtEpochMillis() {
        return occurredAtEpochMillis;
    }
}

