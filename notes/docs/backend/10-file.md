# 10 文件处理

<NoteStatus level="todo" />

::: info 模块坐标
源码：`E:\houduan\10-file`（**目录已建，仅有 `.gitkeep` 占位，代码未开始**）　·　计划端口：`8010`
:::

> 这一篇是**施工图**。知识点和待办清单已定好，动手时按结构填内容。

## 0. 这个模块要解决什么问题

**规划中的五条**

1. 上传一个 500MB 的文件，怎么保证不撑爆 JVM 内存？
2. 怎么限制上传文件的类型和大小？前端限制能被绕过吗？
3. 中文文件名下载后变成乱码，问题出在哪？
4. 下载大文件时，怎么不阻塞 Tomcat 的工作线程？
5. 文件存本地磁盘还是对象存储？分别有什么代价？

## 1. 模块定位

| 前的准备 | 本模块 | 后面依赖它的 |
|---|---|---|
| 04 Web：会写接口 | **10：处理二进制流和大文件** | 业务功能（头像上传、Excel 导入导出、附件） |
| 09 安全：知道谁在传 | | |

**核心价值**：文件处理是**最容易写出内存问题**的地方。做对了是功能，做错了是生产事故。

## 2. 计划覆盖的知识点

### 2.1 上传基础

```java
@PostMapping("/upload")
public Result<String> upload(@RequestParam("file") MultipartFile file) {
    // file.getOriginalFilename()  原始文件名
    // file.getSize()              字节数
    // file.getContentType()       MIME 类型（客户端提供，不可信）
    // file.getInputStream()       输入流
    // file.transferTo(目标文件)    转存到磁盘
}
```

**前端表单必须带 `enctype`**：

```html
<form action="/file/upload" method="post" enctype="multipart/form-data">
  <input type="file" name="file" />
  <button type="submit">上传</button>
</form>
```

::: warning 少了 `enctype="multipart/form-data"` 会怎样
Spring 抛异常：

```
Current request is not a multipart request
```

因为默认的 `application/x-www-form-urlencoded` 编码传不了二进制内容，请求体里根本没有文件数据。
:::

### 2.2 大小限制

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB          # 单个文件上限
      max-request-size: 50MB       # 整个请求上限（含所有文件和其他字段）
      file-size-threshold: 1MB     # 超过这个大小就写到磁盘临时文件，不占内存
      location: /tmp/upload        # 临时文件目录
```

::: danger 三个限制层次，别只配一层
| 层次 | 在哪配 | 绕过方式 |
|---|---|---|
| 前端 JS 校验 | 浏览器 | **改代码/用 Postman 直接调接口，完全绕过** |
| Spring 的 `multipart` 上限 | `application.yml` | 改不了，会抛 `MaxUploadSizeExceededException` |
| **Nginx / 网关限制** | 反向代理配置 | —— |

**前端校验只是为了用户体验**（早点告诉用户文件太大），**服务端校验才是安全边界**。

还有个容易忽略的：Spring 的限制**只在经过 DispatcherServlet 时生效**。如果前面有 Nginx，Nginx 的 `client_max_body_size`（默认 1MB！）会先把请求拦掉，报 413。
:::

### 2.3 `MultipartFile` 和内存的关系

**关键机制**（Servlet 规范定义）：

```
文件大小 ≤ file-size-threshold（默认 1MB 左右）
    → 整个文件放在内存里
    → 安全

文件大小 > file-size-threshold
    → 写到磁盘临时文件
    → 也安全

但如果阈值配得过大（比如设成 100MB）+ 并发 20 个上传
    → 20 × 100MB = 2GB 堆内存被占
    → OutOfMemoryError
```

::: warning 阈值不要配大
`file-size-threshold` 保持默认值（1MB 或更小）。它决定的是「多大的文件放内存」，配大了就是在给自己埋内存炸弹。

**处理大文件的原则**：不要让整个文件进内存，用**流式处理**。
:::

### 2.4 文件名安全

**不能直接用 `getOriginalFilename()` 拼路径**：

```java
// ❌ 目录穿越攻击
String fileName = file.getOriginalFilename();
// 攻击者构造文件名：../../../../etc/passwd
// 结果文件被写到系统目录

// ✅ 重新生成文件名，只保留扩展名
String ext = StringUtils.getFilenameExtension(originalName);
String newName = UUID.randomUUID() + "." + ext;
```

**为什么必须重命名**：

| 风险 | 说明 |
|---|---|
| **路径穿越** | 文件名含 `../` 写到任意目录 |
| **覆盖已有文件** | 两个人传同名文件，后者覆盖前者 |
| **特殊字符** | 文件名里的 `/` `\` `:` 在 Windows 下是非法字符 |
| **中文/空格** | URL 编码问题，下载时乱码 |
| **可执行扩展名** | 传个 `.jsp` 到 Web 目录 → 直接 getshell（虽然 Spring Boot 默认不解析 JSP） |

**黑名单校验扩展名**：

```java
private static final Set<String> ALLOWED = Set.of("jpg", "jpeg", "png", "gif", "pdf", "xlsx", "docx");

if (!ALLOWED.contains(ext.toLowerCase())) {
    throw new BusinessException("不支持的文件类型");
}
```

::: danger 用白名单，不要用黑名单
黑名单（禁止 `exe`/`jsp`/`php`）永远列不全。改成允许的扩展名列表，**不在列表里的一律拒绝**。
:::

### 2.5 中文文件名下载乱码

**根因**：HTTP 响应头里的 `Content-Disposition` 规范要求用 ISO-8859-1 编码，直接放中文会乱码或被截断。

**正确写法**（RFC 5987）：

```java
String fileName = "报表-2026.xlsx";
String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");

response.setHeader("Content-Disposition",
        "attachment; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded);
```

| 部分 | 作用 |
|---|---|
| `filename="..."` | 老浏览器兼容（用 URL 编码后的串） |
| `filename*=UTF-8''...` | RFC 5987 标准写法，新浏览器优先用这个 |

用 Spring 的 `ContentDisposition` 更省事：

```java
ContentDisposition cd = ContentDisposition.attachment()
        .filename(fileName, StandardCharsets.UTF_8)
        .build();
response.setHeader(HttpHeaders.CONTENT_DISPOSITION, cd.toString());
```

### 2.6 流式下载

```java
@GetMapping("/download/{id}")
public void download(@PathVariable Long id, HttpServletResponse response) throws IOException {
    File file = fileService.get(id);
    response.setContentType("application/octet-stream");
    response.setContentLengthLong(file.length());

    try (InputStream in = new FileInputStream(file);
         OutputStream out = response.getOutputStream()) {
        // 8KB 缓冲区，循环搬运，全程不把整个文件读进内存
        byte[] buffer = new byte[8192];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }
}
```

**两种写法对比**：

```java
// ❌ 小文件可以，大文件会 OOM
byte[] bytes = Files.readAllBytes(file.toPath());   // 100MB 文件 = 100MB 堆内存
return bytes;

// ✅ 流式，内存占用固定 8KB
```

也可以让 Spring 用 `Resource` 自动处理：

```java
@GetMapping("/download/{id}")
public ResponseEntity<Resource> download(@PathVariable Long id) {
    FileSystemResource resource = new FileSystemResource(file);
    return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
            .body(resource);        // Spring 会自动流式写出
}
```

::: tip 流式处理的边界问题
```java
try (InputStream in = ...; OutputStream out = ...) {
    // 正常搬运
} catch (ClientAbortException e) {
    // ← 用户中途取消了下载，会抛这个异常
    log.warn("客户端取消下载: {}", fileName);
}
```

**用户点了取消，服务端会收到连接断开**，抛 `ClientAbortException`。如果不在日志里单独识别，你的错误日志会被这种「假异常」刷满。
:::

### 2.7 与 09 模块的衔接

文件接口**必须鉴权**：

| 场景 | 要求 |
|---|---|
| 上传 | 必须登录（防止被当图床滥用） |
| 下载 | 必须校验**文件归属**（A 用户不能下载 B 用户的私有文件） |
| 文件名 | **不要暴露真实存储路径**，用文件 ID 查 |

**典型的越权缺陷**：

```java
// ❌ 只校验了登录，没校验归属
@GetMapping("/download/{fileId}")
public void download(@PathVariable Long fileId) { ... }
// 攻击者遍历 fileId 就能下载所有人的文件

// ✅ 校验归属
FileEntity file = fileService.get(fileId);
if (!file.getOwnerId().equals(currentUserId) && !isAdmin()) {
    throw new BusinessException("无权访问该文件");
}
```

### 2.8 本地存储 vs 对象存储

| 维度 | 本地磁盘 | 对象存储（OSS/MinIO/S3） |
|---|---|---|
| 部署复杂度 | 低（直接用） | 需要额外服务或开通云服务 |
| **多实例** | ❌ **致命问题**：实例 A 传的文件，请求打到实例 B 就找不到 | ✅ 天然共享 |
| 容量扩展 | 受单机磁盘限制 | 近乎无限 |
| 备份 | 自己做 | 服务商提供 |
| 成本 | 磁盘便宜 | 按量付费 + 流量费 |
| CDN 加速 | 要自己搭 | 直接接入 |

**结论**：

```
单实例、内部系统 → 本地磁盘够用
多实例、面向公网 → 对象存储
```

::: tip 本地存储的过渡方案
如果暂时上不了对象存储，但要支持多实例，可以：
- 用 NFS 共享目录
- 或者文件服务独立部署一台机器（应用通过 HTTP 调用它）

**但这些都是权宜之计**，最终还是要走对象存储。
:::

## 3. 计划产出

| 产出 | 内容 |
|---|---|
| 代码 | `FileController`：上传、下载、查询、删除四个接口 |
| 代码 | `FileService`：重命名、白名单校验、归属校验 |
| 代码 | `file` 表（复用 06 的 ORM）：存文件 ID、原始名、存储路径、大小、类型、上传人 |
| 配置 | `spring.servlet.multipart.*` 上限配置 |
| 代码 | 全局异常处理里加 `MaxUploadSizeExceededException` 的友好提示 |
| 笔记 | **大文件上传/下载的内存占用实测**（对比流式与非流式） |

## 4. 待办清单

- [ ] 建模块、挂父 pom、删 `.gitkeep`、配 8010 端口
- [ ] 建 `file` 表（复用 06 的 Mapper）
- [ ] 写上传接口，用 UUID 重命名，白名单校验扩展名
- [ ] 配 `multipart` 上限，实测超限时的报错
- [ ] 在全局异常处理里接住 `MaxUploadSizeExceededException`，返回友好 JSON
- [ ] 写下载接口，**用流式 + `ContentDisposition` 正确编码中文名**
- [ ] 实测中文文件名下载不乱码
- [ ] **故意用 `Files.readAllBytes` 传一个大文件**，观察内存占用（踩坑素材）
- [ ] 加归属校验，实测 A 用户访问 B 用户文件返回 403
- [ ] 贴真实输出：上传结果、下载响应头、大小超限报错、跨用户越权的拒绝响应
- [ ] 写踩坑：`not a multipart request`、Nginx 413、中文乱码、`ClientAbortException`
- [ ] 更新内容规划表与进度快照

## 5. 关键决策（写笔记时要回答）

| 问题 | 可选方案 |
|---|---|
| 本地存还是对象存储？ | 单机 → 本地；多实例/公网 → 对象存储 |
| 文件放哪个目录？ | 独立于应用目录（如 `D:\uploads`），**不能放 `resources/static`**（会随打包覆盖，且重启后丢失） |
| 用什么命名？ | UUID + 扩展名（**不要用原始名**） |
| 原始文件名存哪 | 数据库（下载时用它作为 `Content-Disposition` 的文件名） |
| 允许哪些扩展名 | 白名单，按业务需要最小化 |
| 单文件大小上限 | 按业务定，一般 10~50MB；更大考虑分片上传 |
| 要不要 md5 去重 | 文件量大时有价值（相同内容只存一份） |

::: warning 上传目录不要放在项目目录里
```yaml
# ❌ 错误
file.upload-path: ./src/main/resources/static/

# ✅ 正确
file.upload-path: D:/uploads/
```

放在 `resources/static` 的问题：
1. `mvn clean` 会删掉
2. 重新打包部署时被覆盖
3. 开发环境能看到，但生产打包后 jar 里的路径是只读的
:::

## 6. 预习要点

1. **`multipart/form-data` 编码**：每个字段用 boundary 分隔，文件内容原样放进请求体
2. **`Content-Type` vs `Content-Disposition`**：前者说「这是什么类型的数据」，后者说「该怎么处理它」（内联显示还是下载）
3. **`application/octet-stream`**：万能二进制类型，触发浏览器下载
4. **分片上传**：大文件切成小块分别上传，最后合并 —— 支持断点续传，是大文件上传的标准方案

## 下一步

[11 接口文档](/backend/11-doc) —— 接口越来越多，怎么让文档自动跟上代码。
