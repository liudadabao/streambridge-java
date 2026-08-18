package io.github.streambridge.api;

public interface RtpServerHandle extends StreamHandle {
    int port();

    void updateSsrc(long ssrc);
}
