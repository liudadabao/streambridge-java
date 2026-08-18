package io.github.streambridge.spring.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties("streambridge")
public class StreamBridgeProperties {
    private boolean enabled = true;
    private String engine = "mock";
    private boolean autoStart = true;
    private boolean discoverPlugins = true;
    private Map<String, String> options = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public boolean isDiscoverPlugins() {
        return discoverPlugins;
    }

    public void setDiscoverPlugins(boolean discoverPlugins) {
        this.discoverPlugins = discoverPlugins;
    }

    public Map<String, String> getOptions() {
        return options;
    }

    public void setOptions(Map<String, String> options) {
        this.options = options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options);
    }
}

