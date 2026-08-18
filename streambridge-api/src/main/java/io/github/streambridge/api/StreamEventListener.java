package io.github.streambridge.api;

@FunctionalInterface
public interface StreamEventListener {
    void onEvent(StreamEvent event);
}

