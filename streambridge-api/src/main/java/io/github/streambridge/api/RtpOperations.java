package io.github.streambridge.api;

public interface RtpOperations {
    RtpServerHandle openRtpServer(RtpServerRequest request);
}
