package io.github.streambridge.engine.zlm;

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.EncodedFrame;
import io.github.streambridge.api.FrameListener;
import io.github.streambridge.api.MediaInputRequest;
import io.github.streambridge.api.MediaCodec;
import io.github.streambridge.api.PlayerRequest;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.PushRequest;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RecordingRequest;
import io.github.streambridge.api.RtpServerRequest;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.WebRtcRequest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class JnaZlmNativeClient implements ZlmNativeClient {
    private final String library;
    private final Set<ManagedHandle> handles = Collections.newSetFromMap(new ConcurrentHashMap<ManagedHandle, Boolean>());
    private final Set<ZlmNativeApi.WebRtcCallback> pendingWebRtcCallbacks =
        Collections.newSetFromMap(new ConcurrentHashMap<ZlmNativeApi.WebRtcCallback, Boolean>());
    private ZlmNativeApi api;
    private volatile StreamChangeListener streamChangeListener;
    private ZlmNativeApi.Events events;

    JnaZlmNativeClient(EngineConfiguration configuration) {
        this.library = configuration.get(ZlmOptions.LIBRARY, "mk_api");
    }

    @Override
    public void initialize(EngineConfiguration configuration) {
        try {
            api = Native.load(library, ZlmNativeApi.class);
            api.mk_env_init2(
                configuration.getInt(ZlmOptions.THREADS, 0),
                configuration.getInt(ZlmOptions.LOG_LEVEL, 1),
                configuration.getInt(ZlmOptions.LOG_MASK, 1),
                nullable(configuration.get(ZlmOptions.LOG_PATH)),
                configuration.getInt(ZlmOptions.LOG_DAYS, 0),
                configuration.getBoolean(ZlmOptions.INI_IS_PATH, true) ? 1 : 0,
                nullable(configuration.get(ZlmOptions.INI)),
                configuration.getBoolean(ZlmOptions.SSL_IS_PATH, true) ? 1 : 0,
                nullable(configuration.get(ZlmOptions.SSL)),
                nullable(configuration.get(ZlmOptions.SSL_PASSWORD))
            );
            installEventListener();
        } catch (UnsatisfiedLinkError failure) {
            throw new StreamBridgeException(
                "Unable to load ZLMediaKit native library '" + library + "' on "
                    + System.getProperty("os.name") + "/" + System.getProperty("os.arch")
                    + ". Set '" + ZlmOptions.LIBRARY + "' to a trusted absolute path or configure java.library.path.",
                failure
            );
        }
    }

    @Override
    public int startHttp(int port, boolean tls) {
        ensureLoaded();
        return unsigned(api.mk_http_server_start(toUnsignedShort(port), tls ? 1 : 0));
    }

    @Override
    public int startRtsp(int port, boolean tls) {
        ensureLoaded();
        return unsigned(api.mk_rtsp_server_start(toUnsignedShort(port), tls ? 1 : 0));
    }

    @Override
    public int startRtmp(int port, boolean tls) {
        ensureLoaded();
        return unsigned(api.mk_rtmp_server_start(toUnsignedShort(port), tls ? 1 : 0));
    }

    @Override
    public List<ZlmStreamInfo> listStreams() {
        ensureLoaded();
        final List<ZlmStreamInfo> result = new ArrayList<ZlmStreamInfo>();
        api.mk_media_source_for_each(null, new ZlmNativeApi.MediaSourceCallback() {
            @Override
            public void invoke(Pointer userData, Pointer source) {
                if (!isNull(source)) {
                    result.add(readStream(source));
                }
            }
        }, null, null, null, null);
        return result;
    }

    @Override
    public NativeHandle pull(PullRequest request, final OperationListener listener) {
        ensureLoaded();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        StreamKey key = request.target();
        Pointer pointer = api.mk_proxy_player_create3(key.virtualHost(), key.application(), key.stream(),
            booleanOption(request.options(), "enable_hls") ? 1 : 0,
            booleanOption(request.options(), "enable_mp4") ? 1 : 0,
            intOption(request.options(), "retry_count", -1));
        ensurePointer(pointer, "create pull proxy");
        final ProxyHandle[] holder = new ProxyHandle[1];
        ZlmNativeApi.ProxyCallback closeCallback = new ZlmNativeApi.ProxyCallback() {
            @Override public void invoke(Pointer data, int error, String message, int systemError) {
                if (holder[0] != null) holder[0].close();
                listener.onClosed(operationMessage(error, systemError, message));
            }
        };
        ZlmNativeApi.ProxyCallback resultCallback = new ZlmNativeApi.ProxyCallback() {
            @Override public void invoke(Pointer data, int error, String message, int systemError) {
                listener.onResult(error == 0, operationMessage(error, systemError, message));
            }
        };
        ProxyHandle handle = new ProxyHandle(pointer, closeCallback, resultCallback);
        holder[0] = handle;
        handles.add(handle);
        api.mk_proxy_player_set_on_close(pointer, closeCallback, null);
        api.mk_proxy_player_set_on_play_result(pointer, resultCallback, null, null);
        applyProxyOptions(pointer, request.options());
        api.mk_proxy_player_play(pointer, request.sourceUri().toString());
        return handle;
    }

    @Override
    public NativeHandle push(PushRequest request, final OperationListener listener) {
        ensureLoaded();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        StreamKey source = request.source();
        Pointer pointer = api.mk_pusher_create(request.sourceSchema(), source.virtualHost(), source.application(), source.stream());
        ensurePointer(pointer, "create push proxy");
        final PushHandle[] holder = new PushHandle[1];
        ZlmNativeApi.PushCallback resultCallback = new ZlmNativeApi.PushCallback() {
            @Override public void invoke(Pointer data, int error, String message) {
                listener.onResult(error == 0, operationMessage(error, 0, message));
            }
        };
        ZlmNativeApi.PushCallback shutdownCallback = new ZlmNativeApi.PushCallback() {
            @Override public void invoke(Pointer data, int error, String message) {
                if (holder[0] != null) holder[0].close();
                listener.onClosed(operationMessage(error, 0, message));
            }
        };
        PushHandle handle = new PushHandle(pointer, resultCallback, shutdownCallback);
        holder[0] = handle;
        handles.add(handle);
        api.mk_pusher_set_on_result(pointer, resultCallback, null);
        api.mk_pusher_set_on_shutdown(pointer, shutdownCallback, null);
        for (Map.Entry<String, String> option : request.options().entrySet()) {
            api.mk_pusher_set_option(pointer, option.getKey(), option.getValue());
        }
        api.mk_pusher_publish(pointer, request.targetUri().toString());
        return handle;
    }

    @Override
    public boolean closeStream(final StreamKey key) {
        ensureLoaded();
        if (key == null) throw new IllegalArgumentException("key must not be null");
        final AtomicBoolean closed = new AtomicBoolean(false);
        api.mk_media_source_for_each(null, new ZlmNativeApi.MediaSourceCallback() {
            @Override public void invoke(Pointer data, Pointer source) {
                if (!isNull(source) && sameKey(source, key) && api.mk_media_source_close(source, 1) != 0) {
                    closed.set(true);
                }
            }
        }, null, key.virtualHost(), key.application(), key.stream());
        return closed.get();
    }

    @Override
    public boolean startRecording(RecordingRequest request) {
        ensureLoaded();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        StreamKey key = request.stream();
        String path = request.outputDirectory() == null ? null : request.outputDirectory().toAbsolutePath().toString();
        long seconds = request.segmentDuration() == null ? 0 : request.segmentDuration().getSeconds();
        return api.mk_recorder_start(request.format().nativeCode(), key.virtualHost(), key.application(), key.stream(), path, seconds) != 0;
    }

    @Override
    public boolean stopRecording(StreamKey key, RecordingFormat format) {
        ensureLoaded();
        requireRecordingArguments(key, format);
        return api.mk_recorder_stop(format.nativeCode(), key.virtualHost(), key.application(), key.stream()) != 0;
    }

    @Override
    public boolean isRecording(StreamKey key, RecordingFormat format) {
        ensureLoaded();
        requireRecordingArguments(key, format);
        return api.mk_recorder_is_recording(format.nativeCode(), key.virtualHost(), key.application(), key.stream()) != 0;
    }

    @Override
    public NativeRtpHandle openRtpServer(RtpServerRequest request, final Runnable onDetach) {
        ensureLoaded();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        StreamKey key = request.target();
        Pointer pointer = api.mk_rtp_server_create3(toUnsignedShort(request.port()), request.transport().nativeCode(),
            key.virtualHost(), key.application(), key.stream(), request.multiplexed() ? 1 : 0);
        ensurePointer(pointer, "create RTP server");
        final RtpHandle[] holder = new RtpHandle[1];
        ZlmNativeApi.RtpDetachCallback callback = new ZlmNativeApi.RtpDetachCallback() {
            @Override public void invoke(Pointer data) {
                if (holder[0] != null) holder[0].close();
                if (onDetach != null) onDetach.run();
            }
        };
        RtpHandle handle = new RtpHandle(pointer, unsigned(api.mk_rtp_server_port(pointer)), callback);
        holder[0] = handle;
        handles.add(handle);
        api.mk_rtp_server_set_on_detach(pointer, callback, null);
        return handle;
    }

    @Override
    public CompletionStage<String> answer(WebRtcRequest request) {
        ensureLoaded();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        final CompletableFuture<String> future = new CompletableFuture<String>();
        final ZlmNativeApi.WebRtcCallback[] holder = new ZlmNativeApi.WebRtcCallback[1];
        ZlmNativeApi.WebRtcCallback callback = new ZlmNativeApi.WebRtcCallback() {
            @Override public void invoke(Pointer data, String answer, String error) {
                pendingWebRtcCallbacks.remove(holder[0]);
                if (error != null && !error.trim().isEmpty()) future.completeExceptionally(new StreamBridgeException(error));
                else future.complete(answer);
            }
        };
        holder[0] = callback;
        pendingWebRtcCallbacks.add(callback);
        api.mk_webrtc_get_answer_sdp(null, callback, request.type().name().toLowerCase(), request.offerSdp(), request.streamUri().toString());
        return future;
    }

    @Override
    public NativeMediaHandle createPublisher(MediaInputRequest request) {
        ensureLoaded();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        StreamKey key = request.target();
        Pointer pointer = api.mk_media_create(key.virtualHost(), key.application(), key.stream(), 0,
            request.hls() ? 1 : 0, request.mp4() ? 1 : 0);
        ensurePointer(pointer, "create media publisher");
        try {
            if (request.videoCodec() != null && api.mk_media_init_video(pointer, request.videoCodec().nativeCode(),
                request.width(), request.height(), request.framesPerSecond(), request.bitRate()) == 0) {
                throw new StreamBridgeException("ZLMediaKit rejected the video track");
            }
            if (request.audioCodec() != null && api.mk_media_init_audio(pointer, request.audioCodec().nativeCode(),
                request.sampleRate(), request.channels(), request.sampleBits()) == 0) {
                throw new StreamBridgeException("ZLMediaKit rejected the audio track");
            }
            api.mk_media_init_complete(pointer);
            final MediaHandle[] holder = new MediaHandle[1];
            ZlmNativeApi.MediaCloseCallback callback = new ZlmNativeApi.MediaCloseCallback() {
                @Override public void invoke(Pointer data) { if (holder[0] != null) holder[0].close(); }
            };
            MediaHandle handle = new MediaHandle(pointer, callback);
            holder[0] = handle;
            handles.add(handle);
            api.mk_media_set_on_close(pointer, callback, null);
            return handle;
        } catch (RuntimeException failure) {
            api.mk_media_release(pointer);
            throw failure;
        }
    }

    @Override
    public NativePlayerHandle play(PlayerRequest request) {
        return play(request, null);
    }

    @Override
    public NativePlayerHandle play(PlayerRequest request, final FrameListener frameListener) {
        ensureLoaded();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        Pointer pointer = api.mk_player_create();
        ensurePointer(pointer, "create player");
        final PlayerHandle[] holder = new PlayerHandle[1];
        ZlmNativeApi.PlayerCallback result = new ZlmNativeApi.PlayerCallback() {
            @Override public void invoke(Pointer data, int error, String message, Pointer tracks, int trackCount) {
                if (holder[0] != null) {
                    holder[0].active.set(error == 0);
                    if (error == 0 && frameListener != null) holder[0].attachTracks(tracks, trackCount, frameListener);
                }
            }
        };
        ZlmNativeApi.PlayerCallback shutdown = new ZlmNativeApi.PlayerCallback() {
            @Override public void invoke(Pointer data, int error, String message, Pointer tracks, int trackCount) {
                if (holder[0] != null) holder[0].close();
            }
        };
        PlayerHandle handle = new PlayerHandle(pointer, result, shutdown);
        holder[0] = handle;
        handles.add(handle);
        for (Map.Entry<String, String> option : request.options().entrySet()) {
            api.mk_player_set_option(pointer, option.getKey(), option.getValue());
        }
        api.mk_player_set_on_result(pointer, result, null);
        api.mk_player_set_on_shutdown(pointer, shutdown, null);
        api.mk_player_play(pointer, request.sourceUri().toString());
        return handle;
    }

    @Override
    public void setOption(String key, String value) {
        ensureLoaded();
        if (key == null || key.trim().isEmpty()) throw new IllegalArgumentException("key must not be blank");
        api.mk_set_option(key, value);
    }

    @Override
    public String getOption(String key) {
        ensureLoaded();
        if (key == null || key.trim().isEmpty()) throw new IllegalArgumentException("key must not be blank");
        return api.mk_get_option(key);
    }

    @Override
    public void setStreamChangeListener(StreamChangeListener listener) {
        streamChangeListener = listener;
    }

    @Override
    public void stopAll() {
        if (api != null) {
            for (ManagedHandle handle : new ArrayList<ManagedHandle>(handles)) {
                handle.close();
            }
            pendingWebRtcCallbacks.clear();
            api.mk_events_listen(null);
            events = null;
            api.mk_stop_all_server();
            api = null;
        }
    }

    private void ensureLoaded() {
        if (api == null) {
            throw new StreamBridgeException("ZLMediaKit native library is not initialized");
        }
    }

    private static short toUnsignedShort(int port) {
        if (port < 0 || port > 65535) {
            throw new StreamBridgeException("Port must be between 0 and 65535, but was " + port);
        }
        return (short) (port & 0xffff);
    }

    private static int unsigned(short value) {
        return value & 0xffff;
    }

    private static String nullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    private ZlmStreamInfo readStream(Pointer source) {
        return new ZlmStreamInfo(api.mk_media_source_get_schema(source), api.mk_media_source_get_vhost(source),
            api.mk_media_source_get_app(source), api.mk_media_source_get_stream(source),
            api.mk_media_source_get_origin_url(source), api.mk_media_source_get_reader_count(source),
            api.mk_media_source_get_total_reader_count(source), api.mk_media_source_get_bytes_speed(source),
            api.mk_media_source_get_alive_second(source));
    }

    private void installEventListener() {
        events = new ZlmNativeApi.Events();
        events.on_mk_media_changed = new ZlmNativeApi.MediaChangedCallback() {
            @Override public void invoke(int registered, Pointer source) {
                StreamChangeListener listener = streamChangeListener;
                if (listener != null && !isNull(source)) listener.onChanged(registered != 0, readStream(source));
            }
        };
        events.write();
        api.mk_events_listen(events);
    }

    private boolean sameKey(Pointer source, StreamKey key) {
        return key.virtualHost().equals(api.mk_media_source_get_vhost(source))
            && key.application().equals(api.mk_media_source_get_app(source))
            && key.stream().equals(api.mk_media_source_get_stream(source));
    }

    private void applyProxyOptions(Pointer pointer, Map<String, String> options) {
        for (Map.Entry<String, String> option : options.entrySet()) {
            if (!"enable_hls".equals(option.getKey()) && !"enable_mp4".equals(option.getKey()) && !"retry_count".equals(option.getKey())) {
                api.mk_proxy_player_set_option(pointer, option.getKey(), option.getValue());
            }
        }
    }

    private static boolean booleanOption(Map<String, String> options, String key) {
        return Boolean.parseBoolean(options.get(key));
    }

    private static int intOption(Map<String, String> options, String key, int fallback) {
        String value = options.get(key);
        if (value == null) return fallback;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException failure) { throw new StreamBridgeException("Option '" + key + "' must be an integer", failure); }
    }

    private static String operationMessage(int error, int systemError, String message) {
        String text = message == null ? "" : message;
        return text + (error == 0 && systemError == 0 ? "" : " (error=" + error + ", system=" + systemError + ")");
    }

    private static boolean isNull(Pointer pointer) { return pointer == null || Pointer.nativeValue(pointer) == 0; }
    private static void ensurePointer(Pointer pointer, String operation) {
        if (isNull(pointer)) throw new StreamBridgeException("ZLMediaKit failed to " + operation);
    }
    private static void requireRecordingArguments(StreamKey key, RecordingFormat format) {
        if (key == null) throw new IllegalArgumentException("key must not be null");
        if (format == null) throw new IllegalArgumentException("format must not be null");
    }

    private abstract class ManagedHandle implements NativeHandle {
        final Pointer pointer;
        final AtomicBoolean open = new AtomicBoolean(true);
        ManagedHandle(Pointer pointer) { this.pointer = pointer; }
        public boolean isOpen() { return open.get(); }
        public final void close() {
            if (open.compareAndSet(true, false)) {
                release(pointer);
                handles.remove(this);
            }
        }
        abstract void release(Pointer value);
    }

    private final class ProxyHandle extends ManagedHandle {
        @SuppressWarnings("unused") private final ZlmNativeApi.ProxyCallback closeCallback;
        @SuppressWarnings("unused") private final ZlmNativeApi.ProxyCallback resultCallback;
        ProxyHandle(Pointer pointer, ZlmNativeApi.ProxyCallback closeCallback, ZlmNativeApi.ProxyCallback resultCallback) {
            super(pointer); this.closeCallback = closeCallback; this.resultCallback = resultCallback;
        }
        void release(Pointer value) { api.mk_proxy_player_release(value); }
    }

    private final class PushHandle extends ManagedHandle {
        @SuppressWarnings("unused") private final ZlmNativeApi.PushCallback resultCallback;
        @SuppressWarnings("unused") private final ZlmNativeApi.PushCallback shutdownCallback;
        PushHandle(Pointer pointer, ZlmNativeApi.PushCallback resultCallback, ZlmNativeApi.PushCallback shutdownCallback) {
            super(pointer); this.resultCallback = resultCallback; this.shutdownCallback = shutdownCallback;
        }
        void release(Pointer value) { api.mk_pusher_release(value); }
    }

    private final class RtpHandle extends ManagedHandle implements NativeRtpHandle {
        private final int port;
        @SuppressWarnings("unused") private final ZlmNativeApi.RtpDetachCallback callback;
        RtpHandle(Pointer pointer, int port, ZlmNativeApi.RtpDetachCallback callback) { super(pointer); this.port = port; this.callback = callback; }
        public int port() { return port; }
        public void updateSsrc(long ssrc) {
            if (ssrc < 0 || ssrc > 0xffffffffL) throw new IllegalArgumentException("ssrc must be an unsigned 32-bit value");
            api.mk_rtp_server_update_ssrc(pointer, (int) ssrc);
        }
        void release(Pointer value) { api.mk_rtp_server_release(value); }
    }

    private final class MediaHandle extends ManagedHandle implements NativeMediaHandle {
        @SuppressWarnings("unused") private final ZlmNativeApi.MediaCloseCallback callback;
        MediaHandle(Pointer pointer, ZlmNativeApi.MediaCloseCallback callback) { super(pointer); this.callback = callback; }
        public boolean input(EncodedFrame frame) {
            if (frame == null) throw new IllegalArgumentException("frame must not be null");
            if (!isOpen()) return false;
            byte[] data = frame.data();
            Pointer nativeFrame = api.mk_frame_create(frame.codec().nativeCode(), frame.decodingTimestamp(),
                frame.presentationTimestamp(), data, data.length, null, null);
            ensurePointer(nativeFrame, "create media frame");
            try { return api.mk_media_input_frame(pointer, nativeFrame) != 0; }
            finally { api.mk_frame_unref(nativeFrame); }
        }
        public int readerCount() { return isOpen() ? api.mk_media_total_reader_count(pointer) : 0; }
        void release(Pointer value) { api.mk_media_release(value); }
    }

    private final class PlayerHandle extends ManagedHandle implements NativePlayerHandle {
        private final AtomicBoolean active = new AtomicBoolean(false);
        @SuppressWarnings("unused") private final ZlmNativeApi.PlayerCallback result;
        @SuppressWarnings("unused") private final ZlmNativeApi.PlayerCallback shutdown;
        private final List<TrackDelegate> delegates = new ArrayList<TrackDelegate>();
        PlayerHandle(Pointer pointer, ZlmNativeApi.PlayerCallback result, ZlmNativeApi.PlayerCallback shutdown) {
            super(pointer); this.result = result; this.shutdown = shutdown;
        }
        @Override public boolean isOpen() { return super.isOpen() && active.get(); }
        public void pause(boolean paused) { api.mk_player_pause(pointer, paused ? 1 : 0); }
        public void speed(float multiplier) { if (multiplier <= 0) throw new IllegalArgumentException("multiplier must be positive"); api.mk_player_speed(pointer, multiplier); }
        public void seek(float progress) { if (progress < 0 || progress > 1) throw new IllegalArgumentException("progress must be between 0 and 1"); api.mk_player_seekto(pointer, progress); }
        public void seekSeconds(int seconds) { if (seconds < 0) throw new IllegalArgumentException("seconds must not be negative"); api.mk_player_seekto_pos(pointer, seconds); }
        public float durationSeconds() { return api.mk_player_duration(pointer); }
        public float progress() { return api.mk_player_progress(pointer); }
        public int progressSeconds() { return api.mk_player_progress_pos(pointer); }
        public float lossRate(boolean video) { return api.mk_player_loss_rate(pointer, video ? 0 : 1); }
        synchronized void attachTracks(Pointer tracks, int count, final FrameListener listener) {
            if (tracks == null || count <= 0 || !delegates.isEmpty()) return;
            Pointer[] values = tracks.getPointerArray(0, count);
            for (final Pointer track : values) {
                ZlmNativeApi.TrackFrameCallback callback = new ZlmNativeApi.TrackFrameCallback() {
                    @Override public void invoke(Pointer data, Pointer frame) {
                        try {
                            int size = Math.toIntExact(api.mk_frame_get_data_size(frame));
                            byte[] bytes = api.mk_frame_get_data(frame).getByteArray(0, size);
                            listener.onFrame(new EncodedFrame(MediaCodec.fromNativeCode(api.mk_frame_codec_id(frame)), bytes,
                                api.mk_frame_get_dts(frame), api.mk_frame_get_pts(frame)));
                        } catch (RuntimeException ignored) {
                            // Consumer exceptions must not cross a native callback boundary.
                        }
                    }
                };
                Pointer tag = api.mk_track_add_delegate(track, callback, null);
                if (!isNull(tag)) delegates.add(new TrackDelegate(track, tag, callback));
            }
        }
        synchronized void release(Pointer value) {
            for (TrackDelegate delegate : delegates) api.mk_track_del_delegate(delegate.track, delegate.tag);
            delegates.clear();
            api.mk_player_release(value);
        }
    }

    private static final class TrackDelegate {
        final Pointer track;
        final Pointer tag;
        @SuppressWarnings("unused") final ZlmNativeApi.TrackFrameCallback callback;
        TrackDelegate(Pointer track, Pointer tag, ZlmNativeApi.TrackFrameCallback callback) {
            this.track = track; this.tag = tag; this.callback = callback;
        }
    }
}
