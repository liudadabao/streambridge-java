package io.github.streambridge.api;

public interface StreamHandle extends AutoCloseable {
    StreamKey key();

    boolean isOpen();

    @Override
    void close();
}

