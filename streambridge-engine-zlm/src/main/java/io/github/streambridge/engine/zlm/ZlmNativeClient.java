package io.github.streambridge.engine.zlm;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.EncodedFrame;
import io.github.streambridge.api.MediaInputRequest;
import io.github.streambridge.api.PlayerRequest;
import io.github.streambridge.api.FrameListener;
import io.github.streambridge.api.PushRequest;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RecordingRequest;
import io.github.streambridge.api.RtpServerRequest;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.WebRtcRequest;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface ZlmNativeClient {
    void initialize(EngineConfiguration configuration);

    int startHttp(int port, boolean tls);

    int startRtsp(int port, boolean tls);

    int startRtmp(int port, boolean tls);

    default List<ZlmStreamInfo> listStreams() { throw unsupported(); }

    default NativeHandle pull(PullRequest request, OperationListener listener) { throw unsupported(); }

    default NativeHandle push(PushRequest request, OperationListener listener) { throw unsupported(); }

    default boolean closeStream(StreamKey key) { throw unsupported(); }

    default boolean startRecording(RecordingRequest request) { throw unsupported(); }

    default boolean stopRecording(StreamKey key, RecordingFormat format) { throw unsupported(); }

    default boolean isRecording(StreamKey key, RecordingFormat format) { throw unsupported(); }

    default NativeRtpHandle openRtpServer(RtpServerRequest request, Runnable onDetach) { throw unsupported(); }

    default CompletionStage<String> answer(WebRtcRequest request) { throw unsupported(); }

    default NativeMediaHandle createPublisher(MediaInputRequest request) { throw unsupported(); }

    default NativePlayerHandle play(PlayerRequest request) { throw unsupported(); }

    default NativePlayerHandle play(PlayerRequest request, FrameListener listener) { return play(request); }

    default void setOption(String key, String value) { throw unsupported(); }

    default String getOption(String key) { throw unsupported(); }

    default void setStreamChangeListener(StreamChangeListener listener) { }

    void stopAll();

    static UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("Native client operation is not implemented");
    }

    interface NativeHandle extends AutoCloseable {
        boolean isOpen();
        void close();
    }

    interface NativeRtpHandle extends NativeHandle {
        int port();
        void updateSsrc(long ssrc);
    }

    interface NativeMediaHandle extends NativeHandle {
        boolean input(EncodedFrame frame);
        int readerCount();
    }

    interface NativePlayerHandle extends NativeHandle {
        void pause(boolean paused);
        void speed(float multiplier);
        void seek(float progress);
        void seekSeconds(int seconds);
        float durationSeconds();
        float progress();
        int progressSeconds();
        float lossRate(boolean video);
    }

    interface OperationListener {
        void onResult(boolean success, String message);
        void onClosed(String message);
    }

    interface StreamChangeListener {
        void onChanged(boolean registered, ZlmStreamInfo stream);
    }
}
