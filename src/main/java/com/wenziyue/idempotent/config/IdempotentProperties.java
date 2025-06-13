package com.wenziyue.idempotent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author wenziyue
 */
@Data
@ConfigurationProperties(prefix = "wenziyue.idempotent")
public class IdempotentProperties {

    /**
     * 是否启用幂等功能
     */
    private boolean enabled = true;

    /**
     * 默认幂等超时时间（秒）
     */
    private long defaultTimeout = 60;
}
