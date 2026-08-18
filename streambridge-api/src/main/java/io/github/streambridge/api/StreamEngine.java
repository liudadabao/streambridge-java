package io.github.streambridge.api;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface StreamEngine extends AutoCloseable {
    String id();

    EngineState state();

    Set<EngineCapability> capabilities();

    void start();

    void stop();

    List<StreamDescriptor> listStreams();

    StreamHandle pull(PullRequest request);

    boolean closeStream(StreamKey key);

    Subscription subscribe(StreamEventListener listener);

    /**
     * Returns an optional, narrowly scoped engine feature without coupling callers to an implementation.
     */
    default <T> Optional<T> extension(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return type.isInstance(this) ? Optional.of(type.cast(this)) : Optional.<T>empty();
    }

    @Override
    default void close() {
        stop();
    }
}
