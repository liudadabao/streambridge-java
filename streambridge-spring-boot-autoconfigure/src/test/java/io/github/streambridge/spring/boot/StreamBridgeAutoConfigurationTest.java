package io.github.streambridge.spring.boot;

import io.github.streambridge.api.EngineState;
import io.github.streambridge.api.StreamBridge;
import io.github.streambridge.api.StreamEngine;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class StreamBridgeAutoConfigurationTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(StreamBridgeAutoConfiguration.class));

    @Test
    void discoversMockPluginAndStartsWithContext() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(StreamBridge.class);
            assertThat(context).hasSingleBean(StreamEngine.class);
            assertThat(context.getBean(StreamEngine.class).state()).isEqualTo(EngineState.RUNNING);
        });
    }

    @Test
    void canBeDisabled() {
        runner.withPropertyValues("streambridge.enabled=false")
            .run(context -> assertThat(context).doesNotHaveBean(StreamBridge.class));
    }
}

