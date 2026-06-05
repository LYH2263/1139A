<template>
  <div class="mistakes-page page-container" v-loading="loading">
    <PageHeader 
      title="错题本" 
      subtitle="汇总所有历史测验的错误单词，帮助你识别薄弱环节。" 
    >
      <template #action>
        <el-button type="primary" @click="$router.push('/quiz')">
          <el-icon><Edit /></el-icon>
          去测验
        </el-button>
      </template>
    </PageHeader>

    <div class="content-grid">
      <div class="main-content">
        <div class="filter-bar">
          <el-select 
            v-model="selectedPos" 
            placeholder="按词性筛选" 
            clearable
            @change="handlePosChange"
          >
            <el-option label="名词 (n.)" value="n." />
            <el-option label="动词 (v.)" value="v." />
            <el-option label="形容词 (adj.)" value="adj." />
            <el-option label="副词 (adv.)" value="adv." />
            <el-option label="介词 (prep.)" value="prep." />
            <el-option label="连词 (conj.)" value="conj." />
          </el-select>
          <span class="total-count">共 {{ total }} 个错题单词</span>
        </div>

        <div v-if="mistakes.length === 0" class="empty-state">
          <EmptyState 
            title="暂无错题记录" 
            description="完成几次测验后，你的错题会自动汇总到这里。"
          >
            <template #icon>
              <Edit />
            </template>
          </EmptyState>
        </div>

        <div v-else class="mistake-list">
          <div 
            v-for="(item, index) in mistakes" 
            :key="item.wordId" 
            class="mistake-card"
          >
            <div class="card-header">
              <div class="word-info">
                <span class="rank-badge">#{{ index + 1 }}</span>
                <span class="word-text">{{ item.word }}</span>
                <span class="phonetic" v-if="item.phonetic">{{ item.phonetic }}</span>
                <el-tag v-if="item.pos" size="small" class="pos-tag">{{ item.pos }}</el-tag>
              </div>
              <div class="error-badge">
                <el-icon color="#f56c6c"><Warning /></el-icon>
                <span>错 {{ item.errorCount }} 次</span>
              </div>
            </div>

            <div class="card-body">
              <p class="meaning">{{ item.meaning }}</p>
              <p v-if="item.example" class="example">{{ item.example }}</p>
              
              <div class="stats-row">
                <div class="stat-item">
                  <span class="stat-label">正确率</span>
                  <span class="stat-value" :class="getAccuracyClass(item.accuracy)">
                    {{ item.accuracy }}%
                  </span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">总测验次数</span>
                  <span class="stat-value">{{ item.totalAttempts }}</span>
                </div>
                <div class="stat-item">
                  <span class="stat-label">最近错误</span>
                  <span class="stat-value">{{ formatDate(item.lastErrorTime) }}</span>
                </div>
              </div>

              <div v-if="item.accuracyTrend && item.accuracyTrend.length > 0" class="trend-chart">
                <div class="chart-title">正确率趋势</div>
                <v-chart 
                  :option="getTrendChartOption(item.accuracyTrend)" 
                  :autoresize="true"
                  class="mini-chart"
                />
              </div>
            </div>

            <div class="card-footer">
              <el-button 
                v-if="!item.inTodayReview"
                type="primary" 
                size="small" 
                :loading="addingWordId === item.wordId"
                @click="addToReview(item)"
              >
                <el-icon><Plus /></el-icon>
                加入今日复习
              </el-button>
              <el-tag v-else type="success" size="small">
                <el-icon><Check /></el-icon>
                已在复习计划中
              </el-tag>
              <el-button size="small" @click="goToWordDetail(item.wordId)">
                查看详情
              </el-button>
            </div>
          </div>
        </div>

        <div v-if="total > 0" class="pagination-wrapper">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <div class="side-content">
        <SectionCard title="薄弱点分析" class="analysis-card">
          <el-alert 
            v-if="weaknessAnalysis" 
            :title="weaknessAnalysis.overallSuggestion" 
            type="info" 
            :closable="false"
            class="suggestion-alert"
          />

          <div v-if="weaknessAnalysis && weaknessAnalysis.posDistribution.length > 0">
            <h4 class="chart-subtitle">词性错误率分布</h4>
            <v-chart 
              :option="getPosRadarOption()" 
              :autoresize="true"
              class="analysis-chart"
            />
          </div>

          <div v-if="weaknessAnalysis && weaknessAnalysis.relationTypeDistribution.length > 0">
            <h4 class="chart-subtitle">词汇关系错误分布</h4>
            <v-chart 
              :option="getRelationPieOption()" 
              :autoresize="true"
              class="analysis-chart pie-chart"
            />
          </div>

          <div v-if="weaknessAnalysis && weaknessAnalysis.weakPos.length > 0" class="weak-points">
            <h4 class="chart-subtitle">需要重点关注</h4>
            <div class="weak-tags">
              <el-tag 
                v-for="pos in weaknessAnalysis.weakPos" 
                :key="pos" 
                type="danger" 
                class="weak-tag"
              >
                {{ pos }}
              </el-tag>
              <el-tag 
                v-for="rel in weaknessAnalysis.weakRelationTypes" 
                :key="rel" 
                type="warning" 
                class="weak-tag"
              >
                {{ rel }}
              </el-tag>
            </div>
          </div>

          <div v-if="!weaknessAnalysis" class="analysis-empty">
            <EmptyState 
              title="暂无分析数据" 
              description="多做几次测验后，系统会为你生成个性化的薄弱点分析。"
            >
              <template #icon>
                <TrendCharts />
              </template>
            </EmptyState>
          </div>
        </SectionCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Edit, Warning, Plus, Check, TrendCharts } from '@element-plus/icons-vue'
import type { MistakeItem, WeaknessAnalysisResponse, AccuracyPoint } from '@/types'
import { quizApi, statsApi } from '@/api/study'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { RadarChart, PieChart, LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  RadarComponent,
  GridComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([
  RadarChart,
  PieChart,
  LineChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  RadarComponent,
  GridComponent,
  CanvasRenderer
])

const router = useRouter()
const loading = ref(false)
const analysisLoading = ref(false)
const mistakes = ref<MistakeItem[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedPos = ref('')
const addingWordId = ref<number | null>(null)
const weaknessAnalysis = ref<WeaknessAnalysisResponse | null>(null)

const fetchMistakes = async () => {
  loading.value = true
  try {
    const res = await quizApi.getMistakes(currentPage.value - 1, pageSize.value, selectedPos.value)
    mistakes.value = res.list
    total.value = res.total
  } catch (error) {
    console.error(error)
    ElMessage.error('获取错题列表失败')
  } finally {
    loading.value = false
  }
}

const fetchWeaknessAnalysis = async () => {
  analysisLoading.value = true
  try {
    const res = await quizApi.getWeaknessAnalysis()
    weaknessAnalysis.value = res
  } catch (error) {
    console.error(error)
  } finally {
    analysisLoading.value = false
  }
}

const handlePosChange = () => {
  currentPage.value = 1
  fetchMistakes()
}

const handlePageChange = (page: number) => {
  currentPage.value = page
  fetchMistakes()
}

const handleSizeChange = (size: number) => {
  pageSize.value = size
  currentPage.value = 1
  fetchMistakes()
}

const addToReview = async (item: MistakeItem) => {
  addingWordId.value = item.wordId
  try {
    await statsApi.createStudyPlan(item.wordId, 'TODAY')
    item.inTodayReview = true
    ElMessage.success(`已将 "${item.word}" 加入今日复习计划`)
  } catch (error) {
    console.error(error)
    ElMessage.error('加入复习计划失败')
  } finally {
    addingWordId.value = null
  }
}

const goToWordDetail = (wordId: number) => {
  router.push(`/words/${wordId}`)
}

const formatDate = (dateStr: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const days = Math.floor(diff / (1000 * 60 * 60 * 24))
  
  if (days === 0) return '今天'
  if (days === 1) return '昨天'
  if (days < 7) return `${days}天前`
  return date.toLocaleDateString('zh-CN')
}

const getAccuracyClass = (accuracy: number) => {
  if (accuracy >= 80) return 'accuracy-high'
  if (accuracy >= 60) return 'accuracy-mid'
  return 'accuracy-low'
}

const getTrendChartOption = (trend: AccuracyPoint[]) => {
  return {
    grid: {
      left: '3%',
      right: '3%',
      top: '10%',
      bottom: '10%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: trend.map(t => t.date),
      axisLine: { lineStyle: { color: '#e4e7ed' } },
      axisLabel: { color: '#909399', fontSize: 10 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { 
        color: '#909399', 
        fontSize: 10,
        formatter: '{value}%'
      },
      splitLine: { lineStyle: { color: '#f0f2f5' } }
    },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const data = params[0]
        return `${data.name}<br/>正确率: ${data.value}%`
      }
    },
    series: [{
      data: trend.map(t => t.accuracy),
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: {
        color: '#409eff',
        width: 2
      },
      itemStyle: {
        color: '#409eff'
      },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }
          ]
        }
      }
    }]
  }
}

const getPosRadarOption = () => {
  if (!weaknessAnalysis.value) return {}
  
  const indicator = weaknessAnalysis.value.posDistribution.map(p => ({
    name: p.posName,
    max: 100
  }))
  
  const values = weaknessAnalysis.value.posDistribution.map(p => p.errorRate)
  
  return {
    tooltip: {
      trigger: 'item',
      formatter: (params: any) => {
        return `${params.name}<br/>错误率: ${params.value}%`
      }
    },
    radar: {
      indicator,
      shape: 'polygon',
      splitNumber: 4,
      axisName: {
        color: '#606266',
        fontSize: 12
      },
      splitLine: {
        lineStyle: { color: '#e4e7ed' }
      },
      splitArea: {
        areaStyle: { color: ['#fafafa', '#f5f7fa'] }
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: values,
        name: '错误率',
        symbol: 'circle',
        symbolSize: 6,
        lineStyle: { color: '#f56c6c', width: 2 },
        itemStyle: { color: '#f56c6c' },
        areaStyle: {
          color: 'rgba(245, 108, 108, 0.3)'
        }
      }]
    }]
  }
}

const getRelationPieOption = () => {
  if (!weaknessAnalysis.value) return {}
  
  const data = weaknessAnalysis.value.relationTypeDistribution.map(r => ({
    value: r.errorCount,
    name: r.typeName
  }))
  
  const colors = ['#f56c6c', '#e6a23c', '#409eff', '#67c23a', '#909399', '#9b59b6', '#1abc9c']
  
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c}次错误 ({d}%)'
    },
    legend: {
      type: 'scroll',
      orient: 'horizontal',
      bottom: 0,
      textStyle: { fontSize: 11, color: '#606266' }
    },
    color: colors,
    series: [{
      type: 'pie',
      radius: ['35%', '65%'],
      center: ['50%', '45%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 12,
          fontWeight: 'bold'
        }
      },
      data
    }]
  }
}

onMounted(() => {
  fetchMistakes()
  fetchWeaknessAnalysis()
})
</script>

<style scoped>
.mistakes-page {
  min-height: 100%;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: var(--space-xl);
  align-items: start;
}

.main-content {
  min-width: 0;
}

.side-content {
  position: sticky;
  top: var(--space-xl);
}

.filter-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
}

.total-count {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.empty-state {
  padding: var(--space-xxl) 0;
}

.mistake-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.mistake-card {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--c-border-light);
  overflow: hidden;
  transition: all 0.2s;
}

.mistake-card:hover {
  box-shadow: var(--shadow-md);
  border-color: var(--c-primary-light);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md) var(--space-lg);
  background: linear-gradient(135deg, #fef2f2 0%, #fff 100%);
  border-bottom: 1px solid var(--c-border-light);
}

.word-info {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.rank-badge {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--c-primary);
  color: white;
  border-radius: var(--radius-md);
  font-size: var(--font-size-xs);
  font-weight: 700;
}

.word-text {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.phonetic {
  color: var(--c-text-tertiary);
  font-size: var(--font-size-sm);
}

.pos-tag {
  margin-left: 4px;
}

.error-badge {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--c-danger);
  font-weight: 600;
  font-size: var(--font-size-sm);
}

.card-body {
  padding: var(--space-lg);
}

.meaning {
  font-size: var(--font-size-base);
  color: var(--c-text-primary);
  margin: 0 0 var(--space-sm);
  line-height: 1.6;
}

.example {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  margin: 0;
  padding: var(--space-sm);
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--c-primary-light);
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-md);
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px dashed var(--c-border-light);
}

.stat-item {
  text-align: center;
}

.stat-label {
  display: block;
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
  margin-bottom: 4px;
}

.stat-value {
  display: block;
  font-size: 18px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.accuracy-high { color: var(--c-success) !important; }
.accuracy-mid { color: var(--c-warning) !important; }
.accuracy-low { color: var(--c-danger) !important; }

.trend-chart {
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px dashed var(--c-border-light);
}

.chart-title {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  margin-bottom: var(--space-sm);
  font-weight: 500;
}

.mini-chart {
  height: 120px;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-sm);
  padding: var(--space-md) var(--space-lg);
  background: var(--c-bg-body);
  border-top: 1px solid var(--c-border-light);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--space-xl);
}

.analysis-card {
  margin-bottom: 0;
}

.suggestion-alert {
  margin-bottom: var(--space-lg);
}

.chart-subtitle {
  font-size: var(--font-size-sm);
  color: var(--c-text-primary);
  font-weight: 600;
  margin: var(--space-lg) 0 var(--space-md);
}

.analysis-chart {
  height: 280px;
}

.pie-chart {
  height: 300px;
}

.weak-points {
  margin-top: var(--space-lg);
  padding-top: var(--space-lg);
  border-top: 1px dashed var(--c-border-light);
}

.weak-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.weak-tag {
  margin-right: 0 !important;
}

.analysis-empty {
  padding: var(--space-xxl) 0;
}

@media (max-width: 1024px) {
  .content-grid {
    grid-template-columns: 1fr;
  }
  
  .side-content {
    position: static;
  }
}

@media (max-width: 640px) {
  .stats-row {
    gap: var(--space-sm);
  }
  
  .stat-value {
    font-size: 16px;
  }
  
  .word-text {
    font-size: 18px;
  }
  
  .filter-bar {
    flex-direction: column;
    align-items: flex-start;
    gap: var(--space-sm);
  }
}
</style>
