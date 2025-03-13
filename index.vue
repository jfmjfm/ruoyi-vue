<style scoped>
.app-container {
  position: fixed;
  top: 84px;
  left: 0;
  right: 0;
  bottom: 0;
  overflow: hidden;
}

.menu-container {
  height: 100%;
  overflow-y: auto;
  position: absolute;
  top: 0;
  left: 0;
  bottom: 0;
}

.map-container {
  height: 100%;
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
}

.content-container {
  position: relative;
  width: 100%;
  height: 100%;
}
</style>

<template>
  <div class="app-container">
    <div class="content-container">
      <div class="menu-container" :style="{ width: menuWidth + 'px' }">
        <!-- 功能菜单区域 -->
        // ... existing code ...
      </div>
      <div class="map-container" :style="{ width: 'calc(100% - ' + menuWidth + 'px)', left: menuWidth + 'px' }">
        <!-- 地图区域 -->
        // ... existing code ...
      </div>
    </div>
  </div>
</template>

// ... existing code ... 