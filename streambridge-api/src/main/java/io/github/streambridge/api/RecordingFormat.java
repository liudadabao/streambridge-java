package io.github.streambridge.api;

public enum RecordingFormat {
    HLS(0),
    MP4(1);

    private final int nativeCode;

    RecordingFormat(int nativeCode) {
        this.nativeCode = nativeCode;
    }

    public int nativeCode() {
        return nativeCode;
    }
}
