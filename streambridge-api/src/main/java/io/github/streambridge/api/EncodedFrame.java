package io.github.streambridge.api;

import java.util.Arrays;
import java.util.Objects;

public final class EncodedFrame {
    private final MediaCodec codec;
    private final byte[] data;
    private final long decodingTimestamp;
    private final long presentationTimestamp;

    public EncodedFrame(MediaCodec codec, byte[] data, long decodingTimestamp, long presentationTimestamp) {
        this.codec = Objects.requireNonNull(codec, "codec");
        this.data = Arrays.copyOf(Objects.requireNonNull(data, "data"), data.length);
        this.decodingTimestamp = decodingTimestamp;
        this.presentationTimestamp = presentationTimestamp;
    }

    public MediaCodec codec() { return codec; }
    public byte[] data() { return Arrays.copyOf(data, data.length); }
    public long decodingTimestamp() { return decodingTimestamp; }
    public long presentationTimestamp() { return presentationTimestamp; }
}
