package com.wenziyue.idempotent.handler;

import org.aspectj.lang.ProceedingJoinPoint;

/**
 * 重复提交处理器
 *
 * @author wenziyue
 */
@FunctionalInterface
public interface RepeatSubmitHandler {

    /**
     * 处理重复提交的行为
     *
     * @param redisKey 当前请求的幂等键
     * @param context  当前方法执行上下文（可以是 ProceedingJoinPoint、方法、参数等）
     */
    Object onRepeatSubmit(String redisKey, ProceedingJoinPoint context) throws Throwable;
}