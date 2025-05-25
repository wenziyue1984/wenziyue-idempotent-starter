package com.wenziyue.idempotent.handler;

import com.wenziyue.idempotent.config.RepeatSubmitException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;

/**
 * @author wenziyue
 */
@Slf4j
public class DefaultRepeatSubmitHandler implements RepeatSubmitHandler{
    @Override
    public Object onRepeatSubmit(String redisKey, ProceedingJoinPoint context) {
        log.warn("默认重复提交处理策略触发：{}", redisKey);
        throw new RepeatSubmitException("幂等方法重复提交: " + redisKey);
    }
}
