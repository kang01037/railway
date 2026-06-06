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
| 5 | service-train-stock（23 源文件，Redis+Lua 原子预占） | ✅ 完成 | `service-train-stock/pom.xml`, `TrainStockApplication.java` |
| 6 | service-ticket（13 源文件，票号 + 状态机） | ✅ 完成 | `service-ticket/pom.xml`, `TicketApplication.java` |
| 7 | service-payment（11 源文件，模拟支付 + MQ order.paid） | ✅ 完成 | `service-payment/pom.xml`, `PaymentApplication.java` |
| 8 | service-order（Feign 编排 + RabbitMQ 延迟关单 + 幂等键） | ✅ 完成（38 源文件，jar 81.5MB） | dev 文档 §11 |
| 9 | service-notification（MQ 消费者） | ⏳ **下一个** | dev 文档 §12 |
| 10 | service-search（ES 检索） | ⏳ | dev 文档 §13 |
| 11 | 联调 / 压测 | ⏳ | dev 文档 §14 |

**当前可构建**：common + gateway + service-user + service-train-stock + service-ticket + service-payment + service-order（7 个模块）。
```bash
mvn -pl common,gateway,service-user,service-train-stock,service-ticket,service-payment,service-order -am clean package -DskipTests
→ 7/7 BUILD SUCCESS
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

## 6b. service-train-stock 模块

> 路径：`service-train-stock/src/main/java/com/railway/trainstock/`
> 端口 9102，MySQL `railway-real`，Redis + Lua 原子预占。

### 6b.1 实体 / Mapper

| Entity | 表 | Mapper 关键方法 |
|---|---|---|
| `TrainDO` | `train` | `TrainMapper`（`listByCondition(start,end,status)`, `countRunningOnDate(trainNo,runDate)` 用 `WEEKDAY()` 切 run_days） |
| `TrainSeatStockDO` | `train_seat_stock` | `TrainSeatStockMapper`（`decreaseRemainByVersion` 乐观锁、`increaseRemainByVersion` 乐观锁、`listByTrainDate`） |
| `StockFlowDO` | `stock_flow` | `StockFlowMapper`（`listByOrderNo`） |

### 6b.2 Controller 路径

| 类 | 路径 | 鉴权 |
|---|---|---|
| `TrainController` | `/trains`, `/trains/{id}` | POST/PUT/DELETE `@RequireRole("ADMIN")`；GET 任意 |
| `StockController` | `/stock/occupy`, `/stock/confirm`, `/stock/release` | **全部 `@AuthIgnore`**（Feign 内部调用） |
| `SeatController` | `/trains/{trainNo}/seats?runDate=yyyy-MM-dd` | 任意 |

### 6b.3 服务 / Manager

| 类 | 职责 |
|---|---|
| `manager/StockManager` | **封装 Redis + Lua**：key 拼装、`ensureStockKey` 懒加载、`occupy` / `release` 调 Lua 脚本。业务层不直接碰 `StringRedisTemplate` 和 Lua。 |
| `service/TrainService` | 车次 CRUD + 座位库存查询；新增时同一事务内插 `train` + 多行 `train_seat_stock` |
| `service/StockService` | occupy / confirm / release 三件事：Redis 原子 → 写 stock_flow → DB 乐观锁扣/回 |

### 6b.4 库存原子性流程（详见 §7.9）

```
occupy:
  1. 懒加载 Redis key (SETNX + 1d TTL)
  2. Lua occupy_stock: 1 成功 / 0 库存不足 / -1 key 不存在
  3. 写 stock_flow (bizType=1, delta=-num)
  4. DB decreaseRemainByVersion (version 一致 & remain >= num)
     失败 → 回滚 Redis + 抛 STOCK_VERSION_CONFLICT
release:
  1. 懒加载 Redis key
  2. Lua release_stock (幂等: key 不存在视为成功)
  3. 写 stock_flow (bizType=3, delta=+num)
  4. DB increaseRemainByVersion
confirm:
  1. 写 stock_flow (bizType=2, delta=-num)，不再扣
```

---

## 6c. service-ticket 模块

> 路径：`service-ticket/src/main/java/com/railway/ticket/`
> 端口 9103。被 service-order 通过 Feign 调用，**用户也能经 gateway 查自己订单的票**。

### 6c.1 实体 / Mapper

| Entity | 表 | Mapper 关键方法 |
|---|---|---|
| `TicketDO` | `ticket` | `TicketMapper`（`selectByTicketNo`, `listByOrderNo`, `countByOrderNo`, **`confirmByOrderNo` SQL 0→1**, **`cancelByOrderNo` SQL 0/1→3**） |

### 6c.2 状态机

```
0-待支付 ──confirm──▶ 1-已出票 ──cancel──▶ 3-已退
   │
   └──cancel──▶ 3-已退
```

- `confirmByOrderNo` 的 SQL 带 `WHERE status = 0` —— 重复 confirm 0 行，影响 0 行（幂等）。
- `cancelByOrderNo` 的 SQL 带 `WHERE status IN (0,1)` —— 只退未改签/未退的票；2-已改签 不在此处理。
- `status = 2 (已改签)` 暂未实现流程，SQL 不动它。

### 6c.3 Controller 路径

| 端点 | 方法 | 鉴权 | 用途 |
|---|---|---|---|
| `POST /tickets/issue` | `issue(IssueDTO)` | `@AuthIgnore` | service-order 调（Feign） |
| `POST /tickets/confirm` | `confirm(ConfirmDTO)` | `@AuthIgnore` | 支付回调后调 |
| `POST /tickets/cancel` | `cancel(CancelDTO)` | `@AuthIgnore` | 取消订单时调 |
| `GET  /tickets/order/{orderNo}` | `listByOrderNo` | 需登录 | 用户查订单的票 |
| `GET  /tickets/{ticketNo}` | `getByTicketNo` | 需登录 | 查单张票 |

### 6c.4 关键服务 / Manager

| 类 | 职责 |
|---|---|
| `manager/SeatPoolManager` | **Redis Set `SEAT_POOL:{trainNo}:{date}:{seatType}`**：`initIfAbsent` 懒加载（用 SADD 1..total 初始化 + 1d TTL）；`pop` 用 SPOP 拿一个座位号；`push` 退回座位号（cancel 时调用）。 |
| `service/TicketService.issue` | **幂等**：`countByOrderNo > 0` 直接返回已存在的票号；否则循环生成 N 张票（`passengers.size()`），每张用雪花 ID 作 `ticketNo`（`T` 前缀），从座位池 SPOP 选座，写库 status=0。 |
| `service/TicketService.confirm` | `UPDATE WHERE status=0`，**0 行不影响**（幂等）。 |
| `service/TicketService.cancel` | `UPDATE WHERE status IN (0,1)`，然后遍历 `listByOrderNo` 把新退的票座位 `push` 回池。 |

### 6c.5 Redis Key 命名

- `SEAT_POOL:{trainNo}:{runDate}:{seatType}` → SET（座位号 `1` `2` `3` ... `total`）
- 座位号 demo 阶段是纯数字串，carriageNo 固定 1；后续要细化"1A/1B/多车厢"改 `SeatPoolManager` 一处即可。

---

## 6d. service-payment 模块

> 路径：`service-payment/src/main/java/com/railway/payment/`
> 端口 9104。**只发不收** MQ（`order.paid`）；消费侧在 service-order。

### 6d.1 实体 / Mapper

| Entity | 表 | Mapper 关键方法 |
|---|---|---|
| `PayRecordDO` | `pay_record` | `PayRecordMapper`（`selectByPayNo`, `selectByOrderNo`, `listByOrderNo`, **`updateStatusFromPending` SQL 强制从 status=0 改起**，返回 0 = 幂等命中） |

### 6d.2 状态机

```
0-待支付 ──callback(success=true)──▶ 1-成功
   │
   └──callback(success=false)──▶ 2-失败
```
- 状态机 0 → 1/2 在 SQL 里 `WHERE status=0`，已处理过则 0 行影响 → **幂等**。
- 0 → 3（已关闭）暂未实现，service-order 关闭订单时再补。
- 1/2 终态，**不可再 callback**（SQL 直接 noop）。

### 6d.3 Controller 路径

| 端点 | 方法 | 鉴权 | 用途 |
|---|---|---|---|
| `POST /pay/create` | `createPay(CreatePayDTO)` | 需登录 | 用户下单后调，返回 payUrl |
| `POST /pay/callback/sim` | `callback(CallbackDTO)` | `@AuthIgnore` | 模拟支付回调（**gateway 白名单 `/api/payment/callback/**`**） |
| `GET /pay/{payNo}` | `getByPayNo` | 需登录 | 查单笔支付单 |
| `GET /pay/order/{orderNo}` | `listByOrderNo` | 需登录 | 查订单的所有支付单 |

### 6d.4 关键流程

**createPay**：
1. 雪花生成 `payNo = "P" + snowflakeId`
2. 写 `pay_record` (status=0)
3. 返回 `payUrl = pay.callbackBaseUrl + "/sim?payNo=" + payNo`（默认 `http://localhost:9000/api/payment/callback/sim?payNo=...`）

**callback**（**幂等**）：
1. `UPDATE pay_record SET status=?, paid_time=NOW() WHERE pay_no=? AND status=0`
2. affected=0 → 已处理过，返回 `false`（no MQ）
3. affected=1 → 发 MQ `order.exchange / order.paid`（body: `{orderNo, payNo, amount, success}`）

### 6d.5 RabbitMQ 配置

- `RabbitConfig`（service-payment 自己声明，**消费侧 service-order 也会重复声明 → RabbitAdmin 幂等 noop**）
  - `orderExchange`: TopicExchange
  - `orderPaidQueue`: Queue
  - `orderPaidBinding`: 绑 RK `order.paid`
  - `RabbitTemplate`: Jackson2JsonMessageConverter
- **本服务不订阅任何队列**（避免循环）

### 6d.6 配置项

```yaml
spring.rabbitmq.host: 127.0.0.1
spring.rabbitmq.port: 5672
spring.rabbitmq.username/password: guest
pay.callback-base-url: http://localhost:9000/api/payment/callback
snowflake.worker-id: 4
```

---

## 7. 关键约定（**违反会被打回**）

### 7.1 MyBatis（**不要用 MyBatis-Plus**）

- 实体 `XxxDO` 字段名严格匹配 DB 列（`user_name` ↔ `userName`），**不加** `@TableName` 等 Plus 注解。
- Mapper 接口放 `com.railway.<module>.mapper`，**不加** `@MapperScan`（common 的 `MyBatisConfig` 已扫 `com.railway.**.mapper`）。
- XML 放 `src/main/resources/mapper/XxxMapper.xml`，命名空间 `com.railway.<module>.mapper.XxxMapper`。
- 动态 SQL 用 `<set>` / `<if>` / `<foreach>`，不在 Java 里拼字符串。
- 复杂查询分页：先 `PageHelper.startPage(pageNum, pageSize)`，再调用 mapper，**返回的 List 已被包装**，包 `new PageInfo<>(list)`。
- 通用 CRUD **不抽 BaseMapper**（dev 文档 §4.2 原则"复制即可，避免抽象过度"）。
- **乐观锁** 模板（service-train-stock 已实现，参考 TrainSeatStockMapper.xml）：
  ```xml
  <update id="decreaseRemainByVersion">
      UPDATE train_seat_stock
      SET remain = remain - #{num}, version = version + 1
      WHERE train_no = #{trainNo} AND run_date = #{runDate} AND seat_type = #{seatType}
        AND version = #{version} AND remain &gt;= #{num}
  </update>
  ```
  Service 层先 `selectByKey` 拿 `version` 再 update，**返回 0 行**即冲突 → 抛 `STOCK_VERSION_CONFLICT`。
  Redis 侧的"原子性"由 Lua 提供；DB 侧的"原子性"由 `version` 乐观锁提供。**二者缺一不可**。

### 7.2 Lombok

- 每个使用 Lombok 的模块 **必须** 在 pom.xml 显式声明 `<scope>provided</scope>` 的 lombok 依赖（**不向下传递**）。
- 业务模块切勿缺它，编译会失败但 mvn 不会清晰提示。

### 7.3 鉴权 / UserContext

- **永远不要在业务代码里 new LoginUser / set UserContext**。统一由 `AuthInterceptor` 处理。
- `UserContext.currentUserId()` 拿不到返回 `null`（**不抛**）；`mustCurrentUserId()` 拿不到抛 `UNAUTHORIZED`。
- 请求结束 `UserContext.clear()` 已被 `afterCompletion` 调用，**不要手动 clear**。
- **服务间内部接口**（如 `service-order` 调 `service-train-stock` 的 `/stock/occupy`）：**加 `@AuthIgnore`**。因为是 Feign 直连不经过 gateway 鉴权链，AuthInterceptor 也读不到 X-User-* 头，反而会因为没 token 抛 401。

### 7.4 JWT

- 所有服务 `jwt.secret` / `jwt.expire-minutes` / `jwt.issuer` **必须完全一致**，否则跨服务 token 校验失败。
- 密钥 ≥ 32 字节（默认 `0123456789abcdef0123456789abcdef0123456789abcdef`，48 字节）。

### 7.5 雪花 ID

- `SnowflakeIdWorker` 1+41+10+12 布局（epoch 2023-11-14）。
- **每个进程/服务用不同 worker-id**（0~1023），生产用 Nacos 注入；demo 阶段在 yml 写死：

| 服务 | worker-id |
|---|---|
| gateway | 0 |
| service-user | 1 |
| service-train-stock | 2 |
| service-ticket | 3 |
| service-payment | 4 |
| service-order | 5 |
| service-notification | 6 |
| service-search | 7 |

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
（服务间 Feign 调用直连业务端口，**不走 gateway**，加 `@AuthIgnore`。）

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

### 7.9 库存原子性模板（**核心，service-order 也要按此调**）

**Redis Key**：`STOCK:{trainNo}:{runDate}:{seatType}`，TTL 1 天，懒加载。
**Lua**：`occupy_stock.lua` 返回 1/0/-1，**调用方**：
```
ensureStockKey(...)     // 懒加载（DB → Redis SETNX）
luaResult = occupy(...)  // -1 报错 STOCK_NOT_FOUND
                          //  0 报错 STOCK_NOT_ENOUGH
                          //  1 继续
saveFlow(bizType=1, delta=-num)
decreaseRemainByVersion(...)  // DB 乐观锁
// 失败：回滚 Redis + 抛 STOCK_VERSION_CONFLICT
```
**`release`**：Lua INCRBY（幂等：key 不存在视为成功）+ saveFlow(bizType=3, delta=+num) + increaseRemainByVersion。
**`confirm`**：仅 saveFlow(bizType=2, delta=-num)，Redis/DB 已在 occupy 阶段扣减，不重复操作。

### 7.10 不要做的事

- ❌ 引入 MyBatis-Plus。
- ❌ 引入 Sentinel / Sleuth / Swagger 聚合（dev 文档明确不要）。
- ❌ 引入 docker-compose / k8s 部署目录。
- ❌ 把 Feign 接口放 `api-*` 模块（统一放调用方 `feign/` 包）。
- ❌ 用 `@Value` 注入 jwt/snowflake，**统一**用 `@ConfigurationProperties` 绑定。
- ❌ 在 common 里写 webmvc 拦截器并期望 gateway 加载——webflux 不兼容。
- ❌ 在 service-order 等服务里**直接**调 mapper 写 stock_flow——统一经 StockService。
- ❌ 删 train 时**级联删库存**：演示阶段 deleteTrain 只打日志（生产应加事务删 stock + train）。

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

## 8e. service-order（已实现 / Stage 8）

> 路径 `service-order/src/main/java/com/railway/order/`，包名 `com.railway.order`，**port 9105, snowflake worker-id=5**。

### 8e.1 实体 + Mapper（状态机 SQL）
- `OrderDO`：表 `orders`（9 字段：id / orderNo "O"+雪花 / userId / trainNo / runDate / seatType / num / amount DECIMAL(10,2) / status / expireTime / createTime / updateTime）。
- `OrderMapper.xml` 状态机 SQL 模板：`UPDATE orders SET status=?, update_time=NOW() WHERE order_no=? AND status IN (...)`：
  - `confirmByOrderNo` 0→1
  - `cancelByOrderNo` 0→2
  - `refundByOrderNo` 0/1→3（关单 / 自动关单）
  - `completeByOrderNo` 1→4
- 全部幂等：affected=0 = 状态不对或已处理，**不抛异常**（消费者场景）。

### 8e.2 Feign 编排（4 客户端 + 4 Fallback）
- 路径 `feign/`：`StockFeignClient` (occupy/confirm/release) / `TicketFeignClient` (issue/confirm/cancel) / `PaymentFeignClient` (createPay) / `UserFeignClient` (listByIds 拿乘客信息)。
- 每个 `*FeignClientFallback` 返 `R.fail(SERVER_ERROR, "服务暂不可用")`。
- 所有 Feign DTO（`OccupyStockDTO` / `ConfirmStockDTO` / `ReleaseStockDTO` / `IssueTicketDTO` / `PassengerVO` / `PayVO` / `TicketVO` ...）在 `feign/dto/`，**不复用内部 VO/DO**，仅按 JSON 契约对得齐。
- Manager 层（`OrderStockManager` / `OrderTicketManager` / `OrderPayManager` / `OrderUserManager`）翻译 `R` → `BizException`（STOCK_NOT_ENOUGH / TICKET_ISSUE_FAILED / PAY_FAILED）。

### 8e.3 RabbitMQ 拓扑（**延迟关单核心**）
```
send ─▶ orderDelayExchange(Direct) ─order.delay─▶ orderDelayQueue
                                                      │ TTL=15min
                                                      │ (无消费者)
                                                      ▼ DLX
                                              orderExchange(Topic) ─order.cancel─▶ orderCancelQueue
                                                                                            │
                                                                                            ▼
                                                                       OrderDelayCancelConsumer
payment.callback ─order.paid─▶ orderExchange ─▶ orderPaidQueue
                                                    │
                                                    ▼
                                         OrderPaidConsumer
```
- 关键实现 `RabbitConfig`：
  - `orderDelayQueue`：`x-message-ttl=900_000` + `x-dead-letter-exchange=orderExchange` + `x-dead-letter-routing-key=order.cancel`。
  - 发延迟消息 `convertAndSend(orderDelayExchange, RK_ORDER_DELAY, {orderNo}, m -> { m.getMessageProperties().setExpiration("900000"); return m; })`。
- **消费者**：
  - `OrderPaidConsumer`：`affected=0` 跳过（幂等）；成功后调 `stock.confirm` + `ticket.confirm`（warn-only 失败，不回滚订单状态）。
  - `OrderDelayCancelConsumer`：仅当 `status=0` 时执行；状态机 0→3（refundByOrderNo）+ `stock.release` + `ticket.cancel`。

### 8e.4 幂等切面（**local 暂存 service-order**）
- `aspect/Idempotent` + `aspect/IdempotentAspect`：读 `X-Idempotent-Key` 头 → Lua `idempotent_set.lua` SETNX → 失败抛 `IDEMPOTENT_REPEAT`。
- `key` 命名：`IDEMPOTENT:{key}`（来自 `RedisKeyConstant.IDEMPOTENT`），TTL 默认 60s。
- **生产化建议**：抽到 common，与 `RequireRoleAspect` 一起。common 已有 `idempotent_set.lua` 脚本可直接复用。

### 8e.5 OrderService.create 主流程（**带补偿**）
```
1. 校验 num ≤ 5 + userId
2. 雪花生 orderNo
3. userManager.listByIds(passengerIds)            // 容错：拿不到给 defaultPassenger
4. stockManager.occupy(...)                        // 失败 → STOCK_NOT_ENOUGH
5. insertOrder(status=0, expireTime=now+15min)     // 失败 → catch safeRelease
6. sendDelayMessage(orderNo)                       // warn-only 失败
7. ticketManager.issue(...)                        // 失败 → safeRelease + 改 status=3 + TICKET_ISSUE_FAILED
8. payManager.createPay(...)                       // warn-only 失败：保留 order，前端可重试
9. 返回 {orderNo, payUrl, amount, expireTime}
```

### 8e.6 Controller（4 endpoint）
| 方法 | 路径 | 鉴权 | 幂等 | 备注 |
|---|---|---|---|---|
| POST | `/orders` | JWT | `@Idempotent(60s)` | 客户端必传 `X-Idempotent-Key` |
| POST | `/orders/{orderNo}/cancel` | JWT | — | 仅 owner + status=0 |
| GET | `/orders/{orderNo}` | JWT | — | 返回 detail（order + 票列表，本期票列表留空） |
| GET | `/orders` | JWT | — | 当前用户订单分页（PageHelper） |

---

## 9. 接下来要做（**阶段 9：service-notification**）

参考 `开发文档.md` §12。核心目标：**纯 MQ 消费者**，无 HTTP Controller。

预期动作：
1. `service-notification/pom.xml`：minimal 依赖（spring-boot-starter-amqp + web 兜底 actuator + lombok + common），**无需 openfeign / nacos-config / mysql / redis / 雪花**。
2. `NotificationApplication`（port 9106，worker-id=6，可省雪花）。
3. **消费者 2 个**（`@RabbitListener` 监听 `orderExchange`）：
   - `OrderPaidNotificationConsumer` 收 `order.paid` → 模拟发短信/邮件 → 落 `notification_log` 表（本地 MySQL，简化版）。
   - `OrderCancelNotificationConsumer` 收 `order.cancel` → 模拟通知用户订单已取消。
4. `RabbitConfig`（声明 `orderExchange` 监听相关队列；不需重声明延迟队列）。
5. `NotificationLogDO` + `NotificationLogMapper`（最小表：id, orderNo, type 0-支付成功 1-订单取消, content, status 0-成功 1-失败, retry_count, create_time）。
6. 跑 `mvn -pl service-notification -am clean package -DskipTests` → 8/8 BUILD SUCCESS。

> **提示**：
> - 演示阶段 notification 的下游 channel（短信/邮件）只打 log，**不接真服务**。
> - DB 仍用 `railway-real`，**notification_log** 表由 SQL 增量新增（`db/init.sql` 末尾 ALTER 或新脚本 `db/notification_log.sql`）。
> - consumer 失败策略：retry 3 次后进死信，或简单 log 后丢弃。开发文档未明，本期沿用 `service-payment` 的 `simple-retry 3x` + warn-only。

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
| 阶段 5 train-stock | `开发文档.md` line 502-641 |
| 阶段 6 ticket | `开发文档.md` line 644-683 |
| 阶段 7 payment | `开发文档.md` line 685-715 |
| 阶段 8 order 编排 | `开发文档.md` line 718-... |
| 雪花算法布局 | `开发文档.md` line ~244 + `common/src/main/java/.../SnowflakeIdWorker.java` |
| Lua 脚本全文 | `common/src/main/resources/lua/*.lua` |
| JWT 解析实现 | `common/src/main/java/com/railway/common/util/JwtUtil.java` |
| 鉴权拦截器实现 | `common/src/main/java/com/railway/common/interceptor/AuthInterceptor.java` |
| 网关鉴权 filter | `gateway/src/main/java/com/railway/gateway/filter/AuthGlobalFilter.java` |
| 库存原子性流程 | `service-train-stock/src/main/java/.../service/impl/StockServiceImpl.java` |
| StockManager 封装 | `service-train-stock/src/main/java/.../manager/StockManager.java` |
| 票状态机 SQL | `service-ticket/src/main/resources/mapper/TicketMapper.xml` |
| 座位池 SPOP | `service-ticket/src/main/java/.../manager/SeatPoolManager.java` |
| 支付状态机 SQL | `service-payment/src/main/resources/mapper/PayRecordMapper.xml` |
| MQ 配置（生产侧） | `service-payment/src/main/java/.../config/RabbitConfig.java` |
| MqConstant 路由 Key | `common/src/main/java/com/railway/common/constant/MqConstant.java` |

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
- 现在做完了 common + gateway + service-user + service-train-stock + service-ticket + service-payment + service-order 七块，能编译能打包，**没跑过**。
- 下一个任务：**阶段 9 service-notification**，按 `开发文档.md` §12 实施，**重点是纯 MQ 消费 + 通知日志表**。
- 别碰 Nacos 配置中心、别装 MyBatis-Plus、别动 common 的 servlet 拦截器。
