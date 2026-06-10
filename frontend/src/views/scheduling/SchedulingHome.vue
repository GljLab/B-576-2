<template>
  <div class="scheduling-home">
    <el-card class="header-card">
      <template #header>
        <div class="card-header">
          <span class="title">智能面试场次编排与资源调度</span>
          <span class="subtitle">支持多天多场次复杂面试项目编排，考虑多维度约束生成最优调度方案</span>
        </div>
      </template>
      <div class="quick-start">
        <h3>选择项目开始编排</h3>
        <el-select
          v-model="selectedProjectId"
          placeholder="请选择面试项目"
          filterable
          clearable
          style="width: 400px"
          @change="onProjectChange">
          <el-option
            v-for="project in projects"
            :key="project.id"
            :label="project.name"
            :value="project.id" />
        </el-select>
        <el-button type="primary" :disabled="!selectedProjectId" @click="goToSessions">
          开始编排
        </el-button>
      </div>
    </el-card>

    <el-row :gutter="20" class="function-cards">
      <el-col :span="8">
        <el-card class="func-card" shadow="hover">
          <div class="func-icon">
            <el-icon :size="40" color="#409EFF"><Calendar /></el-icon>
          </div>
          <h3>场次配置</h3>
          <p>支持创建多时间段面试场次，设置考场范围、考生容量、间隔时间等</p>
          <el-button
            type="primary"
            plain
            :disabled="!selectedProjectId"
            @click="goToSessions">
            配置场次
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="func-card" shadow="hover">
          <div class="func-icon">
            <el-icon :size="40" color="#67C23A"><User /></el-icon>
          </div>
          <h3>考官可用性</h3>
          <p>日历视图管理考官时间可用性，记录历史工作负荷，支持特殊约束设置</p>
          <el-button
            type="success"
            plain
            :disabled="!selectedProjectId"
            @click="goToAvailability">
            管理可用性
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="func-card" shadow="hover">
          <div class="func-icon">
            <el-icon :size="40" color="#E6A23C"><MagicStick /></el-icon>
          </div>
          <h3>智能调度</h3>
          <p>多目标优化算法，支持多种调度策略，自动生成最优场次分配方案</p>
          <el-button
            type="warning"
            plain
            :disabled="!selectedProjectId"
            @click="goToPlan">
            生成方案
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="func-card" shadow="hover">
          <div class="func-icon">
            <el-icon :size="40" color="#F56C6C"><Tools /></el-icon>
          </div>
          <h3>动态调整</h3>
          <p>支持手动微调方案，突发情况应急处理，自动校验约束冲突</p>
          <el-button
            type="danger"
            plain
            :disabled="!selectedProjectId"
            @click="goToAdjustment">
            调整方案
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="func-card" shadow="hover">
          <div class="func-icon">
            <el-icon :size="40" color="#909399"><DataLine /></el-icon>
          </div>
          <h3>调度仪表盘</h3>
          <p>实时展示各场次执行状态，统计分析报表，Excel导出打印</p>
          <el-button
            type="info"
            plain
            :disabled="!selectedProjectId"
            @click="goToDashboard">
            查看仪表盘
          </el-button>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card class="func-card" shadow="hover">
          <div class="func-icon">
            <el-icon :size="40" color="#722ED1"><Histogram /></el-icon>
          </div>
          <h3>统计报表</h3>
          <p>场次编排总结报告，考场使用频次统计，考官工作时长分析</p>
          <el-button
            type="primary"
            plain
            :disabled="!selectedProjectId"
            @click="exportReport">
            导出报表
          </el-button>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="feature-intro">
      <template #header>
        <span>核心功能特性</span>
      </template>
      <el-row :gutter="20">
        <el-col :span="6" v-for="feature in features" :key="feature.title">
          <div class="feature-item">
            <el-icon :size="24" :color="feature.color">
              <component :is="feature.icon" />
            </el-icon>
            <div class="feature-content">
              <h4>{{ feature.title }}</h4>
              <p>{{ feature.desc }}</p>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  Calendar, User, MagicStick, Tools, DataLine, Histogram,
  Clock, House, Avatar, Location, Warning, Document
} from '@element-plus/icons-vue'
import api from '@/api'

const router = useRouter()
const projects = ref([])
const selectedProjectId = ref(null)

const features = [
  { title: '多场次编排', icon: 'Clock', color: '#409EFF', desc: '支持多天上午/下午多场次配置，灵活设置时间间隔' },
  { title: '考场资源优化', icon: 'House', color: '#67C23A', desc: '智能分配考场资源，最大化利用率，避免资源浪费' },
  { title: '考官负荷均衡', icon: 'Avatar', color: '#E6A23C', desc: '自动平衡考官工作负荷，避免连续疲劳，确保公平性' },
  { title: '多策略调度', icon: 'Location', color: '#F56C6C', desc: '支持平衡负荷、压缩时间、职位优先等多种策略' },
  { title: '冲突智能检测', icon: 'Warning', color: '#909399', desc: '自动检测资源冲突，给出明确提示和调整建议' },
  { title: '追溯与审计', icon: 'Document', color: '#722ED1', desc: '所有操作留痕记录，支持完整的追溯审计' }
]

onMounted(() => {
  loadProjects()
})

const loadProjects = async () => {
  try {
    const res = await api.projects.list({ page: 1, size: 100 })
    projects.value = res.data?.records || res.data || []
  } catch (e) {
    ElMessage.error('加载项目列表失败')
  }
}

const onProjectChange = (val) => {
  selectedProjectId.value = val
}

const goToSessions = () => {
  if (selectedProjectId.value) {
    router.push(`/scheduling/${selectedProjectId.value}/sessions`)
  }
}

const goToAvailability = () => {
  if (selectedProjectId.value) {
    router.push(`/scheduling/${selectedProjectId.value}/availability`)
  }
}

const goToPlan = () => {
  if (selectedProjectId.value) {
    router.push(`/scheduling/${selectedProjectId.value}/plan`)
  }
}

const goToAdjustment = () => {
  if (selectedProjectId.value) {
    router.push(`/scheduling/${selectedProjectId.value}/adjustment`)
  }
}

const goToDashboard = () => {
  if (selectedProjectId.value) {
    router.push(`/scheduling/${selectedProjectId.value}/dashboard`)
  }
}

const exportReport = async () => {
  if (!selectedProjectId.value) return
  try {
    const blob = await api.report.exportExcel(selectedProjectId.value)
    const url = window.URL.createObjectURL(new Blob([blob]))
    const link = document.createElement('a')
    link.href = url
    link.download = '面试场次安排.xlsx'
    link.click()
    window.URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error('导出失败')
  }
}
</script>

<style scoped lang="scss">
.scheduling-home {
  padding: 20px;

  .header-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      flex-direction: column;
      gap: 8px;

      .title {
        font-size: 20px;
        font-weight: bold;
        color: #303133;
      }

      .subtitle {
        font-size: 14px;
        color: #909399;
      }
    }

    .quick-start {
      display: flex;
      align-items: center;
      gap: 16px;
      margin-top: 16px;

      h3 {
        margin: 0;
        font-size: 16px;
        color: #606266;
      }
    }
  }

  .function-cards {
    margin-bottom: 20px;

    .func-card {
      text-align: center;
      margin-bottom: 20px;

      .func-icon {
        margin-bottom: 16px;
      }

      h3 {
        font-size: 16px;
        color: #303133;
        margin-bottom: 8px;
      }

      p {
        font-size: 13px;
        color: #909399;
        line-height: 1.6;
        margin-bottom: 16px;
        min-height: 42px;
      }
    }
  }

  .feature-intro {
    .feature-item {
      display: flex;
      align-items: flex-start;
      gap: 12px;
      padding: 12px 0;

      .feature-content {
        flex: 1;

        h4 {
          font-size: 14px;
          color: #303133;
          margin: 0 0 4px 0;
        }

        p {
          font-size: 12px;
          color: #909399;
          margin: 0;
          line-height: 1.5;
        }
      }
    }
  }
}
</style>
