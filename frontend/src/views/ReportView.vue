<template>
  <div class="report-page">
    <div class="filter-section">
      <div class="filter-row">
        <div class="time-range-group">
          <el-radio-group v-model="timeRange" size="large" @change="handleTimeRangeChange">
            <el-radio-button value="WEEK">本周</el-radio-button>
            <el-radio-button value="MONTH">本月</el-radio-button>
            <el-radio-button value="CUSTOM">自定义</el-radio-button>
          </el-radio-group>
        </div>

        <div v-if="timeRange === 'CUSTOM'" class="date-picker-group">
          <el-date-picker
            v-model="customDateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            size="large"
            :disabled-date="disabledDate"
          />
        </div>

        <el-button type="primary" size="large" :loading="loading" @click="generateReport">
          <el-icon><Refresh /></el-icon>
          生成报告
        </el-button>
      </div>
    </div>

    <div v-if="reportData" class="report-content">
      <div ref="reportRef" class="report-area">
        <div class="report-header">
          <div class="report-title">
            <el-icon class="title-icon"><DataAnalysis /></el-icon>
            <span>WordMind 学习报告</span>
          </div>
          <div class="report-subtitle">
            {{ reportData.username }} · {{ formatDateRange(reportData.startDate, reportData.endDate) }}
          </div>
        </div>

        <div class="stats-grid">
          <div class="stat-item">
            <div class="stat-icon study-time">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ formatStudyTime(reportData.totalStudyMinutes) }}</div>
              <div class="stat-label">总学习时长</div>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-icon review-count">
              <el-icon><Reading /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ reportData.reviewedWordsCount }}</div>
              <div class="stat-label">复习单词数</div>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-icon quiz-count">
              <el-icon><Edit /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ reportData.quizCount }}</div>
              <div class="stat-label">测验次数</div>
            </div>
          </div>

          <div class="stat-item">
            <div class="stat-icon avg-score">
              <el-icon><Trophy /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ reportData.averageScore }}<span class="stat-unit">分</span></div>
              <div class="stat-label">平均分数</div>
            </div>
          </div>
        </div>

        <div class="report-section">
          <div class="section-header">
            <el-icon class="section-icon"><PieChart /></el-icon>
            <span class="section-title">掌握度分布</span>
          </div>
          <div class="chart-container">
            <v-chart class="pie-chart" :option="pieChartOption" autoresize />
          </div>
        </div>

        <div class="report-section">
          <div class="section-header">
            <el-icon class="section-icon"><Warning /></el-icon>
            <span class="section-title">最常出错 Top10</span>
          </div>
          <div class="error-words-container">
            <div v-if="reportData.topErrorWords.length === 0" class="empty-errors">
              <el-icon><CircleCheck /></el-icon>
              <span>太棒了！这段时间没有出错的单词</span>
            </div>
            <div v-else class="error-words-list">
              <div 
                v-for="(item, index) in reportData.topErrorWords" 
                :key="item.wordId" 
                class="error-word-item"
              >
                <div class="error-rank" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
                <div class="error-word-info">
                  <div class="error-word">{{ item.word }}</div>
                  <div class="error-meaning">{{ item.meaning }}</div>
                </div>
                <div class="error-count">
                  <el-tag type="danger" effect="light" size="small">
                    错误 {{ item.errorCount }} 次
                  </el-tag>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="report-section">
          <div class="section-header">
            <el-icon class="section-icon"><TrendCharts /></el-icon>
            <span class="section-title">学习连续性评分</span>
          </div>
          <div class="continuity-container">
            <el-progress 
              :percentage="reportData.continuityScore" 
              :color="getContinuityColor(reportData.continuityScore)"
              :stroke-width="24"
              :text-inside="true"
            />
            <div class="continuity-desc">
              <span v-if="reportData.continuityScore >= 90">优秀！您的学习习惯非常棒，继续保持！</span>
              <span v-else-if="reportData.continuityScore >= 70">良好！坚持学习，您已经做得很好了！</span>
              <span v-else-if="reportData.continuityScore >= 50">一般。建议每天花一点时间学习，效果会更好。</span>
              <span v-else>需要加油！坚持每天学习，才能取得好效果。</span>
            </div>
          </div>
        </div>

        <div class="report-footer">
          <span>生成时间：{{ currentTime }}</span>
          <span>WordMind · 让学习更高效</span>
        </div>
      </div>

      <div class="action-bar">
        <el-button size="large" :loading="exporting" @click="exportImage">
          <el-icon><Picture /></el-icon>
          导出为图片
        </el-button>
        <el-button type="success" size="large" :loading="sharing" @click="generateShareLink">
          <el-icon><Share /></el-icon>
          生成分享链接
        </el-button>
      </div>
    </div>

    <el-dialog v-model="shareDialogVisible" title="分享链接已生成" width="500px">
      <div class="share-content">
        <div class="share-info">
          <el-icon class="share-success-icon"><CircleCheck /></el-icon>
          <div class="share-text">
            <div class="share-title">分享链接创建成功！</div>
            <div class="share-desc">链接有效期 7 天，任何人都可以通过此链接查看报告</div>
          </div>
        </div>
        <div class="share-url-row">
          <el-input :value="shareUrl" readonly>
            <template #append>
              <el-button @click="copyShareUrl">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </template>
          </el-input>
        </div>
        <div class="share-expire">
          <el-icon><Timer /></el-icon>
          <span>过期时间：{{ shareExpiresAt }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  DataAnalysis, Clock, Reading, Edit, Trophy, PieChart, Warning, 
  CircleCheck, TrendCharts, Picture, Share, Timer, CopyDocument,
  Refresh
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart as EChartsPie } from 'echarts/charts'
import { 
  TitleComponent, TooltipComponent, LegendComponent 
} from 'echarts/components'
import VChart from 'vue-echarts'
import html2canvas from 'html2canvas'
import type { ReportResponse, ReportRequest } from '@/types'
import { statsApi } from '@/api/study'

use([CanvasRenderer, EChartsPie, TitleComponent, TooltipComponent, LegendComponent])

const router = useRouter()

const timeRange = ref<'WEEK' | 'MONTH' | 'CUSTOM'>('WEEK')
const customDateRange = ref<string[]>([])
const loading = ref(false)
const exporting = ref(false)
const sharing = ref(false)
const reportData = ref<ReportResponse | null>(null)
const reportRef = ref<HTMLElement | null>(null)
const shareDialogVisible = ref(false)
const shareUrl = ref('')
const shareExpiresAt = ref('')
const currentTime = ref('')

const pieChartOption = computed(() => {
  if (!reportData.value) return {}

  const data = Object.entries(reportData.value.proficiencyDistribution).map(([level, count]) => ({
    value: count,
    name: `等级 ${level}`
  }))

  const colors = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#3b82f6', '#8b5cf6']

  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} ({d}%)'
    },
    legend: {
      orient: 'vertical',
      right: '5%',
      top: 'center',
      textStyle: {
        color: 'var(--c-text-primary)',
        fontSize: 13
      }
    },
    color: colors,
    series: [
      {
        name: '掌握度分布',
        type: 'pie',
        radius: ['45%', '70%'],
        center: ['35%', '50%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 8,
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
            fontSize: 16,
            fontWeight: 'bold',
            color: 'var(--c-text-primary)'
          }
        },
        labelLine: {
          show: false
        },
        data: data
      }
    ]
  }
})

const disabledDate = (time: Date) => {
  return time.getTime() > Date.now()
}

const handleTimeRangeChange = () => {
  if (timeRange.value !== 'CUSTOM') {
    customDateRange.value = []
  }
}

const formatDateRange = (start: string, end: string) => {
  const startDate = new Date(start)
  const endDate = new Date(end)
  const format = (d: Date) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
  return `${format(startDate)} 至 ${format(endDate)}`
}

const formatStudyTime = (minutes: number) => {
  if (minutes < 60) return `${minutes}分钟`
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  if (mins === 0) return `${hours}小时`
  return `${hours}小时${mins}分钟`
}

const getContinuityColor = (score: number) => {
  if (score >= 90) return '#22c55e'
  if (score >= 70) return '#3b82f6'
  if (score >= 50) return '#eab308'
  return '#ef4444'
}

const generateReport = async () => {
  if (timeRange.value === 'CUSTOM' && customDateRange.value.length !== 2) {
    ElMessage.warning('请选择自定义时间范围')
    return
  }

  const request: ReportRequest = {
    timeRange: timeRange.value
  }

  if (timeRange.value === 'CUSTOM') {
    request.startDate = customDateRange.value[0]
    request.endDate = customDateRange.value[1]
  }

  loading.value = true
  try {
    const res = await statsApi.generateReport(request)
    reportData.value = res
    updateCurrentTime()
    await nextTick()
    ElMessage.success('报告生成成功')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const updateCurrentTime = () => {
  const now = new Date()
  const format = (n: number) => String(n).padStart(2, '0')
  currentTime.value = `${now.getFullYear()}-${format(now.getMonth() + 1)}-${format(now.getDate())} ${format(now.getHours())}:${format(now.getMinutes())}:${format(now.getSeconds())}`
}

const exportImage = async () => {
  if (!reportRef.value) return

  exporting.value = true
  try {
    const canvas = await html2canvas(reportRef.value, {
      backgroundColor: '#ffffff',
      scale: 2,
      useCORS: true,
      logging: false
    })

    const link = document.createElement('a')
    link.download = `WordMind学习报告_${new Date().toISOString().slice(0, 10)}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()

    ElMessage.success('报告图片已下载')
  } catch (error) {
    console.error(error)
    ElMessage.error('导出失败，请重试')
  } finally {
    exporting.value = false
  }
}

const generateShareLink = async () => {
  if (!reportData.value) return

  sharing.value = true
  try {
    const reportDataJson = JSON.stringify(reportData.value)
    const res = await statsApi.createShare({
      startDate: reportData.value.startDate,
      endDate: reportData.value.endDate,
      reportData: reportDataJson
    })

    shareUrl.value = res.shareUrl
    shareExpiresAt.value = res.expiresAt
    shareDialogVisible.value = true
  } catch (error) {
    console.error(error)
    ElMessage.error('生成分享链接失败')
  } finally {
    sharing.value = false
  }
}

const copyShareUrl = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('链接已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

onMounted(() => {
  generateReport()
})
</script>

<style scoped>
.filter-section {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.filter-row {
  display: flex;
  align-items: center;
  gap: var(--space-lg);
  flex-wrap: wrap;
}

.time-range-group {
  flex-shrink: 0;
}

.date-picker-group {
  flex-shrink: 0;
}

.report-area {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
}

.report-header {
  text-align: center;
  padding-bottom: 24px;
  border-bottom: 2px dashed var(--c-border-light);
  margin-bottom: 24px;
}

.report-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  font-size: 28px;
  font-weight: 700;
  color: var(--c-text-primary);
  margin-bottom: 8px;
}

.title-icon {
  font-size: 32px;
  color: var(--c-primary);
}

.report-subtitle {
  font-size: 14px;
  color: var(--c-text-secondary);
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-item {
  background: white;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stat-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
  flex-shrink: 0;
}

.stat-icon.study-time {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.stat-icon.review-count {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
}

.stat-icon.quiz-count {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
}

.stat-icon.avg-score {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text-primary);
  line-height: 1.2;
}

.stat-unit {
  font-size: 14px;
  font-weight: 500;
  color: var(--c-text-secondary);
  margin-left: 4px;
}

.stat-label {
  font-size: 13px;
  color: var(--c-text-tertiary);
  margin-top: 4px;
}

.report-section {
  background: white;
  border-radius: 12px;
  padding: 24px;
  margin-bottom: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.section-icon {
  font-size: 22px;
  color: var(--c-primary);
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--c-text-primary);
}

.chart-container {
  height: 280px;
}

.pie-chart {
  width: 100%;
  height: 100%;
}

.error-words-container {
  min-height: 80px;
}

.empty-errors {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 40px;
  color: var(--c-text-secondary);
  font-size: 15px;
}

.empty-errors .el-icon {
  font-size: 28px;
  color: var(--c-success);
}

.error-words-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.error-word-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  background: var(--c-bg-body);
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.error-word-item:hover {
  background: var(--c-bg-hover);
}

.error-rank {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  color: white;
  flex-shrink: 0;
}

.error-rank.rank-1 {
  background: linear-gradient(135deg, #ff6b6b, #ee5a24);
}

.error-rank.rank-2 {
  background: linear-gradient(135deg, #ffa502, #ff7f50);
}

.error-rank.rank-3 {
  background: linear-gradient(135deg, #ffd32a, #ffbe0b);
}

.error-rank.rank-4,
.error-rank.rank-5,
.error-rank.rank-6,
.error-rank.rank-7,
.error-rank.rank-8,
.error-rank.rank-9,
.error-rank.rank-10 {
  background: var(--c-text-tertiary);
}

.error-word-info {
  flex: 1;
  min-width: 0;
}

.error-word {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text-primary);
  margin-bottom: 2px;
}

.error-meaning {
  font-size: 13px;
  color: var(--c-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.continuity-container {
  padding: 8px 0;
}

.continuity-desc {
  margin-top: 16px;
  text-align: center;
  font-size: 14px;
  color: var(--c-text-secondary);
}

.report-footer {
  display: flex;
  justify-content: space-between;
  padding-top: 20px;
  margin-top: 20px;
  border-top: 2px dashed var(--c-border-light);
  font-size: 12px;
  color: var(--c-text-tertiary);
}

.action-bar {
  display: flex;
  justify-content: center;
  gap: var(--space-lg);
  margin-top: var(--space-xl);
  position: sticky;
  bottom: var(--space-lg);
  z-index: 10;
}

.action-bar .el-button {
  min-width: 180px;
  padding: 0 32px;
}

.share-content {
  padding: 8px 0;
}

.share-info {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.share-success-icon {
  font-size: 48px;
  color: var(--c-success);
}

.share-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--c-text-primary);
  margin-bottom: 4px;
}

.share-desc {
  font-size: 13px;
  color: var(--c-text-secondary);
}

.share-url-row {
  margin-bottom: 16px;
}

.share-expire {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--c-text-tertiary);
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .report-area {
    padding: 20px;
  }

  .report-title {
    font-size: 22px;
  }

  .stat-value {
    font-size: 20px;
  }

  .action-bar {
    flex-direction: column;
    position: static;
  }

  .action-bar .el-button {
    width: 100%;
  }
}
</style>
