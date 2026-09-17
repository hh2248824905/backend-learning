# 08 定时任务

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\08-schedule`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8008`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 每天凌晨跑统计，怎么让它自动跑？cron 表达式怎么写？
2. `fixedRate` 和 `fixedDelay` 有什么区别？任务耗时超过间隔会怎样？
3. 定时任务跑得慢，怎么让它不阻塞其他任务？
4. 部署了 3 个实例，任务会不会跑 3 遍？怎么保证只跑一次？
5. 任务执行失败了，怎么重试？怎么知道它失败了？

## 1. 模块定位

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 07 消息队列：实时异步 | **08：按时间点执行 + 失败了能补偿** | 09 安全（清理过期 token）、运维（数据归档） |

**07 和 08 的分工**：

| 维度 | 07 消息队列 | 08 定时任务 |
|---|---|---|
| 触发方式 | 事件驱动（有消息就处理） | 时间驱动（到点就处理） |
| 实时性 | 高（毫秒级） | 低（按周期） |
| 主要用途 | 业务解耦、削峰 | 批量计算、清理、补偿 |

## 2. 计划覆盖的知识点

### 2.1 开启定时任务

```java
@SpringBootApplication
@EnableScheduling              // ① 必须开启，否则 @Scheduled 完全不生效
public class ScheduleApplication { }
```

```java
@Component
@Slf4j
public class DataSyncTask {

    @Scheduled(cron = "0 0 2 * * ?")
    public void syncDaily() {
        log.info("开始执行每日数据同步任务");
    }
}
```

::: danger 忘了 `@EnableScheduling` 是最常见的坑
**不加这个注解，`@Scheduled` 编译通过、启动无报错、任务永远不执行。**

排查方法：启动日志里搜 `TaskScheduler`，或者在方法里打一行日志看到底有没有触发。
:::

### 2.2 三种调度方式

| 方式 | 写法 | 含义 |
|---|---|---|
| **cron** | `@Scheduled(cron = "0 0 2 * * ?")` | 按日历时间，**最常用** |
| **fixedRate** | `@Scheduled(fixedRate = 5000)` | 从**上次任务开始**算，每 5 秒跑一次 |
| **fixedDelay** | `@Scheduled(fixedDelay = 5000)` | 从**上次任务结束**算，间隔 5 秒 |

### 2.3 `fixedRate` vs `fixedDelay` —— 必须搞清

假设任务本身耗时 8 秒，间隔设 5 秒：

```
fixedRate = 5000（按开始时间算）
0s ────任务执行(8s)────8s
     ↑ 5s 时就该跑第二次了，但线程还忙着
8s ──立即开始第二次──16s         ← 实际变成了「一个接一个跑」，没有间隔

fixedDelay = 5000（按结束时间算）
0s ────任务执行(8s)────8s        13s ──第二次──21s
                                    ↑ 结束后等 5 秒才开始
```

| 场景 | 选哪个 |
|---|---|
| 任务必须**固定频率**采样（如每 5 秒读一次传感器） | `fixedRate` |
| 任务不能重叠，两轮之间要有喘息（如批量处理） | `fixedDelay` |
| 按具体时间点执行（每天凌晨 2 点） | `cron` |

::: warning `fixedRate` 在任务耗时超过间隔时不会积压
Spring 的 `fixedRate` 不会因为来不及就排队堆积 —— 它等当前执行完后**立即**开始下一轮，不会补跑错过的那些次。

如果两者都需要（固定频率 + 允许并发），得配线程池并理解行为，见下。
:::

### 2.4 cron 表达式

**六位格式**（Spring 版本，不是 Linux crontab 的 5 位）：

```
秒  分  时  日  月  周
0   0   2   *   *   ?
```

| 字段 | 允许值 | 特殊字符 |
|---|---|---|
| 秒 | 0-59 | `,` `-` `*` `/` |
| 分 | 0-59 | 同上 |
| 时 | 0-23 | 同上 |
| 日 | 1-31 | `,` `-` `*` `/` `?` `L` `W` |
| 月 | 1-12 或 JAN-DEC | `,` `-` `*` `/` |
| 周 | 0-7 或 SUN-SAT（0 和 7 都是周日） | `,` `-` `*` `/` `?` `L` `#` |

**日和周必须有一个是 `?`**（互斥，避免语义冲突）。

常用实例：

| 表达式 | 含义 |
|---|---|
| `0 0 2 * * ?` | 每天凌晨 2:00:00 |
| `0 0/5 * * * ?` | 每 5 分钟 |
| `0 0 0 1 * ?` | 每月 1 号 0 点 |
| `0 30 8 ? * MON-FRI` | 工作日 8:30 |
| `0 0 0 L * ?` | 每月最后一天 0 点（`L` = last） |
| `0 0 9 ? * MON#2` | 每月第二个周一 9 点（`#` = 第几个） |

::: tip cron 表达式别硬背，但必须会读
写的时候用在线生成器（如 cron.qqe2.com），但要能一眼读懂别人写的表达式 —— 线上出问题时你就是靠这个定位的。

**读法**：先看第 3 位（时）和第 2 位（分），就知道一天跑几次。
:::

### 2.5 默认单线程 —— 多个任务会互相阻塞

**Spring 默认只给定时任务一个线程**。两个任务时间点撞上，一个会等另一个跑完。

```java
@Configuration
public class ScheduleConfig implements SchedulingConfigurer {

    @Override
    public void configureTasks(ScheduledTaskRegistrar registrar) {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);                              // 并发线程数
        scheduler.setThreadNamePrefix("scheduled-");
        scheduler.setErrorHandler(t -> log.error("定时任务执行异常", t));   // 全局异常处理
        scheduler.initialize();
        registrar.setTaskScheduler(scheduler);
    }
}
```

或者更简单：

```yaml
spring:
  task:
    scheduling:
      pool:
        size: 5
```

::: danger 单线程的隐蔽影响
任务 A 卡住（比如等一个超时的 HTTP 请求 30 秒），任务 B 到点了但**不执行**，等 A 结束才跑。

**表现是「任务偶发不执行」**，但日志里没有任何报错 —— 因为它确实执行了，只是晚了。

**给每个任务加执行日志（开始 + 结束 + 耗时）**，是排查这类问题的基础手段。
:::

### 2.6 分布式下的重复执行

**问题**：应用部署了 3 个实例，每个实例都会执行同一个 `@Scheduled` 方法 → **任务跑 3 遍**。

对「发优惠券」「扣积分」这类任务，跑 3 遍是灾难。

| 方案 | 做法 | 优缺点 |
|---|---|---|
| **分布式锁** | 执行前抢 Redis 锁（`SET NX EX`），抢到才执行 | 简单有效，需处理锁超时 |
| **数据库唯一键** | 任务执行前插入一条「执行记录」，唯一约束冲突就跳过 | 可靠，依赖数据库 |
| **只在一台部署任务** | 把定时任务拆成独立服务，只部署一个实例 | 最干净，但架构要调整 |
| **调度中心** | XXL-JOB / PowerJob，由中心统一调度 | 功能全（可视化、重试、报警），引入成本高 |

**Redis 锁的最小实现**：

```java
String lockKey = "lock:task:daily-sync";
Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, instanceId, Duration.ofMinutes(10));

if (Boolean.FALSE.equals(locked)) {
    log.info("其他实例正在执行，本次跳过");
    return;
}

try {
    doSync();
} finally {
    // 释放锁（生产实现要校验 value 是自己的实例 ID，避免删掉别人的锁）
    redisTemplate.delete(lockKey);
}
```

::: warning 释放锁必须校验持有者
不校验就删，会出现这种事故：

```
实例A 拿锁 → 执行超时 → 锁自动过期
实例B 拿到锁开始执行
实例A 执行完 → 删除锁（删掉了 B 的锁！）
实例C 又能拿到锁 → 重复执行
```

**解法**：value 存实例 ID，释放时用 Lua 脚本「比较 + 删除」保证原子性。
:::

### 2.7 任务失败怎么办

`@Scheduled` **没有内置重试**，方法抛异常后只打一行日志，下一次照常。

| 需求 | 做法 |
|---|---|
| 失败要重试 | 方法内自己 `for` 循环重试，或用 Spring Retry |
| 失败要告警 | `ThreadPoolTaskScheduler.setErrorHandler` 里发告警 |
| 执行记录要留痕 | 每次执行写一条任务日志表（成功/失败/耗时） |
| 补跑历史数据 | 做成「按日期参数执行」的任务，支持手动触发指定日期 |

::: tip 让任务可手动重跑是个好设计
```java
@Scheduled(cron = "0 0 2 * * ?")
public void sync() { syncByDate(LocalDate.now().minusDays(1)); }

// 同一个逻辑抽出来，接口可以按任意日期触发
public void syncByDate(LocalDate date) { ... }
```

**好处**：任务失败后不用改代码重发版，调接口补跑那一天的就行。
:::

### 2.8 与 07 模块的衔接

07 里提到的「本地消息表」方案，需要**定时扫描消息表并投递到 MQ** —— 那个扫描器就是 `@Scheduled` 任务：

```java
@Scheduled(fixedDelay = 5000)
public void resendPendingMessages() {
    // 查 status=待发送 的消息，投递到 MQ，成功后更新状态
    // 加分布式锁避免多实例重复投递
}
```

**这就是 07 + 08 的组合**：队列保证实时性，定时任务保证「最终一定会投出去」。

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | `ScheduleApplication`（`@EnableScheduling`） |
| 代码 | 三个演示任务：`cron` / `fixedRate` / `fixedDelay` |
| 代码 | `ScheduleConfig`：线程池 + 全局异常处理 |
| 代码 | 一个带 Redis 分布式锁的任务，验证多实例只有一个执行 |
| 代码 | 一个「按日期可手动触发」的任务（演示可重跑设计） |
| 笔记 | **`fixedRate` 与 `fixedDelay` 的真实执行时间线**（贴日志时间戳对比） |

## 4. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8008 端口
- [ ] 启动类加 `@EnableScheduling`，写第一个 cron 任务
- [ ] **故意去掉 `@EnableScheduling`**，验证任务不执行（踩坑素材）
- [ ] 写 `fixedRate` 和 `fixedDelay` 两个任务，各自打印开始/结束时间戳
- [ ] 让任务耗时超过间隔，**贴出两组时间戳对比**，证明行为差异
- [ ] 配线程池，验证两个任务时间点撞上时**并发**而非串行
- [ ] 写一个 Redis 锁任务，启动两个实例（不同端口）验证只有一个执行
- [ ] 写一个「按日期可重跑」的任务，暴露接口手动触发
- [ ] 写踩坑：`@EnableScheduling` 忘加、单线程阻塞、多实例重复执行
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| 用 `@Scheduled` 还是 XXL-JOB？ | 任务少（< 10 个）用 `@Scheduled`；要可视化/分布式/报警上调度中心 |
| 线程池大小 | 按任务数量和耗时定，一般 5~10 |
| 防重方案 | Redis 锁（快）/ 数据库唯一键（可靠） |
| 锁超时时间 | 要 > 任务最长耗时，且要处理超时续期 |
| 任务日志存哪 | 数据库表（可查询）/ 日志文件（够用） |
| 失败策略 | 内部重试 / 直接告警人工处理 |

## 6. 预习要点

1. **cron 表达式 6 位**（Spring 含秒），Linux crontab 是 5 位，别搞混
2. **`?` 只能用在「日」和「周」**，且两者必须有一个是 `?`
3. **时区**：`@Scheduled(cron = "...", zone = "Asia/Shanghai")` 可显式指定，容器里 UTC 时区会导致任务跑错时间
4. **任务要幂等**：手动重跑、失败重试、多实例 —— 任何一条都可能导致同一个任务执行两次

## 下一步

[09 安全认证](/backend/09-security) —— 系统能自动跑了，接下来管住「谁能调」。
