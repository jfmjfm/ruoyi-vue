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
            <div class="knob"><div class="knob-line"></div></div>
            <div class="knob"><div class="knob-line"></div></div>
          </div>
          <div class="knobs-row">
            <div class="knob"><div class="knob-line"></div></div>
            <div class="knob"><div class="knob-line"></div></div>
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
      currentMapType: 'image' // 'image' for satellite imagery, 'vector' for vector map
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
          duration: 500
        });
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
  justify-content: space-between;
  min-width: 150px;
  max-width: 300px;
  width: 20%;
}

/* Knobs Section */
.knobs-container {
  display: flex;
  flex-direction: column;
  gap: 15px;
  margin-bottom: 20px;
  height: 30%;
}

.knobs-row {
  display: flex;
  justify-content: space-around;
}

.knob {
  width: calc(100% / 2 - 20px);
  aspect-ratio: 1 / 1;
  background-color: #333;
  border-radius: 50%;
  position: relative;
  box-shadow: 0 5px 10px rgba(0, 0, 0, 0.5), inset 0 0 5px rgba(0, 0, 0, 0.5);
  border: 1px solid #222;
}

.knob-line {
  position: absolute;
  top: 10%;
  left: 50%;
  width: 4px;
  height: 80%;
  background-color: #fff;
  transform: translateX(-50%);
}

/* Buttons Panel */
.buttons-panel {
  background-color: #333;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  flex-direction: column;
  flex: 1;
  justify-content: center;
  box-shadow: inset 0 0 10px rgba(0, 0, 0, 0.5);
  border: 1px solid #222;
  width: 100%;
  height: 70%;
}

.button-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(5, 1fr);
  gap: 8px;
  width: 100%;
  height: 100%;
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
