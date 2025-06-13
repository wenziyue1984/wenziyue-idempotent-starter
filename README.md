# wenziyue-idempotent-starter 



一个基于 **Spring AOP + Redis** 的轻量级幂等控制组件



------



## ✨ 功能特性

1. @WenziyueIdempotent 一行注解即可完成幂等控制
2. **多 SpEL key 组合**：keys={"#dto.userId", "#dto.orderId"}
3. Redis 写入 + 过期控制，超时自动释放
4. **策略模式**：重复请求的处理逻辑可自定义
   - 默认策略：抛 RepeatSubmitException
   - 通过 handler = XxxHandler.class 插拔式替换
5. 全局开关 / 默认过期时间 / 业务异常是否清除 key 都可在 application.yml 配置
6. 仅在检测到项目中 **存在 RedisUtils Bean** 时才会自动装配，非侵入
7. 依赖 wenziyue-redis-starter 采用 optional=true，版本由使用者自行决定



------



## ⚙️ 快速开始



### 1. 引入依赖

首先在settings.xml中添加以下认证信息
```xml
<server>
    <id>wenziyue-idempotent</id>
    <username>你的GitHub用户名</username>
    <password>你的GitHub Token（建议只赋予 read:packages 权限）</password>
</server>
```

再在 `pom.xml` 中添加 GitHub 仓库地址：

```xml
<!-- pom.xml 中添加仓库地址（id 要与上面保持一致） -->
<repositories>
    <repository>
        <id>wenziyue-idempotent</id>
        <url>https://maven.pkg.github.com/wenziyue1984/wenziyue-idempotent-starter</url>
    </repository>
</repositories>
```
然后引入依赖：

```xml
<!-- 幂等 Starter -->
<dependency>
    <groupId>com.wenziyue</groupId>
    <artifactId>wenziyue-idempotent-starter</artifactId>
    <version>1.0.0(请用最新版本)</version>
</dependency>

<!-- 必须显式引入 Redis Starter（版本自定）-->
<dependency>
    <groupId>com.wenziyue</groupId>
    <artifactId>wenziyue-redis-starter</artifactId>
    <version>1.0.3</version>
</dependency>
```

> idempotent-starter 内部声明 redis-starter 为 optional，因此 **不会强行传递**，需自行引入并保证版本兼容。



------





### 2. 配置（可选）

```yaml
wenziyue:
  idempotent:
    idempotentEnabled: true # 是否启用幂等控制（默认 true）
    default-timeout: 60    # 默认过期秒数（默认60s）
```



------



### 3. 使用幂等注解@WenziyueIdempotent

```java
@PostMapping("/order")
@WenziyueIdempotent(
        prefix = "idempotent",			  // 幂等键前缀，用于构建 Redis key，默认idempotent
        keys = {"#dto.userId", "#dto.orderId"},   // 多字段组合
        timeout = 30,                             // 覆盖默认超时
        cleanOnFinish = true,                     // 是否在方法执行完成后清理幂等键，默认为 false
        cleanOnError = true                       // 是否在方法执行出错时清理幂等键，默认为 true
)
public String createOrder(@RequestBody OrderDTO dto) {
    return "下单成功：" + dto.getOrderId();
}
```

若cleanOnFinish=false，30 秒内相同 userId + orderId 组合的请求将被拦截。

若cleanOnFinish=true，方法执行完成后，幂等键将自动被清理，下次请求将正常执行，类似于分布式锁。



生成的 Redis key 类似：

```tex
idempotent:OrderController:createOrder:10086:O20250101
```


自定义重复提交处理策略（可选）
```java
@Component
public class FailResultHandler implements RepeatSubmitHandler {
    @Override
    public Object onRepeatSubmit(String redisKey, ProceedingJoinPoint ctx) {
        return CommonResult.fail("重复请求，请稍后再试");
    }
}
```

使用：

```java
@WenziyueIdempotent(keys="#request.token", handler = FailResultHandler.class)
```

------



## ⚠️ 注意事项

1. 请确保项目中已正确配置 Redis 并能连接成功；
2. 若 keys 留空将使用 **参数哈希** 作为幂等 key，可能存在风险，应尽量显式配置；
3. 同一接口若对不同业务维度有多种幂等规则，可通过不同方法或自行封装网关层解决；
4. 若使用者引入了与 starter 不兼容的 redis-starter 版本，启动时会因 Bean 缺失报错。

------


## 📚 未来规划

- 幂等日志/Audit 落库与可视化
- 网关级通用幂等过滤器
- 支持 Lua 脚本 + Redis 原子锁实现无 AOP 场景
- 基于消息 ID 的消费幂等示例

------

