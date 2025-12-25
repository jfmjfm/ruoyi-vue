# CORS跨域错误 (403) 分析报告

## 问题描述
运行模型时出现：**"运行模型失败：CORS跨域错误 (403)。请确保开发服务器正在运行（npm run serve），并检查 vue.config.js 中的代理配置"**

## 错误原因分析

### 1. 服务端CORS配置限制
根据您提供的服务端CORS配置，允许的源（Origin）为：
- `http://127.0.0.1:5500`
- `http://localhost:5500`
- `http://172.16.123.81:8890/`

### 2. 前端开发服务器端口
根据 `vue.config.js` 配置，Vue开发服务器运行在：
- **默认端口：8890**
- 访问地址：`http://localhost:8890` 或 `http://127.0.0.1:8890`

### 3. 核心问题
**源（Origin）不匹配**：
- 前端运行在：`http://localhost:8890` 或 `http://127.0.0.1:8890`
- 服务端只允许：`http://localhost:5500` 和 `http://127.0.0.1:5500`
- **8890端口不在服务端允许列表中，导致CORS 403错误**

### 4. 请求流程分析
前端代码（`index.vue` 第441行）使用相对路径：
```javascript
const url = `/repa/task/run`;
```

**正常情况**（代理生效）：
- 请求：`http://localhost:8890/repa/task/run`
- Vue代理转发到：`http://172.16.124.1:8686/repa/task/run`
- 此时请求源是 `http://localhost:8890`，服务端检查CORS配置

**异常情况**（代理未生效或直接访问）：
- 如果直接访问 `http://172.16.124.1:8686/repa/task/run`
- 浏览器会发送跨域请求，源为 `http://localhost:8890`
- 服务端检查发现源不在允许列表中，返回403

## 解决方案

### 方案1：修改服务端CORS配置（推荐）⭐
在服务端CORS配置中添加前端开发服务器的源：

```java
cfg.setAllowedOrigins(List.of(
    "http://127.0.0.1:5500", 
    "http://localhost:5500", 
    "http://172.16.123.81:8890/",
    "http://localhost:8890",      // ✅ 新增
    "http://127.0.0.1:8890"       // ✅ 新增
));
```

**优点**：解决根本问题，支持开发环境
**缺点**：需要修改服务端代码并重新部署

### 方案2：修改Vue开发服务器端口
修改 `vue.config.js`，将端口改为5500：

```javascript
const port = process.env.port || process.env.npm_config_port || 5500 // 改为5500
```

**优点**：无需修改服务端
**缺点**：可能与现有服务冲突，需要确保5500端口可用

### 方案3：确保代理正常工作（检查项）
虽然 `vue.config.js` 已配置代理，但需要确保：

1. **开发服务器正在运行**
   ```bash
   npm run serve
   ```

2. **代理配置正确**（已配置，检查是否生效）
   ```javascript
   '/repa': {
     target: 'http://172.16.124.1:8686',
     changeOrigin: true,
     // ...
   }
   ```

3. **使用相对路径请求**（已正确）
   ```javascript
   const url = `/repa/task/run`; // ✅ 正确，会走代理
   // 不要使用：const url = `http://172.16.124.1:8686/repa/task/run`; // ❌ 错误
   ```

4. **重启开发服务器**
   修改 `vue.config.js` 后需要重启：
   ```bash
   # 停止当前服务（Ctrl+C）
   npm run serve
   ```

### 方案4：临时解决方案（仅用于测试）
如果无法修改服务端，可以临时使用浏览器插件禁用CORS（**仅用于开发测试，不要用于生产环境**）

## 推荐解决步骤

### 步骤1：检查开发服务器
```bash
# 确保开发服务器运行在8890端口
npm run serve
# 访问 http://localhost:8890 确认服务正常
```

### 步骤2：检查代理是否生效
打开浏览器开发者工具（F12）：
- Network标签 → 查看请求
- 如果请求URL是 `http://localhost:8890/repa/task/run`，说明代理未生效
- 如果请求URL是 `http://172.16.124.1:8686/repa/task/run`，说明直接访问了目标服务器

### 步骤3：修改服务端CORS配置（最佳方案）
找到服务端CORS配置文件（根据您提供的图片，应该是某个Spring配置类），添加8890端口：

```java
@Bean
public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration cfg = new CorsConfiguration();
    cfg.setAllowedOrigins(List.of(
        "http://127.0.0.1:5500", 
        "http://localhost:5500", 
        "http://172.16.123.81:8890/",
        "http://localhost:8890",      // 新增
        "http://127.0.0.1:8890"       // 新增
    ));
    cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    cfg.setAllowedHeaders(List.of("*"));
    cfg.setExposedHeaders(List.of("Content-Disposition"));
    cfg.setAllowCredentials(true);
    
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/repa/**", cfg);
    source.registerCorsConfiguration("/ecoval/**", cfg);
    return source;
}
```

### 步骤4：重启服务
- 重启服务端应用
- 重启Vue开发服务器（如果修改了vue.config.js）

## 验证方法

1. **检查请求头**
   打开浏览器开发者工具 → Network → 查看请求头：
   - `Origin: http://localhost:8890` 应该被服务端接受

2. **检查响应头**
   查看响应头应该包含：
   - `Access-Control-Allow-Origin: http://localhost:8890`
   - `Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS`

3. **测试请求**
   运行模型，应该不再出现CORS 403错误

## 注意事项

1. **生产环境配置**
   生产环境部署时，需要将实际的前端域名添加到CORS允许列表中

2. **安全性**
   开发环境可以使用 `*` 允许所有源，但生产环境应该明确指定允许的源

3. **代理vs直接访问**
   - 开发环境：使用代理（相对路径 `/repa/...`）
   - 生产环境：可能需要直接访问或使用Nginx反向代理

## 相关文件

- 前端请求代码：`ruoyi-ui/src/views/project/project_service_case/index.vue` (第441行)
- 代理配置：`ruoyi-ui/vue.config.js` (第47-61行)
- 服务端CORS配置：需要找到您图片中显示的配置文件（可能是独立的服务配置）

## 实际解决方案（针对当前IP）

根据您的实际情况，当前前端运行在：`http://172.16.123.81:8890/`

### 服务端CORS配置建议

在服务端CORS配置中添加（如果还没有）：

```java
cfg.setAllowedOrigins(List.of(
    "http://127.0.0.1:5500", 
    "http://localhost:5500", 
    "http://172.16.123.81:8890/",  // ✅ 已包含（带斜杠）
    "http://172.16.123.81:8890"   // ✅ 建议同时添加（不带斜杠，更安全）
));
```

**注意**：浏览器发送的 Origin 头通常是 `http://172.16.123.81:8890`（不带末尾斜杠），但服务端配置是 `http://172.16.123.81:8890/`（带斜杠）。为了兼容，建议同时添加两种格式。

### 启动和验证步骤

1. **启动前端开发服务器**
   ```bash
   npm run dev
   # 或
   npm run serve
   ```
   访问：`http://172.16.123.81:8890/`

2. **确认服务端CORS配置已更新**
   - 确保服务端已添加 `http://172.16.123.81:8890/` 或 `http://172.16.123.81:8890`
   - 重启服务端应用使配置生效

3. **验证代理配置**
   - 打开浏览器开发者工具（F12）
   - Network 标签 → 运行模型
   - 查看请求URL应该是：`http://172.16.123.81:8890/repa/task/run`
   - 查看请求头中的 `Origin: http://172.16.123.81:8890`
   - 查看响应头应该包含：`Access-Control-Allow-Origin: http://172.16.123.81:8890` 或 `http://172.16.123.81:8890/`

4. **测试运行模型**
   - 如果不再出现 CORS 403 错误，说明配置成功

