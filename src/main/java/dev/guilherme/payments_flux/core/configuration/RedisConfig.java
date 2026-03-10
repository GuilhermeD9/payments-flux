package dev.guilherme.payments_flux.core.configuration;

import dev.guilherme.payments_flux.api.dto.TransferDTO;
import dev.guilherme.payments_flux.api.dto.WalletDTO;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;


@Configuration
@EnableCaching
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {

        JacksonJsonRedisSerializer<WalletDTO.Response> walletSerializer = new JacksonJsonRedisSerializer<>(WalletDTO.Response.class);
        JacksonJsonRedisSerializer<TransferDTO.Response> transferSerializer = new JacksonJsonRedisSerializer<>(TransferDTO.Response.class);

        Map<String, RedisCacheConfiguration> cacheConfig = new HashMap<>();

        cacheConfig.put("wallet-cache",
                RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(walletSerializer)));

        cacheConfig.put("transfer-cache",
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofMinutes(15))
                        .disableCachingNullValues()
                        .serializeValuesWith(RedisSerializationContext.SerializationPair
                                .fromSerializer(transferSerializer)));

        return RedisCacheManager
                .builder(connectionFactory)
                .withInitialCacheConfigurations(cacheConfig)
                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig())
                .build();
    }
}
