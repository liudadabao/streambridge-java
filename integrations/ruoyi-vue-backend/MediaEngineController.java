package com.ruoyi.web.controller.media;

import com.ruoyi.common.core.domain.AjaxResult;
import io.github.streambridge.ruoyi.RuoyiStreamBridgeFacade;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RtpTransport;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.WebRtcRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletionStage;

@RestController
@RequestMapping("/media/engine")
public class MediaEngineController {
    private final RuoyiStreamBridgeFacade streamBridge;

    public MediaEngineController(RuoyiStreamBridgeFacade streamBridge) {
        this.streamBridge = streamBridge;
    }

    @PreAuthorize("@ss.hasPermi('media:engine:query')")
    @GetMapping("/status")
    public AjaxResult status() {
        return AjaxResult.success(streamBridge.status());
    }

    @PreAuthorize("@ss.hasPermi('media:engine:query')")
    @GetMapping("/list")
    public AjaxResult list() {
        return AjaxResult.success(streamBridge.streamRows());
    }

    @PreAuthorize("@ss.hasPermi('media:engine:query')")
    @GetMapping("/operations")
    public AjaxResult operations() {
        return AjaxResult.success(streamBridge.operationRows());
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @PostMapping("/pull")
    public AjaxResult pull(@RequestBody PullCommand command) {
        return AjaxResult.success(streamBridge.pull(command.sourceUri(), command.target().toKey(), command.options()));
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @PostMapping("/push")
    public AjaxResult push(@RequestBody PushCommand command) {
        return AjaxResult.success(streamBridge.push(command.source().toKey(), command.sourceSchema(), command.targetUri(), command.options()));
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @DeleteMapping("/operations/{operationId}")
    public AjaxResult stopOperation(@PathVariable String operationId) {
        return AjaxResult.success(streamBridge.stopOperation(operationId));
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @DeleteMapping("/stream")
    public AjaxResult closeStream(@RequestBody StreamAddress stream) {
        return AjaxResult.success(streamBridge.closeStream(stream.toKey()));
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @PostMapping("/recording/start")
    public AjaxResult startRecording(@RequestBody RecordingCommand command) {
        return AjaxResult.success(streamBridge.startRecording(command.stream().toKey(), command.format(),
            command.directory(), command.segmentSeconds()));
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @PostMapping("/recording/stop")
    public AjaxResult stopRecording(@RequestBody RecordingCommand command) {
        return AjaxResult.success(streamBridge.stopRecording(command.stream().toKey(), command.format()));
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @PostMapping("/rtp/open")
    public AjaxResult openRtp(@RequestBody RtpCommand command) {
        return AjaxResult.success(streamBridge.openRtp(command.target().toKey(), command.port(),
            command.transport(), command.multiplexed()));
    }

    @PreAuthorize("@ss.hasPermi('media:engine:edit')")
    @PostMapping("/webrtc/answer")
    public CompletionStage<AjaxResult> webrtcAnswer(@RequestBody WebRtcCommand command) {
        return streamBridge.webrtcAnswer(command.type(), command.offer(), command.streamUri()).thenApply(AjaxResult::success);
    }

    public record StreamAddress(String virtualHost, String application, String stream) {
        StreamKey toKey() { return StreamKey.of(virtualHost, application, stream); }
    }

    public record PullCommand(String sourceUri, StreamAddress target, Map<String, String> options) { }
    public record PushCommand(StreamAddress source, String sourceSchema, String targetUri, Map<String, String> options) { }
    public record RecordingCommand(StreamAddress stream, RecordingFormat format, String directory, Long segmentSeconds) { }
    public record RtpCommand(StreamAddress target, int port, RtpTransport transport, boolean multiplexed) { }
    public record WebRtcCommand(WebRtcRequest.Type type, String offer, String streamUri) { }
}
