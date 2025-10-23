<template>
  <div class="dashboard-container">   
    <!-- 区域详细信息浮动面板 -->
    <div v-if="regionInfo.regionId && regionDetail" 
         class="student-detail-panel"
         :class="{ 'panel-collapsed': detailCollapsed }">
      <div class="panel-header">
        <span style="font-size: smaller;">区域详细信息</span>
        <div class="panel-controls">
          <i :class="detailCollapsed ? 'el-icon-arrow-down' : 'el-icon-arrow-up'" @click="detailCollapsed = !detailCollapsed"></i>
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
    
    <!-- 地图控制面板 -->
    <div class="map-control-panel">
      <div class="map-control-button" @click="resetMapView">
        <i class="el-icon-s-home"></i>
      </div>
      <div class="map-control-button" @click="toggleFullscreen">
        <i class="el-icon-full-screen"></i>
      </div>
      <div class="map-control-button" @click="zoomIn">
        <i class="el-icon-plus"></i>
      </div>
      <div class="map-control-button" @click="zoomOut">
        <i class="el-icon-minus"></i>
      </div>
      <div class="map-control-button" @click="toggleBaseLayer">
        <i class="el-icon-orange"></i>
      </div>
      <div class="map-control-button" @click="toggleMeasureTool">
        <i class="el-icon-crop"></i>
      </div>
    </div>
    
    <!-- 图例面板 - 使用计算属性 -->
    <div class="legend-panel" v-show="showLegend && activePanelIndex !== null">
      <div class="legend-header">{{ activePanelIndex !== null && menuItems[activePanelIndex] ? menuItems[activePanelIndex].fullName : '图例' }}</div>
      <div class="legend-content">
        <div class="legend-gradient-container">
          <div class="legend-gradient" :style="legendStyle"></div>
          <div class="legend-values">
            <span class="min-value">低</span>
            <span class="max-value">高</span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 地图区域 -->
    <div class="map-container">
      <div id="map"></div>
      
      <!-- 时间控制轴 -->
      <div class="time-control-slider">
        <div class="slider-container">
          <el-slider 
            v-model="currentYear" 
            :min="minYear" 
            :max="maxYear" 
            :step="1"
            :marks="timeMarks"
            :show-tooltip="false"
            @change="handleYearChange">
          </el-slider>
        </div>
        <div class="controls-row">
          <div class="year-label start-year">{{ minYear }}</div>
          <div class="center-controls">
            <div class="play-button" @click="toggleTimePlay">
              <i :class="isTimePlayActive ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
            </div>
            <div class="current-year">{{ currentYear }}</div>
          </div>
          <div class="year-label end-year">{{ maxYear }}</div>
        </div>
      </div>
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

    <!-- 控制面板区域 - 使用key确保正确重新渲染 -->
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
          <!-- 动态生成面板内容 -->
          <div class="panel-specific-content">
            <!-- 水源涵养面板 (index === 0 或 type === '0') -->
            <template v-if="item.type === '0'">
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply0">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario0" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators0" placeholder="请选择指标">
                      <el-option label="枯水期基流" value="baseflow"></el-option>
                      <el-option label="土壤湿度" value="soilMoisture"></el-option>
                      <el-option label="蓄水容量" value="waterStorage"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale0">
                      <el-radio label="month">月尺度</el-radio>
                      <el-radio label="year">年尺度</el-radio>
                    </el-radio-group>
                  </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern0">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse0">
                  <div class="parameter-control">
                    <span>下游居民</span>
                    <el-input v-model="benefit0.downstream" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>灌溉农业</span>
                    <el-input v-model="benefit0.irrigation" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit0">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching0">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization0" placeholder="请选择优化方案">
                      <el-option label="植被恢复" value="vegetation"></el-option>
                      <el-option label="土壤改良" value="soil"></el-option>
                      <el-option label="径流引导" value="runoff"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl0" placeholder="请选择调控方案">
                      <el-option label="用水配额" value="quota"></el-option>
                      <el-option label="水价调整" value="price"></el-option>
                      <el-option label="替代水源" value="alternative"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply1">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario1" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators1" placeholder="请选择指标">
                      <el-option label="年均径流量" value="runoff"></el-option>
                      <el-option label="降水量" value="precipitation"></el-option>
                      <el-option label="蒸发量" value="evaporation"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale1">
                      <el-radio label="month">月尺度</el-radio>
                      <el-radio label="year">年尺度</el-radio>
                    </el-radio-group>
                  </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern1">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse1">
                  <div class="parameter-control">
                    <span>城市居民</span>
                    <el-input v-model="benefit1.urban" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>工业企业</span>
                    <el-input v-model="benefit1.industry" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit1">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching1">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization1" placeholder="请选择优化方案">
                      <el-option label="水源保护" value="protection"></el-option>
                      <el-option label="水库建设" value="reservoir"></el-option>
                      <el-option label="引水工程" value="diversion"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl1" placeholder="请选择调控方案">
                      <el-option label="用水效率提高" value="efficiency"></el-option>
                      <el-option label="水价阶梯制" value="priceTier"></el-option>
                      <el-option label="产业结构调整" value="industry"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply2">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario2" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators2" placeholder="请选择指标">
                      <el-option label="土壤侵蚀量" value="erosion"></el-option>
                      <el-option label="植被覆盖度" value="vegetation"></el-option>
                      <el-option label="水土保持能力" value="conservation"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale2">
                      <el-radio label="month">月尺度</el-radio>
                      <el-radio label="year">年尺度</el-radio>
                </el-radio-group>
              </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern2">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse2">
                  <div class="parameter-control">
                    <span>农田管理者</span>
                    <el-input v-model="benefit2.farmers" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>水库管理者</span>
                    <el-input v-model="benefit2.reservoir" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit2">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching2">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization2" placeholder="请选择优化方案">
                      <el-option label="植被恢复" value="vegetation"></el-option>
                      <el-option label="梯田建设" value="terrace"></el-option>
                      <el-option label="保护性耕作" value="conservation"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl2" placeholder="请选择调控方案">
                      <el-option label="耕作方式改变" value="farming"></el-option>
                      <el-option label="土地利用规划" value="landuse"></el-option>
                      <el-option label="生态红线划定" value="ecoRedLine"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply3">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario3" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators3" placeholder="请选择指标">
                      <el-option label="氮净化能力" value="nitrogenPurification"></el-option>
                      <el-option label="磷净化能力" value="phosphorusPurification"></el-option>
                      <el-option label="重金属净化能力" value="metalPurification"></el-option>
                </el-select>
              </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale3">
                      <el-radio label="month">月尺度</el-radio>
                      <el-radio label="year">年尺度</el-radio>
                    </el-radio-group>
                  </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern3">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse3">
                  <div class="parameter-control">
                    <span>水产养殖户</span>
                    <el-input v-model="benefit3.aquaculture" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>饮用水供应商</span>
                    <el-input v-model="benefit3.drinking" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit3">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching3">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization3" placeholder="请选择优化方案">
                      <el-option label="湿地建设" value="wetland"></el-option>
                      <el-option label="植被缓冲带" value="buffer"></el-option>
                      <el-option label="生物过滤" value="biofilter"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl3" placeholder="请选择调控方案">
                      <el-option label="污染物排放控制" value="emission"></el-option>
                      <el-option label="污水处理能力提升" value="treatment"></el-option>
                      <el-option label="清洁生产技术" value="cleanProduction"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply4">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario4" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators4" placeholder="请选择指标">
                      <el-option label="沙尘通量" value="sandFlux"></el-option>
                      <el-option label="植被阻滞能力" value="vegetationBarrier"></el-option>
                      <el-option label="土壤稳定性" value="soilStability"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale4">
                  <el-radio label="month">月尺度</el-radio>
                  <el-radio label="year">年尺度</el-radio>
                </el-radio-group>
              </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern4">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse4">
                  <div class="parameter-control">
                    <span>林业管理者</span>
                    <el-input v-model="benefit4.forestry" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>城市居民</span>
                    <el-input v-model="benefit4.urban" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit4">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching4">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization4" placeholder="请选择优化方案">
                      <el-option label="防护林建设" value="shelter"></el-option>
                      <el-option label="草方格固沙" value="grassSquare"></el-option>
                      <el-option label="沙漠治理" value="desertControl"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl4" placeholder="请选择调控方案">
                      <el-option label="放牧强度控制" value="grazing"></el-option>
                      <el-option label="土地合理利用" value="landUse"></el-option>
                      <el-option label="城市绿化" value="urbanGreen"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply5">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario5" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators5" placeholder="请选择指标">
                      <el-option label="洪峰削减" value="peakReduction"></el-option>
                      <el-option label="洪水滞留时间" value="retentionTime"></el-option>
                      <el-option label="调蓄容量" value="storageCapacity"></el-option>
                </el-select>
              </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale5">
                      <el-radio label="month">月尺度</el-radio>
                      <el-radio label="year">年尺度</el-radio>
                    </el-radio-group>
                  </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern5">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse5">
                  <div class="parameter-control">
                    <span>防洪部门</span>
                    <el-input v-model="benefit5.floodControl" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>沿河居民</span>
                    <el-input v-model="benefit5.riverside" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit5">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching5">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization5" placeholder="请选择优化方案">
                      <el-option label="湿地恢复" value="wetland"></el-option>
                      <el-option label="蓄滞洪区建设" value="floodStorage"></el-option>
                      <el-option label="渗透面增加" value="permeableSurface"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl5" placeholder="请选择调控方案">
                      <el-option label="洪泛区管理" value="floodplain"></el-option>
                      <el-option label="防洪工程" value="floodEngineering"></el-option>
                      <el-option label="预警系统" value="earlyWarning"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply6">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario6" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators6" placeholder="请选择指标">
                      <el-option label="植被固碳量" value="vegetationCarbon"></el-option>
                      <el-option label="土壤固碳量" value="soilCarbon"></el-option>
                      <el-option label="碳通量" value="carbonFlux"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale6">
                      <el-radio label="month">月尺度</el-radio>
                      <el-radio label="year">年尺度</el-radio>
                    </el-radio-group>
                  </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern6">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse6">
                  <div class="parameter-control">
                    <span>碳汇交易企业</span>
                    <el-input v-model="benefit6.carbonTrading" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>气候变化减缓</span>
                    <el-input v-model="benefit6.climateChange" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit6">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching6">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization6" placeholder="请选择优化方案">
                      <el-option label="植树造林" value="afforestation"></el-option>
                      <el-option label="土壤碳汇提升" value="soilCarbon"></el-option>
                      <el-option label="保护性耕作" value="conservation"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl6" placeholder="请选择调控方案">
                      <el-option label="碳排放限额" value="emissionCap"></el-option>
                      <el-option label="碳交易市场" value="carbonMarket"></el-option>
                      <el-option label="清洁能源替代" value="cleanEnergy"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply7">
                  <div class="parameter-control">
                    <span>选择情景</span>
                    <el-select v-model="scenario7" placeholder="请选择情景">
                      <el-option label="基准情景" value="baseline"></el-option>
                      <el-option label="优化情景" value="optimized"></el-option>
                      <el-option label="发展情景" value="development"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>服务指标</span>
                    <el-select v-model="indicators7" placeholder="请选择指标">
                      <el-option label="农田产量" value="cropYield"></el-option>
                      <el-option label="土壤质量" value="soilQuality"></el-option>
                      <el-option label="适宜性等级" value="suitabilityLevel"></el-option>
                </el-select>
              </div>
                  <div class="parameter-control">
                    <span>时间尺度</span>
                    <el-radio-group v-model="timeScale7">
                      <el-radio label="month">月尺度</el-radio>
                      <el-radio label="year">年尺度</el-radio>
                    </el-radio-group>
                  </div>
                  <div class="parameter-control">
                    <span>空间格局</span>
                    <el-radio-group v-model="spatialPattern7">
                      <el-radio label="max">最高值</el-radio>
                      <el-radio label="min">最低值</el-radio>
                      <el-radio label="mode">众数</el-radio>
                    </el-radio-group>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="实际利用" name="actualUse7">
                  <div class="parameter-control">
                    <span>农民</span>
                    <el-input v-model="benefit7.farmers" placeholder="请输入受益量"></el-input>
                  </div>
                  <div class="parameter-control">
                    <span>粮食加工企业</span>
                    <el-input v-model="benefit7.processing" placeholder="请输入受益量"></el-input>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供给赤字" name="deficit7">
                  <!-- 按钮组 - 添加查看数据按钮 -->
                  <div class="chart-controls">
                    <el-button type="primary" size="small" round @click="renderChart(item.type || 0)">重绘图表</el-button>
                    <el-button type="primary" size="small" round @click="viewChartData(item.type || 0)">查看数据</el-button>
                    <el-button type="primary" size="small" round @click="downloadChartData(item.type || 0)">下载数据</el-button>
                  </div>
                  
                  <!-- 图表容器 -->
                  <div class="chart-container" v-show="isChartVisible(item.type)">
                    <div :id="'simpleChart' + item.type" style="width: 100%; height: 300px; background-color: #f9f9f9;"></div>
                  </div>
                </el-collapse-item>
                <el-collapse-item title="供需匹配" name="matching7">
                  <div class="parameter-control">
                    <span>供给优化</span>
                    <el-select v-model="supplyOptimization7" placeholder="请选择优化方案">
                      <el-option label="高产品种" value="highYield"></el-option>
                      <el-option label="耕地质量提升" value="landQuality"></el-option>
                      <el-option label="精准农业" value="precisionAgriculture"></el-option>
                    </el-select>
                  </div>
                  <div class="parameter-control">
                    <span>需求调控</span>
                    <el-select v-model="demandControl7" placeholder="请选择调控方案">
                      <el-option label="粮食节约" value="conservation"></el-option>
                      <el-option label="食物多样化" value="diversity"></el-option>
                      <el-option label="粮食进口" value="import"></el-option>
                    </el-select>
                  </div>
                </el-collapse-item>
              </el-collapse>
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
import { fromLonLat, transformExtent } from 'ol/proj';
import { defaults as defaultControls } from 'ol/control';

import { getProject_region } from "@/api/project/project_region";
import { listProject_region_service } from "@/api/project/project_region_service";
import { parseTime } from "@/utils/ruoyi";
import * as echarts from 'echarts';

export default {
  name: "Index",
  data() {
    return {
      map: null,
      loading: false,
      
      regionInfo: {
        regionId: null,
        regionName: null
      },
      
      regionDetail: null,
      
      serviceTypes: [],
      
      serviceTypeOptions: [],
      
      detailCollapsed: true,
      
      menuItems: [],
      
      activePanelIndex: null,
      
      searchQuery: '',
      opacity: 100,
      showLegend: true,
      activeCategories: ['1'],
      categories: [
      ],
      
      isFullscreen: false,
      currentBaseLayerIndex: 0,
      baseLayerOptions: [
        { name: '影像底图', type: 'satellite' },
        { name: '街道地图', type: 'street' }
      ],
      isMeasureActive: false,
      initialMapCenter: [116.397428, 39.90923],
      initialMapZoom: 7,
      
      waterSupplyThreshold: 200,
      waterSupplyLayers: ['precipitationLayer', 'runoffLayer'],
      
      soilErosionRisk: 'medium',
      soilLayers: ['erosionLayer'],
      
      pollutantType: 'nitrogen',
      waterQualityLayers: ['waterQualityLayer'],
      
      windErosionPeriod: 'year',
      windSandLayers: ['windSpeedLayer'],
      
      floodReturnPeriod: '20',
      floodLayers: ['floodAreaLayer'],
      
      carbonPeriod: [new Date(), new Date()],
      carbonLayers: ['vegetationCarbonLayer'],
      
      cropType: 'wheat',
      foodLayers: ['croplandLayer'],
      
      activeEcoService: ['potentialSupply0', 'potentialSupply1', 'potentialSupply2', 'potentialSupply3', 
                        'potentialSupply4', 'potentialSupply5', 'potentialSupply6', 'potentialSupply7'],
      
      indicators0: 'baseflow',
      timeScale0: 'year',
      spatialPattern0: 'mode',
      
      benefit0: {
        downstream: '',
        irrigation: ''
      },
      
      supplyOptimization0: 'vegetation',
      demandControl0: 'quota',
      
      indicators1: 'runoff',
      timeScale1: 'year',
      spatialPattern1: 'max',
      
      benefit1: {
        urban: '',
        industry: ''
      },
      
      supplyOptimization1: 'protection',
      demandControl1: 'efficiency',
      
      indicators2: 'erosion',
      timeScale2: 'year',
      spatialPattern2: 'min',
      
      benefit2: {
        farmers: '',
        reservoir: ''
      },
      
      supplyOptimization2: 'vegetation',
      demandControl2: 'farming',
      
      indicators3: 'nitrogenPurification',
      timeScale3: 'year',
      spatialPattern3: 'max',
      
      benefit3: {
        aquaculture: '',
        drinking: ''
      },
      
      supplyOptimization3: 'wetland',
      demandControl3: 'emission',
      
      indicators4: 'sandFlux',
      timeScale4: 'year',
      spatialPattern4: 'min',
      
      benefit4: {
        forestry: '',
        urban: ''
      },
      
      supplyOptimization4: 'shelter',
      demandControl4: 'grazing',
      
      indicators5: 'peakReduction',
      timeScale5: 'year',
      spatialPattern5: 'max',
      
      benefit5: {
        floodControl: '',
        riverside: ''
      },
      
      supplyOptimization5: 'wetland',
      demandControl5: 'floodplain',
      
      indicators6: 'vegetationCarbon',
      timeScale6: 'year',
      spatialPattern6: 'max',
      
      benefit6: {
        carbonTrading: '',
        climateChange: ''
      },
      
      supplyOptimization6: 'afforestation',
      demandControl6: 'emissionCap',
      
      indicators7: 'cropYield',
      timeScale7: 'year',
      spatialPattern7: 'max',
      
      benefit7: {
        farmers: '',
        processing: ''
      },
      
      supplyOptimization7: 'highYield',
      demandControl7: 'conservation',
      
      chartObservers: [],
      
      chartStatus: {
        0: false, 1: false, 2: false, 3: false,
        4: false, 5: false, 6: false, 7: false
      },
      
      chartVisibility: {},
      
      currentYear: 2022,
      isTimePlayActive: false,
      timePlayInterval: null,
      minYear: 1985,
      maxYear: 2022,
      yearStep: 1,
      animationSpeed: 800,
      timeMarks: {
        '1985': '1985',
        '1990': '1990',
        '1995': '1995',
        '2000': '2000',
        '2005': '2005',
        '2010': '2010',
        '2015': '2015',
        '2020': '2020',
        '2022': '2022'
      },
      
      // 添加缓存对象
      dataCache: {
        supplyData: {},
        demandData: {},
        regionDetails: {}
      },

      // 添加状态标记以避免重复操作
      stateFlags: {
        panelsInitialized: false,
        mapRendered: false
      },
      
      // 添加场景模型变量
      scenario0: 'baseline',
      scenario1: 'baseline',
      scenario2: 'baseline',
      scenario3: 'baseline',
      scenario4: 'baseline',
      scenario5: 'baseline',
      scenario6: 'baseline',
      scenario7: 'baseline',
    };
  },
  created() {
    // 从路由参数中获取regionId
    const regionId = this.$route.query.regionId;
    if (regionId) {
      this.regionInfo.regionId = regionId;
      // 并行请求数据提高加载速度
      Promise.all([
        this.fetchRegionDetail(regionId),
        this.fetchRegionServiceTypes(regionId),
        this.loadServiceTypeDictionary()
      ]).then(() => {
        // 数据加载完成后设置标志
        this.$nextTick(() => {
          this.stateFlags.panelsInitialized = true;
        });
      }).catch(error => {
        this.$message.error('初始化数据失败：' + (error.message || '未知错误'));
      });
    } else {
      this.$message.warning('未提供区域ID，无法加载区域信息');
    }
  },
  mounted() {
    this.initMap();
    this.initFullscreenEvents();
    
    // 添加全局resize事件处理
    window.addEventListener('resize', this.handleResize);
    
    // 使用IntersectionObserver优化初始化逻辑
    if ('IntersectionObserver' in window) {
      this.setupLazyInitialization();
    } else {
      // 降级处理：在组件挂载后，预加载菜单项并打开第一个面板
      this.$nextTick(() => {
        // 等待500ms确保初始渲染完成
        setTimeout(() => {
          if (this.menuItems.length > 0) {
            this.togglePanel(0);
          }
        }, 500);
      });
    }
  },
  beforeDestroy() {
    // 优化资源清理逻辑，确保所有资源都被正确释放
    
    // 清理所有图表实例
    this.cleanupCharts();
    
    // 清理时间动画相关资源
    this.stopTimeAnimation();
    
    // 清理定时器
    if (this.resizeTimer) {
      clearTimeout(this.resizeTimer);
      this.resizeTimer = null;
    }
    if (this._yearChangeTimer) {
      clearTimeout(this._yearChangeTimer);
      this._yearChangeTimer = null;
    }
    if (this._chartRenderTimer) {
      clearTimeout(this._chartRenderTimer);
      this._chartRenderTimer = null;
    }
    
    // 移除所有事件监听器
    document.removeEventListener('fullscreenchange', this.handleFullscreenChange);
    document.removeEventListener('webkitfullscreenchange', this.handleFullscreenChange);
    document.removeEventListener('mozfullscreenchange', this.handleFullscreenChange);
    document.removeEventListener('MSFullscreenChange', this.handleFullscreenChange);
    window.removeEventListener('resize', this.handleResize);
    
    // 销毁地图实例
    if (this.map) {
      // 先移除所有图层以确保它们的资源被释放
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
      // 创建基础图层 - 使用懒加载策略提高初始加载速度
      const baseLayers = [
        // 天地图影像底图
        new TileLayer({
          source: new XYZ({
            url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=img_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
            maxZoom: 18,
            preload: 0, // 减少预加载，按需加载提高性能
            transition: 200 // 添加切换动画但保持较短以避免性能损失
          }),
          preload: 0
        }),
        // 天地图标注图层
        new TileLayer({
          source: new XYZ({
            url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=cia_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
            maxZoom: 18,
            preload: 0
          }),
          preload: 0
        })
      ];
      
      // 创建地图实例
      this.map = new Map({
        target: 'map',
        controls: [],
        layers: baseLayers,
        view: new View({
          center: fromLonLat(this.initialMapCenter),
          zoom: this.initialMapZoom,
          minZoom: 4,
          maxZoom: 19,
          constrainResolution: true
        }),
        pixelRatio: window.devicePixelRatio > 1 ? 2 : 1, // 根据设备像素比优化渲染
        loadTilesWhileInteracting: true,
        loadTilesWhileAnimating: true
      });
      
      // 设置视图渲染间隔以优化CPU使用
      this.map.once('rendercomplete', () => {
        console.log('地图加载完成');
        this.map.updateSize(); // 确保地图尺寸正确
      });
    },
    
    loadServiceTypeDictionary() {
      this.getDicts("sys_service_type").then(response => {
        if (response && response.data) {
          this.serviceTypeOptions = response.data;
          
          if (this.serviceTypeOptions.length > 0) {
            const firstItem = this.serviceTypeOptions[0];
            
            if (firstItem.listClass) {
            } else if (firstItem.list_class) {
              
              this.serviceTypeOptions.forEach(item => {
                if (item.list_class) {
                  item.listClass = item.list_class;
                }
              });
            }
          }
          
          if (this.regionDetail || this.serviceTypes.length > 0) {
            this.generateMenuItems();
          }
        } else {
          this.$message.error("加载服务类型字典失败");
        }
      }).catch(error => {
        this.$message.error("获取服务类型字典失败");
      });
    },
    
    fetchRegionDetail(regionId) {
      // 检查缓存
      if (this.dataCache.regionDetails[regionId]) {
        this.regionDetail = this.dataCache.regionDetails[regionId];
        this.regionInfo.regionName = this.regionDetail.regionName;
        
        if (this.serviceTypeOptions.length > 0) {
          this.generateMenuItems();
        }
        return Promise.resolve();
      }
      
      this.loading = true;
      return getProject_region(regionId).then(response => {
        if (response && response.code === 200 && response.data) {
          this.regionDetail = response.data;
          this.regionInfo.regionName = response.data.regionName;
          
          // 存入缓存
          this.dataCache.regionDetails[regionId] = response.data;
          
          if (this.serviceTypeOptions.length > 0) {
            this.generateMenuItems();
          }
        } else {
          this.$message.error('获取区域详细信息失败');
        }
        this.loading = false;
      }).catch(error => {
        this.$message.error('获取区域详细信息失败');
        this.loading = false;
        return Promise.reject(error);
      });
    },
    
    fetchRegionServiceTypes(regionId) {
      listProject_region_service({ regionId: regionId }).then(response => {
        if (response && response.code === 200 && response.rows) {
          this.serviceTypes = response.rows.map(item => item.serviceType);
          
          if (this.serviceTypeOptions.length > 0) {
            this.generateMenuItems();
          }
        } else {
          this.$message.warning('获取区域服务类型失败或无服务类型');
        }
      }).catch(error => {
        this.$message.error('获取区域服务类型失败');
      });
    },
    
    generateMenuItems() {
      const items = [];
      const processedTypes = new Set();
      
      if (this.regionDetail && this.regionDetail.description) {
        const typeNames = this.regionDetail.description.split('、');
        
        typeNames.forEach(typeName => {
          const dictItem = this.serviceTypeOptions.find(item => item.dictLabel === typeName);
          
          if (dictItem) {
            const typeId = dictItem.dictValue;
            
            if (processedTypes.has(typeId)) return;
            
            items.push(this.createMenuItem(dictItem.dictLabel, typeId));
            processedTypes.add(typeId);
          }
        });
      }
      
      if (items.length === 0 && this.serviceTypes.length > 0) {
        this.serviceTypes.forEach(typeId => {
          if (processedTypes.has(typeId)) return;
          
          const dictItem = this.serviceTypeOptions.find(item => item.dictValue === typeId);
          
          if (dictItem) {
            items.push(this.createMenuItem(dictItem.dictLabel, typeId));
            processedTypes.add(typeId);
          }
        });
      }
      
      if (items.length === 0) {
        if (this.serviceTypeOptions.length > 0) {
          const defaultType = this.serviceTypeOptions[0];
          items.push(this.createMenuItem(defaultType.dictLabel, defaultType.dictValue));
        } else {
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
    },
    
    createMenuItem(typeName, typeId) {
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
      
      const typeIdStr = String(typeId);
      
      const dictItem = this.serviceTypeOptions.find(item => String(item.dictValue) === typeIdStr);
      
      let icon = 'el-icon-menu';
      
      if (dictItem) {
        if (dictItem.listClass) {
          icon = dictItem.listClass;
        } 
        else if (dictItem.list_class) {
          icon = dictItem.list_class;
        }
        else if (dictItem.cssClass) {
          icon = dictItem.cssClass;
        }
        else {
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
          }
        }
      }
      
      return {
        nameTop: nameTop,
        nameBottom: nameBottom,
        fullName: typeName,
        icon: icon,
        type: typeIdStr
      };
    },
    
    parseTime,
    
    togglePanel(index) {
      // 性能优化：如果点击的是已经激活的面板，则关闭它
      if (this.activePanelIndex === index) {
        this.activePanelIndex = null;
        // 清理图表资源
        this.cleanupChartByIndex(index);
        return;
      }
      
      // 保存之前面板的索引用于清理
      const prevIndex = this.activePanelIndex;
      this.activePanelIndex = index;
      
      // 清理之前的图表资源
      if (prevIndex !== null && prevIndex !== index) {
        this.cleanupChartByIndex(prevIndex);
      }
      
      // 延迟渲染新面板的图表以确保DOM已更新
      this.$nextTick(() => {
        // 优化：不要自动渲染图表，等待用户点击按钮
        // 这里只是准备容器
      });
    },
    
    // 添加按索引清理图表的方法
    cleanupChartByIndex(index) {
      if (index !== null && this.menuItems[index]) {
        const typeId = this.menuItems[index].type;
        if (typeId) {
          const chartId = `simpleChart${typeId}`;
          const chartDom = document.getElementById(chartId);
          if (chartDom) {
            const chart = echarts.getInstanceByDom(chartDom);
            if (chart) {
              chart.dispose();
            }
          }
        }
      }
    },
    
    cleanupCharts() {
      // 使用更高效的方式清理图表实例
      if (typeof echarts.getInstanceByDom === 'function') {
        document.querySelectorAll('[id^="simpleChart"]').forEach(dom => {
          const chart = echarts.getInstanceByDom(dom);
          if (chart) {
            chart.dispose();
          }
        });
      }
    },
    
    renderChart(serviceType) {
      // 确保serviceType是字符串
      const typeId = String(serviceType);
      
      // 设置此图表为可见
      this.$set(this.chartVisibility, typeId, true);
      
      // 根据类型ID构建图表容器ID
      const chartId = `simpleChart${typeId}`;
      
      // 类型名称映射
      const typeNameMap = {
        '0': '水源涵养',
        '1': '水源供给',
        '2': '土壤保持',
        '3': '水质净化',
        '4': '防风固沙',
        '5': '洪水调蓄',
        '6': '固碳服务',
        '7': '粮食供给'
      };
      
      // 获取服务类型名称
      let chartName = '供需差额';
      // 查找当前服务类型对应的菜单项
      const menuItem = this.menuItems.find(item => String(item.type) === typeId);
      if (menuItem) {
        chartName = menuItem.fullName;
      } else {
        // 如果在菜单中找不到，回退到映射表查找
        chartName = typeNameMap[typeId] || '供需差额';
      }
      
      // 显示加载提示
      const loadingMessage = this.$message({
        message: `正在绘制${chartName}图表...`,
        type: 'info',
        duration: 0,
        showClose: true
      });
      
      // 使用nextTick确保DOM已更新，并使用防抖处理避免频繁渲染
      clearTimeout(this._chartRenderTimer);
      this._chartRenderTimer = setTimeout(() => {
        const chartDom = document.getElementById(chartId);
        if (!chartDom) {
          loadingMessage.close();
          this.$message.error(`找不到图表容器: ${chartId}`);
          return;
        }
        
        try {
          // 清除可能存在的旧图表实例
          const existingChart = echarts.getInstanceByDom(chartDom);
          if (existingChart) {
            existingChart.dispose();
          }
          
          // 创建新的图表实例 - 使用空白渲染器初始化以提高速度
          const chart = echarts.init(chartDom, null, { renderer: 'canvas' });
          
          // 获取供给和需求数据 - 预计算以提高性能
          const supplyData = this.getSupplyData(typeId);
          const demandData = this.getDemandData(typeId);
          
          // 计算差额数据
          const balanceData = supplyData.map((supply, index) => supply - demandData[index]);
          
          // 月份数据
          const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
          
          // 优化图表配置以改善性能
          const option = {
            animation: false, // 禁用动画以提高性能
            title: {
              text: `${chartName}供需关系`,
              left: 'center',
              textStyle: { fontSize: 16, fontWeight: 'bold' }
            },
            tooltip: {
              trigger: 'axis',
              confine: true, // 限制tooltip在容器内，避免渲染问题
              formatter: function(params) {
                let result = params[0].name + '<br/>';
                params.forEach(param => {
                  const color = param.seriesName === '差额' ? 
                    (param.value >= 0 ? '#67C23A' : '#F56C6C') : 
                    param.color;
                  result += `<span style="display:inline-block;width:10px;height:10px;border-radius:50%;background-color:${color};margin-right:5px;"></span>`;
                  result += `${param.seriesName}: ${param.value}<br/>`;
                });
                return result;
              }
            },
            legend: {
              data: ['供给', '需求', '差额'],
              top: 30,
              textStyle: { fontSize: 12 },
              selectedMode: false
            },
            grid: {
              left: '3%',
              right: '4%',
              bottom: '10%',
              top: '20%',
              containLabel: true
            },
            xAxis: {
              type: 'category',
              boundaryGap: false,
              data: months,
              axisLine: { lineStyle: { color: '#999' } },
              axisLabel: { color: '#666' }
            },
            yAxis: {
              type: 'value',
              splitLine: { lineStyle: { type: 'dashed', color: '#DDD' } },
              axisLabel: { color: '#666' }
            },
            series: [
              {
                name: '供给',
                type: 'line',
                data: supplyData,
                smooth: true,
                symbol: 'circle',
                symbolSize: 8,
                itemStyle: { color: '#409EFF' },
                lineStyle: { width: 3 },
                z: 3
              },
              {
                name: '需求',
                type: 'line',
                data: demandData,
                smooth: true,
                symbol: 'triangle',
                symbolSize: 8,
                itemStyle: { color: '#F56C6C' },
                lineStyle: { width: 3 },
                z: 2
              },
              {
                name: '差额',
                type: 'line',
                data: balanceData,
                smooth: true,
                symbol: 'diamond',
                symbolSize: 8,
                lineStyle: { type: 'dashed', width: 2 },
                itemStyle: {
                  color: function(params) {
                    return params.value >= 0 ? '#67C23A' : '#F56C6C';
                  }
                },
                areaStyle: {
                  opacity: 0.2,
                  color: function(params) {
                    return params.value >= 0 ? 'rgba(103,194,58,0.3)' : 'rgba(245,108,108,0.3)';
                  }
                },
                z: 1
              }
            ]
          };
          
          // 渲染成功标志
          let renderSuccess = false;
          
          // 添加渲染完成事件，使用一次性事件监听
          const handleRendered = () => {
            if (renderSuccess) return; // 防止多次触发
            renderSuccess = true;
            
            // 关闭加载中提示
            loadingMessage.close();
            
            // 显示成功提示
            this.$message({
              message: `${chartName}图表绘制完成`,
              type: 'success',
              duration: 2000
            });
            
            // 自行移除事件监听器（替代once功能）
            if (typeof chart.off === 'function') {
              chart.off('rendered', handleRendered);
              chart.off('finished', handleRendered);
            }
          };
          
          // 设置图表选项
          chart.setOption(option, true); // 使用notMerge=true提高性能
          
          // 尝试使用ECharts事件API
          try {
            if (typeof chart.on === 'function') {
              // 尝试所有可能的事件
              chart.on('rendered', handleRendered);
              chart.on('finished', handleRendered);
              
              // 如果chart支持getZr方法，使用其on方法（兼容更多版本）
              if (typeof chart.getZr === 'function') {
                const zr = chart.getZr();
                if (zr && typeof zr.on === 'function') {
                  zr.on('rendered', handleRendered);
                }
              }
            } else {
              // 如果on方法不可用，直接调用回调函数
              setTimeout(handleRendered, 100);
            }
          } catch (error) {
            console.error('添加图表事件监听器失败:', error);
            // 直接执行回调函数
            setTimeout(handleRendered, 100);
          }
          
          // 设置超时保障，确保最终会关闭loading
          setTimeout(() => {
            handleRendered();
          }, 1000); // 减少超时时间，提高响应性
          
        } catch (error) {
          // 处理错误
          loadingMessage.close();
          this.$message.error(`图表绘制失败: ${error.message}`);
          console.error('图表绘制错误:', error);
          
          // 尝试备用的简单图表渲染
          this.renderSimpleChartFallback(chartId, typeId, chartName);
        }
      }, 50); // 短时间防抖
    },
    
    // 备用的简单图表渲染方法
    renderSimpleChartFallback(chartId, typeId, chartName) {
      try {
        const chartDom = document.getElementById(chartId);
        if (!chartDom) return;
        
        // 清除DOM中的所有内容
        chartDom.innerHTML = '';
        
        // 获取数据
        const supplyData = this.getSupplyData(typeId);
        const demandData = this.getDemandData(typeId);
        
        // 计算差额数据
        const balanceData = supplyData.map((supply, index) => supply - demandData[index]);
        
        // 创建表格展示数据
        const table = document.createElement('table');
        table.style.width = '100%';
        table.style.borderCollapse = 'collapse';
        table.style.textAlign = 'center';
        table.style.marginTop = '20px';
        
        // 创建表头
        const thead = document.createElement('thead');
        const headerRow = document.createElement('tr');
        headerRow.style.backgroundColor = '#f2f6fc';
        
        ['月份', '供给', '需求', '差额'].forEach(text => {
          const th = document.createElement('th');
          th.style.padding = '8px';
          th.style.border = '1px solid #ddd';
          th.textContent = text;
          headerRow.appendChild(th);
        });
        
        thead.appendChild(headerRow);
        table.appendChild(thead);
        
        // 创建表体
        const tbody = document.createElement('tbody');
        const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
        
        months.forEach((month, index) => {
          const row = document.createElement('tr');
          
          // 月份单元格
          const monthCell = document.createElement('td');
          monthCell.style.padding = '8px';
          monthCell.style.border = '1px solid #ddd';
          monthCell.textContent = month;
          row.appendChild(monthCell);
          
          // 供给单元格
          const supplyCell = document.createElement('td');
          supplyCell.style.padding = '8px';
          supplyCell.style.border = '1px solid #ddd';
          supplyCell.textContent = supplyData[index];
          supplyCell.style.color = '#409EFF';
          row.appendChild(supplyCell);
          
          // 需求单元格
          const demandCell = document.createElement('td');
          demandCell.style.padding = '8px';
          demandCell.style.border = '1px solid #ddd';
          demandCell.textContent = demandData[index];
          demandCell.style.color = '#F56C6C';
          row.appendChild(demandCell);
          
          // 差额单元格
          const balanceCell = document.createElement('td');
          balanceCell.style.padding = '8px';
          balanceCell.style.border = '1px solid #ddd';
          balanceCell.textContent = balanceData[index];
          balanceCell.style.color = balanceData[index] >= 0 ? '#67C23A' : '#F56C6C';
          row.appendChild(balanceCell);
          
          tbody.appendChild(row);
        });
        
        table.appendChild(tbody);
        
        // 添加标题
        const title = document.createElement('div');
        title.style.textAlign = 'center';
        title.style.fontSize = '16px';
        title.style.fontWeight = 'bold';
        title.style.margin = '10px 0';
        title.textContent = `${chartName}供需关系 (表格备用视图)`;
        
        // 清除图表容器并添加备用表格
        chartDom.appendChild(title);
        chartDom.appendChild(table);
        
        // 设置此图表为可见
        this.$set(this.chartVisibility, typeId, true);
        
        console.log('已使用备用表格视图渲染图表数据');
      } catch (fallbackError) {
        console.error('备用图表渲染也失败了:', fallbackError);
      }
    },
    
    setupResizeHandler() {
      window.removeEventListener('resize', this.handleResize);
      window.addEventListener('resize', this.handleResize);
    },
    
    handleResize() {
      if (this.resizeTimer) {
        clearTimeout(this.resizeTimer);
      }
      
      this.resizeTimer = setTimeout(() => {
        // 调整地图大小
        if (this.map) {
          this.map.updateSize();
        }
        
        // 只重新调整可见图表的大小
        const activeType = this.activePanelIndex !== null ? 
          (this.menuItems[this.activePanelIndex] && this.menuItems[this.activePanelIndex].type) : null;
        
        if (activeType) {
          const chartId = `simpleChart${activeType}`;
          const chartDom = document.getElementById(chartId);
          if (chartDom) {
            const chart = echarts.getInstanceByDom(chartDom);
            if (chart) {
              chart.resize();
            }
          }
        }
      }, 200);
    },
    
    closePanel() {
      this.activePanelIndex = null;
    },
    
    isChartVisible(typeId) {
      return this.chartVisibility[typeId] === true;
    },
    
    viewChartData(serviceType) {
      const typeId = String(serviceType);
      
      const supplyData = this.getSupplyData(typeId);
      const demandData = this.getDemandData(typeId);
      
      const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
      
      let tableData = [];
      for (let i = 0; i < 12; i++) {
        tableData.push({
          month: months[i],
          supply: supplyData[i],
          demand: demandData[i],
          balance: supplyData[i] - demandData[i]
        });
      }
      
      let tableHtml = '<table style="width:100%; border-collapse:collapse;">';
      tableHtml += '<tr style="background-color:#f2f6fc;"><th style="padding:10px;border:1px solid #ddd;">月份</th><th style="padding:10px;border:1px solid #ddd;">供给</th><th style="padding:10px;border:1px solid #ddd;">需求</th><th style="padding:10px;border:1px solid #ddd;">差额</th></tr>';
      
      tableData.forEach(row => {
        const balanceClass = row.balance >= 0 ? 'color:#67C23A;' : 'color:#dd5145;';
        tableHtml += `<tr>
          <td style="padding:10px;border:1px solid #ddd;text-align:center;">${row.month}</td>
          <td style="padding:10px;border:1px solid #ddd;text-align:center;">${row.supply}</td>
          <td style="padding:10px;border:1px solid #ddd;text-align:center;">${row.demand}</td>
          <td style="padding:10px;border:1px solid #ddd;text-align:center;${balanceClass}">${row.balance}</td>
        </tr>`;
      });
      
      tableHtml += '</table>';
      
      this.$msgbox({
        title: this.getChartTitle(typeId) + '数据',
        message: tableHtml,
        dangerouslyUseHTMLString: true,
        confirmButtonText: '关闭',
        showCancelButton: false,
        customClass: 'data-dialog'
      });
    },
    
    getChartTitle(typeId) {
      const typeNameMap = {
        '0': '水源涵养',
        '1': '水源供给',
        '2': '土壤保持',
        '3': '水质净化',
        '4': '防风固沙',
        '5': '洪水调蓄',
        '6': '固碳服务',
        '7': '粮食供给'
      };
      
      // 查找当前服务类型对应的菜单项
      const menuItem = this.menuItems.find(item => String(item.type) === typeId);
      if (menuItem) {
        return menuItem.fullName;
      } else {
        return typeNameMap[typeId] || '供需差额';
      }
    },
    
    // 获取供给数据
    getSupplyData(typeId) {
      // 首先检查缓存
      if (this.dataCache.supplyData[typeId]) {
        return this.dataCache.supplyData[typeId];
      }
      
      let data;
      switch(typeId) {
        case '0': // 水源涵养
          data = [150, 220, 180, 250, 190, 270, 250, 230, 260, 280, 210, 190];
          break;
        case '1': // 水源供给
          data = [150, 180, 200, 230, 250, 270, 260, 240, 220, 200, 180, 160];
          break;
        case '2': // 土壤保持
          data = [130, 150, 170, 190, 170, 150, 130, 120, 140, 160, 180, 160];
          break;
        case '3': // 水质净化
          data = [220, 210, 200, 190, 180, 170, 180, 190, 200, 210, 220, 230];
          break;
        case '4': // 防风固沙
          data = [70, 100, 130, 160, 190, 220, 200, 180, 150, 120, 90, 60];
          break;
        case '5': // 洪水调蓄
          data = [260, 240, 220, 200, 180, 160, 140, 160, 180, 200, 220, 240];
          break;
        case '6': // 固碳服务
          data = [90, 110, 130, 150, 170, 190, 210, 190, 170, 150, 130, 110];
          break;
        case '7': // 粮食供给
          data = [150, 180, 210, 240, 270, 300, 270, 240, 210, 180, 150, 120];
          break;
        default:
          data = Array(12).fill(0).map(() => Math.floor(Math.random() * 200) + 100);
      }
      
      // 保存到缓存
      this.dataCache.supplyData[typeId] = data;
      return data;
    },
    
    // 获取需求数据
    getDemandData(typeId) {
      // 首先检查缓存
      if (this.dataCache.demandData[typeId]) {
        return this.dataCache.demandData[typeId];
      }
      
      let data;
      switch(typeId) {
        case '0': // 水源涵养
          data = [120, 132, 101, 134, 90, 230, 210, 180, 190, 210, 150, 130];
          break;
        case '1': // 水源供给
          data = [100, 120, 140, 160, 180, 200, 190, 170, 150, 130, 110, 100];
          break;
        case '2': // 土壤保持
          data = [80, 90, 100, 110, 100, 90, 80, 70, 80, 90, 100, 90];
          break;
        case '3': // 水质净化
          data = [150, 140, 130, 120, 110, 100, 110, 120, 130, 140, 150, 160];
          break;
        case '4': // 防风固沙
          data = [30, 50, 70, 90, 110, 130, 120, 100, 80, 60, 40, 20];
          break;
        case '5': // 洪水调蓄
          data = [200, 180, 160, 140, 120, 100, 80, 100, 120, 140, 160, 180];
          break;
        case '6': // 固碳服务
          data = [50, 60, 70, 80, 90, 100, 110, 100, 90, 80, 70, 60];
          break;
        case '7': // 粮食供给
          data = [110, 130, 150, 170, 190, 210, 190, 170, 150, 130, 110, 90];
          break;
        default:
          data = Array(12).fill(0).map(() => Math.floor(Math.random() * 150) + 50);
      }
      
      // 保存到缓存
      this.dataCache.demandData[typeId] = data;
      return data;
    },
    
    // 地图控制相关方法
    // 重置地图视图到初始状态
    resetMapView() {
      const view = this.map.getView();
      view.animate({
        center: fromLonLat(this.initialMapCenter),
        zoom: this.initialMapZoom,
        duration: 1000, // 平滑动画持续1秒
        easing: function(t) {
          // 使用ease-out动画效果
          return 1 - Math.pow(1 - t, 3);
        }
      });
    },
    
    // 切换全屏模式
    toggleFullscreen() {
      const element = document.documentElement;
      
      if (!this.isFullscreen) {
        if (element.requestFullscreen) {
          element.requestFullscreen();
        } else if (element.webkitRequestFullscreen) {
          element.webkitRequestFullscreen();
        } else if (element.msRequestFullscreen) {
          element.msRequestFullscreen();
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen();
        } else if (document.webkitExitFullscreen) {
          document.webkitExitFullscreen();
        } else if (document.msExitFullscreen) {
          document.msExitFullscreen();
        }
      }
      
      this.isFullscreen = !this.isFullscreen;
    },
    
    // 地图放大
    zoomIn() {
      const view = this.map.getView();
      const currentZoom = view.getZoom();
      view.animate({
        zoom: currentZoom + 1,
        duration: 250,
        easing: function(t) {
          return t * (2 - t); // 平滑的加速度曲线
        }
      });
    },
    
    // 地图缩小
    zoomOut() {
      const view = this.map.getView();
      const currentZoom = view.getZoom();
      view.animate({
        zoom: currentZoom - 1,
        duration: 250,
        easing: function(t) {
          return t * (2 - t); // 平滑的加速度曲线
        }
      });
    },
    
    // 切换底图类型
    toggleBaseLayer() {
      // 切换到下一个底图类型
      this.currentBaseLayerIndex = (this.currentBaseLayerIndex + 1) % this.baseLayerOptions.length;
      const newBaseLayer = this.baseLayerOptions[this.currentBaseLayerIndex];
      
      // 创建新图层，但不立即添加
      let newLayer;
      if (newBaseLayer.type === 'satellite') {
        // 添加卫星影像底图
        newLayer = new TileLayer({
          source: new XYZ({
            url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=img_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
            maxZoom: 18
          }),
          opacity: 0 // 开始时完全透明
        });
      } else {
        // 添加街道底图
        newLayer = new TileLayer({
          source: new XYZ({
            url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=vec_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
            maxZoom: 18
          }),
          opacity: 0
        });
      }
      
      // 获取现有底图
      const layers = this.map.getLayers();
      const oldLayer = layers.getArray()[0];
      
      // 添加新图层到底部
      this.map.getLayers().insertAt(0, newLayer);
      
      // 设置渐变动画
      let start = null;
      const duration = 500; // 500ms的渐变
      
      const animate = (timestamp) => {
        if (!start) start = timestamp;
        const progress = (timestamp - start) / duration;
        
        if (progress < 1) {
          // 新层逐渐显示，旧层逐渐隐藏
          newLayer.setOpacity(Math.min(progress, 1));
          oldLayer.setOpacity(Math.max(1 - progress, 0));
          
          requestAnimationFrame(animate);
        } else {
          // 动画结束，完全显示新层并移除旧层
          newLayer.setOpacity(1);
          layers.remove(oldLayer);
          
          this.$message({
            message: `已切换至${newBaseLayer.name}`,
            type: 'success',
            duration: 2000
          });
        }
      };
      
      requestAnimationFrame(animate);
    },
    
    // 切换测量工具
    toggleMeasureTool() {
      this.isMeasureActive = !this.isMeasureActive;
      
      if (this.isMeasureActive) {
        // 实现测量工具的激活逻辑
        this.$message({
          message: '测量工具已激活',
          type: 'success',
          duration: 2000
        });
      } else {
        // 实现测量工具的关闭逻辑
        this.$message({
          message: '测量工具已关闭',
          type: 'info',
          duration: 2000
        });
      }
    },
    
    // 初始化全屏事件监听
    initFullscreenEvents() {
      document.addEventListener('fullscreenchange', this.handleFullscreenChange);
      document.addEventListener('webkitfullscreenchange', this.handleFullscreenChange);
      document.addEventListener('mozfullscreenchange', this.handleFullscreenChange);
      document.addEventListener('MSFullscreenChange', this.handleFullscreenChange);
    },
    
    // 处理全屏变化事件
    handleFullscreenChange() {
      this.isFullscreen = !!document.fullscreenElement || 
                         !!document.webkitFullscreenElement || 
                         !!document.mozFullScreenElement ||
                         !!document.msFullscreenElement;
    },
    
    // 获取图例样式
    getLegendStyle() {
      if (this.activePanelIndex === null || !this.menuItems[this.activePanelIndex]) {
        return {
          background: 'linear-gradient(to bottom, #3498db, #ffffff)',
          height: '50px'
        };
      }
      
      const menuItem = this.menuItems[this.activePanelIndex];
      const typeId = menuItem.type || '0';
      
      // 根据服务类型返回不同颜色的渐变
      let color;
      switch(typeId) {
        case '0': // 水源涵养
          color = '#3498db'; // 蓝色
          break;
        case '1': // 水源供给
          color = '#1abc9c'; // 青色
          break;
        case '2': // 土壤保持
          color = '#2ecc71'; // 绿色
          break;
        case '3': // 水质净化
          color = '#9b59b6'; // 紫色
          break;
        case '4': // 防风固沙
          color = '#f1c40f'; // 黄色
          break;
        case '5': // 洪水调蓄
          color = '#e74c3c'; // 红色
          break;
        case '6': // 固碳服务
          color = '#27ae60'; // 暗绿色
          break;
        case '7': // 粮食供给
          color = '#f39c12'; // 橙色
          break;
        default:
          color = '#3498db'; // 默认蓝色
      }
      
      return {
        background: `linear-gradient(to bottom, ${color}, #ffffff)`,
        height: '50px',
        width: '20px',
        borderRadius: '2px'
      };
    },
    
    // 切换时间播放状态 - 使用requestAnimationFrame提高性能
    toggleTimePlay() {
      this.isTimePlayActive = !this.isTimePlayActive;
      
      if (this.isTimePlayActive) {
        this.startTimeAnimation();
      } else {
        this.stopTimeAnimation();
      }
    },
    
    // 完全重写动画实现，使用requestAnimationFrame代替setInterval
    startTimeAnimation() {
      // 如果已经是最大年份，则重置为最小年份
      if (this.currentYear >= this.maxYear) {
        this.currentYear = this.minYear;
      }
      
      // 清除可能存在的动画帧
      this.stopTimeAnimation();
      
      // 优化变量用于动画
      const animationState = {
        startTimestamp: null,
        lastStepTime: 0
      };
      
      // 使用requestAnimationFrame实现更平滑的动画
      const animate = (timestamp) => {
        if (!this.isTimePlayActive) return;
        
        // 初始化开始时间
        if (!animationState.startTimestamp) {
          animationState.startTimestamp = timestamp;
          animationState.lastStepTime = timestamp;
        }
        
        // 计算经过的时间
        const elapsed = timestamp - animationState.lastStepTime;
        
        // 按指定间隔更新年份
        if (elapsed >= this.animationSpeed) {
          // 更新上次步进时间
          animationState.lastStepTime = timestamp;
          
          // 递增年份
          this.currentYear += this.yearStep;
          
          // 触发年份变化事件 - 只在关键帧更新地图
          this.handleYearChange(this.currentYear);
          
          // 如果达到最大年份，则停止动画
          if (this.currentYear > this.maxYear) {
            this.currentYear = this.maxYear;
            this.stopTimeAnimation();
            this.isTimePlayActive = false;
            return;
          }
        }
        
        // 继续下一帧动画
        this.timePlayInterval = requestAnimationFrame(animate);
      };
      
      // 启动动画循环
      this.timePlayInterval = requestAnimationFrame(animate);
    },
    
    // 停止时间动画 - 使用cancelAnimationFrame
    stopTimeAnimation() {
      if (this.timePlayInterval) {
        cancelAnimationFrame(this.timePlayInterval);
        this.timePlayInterval = null;
      }
    },
    
    // 处理年份变化 - 使用节流避免频繁更新
    handleYearChange(value) {
      // 更新当前年份
      this.currentYear = value;
      
      // 使用节流减少对地图的更新频率
      clearTimeout(this._yearChangeTimer);
      this._yearChangeTimer = setTimeout(() => {
        this.updateMapLayersByYear(value);
      }, 100); // 100ms节流
    },
    
    // 根据年份更新地图图层 - 添加性能优化
    updateMapLayersByYear(year) {
      // 避免不必要的图层更新
      if (this._lastYearUpdate === year) return;
      this._lastYearUpdate = year;
      
      // 如果没有活动面板或未初始化则跳过
      if (this.activePanelIndex === null || !this.menuItems[this.activePanelIndex]) {
        return;
      }
      
      const serviceType = this.menuItems[this.activePanelIndex].type;
      if (!serviceType) return;
      
      console.log(`更新地图数据: 服务类型=${serviceType}, 年份=${year}`);
      
      // 此处应添加实际的地图图层更新逻辑
      // 为避免性能问题，可以考虑使用Web Worker异步处理数据
    },
    
    // 添加新方法：使用IntersectionObserver实现懒加载
    setupLazyInitialization() {
      // 创建一个观察器来监视地图容器是否在视口中
      const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
          if (entry.isIntersecting) {
            // 地图容器可见，可以加载第一个面板
            if (this.menuItems.length > 0 && !this.stateFlags.panelsInitialized) {
              this.togglePanel(0);
              this.stateFlags.panelsInitialized = true;
            }
            
            // 一旦初始化完成，取消观察
            observer.disconnect();
          }
        });
      }, {
        threshold: 0.1 // 当10%的地图容器可见时触发
      });
      
      // 开始观察地图容器
      const mapContainer = document.querySelector('.map-container');
      if (mapContainer) {
        observer.observe(mapContainer);
      }
    },
    
    // 下载图表数据方法
    downloadChartData(serviceType) {
      const typeId = String(serviceType);
      
      const title = this.getChartTitle(typeId);
      const supplyData = this.getSupplyData(typeId);
      const demandData = this.getDemandData(typeId);
      const balanceData = supplyData.map((supply, index) => supply - demandData[index]);
      const months = ['1月', '2月', '3月', '4月', '5月', '6月', '7月', '8月', '9月', '10月', '11月', '12月'];
      
      // 创建CSV内容
      let csvContent = "月份,供给,需求,差额\n";
      
      for (let i = 0; i < 12; i++) {
        csvContent += `${months[i]},${supplyData[i]},${demandData[i]},${balanceData[i]}\n`;
      }
      
      // 创建Blob对象
      const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
      
      // 创建下载链接
      const link = document.createElement('a');
      const url = URL.createObjectURL(blob);
      
      // 设置链接属性
      link.setAttribute('href', url);
      link.setAttribute('download', `${title}供需数据.csv`);
      link.style.visibility = 'hidden';
      
      // 添加到DOM，触发点击后移除
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      this.$message({
        message: `${title}数据已下载`,
        type: 'success',
        duration: 2000
      });
    },
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
    },
    
    // 添加activePanelIndex监听器
    activePanelIndex: {
      handler(newVal) {
        // 删除这里的所有chart渲染逻辑，不再自动渲染
      }
    },
  },
  computed: {
    // 添加计算属性以减少模板中的重复计算
    
    // 当前活动服务类型
    activeServiceType() {
      if (this.activePanelIndex !== null && this.menuItems[this.activePanelIndex]) {
        return this.menuItems[this.activePanelIndex].type;
      }
      return null;
    },
    
    // 图例样式计算
    legendStyle() {
      if (this.activePanelIndex === null || !this.menuItems[this.activePanelIndex]) {
        return {
          background: 'linear-gradient(to bottom, #3498db, #ffffff)',
          height: '50px'
        };
      }
      
      const typeId = this.menuItems[this.activePanelIndex].type || '0';
      
      // 服务类型到颜色的映射
      const colorMap = {
        '0': '#3498db', // 水源涵养 - 蓝色
        '1': '#1abc9c', // 水源供给 - 青色
        '2': '#2ecc71', // 土壤保持 - 绿色
        '3': '#9b59b6', // 水质净化 - 紫色
        '4': '#f1c40f', // 防风固沙 - 黄色
        '5': '#e74c3c', // 洪水调蓄 - 红色
        '6': '#27ae60', // 固碳服务 - 暗绿色
        '7': '#f39c12'  // 粮食供给 - 橙色
      };
      
      const color = colorMap[typeId] || '#3498db';
      
      return {
        background: `linear-gradient(to bottom, ${color}, #ffffff)`,
        height: '50px',
        width: '20px',
        borderRadius: '2px'
      };
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
    font-size: 14px;
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
    padding: 4px 8px 8px;

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
        color: #336699;
        font-weight: bold;
      }
    }

    .layer-list {
      margin-top: 15px;
    }

    .region-info-collapse {
      margin-bottom: 10px;
    }

    .panel-specific-content {
      margin-top: 5px;
      
      .el-divider {
        margin: 12px 0;
      }
      
      .parameter-control {
        margin: 8px 0;
        
        span {
          display: block;
          margin-bottom: 6px;
          color: #409EFF;
          font-weight: bold;
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
  left: 57px;
  top: 5px;
  width: 300px;
  background: #34495e;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.3);
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
    background: #34495e;
    border-bottom: 1px solid rgba(255, 255, 255, 0.1);
    
    span {
      font-weight: bold;
      color: white;
    }
    
    .panel-controls {
      i {
        cursor: pointer;
        color: white;
        
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
    background-color: #f8f9fc;
    
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

// 添加图表控制按钮的样式
.chart-controls {
  display: flex;
  justify-content: center;
  gap: 10px; // 按钮之间的间距
  margin: 10px 0 5px; // 减少下边距
}

// 添加图表容器样式
.chart-container {
  margin: 5px 0 10px !important; // 减少上边距，增加important确保优先级
  border: 1px solid #eee;
  background-color: #fff;
  padding: 10px;
  border-radius: 4px; // 添加圆角使其更美观
  width: 100%; // 确保宽度为100%
  box-sizing: border-box; // 确保padding不会导致宽度溢出
  
  & > div {
    width: 100% !important; // 确保图表div宽度为100%
    height: 300px !important; // 固定高度
  }
}

.map-control-panel {
  position: absolute;
  left: 3px;
  top: 5px;
  width: 46px;
  background: #34495e;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  padding-top: 4px;
  padding-bottom: 4px;
  justify-content: flex-start;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);

  .map-control-button {
    height: 46px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    color: #fff;
    transition: all 0.3s;
    padding: 0;
    margin-bottom: 4px;

    &:first-child {
      height: 46px;
      padding: 0;
    }

    &:last-child {
      margin-bottom: 0;
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
      font-size: 18px;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background-color: rgba(255, 255, 255, 0.2);
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.3s;
      margin-bottom: 2px;
    }

    .map-control-title {
      font-size: 10px;
      text-align: center;
      line-height: 1.2;
      width: 100%;
      
      div {
        height: 10px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}

.reset-map-view {
  background-color: #409EFF;
}

.toggle-fullscreen {
  background-color: #67C23A;
}

.zoom-in {
  background-color: #E74C3C;
}

.zoom-out {
  background-color: #9B59B6;
}

.toggle-base-layer {
  background-color: #F39C12;
}

.toggle-measure-tool {
  background-color: #2ECC71;
}

.legend-panel {
  position: absolute;
  left: 3px;
  top: calc(12px + 6 * 60px + 50px); /* Increased the spacing from 30px to 50px */
  width: 50px;
  background: #34495e;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  padding: 8px 5px;
  z-index: 999;
  color: #fff;

  .legend-header {
    font-size: 12px;
    font-weight: bold;
    margin-bottom: 8px;
    text-align: center;
    color: #fff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    padding-bottom: 5px;
  }

  .legend-content {
    display: flex;
    flex-direction: column;
    align-items: center;

    .legend-gradient-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      width: 100%;

      .legend-gradient {
        width: 20px;
        height: 50px;
        border-radius: 2px;
        margin-bottom: 3px;
      }

      .legend-values {
        display: flex;
        justify-content: space-between;
        width: 100%;
        font-size: 8px;
        color: rgba(255, 255, 255, 0.8);
        
        .min-value {
          margin-right: auto;
        }
        
        .max-value {
          margin-left: auto;
        }
      }
    }
  }
}

.legend-gradient-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-bottom: 10px;
}

.time-control-slider {
  position: absolute;
  bottom: 25px;
  left: 40%;
  transform: translateX(-50%);
  width: 650px;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;

  .slider-container {
    width: 100%;
    position: relative;
    padding: 0;
    
    .el-slider {
      width: 100%;
      margin: 0;
      padding: 0;
      
      ::v-deep .el-slider__runway {
        height: 8px;
        background-color: #e4e7ed;
        border-radius: 4px;
        margin: 0;
      }
      
      ::v-deep .el-slider__bar {
        height: 8px;
        background-color: #344a6c;
        border-radius: 4px;
      }
      
      ::v-deep .el-slider__button-wrapper {
        top: -6px;
        height: 24px; /* Fixed height to control spacing */
      }
      
      ::v-deep .el-slider__button {
        width: 20px;
        height: 20px;
        border: 3px solid #fff;
        background-color: #344a6c;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
      }
      
      ::v-deep .el-slider__marks {
        top: 8px; /* Reduced from 14px */
        height: 0; /* Ensure no extra space */
      }
      
      ::v-deep .el-slider__marks-text {
        display: none;
      }
    }
  }

  .controls-row {
    width: 100%;
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-top: 2px; /* Positive margin to create more space */
    
    .year-label {
      padding: 6px 20px;
      background-color: #344a6c;
      color: white;
      font-weight: bold;
      border-radius: 20px;
      font-size: 14px;
      min-width: 70px;
      text-align: center;
    }
    
    .center-controls {
      display: flex;
      align-items: center;
      gap: 8px;
      
      .play-button {
        cursor: pointer;
        width: 36px;
        height: 36px;
        background: #344a6c;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
        
        i {
          font-size: 18px;
          color: white;
        }
        
        &:hover {
          background: #425b83;
        }
      }
      
      .current-year {
        padding: 6px 20px;
        background-color: #344a6c;
        color: white;
        font-weight: bold;
        border-radius: 20px;
        font-size: 14px;
        min-width: 60px;
        text-align: center;
      }
    }
  }
}
</style>

<!-- Global styles for overriding Element UI collapse panels -->
<style lang="scss">
/* Override collapse panel styles to match the dark blue design */
.panel-content .layer-list .el-collapse-item__header,
.panel-content .region-info-collapse .el-collapse-item__header,
.panel-content .el-collapse .el-collapse-item__header {
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
.panel-content .region-info-collapse .el-collapse-item__arrow,
.panel-content .el-collapse .el-collapse-item__arrow {
  color: white !important;
  margin: 0 !important;
  position: absolute !important;
  right: 12px !important; /* 调整箭头右侧间距 */
  top: 50% !important; /* 垂直居中 */
  transform: translateY(-50%) rotate(-90deg) !important; /* 添加垂直居中的transform */
  font-size: 12px !important; /* 增大箭头尺寸 */
  transition: transform 0.3s !important;
  z-index: 2 !important; /* 确保箭头在最上层 */
}

.panel-content .layer-list .el-collapse-item__header.is-active .el-collapse-item__arrow,
.panel-content .region-info-collapse .el-collapse-item__header.is-active .el-collapse-item__arrow,
.panel-content .el-collapse .el-collapse-item__header.is-active .el-collapse-item__arrow {
  transform: translateY(-50%) rotate(0deg) !important; /* 保持垂直居中的transform */
}

.panel-content .layer-list .el-collapse,
.panel-content .region-info-collapse .el-collapse,
.panel-content .el-collapse {
  border-top: none !important;
  border-bottom: none !important;
}

.panel-content .layer-list .el-collapse-item__wrap,
.panel-content .region-info-collapse .el-collapse-item__wrap,
.panel-content .el-collapse .el-collapse-item__wrap {
  border-bottom: none !important;
}

/* 添加供给赤字图表的样式 */
.chart-container {
  width: 100%;
  height: 300px;
  background-color: #f5f7fa;
  border-radius: 4px;
      display: flex;
  align-items: center;
  justify-content: center;
}

/* 确保所有折叠项使用统一的样式 */
.panel-specific-content .el-collapse .el-collapse-item__header {
  background-color: #34495e !important;
  color: white !important;
  border-bottom: 1px solid white !important;
}

.panel-specific-content .el-collapse .el-collapse-item__content {
  padding: 3px 8px 15px !important;
  background-color: #f8f9fc !important;
}
</style>

<!-- 额外的折叠项内容间距样式 -->
<style lang="scss">
.panel-specific-content .el-collapse .el-collapse-item__content {
  padding: 3px 8px 15px !important;
  background-color: #f8f9fc !important;
}

/* 优化折叠项内第一个元素的间距 */
.parameter-control:first-child {
  margin-top: 4px !important;
}

/* 确保折叠面板内容区域紧凑 */
.el-collapse-item__content > .parameter-control:first-child {
  margin-top: 4px !important;
}

/* 进一步优化所有折叠项内容区域与标题的间距 */
.el-collapse-item__content > div:first-child {
  margin-top: 2px !important;
}

/* 设置折叠项内容的背景色和减小内边距 */
.panel-specific-content .el-collapse .el-collapse-item__content {
  padding: 3px 8px 15px !important;
  background-color: #f8f9fc !important;
}

/* 调整折叠项标题的左内边距 */
.panel-specific-content .el-collapse .el-collapse-item__header {
  padding: 0 35px 0 8px !important;
}

/* 为面板中所有标题文本设置蓝色样式 */
.panel-content span.label,
.panel-content .search-box label,
.panel-content .el-form-item__label {
  color: #409EFF !important;
}

/* 确保单选框和复选框的标签文本为黑色 */
.panel-content .el-radio__label,
.panel-content .el-checkbox__label {
  color: #606266 !important;  /* 设置为Element UI默认的文本颜色 */
}

/* 确保开关组件的标签为黑色 */
.panel-content .el-switch__label {
  color: #606266 !important;
}
</style>

<style>
.data-dialog .el-message-box__message {
  max-height: 70vh;
  overflow-y: auto;
}

.data-dialog .el-message-box__content {
  padding-bottom: 20px;
}

.data-dialog .el-message-box {
  min-width: 500px;
}
</style>