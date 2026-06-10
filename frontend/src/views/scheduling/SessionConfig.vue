<template>
  <div class="session-config page-container">
    <el-breadcrumb separator="/" class="breadcrumb">
      <el-breadcrumb-item :to="{ path: '/scheduling' }">智能调度</el-breadcrumb-item>
      <el-breadcrumb-item>场次配置</el-breadcrumb-item>
    </el-breadcrumb>

    <div class="page-header">
      <span class="title">场次配置</span>
      <div class="header-actions">
        <el-button type="success" @click="handleGenerateDefault" :loading="generating">
          <el-icon><MagicStick /></el-icon>生成默认场次
        </el-button>
        <el-button type="primary" @click="showDialog()">
          <el-icon><Plus /></el-icon>新增场次
        </el-button>
        <el-upload
          :action="uploadUrl"
          :headers="uploadHeaders"
          :data="{ projectId }"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :on-success="onUploadSuccess"
          :on-error="onUploadError"
          accept=".xlsx,.xls"
        >
          <el-button>
            <el-icon><Upload /></el-icon>批量导入
          </el-button>
        </el-upload>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索场次名称"
        style="width: 280px"
        clearable
        @keyup.enter="loadData"
        @clear="loadData"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" @click="loadData">
        <el-icon><Search /></el-icon>搜索
      </el-button>
      <el-button @click="resetSearch">重置</el-button>
    </div>

    <el-table
      :data="tableData"
      v-loading="loading"
      stripe
      border
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column prop="sessionName" label="场次名称" min-width="160" show-overflow-tooltip />
      <el-table-column prop="sessionDate" label="日期" width="130">
        <template #default="{ row }">
          <span>{{ formatDate(row.sessionDate) }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="100" />
      <el-table-column prop="endTime" label="结束时间" width="100" />
      <el-table-column prop="capacity" label="考场容量" width="100">
        <template #default="{ row }">
          <el-tag type="info">{{ row.capacity || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="exclusivePositions" label="专属职位" min-width="180">
        <template #default="{ row }">
          <div class="position-tags">
            <el-tag
              v-for="pos in getPositionNames(row.exclusivePositions)"
              :key="pos"
              size="small"
              type="primary"
              effect="plain"
              style="margin-right: 4px; margin-bottom: 4px"
            >
              {{ pos }}
            </el-tag>
            <span v-if="!row.exclusivePositions || row.exclusivePositions.length === 0" class="text-muted">
              全部职位
            </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.status)">
            {{ getStatusText(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link size="small" @click="showDialog(row)">
            <el-icon><Edit /></el-icon>编辑
          </el-button>
          <el-popconfirm title="确定删除该场次吗？" @confirm="handleDelete(row.id)">
            <template #reference>
              <el-button type="danger" link size="small">
                <el-icon><Delete /></el-icon>删除
              </el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrapper">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑场次' : '新增场次'"
      width="700px"
      :close-on-click-modal="false"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        @submit.prevent
      >
        <el-form-item label="场次名称" prop="sessionName">
          <el-input
            v-model="form.sessionName"
            placeholder="请输入场次名称，如：上午第一场"
            maxlength="50"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="日期" prop="sessionDate">
              <el-date-picker
                v-model="form.sessionDate"
                type="date"
                placeholder="选择日期"
                style="width: 100%"
                value-format="YYYY-MM-DD"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker
                v-model="form.startTime"
                placeholder="选择开始时间"
                style="width: 100%"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker
                v-model="form.endTime"
                placeholder="选择结束时间"
                style="width: 100%"
                format="HH:mm"
                value-format="HH:mm"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="预估考生人数上限">
              <el-input-number
                v-model="form.capacity"
                :min="1"
                :max="1000"
                placeholder="请输入预估人数"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="考场间隔时间(分钟)">
              <el-input-number
                v-model="form.intervalMinutes"
                :min="0"
                :max="120"
                placeholder="请输入间隔时间"
                style="width: 100%"
              />
              <div class="form-tip">考生进入考场的间隔时间</div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="可用考场">
          <el-select
            v-model="form.roomIds"
            multiple
            filterable
            placeholder="请选择可用考场"
            style="width: 100%"
          >
            <el-option
              v-for="room in roomList"
              :key="room.id"
              :label="`${room.roomName} (容量: ${room.capacity}人)`"
              :value="room.id"
            />
          </el-select>
          <div class="form-tip">选择该场次可用的考场，不选则使用全部可用考场</div>
        </el-form-item>

        <el-form-item label="专属职位">
          <el-select
            v-model="form.exclusivePositions"
            multiple
            filterable
            placeholder="请选择专属职位（可选）"
            style="width: 100%"
          >
            <el-option
              v-for="pos in positionList"
              :key="pos.id"
              :label="pos.positionName"
              :value="pos.id"
            />
          </el-select>
          <div class="form-tip">设置后该场次仅安排选中职位的考生，不选则所有职位均可安排</div>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus, Upload, Search, Edit, Delete, MagicStick
} from '@element-plus/icons-vue'
import dayjs from 'dayjs'
import api from '@/api'

const route = useRoute()
const router = useRouter()

const projectId = ref(Number(route.params.projectId))

const loading = ref(false)
const generating = ref(false)
const submitting = ref(false)
const tableData = ref([])
const allData = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)
const searchKeyword = ref('')
const selectedRows = ref([])

const roomList = ref([])
const positionList = ref([])

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref()

const form = reactive({
  id: null,
  projectId: null,
  sessionName: '',
  sessionDate: '',
  startTime: '',
  endTime: '',
  capacity: 30,
  intervalMinutes: 5,
  roomIds: [],
  exclusivePositions: []
})

const rules = {
  sessionName: [
    { required: true, message: '请输入场次名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  sessionDate: [
    { required: true, message: '请选择日期', trigger: 'change' }
  ],
  startTime: [
    { required: true, message: '请选择开始时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择结束时间', trigger: 'change' }
  ]
}

const uploadUrl = computed(() => {
  return '/api/scheduling/sessions/batch-import'
})

const uploadHeaders = computed(() => {
  const token = localStorage.getItem('token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})

const getStatusType = (status) => {
  const types = { 0: 'info', 1: 'success', 2: 'warning' }
  return types[status] || 'info'
}

const getStatusText = (status) => {
  const texts = { 0: '未开始', 1: '进行中', 2: '已完成' }
  return texts[status] || '未知'
}

const formatDate = (date) => {
  if (!date) return '-'
  return dayjs(date).format('YYYY-MM-DD')
}

const getPositionNames = (positionIds) => {
  if (!positionIds || positionIds.length === 0) return []
  return positionIds.map(id => {
    const pos = positionList.value.find(p => p.id === id)
    return pos ? pos.positionName : id
  })
}

const loadRooms = async () => {
  try {
    const res = await api.rooms.getByProject(projectId.value)
    if (res.code === 200) {
      roomList.value = res.data || []
    }
  } catch (error) {
    console.error('加载考场列表失败', error)
  }
}

const loadPositions = async () => {
  try {
    const res = await api.positions.getByProject(projectId.value)
    if (res.code === 200) {
      positionList.value = res.data || []
    }
  } catch (error) {
    console.error('加载职位列表失败', error)
  }
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await api.sessions.list(projectId.value)
    if (res.code === 200) {
      allData.value = res.data || []
      
      let filtered = [...allData.value]
      if (searchKeyword.value) {
        const keyword = searchKeyword.value.toLowerCase()
        filtered = filtered.filter(item =>
          item.sessionName?.toLowerCase().includes(keyword)
        )
      }
      
      total.value = filtered.length
      
      const start = (pageNum.value - 1) * pageSize.value
      const end = start + pageSize.value
      tableData.value = filtered.slice(start, end)
    }
  } catch (error) {
    ElMessage.error('加载场次列表失败')
    console.error('加载失败', error)
  }
  loading.value = false
}

const resetSearch = () => {
  searchKeyword.value = ''
  pageNum.value = 1
  loadData()
}

const handleSelectionChange = (val) => {
  selectedRows.value = val
}

const handleGenerateDefault = async () => {
  try {
    await ElMessageBox.confirm(
      '生成默认场次将根据项目信息自动创建标准场次配置，是否继续？',
      '提示',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    generating.value = true
    const res = await api.sessions.generateDefault(projectId.value)
    if (res.code === 200) {
      ElMessage.success('默认场次生成成功')
      loadData()
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('生成默认场次失败')
    }
  }
  generating.value = false
}

const showDialog = (row = null) => {
  isEdit.value = !!row
  if (row) {
    Object.assign(form, {
      id: row.id,
      projectId: projectId.value,
      sessionName: row.sessionName,
      sessionDate: row.sessionDate,
      startTime: row.startTime,
      endTime: row.endTime,
      capacity: row.capacity || 30,
      intervalMinutes: row.intervalMinutes || 5,
      roomIds: row.roomIds || [],
      exclusivePositions: row.exclusivePositions || []
    })
  } else {
    Object.assign(form, {
      id: null,
      projectId: projectId.value,
      sessionName: '',
      sessionDate: '',
      startTime: '',
      endTime: '',
      capacity: 30,
      intervalMinutes: 5,
      roomIds: [],
      exclusivePositions: []
    })
  }
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  if (form.startTime && form.endTime && form.startTime >= form.endTime) {
    ElMessage.warning('结束时间必须晚于开始时间')
    return
  }

  submitting.value = true
  try {
    const res = isEdit.value
      ? await api.sessions.update(form)
      : await api.sessions.create(form)

    if (res.code === 200) {
      ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
      dialogVisible.value = false
      loadData()
    }
  } catch (error) {
    ElMessage.error('操作失败')
    console.error('操作失败', error)
  }
  submitting.value = false
}

const handleDelete = async (id) => {
  try {
    const res = await api.sessions.delete(id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadData()
    }
  } catch (error) {
    ElMessage.error('删除失败')
    console.error('删除失败', error)
  }
}

const beforeUpload = (file) => {
  const isExcel = file.name.endsWith('.xlsx') || file.name.endsWith('.xls')
  if (!isExcel) {
    ElMessage.error('只能上传 Excel 文件!')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

const onUploadSuccess = (res) => {
  if (res.code === 200) {
    ElMessage.success(`导入成功，共导入 ${res.data?.count || 0} 条数据`)
    loadData()
  } else {
    ElMessage.error(res.message || '导入失败')
  }
}

const onUploadError = () => {
  ElMessage.error('导入失败，请检查文件格式')
}

onMounted(() => {
  loadRooms()
  loadPositions()
  loadData()
})
</script>

<style lang="scss" scoped>
.session-config {
  .breadcrumb {
    margin-bottom: 16px;
  }

  .page-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 20px;
    border-bottom: 1px solid #ebeef5;

    .title {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
    }

    .header-actions {
      display: flex;
      gap: 12px;
    }
  }

  .search-bar {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    flex-wrap: wrap;
  }

  .position-tags {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }

  .pagination-wrapper {
    display: flex;
    justify-content: flex-end;
    margin-top: 20px;
  }

  .text-muted {
    color: #909399;
    font-size: 13px;
  }
}

:deep(.el-table) {
  .el-table__cell {
    padding: 12px 0;
  }

  th.el-table__cell {
    background: #f8f9fa !important;
    color: #606266;
    font-weight: 600;
  }
}
</style>
