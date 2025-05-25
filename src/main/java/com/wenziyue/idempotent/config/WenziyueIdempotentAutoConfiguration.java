package com.wenziyue.idempotent.config;


import com.wenziyue.idempotent.aspect.IdempotentAspect;
import com.wenziyue.idempotent.handler.DefaultRepeatSubmitHandler;
import com.wenziyue.idempotent.handler.RepeatSubmitHandler;
import com.wenziyue.redis.utils.RedisUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 幂等注解自动配置
 *
 * @author wenziyue
 */
@Configuration
@EnableConfigurationProperties(IdempotentProperties.class)
//@ConditionalOnClass(RedisUtils.class)
@ConditionalOnClass(name = "com.wenziyue.redis.utils.RedisUtils")
@ConditionalOnProperty(prefix = "wenziyue.idempotent", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WenziyueIdempotentAutoConfiguration {

    @Bean
    @ConditionalOnBean(RedisUtils.class)
    public IdempotentAspect idempotentAspect(RedisUtils redisUtils, IdempotentProperties idempotentProperties, ApplicationContext applicationContext) {
        return new IdempotentAspect(redisUtils, idempotentProperties, applicationContext);
    }

    @Bean
    @ConditionalOnBean
    public RepeatSubmitHandler repeatSubmitHandler() {
        return new DefaultRepeatSubmitHandler();
    }

    @Bean
    @ConditionalOnMissingBean(DefaultRepeatSubmitHandler.class)
    public DefaultRepeatSubmitHandler defaultRepeatSubmitHandler() {
        return new DefaultRepeatSubmitHandler();
    }
}
