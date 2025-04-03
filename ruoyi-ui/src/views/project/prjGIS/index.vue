<template>
  <div class="dashboard-container">   
    <!-- 区域详细信息浮动面板 -->
    <div v-if="regionInfo.regionId && regionDetail" 
         class="student-detail-panel"
         :class="{ 'panel-collapsed': detailCollapsed }">
      <div class="panel-header">
        <span>区域详细信息</span>
        <div class="panel-controls">
          <i class="el-icon-arrow-up" v-if="!detailCollapsed" @click="detailCollapsed = true"></i>
          <i class="el-icon-arrow-down" v-else @click="detailCollapsed = false"></i>
        </div>
      </div>
      <div v-if="!detailCollapsed" class="panel-body">
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="区域ID">{{ regionDetail.id }}</el-descriptions-item>
          <el-descriptions-item label="区域名称">{{ regionDetail.regionName }}</el-descriptions-item>
          <el-descriptions-item label="服务类型">
            <div class="service-type-list">
              <span 
                v-for="(type, index) in regionDetail.description ? regionDetail.description.split('、') : []" 
                :key="index"
                class="service-type-tag"
              >
                {{ type }}
              </span>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间" v-if="regionDetail.createTime">
            {{ parseTime(regionDetail.createTime) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
    
    <!-- 地图区域 -->
    <div class="map-container">
      <div id="map"></div>
    </div>

    <!-- 功能菜单区域 - 动态生成 -->
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

    <!-- 控制面板区域 - 动态生成 -->
    <transition-group name="panel">
      <div v-for="(item, index) in menuItems" :key="item.type || item.fullName || index"
           v-if="activePanelIndex === index"
           class="control-panel">
        <div class="panel-header">
          <span>{{ item.fullName }}</span>
          <div class="close-button" @click="closePanel">
            <i class="el-icon-close"></i>
          </div>
        </div>
        <div class="panel-content">
          <!-- 通用区域信息显示 - 所有面板都显示 -->
          <template v-if="regionInfo.regionId">
            <el-collapse v-model="activeRegionInfo" class="region-info-collapse">
              <el-collapse-item title="基本信息" name="regionBasicInfo">
                <el-descriptions :column="1" border>
                  <el-descriptions-item label="区域ID">{{ regionInfo.regionId }}</el-descriptions-item>
                  <el-descriptions-item label="区域名称">{{ regionInfo.regionName }}</el-descriptions-item>
                </el-descriptions>
              </el-collapse-item>
            </el-collapse>
          </template>
          
          <!-- 动态生成面板内容 -->
          <div class="panel-specific-content">
            <!-- 水源涵养面板 (index === 0 或 type === '0') -->
            <template v-if="item.type === '0'">
              <el-divider content-position="left">图层控制</el-divider>
              <div class="search-box">
                <el-input placeholder="搜索图层..." v-model="searchQuery" prefix-icon="el-icon-search"></el-input>
              </div>
              <div class="opacity-control">
                <span>透明度设定</span>
                <el-slider v-model="opacity" :min="0" :max="100"></el-slider>
              </div>
              <el-switch v-model="showLegend" active-text="图例"></el-switch>
              <div class="layer-list">
                <el-collapse v-model="activeCategories">
                  <el-collapse-item v-for="category in categories" :key="category.name" :title="category.name">
                    <!-- 图层列表 -->
                  </el-collapse-item>
                </el-collapse>
              </div>
            </template>
            
            <!-- 水源供给面板 -->
            <template v-else-if="item.type === '1'">
              <el-divider content-position="left">水源供给设置</el-divider>
              <div class="parameter-control">
                <span>年均径流量阈值</span>
                <el-slider v-model="waterSupplyThreshold" :min="0" :max="500" :step="10"></el-slider>
              </div>
              <el-divider content-position="left">数据图层</el-divider>
              <div class="data-layers">
                <el-checkbox-group v-model="waterSupplyLayers">
                  <el-checkbox label="precipitationLayer">降水量图层</el-checkbox>
                  <el-checkbox label="evaporationLayer">蒸发量图层</el-checkbox>
                  <el-checkbox label="runoffLayer">径流量图层</el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            
            <!-- 土壤保持面板 -->
            <template v-else-if="item.type === '2'">
              <el-divider content-position="left">土壤保持设置</el-divider>
              <div class="parameter-control">
                <span>土壤侵蚀风险等级</span>
                <el-radio-group v-model="soilErosionRisk">
                  <el-radio label="low">低风险</el-radio>
                  <el-radio label="medium">中风险</el-radio>
                  <el-radio label="high">高风险</el-radio>
                </el-radio-group>
              </div>
              <el-divider content-position="left">数据图层</el-divider>
              <div class="data-layers">
                <el-checkbox-group v-model="soilLayers">
                  <el-checkbox label="erosionLayer">侵蚀量图层</el-checkbox>
                  <el-checkbox label="vegetationLayer">植被覆盖图层</el-checkbox>
                  <el-checkbox label="slopeLayer">坡度图层</el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            
            <!-- 水质净化面板 -->
            <template v-else-if="item.type === '3'">
              <el-divider content-position="left">水质净化设置</el-divider>
              <div class="parameter-control">
                <span>污染物类型</span>
                <el-select v-model="pollutantType" placeholder="请选择">
                  <el-option label="氮" value="nitrogen"></el-option>
                  <el-option label="磷" value="phosphorus"></el-option>
                  <el-option label="重金属" value="heavyMetal"></el-option>
                </el-select>
              </div>
              <el-divider content-position="left">数据图层</el-divider>
              <div class="data-layers">
                <el-checkbox-group v-model="waterQualityLayers">
                  <el-checkbox label="pollutionSourceLayer">污染源图层</el-checkbox>
                  <el-checkbox label="waterQualityLayer">水质分布图层</el-checkbox>
                  <el-checkbox label="purificationLayer">净化能力图层</el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            
            <!-- 防风固沙面板 -->
            <template v-else-if="item.type === '4'">
              <el-divider content-position="left">防风固沙设置</el-divider>
              <div class="parameter-control">
                <span>风蚀模拟周期</span>
                <el-radio-group v-model="windErosionPeriod">
                  <el-radio label="month">月尺度</el-radio>
                  <el-radio label="season">季节尺度</el-radio>
                  <el-radio label="year">年尺度</el-radio>
                </el-radio-group>
              </div>
              <el-divider content-position="left">数据图层</el-divider>
              <div class="data-layers">
                <el-checkbox-group v-model="windSandLayers">
                  <el-checkbox label="windSpeedLayer">风速图层</el-checkbox>
                  <el-checkbox label="sandSourceLayer">沙源图层</el-checkbox>
                  <el-checkbox label="vegetationBarrierLayer">植被屏障图层</el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            
            <!-- 洪水调蓄面板 -->
            <template v-else-if="item.type === '5'">
              <el-divider content-position="left">洪水调蓄设置</el-divider>
              <div class="parameter-control">
                <span>洪水重现期</span>
                <el-select v-model="floodReturnPeriod" placeholder="请选择">
                  <el-option label="5年一遇" value="5"></el-option>
                  <el-option label="10年一遇" value="10"></el-option>
                  <el-option label="20年一遇" value="20"></el-option>
                  <el-option label="50年一遇" value="50"></el-option>
                </el-select>
              </div>
              <el-divider content-position="left">数据图层</el-divider>
              <div class="data-layers">
                <el-checkbox-group v-model="floodLayers">
                  <el-checkbox label="floodAreaLayer">淹没范围图层</el-checkbox>
                  <el-checkbox label="floodDepthLayer">淹没深度图层</el-checkbox>
                  <el-checkbox label="floodDurationLayer">淹没持续时间图层</el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            
            <!-- 固碳服务面板 -->
            <template v-else-if="item.type === '6'">
              <el-divider content-position="left">固碳服务设置</el-divider>
              <div class="parameter-control">
                <span>碳汇计算周期</span>
                <el-date-picker
                  v-model="carbonPeriod"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期">
                </el-date-picker>
              </div>
              <el-divider content-position="left">数据图层</el-divider>
              <div class="data-layers">
                <el-checkbox-group v-model="carbonLayers">
                  <el-checkbox label="vegetationCarbonLayer">植被固碳图层</el-checkbox>
                  <el-checkbox label="soilCarbonLayer">土壤固碳图层</el-checkbox>
                  <el-checkbox label="carbonFluxLayer">碳通量图层</el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            
            <!-- 粮食供给面板 -->
            <template v-else-if="item.type === '7'">
              <el-divider content-position="left">粮食供给设置</el-divider>
              <div class="parameter-control">
                <span>作物类型</span>
                <el-select v-model="cropType" placeholder="请选择">
                  <el-option label="小麦" value="wheat"></el-option>
                  <el-option label="水稻" value="rice"></el-option>
                  <el-option label="玉米" value="corn"></el-option>
                  <el-option label="大豆" value="soybean"></el-option>
                </el-select>
              </div>
              <el-divider content-position="left">数据图层</el-divider>
              <div class="data-layers">
                <el-checkbox-group v-model="foodLayers">
                  <el-checkbox label="croplandLayer">农田分布图层</el-checkbox>
                  <el-checkbox label="yieldLayer">产量分布图层</el-checkbox>
                  <el-checkbox label="soilQualityLayer">土壤质量图层</el-checkbox>
                </el-checkbox-group>
              </div>
            </template>
            
            <!-- 默认面板 - 当没有匹配的类型时显示 -->
            <template v-else>
              <el-divider content-position="left">图层控制</el-divider>
              <div class="search-box">
                <el-input placeholder="搜索图层..." v-model="searchQuery" prefix-icon="el-icon-search"></el-input>
              </div>
              <div class="opacity-control">
                <span>透明度设定</span>
                <el-slider v-model="opacity" :min="0" :max="100"></el-slider>
              </div>
              <el-switch v-model="showLegend" active-text="图例"></el-switch>
              <div class="layer-list">
                <el-collapse v-model="activeCategories">
                  <el-collapse-item v-for="category in categories" :key="category.name" :title="category.name">
                    <!-- 默认图层列表 -->
                  </el-collapse-item>
                </el-collapse>
              </div>
            </template>
          </div>
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
import XYZ from 'ol/source/XYZ';
import { fromLonLat } from 'ol/proj';
// 导入获取区域详情和服务类型的API
import { getProject_region } from "@/api/project/project_region";
import { listProject_region_service } from "@/api/project/project_region_service";
// 导入日期格式化函数
import { parseTime } from "@/utils/ruoyi";

export default {
  name: "Index",
  data() {
    return {
      map: null,
      loading: false,
      
      // 区域基本信息
      regionInfo: {
        regionId: null,
        regionName: null
      },
      
      // 区域详细信息
      regionDetail: null,
      
      // 区域服务类型
      serviceTypes: [],
      
      // 字典数据
      serviceTypeOptions: [],
      
      // 详细信息面板是否折叠
      detailCollapsed: true,
      
      // 菜单项 - 将被动态生成
      menuItems: [],
      
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
      ],
      
      // 水源供给面板数据
      waterSupplyThreshold: 200,
      waterSupplyLayers: ['precipitationLayer', 'runoffLayer'],
      
      // 土壤保持面板数据
      soilErosionRisk: 'medium',
      soilLayers: ['erosionLayer'],
      
      // 水质净化面板数据
      pollutantType: 'nitrogen',
      waterQualityLayers: ['waterQualityLayer'],
      
      // 防风固沙面板数据
      windErosionPeriod: 'year',
      windSandLayers: ['windSpeedLayer'],
      
      // 洪水调蓄面板数据
      floodReturnPeriod: '20',
      floodLayers: ['floodAreaLayer'],
      
      // 固碳服务面板数据
      carbonPeriod: [new Date(), new Date()],
      carbonLayers: ['vegetationCarbonLayer'],
      
      // 粮食供给面板数据
      cropType: 'wheat',
      foodLayers: ['croplandLayer'],
      
      // 新增的activeRegionInfo
      activeRegionInfo: ['regionBasicInfo'],
    };
  },
  created() {
    // 从路由参数中获取regionId
    const regionId = this.$route.query.regionId;
    if (regionId) {
      this.regionInfo.regionId = regionId;
      this.fetchRegionDetail(regionId);
      this.fetchRegionServiceTypes(regionId);
      // 加载服务类型字典
      this.loadServiceTypeDictionary();
    } else {
      this.$message.warning('未提供区域ID，无法加载区域信息');
    }
  },
  mounted() {
    this.initMap();
  },
  methods: {
    // 初始化地图
    initMap() {
      this.map = new Map({
        target: 'map',
        layers: [
          // 天地图影像底图
          new TileLayer({
            source: new XYZ({
              url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=img_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
              maxZoom: 18
            })
          }),
          // 天地图标注图层
          new TileLayer({
            source: new XYZ({
              url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=cia_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
              maxZoom: 18
            })
          })
        ],
        view: new View({
          center: fromLonLat([116.397428, 39.90923]), // 北京坐标
          zoom: 7
        })
      });
    },
    
    // 加载服务类型字典数据
    loadServiceTypeDictionary() {
      this.getDicts("sys_service_type").then(response => {
        if (response && response.data) {
          this.serviceTypeOptions = response.data;
          
          // 添加更详细的日志信息
          console.log("加载的服务类型字典数据:", response.data);
          
          // 检查字典项的属性结构
          if (this.serviceTypeOptions.length > 0) {
            const firstItem = this.serviceTypeOptions[0];
            console.log("字典项结构示例:", firstItem);
            console.log("可用的属性:", Object.keys(firstItem));
            
            // 检查可能的图标属性
            if (firstItem.listClass) {
              console.log("使用 listClass 属性获取图标");
            } else if (firstItem.list_class) {
              console.log("使用 list_class 属性获取图标");
              
              // 如果确实是list_class，修正所有项的属性
              this.serviceTypeOptions.forEach(item => {
                if (item.list_class) {
                  item.listClass = item.list_class;
                }
              });
            } else {
              console.warn("未找到图标属性，可能需要手动映射");
            }
          }
          
          // 如果区域数据已加载，则生成菜单
          if (this.regionDetail || this.serviceTypes.length > 0) {
            this.generateMenuItems();
          }
        } else {
          console.error("加载服务类型字典失败");
        }
      }).catch(error => {
        console.error("获取服务类型字典失败:", error);
      });
    },
    
    // 获取区域详细信息
    fetchRegionDetail(regionId) {
      this.loading = true;
      getProject_region(regionId).then(response => {
        if (response && response.code === 200 && response.data) {
          this.regionDetail = response.data;
          this.regionInfo.regionName = response.data.regionName;
          console.log('获取到区域详细信息:', this.regionDetail);
          
          // 如果服务类型字典已加载，则生成菜单
          if (this.serviceTypeOptions.length > 0) {
            this.generateMenuItems();
          }
        } else {
          this.$message.error('获取区域详细信息失败');
        }
        this.loading = false;
      }).catch(error => {
        console.error('获取区域详细信息错误:', error);
        this.$message.error('获取区域详细信息失败');
        this.loading = false;
      });
    },
    
    // 获取区域服务类型
    fetchRegionServiceTypes(regionId) {
      listProject_region_service({ regionId: regionId }).then(response => {
        if (response && response.code === 200 && response.rows) {
          // 获取服务类型数组
          this.serviceTypes = response.rows.map(item => item.serviceType);
          console.log('获取到区域服务类型ID:', this.serviceTypes);
          
          // 如果字典已加载，则生成菜单
          if (this.serviceTypeOptions.length > 0) {
            this.generateMenuItems();
          }
        } else {
          console.warn('获取区域服务类型失败或无服务类型');
        }
      }).catch(error => {
        console.error('获取区域服务类型错误:', error);
      });
    },
    
    // 根据服务类型生成菜单项
    generateMenuItems() {
      const items = [];
      const processedTypes = new Set(); // 用于跟踪已处理的服务类型
      
      // 首先，尝试从区域详情中的description字段获取服务类型
      if (this.regionDetail && this.regionDetail.description) {
        const typeNames = this.regionDetail.description.split('、');
        
        typeNames.forEach(typeName => {
          // 在字典中查找匹配的服务类型
          const dictItem = this.serviceTypeOptions.find(item => item.dictLabel === typeName);
          
          if (dictItem) {
            const typeId = dictItem.dictValue;
            
            // 如果已经处理过该类型，则跳过
            if (processedTypes.has(typeId)) return;
            
            // 创建菜单项
            items.push(this.createMenuItem(dictItem.dictLabel, typeId));
            processedTypes.add(typeId);
          }
        });
      }
      
      // 如果区域详情中没有找到服务类型，则使用从project_region_service获取的服务类型ID
      if (items.length === 0 && this.serviceTypes.length > 0) {
        this.serviceTypes.forEach(typeId => {
          // 如果已经处理过该类型，则跳过
          if (processedTypes.has(typeId)) return;
          
          // 在字典中查找匹配的服务类型
          const dictItem = this.serviceTypeOptions.find(item => item.dictValue === typeId);
          
          if (dictItem) {
            // 创建菜单项
            items.push(this.createMenuItem(dictItem.dictLabel, typeId));
            processedTypes.add(typeId);
          }
        });
      }
      
      // 如果仍然没有找到任何服务类型，添加默认项
      if (items.length === 0) {
        // 尝试使用第一个字典项作为默认值
        if (this.serviceTypeOptions.length > 0) {
          const defaultType = this.serviceTypeOptions[0];
          items.push(this.createMenuItem(defaultType.dictLabel, defaultType.dictValue));
        } else {
          // 如果字典也为空，创建一个硬编码的默认项
          items.push({
            nameTop: '水源',
            nameBottom: '涵养',
            fullName: '水源涵养',
            icon: 'el-icon-s-grid',
            type: '0'
          });
        }
        this.$message.warning('未找到区域的服务类型，显示默认服务');
      }
      
      this.menuItems = items;
      console.log('生成的菜单项:', this.menuItems);
    },
    
    // 创建菜单项 - 修改以确保正确使用字典数据中的图标
    createMenuItem(typeName, typeId) {
      // 智能拆分名称为上下两部分
      let nameTop, nameBottom;
      
      if (typeName.length <= 2) {
        nameTop = typeName;
        nameBottom = '';
      } else if (typeName.length === 3) {
        nameTop = typeName.substring(0, 2);
        nameBottom = typeName.substring(2);
      } else if (typeName.length === 4) {
        nameTop = typeName.substring(0, 2);
        nameBottom = typeName.substring(2);
      } else {
        const midPoint = Math.ceil(typeName.length / 2);
        nameTop = typeName.substring(0, midPoint);
        nameBottom = typeName.substring(midPoint);
      }
      
      // 确保typeId是字符串，以便正确比较
      const typeIdStr = String(typeId);
      
      // 添加详细的调试日志
      console.log("当前服务类型字典数据:", this.serviceTypeOptions);
      console.log(`查找服务类型ID: ${typeIdStr}`);
      
      // 从字典数据中查找对应的服务类型项
      const dictItem = this.serviceTypeOptions.find(item => String(item.dictValue) === typeIdStr);
      
      // 获取图标配置
      let icon = 'el-icon-menu'; // 默认图标
      
      if (dictItem) {
        console.log("找到字典项:", dictItem);
        
        // 检查所有可能的图标属性名称
        if (dictItem.listClass) {
          icon = dictItem.listClass;
          console.log(`使用listClass: ${icon}`);
        } 
        else if (dictItem.list_class) {
          icon = dictItem.list_class;
          console.log(`使用list_class: ${icon}`);
        }
        // 可能的其他属性名称，如cssClass等
        else if (dictItem.cssClass) {
          icon = dictItem.cssClass;
          console.log(`使用cssClass: ${icon}`);
        }
        else {
          // 硬编码备选图标映射，以防字典数据属性不一致
          const iconMap = {
            '0': 'el-icon-s-grid',       // 水源涵养
            '1': 'el-icon-s-marketing',  // 水源供给
            '2': 'el-icon-data-analysis', // 土壤保持
            '3': 'el-icon-s-flag',       // 水质净化
            '4': 'el-icon-s-tools',      // 防风固沙
            '5': 'el-icon-download',     // 洪水调蓄
            '6': 'el-icon-s-opportunity', // 森林固碳
            '7': 'el-icon-food'          // 粮食供给
          };
          
          if (iconMap[typeIdStr]) {
            icon = iconMap[typeIdStr];
            console.log(`使用备选图标: ${icon}`);
          }
        }
      } else {
        console.warn(`未找到ID为 ${typeIdStr} 的服务类型字典项`);
      }
      
      console.log(`最终使用的图标: ${icon}`);
      
      return {
        nameTop: nameTop,
        nameBottom: nameBottom,
        fullName: typeName,
        icon: icon,
        type: typeIdStr
      };
    },
    
    // 格式化日期
    parseTime,
    
    // 切换控制面板
    togglePanel(index) {
      if (this.activePanelIndex === index) {
        this.activePanelIndex = null;
      } else {
        this.activePanelIndex = index;
      }
    },
    
    // 关闭控制面板
    closePanel() {
      this.activePanelIndex = null;
    }
  },
  watch: {
    // 监听路由参数变化
    '$route.query.regionId': {
      handler(newVal) {
        if (newVal) {
          this.regionInfo.regionId = newVal;
          this.fetchRegionDetail(newVal);
          this.fetchRegionServiceTypes(newVal);
        }
      },
      immediate: true // 确保组件创建时也会执行一次
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
    padding: 8px 15px;

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

    .region-info-collapse {
      margin-bottom: 10px;
    }

    .panel-specific-content {
      margin-top: 10px;
      
      .el-divider {
        margin: 12px 0;
      }
      
      .parameter-control {
        margin: 15px 0;
        
        span {
          display: block;
          margin-bottom: 8px;
          color: #606266;
          font-weight: 500;
        }
      }
      
      .data-layers {
        margin: 15px 0;
        padding: 10px;
        background-color: #f8f8f8;
        border-radius: 4px;
        
        .el-checkbox-group {
          display: flex;
          flex-direction: column;
          
          .el-checkbox {
            margin-left: 0;
            margin-bottom: 8px;
            
            &:last-child {
              margin-bottom: 0;
            }
          }
        }
      }
      
      .el-divider__text {
        font-size: 15px;
        font-weight: 600;
        color: #409EFF;
      }
      
      .el-select {
        width: 100%;
      }
      
      .el-date-editor {
        width: 100%;
      }
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

// 添加学生详细信息面板样式
.student-detail-panel {
  position: absolute;
  left: 40px;
  top: 8px;
  width: 300px;
  background: white;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  z-index: 1000;
  transition: all 0.3s;
  
  &.panel-collapsed {
    height: 40px;
    overflow: hidden;
  }
  
  .panel-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 8px 15px;
    background: #f5f7fa;
    border-bottom: 1px solid #e6ebf5;
    
    span {
      font-weight: bold;
      color: #606266;
    }
    
    .panel-controls {
      i {
        cursor: pointer;
        color: #909399;
        
        &:hover {
          color: #409EFF;
        }
      }
    }
  }
  
  .panel-body {
    padding: 10px;
    max-height: 300px;
    overflow-y: auto;
    
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
  }
}

.service-type-tag {
  display: inline-block;
  padding: 4px 10px;
  margin: 4px;
  border-radius: 4px;
  border: 1px solid #409EFF;
  color: #409EFF;
  background-color: rgba(64, 158, 255, 0.1);
  font-size: 13px;
}

.service-type-list {
  display: flex;
  flex-wrap: wrap;
}
</style>

<!-- Global styles for overriding Element UI collapse panels -->
<style lang="scss">
/* Override collapse panel styles to match the dark blue design */
.panel-content .layer-list .el-collapse-item__header,
.panel-content .region-info-collapse .el-collapse-item__header {
  background-color: #34495e !important;
  color: white !important;
  border-bottom: 1px solid white !important;
  height: 40px !important;
  line-height: 40px !important;
  font-weight: 400 !important;
  padding: 0 35px 0 12px !important; /* 右侧增加padding以确保箭头有足够空间 */
  border-radius: 0 !important;
  position: relative !important;
}

.panel-content .layer-list .el-collapse-item__arrow,
.panel-content .region-info-collapse .el-collapse-item__arrow {
  color: white !important;
  margin: 0 !important;
  position: absolute !important;
  right: 12px !important; /* 调整箭头右侧间距 */
  top: 50% !important; /* 垂直居中 */
  transform: translateY(-50%) rotate(-90deg) !important; /* 添加垂直居中的transform */
  font-size: 16px !important; /* 增大箭头尺寸 */
  transition: transform 0.3s !important;
  z-index: 2 !important; /* 确保箭头在最上层 */
}

.panel-content .layer-list .el-collapse-item__header.is-active .el-collapse-item__arrow,
.panel-content .region-info-collapse .el-collapse-item__header.is-active .el-collapse-item__arrow {
  transform: translateY(-50%) rotate(0deg) !important; /* 保持垂直居中的transform */
}

.panel-content .layer-list .el-collapse,
.panel-content .region-info-collapse .el-collapse {
  border-top: none !important;
  border-bottom: none !important;
}

.panel-content .layer-list .el-collapse-item__wrap,
.panel-content .region-info-collapse .el-collapse-item__wrap {
  border-bottom: none !important;
}
</style>