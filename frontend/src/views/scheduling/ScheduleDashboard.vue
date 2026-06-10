<template>
  <div class="schedule-dashboard">
    <el-card class="breadcrumb-card">
      <div class="header-top">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/scheduling' }">智能调度</el-breadcrumb-item>
          <el-breadcrumb-item>调度仪表盘</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="refresh-controls">
          <el-select v-model="refreshInterval" size="small" style="width: 140px; margin-right: 12px">
            <el-option label="关闭自动刷新" :value="0" />
            <el-option label="30秒刷新" :value="30000" />
            <el-option label="1分钟刷新" :value="60000" />
            <el-option label="5分钟刷新" :value="300000" />
          </el-select>
          <el-button type="primary" size="small" :loading="loading" @click="loadDashboardData">
            <el-icon><Refresh /></el-icon>
            刷新数据
          </el-button>
        </div>
      </div>
      <h2 class="page-title">调度仪表盘与统计报表</h2>
    </el-card>

    <el-row :gutter="20" class="stats-cards">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card blue">
          <div class="stat-icon">
            <el-icon :size="40"><Calendar /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-title">总场次数</div>
            <div class="stat-value">{{ dashboardData.totalSessions || 0 }}</div>
            <div class="stat-desc">全部面试场次</div>
          </div>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card green">
          <div class="stat-icon">
            <el-icon :size="40"><CircleCheck /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-title">已完成场次</div>
            <div class="stat-value">{{ dashboardData.completedSessions || 0 }}</div>
            <div class="stat-desc">
              占比 {{ totalSessions > 0 ? ((dashboardData.completedSessions || 0) / totalSessions * 100).toFixed(1) : 0 }}%
            </div>
          </div>
          <el-progress
            class="stat-progress"
            :percentage="totalSessions > 0 ? Math.round((dashboardData.completedSessions || 0) / totalSessions * 100) : 0"
            :stroke-width="6"
            :show-text="false"
            color="#67c23a" />
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card orange">
          <div class="stat-icon">
            <el-icon :size="40"><VideoPlay /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-title">进行中场次</div>
            <div class="stat-value">{{ dashboardData.ongoingSessions || 0 }}</div>
            <div class="stat-desc">
              占比 {{ totalSessions > 0 ? ((dashboardData.ongoingSessions || 0) / totalSessions * 100).toFixed(1) : 0 }}%
            </div>
          </div>
          <el-progress
            class="stat-progress"
            :percentage="totalSessions > 0 ? Math.round((dashboardData.ongoingSessions || 0) / totalSessions * 100) : 0"
            :stroke-width="6"
            :show-text="false"
            color="#e6a23c" />
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :md="6">
        <div class="stat-card gray">
          <div class="stat-icon">
            <el-icon :size="40"><Clock /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-title">待开始场次</div>
            <div class="stat-value">{{ dashboardData.pendingSessions || 0 }}</div>
            <div class="stat-desc">
              占比 {{ totalSessions > 0 ? ((dashboardData.pendingSessions || 0) / totalSessions * 100).toFixed(1) : 0 }}%
            </div>
          </div>
          <el-progress
            class="stat-progress"
            :percentage="totalSessions > 0 ? Math.round((dashboardData.pendingSessions || 0) / totalSessions * 100) : 0"
            :stroke-width="6"
            :show-text="false"
            color="#909399" />
        </div>
      </el-col>
    </el-row>

    <el-card class="main-tabs-card">
      <el-tabs v-model="activeTab" type="border-card" class="main-tabs">
        <el-tab-pane label="执行进度" name="progress">
          <el-row :gutter="20">
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">场次执行时间轴</h3>
                <div class="timeline-container">
                  <el-timeline>
                    <el-timeline-item
                      v-for="(item, index) in timelineData"
                      :key="index"
                      :timestamp="formatDateTime(item.startTime) + ' - ' + formatDateTime(item.endTime)"
                      :type="getStatusType(item.status)"
                      :color="getStatusColor(item.status)">
                      <div class="timeline-content">
                        <div class="timeline-header">
                          <span class="session-name">{{ item.sessionName }}</span>
                          <el-tag :type="getStatusType(item.status)" size="small">
                            {{ getStatusText(item.status) }}
                          </el-tag>
                        </div>
                        <el-progress
                          :percentage="item.progress || 0"
                          :stroke-width="8"
                          :color="getStatusColor(item.status)" />
                      </div>
                    </el-timeline-item>
                  </el-timeline>
                </div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">场次状态分布</h3>
                <div ref="pieChartRef" class="chart"></div>
              </div>
            </el-col>
          </el-row>
          <div class="chart-section">
            <h3 class="section-title">场次状态列表</h3>
            <el-table :data="timelineData" border stripe style="width: 100%">
              <el-table-column prop="sessionName" label="场次名称" min-width="150" />
              <el-table-column label="时间" min-width="320">
                <template #default="{ row }">
                  {{ formatDateTime(row.startTime) }} - {{ formatDateTime(row.endTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="roomName" label="考场" width="120" />
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusType(row.status)" size="small">
                    {{ getStatusText(row.status) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="进度" width="180">
                <template #default="{ row }">
                  <el-progress
                    :percentage="row.progress || 0"
                    :stroke-width="8"
                    :color="getStatusColor(row.status)" />
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>

        <el-tab-pane label="资源统计" name="resources">
          <el-row :gutter="20">
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">考官负荷排行</h3>
                <div ref="workloadChartRef" class="chart"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">考场使用率</h3>
                <div ref="roomUsageChartRef" class="chart"></div>
              </div>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">考官统计</h3>
                <el-table :data="dashboardData.examinerWorkloads || []" border stripe style="width: 100%">
                  <el-table-column prop="examinerName" label="姓名" width="120" />
                  <el-table-column prop="sessionCount" label="参与场次数" width="120" align="center" />
                  <el-table-column prop="candidateCount" label="评审考生数" width="120" align="center" />
                  <el-table-column label="平均每场时长" width="140" align="center">
                    <template #default="{ row }">
                      {{ row.sessionCount > 0 ? (row.totalHours / row.sessionCount).toFixed(1) : 0 }} 小时
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">考场统计</h3>
                <el-table :data="dashboardData.roomUsages || []" border stripe style="width: 100%">
                  <el-table-column prop="roomName" label="考场名称" width="120" />
                  <el-table-column prop="usageCount" label="使用次数" width="100" align="center" />
                  <el-table-column prop="totalHours" label="总时长(小时)" width="120" align="center" />
                  <el-table-column label="使用率" width="140">
                    <template #default="{ row }">
                      <el-progress
                        :percentage="row.utilizationRate || 0"
                        :stroke-width="8"
                        color="#409eff" />
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-col>
          </el-row>
        </el-tab-pane>

        <el-tab-pane label="报表分析" name="report">
          <div class="report-actions">
            <el-button type="primary" :loading="exporting" @click="exportExcel">
              <el-icon><Download /></el-icon>
              导出Excel
            </el-button>
            <el-button type="success" :loading="generating" @click="generateReport">
              <el-icon><Document /></el-icon>
              生成完整报告
            </el-button>
            <el-select
              v-model="selectedPlan1"
              placeholder="选择方案1"
              size="small"
              style="width: 180px; margin-left: auto">
              <el-option
                v-for="plan in plansList"
                :key="plan.id"
                :label="plan.planName"
                :value="plan.id" />
            </el-select>
            <span style="margin: 0 8px; color: #909399">VS</span>
            <el-select
              v-model="selectedPlan2"
              placeholder="选择方案2"
              size="small"
              style="width: 180px">
              <el-option
                v-for="plan in plansList"
                :key="plan.id"
                :label="plan.planName"
                :value="plan.id" />
            </el-select>
            <el-button
              type="warning"
              size="small"
              :disabled="!selectedPlan1 || !selectedPlan2"
              @click="comparePlans">
              对比分析
            </el-button>
          </div>
          <el-row :gutter="20">
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">调度方案质量评分</h3>
                <div ref="radarChartRef" class="chart"></div>
              </div>
            </el-col>
            <el-col :xs="24" :lg="12">
              <div class="chart-section">
                <h3 class="section-title">不同策略效果对比</h3>
                <div ref="compareChartRef" class="chart"></div>
              </div>
            </el-col>
          </el-row>
          <div class="chart-section">
            <h3 class="section-title">报告概要</h3>
            <el-descriptions :column="2" border class="report-summary">
              <el-descriptions-item label="总场次数">
                <span class="summary-value">{{ dashboardData.totalSessions || 0 }}</span> 场
              </el-descriptions-item>
              <el-descriptions-item label="已完成">
                <span class="summary-value success">{{ dashboardData.completedSessions || 0 }}</span> 场
              </el-descriptions-item>
              <el-descriptions-item label="进行中">
                <span class="summary-value warning">{{ dashboardData.ongoingSessions || 0 }}</span> 场
              </el-descriptions-item>
              <el-descriptions-item label="待开始">
                <span class="summary-value info">{{ dashboardData.pendingSessions || 0 }}</span> 场
              </el-descriptions-item>
              <el-descriptions-item label="各考场使用频次" :span="2">
                <div class="usage-tags">
                  <el-tag
                    v-for="room in dashboardData.roomUsages || []"
                    :key="room.roomId"
                    type="info"
                    size="small"
                    class="usage-tag">
                    {{ room.roomName }}: {{ room.usageCount }}次
                  </el-tag>
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="考官工作时长统计" :span="2">
                <div class="workload-summary">
                  <div
                    v-for="examiner in topExaminers"
                    :key="examiner.examinerId"
                    class="workload-item">
                    <span class="examiner-name">{{ examiner.examinerName }}</span>
                    <el-progress
                      :percentage="Math.min(Math.round(examiner.totalHours / maxWorkloadHours * 100), 100)"
                      :stroke-width="12"
                      style="flex: 1; margin: 0 16px" />
                    <span class="hours-text">{{ examiner.totalHours.toFixed(1) }} 小时</span>
                  </div>
                </div>
              </el-descriptions-item>
              <el-descriptions-item label="综合评分" :span="2">
                <div class="quality-scores">
                  <div class="score-item">
                    <div class="score-label">综合评分</div>
                    <div class="score-value">{{ dashboardData.planQuality?.overallScore || 0 }}</div>
                    <el-progress
                      :percentage="dashboardData.planQuality?.overallScore || 0"
                      :stroke-width="10"
                      color="#409eff" />
                  </div>
                  <div class="score-item">
                    <div class="score-label">负荷均衡</div>
                    <div class="score-value">{{ dashboardData.planQuality?.workloadBalanceScore || 0 }}</div>
                    <el-progress
                      :percentage="dashboardData.planQuality?.workloadBalanceScore || 0"
                      :stroke-width="10"
                      color="#67c23a" />
                  </div>
                  <div class="score-item">
                    <div class="score-label">考场利用</div>
                    <div class="score-value">{{ dashboardData.planQuality?.roomUtilizationScore || 0 }}</div>
                    <el-progress
                      :percentage="dashboardData.planQuality?.roomUtilizationScore || 0"
                      :stroke-width="10"
                      color="#e6a23c" />
                  </div>
                  <div class="score-item">
                    <div class="score-label">时间效率</div>
                    <div class="score-value">{{ dashboardData.planQuality?.timeEfficiencyScore || 0 }}</div>
                    <el-progress
                      :percentage="dashboardData.planQuality?.timeEfficiencyScore || 0"
                      :stroke-width="10"
                      color="#909399" />
                  </div>
                  <div class="score-item">
                    <div class="score-label">冲突率</div>
                    <div class="score-value">{{ dashboardData.planQuality?.conflictRateScore || 0 }}</div>
                    <el-progress
                      :percentage="dashboardData.planQuality?.conflictRateScore || 0"
                      :stroke-width="10"
                      color="#f56c6c" />
                  </div>
                </div>
              </el-descriptions-item>
            </el-descriptions>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Refresh, Calendar, CircleCheck, VideoPlay, Clock, Download, Document
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import api from '@/api'

const route = useRoute()
const router = useRouter()

const projectId = ref(route.params.projectId)
const loading = ref(false)
const exporting = ref(false)
const generating = ref(false)
const activeTab = ref('progress')
const refreshInterval = ref(0)

const dashboardData = reactive({
  totalSessions: 0,
  completedSessions: 0,
  ongoingSessions: 0,
  pendingSessions: 0,
  sessionStatusTimeline: [],
  examinerWorkloads: [],
  roomUsages: [],
  planQuality: {
    overallScore: 0,
    workloadBalanceScore: 0,
    roomUtilizationScore: 0,
    timeEfficiencyScore: 0,
    conflictRateScore: 0
  }
})

const plansList = ref([])
const selectedPlan1 = ref(null)
const selectedPlan2 = ref(null)
const compareData = ref(null)

const pieChartRef = ref(null)
const workloadChartRef = ref(null)
const roomUsageChartRef = ref(null)
const radarChartRef = ref(null)
const compareChartRef = ref(null)

let pieChart = null
let workloadChart = null
let roomUsageChart = null
let radarChart = null
let compareChart = null
let refreshTimer = null

const totalSessions = computed(() => {
  return dashboardData.totalSessions || 0
})

const timelineData = computed(() => {
  return dashboardData.sessionStatusTimeline || []
})

const topExaminers = computed(() => {
  const list = [...(dashboardData.examinerWorkloads || [])]
  return list.sort((a, b) => b.totalHours - a.totalHours).slice(0, 5)
})

const maxWorkloadHours = computed(() => {
  const max = Math.max(...topExaminers.value.map(e => e.totalHours), 1)
  return max
})

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  return dayjs(datetime).format('MM-DD HH:mm')
}

const getStatusType = (status) => {
  const types = {
    completed: 'success',
    ongoing: 'warning',
    pending: 'info'
  }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = {
    completed: '已完成',
    ongoing: '进行中',
    pending: '待开始'
  }
  return texts[status] || '未知'
}

const getStatusColor = (status) => {
  const colors = {
    completed: '#67c23a',
    ongoing: '#e6a23c',
    pending: '#909399'
  }
  return colors[status] || '#909399'
}

const loadDashboardData = async () => {
  loading.value = true
  try {
    const res = await api.report.getDashboard(projectId.value)
    const data = res.data || res
    Object.assign(dashboardData, data)
    await nextTick()
    initCharts()
  } catch (e) {
    ElMessage.error('加载仪表盘数据失败')
  } finally {
    loading.value = false
  }
}

const loadPlans = async () => {
  try {
    const res = await api.scheduling.listPlans(projectId.value)
    plansList.value = res.data?.records || res.data || []
  } catch (e) {
    console.error('加载方案列表失败', e)
  }
}

const exportExcel = async () => {
  exporting.value = true
  try {
    const blob = await api.report.exportExcel(projectId.value)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = `调度报表_${dayjs().format('YYYYMMDD_HHmmss')}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const generateReport = async () => {
  generating.value = true
  try {
    const res = await api.report.generate(projectId.value)
    if (res.success || res.data) {
      ElMessage.success('报告生成成功')
      loadDashboardData()
    } else {
      ElMessage.error(res.message || '报告生成失败')
    }
  } catch (e) {
    ElMessage.error('报告生成失败')
  } finally {
    generating.value = false
  }
}

const comparePlans = async () => {
  if (!selectedPlan1.value || !selectedPlan2.value) return
  try {
    const res = await api.report.comparePlans(selectedPlan1.value, selectedPlan2.value)
    compareData.value = res.data || res
    initCompareChart()
  } catch (e) {
    ElMessage.error('对比失败')
  }
}

const initCharts = () => {
  initPieChart()
  initWorkloadChart()
  initRoomUsageChart()
  initRadarChart()
  initCompareChart()
}

const initPieChart = () => {
  if (!pieChartRef.value) return
  if (pieChart) {
    pieChart.dispose()
  }
  pieChart = echarts.init(pieChartRef.value)

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} 场 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '10%',
      top: 'center'
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 10,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: false,
          position: 'center'
        },
        emphasis: {
          label: {
            show: true,
            fontSize: 20,
            fontWeight: 'bold'
          }
        },
        labelLine: {
          show: false
        },
        data: [
          { value: dashboardData.completedSessions || 0, name: '已完成', itemStyle: { color: '#67c23a' } },
          { value: dashboardData.ongoingSessions || 0, name: '进行中', itemStyle: { color: '#e6a23c' } },
          { value: dashboardData.pendingSessions || 0, name: '待开始', itemStyle: { color: '#909399' } }
        ]
      }
    ]
  }

  pieChart.setOption(option)
}

const initWorkloadChart = () => {
  if (!workloadChartRef.value) return
  if (workloadChart) {
    workloadChart.dispose()
  }
  workloadChart = echarts.init(workloadChartRef.value)

  const examiners = (dashboardData.examinerWorkloads || []).map(e => e.examinerName)
  const workloads = (dashboardData.examinerWorkloads || []).map(e => e.totalHours)

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      },
      formatter: (params) => {
        const data = params[0]
        return `
          <div>考官: ${data.name}</div>
          <div>总时长: ${data.value} 小时</div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: examiners,
      axisLabel: {
        rotate: 45,
        interval: 0,
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: '时长(小时)'
    },
    series: [{
      type: 'bar',
      data: workloads,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}h',
        fontSize: 11
      },
      barWidth: '50%'
    }]
  }

  workloadChart.setOption(option)
}

const initRoomUsageChart = () => {
  if (!roomUsageChartRef.value) return
  if (roomUsageChart) {
    roomUsageChart.dispose()
  }
  roomUsageChart = echarts.init(roomUsageChartRef.value)

  const colors = ['#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4']

  const data = (dashboardData.roomUsages || []).map((room, index) => ({
    value: room.usageCount,
    name: room.roomName,
    itemStyle: { color: colors[index % colors.length] }
  }))

  const option = {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}次 ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      itemWidth: 12,
      itemHeight: 12
    },
    series: [
      {
        type: 'pie',
        radius: ['45%', '75%'],
        center: ['35%', '50%'],
        itemStyle: {
          borderRadius: 6,
          borderColor: '#fff',
          borderWidth: 2
        },
        label: {
          show: true,
          formatter: '{b}\n{d}%',
          fontSize: 11
        },
        labelLine: {
          show: true,
          length: 10,
          length2: 10
        },
        data: data
      }
    ]
  }

  roomUsageChart.setOption(option)
}

const initRadarChart = () => {
  if (!radarChartRef.value) return
  if (radarChart) {
    radarChart.dispose()
  }
  radarChart = echarts.init(radarChartRef.value)

  const quality = dashboardData.planQuality || {}

  const option = {
    tooltip: {
      trigger: 'item'
    },
    radar: {
      indicator: [
        { name: '综合评分', max: 100 },
        { name: '负荷均衡', max: 100 },
        { name: '考场利用', max: 100 },
        { name: '时间效率', max: 100 },
        { name: '冲突率', max: 100 }
      ],
      shape: 'polygon',
      splitNumber: 4,
      axisName: {
        color: '#333',
        fontSize: 12
      },
      splitLine: {
        lineStyle: {
          color: ['#e4e7ed', '#dcdfe6', '#c0c4cc', '#909399']
        }
      },
      splitArea: {
        show: true,
        areaStyle: {
          color: ['rgba(64, 158, 255, 0.05)', 'rgba(64, 158, 255, 0.1)']
        }
      }
    },
    series: [{
      type: 'radar',
      data: [
        {
          value: [
            quality.overallScore || 0,
            quality.workloadBalanceScore || 0,
            quality.roomUtilizationScore || 0,
            quality.timeEfficiencyScore || 0,
            quality.conflictRateScore || 0
          ],
          name: '当前方案',
          areaStyle: {
            color: 'rgba(64, 158, 255, 0.3)'
          },
          lineStyle: {
            color: '#409eff',
            width: 2
          },
          itemStyle: {
            color: '#409eff'
          }
        }
      ]
    }]
  }

  radarChart.setOption(option)
}

const initCompareChart = () => {
  if (!compareChartRef.value) return
  if (compareChart) {
    compareChart.dispose()
  }
  compareChart = echarts.init(compareChartRef.value)

  let seriesData = []
  if (compareData.value) {
    seriesData = [
      {
        name: '方案1',
        type: 'bar',
        data: [
          compareData.value.plan1?.overallScore || 0,
          compareData.value.plan1?.workloadBalanceScore || 0,
          compareData.value.plan1?.roomUtilizationScore || 0,
          compareData.value.plan1?.timeEfficiencyScore || 0,
          compareData.value.plan1?.conflictRateScore || 0
        ],
        itemStyle: {
          color: '#409eff',
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '35%'
      },
      {
        name: '方案2',
        type: 'bar',
        data: [
          compareData.value.plan2?.overallScore || 0,
          compareData.value.plan2?.workloadBalanceScore || 0,
          compareData.value.plan2?.roomUtilizationScore || 0,
          compareData.value.plan2?.timeEfficiencyScore || 0,
          compareData.value.plan2?.conflictRateScore || 0
        ],
        itemStyle: {
          color: '#67c23a',
          borderRadius: [4, 4, 0, 0]
        },
        barWidth: '35%'
      }
    ]
  } else {
    const quality = dashboardData.planQuality || {}
    seriesData = [{
      name: '当前方案',
      type: 'bar',
      data: [
        quality.overallScore || 0,
        quality.workloadBalanceScore || 0,
        quality.roomUtilizationScore || 0,
        quality.timeEfficiencyScore || 0,
        quality.conflictRateScore || 0
      ],
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ]),
        borderRadius: [4, 4, 0, 0]
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}'
      },
      barWidth: '50%'
    }]
  }

  const option = {
    tooltip: {
      trigger: 'axis',
      axisPointer: {
        type: 'shadow'
      }
    },
    legend: {
      data: compareData.value ? ['方案1', '方案2'] : ['当前方案'],
      top: 0
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      top: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: ['综合评分', '负荷均衡', '考场利用', '时间效率', '冲突率'],
      axisLabel: {
        fontSize: 11
      }
    },
    yAxis: {
      type: 'value',
      name: '评分',
      max: 100
    },
    series: seriesData
  }

  compareChart.setOption(option)
}

const handleResize = () => {
  pieChart?.resize()
  workloadChart?.resize()
  roomUsageChart?.resize()
  radarChart?.resize()
  compareChart?.resize()
}

watch(refreshInterval, (val) => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
  if (val > 0) {
    refreshTimer = setInterval(() => {
      loadDashboardData()
    }, val)
  }
})

watch(activeTab, (val) => {
  nextTick(() => {
    if (val === 'progress') {
      pieChart?.resize()
    } else if (val === 'resources') {
      workloadChart?.resize()
      roomUsageChart?.resize()
    } else if (val === 'report') {
      radarChart?.resize()
      compareChart?.resize()
    }
  })
})

onMounted(() => {
  loadDashboardData()
  loadPlans()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (refreshTimer) {
    clearInterval(refreshTimer)
  }
  pieChart?.dispose()
  workloadChart?.dispose()
  roomUsageChart?.dispose()
  radarChart?.dispose()
  compareChart?.dispose()
})
</script>

<style scoped lang="scss">
.schedule-dashboard {
  padding: 20px;

  .breadcrumb-card {
    margin-bottom: 20px;

    .header-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 12px;

      .refresh-controls {
        display: flex;
        align-items: center;
      }
    }

    .page-title {
      margin: 0;
      font-size: 20px;
      font-weight: bold;
      color: #303133;
    }
  }

  .stats-cards {
    margin-bottom: 20px;

    .stat-card {
      padding: 20px;
      border-radius: 12px;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 16px;
      position: relative;
      overflow: hidden;
      min-height: 100px;

      &.blue {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      }

      &.green {
        background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
      }

      &.orange {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
      }

      &.gray {
        background: linear-gradient(135deg, #8e9eab 0%, #eef2f3 100%);
        color: #606266;

        .stat-title,
        .stat-desc {
          color: #909399;
        }

        .stat-value {
          color: #606266;
        }
      }

      .stat-icon {
        flex-shrink: 0;
        opacity: 0.9;
      }

      .stat-info {
        flex: 1;
        min-width: 0;

        .stat-title {
          font-size: 13px;
          opacity: 0.9;
          margin-bottom: 4px;
        }

        .stat-value {
          font-size: 28px;
          font-weight: 600;
          line-height: 1.2;
        }

        .stat-desc {
          font-size: 11px;
          opacity: 0.8;
          margin-top: 4px;
        }
      }

      .stat-progress {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        height: 4px;

        :deep(.el-progress-bar) {
          padding-right: 0;
        }
      }
    }
  }

  .main-tabs-card {
    .main-tabs {
      :deep(.el-tabs__header) {
        margin: 0;
      }
    }
  }

  .chart-section {
    margin-bottom: 24px;

    &:last-child {
      margin-bottom: 0;
    }

    .section-title {
      font-size: 16px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 16px 0;
      padding-left: 12px;
      border-left: 4px solid #409eff;
    }

    .chart {
      height: 350px;
      width: 100%;
    }

    .timeline-container {
      max-height: 350px;
      overflow-y: auto;
      padding-right: 10px;

      .timeline-content {
        .timeline-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 8px;

          .session-name {
            font-weight: 500;
            color: #303133;
          }
        }
      }
    }
  }

  .report-actions {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;
  }

  .report-summary {
    .summary-value {
      font-size: 20px;
      font-weight: 600;
      color: #409eff;

      &.success {
        color: #67c23a;
      }

      &.warning {
        color: #e6a23c;
      }

      &.info {
        color: #909399;
      }
    }

    .usage-tags {
      display: flex;
      flex-wrap: wrap;
      gap: 8px;

      .usage-tag {
        margin: 2px 0;
      }
    }

    .workload-summary {
      .workload-item {
        display: flex;
        align-items: center;
        margin-bottom: 12px;

        &:last-child {
          margin-bottom: 0;
        }

        .examiner-name {
          width: 80px;
          font-weight: 500;
          color: #606266;
          flex-shrink: 0;
        }

        .hours-text {
          width: 80px;
          text-align: right;
          color: #909399;
          font-size: 13px;
          flex-shrink: 0;
        }
      }
    }

    .quality-scores {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
      gap: 20px;

      .score-item {
        text-align: center;

        .score-label {
          font-size: 13px;
          color: #606266;
          margin-bottom: 4px;
        }

        .score-value {
          font-size: 24px;
          font-weight: 600;
          color: #303133;
          margin-bottom: 8px;
        }
      }
    }
  }
}

@media screen and (max-width: 768px) {
  .schedule-dashboard {
    padding: 12px;

    .stats-cards {
      .stat-card {
        min-height: 80px;

        .stat-info {
          .stat-value {
            font-size: 22px;
          }
        }
      }
    }

    .chart-section {
      .chart {
        height: 280px;
      }
    }

    .report-summary {
      .quality-scores {
        grid-template-columns: repeat(auto-fit, minmax(100px, 1fr));
        gap: 12px;
      }
    }
  }
}
</style>
