<template>
  <div class="dashboard-container">
    <!-- 地图区域 -->
    <div class="map-container">
      <div id="map"></div>
    </div>

    <!-- 功能菜单区域 -->
    <div class="function-menu">
      <div v-for="(item, index) in menuItems" :key="index" 
           class="menu-item" 
           @click="togglePanel(index)"
           :class="{ active: activePanelIndex === index }">
        <i :class="item.icon"></i>
        <div class="menu-title">
          <div>{{ item.nameTop }}</div>
          <div>{{ item.nameBottom }}</div>
        </div>
      </div>
    </div>

    <!-- 控制面板区域 -->
    <transition-group name="panel">
      <div v-for="(item, index) in menuItems" :key="index"
           v-if="activePanelIndex === index"
           class="control-panel">
        <div class="panel-header">
          <span>{{ item.fullName }}</span>
          <div class="close-button" @click="closePanel">
            <i class="el-icon-close"></i>
          </div>
        </div>
        <div class="panel-content">
          <template v-if="index === 0">
            <!-- 图层控制面板 -->
            <div class="search-box">
              <el-input placeholder="搜索图层..." v-model="searchQuery" prefix-icon="el-icon-search"></el-input>
            </div>
            <div class="opacity-control">
              <span>透明度的设定</span>
              <el-slider v-model="opacity" :min="0" :max="100"></el-slider>
            </div>
            <el-switch v-model="showLegend" active-text="图例"></el-switch>
            <div class="layer-list">
              <el-collapse v-model="activeCategories">
                <el-collapse-item v-for="category in categories" :key="category.name" :title="category.name">
                  <!-- 图层列表将在这里展示 -->
                </el-collapse-item>
              </el-collapse>
            </div>
          </template>
        </div>
      </div>
    </transition-group>
  </div>
</template>

<script>
import 'ol/ol.css';
import Map from 'ol/Map';
import View from 'ol/View';
import TileLayer from 'ol/layer/Tile';
import OSM from 'ol/source/OSM';
import { fromLonLat } from 'ol/proj';

export default {
  name: "Index",
  data() {
    return {
      map: null,
      menuItems: [
        { 
          nameTop: '水源',
          nameBottom: '涵养',
          fullName: '水源涵养',
          icon: 'el-icon-s-grid' 
        },
        { 
          nameTop: '土壤',
          nameBottom: '保持',
          fullName: '土壤保持',
          icon: 'el-icon-data-analysis' 
        },
        { 
          nameTop: '防风',
          nameBottom: '固沙',
          fullName: '防风固沙',
          icon: 'el-icon-s-tools' 
        },
        { 
          nameTop: '洪水',
          nameBottom: '调蓄',
          fullName: '洪水调蓄',
          icon: 'el-icon-download' 
        },
        { 
          nameTop: '水质',
          nameBottom: '净化',
          fullName: '水质净化',
          icon: 'el-icon-s-flag' 
        }
      ],
      activePanelIndex: null,
      // 图层控制相关数据
      searchQuery: '',
      opacity: 100,
      showLegend: false,
      activeCategories: ['1'],
      categories: [
        { name: 'Biodiversity and Nature Conservation' },
        { name: 'Climate' },
        { name: 'Elevation and Depth' },
        { name: 'Geology and Soils' },
        { name: 'Land Cover and Land Use' },
        { name: 'Orthoimagery' },
        { name: 'Population Distribution' }
      ]
    };
  },
  mounted() {
    this.initMap();
  },
  methods: {
    initMap() {
      this.map = new Map({
        target: 'map',
        layers: [
          new TileLayer({
            source: new OSM()
          })
        ],
        view: new View({
          center: fromLonLat([116.397428, 39.90923]), // 北京坐标
          zoom: 7
        })
      });
    },
    togglePanel(index) {
      if (this.activePanelIndex === index) {
        this.activePanelIndex = null;
      } else {
        this.activePanelIndex = index;
      }
    },
    closePanel() {
      this.activePanelIndex = null;
    }
  }
};
</script>

<style lang="scss" scoped>
.dashboard-container {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 100%;
  
  #map {
    width: 100%;
    height: 100%;
  }
}

.function-menu {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 50px;
  background: #34495e;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  padding-top: 5px;
  justify-content: flex-start;

  .menu-item {
    height: 60px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #fff;
    transition: all 0.3s;
    padding: 0;
    margin-bottom: 8px;

    &:first-child {
      height: 60px;
      padding: 0;
    }

    &:hover {
      background: rgba(255, 255, 255, 0.1);
      
      i {
        background-color: rgba(255, 255, 255, 0.3);
      }
    }

    &.active {
      background: rgba(255, 255, 255, 0.2);
      
      i {
        background-color: rgba(255, 255, 255, 0.4);
      }
    }

    i {
      font-size: 20px;
      width: 36px;
      height: 36px;
      border-radius: 50%;
      background-color: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s;
      margin-bottom: 3px;
    }

    .menu-title {
      font-size: 11px;
      text-align: center;
      line-height: 1.2;
      width: 100%;
      
      div {
        height: 11px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}

.control-panel {
  position: absolute;
  top: 0;
  right: 50px;
  bottom: 0;
  width: 320px;
  background: #fff;
  box-shadow: -2px 0 10px rgba(0,0,0,0.1);
  z-index: 999;
  display: flex;
  flex-direction: column;

  .panel-header {
    height: 50px;
    padding: 0 15px;
    background: #34495e;
    color: #fff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 16px;
    flex-shrink: 0;

    .close-button {
      width: 24px;
      height: 24px;
      border-radius: 50%;
      background-color: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.3s;
      
      i {
        font-size: 16px;
        color: #fff;
      }
      
      &:hover {
        background-color: rgba(255, 255, 255, 0.3);
      }
    }
  }

  .panel-content {
    flex: 1;
    overflow-y: auto;
    padding: 15px;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: #ccc;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-track {
      background: #f5f5f5;
    }

    .search-box {
      margin-bottom: 15px;
    }

    .opacity-control {
      margin: 15px 0;
      span {
        display: block;
        margin-bottom: 5px;
      }
    }

    .layer-list {
      margin-top: 15px;
    }
  }
}

// 面板动画
.panel-enter-active, .panel-leave-active {
  transition: all 0.3s ease;
}
.panel-enter, .panel-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>

