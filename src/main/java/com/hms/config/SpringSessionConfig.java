package com.hms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * SpringSessionConfig – activates Spring Session with Amazon ElastiCache for
 * Redis as the distributed session store (cr-java-0065).
 *
 * Spring Session intercepts every HttpSession call made by the application's
 * servlets and transparently stores/retrieves session data in Redis instead of
 * in-process server memory.  This eliminates server affinity, prevents data
 * loss on instance termination, and enables safe horizontal scaling behind a
 * load balancer.
 *
 * Configuration is driven entirely by environment variables so that no
 * credentials or endpoint addresses are hard-coded in source:
 *
 *   REDIS_HOST            – ElastiCache primary endpoint hostname
 *                           (default: localhost for local development)
 *   REDIS_PORT            – ElastiCache port
 *                           (default: 6379)
 *   SESSION_TIMEOUT_SECS  – Maximum inactive interval in seconds
 *                           (default: 1800 = 30 minutes)
 *
 * The maxInactiveIntervalInSeconds attribute on @EnableRedisHttpSession
 * controls the TTL of session keys in Redis.
 */
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class SpringSessionConfig {

    /** ElastiCache primary endpoint – injected from environment variable REDIS_HOST. */
    @Value("${REDIS_HOST:localhost}")
    private String redisHost;

    /** ElastiCache port – injected from environment variable REDIS_PORT. */
    @Value("${REDIS_PORT:6379}")
    private int redisPort;

    /**
     * Lettuce-based Redis connection factory pointing at the ElastiCache
     * cluster.  Lettuce is the recommended non-blocking Redis client for
     * Spring Session on AWS.
     *
     * @return configured {@link LettuceConnectionFactory}
     */
    @Bean
    public LettuceConnectionFactory connectionFactory() {
        RedisStandaloneConfiguration redisConfig =
                new RedisStandaloneConfiguration(redisHost, redisPort);
        return new LettuceConnectionFactory(redisConfig);
    }
}
