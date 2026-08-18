package io.github.streambridge.api;

public class StreamBridgeException extends RuntimeException {
    public StreamBridgeException(String message) {
        super(message);
    }

    public StreamBridgeException(String message, Throwable cause) {
        super(message, cause);
    }
}

