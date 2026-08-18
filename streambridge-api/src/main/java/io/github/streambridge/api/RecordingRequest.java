package io.github.streambridge.api;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

public final class RecordingRequest {
    private final StreamKey stream;
    private final RecordingFormat format;
    private final Path outputDirectory;
    private final Duration segmentDuration;

    private RecordingRequest(Builder builder) {
        this.stream = Objects.requireNonNull(builder.stream, "stream");
        this.format = Objects.requireNonNull(builder.format, "format");
        this.outputDirectory = builder.outputDirectory;
        this.segmentDuration = builder.segmentDuration;
        if (segmentDuration != null && (segmentDuration.isNegative() || segmentDuration.isZero())) {
            throw new IllegalArgumentException("segmentDuration must be positive");
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public StreamKey stream() { return stream; }
    public RecordingFormat format() { return format; }
    public Path outputDirectory() { return outputDirectory; }
    public Duration segmentDuration() { return segmentDuration; }

    public static final class Builder {
        private StreamKey stream;
        private RecordingFormat format = RecordingFormat.MP4;
        private Path outputDirectory;
        private Duration segmentDuration;

        public Builder stream(StreamKey value) { this.stream = value; return this; }
        public Builder format(RecordingFormat value) { this.format = value; return this; }
        public Builder outputDirectory(Path value) { this.outputDirectory = value; return this; }
        public Builder segmentDuration(Duration value) { this.segmentDuration = value; return this; }
        public RecordingRequest build() { return new RecordingRequest(this); }
    }
}
