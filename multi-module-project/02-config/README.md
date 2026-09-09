# 02-config - Spring Boot 配置管理 + API Fox 集成

本模块演示两件事：

1. **Spring Boot 配置管理**：`application.yml` 配置项 + `@Value` 注解读取
2. **API Fox 集成**：把 Controller 接口同步到 API Fox 云端做测试

## 一、运行方式

```bash
# 在 multi-module-project 根目录
../mvnw clean package -DskipTests

# 启动（监听 8888 端口，与 application.yml 配置一致）
java -jar 02-config/target/02-config-0.0.1-SNAPSHOT.jar
```

启动后访问：

| 端点 | 用途 | 验证内容 |
|---|---|---|
| http://localhost:8888/config/info | 返回 4 个 `@Value` 注入的配置值 | 配置读取是否生效 |
| http://localhost:8888/user/info | 返回 `User` 对象 JSON | Lombok + LocalDate 封装 |

## 二、application.yml 配置项

```yaml
server:
  port: 8888
spring:
  application:
    name: 02-config
moqixu:
  name: 许莫淇
  job: 讲师
```

## 三、@Value 注入演示

`UserController` 里 4 个字段演示了 `@Value` 的标准用法：

```java
@Value("${server.port}")                  private String serverPort;
@Value("${spring.application.name}")      private String applicationName;
@Value("${moqixu.name}")                  private String moqixuName;
@Value("${moqixu.job}")                   private String moqixuJob;
```

**使用规范**：
- 语法必须是 `@Value("${属性名}")` —— `$` 符号和 `{}` 花括号必须齐全
- 注解包路径必须是 `org.springframework.beans.factory.annotation.Value`（springframework 包，不是别的）

## 四、API Fox 集成操作清单

API Fox 是 SaaS 接口测试平台 + IDEA 插件。集成步骤如下（你 IDEA 装好插件后照做）：

### 1. 注册并创建项目

1. 浏览器打开 https://apifox.com ，用微信/邮箱注册账号并登录
2. 进入工作台，点 **新建项目** → 名称填 `02-config`（或任意）→ 类型选 **HTTP**

### 2. 生成 API 访问令牌

1. 进入项目 → 左侧 **项目设置** → **API 访问令牌**
2. 点 **生成令牌** → 复制保存（只显示一次，关掉就看不到了）

### 3. IDEA 关联 API Fox

1. 打开 IDEA → **Settings** → 搜索 `Api Box` / `Apifox Helper`（插件名）
2. 填入：
   - **服务器地址**：默认 `https://api.apifox.com`（国内 SaaS）
   - **访问令牌**：粘上面生成的 token
   - **项目 ID**：API Fox 项目设置里能看到
3. 点 **Test Connection** 验证连通 → 保存

### 4. 同步 Controller 到 API Fox

1. 打开 `UserController.java`
2. **右键** → **Upload to Api Box**（或菜单 `ApiFox` → `Upload`）
3. 选要同步的接口 → 确认上传
4. 完成后会自动打开 API Fox 网页，可看到 `GET /config/info` 和 `GET /user/info` 两个接口

### 5. 配置测试环境

1. 在 API Fox 项目里 → **环境管理** → 新建环境
2. 名称：`本地开发`（或任意）
3. 基础 URL：`http://localhost:8888`
4. 保存并设为 **当前环境**

### 6. 启动应用 + 测试

1. 启动 02-config（`java -jar ...02-config-0.0.1-SNAPSHOT.jar`）
2. 在 API Fox 里点任一接口 → **发送** 按钮
3. 应该看到 200 状态码 + 正确响应：
   - `/config/info` → `server.port=8888, ...`
   - `/user/info` → `{"id":1,"username":"张三",...}`

## 五、API Fox 之外的备选测试

不安装 API Fox 也行，命令行 `curl` 效果一样：

```bash
curl http://localhost:8888/config/info
curl http://localhost:8888/user/info
```

或者浏览器直接访问这两个 URL。
