package io.github.streambridge.ruoyi;

import io.github.streambridge.api.StreamEngine;
import io.github.streambridge.spring.boot.StreamBridgeAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = StreamBridgeAutoConfiguration.class)
@ConditionalOnClass(name = "com.ruoyi.RuoYiApplication")
@ConditionalOnBean(StreamEngine.class)
@ConditionalOnProperty(prefix = "streambridge.ruoyi", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RuoyiStreamBridgeAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public RuoyiStreamBridgeFacade ruoyiStreamBridgeFacade(StreamEngine engine) {
        return new RuoyiStreamBridgeFacade(engine);
    }
}

