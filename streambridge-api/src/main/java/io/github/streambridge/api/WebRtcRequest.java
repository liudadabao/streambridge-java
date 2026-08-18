package io.github.streambridge.api;

import java.net.URI;
import java.util.Objects;

public final class WebRtcRequest {
    public enum Type { PLAY, PUSH, ECHO }

    private final Type type;
    private final String offerSdp;
    private final URI streamUri;

    public WebRtcRequest(Type type, String offerSdp, URI streamUri) {
        this.type = Objects.requireNonNull(type, "type");
        this.offerSdp = requireText(offerSdp, "offerSdp");
        this.streamUri = Objects.requireNonNull(streamUri, "streamUri");
    }

    public Type type() { return type; }
    public String offerSdp() { return offerSdp; }
    public URI streamUri() { return streamUri; }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value;
    }
}
