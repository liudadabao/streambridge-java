package io.github.streambridge.api;

import java.util.Objects;

public final class RtpServerRequest {
    private final StreamKey target;
    private final int port;
    private final RtpTransport transport;
    private final boolean multiplexed;

    public RtpServerRequest(StreamKey target, int port, RtpTransport transport, boolean multiplexed) {
        this.target = Objects.requireNonNull(target, "target");
        if (port < 0 || port > 65535) throw new IllegalArgumentException("port must be between 0 and 65535");
        this.port = port;
        this.transport = Objects.requireNonNull(transport, "transport");
        this.multiplexed = multiplexed;
    }

    public StreamKey target() { return target; }
    public int port() { return port; }
    public RtpTransport transport() { return transport; }
    public boolean multiplexed() { return multiplexed; }
}
