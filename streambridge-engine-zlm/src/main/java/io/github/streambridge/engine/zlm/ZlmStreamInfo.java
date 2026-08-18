package io.github.streambridge.engine.zlm;

final class ZlmStreamInfo {
    final String schema;
    final String virtualHost;
    final String application;
    final String stream;
    final String originUrl;
    final int readers;
    final int totalReaders;
    final int bytesPerSecond;
    final long aliveSeconds;

    ZlmStreamInfo(String schema, String virtualHost, String application, String stream, String originUrl,
                  int readers, int totalReaders, int bytesPerSecond, long aliveSeconds) {
        this.schema = schema;
        this.virtualHost = virtualHost;
        this.application = application;
        this.stream = stream;
        this.originUrl = originUrl;
        this.readers = readers;
        this.totalReaders = totalReaders;
        this.bytesPerSecond = bytesPerSecond;
        this.aliveSeconds = aliveSeconds;
    }
}
