package io.github.streambridge.engine.zlm;

import com.sun.jna.Native;
import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.EngineState;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RecordingOperations;
import io.github.streambridge.api.RecordingRequest;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZlmStreamEngineTest {
    @Test
    void nativeEventStructureMatchesOfficialPointerFieldCount() {
        assertEquals(22 * Native.POINTER_SIZE, new ZlmNativeApi.Events().size());
    }

    @Test
    void mapsConfigurationToNativeLifecycleWithoutExposingJna() {
        RecordingNativeClient nativeClient = new RecordingNativeClient();
        ZlmStreamEnginePlugin plugin = new ZlmStreamEnginePlugin(configuration -> nativeClient);
        EngineConfiguration configuration = EngineConfiguration.builder()
            .option(ZlmOptions.HTTP_PORT, 7788)
            .option(ZlmOptions.RTSP_PORT, 554)
            .option(ZlmOptions.RTMP_PORT, -1)
            .build();

        StreamEngine engine = plugin.create(configuration);
        engine.start();
        engine.stop();

        assertEquals(EngineState.STOPPED, engine.state());
        assertEquals("initialize", nativeClient.calls.get(0));
        assertEquals("http:7788:false", nativeClient.calls.get(1));
        assertEquals("rtsp:554:false", nativeClient.calls.get(2));
        assertEquals("stop", nativeClient.calls.get(3));
    }

    @Test
    void delegatesStreamAndRecordingCapabilitiesToNativeClient() {
        OperationsNativeClient nativeClient = new OperationsNativeClient();
        StreamEngine engine = new ZlmStreamEnginePlugin(configuration -> nativeClient).create(EngineConfiguration.builder()
            .option(ZlmOptions.HTTP_PORT, -1).build());
        engine.start();
        StreamKey key = StreamKey.of("__defaultVhost__", "live", "camera-1");

        assertEquals(1, engine.listStreams().size());
        StreamHandle handle = engine.pull(PullRequest.builder().sourceUri("rtsp://example.test/camera-1").target(key).build());
        assertTrue(handle.isOpen());
        RecordingOperations recording = engine.extension(RecordingOperations.class).get();
        assertTrue(recording.startRecording(RecordingRequest.builder().stream(key).format(RecordingFormat.MP4).build()));
        assertTrue(recording.isRecording(key, RecordingFormat.MP4));

        handle.close();
        engine.stop();
    }

    private static class RecordingNativeClient implements ZlmNativeClient {
        private final List<String> calls = new ArrayList<String>();

        @Override
        public void initialize(EngineConfiguration configuration) {
            calls.add("initialize");
        }

        @Override
        public int startHttp(int port, boolean tls) {
            calls.add("http:" + port + ":" + tls);
            return port == 0 ? 10080 : port;
        }

        @Override
        public int startRtsp(int port, boolean tls) {
            calls.add("rtsp:" + port + ":" + tls);
            return port;
        }

        @Override
        public int startRtmp(int port, boolean tls) {
            calls.add("rtmp:" + port + ":" + tls);
            return port;
        }

        @Override
        public void stopAll() {
            calls.add("stop");
        }
    }

    private static final class OperationsNativeClient extends RecordingNativeClient {
        private boolean recording;

        @Override
        public List<ZlmStreamInfo> listStreams() {
            return Collections.singletonList(new ZlmStreamInfo("rtsp", "__defaultVhost__", "live", "camera-1",
                "rtsp://example.test/camera-1", 2, 3, 4096, 8));
        }

        @Override
        public NativeHandle pull(PullRequest request, OperationListener listener) {
            final AtomicBoolean open = new AtomicBoolean(true);
            listener.onResult(true, "ok");
            return new NativeHandle() {
                public boolean isOpen() { return open.get(); }
                public void close() { open.set(false); }
            };
        }

        @Override public boolean startRecording(RecordingRequest request) { recording = true; return true; }
        @Override public boolean stopRecording(StreamKey key, RecordingFormat format) { recording = false; return true; }
        @Override public boolean isRecording(StreamKey key, RecordingFormat format) { return recording; }
    }
}
