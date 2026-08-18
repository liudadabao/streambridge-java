package io.github.streambridge.engine.mock;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.StreamBridge;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamEvent;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RecordingOperations;
import io.github.streambridge.api.RecordingRequest;
import io.github.streambridge.api.RtpOperations;
import io.github.streambridge.api.RtpServerHandle;
import io.github.streambridge.api.RtpServerRequest;
import io.github.streambridge.api.RtpTransport;
import io.github.streambridge.api.WebRtcOperations;
import io.github.streambridge.api.WebRtcRequest;
import io.github.streambridge.api.EncodedFrame;
import io.github.streambridge.api.MediaCodec;
import io.github.streambridge.api.MediaInputOperations;
import io.github.streambridge.api.MediaInputRequest;
import io.github.streambridge.api.MediaPlayer;
import io.github.streambridge.api.MediaPublisher;
import io.github.streambridge.api.PlayerOperations;
import io.github.streambridge.api.PlayerRequest;
import io.github.streambridge.api.RuntimeConfigurationOperations;
import io.github.streambridge.core.StreamBridges;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockStreamEngineTest {
    @Test
    void serviceLoaderDiscoversPluginAndStreamLifecycleFormsAClosedLoop() {
        List<StreamEvent.Type> events = new ArrayList<StreamEvent.Type>();
        try (StreamBridge bridge = StreamBridges.builder().build()) {
            assertTrue(bridge.availablePlugins().contains("mock"));
            try (StreamEngine engine = bridge.open("mock", EngineConfiguration.empty())) {
                engine.subscribe(event -> events.add(event.type()));
                StreamHandle handle = engine.pull(PullRequest.builder()
                    .sourceUri("rtsp://example.test/live/camera-1")
                    .target(StreamKey.of("__defaultVhost__", "live", "camera-1"))
                    .build());

                assertTrue(handle.isOpen());
                assertEquals(1, engine.listStreams().size());

                handle.close();
                assertFalse(handle.isOpen());
                assertTrue(engine.listStreams().isEmpty());
            }
        }
        assertTrue(events.contains(StreamEvent.Type.STREAM_AVAILABLE));
        assertTrue(events.contains(StreamEvent.Type.STREAM_UNAVAILABLE));
    }

    @Test
    void optionalCapabilitiesCanBeUsedWithoutImplementationTypes() throws Exception {
        StreamKey key = StreamKey.of("__defaultVhost__", "live", "camera-2");
        try (StreamBridge bridge = StreamBridges.builder().build();
             StreamEngine engine = bridge.open("mock", EngineConfiguration.empty())) {
            RecordingOperations recording = engine.extension(RecordingOperations.class).get();
            assertTrue(recording.startRecording(RecordingRequest.builder().stream(key).format(RecordingFormat.MP4).build()));
            assertTrue(recording.isRecording(key, RecordingFormat.MP4));
            assertTrue(recording.stopRecording(key, RecordingFormat.MP4));

            RtpServerHandle rtp = engine.extension(RtpOperations.class).get()
                .openRtpServer(new RtpServerRequest(key, 0, RtpTransport.UDP, false));
            assertEquals(10000, rtp.port());
            rtp.updateSsrc(0xffffffffL);
            rtp.close();

            String answer = engine.extension(WebRtcOperations.class).get()
                .answer(new WebRtcRequest(WebRtcRequest.Type.PLAY, "v=0", URI.create("webrtc://localhost/live/camera-2")))
                .toCompletableFuture().get();
            assertTrue(answer.contains("x-request-type:play"));

            MediaPublisher publisher = engine.extension(MediaInputOperations.class).get().createPublisher(
                MediaInputRequest.builder().target(key).video(MediaCodec.H264, 1920, 1080, 25, 2_000_000).build());
            assertTrue(publisher.input(new EncodedFrame(MediaCodec.H264, new byte[] {0, 0, 1, 9}, 0, 0)));
            publisher.close();

            MediaPlayer player = engine.extension(PlayerOperations.class).get().play(
                PlayerRequest.builder().sourceUri("rtsp://example.test/camera-2").build());
            player.seek(0.5f);
            assertEquals(30, player.progressSeconds());
            player.close();

            RuntimeConfigurationOperations options = engine.extension(RuntimeConfigurationOperations.class).get();
            options.setOption("protocol.enable_hls", "1");
            assertEquals("1", options.getOption("protocol.enable_hls"));
        }
    }
}
