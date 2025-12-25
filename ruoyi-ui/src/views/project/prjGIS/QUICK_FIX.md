# 快速修复：Extension context invalidated 错误

## 🔴 问题
```
Uncaught Error: Extension context invalidated.
```

## ✅ 快速解决（3个方法，任选其一）

### 方法 1：禁用浏览器扩展（最快）

**Chrome/Edge:**
1. 地址栏输入: `chrome://extensions/` 或 `edge://extensions/`
2. 禁用所有扩展（特别是 Vue DevTools、AdBlock）
3. 刷新页面

**Firefox:**
1. 地址栏输入: `about:addons`
2. 禁用所有扩展
3. 刷新页面

### 方法 2：使用无痕/隐私模式（最简单）

- **Windows**: `Ctrl + Shift + N`
- **Mac**: `Cmd + Shift + N`

### 方法 3：刷新页面（代码已修复）

直接刷新页面，错误会被自动忽略，不影响功能。

## 📝 说明

- ❌ **不是代码错误** - 是浏览器扩展的问题
- ✅ **功能正常** - 代码已添加错误处理
- ⚠️ **控制台警告** - 可能看到黄色警告，可以忽略

## 📖 详细文档

查看 [EXTENSION_ERROR_FIX.md](./EXTENSION_ERROR_FIX.md) 了解详细信息。

