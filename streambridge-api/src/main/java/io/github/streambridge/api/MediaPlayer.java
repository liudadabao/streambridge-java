package io.github.streambridge.api;

public interface MediaPlayer extends AutoCloseable {
    boolean isOpen();
    void pause(boolean paused);
    void speed(float multiplier);
    void seek(float progress);
    void seekSeconds(int seconds);
    float durationSeconds();
    float progress();
    int progressSeconds();
    float lossRate(boolean video);
    void close();
}
