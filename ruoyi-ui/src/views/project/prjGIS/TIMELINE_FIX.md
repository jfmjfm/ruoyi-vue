# 时间轴切换问题修复说明

## 问题描述

在年尺度下，当切换功能菜单或点击时间轴时，地图图层一直停留在第一帧，不会随时间变化而更新。

## 问题原因

### 根本原因

代码在处理年尺度和月尺度时使用了不一致的索引逻辑：

**年尺度：**
- `currentYear` 的值是实际的年份（如 2009, 2010, 2011...）
- 时间序列数组的长度通常只有 10 个元素（如 2009-2018 年）

**月尺度：**
- `currentYear` 的值是月份索引（0, 1, 2...）
- 时间序列数组的长度可能有 120 个元素

### 错误代码

```javascript
// ❌ 错误的做法
const currentTime = timeSequence[yearOrIndex] || scaleConfig.time[0];

// 当 yearOrIndex = 2009 时，访问 timeSequence[2009]
// 但数组长度只有 10，所以总是返回 undefined
// 最后回退到 scaleConfig.time[0]，始终是第一帧
```

### 示例说明

假设时间序列配置：
- 起始时间：2009-01-01
- 结束时间：2018-12-01
- 持续时间：10 年
- 时间序列数组：`['2009-01-01', '2010-01-01', ..., '2018-01-01']`（长度 = 10）

当用户点击时间轴的 2015 年时：
```javascript
yearOrIndex = 2015  // 实际年份
timeSequence[2015]  // ❌ 访问第 2015 个元素，超出数组边界
// 返回 undefined，回退到第一帧 '2009-01-01'
```

正确的做法：
```javascript
yearOrIndex = 2015
startYear = 2009
index = 2015 - 2009 = 6  // 正确的索引
timeSequence[6]  // ✅ 返回 '2015-01-01'
```

## 修复方案

### 修改的代码

**1. `loadWMSLayer()` 方法**

```javascript
// 获取当前帧的时间
let currentTime;
if (currentScale === 'yearly') {
  // 年尺度：yearOrIndex 是实际年份，需要计算相对于起始年份的索引
  const startYear = new Date(scaleConfig.time[0]).getFullYear();
  const index = yearOrIndex - startYear;
  currentTime = timeSequence[index] || scaleConfig.time[0];
} else {
  // 月尺度：yearOrIndex 已经是索引（0开始）
  currentTime = timeSequence[yearOrIndex] || scaleConfig.time[0];
}
```

**2. `loadWMSLayerWithLogging()` 方法**

```javascript
// 获取当前帧的时间
let currentTime;
let timeIndex; // 用于日志输出
if (currentScale === 'yearly') {
  // 年尺度：yearOrIndex 是实际年份，需要计算相对于起始年份的索引
  const startYear = new Date(scaleConfig.time[0]).getFullYear();
  timeIndex = yearOrIndex - startYear;
  currentTime = timeSequence[timeIndex] || scaleConfig.time[0];
} else {
  // 月尺度：yearOrIndex 已经是索引（0开始）
  timeIndex = yearOrIndex;
  currentTime = timeSequence[yearOrIndex] || scaleConfig.time[0];
}
```

**3. 增强的日志输出**

```javascript
console.log(`[WMS] 时间尺度: ${currentScale}`);
console.log(`[WMS] 年份/索引输入: ${yearOrIndex}`);
console.log(`[WMS] 时间序列索引: ${timeIndex} (共 ${timeSequence.length} 帧)`);
console.log(`[WMS] 当前时间: ${currentTime}`);
console.log(`[WMS] 图层: ${overrides.layers}`);
console.log(`[WMS] 样式: ${overrides.styles}`);
console.log(`[WMS] 完整URL: ${fullUrl}`);
```

## 验证修复

### 测试步骤

**1. 测试年尺度切换**

```
操作：
1. 切换到任一功能菜单（如"水源涵养"）
2. 确认时间尺度为"年尺度"
3. 点击时间轴上的 2010 年
4. 观察地图是否更新

预期结果：
✅ 控制台显示：[WMS] 年份/索引输入: 2010
✅ 控制台显示：[WMS] 时间序列索引: 1 (共 10 帧)
✅ 控制台显示：[WMS] 当前时间: 2010-01-01
✅ 地图图层更新为 2010 年的数据
```

**2. 测试年尺度动画**

```
操作：
1. 在年尺度下，点击播放按钮
2. 观察时间轴和地图是否按顺序更新

预期结果：
✅ 时间轴从 2009 逐年递增到 2018
✅ 每一年的地图数据都正确显示
✅ 控制台显示的索引从 0 递增到 9
```

**3. 测试月尺度切换**

```
操作：
1. 切换到"月尺度"
2. 点击时间轴上的不同月份
3. 观察地图是否更新

预期结果：
✅ 控制台显示：[WMS] 年份/索引输入: 15（月份索引）
✅ 控制台显示：[WMS] 时间序列索引: 15 (共 120 帧)
✅ 控制台显示：[WMS] 当前时间: 2010-04-01
✅ 地图图层更新为对应月份的数据
```

**4. 测试尺度切换**

```
操作：
1. 在年尺度下点击某一年（如 2015）
2. 切换到月尺度
3. 再切换回年尺度
4. 点击不同的年份

预期结果：
✅ 每次切换都能正确显示对应时间的数据
✅ 索引计算正确
✅ 无错误信息
```

## 日志解读

修复后的日志输出示例：

### 年尺度示例

```javascript
[菜单切换] 服务类型: 0, 服务名称: 水源涵养
[菜单切换] 加载初始时间: 2009
[WMS] 时间尺度: yearly
[WMS] 年份/索引输入: 2009        // 用户输入的年份
[WMS] 时间序列索引: 0 (共 10 帧)  // 计算出的数组索引
[WMS] 当前时间: 2009-01-01       // 实际使用的时间
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-YEARLY
[WMS] 完整URL: http://...

// 用户点击 2015 年
更新地图数据: 服务类型=0, 年份/索引=2015
[WMS] 时间尺度: yearly
[WMS] 年份/索引输入: 2015        // 用户输入的年份
[WMS] 时间序列索引: 6 (共 10 帧)  // 2015 - 2009 = 6
[WMS] 当前时间: 2015-01-01       // ✅ 正确！
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-YEARLY
[WMS] 完整URL: http://...
```

### 月尺度示例

```javascript
// 切换到月尺度
时间尺度变化: 服务类型=0, 新尺度=monthly
[WMS] 时间尺度: monthly
[WMS] 年份/索引输入: 0           // 月份索引
[WMS] 时间序列索引: 0 (共 120 帧) // 直接使用
[WMS] 当前时间: 2009-01-01
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-MONTHLY
[WMS] 完整URL: http://...

// 用户点击第 15 个月
更新地图数据: 服务类型=0, 年份/索引=15
[WMS] 时间尺度: monthly
[WMS] 年份/索引输入: 15          // 月份索引
[WMS] 时间序列索引: 15 (共 120 帧) // 直接使用
[WMS] 当前时间: 2010-04-01       // ✅ 正确！
[WMS] 图层: repa:USLE-147-2-SWAT-296
[WMS] 样式: repa:USLE-147-2-SWAT-296-MONTHLY
[WMS] 完整URL: http://...
```

## 关键要点

### 年尺度 vs 月尺度

| 方面 | 年尺度 | 月尺度 |
|------|--------|--------|
| **currentYear 的值** | 实际年份（2009, 2010...） | 索引（0, 1, 2...） |
| **时间序列长度** | 通常 10-50 | 通常 12-600 |
| **索引计算** | `yearOrIndex - startYear` | `yearOrIndex`（直接使用） |
| **时间格式** | `YYYY-01-01` | `YYYY-MM-DD` |

### 索引计算公式

```javascript
// 年尺度
实际索引 = 当前年份 - 起始年份
例如：2015 - 2009 = 6

// 月尺度
实际索引 = 当前索引（已经是正确的）
例如：15 就是第 15 个月
```

## 相关代码位置

- `loadWMSLayer()` - 第 2045 行
- `loadWMSLayerWithLogging()` - 第 3232 行
- `adjustTimelineByWMSConfig()` - 第 1866 行
- `updateMapLayersByYear()` - 第 3207 行

## 常见问题

### Q: 为什么月尺度不需要计算索引？

A: 因为在 `adjustTimelineByWMSConfig()` 方法中，月尺度的时间轴已经设置为索引模式：
```javascript
if (scaleConfig.scale === 'monthly') {
  this.minYear = 0;                      // 从 0 开始
  this.maxYear = scaleConfig.duration - 1; // 到 duration-1
  this.currentYear = 0;
}
```

### Q: 为什么年尺度使用实际年份而不是索引？

A: 这样更直观，用户看到的时间轴显示的是实际年份（2009, 2010...），而不是索引（0, 1, 2...）。

### Q: 如果起始年份不是整数会怎样？

A: `getFullYear()` 总是返回整数年份，所以不会有问题。但要注意时间序列生成时应该保持一致。

## 相关文档

- [WMS_DEBUG_LOGGING.md](./WMS_DEBUG_LOGGING.md) - WMS 调试日志说明
- [UPDATE_NOTES.md](./UPDATE_NOTES.md) - 数据格式更新说明
- [case_dir_example.json](./case_dir_example.json) - 配置示例

---

修复完成！现在时间轴切换应该能正常工作了。🎉

