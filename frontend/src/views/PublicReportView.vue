<template>
  <div class="public-report-page">
    <div v-if="loading" class="loading-container">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>加载中...</span>
    </div>

    <div v-else-if="error" class="error-container">
      <el-icon class="error-icon"><Warning /></el-icon>
      <div class="error-title">分享链接无效或已过期</div>
      <div class="error-desc">该分享链接可能已被删除或已超过7天有效期</div>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>

    <div v-else-if="reportData" class="report-wrapper">
      <div class="report-header-bar">
        <div class="header-info">
          <el-icon class="header-icon"><Share /></el-icon>
          <div>
            <div class="header-title">分享的学习报告</div>
            <div class="header-subtitle">
              {{ publicReportInfo.username }} · 生成于 {{ publicReportInfo.createdAt }}
            </div>
          </div>
        </div>
        <el-button type="primary" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          进入WordMind
        </el-button>
      </div>

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
              <span v-if="reportData.continuityScore >= 90">优秀！学习习惯非常棒！</span>
              <span v-else-if="reportData.continuityScore >= 70">良好！坚持学习，做得很好！</span>
              <span v-else-if="reportData.continuityScore >= 50">一般。建议每天花一点时间学习。</span>
              <span v-else>需要加油！坚持每天学习，才能取得好效果。</span>
            </div>
          </div>
        </div>

        <div class="report-footer">
          <span>生成时间：{{ publicReportInfo.createdAt }}</span>
          <span>WordMind · 让学习更高效</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { 
  DataAnalysis, Clock, Reading, Edit, Trophy, PieChart, Warning, 
  CircleCheck, TrendCharts, Share, HomeFilled, Loading
} from '@element-plus/icons-vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { PieChart as EChartsPie } from 'echarts/charts'
import { 
  TitleComponent, TooltipComponent, LegendComponent 
} from 'echarts/components'
import VChart from 'vue-echarts'
import type { ReportResponse, PublicReportResponse } from '@/types'
import { statsApi } from '@/api/study'

use([CanvasRenderer, EChartsPie, TitleComponent, TooltipComponent, LegendComponent])

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const error = ref(false)
const reportData = ref<ReportResponse | null>(null)
const reportRef = ref<HTMLElement | null>(null)
const publicReportInfo = ref<PublicReportResponse | null>(null)

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

const fetchReport = async () => {
  const token = route.params.token as string
  if (!token) {
    error.value = true
    loading.value = false
    return
  }

  loading.value = true
  error.value = false
  try {
    const res = await statsApi.getPublicReport(token)
    publicReportInfo.value = res
    reportData.value = JSON.parse(res.reportData) as ReportResponse
  } catch (err) {
    console.error(err)
    error.value = true
  } finally {
    loading.value = false
  }
}

const goHome = () => {
  router.push('/')
}

onMounted(() => {
  fetchReport()
})
</script>

<style scoped>
.public-report-page {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 40px 20px;
}

.loading-container,
.error-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 60vh;
  color: white;
  gap: 16px;
}

.loading-icon {
  font-size: 48px;
  animation: rotate 1s linear infinite;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.error-icon {
  font-size: 64px;
  color: #fbbf24;
}

.error-title {
  font-size: 24px;
  font-weight: 600;
}

.error-desc {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 16px;
}

.report-wrapper {
  max-width: 900px;
  margin: 0 auto;
}

.report-header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 16px 16px 0 0;
  border-bottom: 1px solid var(--c-border-light);
  backdrop-filter: blur(10px);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-icon {
  font-size: 32px;
  color: var(--c-primary);
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--c-text-primary);
}

.header-subtitle {
  font-size: 13px;
  color: var(--c-text-secondary);
  margin-top: 2px;
}

.report-area {
  background: linear-gradient(135deg, #ffffff 0%, #f8fafc 100%);
  border-radius: 0 0 16px 16px;
  padding: 32px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
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

@media (max-width: 768px) {
  .public-report-page {
    padding: 20px 12px;
  }

  .report-header-bar {
    flex-direction: column;
    gap: 16px;
    text-align: center;
  }

  .header-info {
    flex-direction: column;
  }

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
}
</style>
