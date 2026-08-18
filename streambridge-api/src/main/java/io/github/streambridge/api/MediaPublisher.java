package io.github.streambridge.api;

public interface MediaPublisher extends StreamHandle {
    boolean input(EncodedFrame frame);

    int readerCount();
}
