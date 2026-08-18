package io.github.streambridge.api;

import java.util.Objects;

public final class StreamKey {
    private final String virtualHost;
    private final String application;
    private final String stream;

    private StreamKey(String virtualHost, String application, String stream) {
        this.virtualHost = requireText(virtualHost, "virtualHost");
        this.application = requireText(application, "application");
        this.stream = requireText(stream, "stream");
    }

    public static StreamKey of(String virtualHost, String application, String stream) {
        return new StreamKey(virtualHost, application, stream);
    }

    public String virtualHost() {
        return virtualHost;
    }

    public String application() {
        return application;
    }

    public String stream() {
        return stream;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StreamKey)) {
            return false;
        }
        StreamKey that = (StreamKey) other;
        return virtualHost.equals(that.virtualHost)
            && application.equals(that.application)
            && stream.equals(that.stream);
    }

    @Override
    public int hashCode() {
        return Objects.hash(virtualHost, application, stream);
    }

    @Override
    public String toString() {
        return virtualHost + "/" + application + "/" + stream;
    }

    private static String requireText(String value, String name) {
        String validated = Objects.requireNonNull(value, name).trim();
        if (validated.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return validated;
    }
}

