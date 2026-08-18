package io.github.streambridge.api;

@FunctionalInterface
public interface FrameListener {
    void onFrame(EncodedFrame frame);
}
