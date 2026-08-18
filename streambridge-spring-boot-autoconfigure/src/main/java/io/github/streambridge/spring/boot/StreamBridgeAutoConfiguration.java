package io.github.streambridge.spring.boot;

import io.github.streambridge.api.EngineConfiguration;
import io.github.streambridge.api.StreamBridge;
import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.api.StreamEnginePlugin;
import io.github.streambridge.core.StreamBridgeBuilder;
import io.github.streambridge.core.StreamBridges;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(StreamBridge.class)
@ConditionalOnProperty(prefix = "streambridge", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(StreamBridgeProperties.class)
public class StreamBridgeAutoConfiguration {
    @Bean(destroyMethod = "close")
    @ConditionalOnMissingBean
    public StreamBridge streamBridge(
        StreamBridgeProperties properties,
        ObjectProvider<StreamEnginePlugin> pluginProvider
    ) {
        StreamBridgeBuilder builder = StreamBridges.builder().discoverPlugins(properties.isDiscoverPlugins());
        pluginProvider.orderedStream().forEach(builder::plugin);
        return builder.build();
    }

    @Bean
    @ConditionalOnMissingBean
    public StreamEngine streamEngine(StreamBridge bridge, StreamBridgeProperties properties) {
        EngineConfiguration configuration = EngineConfiguration.builder().options(properties.getOptions()).build();
        return bridge.create(properties.getEngine(), configuration);
    }

    @Bean
    @ConditionalOnMissingBean
    public StreamEngineLifecycle streamEngineLifecycle(StreamEngine streamEngine, StreamBridgeProperties properties) {
        return new StreamEngineLifecycle(streamEngine, properties.isAutoStart());
    }
}

