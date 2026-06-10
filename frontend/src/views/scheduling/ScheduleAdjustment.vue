<template>
  <div class="schedule-adjustment">
    <el-card class="breadcrumb-card">
      <el-breadcrumb separator="/">
        <el-breadcrumb-item :to="{ path: '/scheduling' }">智能调度</el-breadcrumb-item>
        <el-breadcrumb-item>动态调整与应急处理</el-breadcrumb-item>
      </el-breadcrumb>
      <h2 class="page-title">动态调整与应急处理</h2>
    </el-card>

    <el-card class="plan-selector-card">
      <el-form :inline="true" class="plan-form">
        <el-form-item label="选择执行方案">
          <el-select
            v-model="selectedPlanId"
            placeholder="请选择要调整的执行方案"
            style="width: 300px"
            @change="handlePlanChange">
            <el-option
              v-for="plan in plans"
              :key="plan.id"
              :label="plan.planName"
              :value="plan.id">
              <span>{{ plan.planName }}</span>
              <el-tag v-if="plan.isExecution" type="warning" size="small" style="margin-left: 8px">
                执行方案
              </el-tag>
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert
      v-if="impactAssessment.visible"
      :title="impactAssessment.title"
      :type="impactAssessment.type"
      show-icon
      closable
      @close="impactAssessment.visible = false"
      class="impact-alert">
      <div class="impact-content">
        <div class="impact-item">
          <span class="label">受影响人数：</span>
          <span class="value">{{ impactAssessment.affectedCount }} 人</span>
        </div>
        <div class="impact-item">
          <span class="label">是否有冲突：</span>
          <span :class="['value', { 'has-conflict': impactAssessment.hasConflict }]">
            {{ impactAssessment.hasConflict ? '是' : '否' }}
          </span>
        </div>
        <div v-if="impactAssessment.suggestion" class="impact-item">
          <span class="label">建议：</span>
          <span class="value suggestion">{{ impactAssessment.suggestion }}</span>
        </div>
      </div>
    </el-alert>

    <el-card class="tabs-card">
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane label="分配调整" name="assignment">
          <div class="assignment-container">
            <div class="session-list">
              <div class="list-header">
                <el-icon><List /></el-icon>
                <span>场次列表</span>
              </div>
              <div v-if="assignments.length === 0" class="empty-state">
                <el-empty description="暂无场次数据" />
              </div>
              <div v-else class="sessions">
                <div
                  v-for="assignment in assignments"
                  :key="assignment.id"
                  class="session-item"
                  :class="{ active: selectedAssignment?.id === assignment.id }"
                  @click="selectAssignment(assignment)">
                  <div class="session-name">{{ assignment.sessionName }}</div>
                  <div class="session-info">
                    <span class="info-item">
                      <el-icon><Location /></el-icon>
                      {{ assignment.roomName || '未分配' }}
                    </span>
                    <span class="info-item">
                      <el-icon><User /></el-icon>
                      {{ assignment.examinerNames?.join('、') || '未分配' }}
                    </span>
                    <span class="info-item">
                      <el-icon><Avatar /></el-icon>
                      {{ assignment.candidateCount || 0 }} 人
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div class="assignment-detail">
              <div v-if="!selectedAssignment" class="empty-detail">
                <el-empty description="请选择一个场次查看详情" />
              </div>
              <div v-else>
                <div class="detail-header">
                  <h3>{{ selectedAssignment.sessionName }} - 分配详情</h3>
                  <div class="action-buttons">
                    <el-button
                      type="primary"
                      size="small"
                      :disabled="selectedCandidates.length === 0"
                      @click="showMoveDialog = true">
                      <el-icon><Switch /></el-icon>
                      移动考生
                    </el-button>
                    <el-button
                      type="warning"
                      size="small"
                      @click="showReplaceDialog = true">
                      <el-icon><Refresh /></el-icon>
                      替换考官
                    </el-button>
                    <el-button
                      type="success"
                      size="small"
                      @click="showRoomDialog = true">
                      <el-icon><OfficeBuilding /></el-icon>
                      调整考场
                    </el-button>
                  </div>
                </div>

                <div class="detail-content">
                  <div class="candidates-section">
                    <div class="section-header">
                      <span>考生列表</span>
                      <el-checkbox
                        :model-value="isAllCandidatesSelected"
                        :indeterminate="isIndeterminate"
                        @change="toggleAllCandidates">
                        全选
                      </el-checkbox>
                    </div>
                    <div v-if="!selectedAssignment.candidates?.length" class="empty-state">
                      <el-empty description="暂无考生" :image-size="60" />
                    </div>
                    <el-checkbox-group v-else v-model="selectedCandidates" class="candidate-list">
                      <el-checkbox
                        v-for="candidate in selectedAssignment.candidates"
                        :key="candidate.id"
                        :value="candidate.id"
                        class="candidate-item">
                        <span class="candidate-name">{{ candidate.name }}</span>
                        <span class="candidate-position">{{ candidate.positionName }}</span>
                      </el-checkbox>
                    </el-checkbox-group>
                  </div>

                  <div class="examiners-section">
                    <div class="section-header">
                      <span>考官列表</span>
                    </div>
                    <div v-if="!selectedAssignment.examiners?.length" class="empty-state">
                      <el-empty description="暂无考官" :image-size="60" />
                    </div>
                    <div v-else class="examiner-list">
                      <div
                        v-for="examiner in selectedAssignment.examiners"
                        :key="examiner.id"
                        class="examiner-item">
                        <div class="examiner-avatar">
                          {{ examiner.name?.charAt(0) }}
                        </div>
                        <div class="examiner-info">
                          <div class="examiner-name">{{ examiner.name }}</div>
                          <div class="examiner-major">{{ examiner.major || '未设置专业' }}</div>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="应急处理" name="emergency">
          <div class="emergency-container">
            <el-form :model="emergencyForm" label-width="120px" class="emergency-form">
              <el-form-item label="应急类型">
                <el-radio-group v-model="emergencyForm.emergencyType">
                  <el-radio value="EXAMINER_LEAVE">考官临时请假</el-radio>
                  <el-radio value="CANDIDATE_ABSENT">考生缺席</el-radio>
                  <el-radio value="ROOM_FAILURE">考场设备故障</el-radio>
                  <el-radio value="OTHER">其他突发情况</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item label="选择考官" v-if="emergencyForm.emergencyType === 'EXAMINER_LEAVE'">
                <el-select
                  v-model="emergencyForm.examinerId"
                  placeholder="请选择请假的考官"
                  style="width: 300px"
                  filterable>
                  <el-option
                    v-for="examiner in allExaminers"
                    :key="examiner.id"
                    :label="examiner.name"
                    :value="examiner.id" />
                </el-select>
              </el-form-item>

              <el-form-item label="选择考生" v-if="emergencyForm.emergencyType === 'CANDIDATE_ABSENT'">
                <el-select
                  v-model="emergencyForm.candidateId"
                  placeholder="请选择缺席的考生"
                  style="width: 300px"
                  filterable>
                  <el-option
                    v-for="candidate in allCandidates"
                    :key="candidate.id"
                    :label="candidate.name"
                    :value="candidate.id" />
                </el-select>
              </el-form-item>

              <el-form-item label="选择考场" v-if="emergencyForm.emergencyType === 'ROOM_FAILURE'">
                <el-select
                  v-model="emergencyForm.roomId"
                  placeholder="请选择故障的考场"
                  style="width: 300px"
                  filterable>
                  <el-option
                    v-for="room in allRooms"
                    :key="room.id"
                    :label="room.name"
                    :value="room.id" />
                </el-select>
              </el-form-item>

              <el-form-item label="受影响场次">
                <el-select
                  v-model="emergencyForm.affectedAssignmentIds"
                  multiple
                  placeholder="请选择受影响的场次"
                  style="width: 100%">
                  <el-option
                    v-for="assignment in assignments"
                    :key="assignment.id"
                    :label="assignment.sessionName"
                    :value="assignment.id" />
                </el-select>
              </el-form-item>

              <el-form-item label="原因说明">
                <el-input
                  v-model="emergencyForm.reason"
                  type="textarea"
                  :rows="4"
                  placeholder="请详细说明突发情况的原因" />
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="generateEmergencyPlan">
                  <el-icon><MagicStick /></el-icon>
                  生成应急方案
                </el-button>
              </el-form-item>
            </el-form>

            <el-divider v-if="emergencyPlan" />

            <div v-if="emergencyPlan" class="emergency-plan">
              <h3>应急方案预览</h3>
              <el-descriptions :column="2" border>
                <el-descriptions-item label="影响范围">
                  {{ emergencyPlan.affectedScope }}
                </el-descriptions-item>
                <el-descriptions-item label="预计调整时间">
                  {{ emergencyPlan.estimatedTime }} 分钟
                </el-descriptions-item>
                <el-descriptions-item label="调整措施" :span="2">
                  <div v-for="(action, index) in emergencyPlan.actions" :key="index" class="action-item">
                    {{ index + 1 }}. {{ action }}
                  </div>
                </el-descriptions-item>
              </el-descriptions>
              <div class="plan-actions">
                <el-button type="primary" :loading="applyingEmergency" @click="applyEmergencyAdjustment">
                  <el-icon><Check /></el-icon>
                  应用应急调整
                </el-button>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="调整历史" name="history">
          <div class="history-container">
            <div class="history-toolbar">
              <el-select
                v-model="historyFilter"
                placeholder="筛选调整类型"
                clearable
                style="width: 200px"
                @change="loadHistory">
                <el-option label="移动考生" value="MOVE_CANDIDATE" />
                <el-option label="替换考官" value="REPLACE_EXAMINER" />
                <el-option label="应急处理" value="EMERGENCY" />
              </el-select>
              <el-button @click="loadHistory">
                <el-icon><Refresh /></el-icon>
                刷新
              </el-button>
            </div>
            <el-table
              :data="historyList"
              border
              stripe
              style="width: 100%">
              <el-table-column prop="createTime" label="调整时间" width="180">
                <template #default="{ row }">
                  {{ formatDateTime(row.createTime) }}
                </template>
              </el-table-column>
              <el-table-column prop="adjustmentType" label="调整类型" width="120">
                <template #default="{ row }">
                  <el-tag :type="getAdjustmentTypeTag(row.adjustmentType)" size="small">
                    {{ getAdjustmentTypeName(row.adjustmentType) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="adjustmentContent" label="调整内容摘要" min-width="200">
                <template #default="{ row }">
                  {{ row.adjustmentContent || row.newInfo || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="operatorName" label="操作人" width="120" />
              <el-table-column prop="affectedCount" label="影响人数" width="100" align="center">
                <template #default="{ row }">
                  {{ row.affectedCount || 0 }}
                </template>
              </el-table-column>
              <el-table-column prop="reason" label="原因说明" min-width="150" show-overflow-tooltip />
              <el-table-column label="操作" width="100" align="center">
                <template #default="{ row }">
                  <el-button type="primary" link size="small" @click="showHistoryDetail(row)">
                    详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <el-dialog v-model="showMoveDialog" title="移动考生" width="500px">
      <el-form label-width="100px">
        <el-form-item label="已选考生">
          <el-tag
            v-for="id in selectedCandidates"
            :key="id"
            size="small"
            class="candidate-tag">
            {{ getCandidateName(id) }}
          </el-tag>
        </el-form-item>
        <el-form-item label="目标场次">
          <el-select
            v-model="targetAssignmentId"
            placeholder="请选择目标场次"
            style="width: 100%">
            <el-option
              v-for="assignment in otherAssignments"
              :key="assignment.id"
              :label="assignment.sessionName"
              :value="assignment.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input
            v-model="moveReason"
            type="textarea"
            :rows="3"
            placeholder="请说明调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showMoveDialog = false">取消</el-button>
        <el-button type="primary" :loading="movingCandidates" @click="confirmMoveCandidates">
          确认移动
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showReplaceDialog" title="替换考官" width="500px">
      <el-form label-width="100px">
        <el-form-item label="当前考官">
          <el-select
            v-model="oldExaminerId"
            placeholder="请选择要替换的考官"
            style="width: 100%">
            <el-option
              v-for="examiner in selectedAssignment?.examiners"
              :key="examiner.id"
              :label="examiner.name"
              :value="examiner.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="新考官">
          <el-select
            v-model="newExaminerId"
            placeholder="请选择新考官"
            filterable
            style="width: 100%">
            <el-option
              v-for="examiner in availableExaminers"
              :key="examiner.id"
              :label="examiner.name"
              :value="examiner.id">
              <span>{{ examiner.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px">
                {{ examiner.major }}
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input
            v-model="replaceReason"
            type="textarea"
            :rows="3"
            placeholder="请说明调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReplaceDialog = false">取消</el-button>
        <el-button type="primary" :loading="replacingExaminer" @click="confirmReplaceExaminer">
          确认替换
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRoomDialog" title="调整考场" width="500px">
      <el-form label-width="100px">
        <el-form-item label="当前考场">
          <el-input :value="selectedAssignment?.roomName" disabled />
        </el-form-item>
        <el-form-item label="备用考场">
          <el-select
            v-model="targetRoomId"
            placeholder="请选择备用考场"
            style="width: 100%">
            <el-option
              v-for="room in availableRooms"
              :key="room.id"
              :label="room.name"
              :value="room.id">
              <span>{{ room.name }}</span>
              <span style="float: right; color: #8492a6; font-size: 12px">
                容量: {{ room.capacity }}人
              </span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="调整原因">
          <el-input
            v-model="roomReason"
            type="textarea"
            :rows="3"
            placeholder="请说明调整原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoomDialog = false">取消</el-button>
        <el-button type="primary" :loading="adjustingRoom" @click="confirmAdjustRoom">
          确认调整
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="调整详情" width="600px">
      <el-descriptions :column="1" border v-if="currentHistoryDetail">
        <el-descriptions-item label="调整时间">
          {{ formatDateTime(currentHistoryDetail.createTime) }}
        </el-descriptions-item>
        <el-descriptions-item label="调整类型">
          <el-tag :type="getAdjustmentTypeTag(currentHistoryDetail.adjustmentType)" size="small">
            {{ getAdjustmentTypeName(currentHistoryDetail.adjustmentType) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">
          {{ currentHistoryDetail.operatorName }}
        </el-descriptions-item>
        <el-descriptions-item label="原始信息">
          {{ currentHistoryDetail.originalInfo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="调整后信息">
          {{ currentHistoryDetail.newInfo || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="影响人数">
          {{ currentHistoryDetail.affectedCount || 0 }} 人
        </el-descriptions-item>
        <el-descriptions-item label="原因说明">
          {{ currentHistoryDetail.reason || '-' }}
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  List, Location, User, Avatar, Switch, Refresh, OfficeBuilding,
  MagicStick, Check
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import api from '@/api'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const projectId = ref(route.params.projectId)
const activeTab = ref('assignment')
const historyFilter = ref(null)

const plans = ref([])
const selectedPlanId = ref(null)
const assignments = ref([])
const selectedAssignment = ref(null)
const selectedCandidates = ref([])

const showMoveDialog = ref(false)
const showReplaceDialog = ref(false)
const showRoomDialog = ref(false)
const showDetailDialog = ref(false)

const targetAssignmentId = ref(null)
const moveReason = ref('')
const movingCandidates = ref(false)

const oldExaminerId = ref(null)
const newExaminerId = ref(null)
const replaceReason = ref('')
const replacingExaminer = ref(false)

const targetRoomId = ref(null)
const roomReason = ref('')
const adjustingRoom = ref(false)

const allExaminers = ref([])
const allCandidates = ref([])
const allRooms = ref([])

const emergencyForm = reactive({
  emergencyType: '',
  examinerId: null,
  candidateId: null,
  roomId: null,
  affectedAssignmentIds: [],
  reason: ''
})
const emergencyPlan = ref(null)
const applyingEmergency = ref(false)

const historyList = ref([])
const currentHistoryDetail = ref(null)

const impactAssessment = reactive({
  visible: false,
  title: '',
  type: 'info',
  affectedCount: 0,
  hasConflict: false,
  suggestion: ''
})

const userInfo = computed(() => userStore.userInfo)

const otherAssignments = computed(() => {
  if (!selectedAssignment.value) return []
  return assignments.value.filter(a => a.id !== selectedAssignment.value.id)
})

const availableExaminers = computed(() => {
  if (!selectedAssignment.value?.examiners) return allExaminers.value
  const currentExaminerIds = selectedAssignment.value.examiners.map(e => e.id)
  return allExaminers.value.filter(e => !currentExaminerIds.includes(e.id))
})

const availableRooms = computed(() => {
  if (!selectedAssignment.value?.roomId) return allRooms.value
  return allRooms.value.filter(r => r.id !== selectedAssignment.value.roomId)
})

const isAllCandidatesSelected = computed(() => {
  if (!selectedAssignment.value?.candidates?.length) return false
  return selectedCandidates.value.length === selectedAssignment.value.candidates.length
})

const isIndeterminate = computed(() => {
  const count = selectedCandidates.value.length
  return count > 0 && count < (selectedAssignment.value?.candidates?.length || 0)
})

const formatDateTime = (datetime) => {
  if (!datetime) return '-'
  return dayjs(datetime).format('YYYY-MM-DD HH:mm:ss')
}

const getAdjustmentTypeName = (type) => {
  const names = {
    MOVE_CANDIDATE: '移动考生',
    REPLACE_EXAMINER: '替换考官',
    ADJUST_ROOM: '调整考场',
    EMERGENCY: '应急处理'
  }
  return names[type] || type || '未知'
}

const getAdjustmentTypeTag = (type) => {
  const types = {
    MOVE_CANDIDATE: 'primary',
    REPLACE_EXAMINER: 'warning',
    ADJUST_ROOM: 'success',
    EMERGENCY: 'danger'
  }
  return types[type] || 'info'
}

const getCandidateName = (id) => {
  const candidate = selectedAssignment.value?.candidates?.find(c => c.id === id)
  return candidate?.name || '未知'
}

const toggleAllCandidates = (val) => {
  if (!selectedAssignment.value?.candidates) return
  if (val) {
    selectedCandidates.value = selectedAssignment.value.candidates.map(c => c.id)
  } else {
    selectedCandidates.value = []
  }
}

const loadPlans = async () => {
  try {
    const res = await api.scheduling.listPlans(projectId.value)
    plans.value = res.data?.records || res.data || []
    if (plans.value.length > 0) {
      const executionPlan = plans.value.find(p => p.isExecution)
      selectedPlanId.value = executionPlan?.id || plans.value[0].id
      await loadAssignmentDetails(selectedPlanId.value)
    }
  } catch (e) {
    ElMessage.error('加载方案列表失败')
  }
}

const handlePlanChange = async (planId) => {
  selectedAssignment.value = null
  selectedCandidates.value = []
  await loadAssignmentDetails(planId)
}

const loadAssignmentDetails = async (planId) => {
  if (!planId) return
  try {
    const res = await api.scheduling.getAssignmentDetails(planId)
    assignments.value = res.data?.records || res.data || []
  } catch (e) {
    ElMessage.error('加载分配详情失败')
  }
}

const selectAssignment = (assignment) => {
  selectedAssignment.value = assignment
  selectedCandidates.value = []
}

const showImpactAssessment = async (data) => {
  try {
    const res = await api.adjustment.getAffectedCount(data)
    const result = res.data || res
    impactAssessment.visible = true
    impactAssessment.title = '影响评估'
    impactAssessment.affectedCount = result.affectedCount || 0
    impactAssessment.hasConflict = result.hasConflict || false
    impactAssessment.suggestion = result.suggestion || ''
    impactAssessment.type = result.hasConflict ? 'warning' : 'success'
  } catch (e) {
    console.error('获取影响评估失败', e)
  }
}

const confirmMoveCandidates = async () => {
  if (selectedCandidates.value.length === 0) {
    ElMessage.warning('请选择要移动的考生')
    return
  }
  if (!targetAssignmentId.value) {
    ElMessage.warning('请选择目标场次')
    return
  }

  const data = {
    planId: selectedPlanId.value,
    fromAssignmentId: selectedAssignment.value.id,
    toAssignmentId: targetAssignmentId.value,
    candidateIds: selectedCandidates.value,
    adjustmentType: 'MOVE_CANDIDATE',
    reason: moveReason.value,
    operatorId: userInfo.value?.id,
    operatorName: userInfo.value?.realName || userInfo.value?.username
  }

  movingCandidates.value = true
  try {
    await ElMessageBox.confirm(
      `确定将 ${selectedCandidates.value.length} 名考生移动到目标场次？`,
      '确认移动',
      { type: 'warning' }
    )
    await api.adjustment.moveCandidates(data)
    ElMessage.success('考生移动成功')
    showMoveDialog.value = false
    moveReason.value = ''
    targetAssignmentId.value = null
    selectedCandidates.value = []
    await loadAssignmentDetails(selectedPlanId.value)
    await showImpactAssessment(data)
    await loadHistory()
    await api.adjustment.revalidate(selectedPlanId.value)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('移动失败')
    }
  } finally {
    movingCandidates.value = false
  }
}

const confirmReplaceExaminer = async () => {
  if (!oldExaminerId.value) {
    ElMessage.warning('请选择要替换的考官')
    return
  }
  if (!newExaminerId.value) {
    ElMessage.warning('请选择新考官')
    return
  }

  const data = {
    planId: selectedPlanId.value,
    fromAssignmentId: selectedAssignment.value.id,
    oldExaminerId: oldExaminerId.value,
    newExaminerId: newExaminerId.value,
    adjustmentType: 'REPLACE_EXAMINER',
    reason: replaceReason.value,
    operatorId: userInfo.value?.id,
    operatorName: userInfo.value?.realName || userInfo.value?.username
  }

  replacingExaminer.value = true
  try {
    const oldExaminer = selectedAssignment.value.examiners.find(e => e.id === oldExaminerId.value)
    const newExaminer = availableExaminers.value.find(e => e.id === newExaminerId.value)
    await ElMessageBox.confirm(
      `确定将考官 "${oldExaminer?.name}" 替换为 "${newExaminer?.name}"？`,
      '确认替换',
      { type: 'warning' }
    )
    await api.adjustment.replaceExaminer(data)
    ElMessage.success('考官替换成功')
    showReplaceDialog.value = false
    replaceReason.value = ''
    oldExaminerId.value = null
    newExaminerId.value = null
    await loadAssignmentDetails(selectedPlanId.value)
    await showImpactAssessment(data)
    await loadHistory()
    await api.adjustment.revalidate(selectedPlanId.value)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('替换失败')
    }
  } finally {
    replacingExaminer.value = false
  }
}

const confirmAdjustRoom = async () => {
  if (!targetRoomId.value) {
    ElMessage.warning('请选择备用考场')
    return
  }

  const data = {
    planId: selectedPlanId.value,
    fromAssignmentId: selectedAssignment.value.id,
    toAssignmentId: selectedAssignment.value.id,
    adjustmentType: 'ADJUST_ROOM',
    reason: roomReason.value,
    operatorId: userInfo.value?.id,
    operatorName: userInfo.value?.realName || userInfo.value?.username
  }

  adjustingRoom.value = true
  try {
    const targetRoom = availableRooms.value.find(r => r.id === targetRoomId.value)
    await ElMessageBox.confirm(
      `确定将考场调整为 "${targetRoom?.name}"？`,
      '确认调整',
      { type: 'warning' }
    )
    await api.adjustment.adjust(data)
    ElMessage.success('考场调整成功')
    showRoomDialog.value = false
    roomReason.value = ''
    targetRoomId.value = null
    await loadAssignmentDetails(selectedPlanId.value)
    await showImpactAssessment(data)
    await loadHistory()
    await api.adjustment.revalidate(selectedPlanId.value)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('调整失败')
    }
  } finally {
    adjustingRoom.value = false
  }
}

const loadExaminers = async () => {
  try {
    const res = await api.examiners.list({ projectId: projectId.value })
    allExaminers.value = res.data?.records || res.data || []
  } catch (e) {
    console.error('加载考官列表失败', e)
  }
}

const loadCandidates = async () => {
  try {
    const res = await api.candidates.list({ projectId: projectId.value })
    allCandidates.value = res.data?.records || res.data || []
  } catch (e) {
    console.error('加载考生列表失败', e)
  }
}

const loadRooms = async () => {
  try {
    const res = await api.rooms.list({ projectId: projectId.value })
    allRooms.value = res.data?.records || res.data || []
  } catch (e) {
    console.error('加载考场列表失败', e)
  }
}

const generateEmergencyPlan = async () => {
  if (!emergencyForm.emergencyType) {
    ElMessage.warning('请选择应急类型')
    return
  }
  if (emergencyForm.affectedAssignmentIds.length === 0) {
    ElMessage.warning('请选择受影响的场次')
    return
  }

  try {
    const typeNames = {
      EXAMINER_LEAVE: '考官临时请假',
      CANDIDATE_ABSENT: '考生缺席',
      ROOM_FAILURE: '考场设备故障',
      OTHER: '其他突发情况'
    }

    emergencyPlan.value = {
      affectedScope: `${typeNames[emergencyForm.emergencyType]}，影响 ${emergencyForm.affectedAssignmentIds.length} 个场次`,
      estimatedTime: 30,
      actions: [
        '立即通知相关人员调整安排',
        '协调备用资源进行替换',
        '更新调度方案并重新发布',
        '记录调整日志并通知所有相关方'
      ]
    }

    ElMessage.success('应急方案已生成')
  } catch (e) {
    ElMessage.error('生成应急方案失败')
  }
}

const applyEmergencyAdjustment = async () => {
  const data = {
    projectId: projectId.value,
    planId: selectedPlanId.value,
    emergencyType: emergencyForm.emergencyType,
    examinerId: emergencyForm.examinerId,
    roomId: emergencyForm.roomId,
    candidateId: emergencyForm.candidateId,
    affectedAssignmentIds: emergencyForm.affectedAssignmentIds,
    reason: emergencyForm.reason,
    operatorId: userInfo.value?.id,
    operatorName: userInfo.value?.realName || userInfo.value?.username
  }

  applyingEmergency.value = true
  try {
    await ElMessageBox.confirm(
      '确定应用此应急调整方案？此操作将影响多个场次。',
      '确认应用',
      { type: 'warning' }
    )
    await api.adjustment.emergency(data)
    ElMessage.success('应急调整已应用')
    emergencyPlan.value = null
    Object.assign(emergencyForm, {
      emergencyType: '',
      examinerId: null,
      candidateId: null,
      roomId: null,
      affectedAssignmentIds: [],
      reason: ''
    })
    await loadAssignmentDetails(selectedPlanId.value)
    await showImpactAssessment(data)
    await loadHistory()
    await api.adjustment.revalidate(selectedPlanId.value)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('应用失败')
    }
  } finally {
    applyingEmergency.value = false
  }
}

const loadHistory = async () => {
  try {
    const res = await api.adjustment.getHistory(projectId.value, historyFilter.value)
    historyList.value = res.data?.records || res.data || []
  } catch (e) {
    ElMessage.error('加载调整历史失败')
  }
}

const showHistoryDetail = (row) => {
  currentHistoryDetail.value = row
  showDetailDialog.value = true
}

watch(activeTab, (val) => {
  if (val === 'history') {
    loadHistory()
  }
})

onMounted(() => {
  loadPlans()
  loadExaminers()
  loadCandidates()
  loadRooms()
  loadHistory()
})
</script>

<style scoped lang="scss">
.schedule-adjustment {
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

  .plan-selector-card {
    margin-bottom: 20px;

    .plan-form {
      margin: 0;
    }
  }

  .impact-alert {
    margin-bottom: 20px;

    .impact-content {
      display: flex;
      flex-wrap: wrap;
      gap: 24px;
      margin-top: 8px;

      .impact-item {
        .label {
          color: #606266;
        }

        .value {
          font-weight: 500;

          &.has-conflict {
            color: #f56c6c;
          }

          &.suggestion {
            color: #e6a23c;
          }
        }
      }
    }
  }

  .tabs-card {
    .assignment-container {
      display: flex;
      gap: 20px;
      height: 600px;

      .session-list {
        width: 320px;
        flex-shrink: 0;
        border-right: 1px solid #e4e7ed;
        padding-right: 20px;
        display: flex;
        flex-direction: column;

        .list-header {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: bold;
          margin-bottom: 16px;
          padding-bottom: 12px;
          border-bottom: 1px solid #e4e7ed;
        }

        .sessions {
          flex: 1;
          overflow-y: auto;

          .session-item {
            padding: 12px;
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

            .session-name {
              font-weight: 500;
              color: #303133;
              margin-bottom: 8px;
            }

            .session-info {
              display: flex;
              flex-direction: column;
              gap: 4px;

              .info-item {
                display: flex;
                align-items: center;
                gap: 4px;
                font-size: 12px;
                color: #909399;

                .el-icon {
                  font-size: 12px;
                }
              }
            }
          }
        }
      }

      .assignment-detail {
        flex: 1;
        overflow-y: auto;

        .empty-detail {
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
        }

        .detail-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
          padding-bottom: 12px;
          border-bottom: 1px solid #e4e7ed;

          h3 {
            margin: 0;
            font-size: 16px;
          }

          .action-buttons {
            display: flex;
            gap: 8px;
          }
        }

        .detail-content {
          display: flex;
          gap: 20px;
          height: calc(100% - 80px);

          .candidates-section,
          .examiners-section {
            flex: 1;
            display: flex;
            flex-direction: column;

            .section-header {
              display: flex;
              justify-content: space-between;
              align-items: center;
              font-weight: 500;
              margin-bottom: 12px;
              padding-bottom: 8px;
              border-bottom: 1px solid #e4e7ed;
            }

            .empty-state {
              flex: 1;
              display: flex;
              align-items: center;
              justify-content: center;
            }
          }

          .candidates-section {
            .candidate-list {
              flex: 1;
              overflow-y: auto;
              display: flex;
              flex-direction: column;
              gap: 8px;

              .candidate-item {
                padding: 8px 12px;
                border: 1px solid #e4e7ed;
                border-radius: 6px;
                display: flex;
                justify-content: space-between;
                align-items: center;

                .candidate-name {
                  font-weight: 500;
                }

                .candidate-position {
                  font-size: 12px;
                  color: #909399;
                }
              }
            }
          }

          .examiners-section {
            .examiner-list {
              flex: 1;
              overflow-y: auto;
              display: flex;
              flex-direction: column;
              gap: 12px;

              .examiner-item {
                display: flex;
                align-items: center;
                gap: 12px;
                padding: 12px;
                border: 1px solid #e4e7ed;
                border-radius: 8px;

                .examiner-avatar {
                  width: 40px;
                  height: 40px;
                  border-radius: 50%;
                  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                  color: white;
                  display: flex;
                  align-items: center;
                  justify-content: center;
                  font-weight: bold;
                  font-size: 16px;
                }

                .examiner-info {
                  .examiner-name {
                    font-weight: 500;
                    color: #303133;
                  }

                  .examiner-major {
                    font-size: 12px;
                    color: #909399;
                  }
                }
              }
            }
          }
        }
      }
    }

    .emergency-container {
      .emergency-form {
        max-width: 800px;
      }

      .emergency-plan {
        margin-top: 20px;

        h3 {
          margin: 0 0 16px 0;
          font-size: 16px;
        }

        .action-item {
          margin-bottom: 8px;
        }

        .plan-actions {
          margin-top: 20px;
          text-align: right;
        }
      }
    }

    .history-container {
      .history-toolbar {
        display: flex;
        gap: 12px;
        margin-bottom: 16px;
      }
    }
  }

  .candidate-tag {
    margin-right: 8px;
    margin-bottom: 8px;
  }
}
</style>
