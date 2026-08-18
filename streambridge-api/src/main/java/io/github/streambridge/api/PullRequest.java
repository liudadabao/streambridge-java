package io.github.streambridge.api;

import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PullRequest {
    private final URI sourceUri;
    private final StreamKey target;
    private final Map<String, String> options;

    private PullRequest(Builder builder) {
        this.sourceUri = Objects.requireNonNull(builder.sourceUri, "sourceUri");
        this.target = Objects.requireNonNull(builder.target, "target");
        this.options = Collections.unmodifiableMap(new LinkedHashMap<String, String>(builder.options));
    }

    public static Builder builder() {
        return new Builder();
    }

    public URI sourceUri() {
        return sourceUri;
    }

    public StreamKey target() {
        return target;
    }

    public Map<String, String> options() {
        return options;
    }

    public static final class Builder {
        private URI sourceUri;
        private StreamKey target;
        private final Map<String, String> options = new LinkedHashMap<String, String>();

        public Builder sourceUri(String sourceUri) {
            this.sourceUri = URI.create(Objects.requireNonNull(sourceUri, "sourceUri"));
            return this;
        }

        public Builder sourceUri(URI sourceUri) {
            this.sourceUri = Objects.requireNonNull(sourceUri, "sourceUri");
            return this;
        }

        public Builder target(StreamKey target) {
            this.target = Objects.requireNonNull(target, "target");
            return this;
        }

        public Builder option(String key, Object value) {
            options.put(Objects.requireNonNull(key, "key"), String.valueOf(Objects.requireNonNull(value, "value")));
            return this;
        }

        public PullRequest build() {
            return new PullRequest(this);
        }
    }
}

