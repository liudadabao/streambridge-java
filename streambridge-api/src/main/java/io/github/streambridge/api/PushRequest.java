package io.github.streambridge.api;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PushRequest {
    private final StreamKey source;
    private final String sourceSchema;
    private final URI targetUri;
    private final Map<String, String> options;

    private PushRequest(Builder builder) {
        this.source = Objects.requireNonNull(builder.source, "source");
        this.sourceSchema = requireText(builder.sourceSchema, "sourceSchema");
        this.targetUri = Objects.requireNonNull(builder.targetUri, "targetUri");
        this.options = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.options));
    }

    public static Builder builder() { return new Builder(); }
    public StreamKey source() { return source; }
    public String sourceSchema() { return sourceSchema; }
    public URI targetUri() { return targetUri; }
    public Map<String, String> options() { return options; }

    private static String requireText(String value, String name) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(name + " must not be blank");
        return value.trim();
    }

    public static final class Builder {
        private StreamKey source;
        private String sourceSchema = "rtmp";
        private URI targetUri;
        private final Map<String, String> options = new LinkedHashMap<String, String>();
        public Builder source(StreamKey value) { source = value; return this; }
        public Builder sourceSchema(String value) { sourceSchema = value; return this; }
        public Builder targetUri(String value) { targetUri = URI.create(Objects.requireNonNull(value, "targetUri")); return this; }
        public Builder targetUri(URI value) { targetUri = value; return this; }
        public Builder option(String key, Object value) { options.put(Objects.requireNonNull(key, "key"), String.valueOf(Objects.requireNonNull(value, "value"))); return this; }
        public PushRequest build() { return new PushRequest(this); }
    }
}
