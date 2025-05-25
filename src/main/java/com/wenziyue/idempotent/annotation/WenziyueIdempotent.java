package com.wenziyue.idempotent.annotation;

import com.wenziyue.idempotent.handler.DefaultRepeatSubmitHandler;
import com.wenziyue.idempotent.handler.RepeatSubmitHandler;

import java.lang.annotation.*;

/**
 * @author wenziyue
 */

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface WenziyueIdempotent {
    /**
     * 幂等键前缀，用于构建 Redis key
     */
    String prefix() default "idempotent";

    /**
     * 幂等键 EL 表达式（如 keys = {"#dto.userId", "#dto.orderId"}）
     */
    String[] keys() default {};

    /**
     * 幂等键的过期时间（单位：秒）
     */
    long timeout() default -1;

    /**
     * 重复提交处理器
     */
    Class<? extends RepeatSubmitHandler> handler() default DefaultRepeatSubmitHandler.class;
}
