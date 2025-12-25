# WMS 调试日志说明

## 功能概述

为了方便追踪地图服务是否正常，系统现在会在控制台输出详细的 WMS 请求信息。

## 日志格式

### 1. 菜单切换时的日志

当你点击右侧功能菜单切换不同服务时，会输出：

```
[菜单切换] 服务类型: 1, 服务名称: 水源供给
[菜单切换] 加载初始时间: 2009
[WMS] 时间尺度: yearly
[WMS] 当前时间: 2009-01-01
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-YEARLY
[WMS] 完整URL: http://172.16.124.205:31490/geoserver/repa/wms?service=WMS&version=1.1.0&request=GetMap&layers=repa%3AUSLE-147-2-SWAT-296&styles=repa%3AUSLE-147-2-SWAT-296-YEARLY&srs=EPSG%3A4326&bbox=118.41667877737004%2C44.45291778219248%2C119.14330242199273%2C45.23305354431724&width=1024&height=768&format=image%2Fpng&transparent=false&bgcolor=0x000000&viewparams=sim_time%3A2009-01-01%3Bscale%3Ayearly
```

### 2. 时间轴变化时的日志

当你点击时间轴或拖动滑块时，会输出：

```
更新地图数据: 服务类型=1, 年份/索引=2010
[WMS] 时间尺度: yearly
[WMS] 当前时间: 2010-01-01
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-YEARLY
[WMS] 完整URL: http://172.16.124.205:31490/geoserver/repa/wms?service=WMS&version=1.1.0&...
```

### 3. 时间尺度切换时的日志

当你在面板中切换时间尺度（月尺度/年尺度）时，会输出：

```
时间尺度变化: 服务类型=1, 新尺度=monthly
时间轴已调整 [月尺度]: 2009-01 开始, 共 120 个月
图层: USLE-147-2-SWAT-296, 样式: USLE-147-2-SWAT-296-MONTHLY
分级值: [0.906, 3.486, 7.585, 14.008, 23.118, 38.938, 57.002, 77.798, 97.52, 145.099]
```

## 日志字段说明

### [WMS] 前缀的日志

| 字段 | 说明 | 示例 |
|------|------|------|
| 时间尺度 | 当前使用的时间尺度 | `yearly` 或 `monthly` |
| 当前时间 | WMS 请求的时间参数 | `2009-01-01` |
| 图层 | GeoServer 图层名称（含命名空间） | `repa:USLE-147-2-SWAT-296` |
| 样式 | GeoServer 样式名称 | `repa:USLE-147-2-SWAT-296-YEARLY` |
| 完整URL | 完整的 WMS GetMap 请求 URL | `http://...?params=...` |

## 如何使用日志进行调试

### 1. 检查 URL 是否正确

复制控制台中的完整 URL，在浏览器新标签页中打开：

- ✅ **成功**: 应该看到地图图层的图像
- ❌ **失败**: 会显示错误信息或空白

### 2. 验证参数是否正确

检查 URL 中的关键参数：

```
✅ layers: 应该与配置的图层名称一致
✅ styles: 应该与当前时间尺度匹配
✅ viewparams: sim_time 应该是当前选择的时间
✅ viewparams: scale 应该是当前的时间尺度
✅ bbox: 边界框坐标应该合理
✅ srs/crs: 坐标系应该是 EPSG:4326
```

### 3. 常见问题排查

#### 问题：地图不显示

**检查步骤：**

1. 查看控制台是否有 `[WMS]` 日志
   - 如果没有 → 检查配置是否加载（查看 `[菜单切换]` 日志）
   - 如果有 → 继续下一步

2. 复制完整 URL 在浏览器中测试
   - 如果返回图像 → 可能是地图渲染问题
   - 如果返回错误 → 检查 GeoServer 配置

3. 检查图层和样式名称
   - 确认 GeoServer 中存在该图层
   - 确认样式名称拼写正确

#### 问题：时间切换没反应

**检查步骤：**

1. 查看是否有 "更新地图数据" 日志
   - 如果没有 → 可能是事件未触发
   - 如果有 → 查看 URL 中的 `sim_time` 参数是否变化

2. 确认时间序列是否正确
   - 年尺度：`sim_time` 应该是 `YYYY-01-01`
   - 月尺度：`sim_time` 应该是 `YYYY-MM-01`

#### 问题：尺度切换失败

**检查步骤：**

1. 查看 "时间尺度变化" 日志
2. 确认 `scales_info` 中有对应尺度的配置
3. 检查样式名称是否包含尺度标识（如 `-YEARLY` 或 `-MONTHLY`）

## 日志级别说明

系统使用不同的日志级别：

- `console.log()` - 📘 **信息**: 正常操作日志
- `console.warn()` - ⚠️ **警告**: 可能的问题，但不影响运行
- `console.error()` - ❌ **错误**: 严重问题，需要修复

## 示例：完整的操作日志

以下是切换服务并调整时间的完整日志示例：

```javascript
// 1. 点击菜单切换到"水源供给"服务
[菜单切换] 服务类型: 1, 服务名称: 水源供给
[菜单切换] 加载初始时间: 2009
时间轴已调整 [年尺度]: 2009 - 2018, 共 10 年
图层: USLE-147-2-SWAT-296, 样式: USLE-147-2-SWAT-296-YEARLY
分级值: [0.06, 0.186, 0.354, 0.562, 0.789, 1.028, 1.244, 1.493, 1.775, 1.9325]
[WMS] 时间尺度: yearly
[WMS] 当前时间: 2009-01-01
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-YEARLY
[WMS] 完整URL: http://172.16.124.205:31490/geoserver/repa/wms?service=WMS&...

// 2. 拖动时间轴到 2015 年
更新地图数据: 服务类型=1, 年份/索引=2015
[WMS] 时间尺度: yearly
[WMS] 当前时间: 2015-01-01
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-YEARLY
[WMS] 完整URL: http://172.16.124.205:31490/geoserver/repa/wms?service=WMS&...

// 3. 切换到月尺度
时间尺度变化: 服务类型=1, 新尺度=monthly
时间轴已调整 [月尺度]: 2009-01 开始, 共 120 个月
图层: USLE-147-2-SWAT-296, 样式: USLE-147-2-SWAT-296-MONTHLY
分级值: [0.906, 3.486, 7.585, ...]
[WMS] 时间尺度: monthly
[WMS] 当前时间: 2009-01-01
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-MONTHLY
[WMS] 完整URL: http://172.16.124.205:31490/geoserver/repa/wms?service=WMS&...
```

## URL 解码工具

如果 URL 太长难以阅读，可以使用以下方法解码：

### 方法 1：在控制台解码

```javascript
// 复制完整的 URL
const encodedUrl = "http://...?param1%3Avalue1%3B...";

// 解码
const decodedUrl = decodeURIComponent(encodedUrl);
console.log(decodedUrl);
```

### 方法 2：在线解码工具

访问以下网站进行 URL 解码：
- https://www.urldecoder.org/
- https://meyerweb.com/eric/tools/dencoder/

### 方法 3：手动解析关键参数

常见的 URL 编码：
- `%3A` = `:`
- `%3B` = `;`
- `%2C` = `,`
- `%2F` = `/`
- `%20` = 空格

## 性能影响

日志输出对性能的影响很小：

- ✅ 只在地图更新时输出
- ✅ 不影响用户体验
- ✅ 可以通过浏览器控制台筛选查看

## 禁用详细日志（可选）

如果在生产环境不需要详细日志，可以设置环境变量：

```javascript
// 在 data() 中添加
debugMode: process.env.NODE_ENV === 'development',

// 在日志输出前检查
if (this.debugMode) {
  console.log('[WMS] 完整URL:', fullUrl);
}
```

## 相关文档

- [UPDATE_NOTES.md](./UPDATE_NOTES.md) - 数据格式更新说明
- [EXTENSION_ERROR_FIX.md](./EXTENSION_ERROR_FIX.md) - 扩展错误处理
- [case_dir_example.json](./case_dir_example.json) - 配置示例

---

希望这些日志能帮助你快速定位和解决地图服务的问题！ 🚀

