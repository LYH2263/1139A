<template>
  <div class="mindmap-page page-container">
    <PageHeader 
      title="思维导图" 
      subtitle="可视化探索单词关联网络" 
    />
    
    <div class="mindmap-content">
      <!-- Controls -->
      <SectionCard class="controls-card" :body-style="{ padding: 'var(--space-md)' }">
        <div class="controls-wrapper">
          <div class="search-control">
            <el-input 
              v-model="centerWordId" 
              placeholder="输入中心单词 ID" 
              class="search-input"
              @keyup.enter="loadMindMap"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" @click="loadMindMap" :loading="loading">
              生成导图
            </el-button>
            <el-button 
              type="warning" 
              @click="toggleLearningPath" 
              :loading="pathLoading"
              :disabled="!mindMapData"
            >
              <el-icon><Promotion /></el-icon>
              {{ showLearningPath ? '隐藏学习路径' : '显示学习路径' }}
            </el-button>
          </div>
          
          <div class="depth-control">
            <span class="label">关联深度:</span>
            <el-radio-group v-model="depth" size="small">
              <el-radio-button :label="1">直接关联 (1度)</el-radio-button>
              <el-radio-button :label="2">扩展关联 (2度)</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </SectionCard>
      
      <!-- Learning Path Info -->
      <SectionCard v-if="learningPathData && showLearningPath" class="path-info-card" :body-style="{ padding: 'var(--space-md)' }">
        <div class="path-info">
          <div class="path-stats">
            <div class="stat-item">
              <span class="stat-label">未掌握单词:</span>
              <span class="stat-value unmastered">{{ learningPathData.unmasteredNodes.length }}</span>
            </div>
            <div class="stat-item">
              <span class="stat-label">推荐路径步数:</span>
              <span class="stat-value path">{{ learningPathData.recommendedPath.length }}</span>
            </div>
          </div>
          <div class="path-tip">
            <el-icon><InfoFilled /></el-icon>
            <span>路径优先级: 同义 > 词根 > 主题 > 场景。点击未掌握单词可直接进入复习。</span>
          </div>
        </div>
      </SectionCard>
      
      <!-- Graph Container -->
      <div class="graph-container" v-loading="loading || pathLoading">
        <div ref="chartRef" class="chart-canvas"></div>
        
        <div v-if="!mindMapData && !loading" class="empty-placeholder">
          <EmptyState 
            title="准备生成导图" 
            description="请输入单词 ID 并点击生成，探索词汇网络。"
            icon="Share"
          />
        </div>
        
        <div class="legend" v-if="mindMapData">
          <div class="legend-item">
            <span class="dot primary"></span> 中心词
          </div>
          <div class="legend-item">
            <span class="dot secondary"></span> 已掌握
          </div>
          <div class="legend-item" v-if="showLearningPath">
            <span class="dot unmastered"></span> 未掌握
          </div>
          <div class="legend-item" v-if="showLearningPath">
            <span class="path-line"></span> 推荐路径
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { MindMapResponse, LearningPathResponse, PathWordNode } from '@/types'
import { mindMapApi } from '@/api/study'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Share, Promotion, InfoFilled } from '@element-plus/icons-vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const route = useRoute()
const router = useRouter()
const chartRef = ref<HTMLDivElement>()
const chart = ref<echarts.ECharts | null>(null)
const loading = ref(false)
const pathLoading = ref(false)
const centerWordId = ref<string>('')
const depth = ref(1)
const mindMapData = ref<MindMapResponse | null>(null)
const learningPathData = ref<LearningPathResponse | null>(null)
const showLearningPath = ref(false)

const proficiencyMap = ref<Map<number, PathWordNode>>(new Map())
const recommendedPathSet = ref<Set<string>>(new Set())

const loadMindMap = async () => {
  if (!centerWordId.value) {
    ElMessage.warning('请输入单词 ID')
    return
  }
  
  showLearningPath.value = false
  learningPathData.value = null
  loading.value = true
  try {
    mindMapData.value = await mindMapApi.getMindMap(Number(centerWordId.value), depth.value)
    nextTick(() => {
      renderChart()
    })
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const toggleLearningPath = async () => {
  if (showLearningPath.value) {
    showLearningPath.value = false
    renderChart()
    return
  }
  
  if (!centerWordId.value) {
    ElMessage.warning('请先生成思维导图')
    return
  }
  
  pathLoading.value = true
  try {
    learningPathData.value = await mindMapApi.getLearningPath(Number(centerWordId.value), depth.value)
    
    proficiencyMap.value = new Map()
    learningPathData.value.nodes.forEach(node => {
      proficiencyMap.value.set(node.id, node)
    })
    
    recommendedPathSet.value = new Set()
    learningPathData.value.recommendedPath.forEach(edge => {
      recommendedPathSet.value.add(`${edge.source}-${edge.target}`)
      recommendedPathSet.value.add(`${edge.target}-${edge.source}`)
    })
    
    showLearningPath.value = true
    nextTick(() => {
      renderChart()
    })
  } catch (error) {
    console.error(error)
    ElMessage.error('加载学习路径失败')
  } finally {
    pathLoading.value = false
  }
}

const getNodeColor = (nodeId: number, depth: number): string => {
  if (depth === 0) return '#3b82f6'
  
  if (showLearningPath.value && proficiencyMap.value.has(nodeId)) {
    const nodeInfo = proficiencyMap.value.get(nodeId)!
    if (!nodeInfo.mastered) {
      return '#ef4444'
    }
  }
  
  return '#10b981'
}

const getNodeBorderColor = (nodeId: number): string => {
  if (showLearningPath.value && proficiencyMap.value.has(nodeId)) {
    const nodeInfo = proficiencyMap.value.get(nodeId)!
    if (!nodeInfo.mastered) {
      return '#fbbf24'
    }
  }
  return '#fff'
}

const getNodeSymbolSize = (nodeId: number, depth: number): number => {
  if (depth === 0) return 60
  
  if (showLearningPath.value && proficiencyMap.value.has(nodeId)) {
    const nodeInfo = proficiencyMap.value.get(nodeId)!
    if (!nodeInfo.mastered) {
      return 50
    }
  }
  
  return 40
}

const getLineStyle = (source: string, target: string) => {
  const key = `${source}-${target}`
  if (showLearningPath.value && recommendedPathSet.value.has(key)) {
    return {
      curveness: 0.2,
      color: '#f97316',
      width: 3,
      type: 'dashed' as const,
      dashOffset: 0
    }
  }
  return {
    curveness: 0.2,
    color: '#cbd5e1'
  }
}

const getLineLabel = (edge: any) => {
  const key = `${edge.source}-${edge.target}`
  if (showLearningPath.value && recommendedPathSet.value.has(key)) {
    return {
      show: true,
      formatter: edge.label,
      fontSize: 11,
      fontWeight: 'bold' as const,
      color: '#f97316',
      backgroundColor: '#fff7ed',
      padding: [2, 6],
      borderRadius: 4
    }
  }
  return {
    show: true,
    formatter: edge.label,
    fontSize: 10,
    color: '#94a3b8'
  }
}

const renderChart = () => {
  if (!chartRef.value || !mindMapData.value) return
  
  if (chart.value) {
    chart.value.dispose()
  }
  
  chart.value = echarts.init(chartRef.value)
  
  const { nodes, edges } = mindMapData.value
  
  const getEdgeKey = (s: number, t: number) => `${Math.min(s, t)}-${Math.max(s, t)}`
  const seenEdges = new Set<string>()
  const dedupedEdges = edges.filter(edge => {
    const key = getEdgeKey(edge.source, edge.target)
    if (seenEdges.has(key)) return false
    seenEdges.add(key)
    return true
  })
  
  const chartData = nodes.map(node => {
    const nodeId = node.id
    const baseItem: any = {
      id: String(nodeId),
      name: node.word,
      value: node.meaning,
      symbolSize: getNodeSymbolSize(nodeId, node.depth),
      itemStyle: {
        color: getNodeColor(nodeId, node.depth),
        borderColor: getNodeBorderColor(nodeId),
        borderWidth: showLearningPath.value && proficiencyMap.value.has(nodeId) && !proficiencyMap.value.get(nodeId)!.mastered ? 3 : 2,
        shadowBlur: 15,
        shadowColor: showLearningPath.value && proficiencyMap.value.has(nodeId) && !proficiencyMap.value.get(nodeId)!.mastered ? 'rgba(239, 68, 68, 0.4)' : 'rgba(0,0,0,0.1)'
      },
      label: {
        show: true,
        formatter: '{b}',
        fontSize: node.depth === 0 ? 14 : (showLearningPath.value && proficiencyMap.value.has(nodeId) && !proficiencyMap.value.get(nodeId)!.mastered ? 13 : 12),
        fontWeight: showLearningPath.value && proficiencyMap.value.has(nodeId) && !proficiencyMap.value.get(nodeId)!.mastered ? 'bold' : 'normal',
        color: '#fff'
      }
    }
    
    if (showLearningPath.value && proficiencyMap.value.has(nodeId)) {
      const nodeInfo = proficiencyMap.value.get(nodeId)!
      baseItem.value = `${node.meaning}<br/>熟练度: ${nodeInfo.proficiency}/5`
    }
    
    return baseItem
  })
  
  const chartLinks = dedupedEdges.map(edge => ({
    source: String(edge.source),
    target: String(edge.target),
    value: edge.label,
    label: getLineLabel(edge),
    lineStyle: getLineStyle(String(edge.source), String(edge.target))
  }))
  
  const option: echarts.EChartsOption = {
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(255, 255, 255, 0.95)',
      borderColor: '#e2e8f0',
      textStyle: { color: '#1e293b' },
      formatter: (params: any) => {
        if (params.dataType === 'node') {
          const nodeId = Number(params.data.id)
          let extraInfo = ''
          if (showLearningPath.value && proficiencyMap.value.has(nodeId)) {
            const nodeInfo = proficiencyMap.value.get(nodeId)!
            const statusText = nodeInfo.mastered ? '已掌握' : '未掌握'
            const statusColor = nodeInfo.mastered ? '#10b981' : '#ef4444'
            extraInfo = `<div style="margin-top:6px;padding-top:6px;border-top:1px solid #e2e8f0;">
              <span style="color:#64748b;">熟练度:</span> 
              <span style="font-weight:bold;">${nodeInfo.proficiency}/5</span>
              <span style="margin-left:8px;color:${statusColor};font-weight:bold;">${statusText}</span>
            </div>`
          }
          return `<div style="font-weight:bold;margin-bottom:4px;font-size:14px;">${params.name}</div>
                  <div style="font-size:12px;color:#64748b;line-height:1.5;">${params.value}</div>
                  ${extraInfo}`
        }
        return `<div style="font-size:12px;"><span style="color:#64748b;">关系:</span> ${params.value}</div>`
      }
    },
    series: [{
      type: 'graph',
      layout: 'force',
      data: chartData,
      links: chartLinks,
      roam: true,
      draggable: true,
      focusNodeAdjacency: true,
      force: {
        repulsion: 350,
        edgeLength: 130,
        gravity: 0.06
      },
      emphasis: {
        focus: 'adjacency',
        lineStyle: {
          width: 4,
          color: '#3b82f6'
        }
      },
      animation: true,
      animationDuration: 1500,
      animationEasingUpdate: 'quinticInOut'
    }]
  }
  
  chart.value.setOption(option)
  
  if (showLearningPath.value && learningPathData.value?.recommendedPath.length) {
    animatePath()
  }
  
  chart.value.on('click', async (params: any) => {
    if (params.dataType === 'node') {
      const wordId = Number(params.data.id)
      
      if (showLearningPath.value && proficiencyMap.value.has(wordId)) {
        const nodeInfo = proficiencyMap.value.get(wordId)!
        if (!nodeInfo.mastered) {
          try {
            await ElMessageBox.confirm(
              `"${params.name}" 尚未掌握，是否立即进入复习模式？`,
              '复习确认',
              {
                confirmButtonText: '开始复习',
                cancelButtonText: '查看详情',
                type: 'warning',
                icon: Promotion
              }
            )
            ElMessage.success('即将跳转到复习页面...')
            router.push('/review')
            return
          } catch (action: any) {
            if (action !== 'cancel') {
              console.error(action)
            }
          }
        }
      }
      
      router.push(`/words/${wordId}`)
      ElMessage.success({ message: `跳转至 "${params.name}" 详情`, type: 'success' })
    }
  })
}

const animatePath = () => {
  if (!chart.value || !learningPathData.value) return
  
  let dashOffset = 0
  const animate = () => {
    if (!showLearningPath.value || !chart.value) return
    
    dashOffset = (dashOffset - 2) % 20
    
    const option = chart.value.getOption()
    if (option.series && option.series[0] && option.series[0].links) {
      option.series[0].links = option.series[0].links.map((link: any) => {
        const key = `${link.source}-${link.target}`
        if (recommendedPathSet.value.has(key)) {
          return {
            ...link,
            lineStyle: {
              ...link.lineStyle,
              dashOffset: dashOffset
            }
          }
        }
        return link
      })
      chart.value.setOption({ series: option.series })
    }
    
    requestAnimationFrame(animate)
  }
  
  requestAnimationFrame(animate)
}

watch(depth, () => {
  if (mindMapData.value) {
    loadMindMap()
  }
})

const handleResize = () => {
    chart.value?.resize()
}

onMounted(() => {
  const wordId = route.params.wordId
  if (wordId) {
    centerWordId.value = String(wordId)
    loadMindMap()
  }
  
  window.addEventListener('resize', handleResize)
})
</script>

<style scoped>
.controls-card {
  margin-bottom: var(--space-md);
  background: white;
}

.path-info-card {
  margin-bottom: var(--space-md);
  background: linear-gradient(135deg, #fff7ed 0%, #fef3c7 100%);
  border: 1px solid #fed7aa;
}

.path-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.path-stats {
  display: flex;
  gap: var(--space-lg);
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.stat-label {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
}

.stat-value.unmastered {
  color: #ef4444;
}

.stat-value.path {
  color: #f97316;
}

.path-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  color: #92400e;
}

.controls-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-md);
}

.search-control {
  display: flex;
  gap: var(--space-sm);
  flex: 1;
  max-width: 600px;
  align-items: center;
}

.depth-control {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}

.label {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.graph-container {
  height: calc(100vh - 320px);
  min-height: 500px;
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  border: 1px solid var(--c-border-light);
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

.empty-placeholder {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  width: 100%;
}

.legend {
  position: absolute;
  bottom: var(--space-md);
  right: var(--space-md);
  background: rgba(255, 255, 255, 0.95);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-md);
  display: flex;
  flex-direction: column;
  gap: 6px;
  z-index: 10;
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: var(--c-text-secondary);
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 8px;
}

.dot.primary { background: #3b82f6; }
.dot.secondary { background: #10b981; }
.dot.unmastered { background: #ef4444; }

.path-line {
  width: 20px;
  height: 3px;
  background: repeating-linear-gradient(90deg, #f97316 0px, #f97316 4px, transparent 4px, transparent 8px);
  margin-right: 8px;
  border-radius: 2px;
}

@media (max-width: 640px) {
  .controls-wrapper {
    flex-direction: column;
    align-items: stretch;
  }
  
  .search-control {
    max-width: none;
    flex-wrap: wrap;
  }
  
  .depth-control {
    justify-content: flex-end;
  }
  
  .path-info {
    flex-direction: column;
    align-items: flex-start;
  }
  
  .path-stats {
    flex-wrap: wrap;
  }
}
</style>
