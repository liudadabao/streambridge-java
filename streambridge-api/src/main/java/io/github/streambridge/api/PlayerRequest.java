package io.github.streambridge.api;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PlayerRequest {
    private final URI sourceUri;
    private final Map<String, String> options;

    private PlayerRequest(Builder builder) {
        sourceUri = Objects.requireNonNull(builder.sourceUri, "sourceUri");
        options = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.options));
    }
    public static Builder builder() { return new Builder(); }
    public URI sourceUri() { return sourceUri; }
    public Map<String, String> options() { return options; }
    public static final class Builder {
        private URI sourceUri;
        private final Map<String, String> options = new LinkedHashMap<String, String>();
        public Builder sourceUri(String value) { sourceUri = URI.create(Objects.requireNonNull(value, "sourceUri")); return this; }
        public Builder sourceUri(URI value) { sourceUri = value; return this; }
        public Builder option(String key, Object value) { options.put(Objects.requireNonNull(key, "key"), String.valueOf(Objects.requireNonNull(value, "value"))); return this; }
        public PlayerRequest build() { return new PlayerRequest(this); }
    }
}
