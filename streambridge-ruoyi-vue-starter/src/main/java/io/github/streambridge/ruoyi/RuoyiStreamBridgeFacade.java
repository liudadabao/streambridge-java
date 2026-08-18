package io.github.streambridge.ruoyi;

import io.github.streambridge.api.StreamDescriptor;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.PushOperations;
import io.github.streambridge.api.PushRequest;
import io.github.streambridge.api.RecordingFormat;
import io.github.streambridge.api.RecordingOperations;
import io.github.streambridge.api.RecordingRequest;
import io.github.streambridge.api.RtpOperations;
import io.github.streambridge.api.RtpServerHandle;
import io.github.streambridge.api.RtpServerRequest;
import io.github.streambridge.api.RtpTransport;
import io.github.streambridge.api.StreamBridgeException;
import io.github.streambridge.api.StreamHandle;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.api.WebRtcOperations;
import io.github.streambridge.api.WebRtcRequest;

import java.net.URI;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class RuoyiStreamBridgeFacade {
    private final StreamEngine engine;
    private final ConcurrentMap<String, ManagedOperation> operations = new ConcurrentHashMap<>();

    public RuoyiStreamBridgeFacade(StreamEngine engine) {
        this.engine = Objects.requireNonNull(engine, "engine");
    }

    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("engine", engine.id());
        status.put("state", engine.state().name());
        status.put("capabilities", engine.capabilities());
        return status;
    }

    public List<StreamDescriptor> streams() {
        return engine.listStreams();
    }

    public List<Map<String, Object>> streamRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (StreamDescriptor descriptor : engine.listStreams()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("virtualHost", descriptor.key().virtualHost());
            row.put("application", descriptor.key().application());
            row.put("stream", descriptor.key().stream());
            row.put("sourceUri", descriptor.sourceUri());
            row.put("readerCount", descriptor.readerCount());
            rows.add(row);
        }
        return rows;
    }

    public StreamEngine engine() {
        return engine;
    }

    public Map<String, Object> pull(String sourceUri, StreamKey target, Map<String, String> options) {
        PullRequest.Builder builder = PullRequest.builder().sourceUri(sourceUri).target(target);
        for (Map.Entry<String, String> option : safeOptions(options).entrySet()) builder.option(option.getKey(), option.getValue());
        return register("pull", engine.pull(builder.build()));
    }

    public Map<String, Object> push(StreamKey source, String sourceSchema, String targetUri, Map<String, String> options) {
        PushOperations extension = require(PushOperations.class);
        PushRequest.Builder builder = PushRequest.builder().source(source).sourceSchema(sourceSchema).targetUri(targetUri);
        for (Map.Entry<String, String> option : safeOptions(options).entrySet()) builder.option(option.getKey(), option.getValue());
        return register("push", extension.push(builder.build()));
    }

    public boolean stopOperation(String operationId) {
        ManagedOperation operation = operations.remove(operationId);
        if (operation == null) return false;
        operation.handle.close();
        return true;
    }

    public boolean closeStream(StreamKey key) { return engine.closeStream(key); }

    public boolean startRecording(StreamKey key, RecordingFormat format, String directory, Long segmentSeconds) {
        RecordingRequest.Builder builder = RecordingRequest.builder().stream(key).format(format);
        if (directory != null && !directory.trim().isEmpty()) builder.outputDirectory(Paths.get(directory));
        if (segmentSeconds != null) builder.segmentDuration(Duration.ofSeconds(segmentSeconds));
        return require(RecordingOperations.class).startRecording(builder.build());
    }

    public boolean stopRecording(StreamKey key, RecordingFormat format) {
        return require(RecordingOperations.class).stopRecording(key, format);
    }

    public boolean isRecording(StreamKey key, RecordingFormat format) {
        return require(RecordingOperations.class).isRecording(key, format);
    }

    public Map<String, Object> openRtp(StreamKey target, int port, RtpTransport transport, boolean multiplexed) {
        RtpServerHandle handle = require(RtpOperations.class)
            .openRtpServer(new RtpServerRequest(target, port, transport, multiplexed));
        String id = UUID.randomUUID().toString();
        ManagedOperation operation = new ManagedOperation("rtp", handle);
        operations.put(id, operation);
        Map<String, Object> result = operationRow(id, operation);
        return result;
    }

    public CompletionStage<String> webrtcAnswer(WebRtcRequest.Type type, String offer, String streamUri) {
        return require(WebRtcOperations.class).answer(new WebRtcRequest(type, offer, URI.create(streamUri)));
    }

    public List<Map<String, Object>> operationRows() {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Map.Entry<String, ManagedOperation> entry : operations.entrySet()) {
            rows.add(operationRow(entry.getKey(), entry.getValue()));
        }
        return rows;
    }

    private Map<String, Object> register(String type, StreamHandle handle) {
        String id = UUID.randomUUID().toString();
        ManagedOperation operation = new ManagedOperation(type, handle);
        operations.put(id, operation);
        return operationRow(id, operation);
    }

    private static Map<String, Object> operationRow(String id, ManagedOperation operation) {
        StreamHandle handle = operation.handle;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("operationId", id);
        result.put("type", operation.type);
        result.put("stream", handle.key().stream());
        result.put("open", handle.isOpen());
        if (handle instanceof RtpServerHandle) result.put("port", ((RtpServerHandle) handle).port());
        return result;
    }

    private <T> T require(Class<T> type) {
        return engine.extension(type).orElseThrow(() -> new StreamBridgeException(
            "Engine '" + engine.id() + "' does not support " + type.getSimpleName()));
    }

    private static Map<String, String> safeOptions(Map<String, String> options) {
        return options == null ? Collections.emptyMap() : options;
    }

    private static final class ManagedOperation {
        private final String type;
        private final StreamHandle handle;
        private ManagedOperation(String type, StreamHandle handle) { this.type = type; this.handle = handle; }
    }
}
