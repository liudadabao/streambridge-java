package io.github.streambridge.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class EngineConfiguration {
    private static final EngineConfiguration EMPTY = new EngineConfiguration(Collections.<String, String>emptyMap());

    private final Map<String, String> options;

    private EngineConfiguration(Map<String, String> options) {
        this.options = Collections.unmodifiableMap(new LinkedHashMap<String, String>(options));
    }

    public static EngineConfiguration empty() {
        return EMPTY;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Map<String, String> asMap() {
        return options;
    }

    public String get(String key) {
        return options.get(key);
    }

    public String get(String key, String defaultValue) {
        String value = options.get(key);
        return value == null ? defaultValue : value;
    }

    public int getInt(String key, int defaultValue) {
        String value = options.get(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            throw new StreamBridgeException("Configuration '" + key + "' must be an integer, but was '" + value + "'", exception);
        }
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        String value = options.get(key);
        return value == null ? defaultValue : Boolean.parseBoolean(value);
    }

    public static final class Builder {
        private final Map<String, String> options = new LinkedHashMap<String, String>();

        public Builder option(String key, Object value) {
            String validatedKey = Objects.requireNonNull(key, "key").trim();
            if (validatedKey.isEmpty()) {
                throw new IllegalArgumentException("Configuration key must not be blank");
            }
            options.put(validatedKey, String.valueOf(Objects.requireNonNull(value, "value")));
            return this;
        }

        public Builder options(Map<String, String> values) {
            if (values != null) {
                for (Map.Entry<String, String> entry : values.entrySet()) {
                    option(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }

        public EngineConfiguration build() {
            return options.isEmpty() ? EMPTY : new EngineConfiguration(options);
        }
    }
}

