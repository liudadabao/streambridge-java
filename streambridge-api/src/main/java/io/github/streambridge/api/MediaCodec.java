package io.github.streambridge.api;

public enum MediaCodec {
    H264(0, true), H265(1, true), AAC(2, false), G711A(3, false), G711U(4, false),
    OPUS(5, false), L16(6, false), VP8(7, true), VP9(8, true), AV1(9, true), JPEG(10, true);

    private final int nativeCode;
    private final boolean video;
    MediaCodec(int nativeCode, boolean video) { this.nativeCode = nativeCode; this.video = video; }
    public int nativeCode() { return nativeCode; }
    public boolean video() { return video; }

    public static MediaCodec fromNativeCode(int code) {
        for (MediaCodec codec : values()) if (codec.nativeCode == code) return codec;
        throw new IllegalArgumentException("Unknown media codec code: " + code);
    }
}
