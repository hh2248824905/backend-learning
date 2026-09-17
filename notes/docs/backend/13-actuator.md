# 13 监控运维

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\13-actuator`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8013`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 服务上线后是死是活、内存用了多少、慢在哪 —— 怎么不登服务器就知道？
2. Actuator 默认暴露哪些端点？哪些能安全暴露、哪些绝对不能？
3. 怎么把指标接到 Prometheus + Grafana 做可视化？
4. 服务重启时正在处理的请求怎么办？
5. `/actuator/env` 为什么绝对不能对外？

## 1. 模块定位

| 前的准备 | 本模块 | 后面 |
|---|---|---|
| 12 测试：证明功能对 | **13：证明线上活着、看得见** | 部署与运维 |

**前面 12 个模块解决「功能正确」，13 解决「运行状态可见」。**

## 2. 计划覆盖的知识点

### 2.1 集成

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

**只暴露两个端点**（默认行为）：

```bash
curl http://localhost:8013/actuator
```

```json
{
  "_links": {
    "self": { "href": "http://localhost:8013/actuator", "templated": false },
    "health": { "href": "http://localhost:8013/actuator/health", "templated": false }
  }
}
```

::: danger 默认只暴露 2 个端点是**安全设计**，不是功能缺失
Actuator 一共有 **20+ 个端点**，Spring Boot 默认只暴露 `health` 和 `info`。这是刻意的 —— 因为其他端点**会泄露敏感信息**。

很多人为了"方便调试"配成：
```yaml
management:
  endpoints:
    web:
      exposure:
        include: "*"      # ← 生产环境的重大安全事故
```
**千万不要这样。** 后面详细说为什么。
:::

### 2.2 端点清单（关键几个）

| 端点 | 作用 | 能暴露吗 |
|---|---|---|
| `health` | 健康状态（DB、Redis、磁盘连通性） | ✅ 可以（运维探针要用） |
| `info` | 应用信息（版本、构建时间） | ✅ 可以（自己控制内容） |
| `metrics` | 指标（CPU、内存、HTTP 请求数、耗时） | ⚠️ 谨慎（有信息价值也有泄露价值） |
| `loggers` | 查看/修改日志级别 | ❌ **不能**（`POST` 能动态开启 debug，泄露敏感日志） |
| `env` | **所有配置项**（含脱敏后的值） | ❌ **绝对不能**（见下） |
| `configprops` | 所有 `@ConfigurationProperties` Bean | ❌ **绝对不能** |
| `beans` | 所有 Bean 定义和依赖关系 | ❌ **不能**（暴露内部架构） |
| `mappings` | 所有 URL 映射 | ❌ **不能**（等于把接口清单送人） |
| `heapdump` / `threaddump` | **下载堆内存快照** | ❌ **绝对不能**（详见下） |
| `shutdown` | 优雅关闭应用 | ❌ **绝对不能**（远程关停服务） |

::: danger `/actuator/heapdump` 是最高危的端点
它允许**下载整个 JVM 堆内存快照**。堆里有什么？

- 用户的密码（明文，只要进过内存）
- JWT 的签名密钥
- 数据库连接串和密码
- 正在处理的请求体（含个人信息）
- Session 内容

**拿到 heapdump 就等于拿到了应用内所有的秘密。** 用 MAT / JVisualVM 打开就能逐字段搜。

**永远不要在生产暴露 `heapdump`。** 关联风险：`env`、`configprops`、`threaddump` 同理。
:::

### 2.3 安全的暴露配置

```yaml
# application-dev.yml —— 开发环境宽松一点，方便调试
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, loggers, mappings
  endpoint:
    health:
      show-details: always        # 显示详细健康信息（哪个组件不健康）

# application-prod.yml —— 生产环境只暴露必要端点
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      show-details: when-authorized   # 只有认证后才能看详情
  server:
    port: 8081                    # 或者：把管理端点放到独立端口，不对公网开放
```

::: tip 生产环境的两种隔离方案
**方案一：独立端口**
```yaml
management:
  server:
    port: 8081      # 管理端点在 8081，业务在 8013
```
然后只让内网（监控系统）能访问 8081，公网只放 8013。

**方案二：独立路径 + 鉴权**
```yaml
management:
  endpoints:
    web:
      base-path: /internal-monitor    # 改掉默认的 /actuator
```
再配合 Spring Security 限制访问。

**两者可以叠加用。** 改路径**不算安全措施**（能被扫出来），真正的边界是**鉴权和网络隔离**。
:::

### 2.4 健康检查

```bash
curl http://localhost:8013/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP", "details": { "database": "MySQL", "validationQuery": "isValid()" } },
    "diskSpace": { "status": "UP", "details": { "total": 500107862016, "free": 301989888000 } },
    "ping": { "status": "UP" }
  }
}
```

**`health` 端点是 K8s / 负载均衡器探针的入口**：

| 探针类型 | 用途 | 配到哪 |
|---|---|---|
| **liveness** | 进程还活着吗？不活就重启 | `/actuator/health/liveness` |
| **readiness** | 能接流量了吗？不能就从负载均衡摘掉 | `/actuator/health/readiness` |

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true        # 开启 liveness / readiness 分组
```

::: warning `status: UP` 不一定代表真的健康
默认的健康检查只验证「依赖能连上」（DB 能 ping 通），**不验证「业务逻辑正常」**。

如果你的应用连上了数据库但所有查询都超时，`health` 依然报 `UP`。

**需要业务级健康检查时，自己实现**：

```java
@Component
public class OrderQueueHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        int pending = orderQueue.size();
        if (pending > 10000) {
            return Health.down().withDetail("pending", pending).build();   // 积压过多，报不健康
        }
        return Health.up().withDetail("pending", pending).build();
    }
}
```
:::

### 2.5 指标与 Micrometer

Actuator 用 **Micrometer** 做指标的抽象层（和 SLF4J 之于日志是一个思路）：

```
业务代码打点 → Micrometer API → 导出到 Prometheus / InfluxDB / Datadog / ...
```

**开箱即用的指标**（引入 micrometer 后自动采集）：

| 类别 | 指标 |
|---|---|
| JVM | 堆内存、非堆内存、GC 次数与耗时、线程数 |
| 系统 | CPU 使用率、负载、磁盘 |
| HTTP | 各接口的请求数、响应时间、状态码分布 |
| 数据源 | 连接池活跃/空闲连接、等待时间 |

**自定义业务指标**：

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MeterRegistry meterRegistry;
    private final Counter orderCounter = Counter.builder("business.order.created")
            .description("创建的订单数")
            .register(meterRegistry);

    public void create(Order order) {
        // 业务逻辑
        orderCounter.increment();          // 计数 +1
    }
}
```

**对接 Prometheus**：

```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics, prometheus
  metrics:
    tags:
      application: ${spring.application.name}      # 给所有指标打上应用名标签
```

然后 `/actuator/prometheus` 会返回 Prometheus 格式的指标文本，Prometheus 定时抓取，Grafana 画图。

### 2.6 优雅停机

**问题**：服务收到停止信号时，**正在处理的请求会被直接掐断**，客户端收到连接重置。

```yaml
server:
  shutdown: graceful              # 开启优雅停机

spring:
  lifecycle:
    timeout-per-shutdown-phase: 30s    # 最多等 30 秒
```

**优雅停机的行为**：

```
收到 SIGTERM
    ↓
① 停止接受新请求（从负载均衡摘除）
    ↓
② 等待正在处理的请求完成（最多 30 秒）
    ↓
③ 关闭线程池、数据源、Spring 容器
    ↓
进程退出
```

::: danger 不配优雅停机的后果
用户正在提交订单，服务重启 → **连接被重置，请求丢失**。

配合 K8s 的滚动更新时，每发布一次都有一批用户请求失败。

**这是「发布期间偶发报错」的常见原因**，而且非常难排查 —— 因为只在重启那几秒出现。
:::

### 2.7 与 09/11 模块的衔接

| 关联 | 说明 |
|---|---|
| **09 安全** | Actuator 端点必须接入 Spring Security 的保护范围，`/actuator/**` 不能是白名单 |
| **11 文档** | springdoc 会扫到 Actuator 的端点，所以 `packages-to-scan` 要限定包 |
| **02 配置** | `management.*` 配置项本身就该按环境区分（dev 宽松、prod 收紧）—— 正是 02 讲的多环境原则 |

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | `spring-boot-starter-actuator` + `micrometer-registry-prometheus` 依赖 |
| 代码 | 一个自定义 `HealthIndicator`（业务级健康检查） |
| 代码 | 一个自定义 `Counter` 指标（业务埋点） |
| 配置 | dev / prod 两套 `management` 配置（对比端点暴露范围） |
| 配置 | `server.shutdown: graceful` + 超时 |
| 笔记 | **非法暴露端点的实测**：访问 `/actuator/env` 看它泄露了什么 |

## 4. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8013 端口
- [ ] 引 actuator 依赖，访问 `/actuator/health` 和 `/actuator` 看默认暴露了什么
- [ ] **临时把 `include` 配成 `*`**，逐个访问 `env`、`beans`、`mappings`、`heapdump`
- [ ] **贴出 `/actuator/env` 的真实输出**（作为安全警示素材，注意脱敏）
- [ ] 恢复安全配置：只暴露 `health, info, metrics, prometheus`
- [ ] 写自定义 `HealthIndicator`，模拟「依赖超时 → 状态 DOWN」
- [ ] 写自定义 `Counter`，在接口里打点，验证 `/actuator/metrics/business.order.created` 有数据
- [ ] 配 Prometheus registry，访问 `/actuator/prometheus` 看指标文本
- [ ] 配 `server.shutdown: graceful`，起服务后发 SIGTERM，观察是否等待请求完成
- [ ] 贴真实输出：health JSON、metrics 数据、优雅停机的日志
- [ ] 写踩坑：`include: "*"` 的安全事故、health 报 UP 但业务不可用、优雅停机没生效
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| 暴露哪些端点 | prod 只 `health, info, metrics`；dev 可多加 `loggers`、`mappings` |
| 端点在哪个端口 | 独立端口（更安全）/ 同端口 + 路径隔离 + 鉴权 |
| `health` 详情给谁看 | `show-details: when-authorized`（推荐）/ `never`（最保守） |
| 指标给谁抓 | Prometheus（主流）/ 云厂商 APM |
| 要不要自定义健康检查 | 有关键外部依赖或积压队列时**要** |
| 优雅停机等多久 | 略大于 P99 请求耗时，一般 30 秒 |

## 6. 预习要点

1. **可观测性的三大支柱**：Metrics（指标，看趋势）、Logging（日志，看细节）、Tracing（链路，看调用关系）—— Actuator 主要覆盖前两个
2. **`health` 端点要给谁用**：负载均衡器/K8s 探针自动调用，不是给人看的（人看的是 Grafana）
3. **指标要有维度**：只报「总请求数」用处不大，按「接口 + 状态码」打标签才能定位问题
4. **监控的告警阈值**要从真实数据推出来，拍脑袋定的阈值只会造成告警疲劳

## 下一步

13 是后端主线的最后一站。

- 回到 [学习路线](/guide/roadmap) 看整体进度
- 或者去 [工程化部分](/engineering/) 补 Maven、Git、IDEA 这些跨模块的通用技能
