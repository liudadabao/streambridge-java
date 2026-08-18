package io.github.streambridge.engine.mock;

import io.github.streambridge.api.EngineCapability;
import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.EncodedFrame;
import io.github.streambridge.api.MediaInputOperations;
import io.github.streambridge.api.MediaInputRequest;
import io.github.streambridge.api.MediaPublisher;
import io.github.streambridge.api.MediaPlayer;
import io.github.streambridge.api.PlayerOperations;
import io.github.streambridge.api.PlayerRequest;
import io.github.streambridge.api.RuntimeConfigurationOperations;
import io.github.streambridge.api.PushOperations;
import io.github.streambridge.api.PushRequest;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RecordingOperations;
import io.github.streambridge.api.RecordingRequest;
import io.github.streambridge.api.RtpOperations;
import io.github.streambridge.api.RtpServerHandle;
import io.github.streambridge.api.RtpServerRequest;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamDescriptor;
import io.github.streambridge.api.StreamEvent;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.WebRtcOperations;
import io.github.streambridge.api.WebRtcRequest;
import io.github.streambridge.core.AbstractStreamEngine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

final class MockStreamEngine extends AbstractStreamEngine implements RecordingOperations, PushOperations, RtpOperations, WebRtcOperations, MediaInputOperations, PlayerOperations, RuntimeConfigurationOperations {
    private final ConcurrentMap<StreamKey, StreamDescriptor> streams = new ConcurrentHashMap<StreamKey, StreamDescriptor>();
    private final ConcurrentMap<StreamKey, AtomicBoolean> pushes = new ConcurrentHashMap<StreamKey, AtomicBoolean>();
    private final Set<String> recordings = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    private final ConcurrentMap<StreamKey, MockRtpHandle> rtpServers = new ConcurrentHashMap<StreamKey, MockRtpHandle>();
    private final ConcurrentMap<String, String> runtimeOptions = new ConcurrentHashMap<String, String>();

    MockStreamEngine(EngineConfiguration configuration) {
        super(MockStreamEnginePlugin.ID, configuration,
            EnumSet.of(EngineCapability.SERVER_LIFECYCLE, EngineCapability.STREAM_QUERY, EngineCapability.PULL_PROXY,
                EngineCapability.PUSH_PROXY, EngineCapability.RECORDING, EngineCapability.WEBRTC,
                EngineCapability.GB28181, EngineCapability.RTP_SERVER, EngineCapability.MEDIA_INPUT, EngineCapability.PLAYER,
                EngineCapability.RUNTIME_CONFIGURATION));
    }

    @Override
    protected void doStart() {
        // Intentionally empty: no ports, files, threads, or native libraries.
    }

    @Override
    protected void doStop() {
        for (StreamKey key : new ArrayList<StreamKey>(streams.keySet())) {
            removeStream(key);
        }
        pushes.clear();
        recordings.clear();
        for (MockRtpHandle handle : new ArrayList<MockRtpHandle>(rtpServers.values())) {
            handle.close();
        }
    }

    @Override
    public List<StreamDescriptor> listStreams() {
        requireRunning();
        return Collections.unmodifiableList(new ArrayList<StreamDescriptor>(streams.values()));
    }

    @Override
    public StreamHandle pull(PullRequest request) {
        requireRunning();
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        final StreamKey key = request.target();
        StreamDescriptor descriptor = new StreamDescriptor(key, request.sourceUri().toString(), 0, request.options());
        if (streams.putIfAbsent(key, descriptor) != null) {
            throw new StreamBridgeException("Stream '" + key + "' already exists");
        }
        publish(StreamEvent.Type.STREAM_AVAILABLE, key, "Mock stream is available");
        return new StreamHandle() {
            private final AtomicBoolean open = new AtomicBoolean(true);

            @Override
            public StreamKey key() {
                return key;
            }

            @Override
            public boolean isOpen() {
                return open.get() && streams.containsKey(key);
            }

            @Override
            public void close() {
                if (open.compareAndSet(true, false)) {
                    removeStream(key);
                }
            }
        };
    }

    @Override
    public boolean closeStream(StreamKey key) {
        requireRunning();
        return removeStream(key);
    }

    @Override
    public StreamHandle push(PushRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        final StreamKey key = request.source();
        final AtomicBoolean open = new AtomicBoolean(true);
        if (pushes.putIfAbsent(key, open) != null) throw new StreamBridgeException("Push for '" + key + "' already exists");
        return new StreamHandle() {
            public StreamKey key() { return key; }
            public boolean isOpen() { return open.get(); }
            public void close() { if (open.compareAndSet(true, false)) pushes.remove(key, open); }
        };
    }

    @Override
    public boolean startRecording(RecordingRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return recordings.add(recordingKey(request.stream(), request.format()));
    }

    @Override
    public boolean stopRecording(StreamKey stream, RecordingFormat format) {
        requireRunning();
        return recordings.remove(recordingKey(stream, format));
    }

    @Override
    public boolean isRecording(StreamKey stream, RecordingFormat format) {
        requireRunning();
        return recordings.contains(recordingKey(stream, format));
    }

    @Override
    public RtpServerHandle openRtpServer(RtpServerRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        MockRtpHandle handle = new MockRtpHandle(request.target(), request.port() == 0 ? 10000 : request.port());
        if (rtpServers.putIfAbsent(request.target(), handle) != null) {
            throw new StreamBridgeException("RTP server for '" + request.target() + "' already exists");
        }
        return handle;
    }

    @Override
    public CompletionStage<String> answer(WebRtcRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return CompletableFuture.completedFuture("v=0\r\ns=StreamBridge Mock\r\na=x-request-type:" + request.type().name().toLowerCase() + "\r\n");
    }

    @Override
    public MediaPublisher createPublisher(MediaInputRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        final StreamKey key = request.target();
        StreamDescriptor descriptor = new StreamDescriptor(key, "memory://publisher/" + key.stream(), 0, Collections.<String, String>emptyMap());
        if (streams.putIfAbsent(key, descriptor) != null) throw new StreamBridgeException("Stream '" + key + "' already exists");
        publish(StreamEvent.Type.STREAM_AVAILABLE, key, "Mock publisher is available");
        return new MediaPublisher() {
            private final AtomicBoolean open = new AtomicBoolean(true);
            public StreamKey key() { return key; }
            public boolean isOpen() { return open.get() && streams.containsKey(key); }
            public boolean input(EncodedFrame frame) {
                if (frame == null) throw new IllegalArgumentException("frame must not be null");
                return isOpen();
            }
            public int readerCount() { return 0; }
            public void close() { if (open.compareAndSet(true, false)) removeStream(key); }
        };
    }

    @Override
    public MediaPlayer play(PlayerRequest request) {
        requireRunning();
        if (request == null) throw new IllegalArgumentException("request must not be null");
        return new MediaPlayer() {
            private final AtomicBoolean open = new AtomicBoolean(true);
            private volatile float progress;
            public boolean isOpen() { return open.get(); }
            public void pause(boolean paused) { requireOpen(); }
            public void speed(float multiplier) { requireOpen(); if (multiplier <= 0) throw new IllegalArgumentException("multiplier must be positive"); }
            public void seek(float value) { requireOpen(); if (value < 0 || value > 1) throw new IllegalArgumentException("progress must be between 0 and 1"); progress = value; }
            public void seekSeconds(int seconds) { requireOpen(); progress = Math.max(0, seconds) / 60.0f; }
            public float durationSeconds() { return 60; }
            public float progress() { return progress; }
            public int progressSeconds() { return Math.round(progress * 60); }
            public float lossRate(boolean video) { return 0; }
            public void close() { open.set(false); }
            private void requireOpen() { if (!isOpen()) throw new StreamBridgeException("Player is closed"); }
        };
    }

    @Override
    public void setOption(String key, String value) {
        requireRunning();
        if (key == null || key.trim().isEmpty()) throw new IllegalArgumentException("key must not be blank");
        if (value == null) runtimeOptions.remove(key); else runtimeOptions.put(key, value);
    }

    @Override
    public String getOption(String key) {
        requireRunning();
        return runtimeOptions.get(key);
    }

    private boolean removeStream(StreamKey key) {
        if (key != null && streams.remove(key) != null) {
            publish(StreamEvent.Type.STREAM_UNAVAILABLE, key, "Mock stream is unavailable");
            return true;
        }
        return false;
    }

    private static String recordingKey(StreamKey stream, RecordingFormat format) {
        if (stream == null) throw new IllegalArgumentException("stream must not be null");
        if (format == null) throw new IllegalArgumentException("format must not be null");
        return stream.toString() + ":" + format.name();
    }

    private final class MockRtpHandle implements RtpServerHandle {
        private final StreamKey key;
        private final int port;
        private final AtomicBoolean open = new AtomicBoolean(true);
        private volatile long ssrc;

        private MockRtpHandle(StreamKey key, int port) { this.key = key; this.port = port; }
        public StreamKey key() { return key; }
        public int port() { return port; }
        public boolean isOpen() { return open.get(); }
        public void updateSsrc(long value) {
            if (value < 0 || value > 0xffffffffL) throw new IllegalArgumentException("ssrc must be an unsigned 32-bit value");
            ssrc = value;
        }
        public void close() { if (open.compareAndSet(true, false)) rtpServers.remove(key, this); }
    }
}
