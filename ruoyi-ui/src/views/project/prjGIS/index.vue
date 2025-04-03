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
          <!-- 动态生成面板内容 -->
          <div class="panel-specific-content">
            <!-- 水源涵养面板 (index === 0 或 type === '0') -->
            <template v-if="item.type === '0'">
              <el-collapse v-model="activeEcoService">
                <el-collapse-item title="潜在供给" name="potentialSupply0">
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
                  <div class="chart-container">
                    <div ref="deficit0Chart" style="width: 100%; height: 300px;"></div>
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
                  <div class="chart-container">
                    <div ref="deficit1Chart" style="width: 100%; height: 300px;"></div>
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
                  <div class="chart-container">
                    <div ref="deficit2Chart" style="width: 100%; height: 300px;"></div>
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
                  <div class="chart-container">
                    <div ref="deficit3Chart" style="width: 100%; height: 300px;"></div>
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
                  <div class="chart-container">
                    <div ref="deficit4Chart" style="width: 100%; height: 300px;"></div>
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
                  <div class="chart-container">
                    <div ref="deficit5Chart" style="width: 100%; height: 300px;"></div>
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
                  <div class="chart-container">
                    <div ref="deficit6Chart" style="width: 100%; height: 300px;"></div>
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
                  <div class="chart-container">
                    <div ref="deficit7Chart" style="width: 100%; height: 300px;"></div>
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
      
      // 新增的服务评估折叠面板控制
      activeEcoService: ['potentialSupply0', 'potentialSupply1', 'potentialSupply2', 'potentialSupply3', 
                        'potentialSupply4', 'potentialSupply5', 'potentialSupply6', 'potentialSupply7'],
      
      // 潜在供给相关数据 - 水源涵养
      indicators0: 'baseflow',
      timeScale0: 'year',
      spatialPattern0: 'mode',
      
      // 实际利用相关数据 - 水源涵养
      benefit0: {
        downstream: '',
        irrigation: ''
      },
      
      // 供需匹配相关数据 - 水源涵养
      supplyOptimization0: 'vegetation',
      demandControl0: 'quota',
      
      // 潜在供给相关数据 - 水源供给
      indicators1: 'runoff',
      timeScale1: 'year',
      spatialPattern1: 'max',
      
      // 实际利用相关数据 - 水源供给
      benefit1: {
        urban: '',
        industry: ''
      },
      
      // 供需匹配相关数据 - 水源供给
      supplyOptimization1: 'protection',
      demandControl1: 'efficiency',
      
      // 潜在供给相关数据 - 土壤保持
      indicators2: 'erosion',
      timeScale2: 'year',
      spatialPattern2: 'min',
      
      // 实际利用相关数据 - 土壤保持
      benefit2: {
        farmers: '',
        reservoir: ''
      },
      
      // 供需匹配相关数据 - 土壤保持
      supplyOptimization2: 'vegetation',
      demandControl2: 'farming',
      
      // 潜在供给相关数据 - 水质净化
      indicators3: 'nitrogenPurification',
      timeScale3: 'year',
      spatialPattern3: 'max',
      
      // 实际利用相关数据 - 水质净化
      benefit3: {
        aquaculture: '',
        drinking: ''
      },
      
      // 供需匹配相关数据 - 水质净化
      supplyOptimization3: 'wetland',
      demandControl3: 'emission',
      
      // 潜在供给相关数据 - 防风固沙
      indicators4: 'sandFlux',
      timeScale4: 'year',
      spatialPattern4: 'min',
      
      // 实际利用相关数据 - 防风固沙
      benefit4: {
        forestry: '',
        urban: ''
      },
      
      // 供需匹配相关数据 - 防风固沙
      supplyOptimization4: 'shelter',
      demandControl4: 'grazing',
      
      // 潜在供给相关数据 - 洪水调蓄
      indicators5: 'peakReduction',
      timeScale5: 'year',
      spatialPattern5: 'max',
      
      // 实际利用相关数据 - 洪水调蓄
      benefit5: {
        floodControl: '',
        riverside: ''
      },
      
      // 供需匹配相关数据 - 洪水调蓄
      supplyOptimization5: 'wetland',
      demandControl5: 'floodplain',
      
      // 潜在供给相关数据 - 固碳服务
      indicators6: 'vegetationCarbon',
      timeScale6: 'year',
      spatialPattern6: 'max',
      
      // 实际利用相关数据 - 固碳服务
      benefit6: {
        carbonTrading: '',
        climateChange: ''
      },
      
      // 供需匹配相关数据 - 固碳服务
      supplyOptimization6: 'afforestation',
      demandControl6: 'emissionCap',
      
      // 潜在供给相关数据 - 粮食供给
      indicators7: 'cropYield',
      timeScale7: 'year',
      spatialPattern7: 'max',
      
      // 实际利用相关数据 - 粮食供给
      benefit7: {
        farmers: '',
        processing: ''
      },
      
      // 供需匹配相关数据 - 粮食供给
      supplyOptimization7: 'highYield',
      demandControl7: 'conservation',
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
          
          // 检查字典项的属性结构
          if (this.serviceTypeOptions.length > 0) {
            const firstItem = this.serviceTypeOptions[0];
            
            // 检查可能的图标属性
            if (firstItem.listClass) {
              // 使用 listClass 属性
            } else if (firstItem.list_class) {
              // 使用 list_class 属性
              
              // 如果确实是list_class，修正所有项的属性
              this.serviceTypeOptions.forEach(item => {
                if (item.list_class) {
                  item.listClass = item.list_class;
                }
              });
            }
          }
          
          // 如果区域数据已加载，则生成菜单
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
    
    // 获取区域详细信息
    fetchRegionDetail(regionId) {
      this.loading = true;
      getProject_region(regionId).then(response => {
        if (response && response.code === 200 && response.data) {
          this.regionDetail = response.data;
          this.regionInfo.regionName = response.data.regionName;
          
          // 如果服务类型字典已加载，则生成菜单
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
      });
    },
    
    // 获取区域服务类型
    fetchRegionServiceTypes(regionId) {
      listProject_region_service({ regionId: regionId }).then(response => {
        if (response && response.code === 200 && response.rows) {
          // 获取服务类型数组
          this.serviceTypes = response.rows.map(item => item.serviceType);
          
          // 如果字典已加载，则生成菜单
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
      
      // 从字典数据中查找对应的服务类型项
      const dictItem = this.serviceTypeOptions.find(item => String(item.dictValue) === typeIdStr);
      
      // 获取图标配置
      let icon = 'el-icon-menu'; // 默认图标
      
      if (dictItem) {
        // 检查所有可能的图标属性名称
        if (dictItem.listClass) {
          icon = dictItem.listClass;
        } 
        else if (dictItem.list_class) {
          icon = dictItem.list_class;
        }
        // 可能的其他属性名称，如cssClass等
        else if (dictItem.cssClass) {
          icon = dictItem.cssClass;
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
          color: #336699;
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
  font-size: 16px !important; /* 增大箭头尺寸 */
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