package io.github.streambridge.api;

public interface PlayerOperations {
    MediaPlayer play(PlayerRequest request);

    default MediaPlayer play(PlayerRequest request, FrameListener listener) {
        if (listener != null) throw new UnsupportedOperationException("This engine does not expose frame delegates");
        return play(request);
    }
}
