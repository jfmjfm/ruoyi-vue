# 双缓冲机制可视化指南

## 目录

1. [系统架构图](#系统架构图)
2. [双缓冲状态机](#双缓冲状态机)
3. [时间序列动画](#时间序列动画)
4. [内存管理流程](#内存管理流程)
5. [性能对比图表](#性能对比图表)
6. [调试可视化工具](#调试可视化工具)

---

## 系统架构图

### 整体架构（三层模型）

```
┌─────────────────────────────────────────────────────────────────────┐
│                           用户界面层 (UI Layer)                       │
│  ┌───────────┐  ┌───────────┐  ┌───────────┐  ┌──────────────┐    │
│  │ 时间滑块   │  │ 播放按钮   │  │ 速度控制   │  │ 时间标签      │    │
│  └───────────┘  └───────────┘  └───────────┘  └──────────────┘    │
└────────────────────────────┬────────────────────────────────────────┘
                             │ 用户交互事件
                             ↓
┌─────────────────────────────────────────────────────────────────────┐
│                      双缓冲管理层 (Buffer Manager)                    │
│                                                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  状态管理                                                      │  │
│  │  • wmsLayers: [Layer A, Layer B]                             │  │
│  │  • wmsCurrentLayerIndex: 0 | 1                               │  │
│  │  • wmsTransitionMs: 800                                      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                                                                       │
│  ┌───────────────────┐              ┌────────────────────┐          │
│  │  图层A (前缓冲)    │◄─────交替────►│  图层B (后缓冲)     │          │
│  │  ┌─────────────┐ │              │  ┌─────────────┐  │          │
│  │  │ 当前帧显示   │ │              │  │ 下一帧加载   │  │          │
│  │  │ opacity: 1  │ │              │  │ opacity: 0→1│  │          │
│  │  └─────────────┘ │              │  └─────────────┘  │          │
│  └───────────────────┘              └────────────────────┘          │
└────────────────────────────┬────────────────────────────────────────┘
                             │ OpenLayers API
                             ↓
┌─────────────────────────────────────────────────────────────────────┐
│                      地图渲染层 (Map Renderer)                        │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                  OpenLayers Map Instance                      │  │
│  │                                                               │  │
│  │  Layer Stack (从下到上):                                      │  │
│  │  ┌────────────────────────────────────────────────────┐     │  │
│  │  │ 0. 底图 (天地图/卫星影像) zIndex: 0                 │     │  │
│  │  ├────────────────────────────────────────────────────┤     │  │
│  │  │ 1. 图层A (ImageWMS) zIndex: 100                   │     │  │
│  │  ├────────────────────────────────────────────────────┤     │  │
│  │  │ 2. 图层B (ImageWMS) zIndex: 100                   │     │  │
│  │  ├────────────────────────────────────────────────────┤     │  │
│  │  │ 3. 矢量标注层 (Vector) zIndex: 200                 │     │  │
│  │  └────────────────────────────────────────────────────┘     │  │
│  └──────────────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────────────┘
                             │ HTTP WMS请求
                             ↓
┌─────────────────────────────────────────────────────────────────────┐
│                       数据服务层 (Data Service)                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │                      GeoServer WMS Endpoint                   │  │
│  │                                                               │  │
│  │  URL: /wms?LAYERS=xxx&TIME=2024-01-01&FORMAT=image/png      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│                             ↓                                        │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              NetCDF 数据集 (时间维度)                          │  │
│  │  • 时间范围: 1980-01-01 ~ 2050-12-31                          │  │
│  │  • 时间步长: 每日                                             │  │
│  │  • 空间范围: 115.5°E-119.6°E, 39.7°N-42.7°N                  │  │
│  │  • 变量: 降水、径流、蒸发、侵蚀...                              │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 双缓冲状态机

### 状态转换图（完整生命周期）

```
                  ┌──────────────┐
                  │   初始状态    │
                  │  wmsLayers   │
                  │  = [null,    │
                  │     null]    │
                  └──────┬───────┘
                         │ initWmsFirstFrame()
                         ↓
                  ┌──────────────┐
                  │   状态A-0     │
                  │ [Layer(T0),  │  ← T0 = 2024-01-01
                  │  null]       │
                  │ index = 0    │
                  └──────┬───────┘
                         │ switchToWmsDate(T1)
                         │
                  ┌──────▼────────┐
                  │  过渡期A→B-1   │
                  │ [Layer(T0),   │  ← T0显示，T1加载中
                  │  Layer(T1)↻]  │
                  │ index = 0     │
                  └──────┬────────┘
                         │ CSS transition (800ms)
                         │
                  ┌──────▼────────┐
                  │  过渡期A→B-2   │
                  │ [Layer(T0),   │  ← 两层叠加显示
                  │  Layer(T1)↑]  │    T1 opacity: 0→1
                  │ index = 0     │
                  └──────┬────────┘
                         │ transition完成
                         │ 删除Layer(T0)
                         ↓
                  ┌──────────────┐
                  │   状态B-1     │
                  │ [null,       │
                  │  Layer(T1)]  │  ← T1 = 2024-01-02
                  │ index = 1    │
                  └──────┬───────┘
                         │ switchToWmsDate(T2)
                         │
                  ┌──────▼────────┐
                  │  过渡期B→A-1   │
                  │ [Layer(T2)↻,  │  ← T1显示，T2加载中
                  │  Layer(T1)]   │
                  │ index = 1     │
                  └──────┬────────┘
                         │ CSS transition (800ms)
                         │
                  ┌──────▼────────┐
                  │  过渡期B→A-2   │
                  │ [Layer(T2)↑,  │  ← 两层叠加显示
                  │  Layer(T1)]   │    T2 opacity: 0→1
                  │ index = 1     │
                  └──────┬────────┘
                         │ transition完成
                         │ 删除Layer(T1)
                         ↓
                  ┌──────────────┐
                  │   状态A-2     │
                  │ [Layer(T2),  │
                  │  null]       │  ← T2 = 2024-01-03
                  │ index = 0    │
                  └──────┬───────┘
                         │
                         ↓ 循环继续...
                    (回到"过渡期A→B-1"模式)
```

### 索引切换算法可视化

```
┌─────────────────────────────────────────────────────────────┐
│  算法：nextIndex = 1 - currentIndex                          │
└─────────────────────────────────────────────────────────────┘

步骤1: currentIndex = 0
       nextIndex = 1 - 0 = 1
       
       wmsLayers:  [🟢Layer A,  ⚪empty   ]
       索引:         ↑current   ↑next
       
步骤2: 加载完成，切换索引
       currentIndex = 1
       
       wmsLayers:  [⚪empty,    🟢Layer B ]
       索引:                    ↑current
       
步骤3: 下一帧
       nextIndex = 1 - 1 = 0
       
       wmsLayers:  [⚪empty,    🟢Layer B ]
       索引:         ↑next      ↑current
       
步骤4: 加载完成，再次切换
       currentIndex = 0
       
       wmsLayers:  [🟢Layer C,  ⚪empty   ]
       索引:         ↑current
```

---

## 时间序列动画

### 完整动画周期（单帧切换详细时间线）

```
═════════════════════════════════════════════════════════════════════
时间轴：0ms ────────────────────────────────────────────────► 900ms
═════════════════════════════════════════════════════════════════════

阶段1: 调用 switchToWmsDate()
───────────────────────────────────────────────────────────
t=0ms
┌─────────────────────────────────────────────┐
│ JavaScript执行                               │
│ • 计算 nextIndex = 1                         │
│ • 创建 nextLayer (TIME=2024-01-02)          │
│ • map.addLayer(nextLayer)                   │
└─────────────────────────────────────────────┘
         │
         └──► 触发 WMS HTTP 请求
              GET /wms?TIME=2024-01-02&_cb=1702345678901
              
用户看到: 2024-01-01 (旧帧，完全不变)

═════════════════════════════════════════════════════════════════════
阶段2: 网络传输 + 图像解码
───────────────────────────────────────────────────────────
t=0-200ms
┌─────────────────────────────────────────────┐
│ 网络活动                                     │
│ • DNS查询 (如有缓存则跳过)                    │
│ • TCP连接建立                                │
│ • HTTP请求发送                               │
│ • GeoServer处理NetCDF切片                    │
│ • PNG编码                                    │
│ • 响应传输                                   │
└─────────────────────────────────────────────┘
         │
         └──► 浏览器接收图像数据
         
┌─────────────────────────────────────────────┐
│ 图像解码 (50-100ms)                          │
│ • PNG解压缩                                  │
│ • 像素数据解码                                │
│ • 传递给OpenLayers                           │
└─────────────────────────────────────────────┘

用户看到: 2024-01-01 (旧帧，仍然不变)
网络指示器: 可能显示"加载中..."

═════════════════════════════════════════════════════════════════════
阶段3: OpenLayers渲染
───────────────────────────────────────────────────────────
t=100ms (第一个setTimeout触发)
┌─────────────────────────────────────────────┐
│ DOM操作                                      │
│ • 获取 layerElement (Canvas元素)             │
│ • 设置 transition = "opacity 800ms ease"    │
│ • 设置 opacity = '0' (初始透明)              │
└─────────────────────────────────────────────┘

DOM结构:
<div id="map">
  <canvas id="layer-old" style="opacity: 1">    ← 旧帧
  <canvas id="layer-new" style="opacity: 0">    ← 新帧（透明）
</div>

用户看到: 2024-01-01 (旧帧，新帧已渲染但透明)

═════════════════════════════════════════════════════════════════════
阶段4: 开始淡入过渡
───────────────────────────────────────────────────────────
t=150ms (第二个setTimeout触发，延迟50ms)
┌─────────────────────────────────────────────┐
│ CSS过渡启动                                  │
│ • 设置 opacity = '1'                         │
│ • 浏览器创建合成层                            │
│ • GPU开始alpha混合计算                       │
└─────────────────────────────────────────────┘

浏览器内部机制:
┌────────────────────────────────────┐
│ 合成层管理 (Compositor)             │
│                                    │
│ Layer Stack:                       │
│ ┌────────────────────────────────┐ │
│ │ Layer-new (合成层2)             │ │
│ │ opacity: 0 → 1 (GPU动画)       │ │
│ ├────────────────────────────────┤ │
│ │ Layer-old (合成层1)             │ │
│ │ opacity: 1 (静态)               │ │
│ └────────────────────────────────┘ │
└────────────────────────────────────┘

═════════════════════════════════════════════════════════════════════
阶段5: 过渡动画进行中 (每帧16.67ms @ 60fps)
───────────────────────────────────────────────────────────
t=150ms - 950ms (800ms过渡)

帧序列:
t=150ms:  opacity = 0.00  ▒▒▒▒▒▒▒▒▒▒  旧帧100%
t=250ms:  opacity = 0.15  ▒▒▒▒▒▒▒▒░░  旧帧85% + 新帧15%
t=350ms:  opacity = 0.35  ▒▒▒▒▒▒░░░░  旧帧65% + 新帧35%
t=450ms:  opacity = 0.55  ▒▒▒▒░░░░░░  旧帧45% + 新帧55%
t=550ms:  opacity = 0.72  ▒▒░░░░░░░░  旧帧28% + 新帧72%
t=650ms:  opacity = 0.85  ▒░░░░░░░░░  旧帧15% + 新帧85%
t=750ms:  opacity = 0.94  ░░░░░░░░░░  旧帧6% + 新帧94%
t=850ms:  opacity = 0.98  ░░░░░░░░░░  旧帧2% + 新帧98%
t=950ms:  opacity = 1.00  ░░░░░░░░░░  新帧100%

用户感知: 平滑的淡入过渡，无闪烁

═════════════════════════════════════════════════════════════════════
阶段6: 清理阶段
───────────────────────────────────────────────────────────
t=900ms (第三个setTimeout触发, transitionMs + 100)
┌─────────────────────────────────────────────┐
│ 资源清理                                     │
│ • map.removeLayer(oldLayer)                 │
│ • wmsLayers[0] = null                       │
│ • wmsCurrentLayerIndex = 1                  │
│ • 垃圾回收释放Canvas内存                     │
└─────────────────────────────────────────────┘

最终状态:
wmsLayers: [null, Layer(2024-01-02)]
currentIndex: 1

用户看到: 2024-01-02 (新帧完全显示)

═════════════════════════════════════════════════════════════════════
```

### 对比：无双缓冲的动画（展示问题）

```
═════════════════════════════════════════════════════════════════════
时间轴（无双缓冲）：0ms ─────────────────────────► 200ms
═════════════════════════════════════════════════════════════════════

t=0ms: 删除旧图层
┌──────────────────┐
│ 显示内容: 空白   │ ← ❌ 用户看到底图或白屏
└──────────────────┘

t=0-200ms: 加载新图层
┌──────────────────┐
│ 显示内容: 空白   │ ← ❌ 持续200ms的白屏
└──────────────────┘

t=200ms: 新图层加载完成
┌──────────────────┐
│ 显示内容: 新帧   │ ← ✓ 突然出现（生硬）
└──────────────────┘

用户体验: 闪烁、白屏、跳跃感
```

---

## 内存管理流程

### 内存占用时间曲线

```
内存 (MB)
│
30 │                    ╱╲              ╱╲              ╱╲
   │                   ╱  ╲            ╱  ╲            ╱  ╲
25 │                  ╱    ╲          ╱    ╲          ╱    ╲
   │                 ╱      ╲        ╱      ╲        ╱      ╲
20 │                ╱        ╲      ╱        ╲      ╱        ╲
   │               ╱          ╲    ╱          ╲    ╱          ╲
15 │──────────────╱────────────╲──╱────────────╲──╱────────────╲──
   │
10 │
   │
 5 │
   │
 0 └─┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──────┬──► 时间
     T0    T1     T2     T3     T4     T5     T6     T7     T8

图例:
─────── 单图层内存 (~12MB)
╱╲╱╲╱╲  双图层内存峰值 (~24MB, 过渡期间)
```

### 详细内存分配表

```
┌──────────┬────────────────────────┬──────────┬─────────────┐
│ 时间点   │ 图层状态                │ 内存占用 │ 说明         │
├──────────┼────────────────────────┼──────────┼─────────────┤
│ T0 初始  │ [Layer(T0), null]      │ 12 MB    │ 初始状态     │
├──────────┼────────────────────────┼──────────┼─────────────┤
│ T1 加载  │ [Layer(T0), Layer(T1)] │ 24 MB    │ 峰值（过渡） │
├──────────┼────────────────────────┼──────────┼─────────────┤
│ T1 完成  │ [null, Layer(T1)]      │ 12 MB    │ 释放旧层     │
├──────────┼────────────────────────┼──────────┼─────────────┤
│ T2 加载  │ [Layer(T2), Layer(T1)] │ 24 MB    │ 峰值（过渡） │
├──────────┼────────────────────────┼──────────┼─────────────┤
│ T2 完成  │ [Layer(T2), null]      │ 12 MB    │ 释放旧层     │
├──────────┼────────────────────────┼──────────┼─────────────┤
│ ...      │ ...                    │ ...      │ 循环往复     │
└──────────┴────────────────────────┴──────────┴─────────────┘
```

### 内存泄漏检测清单

```
✅ 检查项1: 图层清理
   if (oldLayer) {
     this.map.removeLayer(oldLayer);  // ✓ 从地图移除
     this.wmsLayers[currentIndex] = null;  // ✓ 引用置空
     // oldLayer.dispose();  // 可选：显式释放（OpenLayers会自动处理）
   }

✅ 检查项2: 定时器清理
   beforeDestroy() {
     clearTimeout(this.wmsAnimationTimer);  // ✓ 清除定时器
   }

✅ 检查项3: 事件监听器清理
   // 如果添加了自定义事件监听
   layer.getSource().un('imageloadend', this.handleLoad);

✅ 检查项4: Canvas上下文释放
   // OpenLayers内部处理，无需手动操作
   // 但确保没有外部引用持有Canvas元素

❌ 常见泄漏场景:
   • 忘记清空数组元素: wmsLayers[i] 仍然引用旧对象
   • 定时器未清除: setTimeout/setInterval持续运行
   • 闭包引用: 回调函数持有大对象引用
```

---

## 性能对比图表

### 帧率对比（60fps为目标）

```
帧率 (fps)
│
60 │██████████████████████████████████████  双缓冲+CSS transition
   │
50 │
   │
40 │
   │
30 │████████████████░░░░░░░░░░░░░░░░░░░░  单缓冲+直接替换
   │
20 │
   │
10 │
   │
 0 └─┬────────────────┬────────────────┬────────────────┬──► 测试场景
     普通网络         慢速网络        快速切换         移动设备
     (5Mbps)         (2Mbps)        (500ms/帧)       (低端GPU)
```

### CPU占用率对比

```
CPU使用率 (%)
│
50 │
   │
40 │                 ████████████████████████  Canvas手动绘制
   │
30 │
   │
20 │          ████████████████  单缓冲方案
   │
10 │   ███████  双缓冲CSS方案
   │
 0 └─┴─────────────────────────────────────────────────────► 方案
     CSS   单缓冲   Canvas
     双缓冲 替换    手动绘制
```

### 视觉质量指标（SSIM相似度）

```
SSIM值 (0-1, 越高越好)
│
1.0 │██████████████████████████████  双缓冲（>0.89）
    │
0.8 │
    │
0.6 │
    │
0.4 │
    │
0.2 │████████  单缓冲（白屏期间降至0.12）
    │
0.0 └─┴─────────────────────────────────────────────────────►
      过渡期间         白屏期           恢复期

说明: SSIM > 0.85 为人眼感知连续的阈值
```

### 内存占用对比

```
内存 (MB)
│
100 │                              ████████████████  预加载5帧
    │
 80 │
    │
 60 │                 ████████████  预加载3帧
    │
 40 │        ████████  双缓冲（峰值24MB）
    │
 20 │   ████  单缓冲（12MB）
    │
  0 └─┴──────────────────────────────────────────────────────►
      单缓冲  双缓冲  预加载3帧  预加载5帧
```

---

## 调试可视化工具

### 浏览器开发者工具配置

#### Chrome DevTools - Performance Timeline

```
═══════════════════════════════════════════════════════════════
Performance Recording (switchToWmsDate执行)
═══════════════════════════════════════════════════════════════

Main Thread:
├─ switchToWmsDate (function call) ────────┐
│  ├─ formatDate                           │ 0.2ms
│  ├─ createWmsLayer                       │ 1.5ms
│  │  └─ new ImageWMS                      │
│  ├─ map.addLayer                         │ 3.8ms
│  │  └─ trigger WMS request ──────────────┼─► Network
│  └─ setTimeout callbacks                 │
│     ├─ [100ms] DOM access                │ 0.3ms
│     ├─ [150ms] opacity transition start  │ 0.1ms
│     └─ [900ms] cleanup                   │ 2.1ms
└───────────────────────────────────────────┘

Compositor Thread (GPU):
├─ CSS Transition (opacity)
│  ├─ Create compositing layer              2.3ms
│  ├─ GPU texture upload                    8.5ms
│  └─ Alpha blending (800ms)
│     ├─ Frame 1 (16.67ms) ──► Present
│     ├─ Frame 2 (16.67ms) ──► Present
│     ├─ ...
│     └─ Frame 48 (16.67ms) ─► Present
└────────────────────────────────────────────

Network:
├─ WMS Request
│  ├─ DNS Lookup                            0ms (cached)
│  ├─ TCP Connection                        15ms
│  ├─ Request Sent                          1ms
│  ├─ Waiting (TTFB)                        120ms  ← GeoServer处理
│  ├─ Content Download                      45ms
│  └─ Total                                 181ms
└────────────────────────────────────────────
```

#### Chrome DevTools - Layers Panel

```
═══════════════════════════════════════════════════════════════
Layers (过渡期间快照)
═══════════════════════════════════════════════════════════════

Root Layer
├─ Compositing Layer #1 (oldLayer)
│  ├─ Size: 1920×1080px
│  ├─ Memory: 8.3 MB
│  ├─ Opacity: 1.0
│  ├─ Paint Count: 1
│  └─ Reason: Active CSS opacity animation
│
├─ Compositing Layer #2 (newLayer)
│  ├─ Size: 1920×1080px
│  ├─ Memory: 8.3 MB
│  ├─ Opacity: 0.0 → 1.0 (animating)  ← GPU动画
│  ├─ Paint Count: 1
│  └─ Reason: Active CSS opacity animation
│
└─ Total GPU Memory: 16.6 MB
```

### 自定义调试工具（可添加到代码中）

#### 1. 帧切换日志工具

```javascript
class DebugLogger {
  constructor() {
    this.logs = [];
    this.startTime = Date.now();
  }
  
  log(event, data) {
    const timestamp = Date.now() - this.startTime;
    this.logs.push({ timestamp, event, data });
    
    console.log(
      `%c[${timestamp}ms] ${event}`,
      'color: #3498db; font-weight: bold',
      data
    );
  }
  
  visualize() {
    console.table(this.logs);
  }
}

// 使用示例
const debugLogger = new DebugLogger();

switchToWmsDate(dateObj) {
  debugLogger.log('切换开始', { date: dateObj, currentIndex: this.wmsCurrentLayerIndex });
  
  const nextIndex = 1 - this.wmsCurrentLayerIndex;
  debugLogger.log('计算索引', { nextIndex });
  
  const nextLayer = this.createWmsLayer(dateStr);
  debugLogger.log('创建图层', { layerId: nextLayer.ol_uid });
  
  this.map.addLayer(nextLayer);
  debugLogger.log('添加到地图', { totalLayers: this.map.getLayers().getLength() });
  
  // ... 其他操作 ...
}
```

**输出示例:**
```
[0ms] 切换开始 { date: '2024-01-02', currentIndex: 0 }
[2ms] 计算索引 { nextIndex: 1 }
[5ms] 创建图层 { layerId: 'layer_123' }
[12ms] 添加到地图 { totalLayers: 3 }
[112ms] 开始过渡 { opacity: 0 }
[162ms] 执行淡入 { opacity: 1 }
[912ms] 清理完成 { removedLayer: 'layer_122' }
```

#### 2. 内存监控工具

```javascript
class MemoryMonitor {
  constructor(map) {
    this.map = map;
    this.samples = [];
  }
  
  sample() {
    const memory = {
      timestamp: Date.now(),
      layers: this.map.getLayers().getLength(),
      jsHeap: 0,
      canvasCount: 0
    };
    
    if (performance.memory) {
      memory.jsHeap = (performance.memory.usedJSHeapSize / 1048576).toFixed(2);
    }
    
    memory.canvasCount = document.querySelectorAll('canvas').length;
    
    this.samples.push(memory);
    return memory;
  }
  
  report() {
    console.log('═══════════════════════════════════════');
    console.log('内存监控报告');
    console.log('═══════════════════════════════════════');
    console.table(this.samples);
    
    const avgHeap = this.samples.reduce((sum, s) => sum + parseFloat(s.jsHeap), 0) / this.samples.length;
    console.log(`平均JS堆内存: ${avgHeap.toFixed(2)} MB`);
  }
  
  startAutoSample(interval = 1000) {
    this.intervalId = setInterval(() => {
      const mem = this.sample();
      console.log(`[内存] ${mem.jsHeap} MB, 图层数: ${mem.layers}, Canvas数: ${mem.canvasCount}`);
    }, interval);
  }
  
  stopAutoSample() {
    clearInterval(this.intervalId);
  }
}

// 使用示例
const memMonitor = new MemoryMonitor(this.map);
memMonitor.startAutoSample(2000); // 每2秒采样
```

#### 3. 可视化面板（HTML覆盖层）

```html
<!-- 添加到页面 -->
<div id="debug-panel" style="position: fixed; top: 10px; right: 10px; 
     background: rgba(0,0,0,0.8); color: #0f0; padding: 15px; 
     font-family: monospace; font-size: 12px; z-index: 9999;">
  <div>当前索引: <span id="dbg-index">0</span></div>
  <div>图层A: <span id="dbg-layerA">2024-01-01</span></div>
  <div>图层B: <span id="dbg-layerB">null</span></div>
  <div>状态: <span id="dbg-status">空闲</span></div>
  <div>过渡进度: <span id="dbg-progress">0%</span></div>
  <canvas id="dbg-timeline" width="300" height="50"></canvas>
</div>
```

```javascript
class DebugPanel {
  constructor() {
    this.timeline = document.getElementById('dbg-timeline').getContext('2d');
    this.transitions = [];
  }
  
  update(state) {
    document.getElementById('dbg-index').textContent = state.currentIndex;
    document.getElementById('dbg-layerA').textContent = state.layerA || 'null';
    document.getElementById('dbg-layerB').textContent = state.layerB || 'null';
    document.getElementById('dbg-status').textContent = state.status;
  }
  
  recordTransition(startTime, endTime) {
    this.transitions.push({ start: startTime, end: endTime });
    this.drawTimeline();
  }
  
  drawTimeline() {
    const ctx = this.timeline;
    const width = 300;
    const height = 50;
    
    ctx.clearRect(0, 0, width, height);
    
    // 绘制时间轴
    ctx.strokeStyle = '#333';
    ctx.beginPath();
    ctx.moveTo(0, height / 2);
    ctx.lineTo(width, height / 2);
    ctx.stroke();
    
    // 绘制过渡事件
    const now = Date.now();
    const timeWindow = 10000; // 10秒窗口
    
    this.transitions.forEach(trans => {
      const x = ((trans.start - (now - timeWindow)) / timeWindow) * width;
      const w = ((trans.end - trans.start) / timeWindow) * width;
      
      if (x >= 0 && x < width) {
        ctx.fillStyle = '#0f0';
        ctx.fillRect(x, height / 2 - 10, w, 20);
      }
    });
  }
}
```

---

## 性能分析检查清单

### ✅ 运行前检查

```
□ 确认wmsTransitionMs < wmsIntervalMs
□ 测试网络延迟（ping GeoServer）
□ 检查浏览器是否支持CSS transitions
□ 验证NetCDF数据集时间范围
□ 确认CORS配置正确
```

### ✅ 运行中监控

```
□ 打开Chrome DevTools > Performance
□ 记录3-5次帧切换
□ 检查是否有长任务（Long Tasks）
□ 观察GPU进程占用率
□ 监控网络请求队列
□ 检查Console是否有错误
```

### ✅ 性能指标阈值

```
✓ 帧率: ≥ 55 fps (过渡期间)
✓ CPU: ≤ 10% (平均)
✓ 内存: 峰值 ≤ 30MB (双层)
✓ 网络延迟: ≤ 500ms (WMS请求)
✓ 过渡时间: 600-1000ms (用户可接受)
```

---

## 总结

本可视化指南展示了双缓冲机制的：

1. **架构设计:** 三层模型（UI/缓冲管理/渲染）
2. **状态转换:** 完整的状态机和索引切换逻辑
3. **时序分析:** 毫秒级的执行时间线
4. **内存管理:** 峰值和释放的周期模式
5. **性能对比:** 与其他方案的量化比较
6. **调试工具:** 实用的监控和可视化代码

这些图表和工具可以帮助理解、实现和优化双缓冲机制。

---

**提示:** 将这些可视化工具集成到开发环境中，可以实时监控动画性能，及时发现问题。

