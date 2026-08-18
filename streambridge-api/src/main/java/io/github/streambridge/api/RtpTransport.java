package io.github.streambridge.api;

public enum RtpTransport {
    UDP(0),
    TCP_ACTIVE(1),
    TCP_PASSIVE(2);

    private final int nativeCode;
    RtpTransport(int nativeCode) { this.nativeCode = nativeCode; }
    public int nativeCode() { return nativeCode; }
}
