<template>
  <div class="examiner-availability">
    <el-page-header
      :icon="Back"
      content="调度首页"
      class="breadcrumb"
      @back="goBack">
      <template #content>
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/scheduling' }">调度首页</el-breadcrumb-item>
          <el-breadcrumb-item>考官可用性管理</el-breadcrumb-item>
        </el-breadcrumb>
      </template>
    </el-page-header>

    <el-card class="content-card">
      <div class="page-header">
        <div>
          <h2 class="page-title">考官可用性管理</h2>
          <p class="page-desc">日历视图管理考官在各个时间段的可用状态</p>
        </div>
      </div>

      <div class="action-bar">
        <div class="action-left">
          <el-button type="primary" :icon="RefreshRight" @click="handleInitDefault">
            初始化默认可用性
          </el-button>
          <el-button type="success" :icon="Check" :loading="saving" @click="handleBatchSave">
            批量保存
          </el-button>
        </div>
        <div class="action-right">
          <el-select
            v-model="selectedDept"
            placeholder="筛选部门"
            clearable
            style="width: 200px"
            @change="filterExaminers">
            <el-option
              v-for="dept in departments"
              :key="dept"
              :label="dept"
              :value="dept" />
          </el-select>
          <el-select
            v-model="selectedExaminerId"
            placeholder="筛选考官"
            filterable
            clearable
            style="width: 200px"
            @change="filterExaminers">
            <el-option
              v-for="examiner in examiners"
              :key="examiner.id"
              :label="examiner.examinerName"
              :value="examiner.id" />
          </el-select>
        </div>
      </div>

      <div class="legend">
        <span class="legend-title">状态说明：</span>
        <div class="legend-item">
          <span class="legend-color available-all"></span>
          <span>全天可用</span>
        </div>
        <div class="legend-item">
          <span class="legend-color available-morning"></span>
          <span>仅上午可用</span>
        </div>
        <div class="legend-item">
          <span class="legend-color available-afternoon"></span>
          <span>仅下午可用</span>
        </div>
        <div class="legend-item">
          <span class="legend-color unavailable"></span>
          <span>不可用</span>
        </div>
      </div>

      <div class="main-content">
        <div class="examiner-list-panel">
          <div class="panel-header">
            <el-checkbox
              v-model="selectAll"
              :indeterminate="isIndeterminate"
              @change="handleSelectAll">
              全选
            </el-checkbox>
            <span class="count">{{ filteredExaminers.length }} 位考官</span>
          </div>
          <div class="examiner-list" v-loading="loading">
            <div
              v-for="examiner in filteredExaminers"
              :key="examiner.id"
              class="examiner-item">
              <el-checkbox
                :model-value="selectedExaminers.includes(examiner.id)"
                @change="(val) => handleSelectExaminer(examiner.id, val)">
              </el-checkbox>
              <div class="examiner-info">
                <span
                  class="examiner-name"
                  @click="showExaminerDetail(examiner)">
                  {{ examiner.examinerName }}
                </span>
                <span class="examiner-dept">{{ examiner.organization || '未设置' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="calendar-panel" v-loading="loading">
          <div class="calendar-wrapper">
            <table class="availability-table">
              <thead>
                <tr>
                  <th class="corner-cell">考官\日期</th>
                  <th
                    v-for="col in dateColumns"
                    :key="col.date"
                    :colspan="2"
                    class="date-header">
                    <div class="date-text">{{ col.dateText }}</div>
                    <div class="weekday-text">{{ col.weekdayText }}</div>
                  </th>
                </tr>
                <tr>
                  <th class="corner-cell"></th>
                  <template v-for="col in dateColumns" :key="'slot-' + col.date">
                    <th class="slot-header morning">上午</th>
                    <th class="slot-header afternoon">下午</th>
                  </template>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="examiner in filteredExaminers"
                  :key="examiner.id">
                  <td class="examiner-cell">
                    <span
                      class="examiner-name-cell"
                      @click="showExaminerDetail(examiner)">
                      {{ examiner.examinerName }}
                    </span>
                  </td>
                  <template v-for="col in dateColumns" :key="'cell-' + examiner.id + '-' + col.date">
                    <td
                      :class="getCellClass(examiner.id, col.date, 1)"
                      @click="toggleCell(examiner.id, col.date, 1)">
                      <span v-if="getStatus(examiner.id, col.date) === 1 || getStatus(examiner.id, col.date) === 2">
                        <el-icon><Check /></el-icon>
                      </span>
                      <span v-else>
                        <el-icon><Close /></el-icon>
                      </span>
                    </td>
                    <td
                      :class="getCellClass(examiner.id, col.date, 2)"
                      @click="toggleCell(examiner.id, col.date, 2)">
                      <span v-if="getStatus(examiner.id, col.date) === 1 || getStatus(examiner.id, col.date) === 3">
                        <el-icon><Check /></el-icon>
                      </span>
                      <span v-else>
                        <el-icon><Close /></el-icon>
                      </span>
                    </td>
                  </template>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      :title="`${currentExaminer?.examinerName || ''} - 历史工作量统计`"
      width="600px">
      <div v-if="currentExaminer" class="detail-content">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="姓名">
            {{ currentExaminer.examinerName }}
          </el-descriptions-item>
          <el-descriptions-item label="编号">
            {{ currentExaminer.examinerCode || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="所属单位">
            {{ currentExaminer.organization || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="职称">
            {{ currentExaminer.title || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="联系电话">
            {{ currentExaminer.phone || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="专业领域">
            {{ currentExaminer.expertise || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="workload-stats">
          <h4>工作量统计</h4>
          <el-row :gutter="20">
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value blue">{{ workloadStats.totalSessions }}</div>
                <div class="stat-label">总场次数</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value green">{{ workloadStats.totalCandidates }}</div>
                <div class="stat-label">评审考生数</div>
              </div>
            </el-col>
            <el-col :span="8">
              <div class="stat-item">
                <div class="stat-value orange">{{ workloadStats.availableDays }}</div>
                <div class="stat-label">可用天数</div>
              </div>
            </el-col>
          </el-row>
        </div>

        <div class="availability-summary">
          <h4>本期可用情况</h4>
          <div class="availability-list">
            <div
              v-for="item in availabilitySummary"
              :key="item.date"
              class="availability-item">
              <span class="date">{{ item.dateText }}</span>
              <el-tag :type="item.statusType" size="small">
                {{ item.statusText }}
              </el-tag>
            </div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Back, RefreshRight, Check, Close
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'
import api from '@/api'

dayjs.locale('zh-cn')

const route = useRoute()
const router = useRouter()

const projectId = computed(() => Number(route.params.projectId))

const loading = ref(false)
const saving = ref(false)
const examiners = ref([])
const departments = ref([])
const dateColumns = ref([])
const selectedDept = ref('')
const selectedExaminerId = ref('')
const selectAll = ref(false)
const selectedExaminers = ref([])
const detailVisible = ref(false)
const currentExaminer = ref(null)

const availabilityMap = reactive(new Map())
const changedCells = reactive(new Set())

const workloadStats = reactive({
  totalSessions: 0,
  totalCandidates: 0,
  availableDays: 0
})

const availabilitySummary = ref([])

const STATUS = {
  UNAVAILABLE: 0,
  ALL_DAY: 1,
  MORNING_ONLY: 2,
  AFTERNOON_ONLY: 3
}

const TIME_SLOT = {
  MORNING: 1,
  AFTERNOON: 2
}

const filteredExaminers = computed(() => {
  let list = examiners.value
  if (selectedDept.value) {
    list = list.filter(e => e.organization === selectedDept.value)
  }
  if (selectedExaminerId.value) {
    list = list.filter(e => e.id === selectedExaminerId.value)
  }
  return list
})

const isIndeterminate = computed(() => {
  const count = filteredExaminers.value.length
  if (count === 0) return false
  const selectedCount = filteredExaminers.value.filter(
    e => selectedExaminers.value.includes(e.id)
  ).length
  return selectedCount > 0 && selectedCount < count
})

const goBack = () => {
  router.push('/scheduling')
}

const loadData = async () => {
  if (!projectId.value) return
  loading.value = true
  try {
    const [projectRes, examinersRes, availabilityRes] = await Promise.all([
      api.projects.get(projectId.value),
      api.examiners.list({ projectId: projectId.value, pageNum: 1, pageSize: 1000 }),
      api.availability.getMap(projectId.value)
    ])

    const project = projectRes.data || projectRes
    examiners.value = examinersRes.data?.records || examinersRes.data || []

    const deptSet = new Set()
    examiners.value.forEach(e => {
      if (e.organization) deptSet.add(e.organization)
    })
    departments.value = Array.from(deptSet)

    generateDateColumns(project)

    const availabilityData = availabilityRes.data || {}
    Object.keys(availabilityData).forEach(examinerId => {
      const list = availabilityData[examinerId] || []
      const examinerMap = new Map()
      list.forEach(item => {
        const date = dayjs(item.availableDate).format('YYYY-MM-DD')
        examinerMap.set(date, item.timeSlot)
      })
      availabilityMap.set(Number(examinerId), examinerMap)
    })

    examiners.value.forEach(examiner => {
      if (!availabilityMap.has(examiner.id)) {
        availabilityMap.set(examiner.id, new Map())
      }
    })
  } catch (e) {
    ElMessage.error('加载数据失败')
    console.error(e)
  }
  loading.value = false
}

const generateDateColumns = (project) => {
  const columns = []
  if (project?.interviewDate) {
    const startDate = dayjs(project.interviewDate)
    for (let i = 0; i < 3; i++) {
      const date = startDate.add(i, 'day')
      columns.push({
        date: date.format('YYYY-MM-DD'),
        dateText: date.format('MM-DD'),
        weekdayText: date.format('dddd')
      })
    }
  } else {
    const today = dayjs()
    for (let i = 0; i < 3; i++) {
      const date = today.add(i, 'day')
      columns.push({
        date: date.format('YYYY-MM-DD'),
        dateText: date.format('MM-DD'),
        weekdayText: date.format('dddd')
      })
    }
  }
  dateColumns.value = columns
}

const getStatus = (examinerId, date) => {
  const examinerMap = availabilityMap.get(examinerId)
  if (!examinerMap) return STATUS.UNAVAILABLE
  return examinerMap.get(date) ?? STATUS.UNAVAILABLE
}

const getCellClass = (examinerId, date, slot) => {
  const status = getStatus(examinerId, date)
  const baseClass = 'availability-cell'
  const changed = changedCells.has(`${examinerId}-${date}`) ? ' changed' : ''

  if (status === STATUS.ALL_DAY) {
    return `${baseClass} available-all${changed}`
  }
  if (status === STATUS.MORNING_ONLY && slot === TIME_SLOT.MORNING) {
    return `${baseClass} available-morning${changed}`
  }
  if (status === STATUS.MORNING_ONLY && slot === TIME_SLOT.AFTERNOON) {
    return `${baseClass} unavailable${changed}`
  }
  if (status === STATUS.AFTERNOON_ONLY && slot === TIME_SLOT.MORNING) {
    return `${baseClass} unavailable${changed}`
  }
  if (status === STATUS.AFTERNOON_ONLY && slot === TIME_SLOT.AFTERNOON) {
    return `${baseClass} available-afternoon${changed}`
  }
  return `${baseClass} unavailable${changed}`
}

const toggleCell = (examinerId, date, slot) => {
  const currentStatus = getStatus(examinerId, date)
  let newStatus

  if (slot === TIME_SLOT.MORNING) {
    if (currentStatus === STATUS.UNAVAILABLE) {
      newStatus = STATUS.MORNING_ONLY
    } else if (currentStatus === STATUS.MORNING_ONLY) {
      newStatus = STATUS.ALL_DAY
    } else if (currentStatus === STATUS.ALL_DAY) {
      newStatus = STATUS.AFTERNOON_ONLY
    } else {
      newStatus = STATUS.UNAVAILABLE
    }
  } else {
    if (currentStatus === STATUS.UNAVAILABLE) {
      newStatus = STATUS.AFTERNOON_ONLY
    } else if (currentStatus === STATUS.AFTERNOON_ONLY) {
      newStatus = STATUS.ALL_DAY
    } else if (currentStatus === STATUS.ALL_DAY) {
      newStatus = STATUS.MORNING_ONLY
    } else {
      newStatus = STATUS.UNAVAILABLE
    }
  }

  const examinerMap = availabilityMap.get(examinerId)
  if (examinerMap) {
    examinerMap.set(date, newStatus)
  }
  changedCells.add(`${examinerId}-${date}`)
}

const handleSelectAll = (val) => {
  if (val) {
    selectedExaminers.value = filteredExaminers.value.map(e => e.id)
  } else {
    selectedExaminers.value = []
  }
}

const handleSelectExaminer = (id, val) => {
  if (val) {
    selectedExaminers.value.push(id)
  } else {
    const index = selectedExaminers.value.indexOf(id)
    if (index > -1) {
      selectedExaminers.value.splice(index, 1)
    }
  }
}

const filterExaminers = () => {
  selectAll.value = false
}

const handleInitDefault = async () => {
  try {
    await ElMessageBox.confirm(
      '初始化将为所有考官设置默认可用性（不可用），是否继续？',
      '确认初始化',
      { type: 'warning' }
    )
    await api.availability.initDefault(projectId.value)
    ElMessage.success('初始化成功')
    loadData()
    changedCells.clear()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('初始化失败')
    }
  }
}

const handleBatchSave = async () => {
  if (changedCells.size === 0) {
    ElMessage.info('没有需要保存的修改')
    return
  }

  const configs = []
  const examinerDateMap = new Map()

  changedCells.forEach(key => {
    const [examinerIdStr, date] = key.split('-')
    const examinerId = Number(examinerIdStr)
    if (!examinerDateMap.has(examinerId)) {
      examinerDateMap.set(examinerId, [])
    }
    examinerDateMap.get(examinerId).push(date)
  })

  examinerDateMap.forEach((dates, examinerId) => {
    const dateAvailabilities = dates.map(date => ({
      date,
      timeSlot: getStatus(examinerId, date),
      remark: ''
    }))
    configs.push({
      projectId: projectId.value,
      examinerId,
      dateAvailabilities,
      maxConsecutiveSessions: 3,
      specialConstraints: ''
    })
  })

  saving.value = true
  try {
    await api.availability.batchSave(projectId.value, configs)
    ElMessage.success(`已保存 ${changedCells.size} 条修改`)
    changedCells.clear()
  } catch (e) {
    ElMessage.error('保存失败')
  }
  saving.value = false
}

const showExaminerDetail = async (examiner) => {
  currentExaminer.value = examiner
  detailVisible.value = true

  workloadStats.totalSessions = 0
  workloadStats.totalCandidates = 0
  workloadStats.availableDays = 0

  const examinerMap = availabilityMap.get(examiner.id)
  if (examinerMap) {
    let availableCount = 0
    const summary = []
    dateColumns.value.forEach(col => {
      const status = examinerMap.get(col.date) ?? STATUS.UNAVAILABLE
      let statusText = '不可用'
      let statusType = 'info'
      if (status === STATUS.ALL_DAY) {
        statusText = '全天可用'
        statusType = 'success'
        availableCount++
      } else if (status === STATUS.MORNING_ONLY) {
        statusText = '仅上午可用'
        statusType = 'warning'
        availableCount += 0.5
      } else if (status === STATUS.AFTERNOON_ONLY) {
        statusText = '仅下午可用'
        statusType = 'warning'
        availableCount += 0.5
      }
      summary.push({
        date: col.date,
        dateText: `${col.dateText} ${col.weekdayText}`,
        statusText,
        statusType
      })
    })
    workloadStats.availableDays = availableCount
    availabilitySummary.value = summary
  }

  try {
    const res = await api.availability.getExaminer(examiner.id)
    const list = res.data || []
    let sessions = 0
    let candidates = 0
    list.forEach(item => {
      sessions += item.totalSessions || 0
      candidates += item.totalCandidates || 0
    })
    workloadStats.totalSessions = sessions
    workloadStats.totalCandidates = candidates
  } catch (e) {
    console.error('加载工作量统计失败', e)
  }
}

watch(selectAll, (val) => {
  if (val) {
    selectedExaminers.value = filteredExaminers.value.map(e => e.id)
  }
})

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.examiner-availability {
  padding: 20px;

  .breadcrumb {
    margin-bottom: 20px;
  }

  .content-card {
    padding: 24px;
  }

  .page-header {
    margin-bottom: 24px;

    .page-title {
      font-size: 22px;
      font-weight: 600;
      color: #303133;
      margin: 0 0 8px 0;
    }

    .page-desc {
      font-size: 14px;
      color: #909399;
      margin: 0;
    }
  }

  .action-bar {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    flex-wrap: wrap;
    gap: 16px;

    .action-left,
    .action-right {
      display: flex;
      gap: 12px;
      align-items: center;
    }
  }

  .legend {
    display: flex;
    align-items: center;
    gap: 24px;
    padding: 16px 20px;
    background: #f8f9fa;
    border-radius: 8px;
    margin-bottom: 20px;
    flex-wrap: wrap;

    .legend-title {
      font-weight: 500;
      color: #606266;
    }

    .legend-item {
      display: flex;
      align-items: center;
      gap: 8px;
      font-size: 13px;
      color: #606266;
    }

    .legend-color {
      width: 20px;
      height: 20px;
      border-radius: 4px;

      &.available-all {
        background: #67c23a;
      }

      &.available-morning {
        background: #f7d154;
      }

      &.available-afternoon {
        background: #f59e0b;
      }

      &.unavailable {
        background: #c0c4cc;
      }
    }
  }

  .main-content {
    display: flex;
    gap: 0;
    border: 1px solid #ebeef5;
    border-radius: 8px;
    overflow: hidden;
  }

  .examiner-list-panel {
    width: 240px;
    border-right: 1px solid #ebeef5;
    background: #fafafa;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-bottom: 1px solid #ebeef5;
      background: #fff;

      .count {
        font-size: 13px;
        color: #909399;
      }
    }

    .examiner-list {
      max-height: 500px;
      overflow-y: auto;
    }

    .examiner-item {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 10px 16px;
      border-bottom: 1px solid #f0f0f0;
      transition: background 0.2s;

      &:hover {
        background: #f0f7ff;
      }

      .examiner-info {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 2px;

        .examiner-name {
          font-size: 14px;
          color: #303133;
          cursor: pointer;
          transition: color 0.2s;

          &:hover {
            color: #409eff;
          }
        }

        .examiner-dept {
          font-size: 12px;
          color: #909399;
        }
      }
    }
  }

  .calendar-panel {
    flex: 1;
    overflow-x: auto;
    background: #fff;

    .calendar-wrapper {
      min-width: 600px;
    }
  }

  .availability-table {
    width: 100%;
    border-collapse: collapse;

    th,
    td {
      border: 1px solid #ebeef5;
      text-align: center;
      padding: 0;
    }

    .corner-cell {
      width: 120px;
      min-width: 120px;
      background: #f8f9fa;
      font-weight: 500;
      color: #606266;
      padding: 12px 8px;
    }

    .date-header {
      background: #f8f9fa;
      padding: 8px;

      .date-text {
        font-size: 14px;
        font-weight: 600;
        color: #303133;
      }

      .weekday-text {
        font-size: 12px;
        color: #909399;
      }
    }

    .slot-header {
      padding: 6px;
      font-size: 13px;
      font-weight: 500;
      color: #606266;
      background: #fafafa;

      &.morning {
        background: #fef6ec;
        color: #e6a23c;
      }

      &.afternoon {
        background: #fef0f0;
        color: #f56c6c;
      }
    }

    .examiner-cell {
      padding: 8px;
      background: #fafafa;

      .examiner-name-cell {
        font-size: 13px;
        color: #303133;
        cursor: pointer;
        transition: color 0.2s;

        &:hover {
          color: #409eff;
        }
      }
    }

    .availability-cell {
      width: 60px;
      height: 48px;
      cursor: pointer;
      transition: all 0.2s;
      font-size: 18px;
      color: #fff;

      &:hover {
        opacity: 0.85;
        transform: scale(0.98);
      }

      &.available-all {
        background: #67c23a;
      }

      &.available-morning {
        background: #f7d154;
        color: #606266;
      }

      &.available-afternoon {
        background: #f59e0b;
      }

      &.unavailable {
        background: #f0f2f5;
        color: #c0c4cc;
      }

      &.changed {
        box-shadow: inset 0 0 0 2px #409eff;
      }
    }
  }

  .detail-content {
    .workload-stats {
      margin-top: 20px;

      h4 {
        margin: 0 0 16px 0;
        font-size: 15px;
        color: #303133;
      }

      .stat-item {
        text-align: center;
        padding: 16px;
        background: #f8f9fa;
        border-radius: 8px;

        .stat-value {
          font-size: 28px;
          font-weight: 600;
          margin-bottom: 4px;

          &.blue {
            color: #409eff;
          }

          &.green {
            color: #67c23a;
          }

          &.orange {
            color: #e6a23c;
          }
        }

        .stat-label {
          font-size: 13px;
          color: #909399;
        }
      }
    }

    .availability-summary {
      margin-top: 20px;

      h4 {
        margin: 0 0 12px 0;
        font-size: 15px;
        color: #303133;
      }

      .availability-list {
        display: flex;
        flex-wrap: wrap;
        gap: 8px;
      }

      .availability-item {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 8px 12px;
        background: #f8f9fa;
        border-radius: 6px;

        .date {
          font-size: 13px;
          color: #606266;
        }
      }
    }
  }
}
</style>
