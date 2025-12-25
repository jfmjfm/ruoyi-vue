# 地图服务数据格式更新说明

## 更新概述

数据库字段 `case_dir` 的格式已从旧的 `URL|||JSON` 格式更新为完整的 JSON 格式，以支持更灵活和完整的 WMS 服务配置。

## 新的 JSON 格式

### 顶层结构

```json
{
  "url_info": { ... },      // WMS 服务的完整配置信息
  "scales_info": [ ... ]    // 不同时间尺度的配置数组
}
```

### url_info 字段

包含所有 WMS GetMap 请求所需的参数：

- **base_url**: WMS 服务的基础 URL
- **service**: 服务类型（通常为 "WMS"）
- **version**: WMS 版本（如 "1.1.0" 或 "1.3.0"）
- **request**: 请求类型（通常为 "GetMap"）
- **layers**: 图层名称
- **styles**: 样式名称
- **srs/crs**: 坐标参考系统（如 "EPSG:4326"）
- **bbox**: 边界框坐标（格式: "minx,miny,maxx,maxy"）
- **width**: 图像宽度（像素）
- **height**: 图像高度（像素）
- **format**: 输出格式（如 "image/png"）
- **transparent**: 是否透明（"true" 或 "false"）
- **bgcolor**: 背景颜色
- **viewparams**: 视图参数对象
  - **sim_time**: 模拟时间（如 "2009-01-01"）
  - **scale**: 时间尺度（"yearly" 或 "monthly"）
- **env**: 环境变量对象
- **dimensions**: 维度参数对象
- **custom_params**: 自定义参数对象

### scales_info 数组

包含不同时间尺度的配置，每个配置对象包含：

- **time**: 时间范围数组 `[起始时间, 结束时间]`（格式: "YYYY-MM-DD"）
- **layers**: 该尺度使用的图层名称
- **styles**: 该尺度使用的样式名称
- **levels**: 分级值数组（用于图例显示，从低到高）
- **scale**: 时间尺度标识（"daily"、"monthly" 或 "yearly"）
- **duration**: 时间帧数（总共有多少个时间点）

## 示例数据

完整示例请参见 `case_dir_example.json` 文件。

```json
{
  "url_info": {
    "base_url": "http://172.16.124.205:31490/geoserver/repa/wms",
    "service": "WMS",
    "version": "1.1.0",
    "request": "GetMap",
    "layers": "repa:USLE-147-2-SWAT-296",
    "styles": "repa:USLE-147-2-SWAT-296-YEARLY",
    "srs": "EPSG:4326",
    "bbox": "118.41667877737004,44.45291778219248,119.14330242199273,45.23305354431724",
    "width": 1024,
    "height": 768,
    "format": "image/png",
    "transparent": "false",
    "viewparams": {
      "sim_time": "2009-01-01",
      "scale": "yearly"
    }
  },
  "scales_info": [
    {
      "time": ["2009-01-01", "2018-12-01"],
      "layers": "USLE-147-2-SWAT-296",
      "styles": "USLE-147-2-SWAT-296-MONTHLY",
      "levels": [0.906, 3.486, 7.585, 14.008, 23.118, 38.938, 57.002, 77.798, 97.52, 145.099],
      "scale": "monthly",
      "duration": 120
    },
    {
      "time": ["2009-01-01", "2018-12-01"],
      "layers": "USLE-147-2-SWAT-296",
      "styles": "USLE-147-2-SWAT-296-YEARLY",
      "levels": [0.06, 0.186, 0.354, 0.562, 0.789, 1.028, 1.244, 1.493, 1.775, 1.9325],
      "scale": "yearly",
      "duration": 10
    }
  ]
}
```

## 代码更新说明

### 主要更新的方法

1. **parseCaseDir(caseDir)**
   - 直接解析 JSON 格式的 `case_dir` 字段
   - 返回包含 `urlInfo`、`scalesInfo` 等字段的配置对象

2. **buildWMSUrl(urlInfo, overrides)**
   - 根据 `url_info` 构建完整的 WMS GetMap URL
   - 支持参数覆盖（用于动态更新时间、图层、样式等）
   - 自动处理 WMS 1.3.0 的坐标顺序问题

3. **generateTimeSequence(startTime, endTime, scale, duration)**
   - 根据起始时间、结束时间、时间尺度和持续时间生成时间序列
   - 支持 daily、monthly、yearly 三种尺度

4. **adjustTimelineByWMSConfig(wmsConfig, currentScale)**
   - 根据 WMS 配置和当前时间尺度调整时间轴范围
   - 从 `scalesInfo` 数组中查找对应尺度的配置
   - 自动设置时间轴的最小值、最大值和标记

5. **loadWMSLayer(wmsConfig, yearOrIndex, adjustView, currentScale)**
   - 加载 WMS 图层并显示在地图上
   - 根据时间索引和尺度自动选择正确的图层、样式和时间参数
   - 支持新旧两种格式（向后兼容）

## 功能特性

### 多时间尺度支持

系统现在完全支持多个时间尺度（年尺度、月尺度）：

- **年尺度**: 用于显示年度数据，时间轴显示年份
- **月尺度**: 用于显示月度数据，时间轴显示年-月

用户可以通过面板中的"时间尺度"选项在不同尺度之间切换，系统会自动：
- 调整时间轴范围
- 更新图层和样式
- 重新加载 WMS 数据

### 动态图层和样式切换

根据选择的时间尺度，系统会自动使用对应的图层和样式：

- 年尺度使用 `USLE-147-2-SWAT-296-YEARLY` 样式
- 月尺度使用 `USLE-147-2-SWAT-296-MONTHLY` 样式

### 图例自动更新

系统会根据当前时间尺度的 `levels` 数组自动更新图例显示，确保图例与实际显示的数据相匹配。

### 时间动画播放

支持时间序列的动画播放：

- 点击播放按钮开始自动播放
- 系统会按照时间顺序依次显示每一帧
- 到达最后一帧自动停止

## 向后兼容性

代码保留了对旧格式的支持：

- 如果 `case_dir` 包含旧格式的数据（`URL|||JSON`），系统会尝试解析
- 所有关键方法都包含了旧格式的处理逻辑
- 不影响已有的数据和功能

## 数据迁移建议

### 数据库字段更新

将现有的 `case_dir` 字段值从旧格式迁移到新格式：

**旧格式示例：**
```
http://example.com/wms?LAYERS=layer1|||{"duration":10,"units":"year","time_start":"2009-01-01","time_end":"2018-12-01","scales":[...]}
```

**新格式示例：**
```json
{
  "url_info": {
    "base_url": "http://example.com/wms",
    "layers": "layer1",
    ...
  },
  "scales_info": [...]
}
```

### 迁移步骤

1. 解析旧格式的 URL 和 JSON 配置
2. 将 URL 参数提取到 `url_info` 对象
3. 将 `scales` 数组重命名为 `scales_info`
4. 确保每个 scale 配置包含完整的字段（time、layers、styles、levels、scale、duration）
5. 将新的 JSON 对象序列化为字符串存入数据库

## 测试建议

1. **基本功能测试**
   - 验证地图能否正常加载
   - 验证时间轴能否正常显示
   - 验证图例能否正确显示

2. **时间尺度切换测试**
   - 从年尺度切换到月尺度
   - 验证时间轴范围是否正确更新
   - 验证图层和样式是否正确切换

3. **动画播放测试**
   - 点击播放按钮
   - 验证时间是否按顺序递增
   - 验证到达最后一帧是否自动停止

4. **向后兼容测试**
   - 使用旧格式的 `case_dir` 数据
   - 验证功能是否正常工作

## 常见问题

### Q: 如果 viewparams 为空怎么办？

A: 如果 `url_info.viewparams` 为空或不存在，系统会使用 `time` 参数代替。

### Q: 如何添加新的时间尺度？

A: 在 `scales_info` 数组中添加新的配置对象，包含所有必需字段（time、layers、styles、levels、scale、duration）。

### Q: BBOX 坐标顺序问题？

A: `buildWMSUrl` 方法会自动处理 WMS 1.3.0 + EPSG:4326 的坐标顺序问题，无需手动调整。

### Q: 如何调试 WMS 请求？

A: 在浏览器的开发者工具中查看 Network 面板，可以看到实际发送的 WMS 请求 URL。

## 参考资源

- [OGC WMS 规范](https://www.ogc.org/standards/wms)
- [GeoServer WMS 参考](https://docs.geoserver.org/stable/en/user/services/wms/reference.html)
- [OpenLayers 文档](https://openlayers.org/en/latest/apidoc/)

## 联系支持

如有问题或需要帮助，请联系开发团队。

