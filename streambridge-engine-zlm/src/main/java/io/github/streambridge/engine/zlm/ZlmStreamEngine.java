package io.github.streambridge.engine.zlm;

import io.github.streambridge.api.EngineCapability;
import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.EncodedFrame;
import io.github.streambridge.api.FrameListener;
import io.github.streambridge.api.MediaInputOperations;
import io.github.streambridge.api.MediaInputRequest;
import io.github.streambridge.api.MediaPublisher;
import io.github.streambridge.api.MediaPlayer;
import io.github.streambridge.api.PlayerOperations;
import io.github.streambridge.api.PlayerRequest;
import io.github.streambridge.api.PushOperations;
import io.github.streambridge.api.PushRequest;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RecordingOperations;
import io.github.streambridge.api.RecordingRequest;
import io.github.streambridge.api.RtpOperations;
import io.github.streambridge.api.RtpServerHandle;
import io.github.streambridge.api.RtpServerRequest;
import io.github.streambridge.api.RuntimeConfigurationOperations;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamDescriptor;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.WebRtcOperations;
import io.github.streambridge.api.WebRtcRequest;
import io.github.streambridge.core.AbstractStreamEngine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicBoolean;

final class ZlmStreamEngine extends AbstractStreamEngine implements RecordingOperations, PushOperations, RtpOperations, WebRtcOperations, MediaInputOperations, PlayerOperations, RuntimeConfigurationOperations {
    private final ZlmNativeClient nativeClient;

    ZlmStreamEngine(EngineConfiguration configuration, ZlmNativeClient nativeClient) {
        super(ZlmStreamEnginePlugin.ID, configuration,
            EnumSet.of(EngineCapability.SERVER_LIFECYCLE, EngineCapability.STREAM_QUERY, EngineCapability.PULL_PROXY,
                EngineCapability.PUSH_PROXY, EngineCapability.RECORDING, EngineCapability.WEBRTC,
                EngineCapability.GB28181, EngineCapability.RTP_SERVER, EngineCapability.MEDIA_INPUT, EngineCapability.PLAYER,
                EngineCapability.RUNTIME_CONFIGURATION, EngineCapability.EMBEDDED));
        this.nativeClient = nativeClient;
    }

    @Override
    protected void doStart() {
        nativeClient.setStreamChangeListener(new ZlmNativeClient.StreamChangeListener() {
            @Override public void onChanged(boolean registered, ZlmStreamInfo stream) {
                StreamKey key = StreamKey.of(stream.virtualHost, stream.application, stream.stream);
                publish(registered ? io.github.streambridge.api.StreamEvent.Type.STREAM_AVAILABLE
                    : io.github.streambridge.api.StreamEvent.Type.STREAM_UNAVAILABLE, key,
                    registered ? "ZLMediaKit stream registered" : "ZLMediaKit stream unregistered");
            }
        });
        nativeClient.initialize(configuration());
        startIfConfigured("HTTP", configuration().getInt(ZlmOptions.HTTP_PORT, 0), false);
        startIfConfigured("HTTPS", configuration().getInt(ZlmOptions.HTTPS_PORT, -1), true);
        startIfConfigured("RTSP", configuration().getInt(ZlmOptions.RTSP_PORT, -1), false);
        startIfConfigured("RTSPS", configuration().getInt(ZlmOptions.RTSPS_PORT, -1), true);
        startIfConfigured("RTMP", configuration().getInt(ZlmOptions.RTMP_PORT, -1), false);
        startIfConfigured("RTMPS", configuration().getInt(ZlmOptions.RTMPS_PORT, -1), true);
    }

    @Override
    protected void doStop() {
        nativeClient.stopAll();
    }

    @Override
    public List<StreamDescriptor> listStreams() {
        requireRunning();
        List<StreamDescriptor> result = new ArrayList<StreamDescriptor>();
        for (ZlmStreamInfo stream : nativeClient.listStreams()) {
            Map<String, String> attributes = new LinkedHashMap<String, String>();
            attributes.put("schema", safe(stream.schema));
            attributes.put("totalReaders", String.valueOf(stream.totalReaders));
            attributes.put("bytesPerSecond", String.valueOf(stream.bytesPerSecond));
            attributes.put("aliveSeconds", String.valueOf(stream.aliveSeconds));
            result.add(new StreamDescriptor(StreamKey.of(stream.virtualHost, stream.application, stream.stream),
                stream.originUrl, stream.readers, attributes));
        }
        return result;
    }

    @Override
    public StreamHandle pull(PullRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        final StreamKey key = request.target();
        final AtomicBoolean available = new AtomicBoolean(false);
        ZlmNativeClient.NativeHandle nativeHandle = nativeClient.pull(request, new ZlmNativeClient.OperationListener() {
            @Override public void onResult(boolean success, String message) {
                if (success) {
                    available.set(true);
                    publish(io.github.streambridge.api.StreamEvent.Type.STREAM_AVAILABLE, key, message);
                } else {
                    publish(io.github.streambridge.api.StreamEvent.Type.STREAM_UNAVAILABLE, key, message);
                }
            }
            @Override public void onClosed(String message) {
                if (available.getAndSet(false)) publish(io.github.streambridge.api.StreamEvent.Type.STREAM_UNAVAILABLE, key, message);
            }
        });
        return wrap(key, nativeHandle, available);
    }

    @Override
    public boolean closeStream(StreamKey key) {
        requireRunning();
        return nativeClient.closeStream(key);
    }

    @Override
    public StreamHandle push(PushRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        final AtomicBoolean active = new AtomicBoolean(false);
        final StreamKey key = request.source();
        ZlmNativeClient.NativeHandle nativeHandle = nativeClient.push(request, new ZlmNativeClient.OperationListener() {
            public void onResult(boolean success, String message) { active.set(success); }
            public void onClosed(String message) { active.set(false); }
        });
        return wrap(key, nativeHandle, active);
    }

    @Override
    public boolean startRecording(RecordingRequest request) { requireRunning(); return nativeClient.startRecording(request); }
    @Override
    public boolean stopRecording(StreamKey stream, RecordingFormat format) { requireRunning(); return nativeClient.stopRecording(stream, format); }
    @Override
    public boolean isRecording(StreamKey stream, RecordingFormat format) { requireRunning(); return nativeClient.isRecording(stream, format); }

    @Override
    public RtpServerHandle openRtpServer(RtpServerRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        final ZlmNativeClient.NativeRtpHandle nativeHandle = nativeClient.openRtpServer(request, null);
        final StreamKey key = request.target();
        return new RtpServerHandle() {
            public StreamKey key() { return key; }
            public int port() { return nativeHandle.port(); }
            public boolean isOpen() { return nativeHandle.isOpen(); }
            public void updateSsrc(long ssrc) { nativeHandle.updateSsrc(ssrc); }
            public void close() { nativeHandle.close(); }
        };
    }

    @Override
    public CompletionStage<String> answer(WebRtcRequest request) { requireRunning(); return nativeClient.answer(request); }

    @Override
    public MediaPublisher createPublisher(MediaInputRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        final StreamKey key = request.target();
        final ZlmNativeClient.NativeMediaHandle handle = nativeClient.createPublisher(request);
        return new MediaPublisher() {
            public StreamKey key() { return key; }
            public boolean isOpen() { return handle.isOpen(); }
            public boolean input(EncodedFrame frame) { return handle.input(frame); }
            public int readerCount() { return handle.readerCount(); }
            public void close() { handle.close(); }
        };
    }

    @Override
    public MediaPlayer play(PlayerRequest request) {
        return play(request, null);
    }

    @Override
    public MediaPlayer play(PlayerRequest request, FrameListener listener) {
        requireRunning();
        final ZlmNativeClient.NativePlayerHandle handle = nativeClient.play(request, listener);
        return new MediaPlayer() {
            public boolean isOpen() { return handle.isOpen(); }
            public void pause(boolean paused) { handle.pause(paused); }
            public void speed(float multiplier) { handle.speed(multiplier); }
            public void seek(float progress) { handle.seek(progress); }
            public void seekSeconds(int seconds) { handle.seekSeconds(seconds); }
            public float durationSeconds() { return handle.durationSeconds(); }
            public float progress() { return handle.progress(); }
            public int progressSeconds() { return handle.progressSeconds(); }
            public float lossRate(boolean video) { return handle.lossRate(video); }
            public void close() { handle.close(); }
        };
    }

    @Override
    public void setOption(String key, String value) { requireRunning(); nativeClient.setOption(key, value); }

    @Override
    public String getOption(String key) { requireRunning(); return nativeClient.getOption(key); }

    private void startIfConfigured(String protocol, int configuredPort, boolean tls) {
        if (configuredPort < 0) {
            return;
        }
        if (configuredPort > 65535) {
            throw new StreamBridgeException(protocol + " port must be between -1 and 65535, but was " + configuredPort);
        }
        int actualPort;
        if (protocol.startsWith("HTTP")) {
            actualPort = nativeClient.startHttp(configuredPort, tls);
        } else if (protocol.startsWith("RTSP")) {
            actualPort = nativeClient.startRtsp(configuredPort, tls);
        } else {
            actualPort = nativeClient.startRtmp(configuredPort, tls);
        }
        if (actualPort == 0) {
            throw new StreamBridgeException("ZLMediaKit failed to start " + protocol + " server on port " + configuredPort);
        }
    }

    private static StreamHandle wrap(final StreamKey key, final ZlmNativeClient.NativeHandle nativeHandle, final AtomicBoolean active) {
        return new StreamHandle() {
            public StreamKey key() { return key; }
            public boolean isOpen() { return nativeHandle.isOpen() && active.get(); }
            public void close() { nativeHandle.close(); active.set(false); }
        };
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
