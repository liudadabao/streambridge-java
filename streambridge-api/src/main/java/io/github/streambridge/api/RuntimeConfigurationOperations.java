package io.github.streambridge.api;

public interface RuntimeConfigurationOperations {
    void setOption(String key, String value);

    String getOption(String key);
}
