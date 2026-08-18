package io.github.streambridge.api;

@FunctionalInterface
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}

