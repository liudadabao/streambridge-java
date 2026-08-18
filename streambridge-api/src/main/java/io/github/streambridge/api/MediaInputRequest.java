package io.github.streambridge.api;

import java.util.Objects;

public final class MediaInputRequest {
    private final StreamKey target;
    private final MediaCodec videoCodec;
    private final int width;
    private final int height;
    private final float framesPerSecond;
    private final int bitRate;
    private final MediaCodec audioCodec;
    private final int sampleRate;
    private final int channels;
    private final int sampleBits;
    private final boolean hls;
    private final boolean mp4;

    private MediaInputRequest(Builder b) {
        target = Objects.requireNonNull(b.target, "target");
        videoCodec = b.videoCodec;
        width = b.width; height = b.height; framesPerSecond = b.framesPerSecond; bitRate = b.bitRate;
        audioCodec = b.audioCodec; sampleRate = b.sampleRate; channels = b.channels; sampleBits = b.sampleBits;
        hls = b.hls; mp4 = b.mp4;
        if (videoCodec == null && audioCodec == null) throw new IllegalArgumentException("at least one track is required");
        if (videoCodec != null && !videoCodec.video()) throw new IllegalArgumentException("videoCodec must be a video codec");
        if (audioCodec != null && audioCodec.video()) throw new IllegalArgumentException("audioCodec must be an audio codec");
    }

    public static Builder builder() { return new Builder(); }
    public StreamKey target() { return target; }
    public MediaCodec videoCodec() { return videoCodec; }
    public int width() { return width; }
    public int height() { return height; }
    public float framesPerSecond() { return framesPerSecond; }
    public int bitRate() { return bitRate; }
    public MediaCodec audioCodec() { return audioCodec; }
    public int sampleRate() { return sampleRate; }
    public int channels() { return channels; }
    public int sampleBits() { return sampleBits; }
    public boolean hls() { return hls; }
    public boolean mp4() { return mp4; }

    public static final class Builder {
        private StreamKey target;
        private MediaCodec videoCodec;
        private int width;
        private int height;
        private float framesPerSecond;
        private int bitRate;
        private MediaCodec audioCodec;
        private int sampleRate = 8000;
        private int channels = 1;
        private int sampleBits = 16;
        private boolean hls;
        private boolean mp4;
        public Builder target(StreamKey value) { target = value; return this; }
        public Builder video(MediaCodec codec, int width, int height, float fps, int bitRate) {
            videoCodec = codec; this.width = width; this.height = height; framesPerSecond = fps; this.bitRate = bitRate; return this;
        }
        public Builder audio(MediaCodec codec, int sampleRate, int channels, int sampleBits) {
            audioCodec = codec; this.sampleRate = sampleRate; this.channels = channels; this.sampleBits = sampleBits; return this;
        }
        public Builder hls(boolean value) { hls = value; return this; }
        public Builder mp4(boolean value) { mp4 = value; return this; }
        public MediaInputRequest build() { return new MediaInputRequest(this); }
    }
}
