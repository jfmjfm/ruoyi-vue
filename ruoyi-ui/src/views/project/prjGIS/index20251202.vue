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
    <div class="legend-panel" v-show="showLegend && activePanelIndex !== null && currentLevels.length > 0">
      <div class="legend-header">{{ activePanelIndex !== null && menuItems[activePanelIndex] ? menuItems[activePanelIndex].fullName : '图例' }}</div>
      <div class="legend-content">
        <div class="legend-items-container">
          <div 
            v-for="(level, index) in currentLevels" 
            :key="index" 
            class="legend-item">
            <div class="legend-color" :style="{ backgroundColor: getLevelColor(index) }"></div>
            <div class="legend-value">{{ level }}</div>
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
          <div class="year-label start-year">{{ formattedStartTime }}</div>
          <div class="center-controls">
            <div class="play-button" @click="toggleTimePlay">
              <i :class="isTimePlayActive ? 'el-icon-video-pause' : 'el-icon-video-play'"></i>
            </div>
            <div class="current-year">{{ formattedCurrentTime }}</div>
          </div>
          <div class="year-label end-year">{{ formattedEndTime }}</div>
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
              
              <!-- WMS图层控制 -->
              <el-divider content-position="left">WMS图层</el-divider>
              <div class="wms-control">
                <el-switch v-model="wmsLayerVisible" active-text="SWAT侵蚀图层" @change="toggleWmsLayer"></el-switch>
                <div class="opacity-control" v-if="wmsLayerVisible">
                  <span>图层透明度</span>
                  <el-slider v-model="wmsLayerOpacity" :min="0" :max="100" @change="updateWmsLayerOpacity"></el-slider>
                </div>
              </div>
              
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
import ImageLayer from 'ol/layer/Image';
import XYZ from 'ol/source/XYZ';
import ImageWMS from 'ol/source/ImageWMS';
import { fromLonLat, transformExtent } from 'ol/proj';
import { defaults as defaultControls } from 'ol/control';

import { getProject_region } from "@/api/project/project_region";
import { listProject_region_service } from "@/api/project/project_region_service";
import { listProject_service_case } from "@/api/project/project_service_case";
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
      // 根据WMS图层边界框设置初始地图中心点和缩放级别
      // 边界框: 118.41667877737004,44.452917782192465,119.14330242199273,45.23305354431723
      initialMapCenter: [118.779990599681385, 44.84298566325485], // 计算的中心点
      initialMapZoom: 9, // 调整缩放级别以显示WMS图层区域
      
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
      
      // WMS图层控制
      wmsLayer: null,
      wmsLayerVisible: true,
      wmsLayerOpacity: 80,
      
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
      
      // 服务案例数据
      serviceCases: [],
      
      // 区域服务列表
      regionServices: [],
      
      // 当前激活的WMS配置
      activeWMSConfig: null,
      
      // 当前时间尺度和配置
      currentTimeUnit: 'yearly',  // 'yearly' 或 'monthly'
      currentTimeConfig: null,     // 当前尺度的配置对象
      
      // 添加场景模型变量
      scenario0: 'baseline',
      scenario1: 'baseline',
      scenario2: 'baseline',
      scenario3: 'baseline',
      scenario4: 'baseline',
      scenario5: 'baseline',
      scenario6: 'baseline',
      scenario7: 'baseline',
      
      // 全局错误处理器引用
      globalErrorHandler: null,
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
    // 添加全局错误处理，防止浏览器扩展错误影响主功能
    this.setupGlobalErrorHandler();
    
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
    
    // 移除全局错误处理器
    if (this.globalErrorHandler) {
      window.removeEventListener('error', this.globalErrorHandler);
      this.globalErrorHandler = null;
    }
    
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
    // 设置全局错误处理器，防止浏览器扩展错误影响主功能
    setupGlobalErrorHandler() {
      this.globalErrorHandler = (event) => {
        // 检查是否是浏览器扩展相关的错误
        const message = event.message || '';
        const filename = event.filename || '';
        
        // 常见的浏览器扩展错误特征
        const isExtensionError = 
          message.includes('Extension context invalidated') ||
          message.includes('Extension') ||
          filename.includes('extension') ||
          filename.includes('content.js') ||
          filename.includes('chrome-extension://') ||
          filename.includes('moz-extension://') ||
          filename.includes('edge-extension://');
        
        if (isExtensionError) {
          // 阻止错误传播，避免中断主功能
          event.preventDefault();
          event.stopPropagation();
          
          // 静默记录（不显示给用户，仅在控制台记录）
          console.warn('[浏览器扩展错误已忽略]', message);
          
          return true; // 表示错误已处理
        }
        
        // 其他错误正常传播
        return false;
      };
      
      // 添加错误监听器
      window.addEventListener('error', this.globalErrorHandler, true);
    },
    
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
          preload: 0,
          zIndex: 0  // 底图在最下层
        }),
        // 天地图标注图层
        new TileLayer({
          source: new XYZ({
            url: 'https://t{0-7}.tianditu.gov.cn/DataServer?T=cia_w&x={x}&y={y}&l={z}&tk=b079cd52cb89ffdc40073702b8cce199',
            maxZoom: 18,
            preload: 0
          }),
          preload: 0,
          zIndex: 1  // 标注在底图之上
        })
      ];
      
      // 添加WMS图层 - 完全透明，不遮挡底图
      const wmsLayer = new ImageLayer({
        source: new ImageWMS({
          url: 'http://172.16.124.1:31490/geoserver/repa/wms',
          params: {
            'LAYERS': 'repa:usle_swat',
            'FORMAT': 'image/png',
            'TRANSPARENT': true,
            'VERSION': '1.1.0',
            'STYLES': '',
            'BGCOLOR': '0x000000'  // 背景色设为黑色，配合TRANSPARENT使其透明
          },
          serverType: 'geoserver',
          crossOrigin: 'anonymous',
          ratio: 1.5,
          imageSmoothing: true  // 启用图像平滑
        }),
        opacity: 0.7,  // 降低透明度，让底图更明显
        visible: false,  // 初始不可见，由菜单激活时才显示
        zIndex: 10  // 在底图和标注之上
      });
      
      // 将WMS图层添加到基础图层数组中
      baseLayers.push(wmsLayer);
      
      // 创建地图实例
      this.map = new Map({
        target: 'map',
        controls: [],
        layers: baseLayers,
        view: new View({
          center: this.initialMapCenter, // 直接使用经纬度坐标，不需要转换
          zoom: this.initialMapZoom,
          minZoom: 4,
          maxZoom: 19,
          constrainResolution: true,
          projection: 'EPSG:4326' // 使用WMS图层相同的投影坐标系
        }),
        pixelRatio: window.devicePixelRatio > 1 ? 2 : 1, // 根据设备像素比优化渲染
        loadTilesWhileInteracting: true,
        loadTilesWhileAnimating: true
      });
      
      // 移除地图容器的背景色，确保完全透明
      const mapElement = document.getElementById('map');
      if (mapElement) {
        mapElement.style.backgroundColor = 'transparent';
      }
      
      // 设置视图渲染间隔以优化CPU使用
      this.map.once('rendercomplete', () => {
        console.log('地图加载完成');
        this.map.updateSize(); // 确保地图尺寸正确
      });
      
      // 保存WMS图层引用以便后续控制
      this.wmsLayer = wmsLayer;
    },
    
    // WMS图层控制方法
    toggleWmsLayer(visible) {
      if (this.wmsLayer) {
        this.wmsLayer.setVisible(visible);
      }
    },
    
    updateWmsLayerOpacity(opacity) {
      if (this.wmsLayer) {
        this.wmsLayer.setOpacity(opacity / 100);
      }
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
          
          // 只有在 serviceCases 已准备好时才生成菜单
          if (this.serviceCases && this.serviceCases.length > 0) {
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
        
        // 只有在 serviceCases 已准备好时才生成菜单
        if (this.serviceTypeOptions.length > 0 && this.serviceCases && this.serviceCases.length > 0) {
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
          
          // 只有在 serviceCases 已准备好时才生成菜单
          if (this.serviceTypeOptions.length > 0 && this.serviceCases && this.serviceCases.length > 0) {
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
          this.regionServices = response.rows;
          
          // 提取所有 region_service_id
          const regionServiceIds = response.rows.map(item => {
            const id = item.id;
            return typeof id === 'string' ? parseInt(id, 10) : id;
          });
          
          // 获取服务案例
          this.fetchServiceCases(regionServiceIds);
        } else {
          this.$message.warning('获取区域服务类型失败或无服务类型');
        }
      }).catch(error => {
        this.$message.error('获取区域服务类型失败');
      });
    },
    
    // 获取服务案例数据（is_default = 1）
    fetchServiceCases(regionServiceIds) {
      if (!regionServiceIds || regionServiceIds.length === 0) {
        this.$message.warning('未找到区域服务');
        this.generateMenuItemsFallback();
        return;
      }
      
      // 查询所有 project_service_case 记录
      listProject_service_case({
        pageNum: 1,
        pageSize: 9999
      }).then(response => {
        if (!response || response.code !== 200 || !response.rows) {
          this.$message.error('获取服务案例失败');
          this.generateMenuItemsFallback();
          return;
        }
        
        // 前端筛选：region_service_id 在列表中 且 is_default = 1
        const filteredCases = response.rows.filter(item => {
          // 获取字段值（兼容驼峰和下划线命名）
          const regionServiceId = item.regionServiceId !== undefined ? item.regionServiceId : item.region_service_id;
          const isDefault = item.isDefault !== undefined ? item.isDefault : item.is_default;
          
          // 转换为数字进行比较
          const normalizedId = typeof regionServiceId === 'string' ? parseInt(regionServiceId) : regionServiceId;
          const normalizedDefault = typeof isDefault === 'string' ? parseInt(isDefault) : isDefault;
          
          // 条件1: region_service_id 在列表中
          const inList = regionServiceIds.includes(normalizedId);
          
          // 条件2: is_default = 1
          const isDefaultOne = normalizedDefault === 1;
          
          return inList && isDefaultOne;
        });
        
        if (filteredCases.length > 0) {
          this.serviceCases = filteredCases;
          
          // 等待字典加载完成再生成菜单
          if (this.serviceTypeOptions.length > 0) {
            this.generateMenuItems();
          } else {
            // 延迟等待字典加载
            setTimeout(() => {
              if (this.serviceTypeOptions.length > 0) {
                this.generateMenuItems();
              }
            }, 500);
          }
        } else {
          this.$message.warning('未找到可用的服务案例（is_default=1）');
          this.generateMenuItemsFallback();
        }
      }).catch(error => {
        this.$message.error('获取服务案例失败');
        this.generateMenuItemsFallback();
      });
    },
    
    // 解析 case_dir 字段，提取 WMS URL 和时间配置
    parseCaseDir(caseDir) {
      if (!caseDir || typeof caseDir !== 'string') {
        return null;
      }
      
      try {
        // 解析 JSON 格式的 case_dir
        const data = JSON.parse(caseDir);
        
        if (!data.url_info) {
          console.error('case_dir 缺少 url_info 字段');
          return null;
        }
        
        // 提取基础信息
        const urlInfo = data.url_info;
        const scalesInfo = data.scales_info || [];
        
        // 返回解析后的配置
        return {
          urlInfo: urlInfo,
          scalesInfo: scalesInfo,
          baseUrl: urlInfo.base_url,
          // 为了向后兼容，保留一些字段
          wmsUrl: urlInfo.base_url,
          scales: scalesInfo
        };
      } catch (error) {
        console.error('解析 case_dir JSON 失败:', error);
        return null;
      }
    },
    
    // 检查值是否有效（非空、非null、非undefined）
    isValidValue(value) {
      if (value === null || value === undefined) return false;
      if (typeof value === "string" && value.trim() === "") return false;
      return true;
    },
    
    // 构建完整的 WMS GetMap URL
    buildWMSUrl(urlInfo, overrides = {}) {
      if (!urlInfo || !urlInfo.base_url) {
        console.error("缺少 base_url 字段");
        return "";
      }
      
      // 合并覆盖参数
      const finalUrlInfo = { ...urlInfo, ...overrides };
      
      const version = finalUrlInfo.version || "1.3.0";
      const isWMS130 = version === "1.3.0";
      const isEPSG4326 = (finalUrlInfo.crs || finalUrlInfo.srs || "").toUpperCase().includes("EPSG:4326");
      
      // URL编码键值对
      const encodeKV = (k, v) => `${encodeURIComponent(k)}=${encodeURIComponent(v)}`;
      
      // 处理对象类型参数（viewparams, env, dimensions等）
      const encodeObjectParam = (obj) => {
        return Object.entries(obj)
          .filter(([_, v]) => this.isValidValue(v))
          .map(([k, v]) => `${encodeURIComponent(k)}:${encodeURIComponent(v)}`)
          .join(";");
      };
      
      // 处理 BBOX 坐标交换（WMS 1.3.0 + EPSG:4326）
      const processBbox = (bbox) => {
        if (!isWMS130 || !isEPSG4326) return bbox;
        
        const coords = typeof bbox === "string" ? bbox.split(",").map(Number) : bbox;
        if (coords.length === 4) {
          // 交换坐标顺序：[minX, minY, maxX, maxY] → [minY, minX, maxY, maxX]
          return [coords[1], coords[0], coords[3], coords[2]].join(",");
        }
        return bbox;
      };
      
      // 构建查询参数
      const queryParts = [];
      
      for (const [key, value] of Object.entries(finalUrlInfo)) {
        // 跳过 base_url 和无效值
        if (key === "base_url" || !this.isValidValue(value)) continue;
        
        // 对象类型参数
        if (typeof value === "object" && !Array.isArray(value)) {
          const encoded = encodeObjectParam(value);
          if (encoded) queryParts.push(encodeKV(key, encoded));
        }
        // BBOX 特殊处理
        else if (key === "bbox") {
          queryParts.push(encodeKV(key, processBbox(value)));
        }
        // 基本类型参数
        else if (["string", "number", "boolean"].includes(typeof value)) {
          queryParts.push(encodeKV(key, value));
        }
      }
      
      return `${finalUrlInfo.base_url.replace(/\/+$/, '')}?${queryParts.join("&")}`;
    },
    
    // 生成时间序列（根据起始时间、结束时间和时间尺度）
    generateTimeSequence(startTime, endTime, scale, duration) {
      const times = [];
      const start = new Date(startTime);
      const end = new Date(endTime);
      
      for (let i = 0; i < duration; i++) {
        const current = new Date(start);
        
        if (scale === 'daily') {
          current.setDate(start.getDate() + i);
        } else if (scale === 'monthly') {
          current.setMonth(start.getMonth() + i);
        } else if (scale === 'yearly') {
          current.setFullYear(start.getFullYear() + i);
        }
        
        if (current <= end) {
          times.push(current.toISOString().split('T')[0]);
        }
      }
      
      return times;
    },
    
    // 基于服务案例生成菜单项
    generateMenuItems() {
      const items = [];
      
      if (this.serviceCases && this.serviceCases.length > 0) {
        this.serviceCases.forEach((serviceCase, index) => {
          // 兼容多种字段命名
          const caseDir = serviceCase.caseDir || serviceCase.case_dir;
          const caseName = serviceCase.caseName || serviceCase.case_name;
          const serviceType = serviceCase.serviceType || serviceCase.service_type;
          
          // 解析 case_dir 获取 WMS 配置
          const wmsConfig = this.parseCaseDir(caseDir);
          
          // 获取服务类型名称
          let typeName = caseName || '未命名服务';
          let typeId = serviceType || String(index);
          
          // 尝试从字典中获取服务类型信息
          const dictItem = this.serviceTypeOptions.find(item => String(item.dictValue) === String(typeId));
          if (dictItem) {
            typeName = dictItem.dictLabel || typeName;
          }
          
          // 创建菜单项
          const menuItem = this.createMenuItem(typeName, typeId);
          
          // 附加服务案例的额外信息
          menuItem.caseId = serviceCase.id;
          menuItem.wmsConfig = wmsConfig;
          menuItem.serviceCase = serviceCase;
          
          items.push(menuItem);
        });
      }
      
      if (items.length === 0) {
        this.generateMenuItemsFallback();
        return;
      }
      
      this.menuItems = items;
    },
    
    // 回退方法：使用原有逻辑生成菜单项
    generateMenuItemsFallback() {
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
        // 清除活动的 WMS 配置
        this.activeWMSConfig = null;
        // 隐藏WMS图层
        if (this.wmsLayer) {
          this.wmsLayer.setVisible(false);
        }
        // 恢复到初始视图范围（可选，平滑过渡）
        if (this.map) {
          this.map.getView().animate({
            center: this.initialMapCenter,
            zoom: this.initialMapZoom,
            duration: 800
          });
        }
        return;
      }
      
      // 保存之前面板的索引用于清理
      const prevIndex = this.activePanelIndex;
      this.activePanelIndex = index;
      
      // 清理之前的图表资源
      if (prevIndex !== null && prevIndex !== index) {
        this.cleanupChartByIndex(prevIndex);
      }
      
      // 获取当前菜单项的 WMS 配置
      const menuItem = this.menuItems[index];
      if (menuItem && menuItem.wmsConfig) {
        this.activeWMSConfig = menuItem.wmsConfig;
        
        // 获取当前服务的时间尺度
        const currentScale = this.getCurrentTimeScale(menuItem.type);
        
        console.log(`[菜单切换] 服务类型: ${menuItem.type}, 服务名称: ${menuItem.fullName}`);
        
        // 根据时间配置调整时间轴范围
        this.adjustTimelineByWMSConfig(menuItem.wmsConfig, currentScale);
        
        // 加载初始年的 WMS 图层（切换菜单时调整地图范围到WMS的bbox）
        this.$nextTick(() => {
          console.log(`[菜单切换] 加载初始时间: ${this.currentYear}`);
          this.loadWMSLayerWithLogging(menuItem.wmsConfig, this.currentYear, true, currentScale);
        });
      }
      
      // 延迟渲染新面板的图表以确保DOM已更新
      this.$nextTick(() => {
        // 优化：不要自动渲染图表，等待用户点击按钮
        // 这里只是准备容器
      });
    },
    
    // 根据 WMS 配置调整时间轴范围
    adjustTimelineByWMSConfig(wmsConfig, currentScale = 'yearly') {
      if (!wmsConfig) return;
      
      // 如果有 scalesInfo 数组（新格式），使用对应尺度的配置
      if (wmsConfig.scalesInfo && wmsConfig.scalesInfo.length > 0) {
        // 查找对应尺度的配置
        const scaleConfig = wmsConfig.scalesInfo.find(s => s.scale === currentScale);
        
        if (scaleConfig && scaleConfig.time && scaleConfig.time.length >= 2) {
          try {
            const startDate = new Date(scaleConfig.time[0]);
            const endDate = new Date(scaleConfig.time[1]);
            
            // 保存当前时间尺度配置
            this.currentTimeUnit = scaleConfig.scale;
            this.currentTimeConfig = {
              ...scaleConfig,
              startDate: startDate,
              endDate: endDate,
              startYear: startDate.getFullYear(),
              startMonth: startDate.getMonth()
            };
            
            // 根据 scale 设置时间轴
            if (scaleConfig.scale === 'yearly') {
              // 年尺度：minYear/maxYear 表示实际年份
              const startYear = startDate.getFullYear();
              const endYear = startYear + scaleConfig.duration - 1;
              
              this.minYear = startYear;
              this.maxYear = endYear;
              this.currentYear = startYear;
              
              // 生成年尺度的时间标记
              this.generateTimeMarks(startYear, endYear, scaleConfig.duration);
              
              console.log(`时间轴已调整 [年尺度]: ${startYear} - ${endYear}, 共 ${scaleConfig.duration} 年`);
              console.log(`图层: ${scaleConfig.layers}, 样式: ${scaleConfig.styles}`);
              console.log(`分级值: [${scaleConfig.levels.join(', ')}]`);
            } else if (scaleConfig.scale === 'monthly') {
              // 月尺度：minYear/maxYear 表示月份索引（0到duration-1）
              this.minYear = 0;
              this.maxYear = scaleConfig.duration - 1;
              this.currentYear = 0;
              
              // 生成月尺度的时间标记
              this.generateMonthlyTimeMarks(startDate, scaleConfig.duration);
              
              console.log(`时间轴已调整 [月尺度]: ${startDate.toISOString().substr(0,7)} 开始, 共 ${scaleConfig.duration} 个月`);
              console.log(`图层: ${scaleConfig.layers}, 样式: ${scaleConfig.styles}`);
              console.log(`分级值: [${scaleConfig.levels.join(', ')}]`);
            }
            
            return;
          } catch (error) {
            console.error('解析时间配置失败:', error);
          }
        }
      }
      
      // 如果有 scales 数组（旧格式），继续支持
      if (wmsConfig.scales && wmsConfig.scales.length > 0) {
        const scaleConfig = wmsConfig.scales.find(s => s.scale === currentScale);
        
        if (scaleConfig && scaleConfig.time && scaleConfig.time.length >= 2) {
          try {
            const startDate = new Date(scaleConfig.time[0]);
            
            this.currentTimeUnit = scaleConfig.scale;
            this.currentTimeConfig = {
              ...scaleConfig,
              startDate: startDate,
              startYear: startDate.getFullYear(),
              startMonth: startDate.getMonth()
            };
            
            if (scaleConfig.scale === 'yearly') {
              const startYear = startDate.getFullYear();
              const endYear = startYear + scaleConfig.duration - 1;
              
              this.minYear = startYear;
              this.maxYear = endYear;
              this.currentYear = startYear;
              
              this.generateTimeMarks(startYear, endYear, scaleConfig.duration);
              
              console.log(`时间轴已调整 [年尺度]: ${startYear} - ${endYear}, 共 ${scaleConfig.duration} 年`);
            } else if (scaleConfig.scale === 'monthly') {
              this.minYear = 0;
              this.maxYear = scaleConfig.duration - 1;
              this.currentYear = 0;
              
              this.generateMonthlyTimeMarks(startDate, scaleConfig.duration);
              
              console.log(`时间轴已调整 [月尺度]: ${startDate.toISOString().substr(0,7)} 开始, 共 ${scaleConfig.duration} 个月`);
            }
            
            return;
          } catch (error) {
            console.error('解析时间配置失败:', error);
          }
        }
      }
    },
    
    // 动态生成时间标记（年尺度）
    generateTimeMarks(startYear, endYear, duration) {
      const marks = {};
      const range = endYear - startYear;
      
      if (range <= 0) {
        marks[String(startYear)] = String(startYear);
        this.timeMarks = marks;
        return;
      }
      
      // 根据范围动态确定标记间隔
      let interval = 1;
      if (range > 50) {
        interval = 10;
      } else if (range > 20) {
        interval = 5;
      } else if (range > 10) {
        interval = 2;
      }
      
      // 添加起始和结束年份
      marks[String(startYear)] = String(startYear);
      
      // 添加中间标记
      for (let year = startYear + interval; year < endYear; year += interval) {
        marks[String(year)] = String(year);
      }
      
      // 添加结束年份
      marks[String(endYear)] = String(endYear);
      
      this.timeMarks = marks;
    },
    
    // 动态生成时间标记（月尺度）
    generateMonthlyTimeMarks(startDate, totalMonths) {
      const marks = {};
      
      // 根据月数确定标记间隔
      let interval = 1;
      if (totalMonths > 60) {
        interval = 12; // 每年标记一次
      } else if (totalMonths > 24) {
        interval = 6;  // 每半年标记一次
      } else if (totalMonths > 12) {
        interval = 3;  // 每季度标记一次
      }
      
      // 生成标记
      for (let i = 0; i < totalMonths; i += interval) {
        const date = new Date(startDate);
        date.setMonth(date.getMonth() + i);
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        marks[String(i)] = `${year}-${month}`;
      }
      
      // 确保包含最后一个月
      if (!marks[String(totalMonths - 1)]) {
        const lastDate = new Date(startDate);
        lastDate.setMonth(lastDate.getMonth() + totalMonths - 1);
        const year = lastDate.getFullYear();
        const month = (lastDate.getMonth() + 1).toString().padStart(2, '0');
        marks[String(totalMonths - 1)] = `${year}-${month}`;
      }
      
      this.timeMarks = marks;
    },
    
    // 加载 WMS 图层
    // adjustView: 是否调整地图视图范围（切换服务时为true，动画播放时为false）
    // currentScale: 当前时间尺度（'yearly' 或 'monthly'）
    loadWMSLayer(wmsConfig, yearOrIndex, adjustView = false, currentScale = 'yearly') {
      if (!wmsConfig) {
        console.warn('无效的 WMS 配置');
        return;
      }
      
      try {
        // 查找或创建 WMS 图层
        if (!this.wmsLayer) {
          console.warn('WMS 图层未初始化，跳过加载');
          return;
        }
        
        // 首先显示WMS图层
        this.wmsLayer.setVisible(true);
        
        // 根据新的数据结构构建 URL
        if (wmsConfig.urlInfo && wmsConfig.scalesInfo) {
          // 新格式：使用 urlInfo 和 scalesInfo
          const scaleConfig = wmsConfig.scalesInfo.find(s => s.scale === currentScale);
          
          if (!scaleConfig) {
            console.warn(`未找到 ${currentScale} 尺度配置`);
            return;
          }
          
          // 生成时间序列
          const timeSequence = this.generateTimeSequence(
            scaleConfig.time[0],
            scaleConfig.time[1],
            scaleConfig.scale,
            scaleConfig.duration
          );
          
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
          
          // 构建覆盖参数
          const overrides = {
            layers: `repa:${scaleConfig.layers}`,
            styles: `repa:${scaleConfig.styles}`
          };
          
          // 更新 viewparams 或 time 参数
          if (wmsConfig.urlInfo.viewparams && Object.keys(wmsConfig.urlInfo.viewparams).length > 0) {
            overrides.viewparams = {
              ...wmsConfig.urlInfo.viewparams,
              sim_time: currentTime,
              scale: scaleConfig.scale
            };
          } else {
            overrides.time = currentTime;
          }
          
          // 构建完整 URL
          const fullUrl = this.buildWMSUrl(wmsConfig.urlInfo, overrides);
          
          // 解析 URL 并更新图层参数
          const urlObj = new URL(fullUrl);
          const params = {};
          
          // 提取bbox参数用于调整地图范围
          let bboxString = null;
          
          // 添加URL中的参数
          urlObj.searchParams.forEach((value, key) => {
            const upperKey = key.toUpperCase();
            if (upperKey === 'BBOX') {
              bboxString = value;
            }
            params[key] = value;
          });
          
          // 更新图层参数
          const source = this.wmsLayer.getSource();
          if (source && source.updateParams) {
            source.updateParams(params);
            
            // 只有在切换服务时才调整地图视图范围到WMS图层的bbox
            if (adjustView && bboxString && this.map) {
              try {
                const bboxArray = bboxString.split(',').map(coord => parseFloat(coord.trim()));
                if (bboxArray.length === 4 && bboxArray.every(n => !isNaN(n))) {
                  const extent = bboxArray;
                  
                  this.map.getView().fit(extent, {
                    duration: 1000,
                    padding: [50, 50, 50, 50],
                    maxZoom: 12
                  });
                }
              } catch (bboxError) {
                console.warn('解析bbox失败:', bboxError);
              }
            }
            
            // 刷新图层
            this.wmsLayer.changed();
          }
        } else if (wmsConfig.wmsUrl) {
          // 旧格式：使用现有的 URL 更新逻辑（向后兼容）
          const updatedUrl = this.updateWMSUrlWithYearAndScale(wmsConfig.wmsUrl, yearOrIndex, wmsConfig.scales, currentScale);
          
          const source = this.wmsLayer.getSource();
          if (source && source.updateParams) {
            const urlObj = new URL(updatedUrl);
            const params = {};
            let bboxString = null;
            
            urlObj.searchParams.forEach((value, key) => {
              const upperKey = key.toUpperCase();
              if (upperKey === 'BBOX') {
                bboxString = value;
              }
              if (!['SERVICE', 'REQUEST', 'VERSION'].includes(upperKey)) {
                params[key] = value;
              }
            });
            
            source.updateParams(params);
            
            if (adjustView && bboxString && this.map) {
              try {
                const bboxArray = bboxString.split(',').map(coord => parseFloat(coord.trim()));
                if (bboxArray.length === 4 && bboxArray.every(n => !isNaN(n))) {
                  this.map.getView().fit(bboxArray, {
                    duration: 1000,
                    padding: [50, 50, 50, 50],
                    maxZoom: 12
                  });
                }
              } catch (bboxError) {
                console.warn('解析bbox失败:', bboxError);
              }
            }
            
            this.wmsLayer.changed();
          }
        }
      } catch (error) {
        console.error('加载 WMS 图层失败:', error);
      }
    },
    
    // 更新 WMS URL 中的年份参数（旧方法，保留用于向后兼容）
    updateWMSUrlWithYear(wmsUrl, year) {
      if (!wmsUrl) return wmsUrl;
      
      try {
        // 检查 URL 中是否有 viewparams 参数
        const urlObj = new URL(wmsUrl);
        const viewparams = urlObj.searchParams.get('viewparams');
        
        if (viewparams) {
          // 解析 viewparams，更新 sim_time
          // 格式: sim_time:2009-01-01
          const parts = viewparams.split(':');
          if (parts.length >= 2) {
            // 假设日期格式为 YYYY-MM-DD，只替换年份部分
            const oldDate = parts[1];
            const dateMatch = oldDate.match(/(\d{4})-(\d{2})-(\d{2})/);
            
            if (dateMatch) {
              const month = dateMatch[2];
              const day = dateMatch[3];
              const newDate = `${year}-${month}-${day}`;
              
              // 更新 viewparams
              const newViewparams = `sim_time:${newDate}`;
              urlObj.searchParams.set('viewparams', newViewparams);
              
              return urlObj.toString();
            }
          }
        }
        
        // 如果没有 viewparams 或解析失败，返回原 URL
        return wmsUrl;
      } catch (error) {
        console.error('更新 WMS URL 失败:', error);
        return wmsUrl;
      }
    },
    
    // 更新 WMS URL 中的年份、styles 和 scale 参数
    updateWMSUrlWithYearAndScale(wmsUrl, yearOrIndex, scales, currentScale = 'yearly') {
      if (!wmsUrl) return wmsUrl;
      
      try {
        const urlObj = new URL(wmsUrl);
        
        // 如果有 scales 配置，更新 styles 和 viewparams 中的 scale
        if (scales && scales.length > 0) {
          const scaleConfig = scales.find(s => s.scale === currentScale);
          
          if (scaleConfig) {
            // 更新 styles 参数
            if (scaleConfig.style) {
              urlObj.searchParams.set('styles', scaleConfig.style);
            }
            
            // 计算实际的日期
            let targetDate;
            if (currentScale === 'yearly') {
              // 年尺度：yearOrIndex 就是实际年份
              targetDate = `${yearOrIndex}-01-01`;
            } else if (currentScale === 'monthly' && this.currentTimeConfig) {
              // 月尺度：yearOrIndex 是月份索引，需要计算实际日期
              const monthIndex = yearOrIndex;
              const startDate = this.currentTimeConfig.startDate;
              const date = new Date(startDate);
              date.setMonth(date.getMonth() + monthIndex);
              
              const year = date.getFullYear();
              const month = (date.getMonth() + 1).toString().padStart(2, '0');
              const day = date.getDate().toString().padStart(2, '0');
              targetDate = `${year}-${month}-${day}`;
            } else {
              // 降级处理
              targetDate = `${yearOrIndex}-01-01`;
            }
            
            // 更新 viewparams 中的 sim_time 和 scale
            const viewparams = urlObj.searchParams.get('viewparams');
            if (viewparams) {
              // 解析现有的 viewparams（格式: sim_time:2009-01-01;scale:yearly）
              const params = {};
              viewparams.split(';').forEach(param => {
                const [key, value] = param.split(':');
                if (key && value) {
                  params[key.trim()] = value.trim();
                }
              });
              
              // 更新 sim_time
              params['sim_time'] = targetDate;
              
              // 更新 scale
              params['scale'] = currentScale;
              
              // 重新组装 viewparams
              const newViewparams = Object.entries(params)
                .map(([key, value]) => `${key}:${value}`)
                .join(';');
              
              urlObj.searchParams.set('viewparams', newViewparams);
            } else {
              // 如果没有 viewparams，创建新的
              const newViewparams = `sim_time:${targetDate};scale:${currentScale}`;
              urlObj.searchParams.set('viewparams', newViewparams);
            }
          }
        } else {
          // 如果没有 scales 配置，使用旧的方法
          return this.updateWMSUrlWithYear(wmsUrl, yearOrIndex);
        }
        
        return urlObj.toString();
      } catch (error) {
        console.error('更新 WMS URL 失败:', error);
        return wmsUrl;
      }
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
      // 清除活动的 WMS 配置
      this.activeWMSConfig = null;
      // 隐藏WMS图层
      if (this.wmsLayer) {
        this.wmsLayer.setVisible(false);
      }
      // 恢复到初始视图范围
      if (this.map) {
        this.map.getView().animate({
          center: this.initialMapCenter,
          zoom: this.initialMapZoom,
          duration: 800
        });
      }
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
        center: this.initialMapCenter, // 直接使用经纬度坐标
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
        lastStepTime: 0,
        isFirstFrame: true  // 标记是否为第一帧
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
        if (elapsed >= this.animationSpeed || animationState.isFirstFrame) {
          // 如果是第一帧，先显示当前帧，不递增
          if (animationState.isFirstFrame) {
            animationState.isFirstFrame = false;
            animationState.lastStepTime = timestamp;
            // 触发当前年份的显示（第一帧）
            this.handleYearChange(this.currentYear);
          } else {
            // 更新上次步进时间
            animationState.lastStepTime = timestamp;
            
            // 递增年份
            this.currentYear += this.yearStep;
            
            // 如果达到最大年份，则停止动画
            if (this.currentYear > this.maxYear) {
              this.currentYear = this.maxYear;
              this.stopTimeAnimation();
              this.isTimePlayActive = false;
              return;
            }
            
            // 触发年份变化事件 - 只在关键帧更新地图
            this.handleYearChange(this.currentYear);
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
      
      const menuItem = this.menuItems[this.activePanelIndex];
      const serviceType = menuItem.type;
      if (!serviceType) return;
      
      console.log(`更新地图数据: 服务类型=${serviceType}, 年份/索引=${year}`);
      
      // 如果有 WMS 配置，更新 WMS 图层（动画播放时不调整地图范围）
      if (this.activeWMSConfig) {
        // 获取当前服务的时间尺度
        const currentScale = this.getCurrentTimeScale(serviceType);
        
        // 生成并输出完整的 WMS URL
        this.loadWMSLayerWithLogging(this.activeWMSConfig, year, false, currentScale);
      }
    },
    
    // 带日志的 WMS 图层加载方法（用于调试）
    loadWMSLayerWithLogging(wmsConfig, yearOrIndex, adjustView = false, currentScale = 'yearly') {
      if (!wmsConfig) {
        console.warn('[WMS] 无效的 WMS 配置');
        return;
      }
      
      try {
        // 根据新的数据结构构建 URL
        if (wmsConfig.urlInfo && wmsConfig.scalesInfo) {
          // 新格式
          const scaleConfig = wmsConfig.scalesInfo.find(s => s.scale === currentScale);
          
          if (!scaleConfig) {
            console.warn(`[WMS] 未找到 ${currentScale} 尺度配置`);
            return;
          }
          
          // 生成时间序列
          const timeSequence = this.generateTimeSequence(
            scaleConfig.time[0],
            scaleConfig.time[1],
            scaleConfig.scale,
            scaleConfig.duration
          );
          
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
          
          // 构建覆盖参数
          const overrides = {
            layers: `repa:${scaleConfig.layers}`,
            styles: `repa:${scaleConfig.styles}`
          };
          
          // 更新 viewparams 或 time 参数
          if (wmsConfig.urlInfo.viewparams && Object.keys(wmsConfig.urlInfo.viewparams).length > 0) {
            overrides.viewparams = {
              ...wmsConfig.urlInfo.viewparams,
              sim_time: currentTime,
              scale: scaleConfig.scale
            };
          } else {
            overrides.time = currentTime;
          }
          
          // 构建完整 URL
          const fullUrl = this.buildWMSUrl(wmsConfig.urlInfo, overrides);
          
          // 输出详细的 WMS 请求信息
          console.log(`[WMS] 时间尺度: ${currentScale}`);
          console.log(`[WMS] 年份/索引输入: ${yearOrIndex}`);
          console.log(`[WMS] 时间序列索引: ${timeIndex} (共 ${timeSequence.length} 帧)`);
          console.log(`[WMS] 当前时间: ${currentTime}`);
          console.log(`[WMS] 图层: ${overrides.layers}`);
          console.log(`[WMS] 样式: ${overrides.styles}`);
          console.log(`[WMS] 完整URL: ${fullUrl}`);
          
          // 调用实际的加载方法
          this.loadWMSLayer(wmsConfig, yearOrIndex, adjustView, currentScale);
          
        } else if (wmsConfig.wmsUrl) {
          // 旧格式
          const updatedUrl = this.updateWMSUrlWithYearAndScale(wmsConfig.wmsUrl, yearOrIndex, wmsConfig.scales, currentScale);
          
          console.log(`[WMS] 时间尺度: ${currentScale}`);
          console.log(`[WMS] 年份/索引: ${yearOrIndex}`);
          console.log(`[WMS] 完整URL (旧格式): ${updatedUrl}`);
          
          // 调用实际的加载方法
          this.loadWMSLayer(wmsConfig, yearOrIndex, adjustView, currentScale);
        }
      } catch (error) {
        console.error('[WMS] 生成URL失败:', error);
      }
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
    
    // 根据索引获取图例颜色
    getLevelColor(index) {
      // 10级颜色，从高到低（index 0-9）
      const colors = [
        'rgb(103, 0, 31)',      // 深红（最高值）
        'rgb(178, 24, 43)',     // 红
        'rgb(214, 96, 77)',     // 橙红
        'rgb(244, 165, 130)',   // 橙
        'rgb(253, 219, 199)',   // 浅橙
        'rgb(209, 229, 240)',   // 浅蓝
        'rgb(146, 197, 222)',   // 蓝
        'rgb(67, 147, 195)',    // 深蓝
        'rgb(33, 102, 172)',    // 更深蓝
        'rgb(5, 48, 97)'        // 最深蓝（最低值）
      ];
      
      return colors[index] || colors[0];
    },
    
    // 处理时间尺度变化
    handleTimeScaleChange(serviceType, newScale) {
      // 只有当前服务面板激活时才处理
      if (this.activePanelIndex === null || !this.menuItems[this.activePanelIndex]) {
        return;
      }
      
      const currentMenuItem = this.menuItems[this.activePanelIndex];
      if (String(currentMenuItem.type) !== String(serviceType)) {
        return;
      }
      
      // 转换为标准格式（month -> monthly, year -> yearly）
      const scaleMap = {
        'month': 'monthly',
        'year': 'yearly'
      };
      const standardScale = scaleMap[newScale] || newScale;
      
      console.log(`时间尺度变化: 服务类型=${serviceType}, 新尺度=${standardScale}`);
      
      // 如果有WMS配置，重新调整时间轴和加载图层
      if (this.activeWMSConfig) {
        // 调整时间轴范围
        this.adjustTimelineByWMSConfig(this.activeWMSConfig, standardScale);
        
        // 重新加载WMS图层（不调整地图范围）
        this.$nextTick(() => {
          this.loadWMSLayer(this.activeWMSConfig, this.currentYear, false, standardScale);
        });
      }
    },
    
    // 获取当前服务类型的时间尺度
    getCurrentTimeScale(serviceType) {
      const scaleMap = {
        'month': 'monthly',
        'year': 'yearly'
      };
      
      // 根据服务类型获取对应的 timeScale 变量
      const timeScaleVar = this[`timeScale${serviceType}`];
      return scaleMap[timeScaleVar] || 'yearly';
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
    
    // 监听时间尺度变化 - 水源涵养
    timeScale0(newVal) {
      this.handleTimeScaleChange('0', newVal);
    },
    
    // 监听时间尺度变化 - 水源供给
    timeScale1(newVal) {
      this.handleTimeScaleChange('1', newVal);
    },
    
    // 监听时间尺度变化 - 土壤保持
    timeScale2(newVal) {
      this.handleTimeScaleChange('2', newVal);
    },
    
    // 监听时间尺度变化 - 水质净化
    timeScale3(newVal) {
      this.handleTimeScaleChange('3', newVal);
    },
    
    // 监听时间尺度变化 - 防风固沙
    timeScale4(newVal) {
      this.handleTimeScaleChange('4', newVal);
    },
    
    // 监听时间尺度变化 - 洪水调蓄
    timeScale5(newVal) {
      this.handleTimeScaleChange('5', newVal);
    },
    
    // 监听时间尺度变化 - 固碳服务
    timeScale6(newVal) {
      this.handleTimeScaleChange('6', newVal);
    },
    
    // 监听时间尺度变化 - 粮食供给
    timeScale7(newVal) {
      this.handleTimeScaleChange('7', newVal);
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
    
    // 获取当前时间尺度的 levels 数组
    currentLevels() {
      if (this.currentTimeConfig && this.currentTimeConfig.levels) {
        // 返回从高到低的 levels（反转数组，因为图例从上到下是从高到低）
        const levels = [...this.currentTimeConfig.levels].reverse();
        
        // 确保返回10个元素，如果不够则用空字符串填充
        if (levels.length < 10) {
          return [...levels, ...Array(10 - levels.length).fill('')];
        }
        
        // 如果超过10个，只取前10个
        return levels.slice(0, 10);
      }
      // 默认返回空数组
      return [];
    },
    
    // 格式化起始时间显示
    formattedStartTime() {
      if (this.currentTimeUnit === 'monthly' && this.currentTimeConfig) {
        const date = this.currentTimeConfig.startDate;
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        return `${year}-${month}`;
      }
      return this.minYear;
    },
    
    // 格式化当前时间显示
    formattedCurrentTime() {
      if (this.currentTimeUnit === 'monthly' && this.currentTimeConfig) {
        const monthIndex = this.currentYear;
        const date = new Date(this.currentTimeConfig.startDate);
        date.setMonth(date.getMonth() + monthIndex);
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        return `${year}-${month}`;
      }
      return this.currentYear;
    },
    
    // 格式化结束时间显示
    formattedEndTime() {
      if (this.currentTimeUnit === 'monthly' && this.currentTimeConfig) {
        const monthIndex = this.maxYear;
        const date = new Date(this.currentTimeConfig.startDate);
        date.setMonth(date.getMonth() + monthIndex);
        const year = date.getFullYear();
        const month = (date.getMonth() + 1).toString().padStart(2, '0');
        return `${year}-${month}`;
      }
      return this.maxYear;
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
  background: transparent;  // 确保容器透明
  
  #map {
    width: 100%;
    height: 100%;
    background: transparent;  // 确保地图背景透明
    
    // 确保OpenLayers的canvas也是透明的
    canvas {
      background: transparent !important;
    }
    
    // 确保图层容器透明
    .ol-layer {
      background: transparent !important;
    }
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
      
      .wms-control {
        margin: 15px 0;
        padding: 10px;
        background-color: #e8f4fd;
        border-radius: 4px;
        border: 1px solid #b3d8ff;
        
        .opacity-control {
          margin-top: 10px;
          
          span {
            display: block;
            margin-bottom: 6px;
            color: #409EFF;
            font-weight: bold;
          }
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
  top: calc(12px + 6 * 60px + 60px);
  width: auto; // 自动宽度，根据内容调整
  min-width: 75px; // 最小宽度
  background: #34495e;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  border-radius: 4px;
  padding: 6px 8px; // 减小内边距
  z-index: 999;
  color: #fff;

  .legend-header {
    font-size: 11px;
    font-weight: bold;
    margin-bottom: 6px;
    text-align: center;
    color: #fff;
    border-bottom: 1px solid rgba(255, 255, 255, 0.2);
    padding-bottom: 4px;
    line-height: 1.2;
  }

  .legend-content {
    display: flex;
    flex-direction: column;

    .legend-items-container {
      display: flex;
      flex-direction: column;
      gap: 1px; // 减小项目之间的间距
      
      .legend-item {
        display: flex;
        align-items: center;
        gap: 5px; // 颜色和数值之间保持适中距离
        
        .legend-color {
          width: 18px; // 稍微减小颜色块宽度
          height: 16px; // 稍微减小颜色块高度
          border-radius: 2px;
          border: 1px solid rgba(255, 255, 255, 0.2);
          flex-shrink: 0;
        }
        
        .legend-value {
          font-size: 10px;
          color: rgba(255, 255, 255, 0.95);
          font-weight: 500;
          text-align: left;
          white-space: nowrap;
          line-height: 1.2;
        }
      }
    }
  }
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