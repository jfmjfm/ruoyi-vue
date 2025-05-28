<template>
  <div class="dashboard-container">   
    <!-- Header section with region information -->
    <div class="header-section">
      <div class="title-box">
        <h2>多目标服务权衡分析</h2>
        <div class="region-info" v-if="regionInfo.regionName">
          <span>{{ regionInfo.regionName }}</span>
        </div>
      </div>
      <div class="actions-box">
        <el-button type="primary" icon="el-icon-download" @click="exportResults" :disabled="!optimizationCompleted">导出结果</el-button>
        <el-button type="success" icon="el-icon-s-claim" @click="saveOptimization" :disabled="!optimizationCompleted">保存方案</el-button>
            </div>
      </div>

    <!-- Main content area - Single column with steps -->
    <div class="main-content">
      <!-- Horizontal steps navigation -->
      <el-card shadow="hover" class="main-steps-card">
        <el-steps :active="currentStep" finish-status="success" align-center>
          <el-step title="优化设置" description="选择算法"></el-step>
          <el-step title="服务目标" description="选择优化目标"></el-step>
          <el-step title="约束条件" description="设置目标范围"></el-step>
          <el-step title="参数配置" description="设置优化参数"></el-step>
          <el-step title="开始优化" description="确认并运行"></el-step>
          <el-step title="结果可视化" description="查看Pareto前沿"></el-step>
          <el-step title="方案比较" description="比较解决方案"></el-step>
          <el-step title="方案详情" description="查看参数详情"></el-step>
        </el-steps>
        
        <!-- Step 1: 优化设置 - 仅算法选择 -->
        <div class="step-content" v-show="currentStep === 0">
          <h3 class="step-title">优化设置</h3>
          <p class="guide-text">请选择多目标优化算法</p>
          <el-row :gutter="20">
            <el-col :span="12">
          <el-form :model="optimizationForm" label-width="100px" size="small">
            <el-form-item label="优化算法">
                  <el-select v-model="optimizationForm.algorithm" placeholder="选择算法" style="width: 100%">
                <el-option label="多目标贝叶斯优化" value="mobo"></el-option>
                <el-option label="NSGA-II算法" value="nsga2"></el-option>
                <el-option label="权重和方法" value="weighted"></el-option>
              </el-select>
            </el-form-item>
              </el-form>
            </el-col>
            <el-col :span="12">
              <div class="algorithm-info">
                <h4>算法说明</h4>
                <p v-if="optimizationForm.algorithm === 'mobo'">多目标贝叶斯优化适用于计算资源有限的情况，能够高效地探索多目标优化空间。</p>
                <p v-else-if="optimizationForm.algorithm === 'nsga2'">NSGA-II是一种流行的进化算法，通过非支配排序和拥挤度距离保持种群多样性。</p>
                <p v-else-if="optimizationForm.algorithm === 'weighted'">权重和方法将多个目标函数转化为单一目标，适合偏好明确的场景。</p>
                <p v-else>请选择一种优化算法以查看相关说明。</p>
              </div>
            </el-col>
          </el-row>
      </div>
              
        <!-- Step 2: 服务目标选择 - 提取为单独步骤 -->
        <div class="step-content" v-show="currentStep === 1">
          <h3 class="step-title">服务目标选择</h3>
          <p class="guide-text">请选择至少两个优化目标并设置优化方向（最大化或最小化）</p>
          <div class="service-selection">
            <el-row :gutter="20">
              <el-col :span="24">
              <el-checkbox-group v-model="optimizationForm.selectedServices">
                  <el-row :gutter="16">
                    <el-col :span="8" v-for="service in availableServices" :key="service.id">
                      <div class="service-item">
                  <el-checkbox :label="service.id">
                    <span>{{ service.name }}</span>
                  </el-checkbox>
                  <el-select v-model="service.direction" size="mini" placeholder="方向" 
                            :disabled="!optimizationForm.selectedServices.includes(service.id)">
                    <el-option label="最大化" value="max"></el-option>
                    <el-option label="最小化" value="min"></el-option>
                  </el-select>
      </div>
                    </el-col>
                  </el-row>
              </el-checkbox-group>
              </el-col>
            </el-row>
          </div>
              </div>
              
        <!-- Step 3: 约束条件 -->
        <div class="step-content" v-show="currentStep === 2">
          <h3 class="step-title">约束条件设置</h3>
          <p class="guide-text">为已选择的服务目标设置取值范围约束</p>
          
          <el-row :gutter="20">
            <el-col :span="12" v-for="service in availableServices" :key="'constraint-'+service.id" 
                v-show="optimizationForm.selectedServices.includes(service.id)">
              <div class="constraint-item">
                <div class="constraint-header" :style="{borderLeftColor: getServiceColor(service.id)}">
                <span class="constraint-name">{{ service.name }}</span>
                  <span class="constraint-direction">{{ service.direction === 'max' ? '(最大化)' : '(最小化)' }}</span>
                </div>
                <div class="constraint-range">
                  <span class="constraint-value">{{ service.minValue }}</span>
                  <el-slider
                    v-model="service.rangeValue"
                    range
                    :min="0"
                    :max="100"
                    @input="updateServiceRange(service)"
                    :format-tooltip="formatRangeTooltip"
                    class="constraint-slider"
                  ></el-slider>
                  <span class="constraint-value">{{ service.maxValue }}</span>
                </div>
              </div>
            </el-col>
          </el-row>
          
          <div class="no-services-selected" v-if="optimizationForm.selectedServices.length === 0">
            <el-empty description="请先在上一步选择服务目标"></el-empty>
              </div>
            </div>
            
        <!-- Step 4: 参数配置 -->
        <div class="step-content" v-show="currentStep === 3">
          <h3 class="step-title">参数配置</h3>
          <p class="guide-text">设置优化算法的参数</p>
            
          <el-row :gutter="20">
            <el-col :span="12">
              <div class="parameter-group">
                <h4>基本参数</h4>
              <div class="parameter-item">
                <span>优化迭代次数</span>
                  <el-tooltip content="迭代次数越多，优化效果越好，但计算时间也越长" placement="top">
                <el-input-number v-model="optimizationForm.iterations" :min="10" :max="1000" :step="10" size="small"></el-input-number>
                  </el-tooltip>
          </div>
              <div class="parameter-item">
                <span>Pareto前沿点数</span>
                  <el-tooltip content="Pareto前沿上的最优解数量" placement="top">
                <el-input-number v-model="optimizationForm.paretoPoints" :min="5" :max="100" :step="5" size="small"></el-input-number>
                  </el-tooltip>
        </div>
              <div class="parameter-item">
                <span>随机种子</span>
                  <el-tooltip content="设置随机数种子以保证结果可重复" placement="top">
                <el-input-number v-model="optimizationForm.seed" :min="1" :max="9999" size="small"></el-input-number>
                  </el-tooltip>
      </div>
              </div>
            </el-col>
            <el-col :span="12">
              <div class="algorithm-parameters" v-if="optimizationForm.algorithm === 'mobo'">
                <h4>贝叶斯优化参数</h4>
                <div class="parameter-item">
                  <span>探索系数</span>
                  <el-slider v-model="moboParams.exploration" :min="0.1" :max="1" :step="0.1" show-stops></el-slider>
                </div>
                <div class="parameter-item">
                  <span>高斯过程核函数</span>
                  <el-select v-model="moboParams.kernel" placeholder="选择核函数" size="small">
                    <el-option label="径向基函数 (RBF)" value="rbf"></el-option>
                    <el-option label="Matérn核函数" value="matern"></el-option>
                  </el-select>
                </div>
              </div>
              <div class="algorithm-parameters" v-else-if="optimizationForm.algorithm === 'nsga2'">
                <h4>NSGA-II参数</h4>
                <div class="parameter-item">
                  <span>种群大小</span>
                  <el-input-number v-model="nsgaParams.populationSize" :min="10" :max="500" size="small"></el-input-number>
                </div>
                <div class="parameter-item">
                  <span>交叉概率</span>
                  <el-slider v-model="nsgaParams.crossoverRate" :min="0" :max="1" :step="0.1" show-stops></el-slider>
                </div>
                <div class="parameter-item">
                  <span>变异概率</span>
                  <el-slider v-model="nsgaParams.mutationRate" :min="0" :max="0.5" :step="0.05" show-stops></el-slider>
                </div>
              </div>
              <div class="algorithm-parameters" v-else-if="optimizationForm.algorithm === 'weighted'">
                <h4>权重和方法参数</h4>
                <div v-for="service in selectedServicesList" :key="'weight-'+service.id" class="parameter-item">
                  <span>{{ service.name }}权重</span>
                  <el-slider v-model="weightParams[service.id]" :min="0" :max="10" :step="1" show-stops></el-slider>
                </div>
              </div>
            </el-col>
          </el-row>
        </div>
        
        <!-- Step 5: 确认并开始优化 -->
        <div class="step-content" v-show="currentStep === 4">
          <h3 class="step-title">确认优化设置</h3>
          <p class="guide-text">请确认所有设置无误后开始优化</p>
          
          <div class="confirmation-panel">
            <el-row :gutter="20">
              <el-col :span="12">
                <div class="confirmation-section">
                  <h4>基本设置</h4>
                  <ul class="confirmation-list">
                    <li><strong>优化算法：</strong>{{ getAlgorithmLabel(optimizationForm.algorithm) }}</li>
                    <li>
                      <strong>服务目标：</strong>
                      <el-tag 
                        v-for="service in selectedServicesList" 
                        :key="service.id" 
                        :type="service.direction === 'max' ? 'success' : 'danger'"
                        size="small"
                        style="margin-right: 5px;">
                        {{ service.name }} ({{ service.direction === 'max' ? '最大化' : '最小化' }})
                      </el-tag>
                    </li>
                    <li>
                      <strong>约束条件：</strong>
                      <div v-for="service in selectedServicesList" :key="'conf-'+service.id" class="constraint-summary">
                        {{ service.name }}: {{ service.minValue }}% ~ {{ service.maxValue }}%
              </div>
                    </li>
                  </ul>
                </div>
              </el-col>
              <el-col :span="12">
                <div class="confirmation-section">
                  <h4>算法参数</h4>
                  <ul class="confirmation-list">
                    <li><strong>迭代次数：</strong>{{ optimizationForm.iterations }}</li>
                    <li><strong>Pareto点数：</strong>{{ optimizationForm.paretoPoints }}</li>
                    <li><strong>随机种子：</strong>{{ optimizationForm.seed }}</li>
                    <!-- 算法特定参数 -->
                    <template v-if="optimizationForm.algorithm === 'mobo'">
                      <li><strong>探索系数：</strong>{{ moboParams.exploration }}</li>
                      <li><strong>核函数：</strong>{{ moboParams.kernel === 'rbf' ? '径向基函数' : 'Matérn核函数' }}</li>
                    </template>
                    <template v-else-if="optimizationForm.algorithm === 'nsga2'">
                      <li><strong>种群大小：</strong>{{ nsgaParams.populationSize }}</li>
                      <li><strong>交叉概率：</strong>{{ nsgaParams.crossoverRate }}</li>
                      <li><strong>变异概率：</strong>{{ nsgaParams.mutationRate }}</li>
                    </template>
                  </ul>
              </div>
              </el-col>
            </el-row>
        
            <!-- 优化进度 -->
            <div class="optimization-progress" v-if="optimizationRunning || optimizationCompleted">
              <h4>优化进度</h4>
            <el-progress :percentage="optimizationProgress" :status="optimizationCompleted ? 'success' : ''"></el-progress>
            <div class="progress-details">
              <div class="progress-item">
                <span>当前迭代</span>
                <span>{{ currentIteration }} / {{ optimizationForm.iterations }}</span>
            </div>
              <div class="progress-item">
                <span>已找到Pareto解</span>
                <span>{{ paretoSolutions.length }} 个</span>
          </div>
              <div class="progress-item">
                <span>超差率</span>
                <span>{{ hypervolumeDifference.toFixed(4) }}</span>
        </div>
      </div>
          </div>
            
            <!-- 操作按钮 -->
            <div class="action-buttons">
              <el-button type="primary" @click="startOptimization" :loading="optimizationRunning" :disabled="!canStartOptimization">
                {{ optimizationRunning ? '优化中...' : '开始优化' }}
              </el-button>
              <el-button @click="resetOptimization" :disabled="optimizationRunning">重置</el-button>
              <el-button @click="refreshServiceList" type="text">刷新服务列表</el-button>
            </div>
          </div>
    </div>

        <!-- Step 6: 结果可视化 -->
        <div class="step-content visualization-step-content" v-show="currentStep === 5">
          <h3 class="step-title">Pareto前沿可视化</h3>
          <p class="guide-text">查看多目标优化结果的Pareto最优解集</p>
          
          <div class="visualization-panel">
            <div class="view-controls">
              <el-radio-group v-model="visualizationType" size="medium" @change="handleVisualizationTypeChange">
                <el-radio-button label="parallel">平行坐标</el-radio-button>
                <el-radio-button label="scatter">散点图</el-radio-button>
                <el-radio-button label="radar">雷达图</el-radio-button>
              </el-radio-group>
        </div>
          
                          <div class="chart-wrapper">
                <div class="chart-container" v-loading="chartLoading">
                  <div v-if="visualizationType" id="paretoChart" class="chart"></div>
                  <div v-else class="no-chart-selected">
                    <i class="el-icon-data-analysis"></i>
                    <p>请点击上方按钮选择图表类型</p>
                  </div>
                </div>
              </div>

                          <div class="chart-description">
                <h4>图表说明</h4>
                <p v-if="!visualizationType">
                  点击上方按钮选择图表类型后将绘制对应图表。各图表类型适合不同分析场景：<br>
                  - <strong>平行坐标</strong>：显示所有目标的完整权衡关系<br>
                  - <strong>散点图</strong>：清晰展示两个目标间的权衡<br>
                  - <strong>雷达图</strong>：方案整体表现的综合比较
                </p>
                <p v-else-if="visualizationType === 'parallel'">
                  平行坐标图展示了所有Pareto最优解在各个目标上的表现。每一条线代表一个解决方案，
                  线穿过各坐标轴的位置表示该方案在对应目标上的表现。
                </p>
                <p v-else-if="visualizationType === 'scatter'">
                  散点图展示了解决方案在两个主要目标上的表现。每个点代表一个Pareto最优解，
                  可以直观看出两个目标之间的权衡关系。
                </p>
                <p v-else-if="visualizationType === 'radar'">
                  雷达图以多边形方式展示了各个解决方案在所有目标上的综合表现。多边形面积越大，
                  综合表现越好，但具体目标的权衡需要仔细观察。
                </p>
                <p v-if="paretoSolutions.length === 0" class="no-data-tip">
                  尚未生成Pareto最优解，请完成优化后再查看
                </p>
              </div>
        </div>
      </div>
      
        <!-- Step 7: 方案比较 -->
        <div class="step-content" v-show="currentStep === 6">
          <h3 class="step-title">解决方案比较</h3>
          <div class="solutions-panel">
            <el-alert
              type="info"
              title="使用说明"
              description="1. 点击表格行选择方案进行详细查看  2. 查看右侧方案对比图  3. 选择方案后点击'下一步'查看详情"
              show-icon
              :closable="false"
              style="margin-bottom: 15px">
            </el-alert>
            <el-row :gutter="20">
              <el-col :span="16">
          <el-table
            v-if="paretoSolutions.length > 0"
            :data="paretoSolutions"
            style="width: 100%"
            border
                  size="small"
                  highlight-current-row
                  @row-click="(row, column, event) => selectSolution(row, paretoSolutions.indexOf(row), event)"
                  :row-class-name="tableRowClassName">
            <el-table-column type="index" width="50" label="序号"></el-table-column>
            <el-table-column v-for="service in selectedServicesList" :key="service.id" 
                            :prop="'services.' + service.id" :label="service.name"
                            width="110">
              <template slot-scope="scope">
                <div class="service-value">
                  <span>{{ scope.row.services[service.id].toFixed(2) }}</span>
                  <i :class="service.direction === 'max' ? 'el-icon-top' : 'el-icon-bottom'" 
                     :style="{color: service.direction === 'max' ? '#67C23A' : '#F56C6C'}"></i>
                  </div>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120" align="center">
              <template slot-scope="scope">
                <el-button type="text" size="mini" @click.stop="viewSolutionMap(scope.row)">空间分布</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="no-solutions" v-else>
            <i class="el-icon-data-line"></i>
            <p>尚未生成Pareto最优解</p>
                  <p class="help-text">请完成优化后再查看</p>
                  </div>
              </el-col>
              <el-col :span="8">
                <div class="solution-comparison" v-if="paretoSolutions.length > 0 && selectedSolution !== null">
                  <h4>方案对比图 <small v-if="selectedServicesList.length > 3" class="chart-type-label">（平行坐标图）</small><small v-else class="chart-type-label">（雷达图）</small></h4>
                  <div class="comparison-chart-container" :style="selectedServicesList.length > 3 ? 'height: 350px' : 'height: 300px'">
                    <div id="comparisonChart" class="chart" style="min-height: 280px;"></div>
                  </div>
                  <div class="chart-type-tip" v-if="selectedServicesList.length > 3">
                    <i class="el-icon-info"></i> 当维度超过3个时，系统自动切换为平行坐标图以更好地展示多维数据关系
                  </div>
                  <div class="selected-solution-details">
                    <h4>方案 {{ selectedSolution + 1 }} 详情</h4>
                    <div v-for="(value, service) in solutionDetails" :key="service" class="solution-detail-item">
                      <span class="service-name">{{ getServiceName(service) }}</span>
                      <div class="value-bar-container">
                        <div class="value-bar" :style="{width: `${value}%`, backgroundColor: getServiceColor(service)}"></div>
                        <span class="value-text">{{ value.toFixed(2) }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="no-solution-selected" v-else-if="paretoSolutions.length > 0">
                  <el-empty description="请在表格中选择一个解决方案"></el-empty>
                </div>
                <div class="no-solution-selected" v-else>
                  <el-empty description="尚未生成优化方案"></el-empty>
                </div>
              </el-col>
            </el-row>
          </div>
                  </div>
                  
        <!-- Step 8: 方案详情 -->
        <div class="step-content" v-show="currentStep === 7">
          <h3 class="step-title">方案详情与空间分布</h3>
          <p class="guide-text">查看选定方案的详细参数和空间分布</p>
          
          <div class="details-panel" v-if="selectedSolution !== null && paretoSolutions.length > 0">
            <el-row :gutter="20">
              <el-col :span="8">
                <div class="parameter-details">
                  <h4>方案 {{ selectedSolution + 1 }} 参数详情</h4>
          <div class="parameters-table">
            <table>
              <thead>
                <tr>
                  <th>参数名称</th>
                  <th>参数值</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(value, param) in currentParameters" :key="param">
                  <td>{{ getParameterName(param) }}</td>
                  <td>{{ value }}</td>
                </tr>
              </tbody>
            </table>
                  </div>
                </div>
              </el-col>
              <el-col :span="16">
                <div class="spatial-distribution">
                  <h4>方案空间分布</h4>
                  <div class="map-container">
                    <div id="solutionMap" class="solution-map"></div>
                  </div>
                  <div class="map-legend">
                    <div v-for="service in selectedServicesList" :key="service.id" class="legend-item">
                      <span class="legend-color" :style="{backgroundColor: getServiceColor(service.id)}"></span>
                      <span class="legend-name">{{ service.name }}</span>
                    </div>
                  </div>
                </div>
              </el-col>
            </el-row>
            <div class="tradeoff-section">
              <h4>权衡分析</h4>
              <p class="guide-text">分析当前方案与其他方案的服务目标权衡关系</p>
              <div class="tradeoff-controls">
                <el-button 
                  type="success" 
                  icon="el-icon-s-promotion" 
                  :disabled="!canStartTradeoff || tradeoffStarted" 
                  @click="startTradeoff"
                >
                  开始权衡分析
                </el-button>
              </div>
              <div v-if="tradeoffStarted" class="tradeoff-progress">
                <div class="tradeoff-status">
                  <span>{{ tradeoffStatus }}</span>
                  <el-tag v-if="tradeoffProgress >= 100" type="success" size="small">已完成</el-tag>
                  <el-tag v-else type="warning" size="small">进行中</el-tag>
                </div>
                <el-progress :percentage="tradeoffProgress" :status="tradeoffProgress >= 100 ? 'success' : ''"></el-progress>
              </div>
            </div>
          </div>
          <div class="no-solution-selected" v-else>
            <el-empty description="请先在上一步选择一个解决方案"></el-empty>
              </div>
    </div>
    
        <!-- Step navigation buttons -->
        <div class="step-navigation">
          <el-button @click.stop="prevStep" :disabled="currentStep === 0 || optimizationRunning">上一步</el-button>
          <el-button type="primary" @click.stop="nextStep" :disabled="!canGoNext || optimizationRunning">
            {{ isLastStep ? '完成' : '下一步' }}
          </el-button>
        </div>
      </el-card>
    </div>
    
    <!-- Map dialog for spatial distribution (kept for backward compatibility) -->
    <el-dialog
      title="方案空间分布"
      :visible.sync="mapDialogVisible"
      width="80%"
      :before-close="closeMapDialog">
      <div class="map-container">
        <div id="dialogSolutionMap" class="solution-map"></div>
          </div>
      <div class="map-legend">
        <div v-for="service in selectedServicesList" :key="service.id" class="legend-item">
          <span class="legend-color" :style="{backgroundColor: getServiceColor(service.id)}"></span>
          <span class="legend-name">{{ service.name }}</span>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="closeMapDialog">关闭</el-button>
        <el-button type="primary" @click="exportMap">导出图像</el-button>
      </span>
    </el-dialog>
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
import { listProject_service_case_by_region_service_id, listProject_service_case } from "@/api/project/project_service_case";
import { parseTime } from "@/utils/ruoyi";
import * as echarts from 'echarts';
import { getDicts } from "@/api/system/dict/data";

export default {
  name: "MultiObjectiveOptimization",
  data() {
    return {
      // Loading state
      loading: false,
      
      // Region information
      regionInfo: {
        regionId: null,
        regionName: null
      },
      
      // Service dictionary data
      serviceTypeOptions: [],
      serviceTypeMap: {},
      
      // Service data
      availableServices: [],
      regionServiceIds: [],
      
      // Optimization form data
      optimizationForm: {
        algorithm: 'mobo',
        selectedServices: [],
        iterations: 100,
        paretoPoints: 20,
        seed: 42
      },
      
      // 新增算法特定参数
      moboParams: {
        exploration: 0.5,
        kernel: 'rbf'
      },
      nsgaParams: {
        populationSize: 100,
        crossoverRate: 0.8,
        mutationRate: 0.1
      },
      weightParams: {},
      
      // 分步表单控制
      currentStep: 0,
      
      // Optimization status
      optimizationRunning: false,
      optimizationCompleted: false,
      optimizationProgress: 0,
      currentIteration: 0,
      hypervolumeDifference: 0,
      
      // Results data
      paretoSolutions: [],
      selectedSolution: null,
      
      // Visualization - default is empty (no selection)
      visualizationType: '',
      chartLoading: false,
      paretoChart: null,
      comparisonChart: null,
      
      // Parameters for selected solution
      currentParameters: {},
      
      // Map dialog
      mapDialogVisible: false,
      solutionMap: null,
      
      // Tradeoff processing
      tradeoffStarted: false,
      tradeoffProgress: 0,
      tradeoffStatus: '准备就绪',
      tradeoffInterval: null,
      
      // Service colors (backup in case services don't have assigned colors)
      serviceColors: {
        'waterRetention': '#3498db',
        'waterSupply': '#1abc9c',
        'soilConservation': '#2ecc71',
        'waterPurification': '#9b59b6',
        'windbreakSand': '#f1c40f',
        'floodRegulation': '#e74c3c',
        'carbonSequestration': '#27ae60',
        'foodProduction': '#f39c12'
      },
      
      // resize防抖计时器
      resizeTimeout: null,
      
      // 添加图表初始化标记
      chartInitialized: false,
      initialRenderComplete: false,
    };
  },
  computed: {
    // Filter services based on selection
    selectedServicesList() {
      return this.availableServices.filter(service => 
        this.optimizationForm.selectedServices.includes(service.id)
      );
    },
    
    // Check if optimization can be started
    canStartOptimization() {
      return this.optimizationForm.selectedServices.length >= 2 && 
             !this.optimizationRunning;
    },
    
    // Solution details for the selected solution
    solutionDetails() {
      if (this.selectedSolution === null || this.paretoSolutions.length === 0) {
        return {};
      }
      return this.paretoSolutions[this.selectedSolution].services;
    },
    
    // Check if tradeoff can be started
    canStartTradeoff() {
      return this.optimizationCompleted && 
             this.paretoSolutions.length > 0 && 
             !this.tradeoffStarted;
    },
    
    // 确定是否可以进入下一步
    canGoNext() {
      if (this.currentStep === 0) {
        // 算法选择
        return !!this.optimizationForm.algorithm;
      } else if (this.currentStep === 1) {
        // 服务目标选择
        return this.optimizationForm.selectedServices.length >= 2;
      } else if (this.currentStep === 4) {
        // 开始优化后才能进入结果可视化
        return this.optimizationCompleted;
      } else if (this.currentStep === 5) {
        // 有Pareto解集才能进入方案比较
        return this.paretoSolutions.length > 0;
      } else if (this.currentStep === 6) {
        // 选择了具体方案才能进入详情页
        return this.selectedSolution !== null;
      }
      return true;
    },
    
    // 是否是最后一步
    isLastStep() {
      return this.currentStep === 7;
    }
  },
  created() {
    // Get region ID from route
    const regionId = this.$route.query.region_id;
    if (regionId) {
      this.regionInfo.regionId = regionId;
      this.fetchRegionDetail(regionId);
      this.loadServiceTypeDictionary();
    } else {
      this.$message.warning('未接收到region_id参数，无法加载区域数据');
    }
    
    // Initialize service ranges for sliders
    this.initServiceRanges();
    
    // 初始化权重参数
    this.initWeightParams();
  },
  mounted() {
    this.$nextTick(() => {
      // 不在mounted中初始化图表，而是等待进入可视化步骤时初始化
      this.initComparisonChart();
    });
    
    // Add resize event listener
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    // Remove resize event listener
    window.removeEventListener('resize', this.handleResize);
    
    // Dispose charts
    if (this.paretoChart) {
      this.paretoChart.dispose();
    }
    
    if (this.comparisonChart) {
      this.comparisonChart.dispose();
    }
    
    if (this.solutionMap) {
      this.solutionMap = null;
    }
    
    // Clear interval if running
    if (this.tradeoffInterval) {
      clearInterval(this.tradeoffInterval);
    }
  },
  methods: {
    // Load service type dictionary
    loadServiceTypeDictionary() {
      getDicts("sys_service_type").then(response => {
        if (response && response.data) {
          this.serviceTypeOptions = response.data;
          
          // Create a mapping of service type values to labels
          this.serviceTypeMap = {};
              this.serviceTypeOptions.forEach(item => {
            this.serviceTypeMap[item.dictValue] = item.dictLabel;
            
            // Store color information if available
            if (item.cssClass) {
              this.serviceColors[item.dictValue] = item.cssClass;
            }
          });
        }
      }).catch(error => {
        this.$message.error('加载服务类型字典失败');
      });
    },
    
    // Fetch region details
    fetchRegionDetail(regionId) {
      this.loading = true;
      getProject_region(regionId).then(response => {
        if (response && response.code === 200 && response.data) {
          this.regionInfo.regionName = response.data.regionName;
          // Get calibrated services for this region
          this.findCalibratedServices(regionId);
        } else {
          this.$message.error('获取区域详细信息失败');
        }
        this.loading = false;
      }).catch(error => {
        this.$message.error('获取区域详细信息失败');
        this.loading = false;
      });
    },
    
    // Find calibrated services for the region (adapted from multidecision.vue)
    findCalibratedServices(regionId) {
      // Clear current services
      this.availableServices = [];
      this.regionServiceIds = [];
      
      // Query all service cases with description="参数率定"
      listProject_service_case({
        description: "参数率定",
        pageSize: 999
      }).then(response => {
        if (!response || !response.rows || response.rows.length === 0) {
          this.$message.warning('未找到已率定的服务案例');
          return;
        }
        
        const calibratedCases = response.rows;
        
        // Get all region_service_ids from these cases
        const regionServiceIds = new Set(calibratedCases.map(item => item.regionServiceId));
        
        // Filter only region services that belong to the current region
        listProject_region_service({ regionId: regionId }).then(rsResponse => {
          if (!rsResponse || !rsResponse.rows) {
            this.$message.warning('获取区域服务失败');
            return;
          }
          
          const regionServices = rsResponse.rows;
          
          // Filter region services to only those that have calibrated cases
          const filteredRegionServices = regionServices.filter(rs => 
            regionServiceIds.has(rs.id)
          );
          
          if (filteredRegionServices.length === 0) {
            this.$message.warning('该区域没有已率定的服务');
            return;
          }
          
          // Generate available services list from filtered region services
          this.regionServiceIds = filteredRegionServices.map(rs => rs.id);
          
          // Create available services
          this.availableServices = filteredRegionServices.map(rs => {
            const serviceType = rs.serviceType;
            const serviceName = this.serviceTypeMap[serviceType] || `服务类型${serviceType}`;
            
            return {
              id: serviceType,
              name: serviceName,
              direction: 'max',
              minValue: 30,
              maxValue: 100,
              rangeValue: [30, 100], // Add range value for slider
              color: this.serviceColors[serviceType] || this.getRandomColor(),
              regionServiceId: rs.id
            };
          });
          
          // Pre-select first 3 services (or less if fewer are available)
          this.optimizationForm.selectedServices = this.availableServices
            .slice(0, Math.min(3, this.availableServices.length))
            .map(s => s.id);
          
          this.$message.success(`已加载${this.availableServices.length}个已率定服务`);
        }).catch(error => {
          this.$message.error('获取区域服务失败');
        });
      }).catch(error => {
        this.$message.error('获取率定服务案例失败');
      });
    },
    
    // Refresh service list
    refreshServiceList() {
      if (this.regionInfo.regionId) {
        this.findCalibratedServices(this.regionInfo.regionId);
      } else {
        this.$message.warning('请先选择区域');
      }
    },
    
    // Generate a random color for services without assigned colors
    getRandomColor() {
      const letters = '0123456789ABCDEF';
      let color = '#';
      for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)];
      }
      return color;
    },
    
    // Start optimization process
    startOptimization() {
      if (!this.canStartOptimization) {
        this.$message.warning('请至少选择两个服务目标');
        return;
      }
      
      // Reset previous results
      this.paretoSolutions = [];
      this.selectedSolution = null;
      this.optimizationCompleted = false;
      this.optimizationProgress = 0;
      this.currentIteration = 0;
      this.hypervolumeDifference = 0;
      this.tradeoffStarted = false;
      this.tradeoffProgress = 0;
      this.tradeoffStatus = '准备就绪';
      
      // Start optimization
      this.optimizationRunning = true;
      this.chartLoading = true;
      
      // Simulate optimization process
      this.simulateOptimization();
    },
    
    // Simulate optimization process (would be replaced with actual algorithm)
    simulateOptimization() {
      const totalIterations = this.optimizationForm.iterations;
      const intervalTime = 100; // ms between updates
      const selectedServices = this.selectedServicesList;
      
      // Clear previous solutions
      this.paretoSolutions = [];
      
      // Simulation interval
      const interval = setInterval(() => {
        this.currentIteration++;
        this.optimizationProgress = (this.currentIteration / totalIterations) * 100;
        
        // Generate random solution
        const solution = this.generateRandomSolution(selectedServices);
        this.paretoSolutions.push(solution);
        
        // Update hypervolume difference (simulated)
        this.hypervolumeDifference = 1 - (this.currentIteration / totalIterations) * 0.95;
        
        // Update chart if on visualization step
        if (this.currentStep === 4) {
        this.updateParetoChart();
        }
        
        // Check if optimization is complete
        if (this.currentIteration >= totalIterations) {
          clearInterval(interval);
          this.optimizationRunning = false;
          this.optimizationCompleted = true;
          this.chartLoading = false;
          
          // Filter solutions to get Pareto front
          this.paretoSolutions = this.filterParetoSolutions(this.paretoSolutions, selectedServices);
          
          // Update charts with final solutions
          if (this.currentStep === 4) {
          this.updateParetoChart();
          }
          
          // Auto-select first solution
          if (this.paretoSolutions.length > 0) {
            this.selectSolution(this.paretoSolutions[0], 0);
          }
          
          // 调用优化完成后处理
          this.afterOptimizationComplete();
        }
      }, intervalTime);
    },
    
    // Generate a random solution for simulation
    generateRandomSolution(selectedServices) {
      const services = {};
      const parameters = {};
      
      // Random service values based on min/max
      selectedServices.forEach(service => {
        const min = service.minValue;
        const max = service.maxValue;
        services[service.id] = parseFloat((Math.random() * (max - min) + min).toFixed(2));
      });
      
      // Random parameters
      const paramNames = ['landuse', 'forestCover', 'cropRotation', 'irrigationScheme', 'fertilization'];
      paramNames.forEach(param => {
        if (param === 'landuse') {
          parameters[param] = Math.random() > 0.5 ? '农田为主' : '林地为主';
        } else if (param === 'forestCover') {
          parameters[param] = (Math.random() * 60 + 30).toFixed(1) + '%';
        } else if (param === 'cropRotation') {
          parameters[param] = Math.random() > 0.5 ? '是' : '否';
        } else if (param === 'irrigationScheme') {
          const schemes = ['滴灌', '喷灌', '沟灌', '微灌'];
          parameters[param] = schemes[Math.floor(Math.random() * schemes.length)];
        } else if (param === 'fertilization') {
          parameters[param] = (Math.random() * 200 + 100).toFixed(0) + ' kg/ha';
        }
      });
      
      return {
        services,
        parameters
      };
    },
    
    // Filter solutions to get Pareto front
    filterParetoSolutions(solutions, selectedServices) {
      // This is a simplified implementation of non-dominated sorting
      const isDominated = (a, b) => {
        let aWorseThanB = false;
        let aEqualToB = true;
        
        for (const service of selectedServices) {
          const aValue = a.services[service.id];
          const bValue = b.services[service.id];
          
          // For maximization objectives
          if (service.direction === 'max') {
            if (aValue < bValue) aWorseThanB = true;
            if (aValue !== bValue) aEqualToB = false;
          } 
          // For minimization objectives
          else {
            if (aValue > bValue) aWorseThanB = true;
            if (aValue !== bValue) aEqualToB = false;
          }
        }
        
        // a is dominated by b if a is worse than b in at least one objective
        // and not better than b in any objective
        return aWorseThanB && !aEqualToB;
      };
      
      // Apply non-dominated sorting
      const paretoFront = [];
      
      for (let i = 0; i < solutions.length; i++) {
        let isDominatedBySomeone = false;
        
        for (let j = 0; j < solutions.length; j++) {
          if (i !== j && isDominated(solutions[i], solutions[j])) {
            isDominatedBySomeone = true;
            break;
          }
        }
        
        if (!isDominatedBySomeone) {
          paretoFront.push(solutions[i]);
        }
      }
      
      // Limit to paretoPoints
      return paretoFront.slice(0, this.optimizationForm.paretoPoints);
    },
    
    // Reset optimization
    resetOptimization() {
      // Reset form but keep selected services
      const selectedServices = [...this.optimizationForm.selectedServices];
      
      this.optimizationForm = {
        algorithm: 'mobo',
        selectedServices: selectedServices,
        iterations: 100,
        paretoPoints: 20,
        seed: 42
      };
      
      this.paretoSolutions = [];
      this.selectedSolution = null;
      this.optimizationCompleted = false;
      this.optimizationProgress = 0;
      this.currentIteration = 0;
      this.hypervolumeDifference = 0;
      this.tradeoffStarted = false;
      this.tradeoffProgress = 0;
      this.tradeoffStatus = '准备就绪';
      
      // Clear interval if running
      if (this.tradeoffInterval) {
        clearInterval(this.tradeoffInterval);
        this.tradeoffInterval = null;
      }
      
      // Reset charts
      this.initParetoChart();
      this.initComparisonChart();
      
      this.$message.info('已重置优化参数');
    },
    
    // Select a solution
    selectSolution(solution, index, event) {
      // 更新选择的方案
      this.selectedSolution = index;
      this.currentParameters = solution.parameters;
      
      // 重新初始化图表，以确保尺寸正确
      this.initComparisonChart();
      
      // 更新图表
      this.updateComparisonChart(solution);
      
      // 触发状态更新，确保Vue重新计算依赖属性
      this.$forceUpdate();
      
      // 输出日志
      console.log("已选择方案:", index+1);
      
      // 防止事件冒泡导致的问题
      if (event && event.stopPropagation) {
        // 如果是从事件处理函数调用的，阻止冒泡但不阻止默认行为
        event.stopPropagation();
      }
    },
    
    // Initialize Pareto front chart
    initParetoChart() {
      console.log('开始初始化图表');
      
      try {
        // 先获取DOM元素
        const chartDom = document.getElementById('paretoChart');
        if (!chartDom) {
          console.error('找不到paretoChart DOM元素');
          this.chartLoading = false; // 确保loading状态结束
          return;
        }
        
        // 清除现有实例
        if (this.paretoChart) {
          this.paretoChart.dispose();
          this.paretoChart = null;
        }
        
        // 设置确定的尺寸 - 增加高度
        chartDom.style.height = '480px';
        chartDom.style.width = '100%';
        
        console.log('图表容器尺寸:', chartDom.clientWidth, 'x', chartDom.clientHeight);
        
        // 创建图表实例
        this.paretoChart = echarts.init(chartDom);
        
        // 标记初始化完成
        this.chartInitialized = true;
        console.log('图表初始化成功');
        
        // 不自动更新图表，等待用户点击选择图表类型
        this.chartLoading = false;
      } catch (error) {
        console.error('图表初始化失败:', error);
        // 确保发生错误时也结束loading状态
        this.chartLoading = false;
      }
    },
    
    // 强制更新平行坐标图的新方法
    forceParetoChartUpdate() {
      if (!this.paretoChart || !this.chartInitialized) {
        console.log('图表未初始化，无法强制更新');
        return;
      }
      
      console.log('开始强制更新图表');
      const services = this.selectedServicesList;
      const solutions = this.paretoSolutions;
      
      if (services.length === 0 || solutions.length === 0) {
        this.showNoDataState();
        return;
      }
      
      // 获取当前图表类型的配置
      let option;
      if (this.visualizationType === 'parallel') {
        option = this.getParallelCoordinatesOption(services, solutions);
      } else if (this.visualizationType === 'scatter') {
        option = this.getScatterPlotOption(services, solutions);
      } else if (this.visualizationType === 'radar') {
        option = this.getRadarChartOption(services, solutions);
      } else {
        option = this.getParallelCoordinatesOption(services, solutions);
      }
      
      try {
        // 清除任何现有的graphic元素
        if (option.graphic) {
          option.graphic = { elements: [] };
        }
        
        // 应用新配置
        this.paretoChart.setOption(option, true);
        
        // 强制清空并重置尺寸
        this.paretoChart.clear();
        this.paretoChart.setOption(option, true);
        
        // 对于平行坐标图，需要特殊处理
        if (this.visualizationType === 'parallel') {
          // 立即结束loading状态，避免阻塞UI
          this.chartLoading = false;
          
          // 多次强制尺寸更新，解决平行坐标图初始显示问题
          for (let i = 0; i < 3; i++) {
            this.paretoChart.resize();
          }
          
          // 定时重绘多次来确保平行坐标图正确渲染
          setTimeout(() => {
            try {
              if (this.paretoChart) {
                this.paretoChart.clear();
                this.paretoChart.setOption(option, true); 
                this.paretoChart.resize();
              }
            } catch (e) {
              console.error('平行坐标图第一次更新失败:', e);
            }
          }, 100);
          
          setTimeout(() => {
            try {
              if (this.paretoChart) {
                this.paretoChart.resize();
                console.log('平行坐标图第二次resize');
              }
            } catch (e) {
              console.error('平行坐标图第二次更新失败:', e);
            }
          }, 300);
          
          setTimeout(() => {
            try {
              if (this.paretoChart) {
                this.paretoChart.resize();
                console.log('平行坐标图第三次resize');
                this.initialRenderComplete = true;
              }
            } catch (e) {
              console.error('平行坐标图第三次更新失败:', e);
            }
          }, 600);
        } else {
          // 连续多次resize确保尺寸正确
          this.paretoChart.resize();
          setTimeout(() => {
            try {
              if (this.paretoChart) {
                this.paretoChart.resize();
                console.log('强制resize完成');
                this.initialRenderComplete = true;
              }
            } catch (e) {
              console.error('resize失败:', e);
            } finally {
              // 确保loading状态结束
              this.chartLoading = false;
            }
          }, 200);
        }
        
        console.log('图表强制更新完成');
      } catch (error) {
        console.error('强制更新图表失败:', error);
        // 确保发生错误时也结束loading状态
        this.chartLoading = false;
      }
    },
    
    // Show no data state for chart
    showNoDataState() {
      try {
        if (!this.paretoChart) {
          // 确保图表不存在时也关闭loading状态
          this.chartLoading = false;
          return;
        }
        
        this.paretoChart.setOption({
          graphic: {
            elements: [{
              type: 'text',
              left: 'center',
              top: 'middle',
              style: {
                text: '暂无Pareto前沿数据',
                fontSize: 20,
                fontWeight: 'normal',
                fill: '#999'
              }
            }]
          },
          series: []
        });
        
        // 即使显示无数据状态后，也进行一次resize确保正确渲染
        this.paretoChart.resize();
      } catch (error) {
        console.error('显示无数据状态失败:', error);
      } finally {
        // 无论任何情况都确保关闭loading状态
        this.chartLoading = false;
      }
    },
    
    // Update Pareto chart based on visualization type and data
    updateParetoChart() {
      if (!this.paretoChart) {
        console.log('图表实例不存在，重新初始化');
        this.initParetoChart();
        return;
      }
      
      const services = this.selectedServicesList;
      const solutions = this.paretoSolutions;
      
      console.log(`更新图表: 类型=${this.visualizationType}, 服务数=${services.length}, 解决方案数=${solutions.length}`);
      
      if (services.length === 0 || solutions.length === 0) {
        console.log('无数据可显示');
        this.showNoDataState();
        this.chartLoading = false;
        return;
      }
      
      let option;
      
      // Different chart types
      switch (this.visualizationType) {
        case 'parallel':
          option = this.getParallelCoordinatesOption(services, solutions);
          // 平行坐标图需要提前结束loading状态，避免卡住
          this.chartLoading = false;
          break;
        
        case 'scatter':
          option = this.getScatterPlotOption(services, solutions);
          break;
        
        case 'radar':
          option = this.getRadarChartOption(services, solutions);
          break;
        
        default:
          option = this.getParallelCoordinatesOption(services, solutions);
          // 默认情况也是平行坐标图，同样提前结束loading
          this.chartLoading = false;
      }
      
      // 清除无数据状态的graphic元素
      if (option.graphic) {
        option.graphic = { elements: [] };
      }
      
      // 打印最终的图表配置
      console.log(`图表配置生成: 类型=${this.visualizationType}`);
      
      try {
        // 更新图表配置
        this.paretoChart.setOption(option, true);
        
        // 强制重绘以确保填满容器
        this.$nextTick(() => {
          if (this.paretoChart) {
            // 先清除容器样式，再重新设置容器样式，解决尺寸问题
            const chartDom = document.getElementById('paretoChart');
            if (chartDom) {
              // 如果是平行坐标图且是初次渲染，执行特殊处理
              if (this.visualizationType === 'parallel') {
                // 先清空图表
                this.paretoChart.clear();
                // 重新应用配置
                this.paretoChart.setOption(option, true);
                // 强制多次重绘
                this.paretoChart.resize();
                
                setTimeout(() => {
                  if (this.paretoChart) {
                    this.paretoChart.resize();
                    this.initialRenderComplete = true;
                  }
                }, 300);
              } else {
                this.paretoChart.resize();
              }
              
              console.log('图表已重绘完成');
            }
          }
        });
      } catch (error) {
        console.error('图表更新失败:', error);
      }
      
      // 确保loading状态结束
      this.chartLoading = false;
    },
    
    // 图表类型更改时的处理
    handleVisualizationTypeChange() {
      // 如果没有数据，不做任何操作
      if (this.paretoSolutions.length === 0) {
        return;
      }
      
      // 设置loading状态
      this.chartLoading = true;
      
      // 重置初始化状态
      this.initialRenderComplete = false;
      
      // 延迟执行以显示loading效果
      setTimeout(() => {
        try {
          // 如果图表未初始化，先初始化
          if (!this.paretoChart || !this.chartInitialized) {
            this.initParetoChart();
          }
          
          // 获取服务和解决方案数据
          const services = this.selectedServicesList;
          const solutions = this.paretoSolutions;
          
          // 根据当前可视化类型获取图表配置
          let option;
          
          if (this.visualizationType === 'parallel') {
            option = this.getParallelCoordinatesOption(services, solutions);
          } else if (this.visualizationType === 'scatter') {
            option = this.getScatterPlotOption(services, solutions);
          } else if (this.visualizationType === 'radar') {
            option = this.getRadarChartOption(services, solutions);
          }
          
          // 应用图表配置
          if (option && this.paretoChart) {
            this.paretoChart.clear();
            this.paretoChart.setOption(option, true);
            this.paretoChart.resize();
          }
        } catch (error) {
          console.error('切换图表类型失败:', error);
        } finally {
          // 无论成功失败，结束loading状态
          this.chartLoading = false;
        }
      }, 300);
    },
    
    // Get parallel coordinates chart option
    getParallelCoordinatesOption(services, solutions) {
      // 如果没有服务或解决方案，返回空配置
      if (services.length === 0 || solutions.length === 0) {
        return {
          series: []
        };
      }
      
      // 准备平行坐标轴
      const parallelAxis = services.map((service, index) => {
        return {
          dim: index,
          name: service.name,
          nameLocation: 'end',
          nameGap: 20,
          nameTextStyle: {
            color: this.getServiceColor(service.id),
            fontSize: 12,
            fontWeight: 'bold'  // 加粗轴名称
          },
          min: 0,
          max: 100,
          inverse: service.direction === 'min',
          axisLine: {
            lineStyle: {
              width: 2,  // 加粗坐标轴线
              color: '#666'  // 坐标轴颜色更深
            }
          },
          axisTick: {
            show: true,  // 显示刻度
            length: 5,   // 刻度长度
            lineStyle: {
              color: '#666' // 刻度颜色
            }
          },
          axisLabel: {
            color: '#333',  // 刻度标签颜色加深
            fontSize: 12,
            fontWeight: 'normal'
          }
        };
      });
      
      // 准备数据
      const data = solutions.map(solution => {
        return services.map(service => solution.services[service.id]);
      });
      
      // 生成不同颜色供线条使用 - 使用更鲜明的配色
      const colors = [
        '#e01f54', '#0098d9', '#2b821d', '#9932CC', 
        '#ff9933', '#8B0000', '#2f4554', '#9b59b6',
        '#1abc9c', '#e74c3c', '#3498db'
      ];
      
      // 返回配置
      return {
        backgroundColor: '#fff',  // 白色背景
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          backgroundColor: 'rgba(255,255,255,0.9)',  // 更不透明的背景
          borderColor: '#ccc',
          borderWidth: 1,
          textStyle: {
            color: '#333'
          },
          formatter: (params) => {
            const solutionIndex = params[0].dataIndex;
            const solution = solutions[solutionIndex];
            let tooltipText = `方案 ${solutionIndex + 1}<br/>`;
            
            services.forEach((service, index) => {
              const value = solution.services[service.id].toFixed(2);
              tooltipText += `${service.name}: ${value}<br/>`;
            });
            
            return tooltipText;
          }
        },
        parallelAxis: parallelAxis,
        parallel: {
          left: '10%',
          right: '10%',
          bottom: '20%',      // 增加底部边距，确保不被遮挡
          top: '15%',         // 减少顶部边距
          width: 'auto',      // 自动宽度
          height: '75%',      // 明确设置高度
          layout: 'horizontal', // 水平布局
          axisExpandable: true,  // 允许坐标轴伸缩
          axisExpandWidth: 50,   // 伸缩的宽度
          axisExpandTriggerOn: 'mousemove',  // 鼠标悬停时触发
          parallelAxisDefault: {
            type: 'value',
            nameGap: 20,
            nameLocation: 'end',
            nameTextStyle: {
              fontSize: 12,
              fontWeight: 'bold'  // 加粗
            },
            axisLine: {
              lineStyle: {
                width: 2,  // 加粗线宽
                color: '#666'  // 更深的颜色
              }
            },
            axisTick: {
              show: true,  // 显示刻度
              lineStyle: {
                color: '#666'  // 刻度颜色
              }
            },
            splitLine: {
              show: true,
              lineStyle: {
                type: 'solid',  // 实线
                opacity: 0.3,   // 增加不透明度
                color: '#aaa'   // 更深的颜色
              }
            }
          }
        },
        animation: false, // 禁用动画，立即渲染
        color: colors,  // 使用自定义颜色列表
        blendMode: 'source-over', // 设置混合模式为正常
        series: [
          {
            name: 'Pareto解集',
            type: 'parallel',
            lineStyle: {
              width: 4,  // 增加线宽
              opacity: 1,  // 完全不透明
              shadowBlur: 0,  // 移除阴影模糊
              color: 'auto'  // 使用系列颜色
            },
            emphasis: {
              lineStyle: {
                width: 6,  // 加粗悬停线宽
                opacity: 1,  // 完全不透明
                shadowBlur: 5,  // 添加阴影
                shadowColor: 'rgba(0,0,0,0.5)'  // 阴影颜色
              }
            },
            data: data,
            smooth: false  // 禁用平滑，使线条更清晰
          }
        ]
      };
    },
    
    // Get scatter plot option
    getScatterPlotOption(services, solutions) {
      // 如果服务少于2个，无法显示散点图
      if (services.length < 2 || solutions.length === 0) {
        return {
          series: []
        };
      }
      
      // 默认使用前两个服务作为XY轴
      const xService = services[0];
      const yService = services[1];
      
      // 准备数据
      const data = solutions.map((solution, index) => {
        return [
          solution.services[xService.id],
          solution.services[yService.id],
          index // 用于标识点
        ];
      });
      
      // 返回配置
      return {
        animation: false, // 禁用动画，立即渲染
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            const solutionIndex = params.data[2];
            const solution = solutions[solutionIndex];
            let tooltipText = `方案 ${solutionIndex + 1}<br/>`;
            
            services.forEach(service => {
              const value = solution.services[service.id].toFixed(2);
              tooltipText += `${service.name}: ${value}<br/>`;
            });
            
            return tooltipText;
          }
        },
        xAxis: {
          name: xService.name,
          nameGap: 25,
          nameLocation: 'middle',
          inverse: xService.direction === 'min',
          min: 0,
          max: 100,
          type: 'value',
          splitLine: {
            lineStyle: {
              type: 'dashed'
            }
          },
          axisLabel: {
            formatter: '{value}'
          }
        },
        yAxis: {
          name: yService.name,
          nameGap: 25,
          nameLocation: 'middle',
          inverse: yService.direction === 'min',
          min: 0,
          max: 100,
          type: 'value',
          splitLine: {
            lineStyle: {
              type: 'dashed'
            }
          },
          axisLabel: {
            formatter: '{value}',
            margin: 10 // 增加标签与轴的距离
          }
        },
        grid: {
          left: '10%',    // 增加左侧边距
          right: '7%',    // 增加右侧边距
          bottom: '15%',  // 增加底部边距，解决底部遮挡问题
          top: '10%',
          containLabel: true
        },
        series: [
          {
            name: 'Pareto解集',
            type: 'scatter',
            symbolSize: 12,
            itemStyle: {
              opacity: 0.9, // 增加不透明度
              color: function(params) {
                const solutionIndex = params.data[2];
                return solutionIndex === this.selectedSolution ? '#ff4500' : '#5470c6';
              }.bind(this)
            },
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowColor: 'rgba(0, 0, 0, 0.3)'
              }
            },
            data: data
          }
        ]
      };
    },
    
    // Get radar chart option
    getRadarChartOption(services, solutions) {
      // 如果没有服务或解决方案，返回空配置
      if (services.length === 0 || solutions.length === 0) {
        return {
          series: []
        };
      }
      
      // 准备雷达图指示器
      const indicator = services.map(service => {
        return {
          name: service.name,
          max: 100,
          min: 0,
          color: this.getServiceColor(service.id)
        };
      });
      
      // 准备数据
      const seriesData = solutions.map((solution, index) => {
        return {
          value: services.map(service => {
            // 对于最小化目标，我们需要反转值以在雷达图上正确显示
            const value = solution.services[service.id];
            return service.direction === 'min' ? 100 - value : value;
          }),
          name: `方案 ${index + 1}`
        };
      });
      
      // 返回配置
      return {
        animation: false, // 禁用动画，立即渲染
        tooltip: {
          trigger: 'item',
          formatter: (params) => {
            const solutionIndex = solutions.findIndex(
              s => `方案 ${solutions.indexOf(s) + 1}` === params.name
            );
            if (solutionIndex === -1) return '';
            
            const solution = solutions[solutionIndex];
            let tooltipText = `${params.name}<br/>`;
            
            services.forEach(service => {
              const value = solution.services[service.id].toFixed(2);
              tooltipText += `${service.name}: ${value}<br/>`;
            });
            
            return tooltipText;
          }
        },
        radar: {
          indicator: indicator,
          radius: '65%', // Reduced from 70% for better fit
          center: ['50%', '55%'], // Shifted down slightly from ['50%', '50%']
          name: {
            formatter: '{value}',
            textStyle: {
              fontSize: 12
            }
          },
          axisName: {
            fontSize: 11, // Reduce font size for axis names
            padding: [3, 5] // Add padding to prevent text overlap with axes
          },
          splitArea: {
            areaStyle: {
              opacity: 0.1 // Reduce opacity of background areas
            }
          }
        },
        series: [
          {
            name: 'Pareto解集',
            type: 'radar',
            data: seriesData,
            symbol: 'circle',
            symbolSize: 6,
            areaStyle: {
              opacity: 0.3
            },
            lineStyle: {
              width: 2,
              opacity: 0.9 // 增加线的不透明度
            }
          }
        ]
      };
    },
    
    // Initialize comparison chart
    initComparisonChart() {
      const chartDom = document.getElementById('comparisonChart');
      if (!chartDom) return;
      
      if (this.comparisonChart) {
        this.comparisonChart.dispose();
      }
      
      // 设置容器样式确保图表能正常显示
      chartDom.style.width = '100%';
      if (this.selectedServicesList.length > 3) {
        chartDom.style.height = '330px'; // 为平行坐标图提供更多空间
      } else {
        chartDom.style.height = '280px';
      }
      
      this.comparisonChart = echarts.init(chartDom, null, {
        renderer: 'canvas',
        useDirtyRect: false,
        resize: true
      });
      
      this.comparisonChart.setOption({
        title: {
          text: '方案对比',
          left: 'center',
          top: 10, // Increased from 0
          textStyle: {
            fontSize: 14
          }
        },
        tooltip: {
          trigger: 'item'
        },
        grid: {
          top: 40, // Increased from 30
          bottom: 40, // Increased from 30
          left: 40, // Increased from 30
          right: 40, // Increased from 30
          containLabel: true // Added to ensure labels are contained within the grid
        },
        series: []
      });
      
      // 强制立即resize以确保容器尺寸正确
      this.comparisonChart.resize();
    },
    
    // Update comparison chart with selected solution
    updateComparisonChart(solution) {
      if (!this.comparisonChart) {
        this.initComparisonChart();
      }
      
      const services = this.selectedServicesList;
      if (services.length === 0) return;
      
      // 根据维度数量选择不同的图表类型
      if (services.length > 3) {
        // 使用平行坐标图来展示4个及以上维度
        this.renderParallelComparisonChart(solution, services);
      } else {
        // 3个及以下维度使用雷达图
        this.renderRadarComparisonChart(solution, services);
      }
      
      // 图表自适应
      this.$nextTick(() => {
        this.comparisonChart.resize();
      });
    },
    
    // 渲染雷达图
    renderRadarComparisonChart(solution, services) {
      // 准备雷达图数据
      const indicator = services.map(service => {
        return {
          name: service.name,
          max: 100,
          min: 0
        };
      });
      
      const data = [{
        value: services.map(service => {
          const value = solution.services[service.id];
          return service.direction === 'min' ? 100 - value : value;
        }),
        name: '当前方案',
        itemStyle: {
          color: '#5470c6'
        }
      }];
      
      this.comparisonChart.setOption({
        radar: {
          indicator: indicator,
          radius: '65%',
          center: ['50%', '55%'],
          name: {
            formatter: '{value}',
            textStyle: {
              fontSize: 12
            }
          },
          axisName: {
            fontSize: 11,
            padding: [3, 5]
          },
          splitArea: {
            areaStyle: {
              opacity: 0.1
            }
          }
        },
        series: [{
          type: 'radar',
          data: data,
          symbol: 'circle',
          symbolSize: 6,
          areaStyle: {
            opacity: 0.3
          },
          lineStyle: {
            width: 2
          },
          emphasis: {
            areaStyle: {
              opacity: 0.8
            }
          }
        }]
      });
    },
    
    // 渲染平行坐标图
    renderParallelComparisonChart(solution, services) {
      // 准备平行坐标轴
      const parallelAxis = services.map((service, index) => {
        return {
          dim: index,
          name: service.name,
          nameGap: 15,
          nameLocation: 'start', // 修改为start，避免标签重叠
          nameTextStyle: {
            fontSize: 12,
            color: this.getServiceColor(service.id),
            fontWeight: 'bold',
            padding: [10, 0, 0, 0] // 添加上方内边距，避免与轴线重叠
          },
          min: 0,
          max: 100,
          inverse: service.direction === 'min', // 对于最小化目标，轴方向反转
          axisLine: {
            lineStyle: {
              width: 2,
              color: this.getServiceColor(service.id)
            }
          },
          axisTick: {
            show: true,
            lineStyle: {
              color: this.getServiceColor(service.id)
            }
          },
          axisLabel: {
            show: true,
            fontSize: 10, // 调小字体，避免拥挤
            formatter: '{value}' // 简单显示数值
          }
        };
      });
      
      // 确保数据格式正确 - 每个方案是一个数组
      const data = [services.map(service => {
        // 对最小化目标进行数值转换，保持一致的比较方向
        const value = solution.services[service.id];
        return service.direction === 'min' ? 100 - value : value;
      })];
      
      // 设置图表
      this.comparisonChart.clear(); // 清除之前的图表
      this.comparisonChart.setOption({
        backgroundColor: '#fff',
        tooltip: {
          show: true,
          trigger: 'item',
          backgroundColor: 'rgba(255,255,255,0.8)',
          borderColor: '#ccc',
          borderWidth: 1,
          padding: 10,
          textStyle: {
            color: '#333'
          },
          formatter: (params) => {
            // 确保正确获取数据
            if (!params.value) return '';
            
            let tooltipText = `方案 ${this.selectedSolution + 1}<br/>`;
            services.forEach((service, index) => {
              const displayValue = params.value[index];
              // 转换回原始值
              const originalValue = service.direction === 'min' ? 
                (100 - displayValue).toFixed(2) : displayValue.toFixed(2);
              tooltipText += `${service.name}: ${originalValue}<br/>`;
            });
            return tooltipText;
          }
        },
        parallelAxis: parallelAxis,
        parallel: {
          left: '8%',
          right: '8%',
          bottom: '15%',
          top: '15%',
          width: 'auto',
          height: 'auto',
          layout: services.length > 5 ? 'vertical' : 'horizontal', // 超过5个维度时使用垂直布局
          axisExpandable: true, // 允许轴扩展
          axisExpandWidth: 40, // 轴扩展宽度
          axisExpandTriggerOn: 'click', // 点击时触发扩展
          parallelAxisDefault: {
            type: 'value',
            axisLine: {
              lineStyle: {
                width: 2,
                color: '#666'
              }
            },
            axisTick: {
              show: true,
              lineStyle: {
                color: '#666'
              }
            },
            splitLine: {
              show: true,
              lineStyle: {
                color: '#ddd',
                type: 'dashed',
                opacity: 0.3
              }
            }
          }
        },
        series: [
          {
            name: '当前方案',
            type: 'parallel',
            lineStyle: {
              width: 3,
              opacity: 0.8,
              color: '#5470c6'
            },
            emphasis: {
              lineStyle: {
                width: 5,
                opacity: 1,
                color: '#f56c6c'
              }
            },
            data: data
          }
        ]
      }, true);
      
      // 强制重绘确保显示正确
      this.$nextTick(() => {
        this.comparisonChart.resize();
        
        // 300ms后再次resize，确保布局完全加载
        setTimeout(() => {
          if (this.comparisonChart) {
            this.comparisonChart.resize();
            console.log('平行坐标图第二次resize完成');
          }
        }, 300);
      });
    },
    
    // View solution map
    viewSolutionMap(solution) {
      this.mapDialogVisible = true;
      
      // Initialize map in next tick to ensure DOM is ready
      this.$nextTick(() => {
        this.initSolutionMap(solution, 'dialogSolutionMap');
      });
    },
    
    // Initialize dialog solution map
    initDialogSolutionMap(solution) {
      this.initSolutionMap(solution, 'dialogSolutionMap');
    },
    
    // Handle resize event
    handleResize() {
      // 使用防抖函数处理resize，避免频繁调用
      if (this.resizeTimeout) {
        clearTimeout(this.resizeTimeout);
      }
      
      this.resizeTimeout = setTimeout(() => {
        // 重绘所有图表
      if (this.paretoChart) {
          this.paretoChart.resize({width: 'auto', height: 'auto'});
      }
      
      if (this.comparisonChart) {
        this.comparisonChart.resize();
      }
        
        // 如果当前在可视化步骤，确保图表填满容器
        if (this.currentStep === 5 && this.paretoChart) {
          this.$nextTick(() => {
            this.paretoChart.resize();
          });
        }
        
        // 如果当前在方案比较步骤，确保比较图表填满容器
        if (this.currentStep === 6 && this.comparisonChart && this.selectedSolution !== null) {
          this.$nextTick(() => {
            this.updateComparisonChart(this.paretoSolutions[this.selectedSolution]);
          });
        }
      }, 200);
    },
    
    // 页面激活时重新调整图表
    activated() {
      // 短暂延迟确保DOM已完全渲染
      setTimeout(() => {
        this.handleResize();
        
        // 如果当前在可视化步骤，重新渲染图表
        if (this.currentStep === 5 && this.paretoChart) {
          this.updateParetoChart();
        }
        
        // 如果当前在方案比较步骤，重新渲染对比图
        if (this.currentStep === 6 && this.comparisonChart && this.selectedSolution !== null) {
          this.updateComparisonChart(this.paretoSolutions[this.selectedSolution]);
        }
      }, 300);
    },
    
    // Export results
    exportResults() {
      if (this.paretoSolutions.length === 0) {
        this.$message.warning('尚未生成优化结果');
        return;
      }
      
      this.$message.success('结果已导出');
    },
    
    // Save optimization
    saveOptimization() {
      if (this.paretoSolutions.length === 0) {
        this.$message.warning('尚未生成优化结果');
        return;
      }
      
      this.$message.success('优化方案已保存');
    },
    
    // Export solutions
    exportSolutions() {
      if (this.paretoSolutions.length === 0) {
        this.$message.warning('尚未生成优化结果');
        return;
      }
      
      // Create CSV content
      let csvContent = "data:text/csv;charset=utf-8,";
      
      // Add header row
      const headers = ["方案编号", ...this.selectedServicesList.map(s => s.name), "参数配置"];
      csvContent += headers.join(",") + "\r\n";
      
      // Add data rows
      this.paretoSolutions.forEach((solution, index) => {
        const row = [
          `方案${index + 1}`,
          ...this.selectedServicesList.map(s => solution.services[s.id].toFixed(2)),
          JSON.stringify(solution.parameters).replace(/,/g, ";")
        ];
        csvContent += row.join(",") + "\r\n";
      });
      
      // Create download link
      const encodedUri = encodeURI(csvContent);
      const link = document.createElement("a");
      link.setAttribute("href", encodedUri);
      link.setAttribute("download", `多目标优化结果_${new Date().toISOString().slice(0, 10)}.csv`);
        document.body.appendChild(link);
      
      // Trigger download
        link.click();
        document.body.removeChild(link);
        
      this.$message.success('解决方案数据已导出');
    },
    
    // View solution map
    viewSolutionMap(solution) {
      this.mapDialogVisible = true;
      
      // Initialize map in next tick to ensure DOM is ready
      this.$nextTick(() => {
        this.initSolutionMap(solution);
      });
    },
    
    // Initialize solution map
    initSolutionMap(solution, targetId = 'solutionMap') {
      const mapContainer = document.getElementById(targetId);
      if (!mapContainer) return;
      
      // This is a placeholder for actual map implementation
      // In a real application, you would use a mapping library like Leaflet or OpenLayers
      
      mapContainer.innerHTML = '';
      const mapInfo = document.createElement('div');
      mapInfo.className = 'map-info';
      mapInfo.innerHTML = `
        <h3>方案空间分布图</h3>
        <p>此处应展示方案在空间上的分布情况</p>
        <p>服务价值数据:</p>
        <ul>
          ${Object.entries(solution.services).map(([key, value]) => 
            `<li>${this.getServiceName(key)}: ${value.toFixed(2)}</li>`
          ).join('')}
        </ul>
      `;
      
      mapContainer.appendChild(mapInfo);
    },
    
    // Close map dialog
    closeMapDialog() {
      this.mapDialogVisible = false;
    },
    
    // Export map as image
    exportMap() {
      this.$message.success('空间分布图已导出');
    },
    
    // Get service color by ID
    getServiceColor(serviceId) {
      const service = this.availableServices.find(s => s.id === serviceId);
      return service ? service.color : this.serviceColors[serviceId] || '#409EFF';
    },
    
    // Get service name by ID
    getServiceName(serviceId) {
      const service = this.availableServices.find(s => s.id === serviceId);
      return service ? service.name : (this.serviceTypeMap[serviceId] || serviceId);
    },
    
    // Get parameter name
    getParameterName(param) {
      const paramNameMap = {
        'landuse': '土地利用类型',
        'forestCover': '森林覆盖率',
        'cropRotation': '作物轮作',
        'irrigationScheme': '灌溉方案',
        'fertilization': '施肥量'
      };
      
      return paramNameMap[param] || param;
    },
    
    // Update service range values
    updateServiceRange(service) {
      if (service.rangeValue && Array.isArray(service.rangeValue)) {
        service.minValue = service.rangeValue[0];
        service.maxValue = service.rangeValue[1];
      }
    },
    
    // Format the range tooltip for sliders
    formatRangeTooltip(val) {
      return val + '%';
    },
    
    // Set initial range values for all services
    initServiceRanges() {
      this.availableServices.forEach(service => {
        if (!service.rangeValue) {
          this.$set(service, 'rangeValue', [service.minValue, service.maxValue]);
        }
      });
    },
    
    // Start tradeoff process
    startTradeoff() {
      if (!this.canStartTradeoff) {
        this.$message.warning('请先完成优化过程');
        return;
      }
      
      this.tradeoffStarted = true;
      this.tradeoffProgress = 0;
      this.tradeoffStatus = '正在分析参数敏感性...';
      
      // Simulate tradeoff process
      if (this.tradeoffInterval) {
        clearInterval(this.tradeoffInterval);
      }
      
      this.tradeoffInterval = setInterval(() => {
        this.tradeoffProgress += 5;
        
        // Update status based on progress
        if (this.tradeoffProgress >= 25 && this.tradeoffProgress < 50) {
          this.tradeoffStatus = '正在计算权衡关系...';
        } else if (this.tradeoffProgress >= 50 && this.tradeoffProgress < 75) {
          this.tradeoffStatus = '正在生成解决方案...';
        } else if (this.tradeoffProgress >= 75 && this.tradeoffProgress < 100) {
          this.tradeoffStatus = '正在优化解决方案...';
        }
        
        if (this.tradeoffProgress >= 100) {
          this.tradeoffProgress = 100;
          this.tradeoffStatus = '权衡分析完成';
          clearInterval(this.tradeoffInterval);
          this.$message.success('多目标服务权衡分析已完成！');
        }
      }, 200);
    },
    
    // 步骤导航方法
    nextStep() {
      if (this.canGoNext) {
        if (this.currentStep === 4 && !this.optimizationCompleted) {
          // 第5步（确认优化）需要先完成优化才能进入下一步
          this.$message.warning('请先完成优化过程再进入下一步');
          return;
        }
        
        // 方案比较步骤的简化处理
        if (this.currentStep === 6 && this.selectedSolution === null) {
          this.$message.warning('请先选择一个解决方案');
          return;
        }
        
        if (this.currentStep < 7) {
          this.currentStep++;
          
          // 从方案比较步骤进入方案详情步骤时初始化地图
          if (this.currentStep === 7 && this.selectedSolution !== null) {
            this.$nextTick(() => {
              this.initEmbeddedSolutionMap(this.paretoSolutions[this.selectedSolution]);
            });
          }
          
          // 切换到可视化步骤时重新渲染图表
          if (this.currentStep === 5) {
            this.$nextTick(() => {
              // 初始化图表容器，但不绘制图表
              this.initParetoChart();
              
              // 如果没有Pareto解集，生成模拟数据
              if (this.paretoSolutions.length === 0 && this.optimizationCompleted) {
                this.generateSimulatedParetoData();
              }
              
                              // 确保没有选中的可视化类型
                this.visualizationType = '';
                
                // 清除任何现有的图表绘制
                if (this.paretoChart) {
                  this.paretoChart.clear();
                }
                
                // 提示用户点击按钮选择图表类型
                this.$message.info('请点击图表类型按钮查看Pareto前沿');
            });
          }
          
          // 切换到方案比较步骤时重新渲染对比图
          if (this.currentStep === 6 && this.selectedSolution !== null) {
            this.$nextTick(() => {
              this.updateComparisonChart(this.paretoSolutions[this.selectedSolution]);
            });
          }
          
          // 切换到方案详情步骤时初始化地图
          if (this.currentStep === 7 && this.selectedSolution !== null) {
            this.$nextTick(() => {
              this.initEmbeddedSolutionMap(this.paretoSolutions[this.selectedSolution]);
            });
          }
        }
      } else {
        // 根据当前步骤给出具体提示
        if (this.currentStep === 0 && !this.optimizationForm.algorithm) {
          this.$message.warning('请选择优化算法');
        } else if (this.currentStep === 1 && this.optimizationForm.selectedServices.length < 2) {
          this.$message.warning('请至少选择两个服务目标');
        } else if (this.currentStep === 4 && !this.optimizationCompleted) {
          this.$message.warning('请先完成优化过程');
        } else if (this.currentStep === 6 && this.selectedSolution === null) {
          this.$message.warning('请先选择一个解决方案');
        }
      }
    },
    
    prevStep() {
      if (this.currentStep > 0) {
        this.currentStep--;
        
        // 当从方案详情返回到方案比较时，确保选择状态保持
        if (this.currentStep === 6) {
          this.$nextTick(() => {
            // 如果有选中的方案，重新加载对比图
            if (this.selectedSolution !== null && this.paretoSolutions.length > 0) {
              this.updateComparisonChart(this.paretoSolutions[this.selectedSolution]);
            }
          });
        }
      }
    },
    
    // 获取算法标签
    getAlgorithmLabel(val) {
      switch(val) {
        case 'mobo': return '多目标贝叶斯优化';
        case 'nsga2': return 'NSGA-II算法';
        case 'weighted': return '权重和方法';
        default: return val;
      }
    },
    
    // 初始化权重参数
    initWeightParams() {
      this.weightParams = {};
      // 为每个可能的服务设置默认权重
      this.availableServices.forEach(service => {
        this.$set(this.weightParams, service.id, 5); // 默认中等权重
      });
    },
    
    // 表格行样式
    tableRowClassName({row, rowIndex}) {
      if (rowIndex === this.selectedSolution) {
        return 'selected-row';
      }
      return '';
    },
    
    // 嵌入式地图初始化
    initEmbeddedSolutionMap(solution) {
      const mapContainer = document.getElementById('solutionMap');
      if (!mapContainer) return;
      
      // This is a placeholder for actual map implementation
      // In a real application, you would use a mapping library like Leaflet or OpenLayers
      
      mapContainer.innerHTML = '';
      const mapInfo = document.createElement('div');
      mapInfo.className = 'map-info';
      mapInfo.innerHTML = `
        <h3>方案空间分布图</h3>
        <p>此处应展示方案在空间上的分布情况</p>
        <p>服务价值数据:</p>
        <ul>
          ${Object.entries(solution.services).map(([key, value]) => 
            `<li>${this.getServiceName(key)}: ${value.toFixed(2)}</li>`
          ).join('')}
        </ul>
      `;
      
      mapContainer.appendChild(mapInfo);
    },
    
    // 优化完成后自动进入结果页
    afterOptimizationComplete() {
      this.$message.success('多目标优化已完成！');
      
      // 确保loading状态结束
      this.chartLoading = false;
      
      // 如果当前在步骤5（确认优化），自动进入下一步
      if (this.currentStep === 4) {
        // 给一点延迟，确保UI更新完成
        setTimeout(() => {
          this.nextStep();
        }, 300);
      }
    },
    
    // Handle resize event
    handleResize() {
      // 使用防抖函数处理resize，避免频繁调用
      if (this.resizeTimeout) {
        clearTimeout(this.resizeTimeout);
      }
      
      this.resizeTimeout = setTimeout(() => {
        // 重绘所有图表
        if (this.paretoChart) {
          this.paretoChart.resize({width: 'auto', height: 'auto'});
        }
        
        if (this.comparisonChart) {
          this.comparisonChart.resize();
        }
        
        // 如果当前在可视化步骤，确保图表填满容器
        if (this.currentStep === 5 && this.paretoChart) {
          this.$nextTick(() => {
            this.paretoChart.resize();
          });
        }
        
        // 如果当前在方案比较步骤，确保比较图表填满容器
        if (this.currentStep === 6 && this.comparisonChart && this.selectedSolution !== null) {
          this.$nextTick(() => {
            this.comparisonChart.resize();
          });
        }
      }, 200);
    },
    
    // 生成模拟的Pareto前沿数据
    generateSimulatedParetoData() {
      if (this.selectedServicesList.length < 2) {
        this.$message.warning('至少需要选择两个服务目标才能生成Pareto前沿');
        return;
      }
      
      // 清空现有数据
      this.paretoSolutions = [];
      
      // 根据配置生成指定数量的解决方案
      const solutionCount = this.optimizationForm.paretoPoints || 20;
      const selectedServices = this.selectedServicesList;
      
      // 生成Pareto前沿形状的点
      for (let i = 0; i < solutionCount; i++) {
        // 对于Pareto前沿，我们希望生成的点能体现出目标之间的权衡关系
        // 例如，对于二维情况，我们希望曲线是凸的
        
        // 使用参数化方法生成解决方案
        const t = i / (solutionCount - 1);  // 范围[0, 1]
        const solution = this.generateParetoSolution(selectedServices, t);
        
        this.paretoSolutions.push(solution);
      }
      
      // 随机选择一个解决方案作为当前选中
      if (this.paretoSolutions.length > 0) {
        const randomIndex = Math.floor(Math.random() * this.paretoSolutions.length);
        this.selectSolution(this.paretoSolutions[randomIndex], randomIndex);
      }
      
      this.$message.success(`已生成${this.paretoSolutions.length}个模拟Pareto最优解！`);
    },
    
    // 根据参数t生成Pareto最优解
    generateParetoSolution(services, t) {
      const serviceValues = {};
      const parameters = {};
      
      // 针对二维或多维Pareto前沿生成均衡的权衡点
      // 例如，一个目标增加，另一个目标就会减少
      services.forEach((service, index) => {
        if (services.length === 2) {
          // 二维情况：生成凸的Pareto前沿曲线
          if (index === 0) {
            // 第一个目标随t增加
            serviceValues[service.id] = service.direction === 'max' 
              ? service.minValue + (service.maxValue - service.minValue) * Math.pow(t, 0.7)
              : service.maxValue - (service.maxValue - service.minValue) * Math.pow(t, 0.7);
          } else {
            // 第二个目标随t减少
            serviceValues[service.id] = service.direction === 'max'
              ? service.maxValue - (service.maxValue - service.minValue) * Math.pow(t, 1.3)
              : service.minValue + (service.maxValue - service.minValue) * Math.pow(t, 1.3);
          }
        } else {
          // 多维情况：使用一个更复杂的公式来确保多个目标之间的权衡
          // 使用余弦函数来创建不同目标之间的波动
          const phase = (2 * Math.PI * index) / services.length;
          const amplitude = (service.maxValue - service.minValue) / 2;
          const offset = service.minValue + amplitude;
          
          // 使用余弦函数生成波动的服务值
          const value = offset + amplitude * Math.cos(phase + t * 2 * Math.PI);
          
          // 保证值在服务的最小值和最大值范围内
          serviceValues[service.id] = Math.max(service.minValue, Math.min(service.maxValue, value));
        }
      });
      
      // 生成随机参数
      const paramNames = ['landuse', 'forestCover', 'cropRotation', 'irrigationScheme', 'fertilization'];
      paramNames.forEach(param => {
        if (param === 'landuse') {
          parameters[param] = Math.random() > 0.5 ? '农田为主' : '林地为主';
        } else if (param === 'forestCover') {
          parameters[param] = (Math.random() * 60 + 30).toFixed(1) + '%';
        } else if (param === 'cropRotation') {
          parameters[param] = Math.random() > 0.5 ? '是' : '否';
        } else if (param === 'irrigationScheme') {
          const schemes = ['滴灌', '喷灌', '沟灌', '微灌'];
          parameters[param] = schemes[Math.floor(Math.random() * schemes.length)];
        } else if (param === 'fertilization') {
          parameters[param] = (Math.random() * 200 + 100).toFixed(0) + ' kg/ha';
        }
      });
      
      return {
        services: serviceValues,
        parameters
      };
    }
  }
};
</script>
<style lang="scss" scoped>
.dashboard-container {
  position: relative;
  width: 100%;
  height: calc(100vh - 100px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
  padding: 15px 20px;
  background-color: white;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  
  .title-box {
      display: flex;
      align-items: center;
    
    h2 {
      margin: 0;
      font-size: 20px;
      color: #303133;
    }
    
    .region-info {
      margin-left: 15px;
      padding: 5px 10px;
      background-color: #ecf5ff;
      border-radius: 4px;
      
      span {
        color: #409EFF;
        font-weight: bold;
      }
    }
  }
  
  .actions-box {
    display: flex;
    gap: 10px;
  }
}

.main-content {
  flex: 1;
  padding: 15px;
  overflow: auto;
  
  .main-steps-card {
    margin-bottom: 15px;
    padding-bottom: 20px;
    }
  }
  
/* 步骤样式 */
.step-content {
  padding: 20px 15px;
  min-height: 400px;
  
  .step-title {
    margin-top: 0;
    margin-bottom: 15px;
    color: #303133;
    font-weight: 500;
    }
    
  .guide-text {
    color: #606266;
    margin-bottom: 20px;
    }
  }
  
.algorithm-info {
  background-color: #f8f9fa;
  border-radius: 4px;
  padding: 15px;
  height: 100%;
  
  h4 {
    margin-top: 0;
    margin-bottom: 12px;
    color: #409EFF;
  }
  
  p {
    line-height: 1.6;
  }
}

/* 服务选择 */
.service-selection {
  margin: 15px 0;
  
  .selection-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 10px;
    
    .subtitle {
      font-weight: bold;
      color: #303133;
    }
    
    .guide-text {
      font-size: 12px;
      color: #909399;
    }
  }
  
  .service-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    padding: 10px;
    border-radius: 4px;
    background-color: #f5f7fa;
    transition: all 0.3s;
    
    &:hover {
      background-color: #ecf5ff;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
    }
    
    .el-checkbox {
      margin-right: 0;
    }
    
    .el-select {
      width: 100px;
    }
    }
  }
  
/* 约束条件 */
  .constraint-item {
    display: flex;
    flex-direction: column;
  margin-bottom: 20px;
  padding: 15px;
    border-radius: 6px;
    background-color: #f9fafc;
    border: 1px solid #ebeef5;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  
  &:hover {
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }
  
  .constraint-header {
    display: flex;
    align-items: center;
    margin-bottom: 15px;
    padding-left: 10px;
    border-left: 4px solid #409EFF;
    
    .constraint-name {
      color: #303133;
      font-weight: 500;
      font-size: 15px;
    }
    
    .constraint-direction {
      margin-left: 8px;
      font-size: 12px;
      color: #909399;
    }
    }
    
    .constraint-range {
      display: flex;
      align-items: center;
      padding: 0 5px;
      
      .constraint-value {
        width: 40px;
        text-align: center;
        font-size: 13px;
        color: #606266;
        font-weight: 500;
      }
      
      .constraint-slider {
        flex: 1;
        margin: 0 8px;
      }
  }
}

/* 参数设置 */
.parameter-group, .algorithm-parameters {
  background-color: #f9fafc;
  border-radius: 6px;
  padding: 15px;
  margin-bottom: 20px;
  
  h4 {
    margin-top: 0;
    margin-bottom: 15px;
    color: #409EFF;
    font-weight: 500;
  }
}

.parameter-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  
  span {
    color: #303133;
    font-weight: 500;
  }
}

/* 确认面板 */
.confirmation-panel {
  background-color: #f9fafc;
  border-radius: 6px;
  padding: 20px;
  
  .confirmation-section {
    margin-bottom: 20px;
    
    h4 {
      margin-top: 0;
      margin-bottom: 15px;
      color: #409EFF;
      font-weight: 500;
    }
  }
  
  .confirmation-list {
    padding-left: 20px;
    margin: 0;
    
    li {
      margin-bottom: 10px;
      color: #606266;
      
      strong {
        color: #303133;
      }
    }
    
    .constraint-summary {
      margin: 5px 0 5px 20px;
      color: #606266;
    }
  }
  
  .optimization-progress {
    margin: 20px 0;
    
    h4 {
      margin-bottom: 15px;
      color: #409EFF;
    }
  
  .progress-details {
    margin-top: 15px;
      display: flex;
      flex-wrap: wrap;
      gap: 15px;
    
    .progress-item {
        flex: 1;
        min-width: 200px;
        display: flex;
      justify-content: space-between;
        align-items: center;
        padding: 10px 15px;
      border-radius: 4px;
      background-color: #f5f7fa;
      
      span:first-child {
        color: #606266;
      }
      
      span:last-child {
        font-weight: bold;
        color: #409EFF;
      }
    }
  }
}

  .action-buttons {
  display: flex;
  justify-content: center;
    gap: 15px;
    margin-top: 20px;
  }
}

/* 可视化步骤特殊样式 */
.visualization-step-content {
  min-height: calc(100vh - 250px);
  height: auto; /* 使用自动高度而不是固定高度 */
  display: flex;
  flex-direction: column;
  overflow: visible; /* 允许内容溢出 */
  padding-bottom: 30px; /* 增加底部内边距 */
}
    
/* 可视化面板 */
.visualization-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 500px;
  overflow: hidden;
  
  .view-controls {
    display: flex;
    justify-content: center;
    margin-bottom: 15px;
  }
  
  .chart-wrapper {
    flex: 1;
    position: relative;
    width: 100%;
    min-height: 500px; /* 增加最小高度 */
    height: calc(100% - 20px); /* 减少一点高度，确保不会溢出 */
    overflow: visible; /* 改为visible，允许内容溢出，防止被裁剪 */
    margin: 0 auto 20px; /* 居中显示并增加底部margin */
    
    .chart-container {
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background-color: #f9fafc;
      border-radius: 8px;
      padding: 15px;
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
      
      .chart {
  width: 100%;
  height: 100%;
        min-height: 400px;
      }
    }
  }
  
  .chart-description {
    background-color: #f9fafc;
    border-radius: 6px;
    padding: 15px;
    
    h4 {
      margin-top: 0;
      margin-bottom: 10px;
      color: #409EFF;
    }
    
    p {
      color: #606266;
      line-height: 1.6;
      margin-bottom: 0;
    }
    
    .no-data-tip {
      color: #f56c6c;
      text-align: center;
      margin-top: 15px;
    }
  }
}

/* 解决方案面板 */
.solutions-panel {
  .comparison-chart-container {
    min-height: 300px; /* 改为min-height而不是固定height */
    margin-bottom: 20px;
    overflow: visible;
    
    .chart {
      width: 100%;
      height: 100%;
      min-height: 280px; /* 确保最小高度 */
    }
  }
  
  .chart-type-tip {
    margin-bottom: 15px;
    padding: 8px 12px;
    background-color: #f0f9eb;
    border-radius: 4px;
    color: #67c23a;
    font-size: 12px;
    line-height: 1.4;
    display: flex;
    align-items: center;
    
    i {
      margin-right: 5px;
      font-size: 14px;
    }
  }
  
  h4 {
    .chart-type-label {
      font-size: 12px;
      font-weight: normal;
      color: #909399;
    }
  }
  
  .selected-solution-details {
    background-color: #f9fafc;
    border-radius: 6px;
    padding: 15px;
    
    h4 {
      margin-top: 0;
      margin-bottom: 15px;
      color: #409EFF;
    }
    
    .solution-detail-item {
      margin-bottom: 12px;
      
      .service-name {
        font-size: 13px;
        color: #606266;
        margin-bottom: 5px;
        display: block;
      }
      
      .value-bar-container {
        display: flex;
        align-items: center;
        height: 18px;
        background-color: #f5f7fa;
        border-radius: 9px;
        overflow: hidden;
        position: relative;
        
        .value-bar {
          height: 100%;
          border-radius: 9px;
        }
        
        .value-text {
          position: absolute;
          margin-left: 10px;
          font-size: 12px;
          color: #fff;
          font-weight: bold;
          text-shadow: 0 0 2px rgba(0, 0, 0, 0.5);
      }
    }
  }
}

  .action-buttons {
  display: flex;
    justify-content: center;
    margin-top: 20px;
  }
}

/* 详情面板 */
.details-panel {
  .parameter-details {
    background-color: #f9fafc;
    border-radius: 6px;
    padding: 15px;
    height: 100%;
    
    h4 {
      margin-top: 0;
      margin-bottom: 15px;
      color: #409EFF;
}

.parameters-table {
  width: 100%;
  
  table {
    width: 100%;
    border-collapse: collapse;
    
    th, td {
      padding: 8px;
      border: 1px solid #ebeef5;
      text-align: left;
    }
    
    th {
      background-color: #f5f7fa;
      font-weight: bold;
      color: #303133;
    }
    
    td:first-child {
      width: 40%;
      color: #606266;
    }
    
    td:last-child {
      color: #303133;
    }
  }
    }
  }
  
  .spatial-distribution {
    h4 {
      margin-top: 0;
      margin-bottom: 15px;
      color: #409EFF;
}

.map-container {
      height: 350px;
      margin-bottom: 15px;
  
  .solution-map {
    width: 100%;
    height: 100%;
    background-color: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
    border-radius: 4px;
    
    .map-info {
      padding: 20px;
      text-align: center;
      
      h3 {
        margin-bottom: 10px;
      }
      
      ul {
        text-align: left;
        display: inline-block;
      }
    }
  }
}

.map-legend {
  display: flex;
  flex-wrap: wrap;
      gap: 10px;
  margin-top: 10px;
  
  .legend-item {
    display: flex;
    align-items: center;
    
    .legend-color {
      width: 16px;
      height: 16px;
      border-radius: 3px;
      margin-right: 5px;
    }
    
    .legend-name {
      font-size: 12px;
      color: #606266;
    }
  }
}
  }
  
  .tradeoff-section {
    margin-top: 20px;
    background-color: #f9fafc;
    border-radius: 6px;
    padding: 15px;
    
    h4 {
      margin-top: 0;
      margin-bottom: 10px;
      color: #409EFF;
}

    .tradeoff-controls {
      display: flex;
      justify-content: center;
      margin: 15px 0;
    }
    
    .tradeoff-progress {
      margin-top: 15px;
      
      .tradeoff-status {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 8px;
        
        span {
          font-size: 14px;
          color: #606266;
        }
      }
    }
  }
}

/* 步骤导航 */
.step-navigation {
  display: flex;
  justify-content: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  gap: 15px;
}

/* 服务列表值 */
.service-value {
  display: flex;
  align-items: center;
  justify-content: space-between;
  
  i {
    margin-left: 5px;
  }
}

/* 无数据状态 */
.no-solutions, .no-solution-selected, .no-data, .no-services-selected {
      display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 300px;
  color: #909399;
  
  i {
    font-size: 36px;
    margin-bottom: 10px;
}

  p {
    margin: 5px 0;
    
    &.help-text {
      font-size: 12px;
      color: #c0c4cc;
    }
  }
}

/* 表格选中行样式 */
.selected-row {
  background-color: #ecf5ff;
}

/* 响应式调整 */
@media screen and (max-width: 1200px) {
  .step-content {
    padding: 15px 10px;
  }
  
  .constraint-item, .service-item {
    padding: 10px;
}

  .parameter-group, .algorithm-parameters, .confirmation-panel {
    padding: 15px;
}
}
</style>

<!-- Global styles for Element UI components -->
<style lang="scss">
/* Override El-Steps style */
.main-steps-card .el-steps {
  padding: 20px 0;
}

/* 滑块样式 */
.constraint-slider .el-slider__runway {
  margin: 10px 0;
}

.constraint-slider .el-slider__bar {
  background-color: #67c23a;
  height: 6px;
}

.constraint-slider .el-slider__button {
  width: 16px;
  height: 16px;
  border: 2px solid #67c23a;
}

/* 进度条样式 */
.tradeoff-progress .el-progress-bar__outer {
  border-radius: 4px;
  height: 10px;
  background-color: #ebeef5;
}

.tradeoff-progress .el-progress-bar__inner {
  border-radius: 4px;
  transition: width 0.3s ease;
}

.tradeoff-progress .el-progress__text {
  font-size: 14px !important;
  font-weight: bold;
}

/* 弹窗样式 */
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

/* 表格选中行样式 */
.el-table .selected-row {
  background-color: #ecf5ff;
}

/* 确保图表容器可见 */
.chart-container {
  min-height: 450px; /* 增加最小高度 */
  margin-bottom: 20px; /* 添加底部margin */
  overflow: visible; /* Allow chart to overflow container if needed */
}

.chart {
  width: 100% !important;
  height: 100% !important;
  min-height: 450px !important; /* 增加最小高度 */
  overflow: visible !important; /* Allow chart elements to be fully visible */
}

.no-chart-selected {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 480px;
  background-color: #f9fafc;
  border-radius: 4px;
  
  i {
    font-size: 48px;
    color: #909399;
    margin-bottom: 20px;
  }
  
  p {
    color: #606266;
    font-size: 16px;
    margin: 6px 0;
  }
  
  .sub-hint {
    color: #909399;
    font-size: 14px;
  }
}
</style>
</style>