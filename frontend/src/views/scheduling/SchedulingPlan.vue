<template>
  <div class="scheduling-plan">
    <el-card class="breadcrumb-card">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/scheduling' }">智能调度</el-breadcrumb-item>
        <el-breadcrumb-item>调度方案</el-breadcrumb-item>
      </el-breadcrumb>
      <h2 class="page-title">智能调度方案</h2>
    </el-card>

    <el-alert
      v-for="conflict in conflicts"
      :key="conflict.message"
      :title="conflict.message"
      :type="conflict.severity === 'error' ? 'error' : conflict.severity === 'warning' ? 'warning' : 'info'"
      :description="conflict.suggestion"
      show-icon
      closable
      class="conflict-alert" />

    <div class="main-content">
      <el-card class="config-panel">
        <template #header>
          <div class="panel-header">
            <el-icon><Setting /></el-icon>
            <span>调度配置</span>
          </div>
        </template>

        <el-form label-position="top" class="config-form">
          <el-form-item label="调度策略">
            <el-radio-group v-model="config.strategy">
              <el-radio :value="0">
                <div class="radio-option">
                  <span class="option-title">均衡负荷</span>
                  <span class="option-desc">优先平衡考官工作量</span>
                </div>
              </el-radio>
              <el-radio :value="1">
                <div class="radio-option">
                  <span class="option-title">压缩时间</span>
                  <span class="option-desc">优先缩短总时长</span>
                </div>
              </el-radio>
              <el-radio :value="2">
                <div class="radio-option">
                  <span class="option-title">职位优先</span>
                  <span class="option-desc">优先满足职位分配</span>
                </div>
              </el-radio>
            </el-radio-group>
          </el-form-item>

          <el-form-item label="每考场考官人数">
            <el-input-number
              v-model="config.examinersPerRoom"
              :min="1"
              :max="10"
              :step="1"
              style="width: 100%" />
          </el-form-item>

          <el-form-item label="每考生面试时长（分钟）">
            <el-input-number
              v-model="config.interviewDuration"
              :min="5"
              :max="120"
              :step="5"
              style="width: 100%" />
          </el-form-item>

          <el-form-item label="最大连续场次数">
            <el-input-number
              v-model="config.maxConsecutiveSessions"
              :min="1"
              :max="10"
              :step="1"
              style="width: 100%" />
          </el-form-item>

          <el-form-item>
            <el-checkbox v-model="config.considerHistoryWorkload">
              考虑考官历史负荷
            </el-checkbox>
          </el-form-item>

          <el-divider />

          <el-button
            type="primary"
            plain
            style="width: 100%; margin-bottom: 12px"
            :loading="validating"
            @click="validateConstraints">
            <el-icon><Check /></el-icon>
            校验约束
          </el-button>

          <el-button
            type="primary"
            style="width: 100%"
            :loading="generating"
            @click="generatePlan">
            <el-icon><MagicStick /></el-icon>
            生成调度方案
          </el-button>
        </el-form>
      </el-card>

      <div class="preview-area">
        <el-card class="preview-card">
          <template #header>
            <div class="preview-header">
              <div class="header-left">
                <el-icon><View /></el-icon>
                <span>方案预览</span>
                <el-tag v-if="currentPlan" type="success" size="small" class="plan-tag">
                  {{ currentPlan.planName }}
                </el-tag>
              </div>
              <div class="header-right">
                <el-button
                  size="small"
                  :disabled="!currentPlan"
                  @click="setExecution">
                  <el-icon><Check /></el-icon>
                  设为执行方案
                </el-button>
                <el-button
                  size="small"
                  type="primary"
                  :disabled="!currentPlan"
                  @click="applyPlan">
                  <el-icon><Check /></el-icon>
                  应用到系统
                </el-button>
                <el-button
                  size="small"
                  @click="exportExcel">
                  <el-icon><Download /></el-icon>
                  导出Excel
                </el-button>
                <el-button
                  size="small"
                  :disabled="savedPlans.length < 2"
                  @click="showCompareDialog = true">
                  <el-icon><Histogram /></el-icon>
                  对比方案
                </el-button>
              </div>
            </div>
          </template>

          <el-tabs v-model="activeTab" class="preview-tabs">
            <el-tab-pane label="方案列表" name="plans">
              <div v-if="savedPlans.length === 0" class="empty-state">
                <el-empty description="暂无保存的方案" />
              </div>
              <div v-else class="plans-list">
                <div
                  v-for="plan in savedPlans"
                  :key="plan.id"
                  class="plan-item"
                  :class="{ active: currentPlan?.id === plan.id }"
                  @click="selectPlan(plan)">
                  <div class="plan-info">
                    <div class="plan-name">
                      {{ plan.planName }}
                      <el-tag v-if="plan.isExecution" type="warning" size="small" class="exec-tag">
                        执行方案
                      </el-tag>
                    </div>
                    <div class="plan-meta">
                      <span>策略: {{ getStrategyName(plan.strategy) }}</span>
                      <span>综合评分: {{ plan.overallScore }}</span>
                    </div>
                  </div>
                  <div class="plan-scores">
                    <div class="score-item">
                      <span class="score-label">负荷均衡</span>
                      <el-progress :percentage="plan.workloadBalanceScore" :stroke-width="8" />
                    </div>
                    <div class="score-item">
                      <span class="score-label">考场利用</span>
                      <el-progress :percentage="plan.roomUtilizationScore" :stroke-width="8" />
                    </div>
                  </div>
                </div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="时间轴视图" name="timeline">
              <div v-if="!currentPlan" class="empty-state">
                <el-empty description="请先生成或选择一个方案" />
              </div>
              <div v-else class="chart-container">
                <div ref="ganttChartRef" class="chart"></div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="考官负荷对比" name="workload">
              <div v-if="!currentPlan" class="empty-state">
                <el-empty description="请先生成或选择一个方案" />
              </div>
              <div v-else class="chart-container">
                <div ref="workloadChartRef" class="chart"></div>
              </div>
            </el-tab-pane>

            <el-tab-pane label="分配详情" name="details">
              <div v-if="!currentPlan" class="empty-state">
                <el-empty description="请先生成或选择一个方案" />
              </div>
              <el-table
                v-else
                :data="assignmentDetails"
                border
                stripe
                style="width: 100%">
                <el-table-column prop="sessionName" label="场次" width="120" />
                <el-table-column prop="roomName" label="考场" width="120" />
                <el-table-column prop="examinerNames" label="考官" min-width="200">
                  <template #default="{ row }">
                    <div class="examiner-tags">
                      <el-tag
                        v-for="(name, idx) in row.examinerNames"
                        :key="idx"
                        size="small"
                        type="info"
                        class="examiner-tag">
                        {{ name }}
                      </el-tag>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column prop="candidateCount" label="考生数" width="100" align="center" />
                <el-table-column prop="startTime" label="开始时间" width="160">
                  <template #default="{ row }">
                    {{ formatDateTime(row.startTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="endTime" label="结束时间" width="160">
                  <template #default="{ row }">
                    {{ formatDateTime(row.endTime) }}
                  </template>
                </el-table-column>
                <el-table-column prop="positionName" label="职位" width="150" />
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </div>
    </div>

    <el-dialog v-model="showCompareDialog" title="方案对比" width="900px">
      <el-form label-width="100px">
        <el-form-item label="方案1">
          <el-select v-model="comparePlan1" placeholder="请选择方案" style="width: 100%">
            <el-option
              v-for="plan in savedPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="方案2">
          <el-select v-model="comparePlan2" placeholder="请选择方案" style="width: 100%">
            <el-option
              v-for="plan in savedPlans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <el-button
        type="primary"
        :disabled="!comparePlan1 || !comparePlan2"
        @click="comparePlans">
        开始对比
      </el-button>
      <div v-if="compareResult" class="compare-result">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="综合评分">
            <span :class="{ 'score-better': compareResult.plan1.overallScore > compareResult.plan2.overallScore }">
              方案1: {{ compareResult.plan1.overallScore }}
            </span>
            <span class="vs">VS</span>
            <span :class="{ 'score-better': compareResult.plan2.overallScore > compareResult.plan1.overallScore }">
              方案2: {{ compareResult.plan2.overallScore }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="负荷均衡">
            <span :class="{ 'score-better': compareResult.plan1.workloadBalanceScore > compareResult.plan2.workloadBalanceScore }">
              方案1: {{ compareResult.plan1.workloadBalanceScore }}
            </span>
            <span class="vs">VS</span>
            <span :class="{ 'score-better': compareResult.plan2.workloadBalanceScore > compareResult.plan1.workloadBalanceScore }">
              方案2: {{ compareResult.plan2.workloadBalanceScore }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="考场利用">
            <span :class="{ 'score-better': compareResult.plan1.roomUtilizationScore > compareResult.plan2.roomUtilizationScore }">
              方案1: {{ compareResult.plan1.roomUtilizationScore }}
            </span>
            <span class="vs">VS</span>
            <span :class="{ 'score-better': compareResult.plan2.roomUtilizationScore > compareResult.plan1.roomUtilizationScore }">
              方案2: {{ compareResult.plan2.roomUtilizationScore }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="总场次">
            <span>方案1: {{ compareResult.plan1.totalSessions }}</span>
            <span class="vs">VS</span>
            <span>方案2: {{ compareResult.plan2.totalSessions }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Setting, Check, MagicStick, View, Download, Histogram
} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import dayjs from 'dayjs'
import api from '@/api'

const route = useRoute()
const router = useRouter()

const projectId = ref(route.params.projectId)

const validating = ref(false)
const generating = ref(false)
const activeTab = ref('plans')
const showCompareDialog = ref(false)
const comparePlan1 = ref(null)
const comparePlan2 = ref(null)
const compareResult = ref(null)

const config = reactive({
  strategy: 0,
  examinersPerRoom: 3,
  interviewDuration: 30,
  maxConsecutiveSessions: 3,
  considerHistoryWorkload: true
})

const conflicts = ref([])
const savedPlans = ref([])
const currentPlan = ref(null)
const assignmentDetails = ref([])

const ganttChartRef = ref(null)
const workloadChartRef = ref(null)
let ganttChart = null
let workloadChart = null

const getStrategyName = (strategy) => {
  const names = ['均衡负荷', '压缩时间', '职位优先']
  return names[strategy] || '未知'
}

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  return dayjs(datetime).format('YYYY-MM-DD HH:mm')
}

const validateConstraints = async () => {
  validating.value = true
  try {
    const res = await api.scheduling.validateConstraints(projectId.value)
    if (res.data && res.data.conflicts) {
      conflicts.value = res.data.conflicts
      if (res.data.conflicts.length === 0) {
        ElMessage.success('约束校验通过，无冲突')
      } else {
        ElMessage.warning(`发现 ${res.data.conflicts.length} 个冲突，请查看提示`)
      }
    } else {
      conflicts.value = []
      ElMessage.success('约束校验通过')
    }
  } catch (e) {
    ElMessage.error('校验失败')
  } finally {
    validating.value = false
  }
}

const generatePlan = async () => {
  generating.value = true
  try {
    const res = await api.scheduling.generateWithStrategy(config, config.strategy)
    if (res.success) {
      ElMessage.success('方案生成成功')
      currentPlan.value = res.data?.plan || res.plan
      conflicts.value = res.data?.conflicts || res.conflicts || []
      if (currentPlan.value) {
        await loadAssignmentDetails(currentPlan.value.id)
        await loadPlans()
        activeTab.value = 'timeline'
        nextTick(() => {
          initCharts()
        })
      }
    } else {
      ElMessage.error(res.message || '方案生成失败')
    }
  } catch (e) {
    ElMessage.error('生成方案失败')
  } finally {
    generating.value = false
  }
}

const loadPlans = async () => {
  try {
    const res = await api.scheduling.listPlans(projectId.value)
    savedPlans.value = res.data?.records || res.data || []
  } catch (e) {
    console.error('加载方案列表失败', e)
  }
}

const selectPlan = async (plan) => {
  currentPlan.value = plan
  await loadAssignmentDetails(plan.id)
  activeTab.value = 'timeline'
  nextTick(() => {
    initCharts()
  })
}

const loadAssignmentDetails = async (planId) => {
  try {
    const res = await api.scheduling.getAssignmentDetails(planId)
    assignmentDetails.value = res.data?.records || res.data || []
  } catch (e) {
    console.error('加载分配详情失败', e)
  }
}

const setExecution = async () => {
  if (!currentPlan.value) return
  try {
    await ElMessageBox.confirm(
      `确定将 "${currentPlan.value.planName}" 设为执行方案？`,
      '确认',
      { type: 'warning' }
    )
    await api.scheduling.setExecution(currentPlan.value.id)
    ElMessage.success('已设为执行方案')
    loadPlans()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const applyPlan = async () => {
  if (!currentPlan.value) return
  try {
    await ElMessageBox.confirm(
      `确定将 "${currentPlan.value.planName}" 应用到系统？应用后将无法撤销。`,
      '确认',
      { type: 'warning' }
    )
    await api.scheduling.applyPlan(currentPlan.value.id)
    ElMessage.success('方案已应用到系统')
    loadPlans()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('应用失败')
    }
  }
}

const exportExcel = async () => {
  try {
    const blob = await api.report.exportExcel(projectId.value)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = `调度方案_${dayjs().format('YYYYMMDD')}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

const comparePlans = async () => {
  if (!comparePlan1.value || !comparePlan2.value) return
  try {
    const res = await api.report.comparePlans(comparePlan1.value, comparePlan2.value)
    compareResult.value = res.data || res
  } catch (e) {
    ElMessage.error('对比失败')
  }
}

const initCharts = () => {
  initGanttChart()
  initWorkloadChart()
}

const initGanttChart = () => {
  if (!ganttChartRef.value) return
  if (ganttChart) {
    ganttChart.dispose()
  }
  ganttChart = echarts.init(ganttChartRef.value)

  const rooms = [...new Set(assignmentDetails.value.map(a => a.roomName))]
  const roomData = rooms.map((room, index) => {
    const roomAssignments = assignmentDetails.value.filter(a => a.roomName === room)
    return roomAssignments.map(a => ({
      name: a.sessionName,
      value: [
        index,
        dayjs(a.startTime).valueOf(),
        dayjs(a.endTime).valueOf(),
        dayjs(a.endTime).diff(dayjs(a.startTime), 'minute')
      ],
      itemStyle: {
        color: getColorByIndex(index)
      }
    }))
  }).flat()

  const option = {
    tooltip: {
      formatter: (params) => {
        const data = params.data
        return `
          <div>场次: ${data.name}</div>
          <div>开始: ${dayjs(data.value[1]).format('YYYY-MM-DD HH:mm')}</div>
          <div>结束: ${dayjs(data.value[2]).format('YYYY-MM-DD HH:mm')}</div>
          <div>时长: ${data.value[3]} 分钟</div>
        `
      }
    },
    grid: {
      left: '10%',
      right: '10%',
      top: '10%',
      bottom: '15%'
    },
    xAxis: {
      type: 'time',
      axisLabel: {
        formatter: (value) => dayjs(value).format('MM-DD HH:mm')
      }
    },
    yAxis: {
      type: 'category',
      data: rooms,
      axisLabel: {
        interval: 0
      }
    },
    series: [{
      type: 'custom',
      renderItem: (params, api) => {
        const categoryIndex = api.value(0)
        const start = api.coord([api.value(1), categoryIndex])
        const end = api.coord([api.value(2), categoryIndex])
        const height = api.size([0, 1])[1] * 0.6

        return {
          type: 'rect',
          shape: echarts.graphic.clipRectByRect(
            {
              x: start[0],
              y: start[1] - height / 2,
              width: end[0] - start[0],
              height: height
            },
            {
              x: params.coordSys.x,
              y: params.coordSys.y,
              width: params.coordSys.width,
              height: params.coordSys.height
            }
          ),
          style: api.style()
        }
      },
      encode: {
        x: [1, 2],
        y: 0
      },
      data: roomData
    }]
  }

  ganttChart.setOption(option)
}

const initWorkloadChart = () => {
  if (!workloadChartRef.value) return
  if (workloadChart) {
    workloadChart.dispose()
  }
  workloadChart = echarts.init(workloadChartRef.value)

  const examinerMap = {}
  assignmentDetails.value.forEach(a => {
    if (a.examinerNames) {
      a.examinerNames.forEach(name => {
        if (!examinerMap[name]) {
          examinerMap[name] = 0
        }
        examinerMap[name] += dayjs(a.endTime).diff(dayjs(a.startTime), 'minute')
      })
    }
  })

  const examiners = Object.keys(examinerMap)
  const workloads = Object.values(examinerMap)

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
          <div>总时长: ${data.value} 分钟</div>
          <div>约 ${(data.value / 60).toFixed(1)} 小时</div>
        `
      }
    },
    grid: {
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: examiners,
      axisLabel: {
        rotate: 45,
        interval: 0
      }
    },
    yAxis: {
      type: 'value',
      name: '时长(分钟)'
    },
    series: [{
      type: 'bar',
      data: workloads,
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#83bff6' },
          { offset: 0.5, color: '#188df0' },
          { offset: 1, color: '#188df0' }
        ])
      },
      label: {
        show: true,
        position: 'top',
        formatter: '{c}'
      },
      barWidth: '60%'
    }]
  }

  workloadChart.setOption(option)
}

const colors = [
  '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de',
  '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc', '#48b4bd'
]

const getColorByIndex = (index) => {
  return colors[index % colors.length]
}

const handleResize = () => {
  ganttChart?.resize()
  workloadChart?.resize()
}

watch(activeTab, (val) => {
  if (val === 'timeline' || val === 'workload') {
    nextTick(() => {
      if (val === 'timeline') {
        ganttChart?.resize()
      } else if (val === 'workload') {
        workloadChart?.resize()
      }
    })
  }
})

onMounted(() => {
  loadPlans()
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  ganttChart?.dispose()
  workloadChart?.dispose()
})
</script>

<style scoped lang="scss">
.scheduling-plan {
  padding: 20px;

  .breadcrumb-card {
    margin-bottom: 20px;

    .page-title {
      margin: 12px 0 0 0;
      font-size: 20px;
      font-weight: bold;
      color: #303133;
    }
  }

  .conflict-alert {
    margin-bottom: 20px;
  }

  .main-content {
    display: flex;
    gap: 20px;
    align-items: flex-start;

    .config-panel {
      width: 320px;
      flex-shrink: 0;

      .panel-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: bold;
      }

      .config-form {
        .radio-option {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .option-title {
            font-weight: 500;
            color: #303133;
          }

          .option-desc {
            font-size: 12px;
            color: #909399;
          }
        }

        :deep(.el-radio) {
          display: block;
          margin-bottom: 12px;
        }
      }
    }

    .preview-area {
      flex: 1;
      min-width: 0;

      .preview-card {
        .preview-header {
          display: flex;
          align-items: center;
          justify-content: space-between;

          .header-left {
            display: flex;
            align-items: center;
            gap: 8px;
            font-weight: bold;

            .plan-tag {
              margin-left: 8px;
            }
          }

          .header-right {
            display: flex;
            gap: 8px;
          }
        }

        .preview-tabs {
          .empty-state {
            padding: 40px 0;
          }

          .plans-list {
            .plan-item {
              display: flex;
              justify-content: space-between;
              align-items: center;
              padding: 16px;
              border: 1px solid #e4e7ed;
              border-radius: 8px;
              margin-bottom: 12px;
              cursor: pointer;
              transition: all 0.3s;

              &:hover {
                border-color: #409eff;
                background-color: #f5f7fa;
              }

              &.active {
                border-color: #409eff;
                background-color: #ecf5ff;
              }

              .plan-info {
                .plan-name {
                  font-size: 16px;
                  font-weight: 500;
                  color: #303133;
                  margin-bottom: 8px;
                  display: flex;
                  align-items: center;
                  gap: 8px;

                  .exec-tag {
                    font-size: 12px;
                  }
                }

                .plan-meta {
                  display: flex;
                  gap: 16px;
                  font-size: 13px;
                  color: #909399;
                }
              }

              .plan-scores {
                width: 200px;

                .score-item {
                  margin-bottom: 8px;

                  &:last-child {
                    margin-bottom: 0;
                  }

                  .score-label {
                    display: block;
                    font-size: 12px;
                    color: #606266;
                    margin-bottom: 4px;
                  }
                }
              }
            }
          }

          .chart-container {
            .chart {
              height: 500px;
              width: 100%;
            }
          }

          .examiner-tags {
            display: flex;
            flex-wrap: wrap;
            gap: 4px;

            .examiner-tag {
              margin: 2px 0;
            }
          }
        }
      }
    }
  }

  .compare-result {
    margin-top: 20px;

    .score-better {
      color: #67c23a;
      font-weight: bold;
    }

    .vs {
      margin: 0 16px;
      color: #909399;
      font-weight: bold;
    }
  }
}
</style>
