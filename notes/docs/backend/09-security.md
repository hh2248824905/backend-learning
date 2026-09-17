# 09 安全认证

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\09-security`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8009`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 「认证」和「授权」是两件事，区别在哪？
2. 用户密码在数据库里怎么存？为什么绝对不能存明文？
3. 用户登录后怎么让后续请求「记住」他？Cookie-Session 和 JWT 该怎么选？
4. Spring Security 的过滤器链是怎么工作的？请求进来经过什么？
5. 怎么控制「这个接口只有管理员能调」？

## 1. 模块定位

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 04 Web：会写接口 | **09：给接口加上「谁能调」的控制** | 10 文件（上传需要身份）、13 监控（`/actuator` 要保护） |

**核心价值**：能说清一个请求从进入到被放行/拒绝的完整链路，并能实现一套可用的认证授权方案。

## 2. 计划覆盖的知识点

### 2.1 认证 vs 授权

| 概念 | 英文 | 回答的问题 | 类比 |
|---|---|---|---|
| **认证** | Authentication | **你是谁？** | 出示身份证 |
| **授权** | Authorization | **你能做什么？** | 看你的权限能进哪些门 |

**顺序**：先认证（确定身份），再授权（判断权限）。认证失败返回 `401 Unauthorized`，授权失败返回 `403 Forbidden`。

::: tip 401 和 403 的区别经常被搞混
| 状态码 | 含义 | 客户端该做什么 |
|---|---|---|
| `401 Unauthorized` | **没认证**（或凭证无效/过期） | 去登录，或刷新 token 后重试 |
| `403 Forbidden` | **认证了，但没权限** | 别重试了，就是不行，提示用户提升权限 |
:::

### 2.2 密码存储

::: danger 三个绝对不允许
1. **存明文** —— 数据库泄露就是灾难
2. **用 MD5/SHA1 单向哈希** —— 彩虹表能反查，且同一密码哈希值相同（能看出哪些用户密码一样）
3. **自己手写加密算法**
:::

**正确做法：BCrypt**

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

**BCrypt 为什么安全**：

| 特性 | 说明 |
|---|---|
| **自带盐值** | 每次加密随机生成 salt，**同一密码两次加密结果不同** |
| **盐值存在哈希串里** | 不需要单独字段存 salt，`$2a$10$xxxx` 这一整串就够校验 |
| **可调计算成本** | `$2a$10$` 里的 `10` 是强度因子，2^10 次迭代。调大更安全但更慢 |
| **验证方式特殊** | 不能解密比对，而是用 `matches(明文, 密文)` 重新算一遍 |

```java
String hash = encoder.encode("123456");     // $2a$10$N9qo8uLOickgx2ZMRZoMy...
encoder.matches("123456", hash);            // true
encoder.matches("1234567", hash);           // false
```

**为什么 MD5 不行**：

```java
// MD5 同一个密码永远得到同一个值
md5("123456") == "e10adc3949ba59abbe56e057f20f883e"   // 永远是这个
// 这个哈希值在彩虹表里一查就得到明文
```

### 2.3 会话 vs JWT

| 维度 | Cookie-Session | JWT |
|---|---|---|
| 状态存哪 | **服务端**（内存/Redis） | **客户端**（token 本身带信息） |
| 服务端有状态 | 有（要管 session 存储和过期） | 无 |
| 水平扩展 | 要共享 session（Redis）或粘性会话 | 天然支持，任何实例都能校验 |
| 主动失效 | ✅ 删掉服务端的 session 即可立即失效 | ❌ **做不到**（见下） |
| 跨域 | 麻烦（Cookie 需要 `SameSite`/`Domain` 配置） | 简单（放 `Authorization` 头） |
| 移动端友好 | 差（Cookie 是浏览器概念） | 好 |
| token 大小 | 小（只是一个 sessionId） | 大（含 payload，可能几 KB） |

### 2.4 JWT 的结构

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiIxIiwibmFtZSI6IuW8oOS4iSJ9 . dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
      Header                     Payload                        Signature
```

| 部分 | 内容 | 是否加密 |
|---|---|---|
| Header | 算法类型（`alg`）和 token 类型（`typ`） | ❌ **只是 Base64 编码，能直接解出来** |
| Payload | 声明（claim）：`sub`、`exp`、自定义字段 | ❌ **同样只是 Base64，能直接解出来** |
| Signature | 用密钥对前两部分签名 | —— |

::: danger JWT 的 Payload 是不加密的
在 [jwt.io](https://jwt.io) 粘一个 token 进去，payload 内容直接明文显示。

**所以 payload 里绝对不能放**：密码、身份证号、手机号、任何敏感信息。

**JWT 解决的问题是「防篡改」，不是「保密」。** 想保密得额外做加密。
:::

**验证流程**：

```
客户端带 token → 服务端用密钥重新计算签名 → 与 token 里的签名比对
                                            ↓
                                    一致 → 没有被篡改，读 payload 取用户信息
                                    不一致 → 拒绝
```

**签名保证了**：别人改了 payload 里的 `userId`，签名就对不上了。

### 2.5 JWT 的致命短板：无法主动失效

**场景**：用户点「退出登录」，或者管理员封了一个账号，或者密码泄露要强制踢下线。

**Session 方案**：删掉服务端的 session 记录，立刻生效。

**JWT 方案**：token 已经发给客户端了，**服务端无法收回**。只能等它自然过期。

**三种缓解方案**：

| 方案 | 做法 | 代价 |
|---|---|---|
| **缩短有效期** | access token 设 15 分钟 | 用户体验差（频繁要重登） |
| **双 token** | access token 短（15min）+ refresh token 长（7天），refresh 存服务端可控 | 复杂度上升，**主流方案** |
| **黑名单** | 把失效的 token 存 Redis，每次请求查一遍 | 又变回有状态了，Redis 挂了就失效 |

::: tip 所以「JWT 无状态」是有代价的
无状态带来了水平扩展的便利，代价是**失去了主动失效的能力**。

**实际生产里的主流方案是「双 token + 短期 access」** —— 既能水平扩展，又能在 refresh 环节控制失效。
:::

### 2.6 Spring Security 过滤器链

一个请求进来，会依次经过一串 Filter：

```
请求
 ↓
UsernamePasswordAuthenticationFilter     ← 表单登录时处理
 ↓
SecurityContextPersistenceFilter        ← 恢复/保存安全上下文
 ↓
自定义 JWT 认证过滤器                    ← 实际项目里加在这里
 ↓
ExceptionTranslationFilter              ← 把认证/授权异常转成 401/403
 ↓
FilterSecurityInterceptor               ← 做最终授权判断
 ↓
Controller
```

**开发中实际要做的事**：

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)              // 前后端分离关掉 CSRF
            .sessionManagement(s -> s.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/login", "/auth/register").permitAll()   // 白名单
                .requestMatchers("/admin/**").hasRole("ADMIN")                  // 需要管理员
                .anyRequest().authenticated()                                    // 其他都要登录
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
```

### 2.7 方法级权限

```java
@PreAuthorize("hasRole('ADMIN')")
@DeleteMapping("/users/{id}")
public Result<Void> delete(@PathVariable Long id) { }

@PreAuthorize("#id == authentication.principal.id or hasRole('ADMIN')")
@GetMapping("/users/{id}")
public Result<User> get(@PathVariable Long id) { }   // 只能看自己，或管理员
```

需要开启：

```java
@EnableMethodSecurity      // 替代已过时的 @EnableGlobalMethodSecurity
```

| 表达式 | 含义 |
|---|---|
| `hasRole('ADMIN')` | 有 ADMIN 角色（**代码里要写 `ROLE_ADMIN`**，Spring 会自动补前缀） |
| `hasAuthority('user:delete')` | 有具体权限（细粒度，不补前缀） |
| `hasAnyRole('ADMIN','USER')` | 任一角色 |
| `#id == authentication.principal.id` | 参数与当前用户 ID 相等 |
| `isAuthenticated()` | 已登录 |
| `permitAll()` / `denyAll()` | 全放行 / 全拒绝 |

::: tip 角色 vs 权限
```java
hasRole('ADMIN')              // 角色：粗粒度，面向"岗位"
hasAuthority('order:delete')  // 权限：细粒度，面向"操作"
```

**小项目用角色就够**；权限点多（几十个操作要分别控制）时上「角色-权限」两层模型。
:::

### 2.8 CORS 与 CSRF

| 概念 | 是什么 | 前后端分离时怎么处理 |
|---|---|---|
| **CSRF**（跨站请求伪造） | 攻击者诱导已登录用户去请求你的接口 | **关掉**（`.csrf().disable()`）。因为不用 Cookie 自动携带凭证，JWT 放 header 里不怕 CSRF |
| **CORS**（跨域资源共享） | 浏览器同源策略下，前端 `localhost:5173` 调后端 `localhost:8009` 被拦 | 后端配 `addCorsMappings` 或用 `@CrossOrigin` |

::: warning 别为了图方便配 `allowedOrigins("*")` + `allowCredentials(true)`
这两个组合会被浏览器拒绝（规范不允许）。如果确实需要携带凭证，必须明确列出允许的源：

```java
registry.addMapping("/**")
        .allowedOrigins("http://localhost:5173")   // 明确列出，不用 *
        .allowCredentials(true)
        .allowedMethods("*");
```
:::

### 2.9 与 04 模块的衔接

04 里写的「统一返回体 + 全局异常处理」在这里要继续用：

- 认证失败（401）也要返回**统一的 JSON 结构**，而不是 Spring Security 默认的那个 HTML 错误页
- 需要自定义 `AuthenticationEntryPoint`（处理 401）和 `AccessDeniedHandler`（处理 403）

**这是 04 的规范在 09 里的延伸** —— 整个系统的错误响应格式必须一致。

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | `SecurityConfig`：过滤器链、白名单、CORS、密码编码器 |
| 代码 | `JwtUtil`：生成/解析 token |
| 代码 | `JwtAuthenticationFilter`：从 header 取 token，解析后放入 SecurityContext |
| 代码 | `AuthController`：注册、登录、刷新 token、登出 |
| 代码 | `User`/`Role` 表与 Mapper（复用 06） |
| 代码 | 一个 `@PreAuthorize` 保护的管理员接口 |
| 笔记 | **不带 token / 带无效 token / 带有效 token 但无权限** 三种情况的真实响应 |

## 4. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8009 端口
- [ ] 引 `spring-boot-starter-security` 和 JWT 库（`jjwt`）
- [ ] 建 `user` / `role` 表，用 BCrypt 存密码
- [ ] 写 `JwtUtil`，实测生成的 token 在 jwt.io 能解出 payload（**验证 payload 不加密**）
- [ ] 写登录接口，返回 token
- [ ] 写 `JwtAuthenticationFilter`，接入过滤器链
- [ ] 实测三种请求：无 token / 错误 token / 正确 token
- [ ] 实测 `@PreAuthorize` 的 403
- [ ] 起前端项目（`E:\qianduan\vue-app`）实测跨域登录
- [ ] 贴真实输出：登录响应、三种鉴权结果、403 响应
- [ ] 写踩坑：比如 token 过期没处理、`hasRole` 前缀问题、CORS 预检 `OPTIONS` 被拦
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| Session 还是 JWT？ | 前后端分离 + 移动端 → JWT；传统服务端渲染 → Session |
| token 存哪 | `localStorage`（简单，有 XSS 风险）/ `httpOnly` Cookie（防 XSS，要处理 CSRF） |
| 有效期 | access 15~30 分钟，refresh 7 天 |
| 用双 token 吗？ | 需要「主动踢人」就必须用 |
| 权限模型 | 纯角色（简单）/ 角色 + 权限（灵活） |
| Redis 用不用 | 用（存 refresh token、黑名单、登录失败计数） |

## 6. 预习要点

1. **同源策略**：协议 + 域名 + 端口三者完全相同才算同源，任一不同就是跨域
2. **`localStorage` 存 token 的风险**：XSS 攻击能直接读走。`httpOnly` Cookie 读不到，但需要防 CSRF
3. **JWT 是「自包含」的**：服务端不存，靠签名验证 —— 这是它所有优点和缺点的根源
4. **Spring Security 6 的变化**：`WebSecurityConfigurerAdapter` 已删除，改用 `SecurityFilterChain` Bean 的写法

## 下一步

[10 文件处理](/backend/10-file) —— 登录问题解决了，接下来处理「上传下载」。
