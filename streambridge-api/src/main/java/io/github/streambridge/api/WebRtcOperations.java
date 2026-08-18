package io.github.streambridge.api;

import java.util.concurrent.CompletionStage;

public interface WebRtcOperations {
    CompletionStage<String> answer(WebRtcRequest request);
}
