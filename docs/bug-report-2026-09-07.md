# 秒杀系统 Bug 排查报告

- **排查日期**：2026-09-07
- **排查范围**：seckill-api / seckill-business / seckill-job 三个模块核心代码（登录鉴权、商品、下单、支付、退款、订单超时、库存一致性、MQ 消费者、过滤器、配置）
- **统计**：共记录 34 个问题 —— P0 致命 4 个、P1 高 8 个、P2 中 10 个、P3 低 12 个
- **严重级别定义**：
  - **P0 致命**：系统无法启动 / 核心链路（登录、下单）完全不可用 / 编译不通过
  - **P1 高**：核心业务出错（超卖、库存泄漏、资金/状态不一致）、关键配置错误
  - **P2 中**：特定场景下的业务错误、安全漏洞、可复现的数据问题
  - **P3 低**：代码质量、健壮性、潜在风险

---

## 一、P0 致命

### BUG-01 登录后所有鉴权请求全部失效：token 被用户 JSON 覆盖

- **位置**：`seckill-business/src/main/java/cn/net/zhu/seckill/business/helper/UserTokenHelper.java:72-73`
- **现象**：用户登录接口返回 token，但携带 token 访问任何需要登录的接口，`UserContext` 中拿不到用户（业务报"请先登录"）。
- **根因**：
  ```java
  redisUtil.set(getTokenKey(username), token, tokenExpireTimeInRecord); // 写 token
  redisUtil.set(getTokenKey(username), json, tokenExpireTimeInRecord);  // ← 同一个 key 又被用户 JSON 覆盖！
  ```
  第二行把用户 JSON 写进了 `token:username` 这个 key，把刚写入的 token 覆盖掉了；而 `user:username`（getUserKey）从未被写入。
  `JwtTokenFilter:56-63` 从 `token:username` 读出来的是用户 JSON，与请求头 token 比对永远不匹配，认证失败后 filter 静默放行（见 BUG-16），`UserContext` 无用户。
- **修复方案**：第二行改为写入用户 key：
  ```java
  redisUtil.set(getTokenKey(username), token, tokenExpireTimeInRecord);
  redisUtil.set(getUserKey(username), json, tokenExpireTimeInRecord);   // 修正：用户 JSON 写入 user:username
  ```
  **自检**：登录成功后检查 Redis 里 `token:xxx` 的值应该是 JWT 字符串，`user:xxx` 是用户 JSON。

### BUG-02 api 模块扫描不到 business 包的所有 Bean，启动即失败

- **位置**：`seckill-api/src/main/java/cn/net/zhu/seckill/api/ApiApplication.java:15`
- **现象**：IDEA 报 `Could not autowire. No beans of 'UserService' type found`；启动报 `No qualifying bean of type 'cn.net.zhu.seckill.business.service.UserService'`。所有 `cn.net.zhu.seckill.business` 下的 @Service/@Component/@Configuration 均无法注入（UserServiceImpl、ProductServiceImpl、RedisUtil、SeckillUserServiceImpl 等）。
- **根因**：`@SpringBootApplication` 默认只扫描启动类所在包 `cn.net.zhu.seckill.api` 及其子包；`cn.net.zhu.seckill.business` 是兄弟包，不在扫描范围。项目里没有任何 `@ComponentScan`/`@Import` 补救。`JobApplication` 已正确处理（`@ComponentScan(basePackages = "cn.net.zhu.seckill")`），api 模块遗漏。
- **修复方案**（二选一）：
  ```java
  @SpringBootApplication(scanBasePackages = "cn.net.zhu.seckill")
  // 或
  @ComponentScan(basePackages = "cn.net.zhu.seckill")
  ```

### BUG-03 订单 insert SQL 语法错误：VALUES 块写成了 `列名 = 参数` 形式，下单必失败

- **位置**：`seckill-business/src/main/resources/cn/net/zhu/seckill/business/mapper/seckill/SeckillOrderTradeMapper.xml:341-417`
- **现象**：消费者执行 `createOrder` 插入订单时抛 SQLSyntaxErrorException，MQ 无限重试，秒杀下单链路完全断裂。
- **根因**：`<insert id="insert">` 的 `values (...)` 块里从 `trade_id` 开始，全部写成了 `trade_id = #{tradeId},` 这种 SET 语法：
  ```xml
  <trim prefix="values (" suffix=")" suffixOverrides=",">
      <if test="id != null">#{id},</if>
      <if test="tradeId != null">trade_id = #{tradeId},</if>   ← 错误，应为 #{tradeId},
      <if test="code != null and code != ''">code = #{code},</if> ← 错误，应为 #{code},
      ...（往下全部同错）
  ```
  生成的 SQL 形如 `INSERT INTO seckill_order_trade (...) values (#{id}, trade_id = #{tradeId}, ...)`，非法 SQL。
- **修复方案**：把 values 块里所有 `列名 = #{参数},` 改成 `#{参数},`（与列名块一一对应）。同时把 `userGeneratedKeys="true"` 拼写错误改为 `useGeneratedKeys="true"`（本项目用雪花 ID 手动 set，其实可删掉这两个属性，见 BUG-25）。
- **自检**：打开 MyBatis SQL 日志，跑一次 createOrder，确认 INSERT 语句合法且受影响行数为 1。

### BUG-04 编译错误集合：EsBaseEntity 无 id 字段 + setRemainSeconds 类型不匹配

- **位置**：
  - `seckill-business/src/main/java/cn/net/zhu/seckill/business/entity/EsBaseEntity.java:12`（空类）
  - `seckill-business/src/main/java/cn/net/zhu/seckill/business/service/impl/ProductServiceImpl.java:168、198、202、206`
- **现象**：
  1. `Cannot resolve method 'setId' in 'SeckillProductDetailPageEntity'` 与 `Cannot resolve method 'getId' in 'EsSeckillProductEntity'`（168 行同一条语句两边标红）；
  2. `setRemainSeconds (Integer) cannot be applied to (long)`（198/202/206 行）。
- **根因**：
  1. 继承链 `SeckillProductDetailPageEntity → SeckillProductDetailEntity → EsSeckillProductEntity → EsBaseEntity` 上没有任何类声明 `id` 字段，`EsBaseEntity` 是空类且无 `@Data`；
  2. `remainSeconds` 字段声明为 `Integer`，而 `DateUtil.between(...)` 返回 `long`，`0L` 也是 long。
- **修复方案**：
  1. 给 `EsBaseEntity` 加 `@Data` 和 `private Long id;`；
  2. `remainSeconds` 字段类型改为 `Long`（`SeckillProductDetailPageEntity.java:21`）。
  - **注意**：加完 id 字段后 168 行仍要改（见 BUG-33）——ES 的 `_id` 不在 `_source` 里，`esProduct.getId()` 永远是 null。

---

## 二、P1 高

### BUG-05 退款"一单多退"编译错误：Service 返回 List，Mapper 返回单条

- **位置**：
  - `seckill-business/src/main/java/cn/net/zhu/seckill/business/service/impl/SeckillRefundServiceImpl.java:224`
  - `seckill-business/src/main/java/cn/net/zhu/seckill/business/mapper/seckill/SeckillRefundMapper.java:37`
- **现象**：`Incompatible types. Found: 'SeckillRefundEntity', required: 'java.util.List<SeckillRefundEntity>'`，编译不过。
- **根因**：业务设计是"一单多退款"（Service 接口 `getRefundsByOrderCode` 返回 List），但 Mapper 的 `findByOrderCode` 返回单个实体，语义是"最近一条退款"；Impl 硬接两者，类型爆炸。
- **修复方案**：
  1. `SeckillRefundMapper.findByOrderCode` 返回类型改为 `List<SeckillRefundEntity>`（XML 不用动，MyBatis 按接口返回类型自动封装多行；0 行返回空 List 不会 NPE）；
  2. `SeckillRefundServiceImpl.applyRefund` 第 74-77 行的防重复校验改为遍历：
     ```java
     List<SeckillRefundEntity> existingList = seckillRefundMapper.findByOrderCode(orderCode);
     boolean processing = existingList.stream()
             .anyMatch(r -> Objects.equals(r.getRefundStatus(), 1) || Objects.equals(r.getRefundStatus(), 2));
     if (processing) throw new RuntimeException("该订单已有退货申请在处理中");
     ```
  3. 顺带把 75 行 `==` 改成 `Objects.equals`（见 BUG-23）。

### BUG-06 下单不检查 DB 扣库存结果：DB 层库存不足仍创建订单（超卖）

- **位置**：`seckill-business/src/main/java/cn/net/zhu/seckill/business/service/impl/UserSeckillProductServiceImpl.java:301-328`
- **现象**：Redis 预扣与 DB 预扣脱节时（如 Redis 有库存但 DB `with_hold_quantity=0`），`reduceWithHoldStock` 受影响行数为 0，代码不检查，继续插入订单 → DB 层超卖。
- **根因**：`seckillProductMapper.reduceWithHoldStock(seckillProductId)` 的返回值被忽略（XML 里有 `with_hold_quantity > 0` 保护，扣不到会返回 0 行，但没人看）。
- **修复方案**：
  ```java
  int affected = seckillProductMapper.reduceWithHoldStock(seckillProductId);
  if (affected <= 0) {
      throw new BusinessException("商品库存不足");
  }
  ```

### BUG-07 DB 库存扣减/恢复硬编码 1，quantity 参数在 DB 层丢失

- **位置**：
  - `seckill-business/src/main/java/cn/net/zhu/seckill/business/mapper/seckill/SeckillProductMapper.java:44、52`
  - `seckill-business/src/main/resources/cn/net/zhu/seckill/business/mapper/seckill/SeckillProductMapper.xml:46-59`
- **现象**：`StockConsistencyServiceImpl.reduceStock(id, quantity)` / `restoreStock(id, quantity)` 传了数量，但 DB 层 SQL 写死 `- 1` / `+ 1`。数量 >1 时 DB 与 Redis 变更不一致；多订单并发恢复会错乱。
- **根因**：Mapper 方法签名只有 id，没有 quantity 参数。
- **修复方案**：
  1. Mapper 加参数：`int reduceWithHoldStock(Long id, Integer quantity)` / `int restoreWithHoldStock(Long id, Integer quantity)`；
  2. XML 改为 `with_hold_quantity = with_hold_quantity - #{quantity}` / `+ #{quantity}`（保留 `> 0` 乐观保护）；
  3. 调用方 `StockConsistencyServiceImpl` 传入 quantity。
  - 当前 order.quantity 恒为 1 时暂不暴露，但属于隐藏雷，务必随 BUG-06 一起修。

### BUG-08 job 模块 Redis 配置错误：秒杀进度写入 Redis DB0，api 读 DB5，前端永远看不到进度

- **位置**：`seckill-job/src/main/resources/application.yml`
- **现象**：消费者创建订单过程中写入的秒杀进度状态（`seckillProcessStatus:*`）和用户已购标记（`userSeckillProduct:*`）落在 Redis **DB 0**；api 模块配置 `spring.redis.database: 5`，前端轮询从 DB 5 读取——永远查不到进度，秒杀结果轮询失效。
- **根因**：job 的 yml 没有配 `spring.redis`，走默认 localhost:6379 DB 0；同时 `spring.application.name` 复制成了 `seckill-api`（应为 `seckill-job`）。
- **修复方案**：job 的 application.yml 补上与 api 一致的 Redis 配置：
  ```yaml
  spring:
    application:
      name: seckill-job
    redis:
      database: 5
      host: 127.0.0.1
      port: 6379
  ```

### BUG-09 商品详情缓存了时间敏感字段：倒计时冻结 5 分钟

- **位置**：`seckill-business/src/main/java/cn/net/zhu/seckill/business/service/impl/ProductServiceImpl.java:137-151`
- **现象**：`getProductDetail` 把组装好 `remainSeconds`/`seckillStatus` 的整页 VO 写入 Redis 缓存 5 分钟。同一商品第二个请求起，倒计时不再走，秒杀状态停留在缓存那一刻（秒杀开始了页面还显示"未开始"）。
- **根因**：缓存了"当前时刻的计算结果"。时间相关字段不可缓存。
- **修复方案**（二选一）：
  1. 只缓存 ES 原始数据（`SeckillProductDetailEntity`），每次请求实时调 `calcSecondAndStatus` 组装页面状态；
  2. 保持现状但命中缓存后重新计算 `remainSeconds`/`seckillStatus` 再返回。
- **自检**：两个请求间隔 10 秒，`remainSeconds` 应相差约 10。

### BUG-10 MQ 消费无幂等：消息重复投递会重复创建订单

- **位置**：
  - `seckill-job/src/main/java/cn/net/zhu/seckill/job/consumer/UserSeckillProductConsumer.java:47`
  - `seckill-business/src/main/java/cn/net/zhu/seckill/business/service/impl/UserSeckillProductServiceImpl.java:284-340`
- **现象**：RocketMQ 重试（消费超时、异常重推）场景下，同一消息被重复消费 → `createOrder` 重复执行 → 同一用户同一商品生成多条订单。
- **根因**：`createOrder` 没有以 msgId/业务唯一键做幂等拦截；消费者还把所有异常都抛出去触发重试（见 BUG-22），放大了重复消费概率。
- **修复方案**：
  1. 消费入口用 `message.getMsgId()` 做幂等 key：`SETNX mqIdempotent:msgId 1`（带过期时间），已存在直接 return 不执行业务；
  2. 业务层兜底：`createOrder` 开头查 `userSeckillProduct:{productId}_{userName}` 已购标记，已存在直接返回。

### BUG-11 退款成功后不回滚库存（Redis + DB），库存永久泄漏

- **位置**：`seckill-business/src/main/java/cn/net/zhu/seckill/business/service/impl/SeckillRefundServiceImpl.java:150-205`
- **现象**：退款成功只更新退款单、支付流水、订单状态，**没有调用 `StockConsistencyService.restoreStock`**。退掉的商品库存不返还，多退几次库存直接耗尽。
- **根因**：`processRefund` 流程漏了库存恢复步骤（代码自评第 315 行也自己点出了这个问题）。
- **修复方案**：在 `processRefund` 退款成功（状态置 5）之后调用：
  ```java
  stockConsistencyService.restoreStock(refund.getSeckillProductId(), refund.getQuantity());
  ```
  （需在 SeckillRefundServiceImpl 注入 StockConsistencyService；注意 refund 实体要带 quantity 字段，若没有则从订单取 `order.getQuantity()`，退款场景按全额退恢复全部数量。）
  - 同时处理 BUG-20 的幂等问题，防止重复恢复。

### BUG-12 订单超时时间硬编码 15 分钟，与消息体 timeoutMinutes 脱节

- **位置**：`seckill-business/src/main/java/cn/net/zhu/seckill/business/service/impl/OrderTimeoutServiceImpl.java:116-123`
- **现象**：生产者按 `timeoutMinutes` 计算延时等级并随消息下发，消费端却硬编码 `15 * 60 * 1000` 判断是否真超时。业务把超时改成 30 分钟后，消费端仍按 15 分钟关单（提前关单）。
- **根因**：`handleOrderTimeout(String orderCode)` 只接收 orderCode，没接收 timeoutMinutes（消费者 `OrderTimeoutConsumer.java:47` 也只传了 orderCode）。
- **修复方案**：
  1. `handleOrderTimeout` 增加 `Integer timeoutMinutes` 入参，时间校验用 `timeoutMinutes * 60 * 1000L`；
  2. `OrderTimeoutConsumer` 把 `timeoutMessage.getTimeoutMinutes()` 一并传入。

---

## 三、P2 中

### BUG-13 MQ 发送失败回滚了 Redis 库存，但没回滚用户当日秒杀次数

- **位置**：`UserSeckillProductServiceImpl.java:176-188`
- **现象**：MQ 投递失败时库存 `increment(stockKey)` 回滚，但 `incrementUserSeckillCount`（178 行，在 try 内）已经把用户当日次数 +1 且未回滚。用户没抢到东西却白白消耗 1/10 的每日名额。
- **修复方案**：catch 块里对当日计数 `decrement`（或增加 `redisUtil.decrement(countKey)` 方法）；更好的做法是调整顺序：MQ 发送成功后再计数。

### BUG-14 LOCAL_LOW_STOCK 售罄标记永久不清理

- **位置**：`UserSeckillProductServiceImpl.java:120、146-148、155、171`
- **现象**：某商品售罄后 JVM 本地标记置 true，此后**永不删除**。补货后该实例仍直接拦截所有请求，直到重启。多实例部署时各实例标记不同步。
- **修复方案**：
  1. 恢复库存（restoreStock/syncStock）时清除本地标记，提供 `LOCAL_LOW_STOCK.remove(productId)` 的清理入口；
  2. 或给标记加时间戳，超过 N 秒自动失效（简单实现：`Map<Long, Long> productId -> 标记时间`，读取时判超时）。

### BUG-15 每日秒杀次数"先查后加"非原子，并发可超限

- **位置**：`UserSeckillProductServiceImpl.java:138、178、195-215`
- **现象**：`checkUserSeckillCount` 读计数判上限，`incrementUserSeckillCount` 再 +1，两步之间无原子性。同一用户并发提交多次请求可突破 10 次/日限制。
- **修复方案**：用 `RedisUtil.increment` 的返回值直接判断：
  ```java
  Long count = redisUtil.increment(countKey);   // 原子 +1 并返回新值
  if (count > 10) { redisUtil.decrement(countKey); throw new BusinessException("您今天已参与10次秒杀"); }
  ```
  同时初次 +1 时需要设置 24h 过期（increment 对不存在的 key 会从 0 开始，需在 key 不存在时补 set 过期时间）。

### BUG-16 JwtTokenFilter 认证失败静默放行 + catch 内二次 doFilter 风险

- **位置**：`seckill-business/src/main/java/cn/net/zhu/seckill/business/filter/JwtTokenFilter.java:75-83`
- **现象**：
  1. token 无效/过期/不匹配时只打日志然后**放行**，接口内部拿不到用户上下文——若某个受保护接口忘了做 `UserContext` 判空，就会以未登录状态执行敏感操作（依赖每个接口自觉判断，非常危险）；
  2. 若下游 filter/controller 在 `filterChain.doFilter`（74 行）抛出异常，catch 块会**再次调用 doFilter**，造成请求被处理两次或抛 IllegalStateException。
- **修复方案**：
  1. 认证失败直接返回 401 JSON，不再放行（公开接口通过白名单路径放行，而不是"无 token 才放行"）；
  2. catch 块中不再重复调用 doFilter，改为响应 401 后 return。

### BUG-17 createOrder 用户不存在时 NPE

- **位置**：`UserSeckillProductServiceImpl.java:305-312`
- **现象**：`seckillUserMapper.findByUsername(userName)` 返回 null（用户被删/数据异常）时，312 行 `user.getId()` 抛 NPE → 消费者无限重试。
- **修复方案**：
  ```java
  if (Objects.isNull(user)) throw new BusinessException("用户不存在");
  ```

### BUG-18 auditRefund 事务内调用 processRefund：退款失败连审核记录一起回滚

- **位置**：`SeckillRefundServiceImpl.java:115-140`
- **现象**：`auditRefund` 与 `processRefund` 同处一个 `@Transactional`，`processRefund` 抛异常会导致审核操作整体回滚——"审核通过"这个事实丢失，管理员需要重新审核。
- **修复方案**：审核落库与退款执行拆开：auditRefund 提交事务后，通过 MQ/异步任务触发 processRefund（退款执行做成独立事务 + 幂等 + 失败重试）。

### BUG-19 支付回调无幂等：重复回调重复处理

- **位置**：`SeckillPaymentServiceImpl.java:93-131`
- **现象**：第三方支付平台重复回调时，`thirdPartyTransactionNo` 无唯一约束，无"已处理"判断，重复执行支付成功流程（重复更新订单、重复记账）。
- **修复方案**：`seckill_payment.third_party_transaction_no` 建唯一索引；processPayment 入口先按 thirdPartyTransactionNo 查重，已处理直接返回 true。

### BUG-20 restoreStock 无幂等，且 DB 层忽略 quantity

- **位置**：`StockConsistencyServiceImpl.java:57-93`
- **现象**：退款路径、超时路径若被重复触发（消息重试、接口重入），DB 库存被重复恢复 → 库存虚增超卖。quantity 参数只作用于 Redis，DB 层固定 +1（同 BUG-07）。
- **修复方案**：引入库存操作记录表（订单号+商品ID+操作类型唯一），restoreStock 先查记录，已恢复直接返回；DB 恢复带 quantity（配合 BUG-07 修改）。

### BUG-21 cancelOrder 不回滚库存

- **位置**：`SeckillOrderTradeServiceImpl.java:104-129`
- **现象**：用户取消订单只改订单状态，预扣库存不释放。与 BUG-11 同类：任何取消/退款出口都必须恢复库存。
- **修复方案**：`cancelOrder` 内订单状态更新成功后调用 `stockConsistencyService.restoreStock(order.getSeckillProductId(), order.getQuantity())`。

### BUG-22 消费者业务异常一律抛 RuntimeException 触发重试

- **位置**：
  - `UserSeckillProductConsumer.java:48-52`
  - `OrderTimeoutConsumer.java:48-52`
- **现象**：业务类异常（库存不足、订单不存在、已支付、JSON 脏数据）也抛出异常触发 MQ 重试 → 无效重试消耗资源；脏消息会一直重试到上限进死信，形成"毒消息"循环。
- **修复方案**：区分异常：
  - 系统异常（DB/Redis 连不上）→ 抛出触发重试；
  - 业务异常 → catch BusinessException 打日志后正常返回（消费成功），不再重试；
  - 反序列化失败的脏消息 → 打日志直接消费成功丢弃（或转人工队列）。

---

## 四、P3 低

### BUG-23 `==` 比较 Integer

- **位置**：`SeckillRefundServiceImpl.java:75、131、136`；`SeckillOrderTradeServiceImpl.java:115`
- **现象**：`existing.getRefundStatus() == 1`、`auditResult == 2`、`order.getOrderStatus() != 1` 等用 `==` 比较 Integer 包装类型，值超出 -128~127 缓存范围时比较失败（当前值 1~6 恰好都在缓存内所以暂时没炸，属于定时炸弹）。
- **修复方案**：全部改为 `Objects.equals(x, y)` 或拆箱前判空。

### BUG-24 垃圾 import

- **位置**：
  - `SeckillProductMapper.java:5`：`import org.apache.xmlbeans.impl.xb.xmlconfig.Extensionconfig;`（xmlbeans 与业务无关，靠 easyexcel 传递依赖才能编译通过）
  - `StockConsistencyConfig.java:3`：`import org.checkerframework.checker.units.qual.C;`（靠 caffeine 传递依赖）
- **修复方案**：删除这两行无用 import。传递依赖一旦变化，这两处会直接变成编译错误。

### BUG-25 `userGeneratedKeys` 拼写错误

- **位置**：`SeckillOrderTradeMapper.xml:262`
- **现象**：MyBatis 不认识 `userGeneratedKeys` 属性（正确写法 `useGeneratedKeys`），该属性被静默忽略。由于本项目用雪花 ID 手动 `setId`，直接删掉 `keyProperty`/`userGeneratedKeys` 即可。

### BUG-26 登录成功未更新 lastLoginTime

- **位置**：`UserServiceImpl.java:116-152`
- **现象**：`SeckillUserService.updateLastLoginTime(id)` 方法存在但登录流程从未调用，`last_login_time` 永远为 null/首次值。
- **修复方案**：login 成功后调用 `seckillUserService.updateLastLoginTime(userEntity.getId())`（注意 Login 读的是 Redis 缓存，需保证 UserEntity.id 存在）。

### BUG-27 注册手机验证码 mock 规则是安全漏洞

- **位置**：`SeckillUserServiceImpl.java:140-150`
- **现象**：`mockCode = phone.substring(phone.length() - 4)` —— 验证码 = 手机号后 4 位，任何人知道手机号即可"验证通过"，形同虚设。注册接口无真实短信验证。
- **修复方案**：接入真实短信服务；至少改为服务端生成的随机验证码 + Redis 过期 + 发送短信。

### BUG-28 秒杀接口收了验证码参数但从不校验

- **位置**：`UserSeckillProductServiceImpl.java:128-189`；`UserSeckillProductEntity.java:21-23`
- **现象**：`UserSeckillProductEntity` 带 `uuid`/`code` 两个 `@NotNull` 字段（排队页 `/queue` 也传了），但 `doSeckillProduct` 全程没有调用 `UserService.checkCode`。脚本只要登录即可绕过图形验证码直接刷秒杀接口。
- **修复方案**：doSeckillProduct 开头调用 `userService.checkCode(entity.getUuid(), entity.getCode())`（用完删除，UserServiceImpl 已有现成方法）。

### BUG-29 JWT 密钥默认值硬编码

- **位置**：`UserTokenHelper.java:48`
- **现象**：`@Value("${mall.mgt.tokenSecret:123456test}")` —— 不配置时用弱密钥 "123456test"，可被离线伪造任意用户 token。
- **修复方案**：配置文件强制配置强随机密钥；代码默认值移除或改为启动时无配置直接 fail-fast。

### BUG-30 getResultWithProgress 解析无容错

- **位置**：`UserSeckillProductServiceImpl.java:254-258`
- **现象**：Redis 值格式异常（脏数据/手动改错）时 `parts[1]` 越界、`Integer.parseInt` 抛 NumberFormatException；状态消息含冒号会被截断。
- **修复方案**：解析前判 `parts.length`，包 try-catch，异常时返回"处理中"兜底；消息拼接改用 JSON 存储。

### BUG-31 两套用户上下文并存，语义不一致

- **位置**：`UserContext.java`（TransmittableThreadLocal）与 `FillUserUtil.java`（SecurityContextHolder）
- **现象**：鉴权链路写的是 UserContext，而 FillUserUtil.fillCreateUserInfo/fillUpdateUserInfo 读的是 SecurityContextHolder（永远拿到 anonymousUser）——两套上下文并存，一旦混用填充逻辑会静默写错创建人/修改人。
- **修复方案**：统一上下文机制。建议 FillUserUtil 改为从 `UserContext.getCurrentUser()` 取值；或删掉 FillUserUtil 中 SecurityContextHolder 系列方法，避免误用。

### BUG-32 calcSecondAndStatus NPE 风险

- **位置**：`ProductServiceImpl.java:193-206`
- **现象**：ES 文档缺 startTime/endTime 字段时，`detail.getStartTime().after(now)` 直接 NPE，接口 500。
- **修复方案**：判空兜底（null 视为状态未知返回默认 0），或 ES 写入侧保证字段必填。

### BUG-33 esProduct.getId() 语义错误：_id 不在 _source 中

- **位置**：`ProductServiceImpl.java:164-168`
- **现象**：即使按 BUG-04 给实体补了 id 字段，`JSON.parseObject(response.getSourceAsString(), ...)` 也反序列化不出 `_id`（ES 文档 `_id` 是元数据，不在 _source JSON 里），`esProduct.getId()` 恒为 null。
- **修复方案**：
  ```java
  detail.setId(Long.parseLong(response.getId()));
  ```
  列表接口 `searchProductList`（112-114 行）同理，如需 id 要取 `hit.getId()` 再 set。

### BUG-34 未显式配置 RocketMQ name-server

- **位置**：`seckill-api/src/main/resources/application.yml`、`seckill-job/src/main/resources/application.yml`
- **现象**：两个模块都没有 `rocketmq.name-server` 配置，依赖 starter 默认值 `127.0.0.1:9876`。MQ 不在本机或端口非默认时生产/消费全部失败，且报错不直观。
- **修复方案**：显式配置 `rocketmq.name-server: ${MQ_HOST}:9876` 并支持环境变量覆盖。

---

## 五、排查中已验证的"非 bug"（避免误修）

1. **验证码 `getCode()` 写法正确**（`UserServiceImpl.java:160-170`）：通过反编译 easy-captcha-1.6.2 字节码确认，`ArithmeticCaptcha.text()` 返回的是**纯数字结果**（如 "7"，`chars` 字段），`getArithmeticString()` 才返回表达式 "3+4=?"。`Double.parseDouble(captcha.text())` 不会抛异常，图片上画的表达式与 Redis 存的答案匹配，逻辑成立。
2. **Mapper XML 无需 mapper-locations 配置**：XML 文件路径与 Mapper 接口全限定名完全一致（`cn/net/zhu/seckill/business/mapper/**/XxxMapper.xml`），MyBatis 会按接口全限定名自动加载同名 XML，不需要 `mybatis.mapper-locations`。已反编译 mybatis-spring-boot-autoconfigure-2.3.2 确认默认不加载 `mapper/**/*.xml` 不影响本项目。

---

## 六、修复优先级建议

| 批次 | 内容 | 理由 |
|---|---|---|
| 第一批（立即） | BUG-01、02、03、04 | 不修则系统编译不过 / 启动失败 / 登录与下单全断 |
| 第二批 | BUG-05、06、07、08、09 | 核心业务正确性：一单多退、防超卖、进度轮询、倒计时 |
| 第三批 | BUG-10、11、12、13、14、15 | 幂等与数据一致性、限购准确 |
| 第四批 | BUG-16 ~ 22 | 安全加固、消费者健壮性 |
| 第五批 | BUG-23 ~ 34 | 代码质量与潜在风险清理 |

**修复后验证清单**：
1. 两个模块都能编译通过（mvn clean compile）；
2. api 启动不报 NoSuchBeanDefinition，swagger 可访问；
3. 注册 → 登录 → 带 token 访问 /getUserInfo 能拿到用户；
4. 秒杀下单全链路：进队列 → 消费者建单成功 → 订单表有数据 → 前端轮询进度走到 100%；
5. 库存：下单后 DB with_hold_quantity -1、Redis 同步 -1；订单超时/退款后两边恢复；
6. 倒计时：详情接口两次调用 remainSeconds 递减；
7. 一单多退：同一订单多次申请退款，getRefundsByOrderCode 返回全部记录。
