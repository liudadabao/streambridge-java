package io.github.streambridge.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class StreamDescriptor {
    private final StreamKey key;
    private final String sourceUri;
    private final int readerCount;
    private final Map<String, String> attributes;

    public StreamDescriptor(StreamKey key, String sourceUri, int readerCount, Map<String, String> attributes) {
        this.key = Objects.requireNonNull(key, "key");
        this.sourceUri = sourceUri;
        this.readerCount = readerCount;
        Map<String, String> source = attributes == null ? Collections.<String, String>emptyMap() : attributes;
        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<String, String>(source));
    }

    public StreamKey key() {
        return key;
    }

    public String sourceUri() {
        return sourceUri;
    }

    public int readerCount() {
        return readerCount;
    }

    public Map<String, String> attributes() {
        return attributes;
    }
}

