# AI 项目索引（铁路购票系统 · railway-real）

> **这份文档的目标读者是 AI Agent**。如果你正在接手这个项目，第一件事是通读本文件。
> 它告诉你：项目是什么 / 已完成哪些阶段 / 代码放哪 / 关键约定 / 下一步该做什么。
> **不要凭直觉修改任何文件**——所有约定都在这里和 `开发文档.md` 里。

---

## 0. 一句话总结

- **栈**：Spring Boot 3.5.14 + Spring Cloud 2025.0.0 + Spring Cloud Alibaba 2025.0.0.0-preview + Java 21 + Maven 3.9.11。
- **ORM**：**原生 MyBatis**（Mapper 接口 + XML）+ PageHelper。**不用 MyBatis-Plus**。
- **架构**：9 模块（1 common + 1 gateway + 7 service-*），单用户本地 demo，不带 docker/k8s。
- **数据库**：`railway-real`（与项目 artifactId 同名），utf8mb4，端口 3306。
- **端口**：gateway `9000` / user `9101` / train-stock `9102` / ticket `9103` / payment `9104` / order `9105` / search `9200` / notification `9106`。
- **Nacos**：注册中心开，配置中心 demo 阶段**关闭**（`spring.cloud.nacos.config.enabled: false`），所有配置在本地 `application.yml`。
- **鉴权**：JWT(HS256, jjwt 0.12.6) + BCrypt(security-crypto) + Redis 登录态（key: `USER_LOGIN:{userId}`）。
- **库存原子性**：Redis + Lua 脚本（`occupy_stock.lua` / `release_stock.lua` / `idempotent_set.lua`）。

---

## 1. 必读文件（先看这几个）

| 文件 | 作用 | 行数 |
|---|---|---|
| `开发文档.md` | 阶段化开发计划（11 个阶段，逐阶段目标/文件/验证） | 933 |
| `项目结构.md` | 目录结构、每个服务放什么、与企业版的差异 | 396 |
| `pom.xml` | 父 POM：BOM 导入、`<properties>` 版本号、`<modules>`、pluginManagement | 167 |
| `db/init.sql` | `railway-real` 库所有表 + 3 辆示例车次 + 库存 | 217 |

> **不要读 `target/`**。构建产物，不入档。

---

## 2. 当前进度

| 阶段 | 模块 | 状态 | 入口 |
|---|---|---|---|
| 0 | 文档（开发文档 / 项目结构） | ✅ 完成 | `开发文档.md`, `项目结构.md` |
| 1 | 父 POM + 9 子模块 stub | ✅ 完成 | `pom.xml` |
| 2 | common（25 源文件 + 3 Lua + 11 单测） | ✅ 完成 | `common/pom.xml` |
| 3 | service-user（17 源文件） | ✅ 完成 | `service-user/pom.xml`, `UserApplication.java` |
| 4 | gateway（5 源文件，webflux 鉴权 + TraceId） | ✅ 完成 | `gateway/pom.xml`, `GatewayApplication.java` |
| 5 | service-train-stock（车次 + 库存 + Redis+Lua） | ⏳ **下一个** | dev 文档 §8 |
| 6 | service-ticket | ⏳ | dev 文档 §9 |
| 7 | service-payment | ⏳ | dev 文档 §10 |
| 8 | service-order（Feign 编排 + RabbitMQ 延迟） | ⏳ | dev 文档 §11 |
| 9 | service-notification（MQ 消费者） | ⏳ | dev 文档 §12 |
| 10 | service-search（ES 检索） | ⏳ | dev 文档 §13 |
| 11 | 联调 / 压测 | ⏳ | dev 文档 §14 |

**当前可构建**：common + gateway + service-user。
```bash
mvn -pl gateway -am clean package -DskipTests   # 82 MB fat jar
mvn -pl service-user -am clean package -DskipTests   # 79 MB fat jar
```

---

## 3. 模块坐标速查

```
groupId  : com.railway
version  : 1.0.0-SNAPSHOT
parent   : com.railway:railway-ticket:pom
子模块 artifactId：
  - common
  - gateway
  - service-user
  - service-train-stock
  - service-ticket
  - service-payment
  - service-order
  - service-search
  - service-notification
```

**包名规范**：每个业务模块用 `com.railway.<module-suffix>`，例如 `service-user` → `com.railway.user`，`service-train-stock` → `com.railway.trainstock`（**注意无连字符**）。

---

## 4. common 模块（**最重要，所有业务服务依赖它**）

> 路径：`common/src/main/java/com/railway/common/`
> 单一 jar，内部按包分类。`@AutoConfiguration` 通过 `META-INF/spring/...AutoConfiguration.imports` 注册。
> 顶层带 `@ConditionalOnWebApplication(SERVLET)` → **gateway（webflux）不会加载任何 common 自动配置**。

### 4.1 包结构与必知

| 包 | 关键文件 | 用途 |
|---|---|---|
| `model/` | `R.java`, `LoginUser.java`, `PageQuery.java` | 统一返回体 / 当前用户 / 分页入参 |
| `exception/` | `ErrorCode.java`, `BizException.java`, `GlobalExceptionHandler.java` | 27 个 ErrorCode 编码（1000~5099）+ `@ControllerAdvice` |
| `util/` | `JwtUtil.java`, `SnowflakeIdWorker.java`, `IdCardValidator.java`, `UserContext.java`, `RedisKeyBuilder.java` | 工具 |
| `annotation/` | `AuthIgnore.java`, `RequireRole.java` | `@AuthIgnore` 跳过鉴权；`@RequireRole("ADMIN")` 角色校验 |
| `aspect/` | `RequireRoleAspect.java` | 角色切面（@Aspect 自动织入） |
| `interceptor/` | `AuthInterceptor.java` | webmvc 拦截器，**优先读 X-User-* 头（网关透传），回退到 JWT 解析** |
| `constant/` | `HeaderConstant.java`, `RedisKeyConstant.java`, `MqConstant.java` | HTTP 头 / Redis key / MQ 路由 key |
| `config/` | `JacksonConfig`, `MyBatisConfig`, `RedisConfig`, `RedisLuaConfig`, `SecurityConfig`, `WebConfig` | 6 个 `@Configuration` |
| `auto/` | `WebAutoConfiguration.java` | 入口，`@Import` 6 个 config + 注册 JwtUtil/Snowflake/AuthInterceptor |

### 4.2 关键 Bean 装配

| Bean | 配置前缀 | 备注 |
|---|---|---|
| `JwtUtil` | `jwt.*` (secret, expire-minutes, issuer) | 所有服务共用，密钥必须一致 |
| `SnowflakeIdWorker` | `snowflake.worker-id` | **每个服务必须用不同 worker-id**（0~1023） |
| `PasswordEncoder` | — | BCrypt，固定 10 轮 |
| `PageInterceptor` | — | PageHelper 分页拦截器，`@MapperScan("com.railway.**.mapper")` |
| `RedisTemplate` / `StringRedisTemplate` | `spring.data.redis.*` | RedisTemplate 默认 JDK 序列化，StringRedisTemplate 字符串 |
| `DefaultRedisScript<Long>` × 3 | — | `lua/occupy_stock.lua` / `release_stock.lua` / `idempotent_set.lua` |

### 4.3 ErrorCode 编码区间

```
200 / 400 / 401 / 403 / 404 / 409 / 500   通用
1000-1099   业务通用
1100-1199   USER       (USER_ALREADY_EXISTS=1101, USER_PASSWORD_ERROR=1102, USER_DISABLED=1103)
1200-1299   PASSENGER  (PASSENGER_NOT_FOUND=1201)
1300-1399   TRAIN
2000-2099   STOCK
3000-3099   ORDER
4000-4099   PAY
5000-5099   TICKET
```

### 4.4 Lua 脚本

| 文件 | KEYS | ARGV | 返回 |
|---|---|---|---|
| `occupy_stock.lua` | `[stock_key]` | `[count]` | `1` 成功，`0` 失败 |
| `release_stock.lua` | `[stock_key]` | `[count]` | `1` 成功 |
| `idempotent_set.lua` | `[idempotent_key]` | `[ttl_seconds]` | `1` 首次，`0` 重复 |

调用模板（在 service-train-stock 阶段实现）：
```java
DefaultRedisScript<Long> script = redisLuaConfig.occupyStockScript();
Long ok = stringRedisTemplate.execute(script, List.of("STOCK:" + trainNo + ":" + date), "1");
```

---

## 5. gateway 模块

> 路径：`gateway/src/main/java/com/railway/gateway/`
> webflux，**不加载 common 的 servlet 拦截器/AOP**。
> 仅做：路由 + JWT 鉴权 + TraceId + CORS。

### 5.1 文件清单

| 文件 | 职责 |
|---|---|
| `GatewayApplication.java` | `@SpringBootApplication @EnableDiscoveryClient`，端口 9000 |
| `config/GatewayConfig.java` | 显式声明 `JwtUtil @Bean`（common 的 WebAutoConfiguration 不会加载） |
| `config/GatewayAuthProperties.java` | `@ConfigurationProperties("gateway")`，`publicPaths: List<String>` |
| `filter/AuthGlobalFilter.java` | `GlobalFilter`，白名单 → JWT 解析 → 注入 X-User-* 头 |
| `filter/TraceIdFilter.java` | `GlobalFilter`，生成/透传 X-Trace-Id |

### 5.2 路由表（`application.yml`）

| Predicate | Downstream URI | StripPrefix |
|---|---|---|
| `/api/user/**` | `lb://service-user` | 2 |
| `/api/train/**` | `lb://service-train-stock` | 2 |
| `/api/ticket/**` | `lb://service-ticket` | 2 |
| `/api/order/**` | `lb://service-order` | 2 |
| `/api/payment/**` | `lb://service-payment` | 2 |
| `/api/search/**` | `lb://service-search` | 2 |
| `/api/notification/**` | `lb://service-notification` | 2 |

### 5.3 鉴权 / TraceId 顺序

1. `TraceIdFilter` (order=HIGHEST_PRECEDENCE)：填 X-Trace-Id。
2. `AuthGlobalFilter` (order=HIGHEST_PRECEDENCE+10)：解析 JWT，注入 X-User-{Id,Name,Roles}。
3. 路由到下游。
4. 下游 `AuthInterceptor`（common）读 X-User-* 头 → 写 `UserContext` ThreadLocal。

### 5.4 白名单（`gateway.public-paths`）

```
/api/user/auth/login
/api/user/auth/register
/api/payment/callback/**
```

> **新增白名单 → 改 gateway 的 `application.yml`，不要写代码**。

---

## 6. service-user 模块

> 路径：`service-user/src/main/java/com/railway/user/`
> 端口 9101，MySQL `railway-real`，Redis 登录态，OpenFeign（暂未调用，预留）。

### 6.1 实体 / Mapper

| Entity | 表 | Mapper |
|---|---|---|
| `UserDO` | `user` | `UserMapper`（`countByUsername`, `selectByUsername`, `selectById`, `insert`, `updateById`, `deleteById`） |
| `PassengerDO` | `passenger` | `PassengerMapper`（`listByUserId`, `listByIds`, `clearDefaultByUserId`, 通用 CRUD） |

### 6.2 Controller 路径

| 类 | 路径 | 鉴权 |
|---|---|---|
| `AuthController` | `/auth/register`, `/auth/login`, `/auth/logout`, `/auth/me` | register/login `@AuthIgnore` |
| `PassengerController` | `/passengers`, `/passengers/{id}` | 全部需登录 |

### 6.3 服务实现要点

- `AuthServiceImpl.register`：BCrypt 密码 + 雪花 ID + `countByUsername` 查重。
- `AuthServiceImpl.login`：password 校验 → 构造 `LoginUser(userId, username, {USER, ADMIN?})` → `jwtUtil.generate` → Redis `USER_LOGIN:{userId}` 写 token（TTL=expire）。
- `PassengerServiceImpl.create`：自动绑 userId，`IdCardValidator.isValid` 校验，默认乘车人时先 `clearDefaultByUserId`。
- `PassengerServiceImpl.page`：`PageHelper.startPage` + `PageInfo<DO>` → `PageInfo<VO>`（VO 身份证脱敏 `IdCardValidator.mask`）。

---

## 7. 关键约定（**违反会被打回**）

### 7.1 MyBatis（**不要用 MyBatis-Plus**）

- 实体 `XxxDO` 字段名严格匹配 DB 列（`user_name` ↔ `userName`），**不加** `@TableName` 等 Plus 注解。
- Mapper 接口放 `com.railway.<module>.mapper`，**不加** `@MapperScan`（common 的 `MyBatisConfig` 已扫 `com.railway.**.mapper`）。
- XML 放 `src/main/resources/mapper/XxxMapper.xml`，命名空间 `com.railway.<module>.mapper.XxxMapper`。
- 动态 SQL 用 `<set>` / `<if>` / `<foreach>`，不在 Java 里拼字符串。
- 复杂查询分页：先 `PageHelper.startPage(pageNum, pageSize)`，再调用 mapper，**返回的 List 已被包装**，包 `new PageInfo<>(list)`。
- 通用 CRUD **不抽 BaseMapper**（dev 文档 §4.2 原则"复制即可，避免抽象过度"）。

### 7.2 Lombok

- 每个使用 Lombok 的模块 **必须** 在 pom.xml 显式声明 `<scope>provided</scope>` 的 lombok 依赖（**不向下传递**）。
- 业务模块切勿缺它，编译会失败但 mvn 不会清晰提示。

### 7.3 鉴权 / UserContext

- **永远不要在业务代码里 new LoginUser / set UserContext**。统一由 `AuthInterceptor` 处理。
- `UserContext.currentUserId()` 拿不到返回 `null`（**不抛**）；`mustCurrentUserId()` 拿不到抛 `UNAUTHORIZED`。
- 请求结束 `UserContext.clear()` 已被 `afterCompletion` 调用，**不要手动 clear**。

### 7.4 JWT

- 所有服务 `jwt.secret` / `jwt.expire-minutes` / `jwt.issuer` **必须完全一致**，否则跨服务 token 校验失败。
- 密钥 ≥ 32 字节（默认 `0123456789abcdef0123456789abcdef0123456789abcdef`，48 字节）。

### 7.5 雪花 ID

- `SnowflakeIdWorker` 1+41+10+12 布局（epoch 2023-11-14）。
- **每个进程/服务用不同 worker-id**（0~1023），生产用 Nacos 注入；demo 阶段在 yml 写死（gateway=0, user=1, train-stock=2, ...）。
- 时钟回拨抛 `SERVER_ERROR`（BizException）。

### 7.6 异常 / R

- 业务异常**只**用 `BizException(ErrorCode, msg?)`。
- 校验失败 `@Valid` 由 `GlobalExceptionHandler` 转 `R.fail(400, msg)`。
- 返回体统一 `R<T> = {code, message, data}`，**不要**直接返回 `Map` / 裸对象。

### 7.7 端口 / 应用名 / 路径

| 服务 | 端口 | spring.application.name |
|---|---|---|
| gateway | 9000 | `gateway` |
| service-user | 9101 | `service-user` |
| service-train-stock | 9102 | `service-train-stock` |
| service-ticket | 9103 | `service-ticket` |
| service-payment | 9104 | `service-payment` |
| service-order | 9105 | `service-order` |
| service-notification | 9106 | `service-notification` |
| service-search | 9200 | `service-search`（非常规，ES 单独端口） |

外部访问路径永远是 `http://localhost:9000/api/<svc>/<path>`，**不直连业务服务**。

### 7.8 包名映射（artifactId → 包前缀）

```
service-user         → com.railway.user
service-train-stock  → com.railway.trainstock      ← 注意无连字符
service-ticket       → com.railway.ticket
service-payment      → com.railway.payment
service-order        → com.railway.order
service-notification → com.railway.notification
service-search       → com.railway.search
```

### 7.9 不要做的事

- ❌ 引入 MyBatis-Plus。
- ❌ 引入 Sentinel / Sleuth / Swagger 聚合（dev 文档明确不要）。
- ❌ 引入 docker-compose / k8s 部署目录。
- ❌ 写 `manager/` 层（dev 文档 §4 标注"服务简单可省"）。
- ❌ 把 Feign 接口放 `api-*` 模块（统一放调用方 `feign/` 包）。
- ❌ 用 `@Value` 注入 jwt/snowflake，**统一**用 `@ConfigurationProperties` 绑定。
- ❌ 在 common 里写 webmvc 拦截器并期望 gateway 加载——webflux 不兼容。

---

## 8. 验证命令

```bash
# 编译全部（10s 级别）
mvn -pl common -am clean compile

# 打包单模块（不含测试）
mvn -pl service-user -am clean package -DskipTests

# 跑 common 单测
mvn -pl common test

# 启动 user（需要本机 MySQL/Redis/Nacos）
java -jar service-user/target/service-user-1.0.0-SNAPSHOT.jar
```

**预期输出**（user 启动成功）：`Tomcat started on port 9101 (http) with context path ''`

---

## 9. 接下来要做（**阶段 5：service-train-stock**）

参考 `开发文档.md` §8（line 502~），核心动作：

1. `service-train-stock/pom.xml`：copy `service-user` 的依赖 + 去掉 openfeign（暂不需要）+ 加 `mysql-connector-j`（其实 service-user 隐式带了）。
2. 实体 `TrainDO` / `TrainSeatStockDO` / `StockFlowDO`。
3. Mapper 三个 + XML。
4. `service/TrainService` + `impl/TrainServiceServiceImpl`（CRUD，车次查询带 `queryTrains(start,end,date)`）。
5. `service/StockService` + `impl/StockServiceImpl`：封装 `occupyStock(trainNo, date, seatType, count)` / `confirmStock(orderNo)` / `releaseStock(orderNo)` 三个方法，**核心是调 Lua + 写 stock_flow**。
6. `manager/StockManager.java`（**按 dev 文档要求保留**，封装 Redis Lua 调用）。
7. `controller/TrainController`（CRUD，admin `@RequireRole("ADMIN")`）+ `StockController`（占/确认/释放，**全部 `@AuthIgnore` 内部接口，仅 service-order 通过 Feign 调**）。
8. `application.yml`：port 9102，worker-id=2。
9. 写一张 `SeatController`（`GET /trains/{no}/seats`）。
10. 跑 `mvn -pl service-train-stock -am clean package -DskipTests`。

> **重要**：StockController 的 `/stock/**` 是服务间调用，**不经过 gateway**（`@AuthIgnore` + 内部 IP 白名单），demo 阶段省白名单只加 `@AuthIgnore`。

---

## 10. 关键文档锚点

| 想看什么 | 去哪 |
|---|---|
| 整体架构图 | `开发文档.md` line 22-50 |
| 阶段列表 | `开发文档.md` line 82-97 |
| 端口规划 | `开发文档.md` line 15 |
| DB schema | `db/init.sql` |
| 阶段 2 common 完整代码 | `开发文档.md` line 175-435 |
| 阶段 3 user 完整代码 | `开发文档.md` line 437-... |
| 阶段 4 gateway | `开发文档.md` line 438-500 |
| 阶段 5 train-stock | `开发文档.md` line 502-... |
| 阶段 8 order 编排 | `开发文档.md` line ~680-... |
| 雪花算法布局 | `开发文档.md` line ~244 + `common/src/main/java/.../SnowflakeIdWorker.java` |
| Lua 脚本全文 | `common/src/main/resources/lua/*.lua` |
| JWT 解析实现 | `common/src/main/java/com/railway/common/util/JwtUtil.java` |
| 鉴权拦截器实现 | `common/src/main/java/com/railway/common/interceptor/AuthInterceptor.java` |
| 网关鉴权 filter | `gateway/src/main/java/com/railway/gateway/filter/AuthGlobalFilter.java` |

---

## 11. 已知风险 / 待办

1. **GlobalExceptionHandler 注册问题**：common 的 `GlobalExceptionHandler` 是 `@ControllerAdvice` 普通类，**未被** `WebAutoConfiguration` 注册为 `@Bean`。依赖业务模块 `@SpringBootApplication` 默认扫描 `com.railway.user` 时能否扫到 `com.railway.common`？**当前未验证启动**。如启动报错，**两种修法**（任选）：
   - (a) 给每个 `XxxApplication` 加 `@ComponentScan({"com.railway.xxx", "com.railway.common"})`。
   - (b) 在 `WebAutoConfiguration` 加 `@Bean GlobalExceptionHandler globalExceptionHandler() { return new GlobalExceptionHandler(); }`。
2. **本机无中间件**：未启 MySQL/Redis/Nacos，所有服务只能编译，**不能 e2e**。要 e2e 请先在 `127.0.0.1` 装 MySQL 8 + Redis 7 + Nacos 2.x（8848/9848）。
3. **MySQL 客户端缺**：未跑过 `db/init.sql`，SQL 是人工审过。
4. **TrainSeatStock 表用乐观锁 `version` 但本阶段没用到**：阶段 5 写 StockService 时记得用。
5. **WebFlux + @ConfigurationProperties 顺序敏感**：common 的 `JwtUtil` bean 在 gateway 没注册，靠 gateway 自己的 `GatewayConfig` 显式补，**后续 webflux 工具类同样要在 gateway 加**。

---

**TL;DR**：
- 现在做完了 common + gateway + service-user 三块，能编译能打包，**没跑过**。
- 下一个任务：阶段 5 service-train-stock，按 `开发文档.md` §8 实施，**重点是 Redis+Lua 原子预占**。
- 别碰 Nacos 配置中心、别装 MyBatis-Plus、别动 common 的 servlet 拦截器。
