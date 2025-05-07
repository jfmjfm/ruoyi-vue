<template>
  <div class="tv-container" ref="tvContainer">
    <div class="tv-frame">
      <div class="tv-screen">
        <div class="tv-content">
          <div id="map" class="map-container"></div>
        </div>
      </div>
      <div class="tv-controls">
        <div class="knobs-container">
          <div class="knobs-row">
            <div class="knob-wrapper">
              <div class="knob-label">现状评估</div>
              <div class="knob" @click="rotateKnob(0)" :style="{transform: `rotate(${knobRotations[0]}deg)`}">
                <div class="knob-line" :style="{transform: `translateX(-50%) rotate(${knobRotations[0]}deg)`}"></div>
                <div class="knob-scale" v-for="n in 12" :key="'knob1-'+n"></div>
                <div class="knob-indicator" :style="{transform: `translateX(-50%) rotate(-${knobRotations[0]}deg)`}" v-if="true">区域选择</div>
              </div>
            </div>
            <div class="knob-wrapper">
              <div class="knob-label">历史演变</div>
              <div class="knob" @click="rotateKnob(1)" :style="{transform: `rotate(${knobRotations[1]}deg)`}">
                <div class="knob-line" :style="{transform: `translateX(-50%) rotate(${knobRotations[1]}deg)`}"></div>
                <div class="knob-scale" v-for="n in 12" :key="'knob2-'+n"></div>
                <div class="knob-indicator" :style="{transform: `translateX(-50%) rotate(-${knobRotations[1]}deg)`}" v-if="true">年份选择</div>
              </div>
            </div>
          </div>
          <div class="knobs-row">
            <div class="knob-wrapper">
              <div class="knob-label">未来趋势</div>
              <div class="knob" @click="rotateKnob(2)" :style="{transform: `rotate(${knobRotations[2]}deg)`}">
                <div class="knob-line" :style="{transform: `translateX(-50%) rotate(${knobRotations[2]}deg)`}"></div>
                <div class="knob-scale" v-for="n in 12" :key="'knob3-'+n"></div>
                <div class="knob-indicator" :style="{transform: `translateX(-50%) rotate(-${knobRotations[2]}deg)`}" v-if="true">预测深度</div>
              </div>
            </div>
            <div class="knob-wrapper">
              <div class="knob-label">决策优化</div>
              <div class="knob" @click="rotateKnob(3)" :style="{transform: `rotate(${knobRotations[3]}deg)`}">
                <div class="knob-line" :style="{transform: `translateX(-50%) rotate(${knobRotations[3]}deg)`}"></div>
                <div class="knob-scale" v-for="n in 12" :key="'knob4-'+n"></div>
                <div class="knob-indicator" :style="{transform: `translateX(-50%) rotate(-${knobRotations[3]}deg)`}" v-if="true">方案选择</div>
              </div>
            </div>
          </div>
        </div>
        <div class="buttons-panel">
          <div class="button-grid">
            <div class="button darker" @click="toggleMapType"><div class="button-line"></div></div>
            <div class="button dark" @click="zoomIn"><div class="button-line"></div></div>
            <div class="button light" @click="zoomOut"><div class="button-line"></div></div>
            <div class="button dark" @click="resetMapView"><div class="button-line"></div></div>
            <div class="button dark"><div class="button-line"></div></div>
            <div class="button light"><div class="button-line"></div></div>
            <div class="button light"><div class="button-line"></div></div>
            <div class="button darker"><div class="button-line"></div></div>
            <div class="button light"><div class="button-line"></div></div>
            <div class="button darker"><div class="button-line"></div></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import 'ol/ol.css';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import XYZ from 'ol/source/XYZ';
import { fromLonLat } from 'ol/proj';

export default {
  name: 'BigScreen',
  data() {
    return {
      isFullscreen: false,
      map: null,
      currentMapType: 'image', // 'image' for satellite imagery, 'vector' for vector map
      knobRotations: [0, 45, 90, 135] // 初始旋钮旋转角度
    }
  },
  mounted() {
    this.addFullscreenEventListeners();
    this.enterFullscreen();
    this.initMap();
  },
  beforeDestroy() {
    this.removeFullscreenEventListeners();
    
    // 销毁地图实例
    if (this.map) {
      const layers = this.map.getLayers();
      if (layers) {
        const layerArray = layers.getArray();
        for (let i = layerArray.length - 1; i >= 0; i--) {
          this.map.removeLayer(layerArray[i]);
        }
      }
      
      // 分离目标元素
      this.map.setTarget(null);
      this.map = null;
    }
  },
  methods: {
    rotateKnob(index) {
      // 每次旋转30度
      this.knobRotations[index] = (this.knobRotations[index] + 30) % 360;
      
      // 设置CSS变量以在hover和active状态中保持旋转
      document.documentElement.style.setProperty(`--rotation-${index}`, `${this.knobRotations[index]}deg`);
      
      // 添加旋转动画效果
      const knob = document.querySelectorAll('.knob')[index];
      knob.style.animation = 'none';
      setTimeout(() => {
        knob.style.animation = 'knob-click-rotate 0.5s ease-out';
      }, 10);
      
      // 根据旋钮索引执行不同功能
      switch(index) {
        case 0: // 第一个旋钮控制亮度
          // 这里可以添加控制地图亮度的逻辑
          break;
        case 1: // 第二个旋钮控制对比度
          // 这里可以添加控制地图对比度的逻辑
          break;
        case 2: // 第三个旋钮控制缩放
          if (this.map) {
            const view = this.map.getView();
            const zoom = view.getZoom();
            const newZoom = Math.min(18, Math.max(2, zoom + (this.knobRotations[index] % 60 === 0 ? 1 : 0)));
            view.setZoom(newZoom);
          }
          break;
        case 3: // 第四个旋钮控制旋转
          if (this.map) {
            const view = this.map.getView();
            view.setRotation((this.knobRotations[index] * Math.PI) / 180);
          }
          break;
      }
    },
    initMap() {
      // 创建基础图层 - 天地图影像底图
      const imageryLayer = new TileLayer({
        source: new XYZ({
          url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=img_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
          maxZoom: 18
        })
      });
      
      // 天地图影像标注图层
      const imageAnnotationLayer = new TileLayer({
        source: new XYZ({
          url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=cia_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
          maxZoom: 18
        })
      });
      
      // 天地图矢量底图
      const vectorLayer = new TileLayer({
        source: new XYZ({
          url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=vec_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
          maxZoom: 18
        }),
        visible: false
      });
      
      // 天地图矢量标注图层
      const vectorAnnotationLayer = new TileLayer({
        source: new XYZ({
          url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=cva_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
          maxZoom: 18
        }),
        visible: false
      });
      
      // 创建地图实例
      this.map = new Map({
        target: 'map',
        layers: [
          imageryLayer,
          imageAnnotationLayer,
          vectorLayer,
          vectorAnnotationLayer
        ],
        view: new View({
          center: fromLonLat([104.06, 30.67]), // 成都坐标，可根据需要修改
          zoom: 5,
          minZoom: 2,
          maxZoom: 18
        })
      });
      
      // 确保地图尺寸正确
      setTimeout(() => {
        this.map.updateSize();
      }, 200);
    },
    
    // 切换底图类型
    toggleMapType() {
      if (this.currentMapType === 'image') {
        // 切换到矢量图
        this.currentMapType = 'vector';
        // 获取图层并设置可见性
        const layers = this.map.getLayers().getArray();
        layers[0].setVisible(false); // 影像底图
        layers[1].setVisible(false); // 影像标注
        layers[2].setVisible(true);  // 矢量底图
        layers[3].setVisible(true);  // 矢量标注
      } else {
        // 切换到影像图
        this.currentMapType = 'image';
        // 获取图层并设置可见性
        const layers = this.map.getLayers().getArray();
        layers[0].setVisible(true);  // 影像底图
        layers[1].setVisible(true);  // 影像标注
        layers[2].setVisible(false); // 矢量底图
        layers[3].setVisible(false); // 矢量标注
      }
    },
    
    addFullscreenEventListeners() {
      document.addEventListener('fullscreenchange', this.fullscreenChangeHandler);
      document.addEventListener('webkitfullscreenchange', this.fullscreenChangeHandler);
      document.addEventListener('mozfullscreenchange', this.fullscreenChangeHandler);
      document.addEventListener('MSFullscreenChange', this.fullscreenChangeHandler);
    },
    removeFullscreenEventListeners() {
      document.removeEventListener('fullscreenchange', this.fullscreenChangeHandler);
      document.removeEventListener('webkitfullscreenchange', this.fullscreenChangeHandler);
      document.removeEventListener('mozfullscreenchange', this.fullscreenChangeHandler);
      document.removeEventListener('MSFullscreenChange', this.fullscreenChangeHandler);
    },
    enterFullscreen() {
      const element = this.$refs.tvContainer;
      if (element.requestFullscreen) {
        element.requestFullscreen();
      } else if (element.webkitRequestFullscreen) {
        element.webkitRequestFullscreen();
      } else if (element.msRequestFullscreen) {
        element.msRequestFullscreen();
      } else if (element.mozRequestFullScreen) {
        element.mozRequestFullScreen();
      }
    },
    exitFullscreen() {
      if (document.exitFullscreen) {
        document.exitFullscreen();
      } else if (document.webkitExitFullscreen) {
        document.webkitExitFullscreen();
      } else if (document.msExitFullscreen) {
        document.msExitFullscreen();
      } else if (document.mozCancelFullScreen) {
        document.mozCancelFullScreen();
      }
    },
    fullscreenChangeHandler() {
      this.isFullscreen = !!document.fullscreenElement || 
        !!document.webkitFullscreenElement || 
        !!document.mozFullScreenElement ||
        !!document.msFullscreenElement;
      
      // 全屏状态变化时更新地图尺寸
      if (this.map) {
        setTimeout(() => {
          this.map.updateSize();
        }, 200);
      }
    },
    zoomIn() {
      if (this.map) {
        const view = this.map.getView();
        const zoom = view.getZoom();
        view.animate({
          zoom: zoom + 1,
          duration: 250
        });
      }
    },
    zoomOut() {
      if (this.map) {
        const view = this.map.getView();
        const zoom = view.getZoom();
        view.animate({
          zoom: zoom - 1,
          duration: 250
        });
      }
    },
    resetMapView() {
      if (this.map) {
        const view = this.map.getView();
        view.animate({
          center: fromLonLat([104.06, 30.67]), // 默认中心点
          zoom: 5,
          rotation: 0,
          duration: 500
        });
        // 重置旋钮角度
        this.knobRotations = [0, 45, 90, 135];
      }
    }
  }
}
</script>

<style scoped>
/* Base Layout */
.tv-container {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100vw;
  height: 100vh;
  background-color: #222;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.tv-frame {
  display: flex;
  background-color: #8B4513;
  border-radius: 20px;
  border: 10px solid #000;
  padding: 10px;
  box-shadow: 0 0 30px rgba(0, 0, 0, 0.7);
  width: 95%;
  height: 95vh;
  margin: 0;
}

/* TV Screen */
.tv-screen {
  flex: 4;
  background-color: #fff;
  border: 6px solid #000;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
  margin-right: 10px;
  box-shadow: inset 0 0 20px rgba(0, 0, 0, 0.3);
}

.tv-content {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 100%;
  text-align: center;
}

/* Map container */
.map-container {
  width: 100%;
  height: 100%;
}

/* Controls Section */
.tv-controls {
  flex: 1;
  background-color: #555;
  border-radius: 0 10px 10px 0;
  padding: 15px;
  display: flex;
  flex-direction: column;
  min-width: 150px;
  max-width: 300px;
  width: 20%;
  box-sizing: border-box;
}

/* Knobs Section */
.knobs-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 20px;
  flex: 0 0 auto;
}

.knobs-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: 20px;
  width: 100%;
}

.knob-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: calc(100% / 2 - 10px);
  transition: all 0.3s ease;
}

.knob-wrapper:hover {
  transform: scale(1.05);
}

.knob-label {
  font-size: 14px;
  color: #f0f0f0;
  margin-bottom: 8px;
  font-weight: 500;
  text-align: center;
  letter-spacing: 1px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.5);
  background: linear-gradient(135deg, #555, #333);
  padding: 5px 10px;
  border-radius: 12px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.3);
  white-space: nowrap;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.knob-label::after {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.2), transparent);
  transition: 0.5s;
}

.knob-wrapper:hover .knob-label::after {
  left: 100%;
}

.knob-wrapper:hover .knob-label {
  background: linear-gradient(135deg, #666, #444);
  transform: translateY(-2px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.4);
}

.knob {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: url('~@/assets/images/rotate.png') no-repeat center center;
  background-size: contain;
  border-radius: 50%;
  position: relative;
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.3);
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.3s ease;
  cursor: pointer;
  transform-origin: center center;
  transform-style: preserve-3d;
  animation: none;
}

.knob:hover {
  box-shadow: 0 6px 10px rgba(0, 0, 0, 0.4);
  animation: knob-hover-rotate 2s ease-in-out infinite;
}

.knob:active {
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.3);
  animation: knob-click-rotate 0.5s ease-out;
}

@keyframes knob-hover-rotate {
  0% {
    transform: rotate(var(--rotation, 0deg));
  }
  25% {
    transform: rotate(calc(var(--rotation, 0deg) + 3deg));
  }
  75% {
    transform: rotate(calc(var(--rotation, 0deg) - 3deg));
  }
  100% {
    transform: rotate(var(--rotation, 0deg));
  }
}

@keyframes knob-click-rotate {
  0% {
    transform: rotate(var(--rotation, 0deg));
  }
  50% {
    transform: rotate(calc(var(--rotation, 0deg) + 15deg));
  }
  100% {
    transform: rotate(var(--rotation, 0deg));
  }
}

/* Remove the transform from the hover effect as it was interfering with the rotation */
.knob:active, .knob:hover {
  /* transform: rotate(var(--rotation, 0deg)); */
}

/* Remove the before and after pseudo-elements since we're now using an image */
.knob::before, .knob::after {
  display: none;
}

/* Hide the knob line since the image already has its own indicator */
.knob-line {
  display: none;
}

/* Hide the knob scales since the image already has marks */
.knob-scale {
  display: none;
}

/* 旋钮指示器 */
.knob-indicator {
  display: none; /* 隐藏指示器 */
  position: absolute;
  bottom: -20px;
  left: 50%;
  transform: translateX(-50%) rotate(0deg);
  font-size: 11px;
  color: #fff;
  background: linear-gradient(135deg, #ff5722, #e64a19);
  padding: 3px 8px;
  border-radius: 10px;
  white-space: nowrap;
  z-index: 10;
  opacity: 0;
  transform-style: preserve-3d;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
  border: 1px solid rgba(255, 255, 255, 0.2);
  transition: all 0.3s ease;
  pointer-events: none;
}

/* 为每个旋钮添加对应的指示器样式 */
.knob:nth-child(1) .knob-indicator {
  transform: translateX(-50%) rotate(-var(--rotation-0, 0deg));
}

.knob:nth-child(2) .knob-indicator {
  transform: translateX(-50%) rotate(-var(--rotation-1, 45deg));
}

.knob:nth-child(3) .knob-indicator {
  transform: translateX(-50%) rotate(-var(--rotation-2, 90deg));
}

.knob:nth-child(4) .knob-indicator {
  transform: translateX(-50%) rotate(-var(--rotation-3, 135deg));
}

.knob:hover .knob-indicator {
  opacity: 0; /* 确保在悬停状态下指示器也不显示 */
  display: none; /* 确保在悬停状态下指示器也不显示 */
}

/* Buttons Panel */
.buttons-panel {
  background-color: #333;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1 1 auto;
  justify-content: center;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.5);
  border: 1px solid #222;
  width: 100%;
  margin-top: 15px;
  box-sizing: border-box;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(5, 1fr);
  gap: 8px;
  width: 100%;
  height: 100%;
  min-height: 240px;
}

.button {
  width: 100%;
  height: 100%;
  border-radius: 6px;
  position: relative;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.4);
  border: 1px solid #222;
  transition: all 0.2s ease;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
  margin: 0;
  overflow: hidden;
}

.button:hover {
  filter: brightness(1.1);
  transform: translateY(-1px);
  cursor: pointer;
}

.dark {
  background-color: #444;
}

.light {
  background-color: #555;
}

.darker {
  background-color: #222;
}

.button-line {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 50%;
  height: 3px;
  background-color: #777;
  transform: translate(-50%, -50%);
}

/* Fullscreen styles */
:fullscreen .tv-container,
:-webkit-full-screen .tv-container,
:-moz-full-screen .tv-container,
:-ms-fullscreen .tv-container {
  width: 100vw;
  height: 100vh;
}

:fullscreen .tv-frame,
:-webkit-full-screen .tv-frame,
:-moz-full-screen .tv-frame,
:-ms-fullscreen .tv-frame {
  width: 98%;
  height: 98vh;
}

/* Media queries for responsive design */
@media screen and (max-width: 1200px) {
  .tv-controls {
    min-width: 120px;
    width: 22%;
  }
  
  .button-grid {
    gap: 6px;
  }
  
  .knob-label {
    font-size: 12px;
    padding: 4px 8px;
  }
}

@media screen and (max-width: 768px) {
  .tv-controls {
    min-width: 100px;
    width: 25%;
  }
  
  .button-grid {
    gap: 4px;
  }
  
  .knobs-container {
    gap: 10px;
  }
  
  .knob-label {
    font-size: 10px;
    padding: 3px 6px;
    white-space: normal;
    text-align: center;
  }
}

@media screen and (max-height: 600px) {
  .button-grid {
    gap: 4px;
  }
}

/* Remove margin and padding from body and html */
:global(body),
:global(html) {
  margin: 0;
  padding: 0;
  overflow: hidden;
}
</style>
