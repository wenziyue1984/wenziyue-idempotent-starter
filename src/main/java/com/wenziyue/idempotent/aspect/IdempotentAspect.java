package com.wenziyue.idempotent.aspect;


import com.wenziyue.idempotent.annotation.WenziyueIdempotent;
import com.wenziyue.idempotent.config.IdempotentProperties;
import com.wenziyue.idempotent.config.RepeatSubmitException;
import com.wenziyue.redis.utils.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

/**
 * @author wenziyue
 */
@Slf4j
@Aspect
@RequiredArgsConstructor
public class IdempotentAspect {

    private final RedisUtils redisUtils;
    private final IdempotentProperties properties;
    private final ApplicationContext applicationContext;
    private static final ExpressionParser SPEL_PARSER = new SpelExpressionParser();

    /**
     * 根据注解生成 key，检查 Redis 是否存在，如果存在则拒绝执行方法，否则写入 Redis 并执行方法。
     *
     * @param joinPoint ProceedingJoinPoint
     * @param wenziyueIdempotent WenziyueIdempotent
     * @return Object
     * @throws Throwable e
     */
    @Around("@annotation(wenziyueIdempotent)")
    public Object around(ProceedingJoinPoint joinPoint, WenziyueIdempotent wenziyueIdempotent) throws Throwable {
        // 没有开启幂等开关直接放行
        if (!properties.isEnabled()) {
            return joinPoint.proceed();
        }

        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] args = joinPoint.getArgs();

        // 1. 生成幂等 key
        String bizKey = buildKey(wenziyueIdempotent, args, paramNames);
        // 更稳健、更避免冲突（例如：idempotent:createOrder:blogUser:orderId123）
        String redisKey = String.join(":",
                wenziyueIdempotent.prefix(),
                method.getDeclaringClass().getSimpleName(),  // 类名
                method.getName(),
                bizKey
        );

        // 2. 检查 Redis 中是否存在
        if (redisUtils.hasKey(redisKey)) {
            log.warn("幂等检查失败：key={} 已存在", redisKey);
            log.debug("使用的重复处理器: {}", wenziyueIdempotent.handler().getSimpleName());
            applicationContext.getBean(wenziyueIdempotent.handler()).onRepeatSubmit(redisKey, joinPoint);
            return null; // 防止编译器报错，实际上上面可能已经 throw 了
        }

        // 3. 写入 Redis
        long timeout = wenziyueIdempotent.timeout() > 0 ? wenziyueIdempotent.timeout() : properties.getDefaultTimeout();
        redisUtils.set(redisKey, "1", timeout, TimeUnit.SECONDS);
        log.debug("幂等 key={} 已写入，超时时间={}秒", redisKey, timeout);

        // 4. 执行业务逻辑
        try {
            return joinPoint.proceed();
        } catch (Throwable ex) {
            // 如果业务异常，清除 Redis key（可选策略，默认true）
            if (wenziyueIdempotent.cleanOnError()) {
                redisUtils.delete(redisKey);
            }
            throw ex;
        } finally {
            // 方法执行完毕，清除 Redis key（可配置，默认false）
            if (wenziyueIdempotent.cleanOnFinish()) {
                redisUtils.delete(redisKey);
            }
        }
    }

    /**
     * 根据注解 + 方法参数构造 Redis Key，支持 SpEL 表达式
     */
    private String buildKey(WenziyueIdempotent anno, Object[] args, String[] paramNames) {
        String[] keys = anno.keys();
        if (keys.length != 0) {
            // 创建 SpEL 上下文
            StandardEvaluationContext context = new StandardEvaluationContext();
            for (int i = 0; i < paramNames.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }

            try {
                StringJoiner joiner = new StringJoiner(":");
                for (String expr : keys) {
                    Object value = SPEL_PARSER.parseExpression(expr).getValue(context);
                    joiner.add(String.valueOf(value));
                }
                return joiner.toString();
            } catch (Exception e) {
                log.error("SpEL 表达式解析失败: {}", keys, e);
                throw new RepeatSubmitException("幂等注解 SpEL 表达式解析失败: " + Arrays.toString(keys));
            }
        }
        // 如果没有指定 key，则默认使用 参数哈希
        log.warn("未指定 keys，自动使用参数哈希作为幂等 key。可能存在风险！");
        return String.valueOf(Arrays.hashCode(args));
    }
}
