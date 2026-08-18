package io.github.streambridge.ruoyi;

import io.github.streambridge.api.PullRequest;
import io.github.streambridge.api.StreamKey;
import io.github.streambridge.spring.boot.StreamBridgeAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RuoyiStreamBridgeAutoConfigurationTest {
    @Test
    void createsFacadeInsideRuoyiApplication() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                StreamBridgeAutoConfiguration.class,
                RuoyiStreamBridgeAutoConfiguration.class
            ))
            .run(context -> {
                assertThat(context).hasSingleBean(RuoyiStreamBridgeFacade.class);
                RuoyiStreamBridgeFacade facade = context.getBean(RuoyiStreamBridgeFacade.class);
                assertThat(facade.status())
                    .containsEntry("engine", "mock")
                    .containsEntry("state", "RUNNING");

                facade.engine().pull(PullRequest.builder()
                    .sourceUri("rtsp://example.test/live/camera-1")
                    .target(StreamKey.of("__defaultVhost__", "live", "camera-1"))
                    .build());

                assertThat(facade.streamRows()).hasSize(1);
                assertThat(facade.streamRows().get(0))
                    .containsEntry("virtualHost", "__defaultVhost__")
                    .containsEntry("application", "live")
                    .containsEntry("stream", "camera-1");
            });
    }
}
