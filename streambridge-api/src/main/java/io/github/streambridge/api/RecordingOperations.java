package io.github.streambridge.api;

public interface RecordingOperations {
    boolean startRecording(RecordingRequest request);

    boolean stopRecording(StreamKey stream, RecordingFormat format);

    boolean isRecording(StreamKey stream, RecordingFormat format);
}
