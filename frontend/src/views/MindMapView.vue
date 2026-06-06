<template>
  <div class="mindmap-page page-container">
    <PageHeader 
      title="思维导图" 
      subtitle="可视化探索单词关联网络" 
    />
    
    <div class="mindmap-content">
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
            <el-button 
              :type="editMode ? 'success' : 'default'" 
              @click="toggleEditMode"
              :disabled="!mindMapData"
            >
              <el-icon><Edit /></el-icon>
              {{ editMode ? '退出编辑' : '编辑模式' }}
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
        <div v-if="editMode" class="edit-tip">
          <el-icon><InfoFilled /></el-icon>
          <span>编辑模式：从一个节点拖拽到另一个节点可创建关系；右键节点可管理该节点的关系。</span>
        </div>
      </SectionCard>
      
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
      
      <div class="graph-container" v-loading="loading || pathLoading" @contextmenu.prevent="hideContextMenu">
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
          <div class="legend-item" v-if="editMode">
            <span class="dot user-contrib"></span> 用户贡献
          </div>
        </div>
        
        <div 
          v-show="contextMenuVisible" 
          class="context-menu"
          :style="{ left: contextMenuX + 'px', top: contextMenuY + 'px' }"
          @click.stop
        >
          <div class="context-menu-header">
            <strong>{{ contextMenuWord }}</strong>
            <span class="context-menu-sub">的关系管理</span>
          </div>
          <el-divider style="margin: 6px 0" />
          <div 
            v-if="nodeRelations.length === 0" 
            class="context-menu-empty"
          >
            暂无关系
          </div>
          <div 
            v-for="rel in nodeRelations" 
            :key="rel.id" 
            class="context-menu-item"
          >
            <div class="relation-info">
              <el-tag size="small" :type="getRelationTagType(rel.relationType)">{{ rel.label }}</el-tag>
              <span class="relation-target">{{ getWordById(rel.source === contextMenuNodeId ? rel.target : rel.source) }}</span>
              <el-tag 
                v-if="rel.isUserContribution" 
                size="small" 
                type="warning" 
                effect="plain"
              >
                用户贡献
              </el-tag>
            </div>
            <el-button 
              link 
              type="danger" 
              size="small"
              @click="handleDeleteRelation(rel)"
            >
              <el-icon><Delete /></el-icon>
            </el-button>
          </div>
        </div>
      </div>
    </div>
    
    <el-dialog
      v-model="relationDialogVisible"
      title="选择关系类型"
      width="420px"
      class="relation-dialog"
      @close="cancelRelationCreation"
    >
      <div class="relation-dialog-content">
        <div class="relation-nodes">
          <div class="relation-node source">
            <span class="node-label">源单词</span>
            <span class="node-word">{{ relationSourceWord }}</span>
          </div>
          <el-icon class="relation-arrow"><Right /></el-icon>
          <div class="relation-node target">
            <span class="node-label">目标单词</span>
            <span class="node-word">{{ relationTargetWord }}</span>
          </div>
        </div>
        <el-divider />
        <div class="relation-types">
          <div 
            v-for="rt in relationTypes" 
            :key="rt.value"
            class="relation-type-card"
            :class="{ active: selectedRelationType === rt.value }"
            @click="selectedRelationType = rt.value"
          >
            <div class="relation-type-name">{{ rt.label }}</div>
            <div class="relation-type-desc">{{ rt.desc }}</div>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="cancelRelationCreation">取消</el-button>
        <el-button 
          type="primary" 
          @click="confirmRelationCreation"
          :disabled="!selectedRelationType"
          :loading="creatingRelation"
        >
          创建关系
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, nextTick, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import * as echarts from 'echarts'
import type { MindMapResponse, LearningPathResponse, PathWordNode, MindMapEdge, RelationType } from '@/types'
import { mindMapApi } from '@/api/study'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Share, Promotion, InfoFilled, Edit, Delete, Right } from '@element-plus/icons-vue'
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
const editMode = ref(false)

const proficiencyMap = ref<Map<number, PathWordNode>>(new Map())
const recommendedPathSet = ref<Set<string>>(new Set())

const relationDialogVisible = ref(false)
const relationSourceId = ref<number | null>(null)
const relationTargetId = ref<number | null>(null)
const relationSourceWord = ref('')
const relationTargetWord = ref('')
const selectedRelationType = ref<RelationType | ''>('')
const creatingRelation = ref(false)

const contextMenuVisible = ref(false)
const contextMenuX = ref(0)
const contextMenuY = ref(0)
const contextMenuNodeId = ref<number | null>(null)
const contextMenuWord = ref('')
const nodeRelations = ref<MindMapEdge[]>([])

const isDragging = ref(false)
const dragSourceId = ref<string | null>(null)

const relationTypes: { value: RelationType; label: string; desc: string }[] = [
  { value: 'SYNONYM', label: '同义', desc: '意思相同或相近的词' },
  { value: 'ANTONYM', label: '反义', desc: '意思相反的词' },
  { value: 'TOPIC', label: '主题', desc: '属于同一主题领域' },
  { value: 'ROOT', label: '词根', desc: '拥有相同词根' },
  { value: 'PREFIX', label: '前缀', desc: '拥有相同前缀' },
  { value: 'SUFFIX', label: '后缀', desc: '拥有相同后缀' },
  { value: 'SCENE', label: '场景', desc: '常用于同一场景' }
]

const loadMindMap = async () => {
  if (!centerWordId.value) {
    ElMessage.warning('请输入单词 ID')
    return
  }
  
  editMode.value = false
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

const toggleEditMode = () => {
  editMode.value = !editMode.value
  if (chart.value) {
    renderChart()
  }
  if (!editMode.value) {
    hideContextMenu()
  }
}

const getNodeColor = (nodeId: number, depth: number, isUserContrib?: boolean): string => {
  if (editMode.value && isUserContrib) return '#f59e0b'
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

const getLineStyle = (source: string, target: string, edge?: MindMapEdge) => {
  const key = `${source}-${target}`
  if (editMode.value && edge?.isUserContribution) {
    return {
      curveness: 0.2,
      color: '#f59e0b',
      width: 2,
      type: 'dashed' as const
    }
  }
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

const getLineLabel = (edge: MindMapEdge) => {
  const key = `${edge.source}-${edge.target}`
  const isUserContrib = editMode.value && edge.isUserContribution
  
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
  if (isUserContrib) {
    return {
      show: true,
      formatter: edge.label,
      fontSize: 10,
      color: '#f59e0b',
      backgroundColor: '#fffbeb',
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

const getRelationTagType = (type: string) => {
  const map: Record<string, '' | 'success' | 'warning' | 'danger' | 'info' | 'primary'> = {
    SYNONYM: 'success',
    ANTONYM: 'danger',
    TOPIC: 'primary',
    ROOT: 'warning',
    PREFIX: 'info',
    SUFFIX: '',
    SCENE: 'warning'
  }
  return map[type] || 'info'
}

const getWordById = (id: number): string => {
  if (!mindMapData.value) return String(id)
  const node = mindMapData.value.nodes.find(n => n.id === id)
  return node ? node.word : String(id)
}

const renderChart = () => {
  if (!chartRef.value || !mindMapData.value) return
  
  if (chart.value) {
    chart.value.dispose()
  }
  
  chart.value = echarts.init(chartRef.value)
  
  const { nodes, edges } = mindMapData.value
  const nodeIdToData = new Map(nodes.map(n => [n.id, n]))
  
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
    const depth = node.depth
    const baseItem: any = {
      id: String(nodeId),
      name: node.word,
      value: node.meaning,
      symbolSize: getNodeSymbolSize(nodeId, depth),
      draggable: true,
      itemStyle: {
        color: getNodeColor(nodeId, depth),
        borderColor: getNodeBorderColor(nodeId),
        borderWidth: showLearningPath.value && proficiencyMap.value.has(nodeId) && !proficiencyMap.value.get(nodeId)!.mastered ? 3 : 2,
        shadowBlur: 15,
        shadowColor: showLearningPath.value && proficiencyMap.value.has(nodeId) && !proficiencyMap.value.get(nodeId)!.mastered ? 'rgba(239, 68, 68, 0.4)' : 'rgba(0,0,0,0.1)'
      },
      label: {
        show: true,
        formatter: '{b}',
        fontSize: depth === 0 ? 14 : (showLearningPath.value && proficiencyMap.value.has(nodeId) && !proficiencyMap.value.get(nodeId)!.mastered ? 13 : 12),
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
    id: String(edge.id),
    source: String(edge.source),
    target: String(edge.target),
    value: edge.label,
    label: getLineLabel(edge),
    lineStyle: getLineStyle(String(edge.source), String(edge.target), edge),
    _edgeData: edge
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
                  ${extraInfo}
                  ${editMode.value ? '<div style="margin-top:6px;padding-top:6px;border-top:1px solid #e2e8f0;font-size:11px;color:#94a3b8;">拖拽到其他节点可创建关系<br/>右键可管理关系</div>' : ''}`
        }
        const edgeData = params.data?._edgeData
        let contribInfo = ''
        if (edgeData?.isUserContribution) {
          contribInfo = `<div style="margin-top:4px;font-size:11px;color:#f59e0b;">用户贡献${edgeData.createdByUsername ? ' by ' + edgeData.createdByUsername : ''}</div>`
        }
        return `<div style="font-size:12px;"><span style="color:#64748b;">关系:</span> ${params.value}${contribInfo}</div>`
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
  
  bindChartEvents()
}

const bindChartEvents = () => {
  if (!chart.value) return
  
  chart.value.off('click')
  chart.value.off('mouseDown')
  chart.value.off('mouseUp')
  chart.value.off('contextmenu')
  
  chart.value.on('click', async (params: any) => {
    if (editMode.value && isDragging.value) {
      isDragging.value = false
      return
    }
    
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
  
  if (editMode.value) {
    chart.value.on('mouseDown', (params: any) => {
      if (params.dataType === 'node') {
        isDragging.value = true
        dragSourceId.value = params.data.id
      }
    })
    
    chart.value.on('mouseUp', (params: any) => {
      if (isDragging.value && dragSourceId.value && params.dataType === 'node') {
        const targetId = params.data.id
        const sourceId = dragSourceId.value
        
        if (sourceId !== targetId) {
          openRelationDialog(Number(sourceId), Number(targetId))
        }
      }
      isDragging.value = false
      dragSourceId.value = null
    })
    
    chart.value.on('contextmenu', (params: any) => {
      if (params.dataType === 'node') {
        params.event?.stop()
        showContextMenu(params)
      }
    })
  }
}

const openRelationDialog = (sourceId: number, targetId: number) => {
  relationSourceId.value = sourceId
  relationTargetId.value = targetId
  relationSourceWord.value = getWordById(sourceId)
  relationTargetWord.value = getWordById(targetId)
  selectedRelationType.value = ''
  relationDialogVisible.value = true
}

const cancelRelationCreation = () => {
  relationDialogVisible.value = false
  relationSourceId.value = null
  relationTargetId.value = null
  relationSourceWord.value = ''
  relationTargetWord.value = ''
  selectedRelationType.value = ''
}

const confirmRelationCreation = async () => {
  if (!relationSourceId.value || !relationTargetId.value || !selectedRelationType.value) {
    return
  }
  
  creatingRelation.value = true
  try {
    const newEdge = await mindMapApi.createRelation({
      sourceWordId: relationSourceId.value,
      targetWordId: relationTargetId.value,
      relationType: selectedRelationType.value
    })
    
    if (mindMapData.value) {
      mindMapData.value.edges.push(newEdge)
    }
    
    ElMessage.success('关系已创建，待管理员审核后生效')
    relationDialogVisible.value = false
    relationSourceId.value = null
    relationTargetId.value = null
    relationSourceWord.value = ''
    relationTargetWord.value = ''
    selectedRelationType.value = ''
    
    nextTick(() => renderChart())
  } catch (error: any) {
    console.error(error)
    ElMessage.error(error?.message || '创建关系失败')
  } finally {
    creatingRelation.value = false
  }
}

const showContextMenu = (params: any) => {
  const nodeId = Number(params.data.id)
  const word = params.data.name
  
  contextMenuNodeId.value = nodeId
  contextMenuWord.value = word
  
  if (mindMapData.value) {
    nodeRelations.value = mindMapData.value.edges.filter(
      e => e.source === nodeId || e.target === nodeId
    )
  }
  
  const container = chartRef.value?.getBoundingClientRect()
  if (container) {
    contextMenuX.value = params.event.offsetX + 10
    contextMenuY.value = params.event.offsetY + 10
  }
  
  contextMenuVisible.value = true
}

const hideContextMenu = () => {
  contextMenuVisible.value = false
  contextMenuNodeId.value = null
  contextMenuWord.value = ''
  nodeRelations.value = []
}

const handleDeleteRelation = async (rel: MindMapEdge) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${getWordById(rel.source)} 和 ${getWordById(rel.target)} 之间的"${rel.label}"关系吗？`,
      '删除关系',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    await mindMapApi.deleteRelation(rel.id)
    
    if (mindMapData.value) {
      mindMapData.value.edges = mindMapData.value.edges.filter(e => e.id !== rel.id)
    }
    
    nodeRelations.value = nodeRelations.value.filter(e => e.id !== rel.id)
    ElMessage.success('删除成功')
    hideContextMenu()
    nextTick(() => renderChart())
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error(error)
      ElMessage.error(error?.message || '删除失败')
    }
  }
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

const handleDocumentClick = () => {
  hideContextMenu()
}

onMounted(() => {
  const wordId = route.params.wordId
  if (wordId) {
    centerWordId.value = String(wordId)
    loadMindMap()
  }
  
  window.addEventListener('resize', handleResize)
  document.addEventListener('click', handleDocumentClick)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  document.removeEventListener('click', handleDocumentClick)
  chart.value?.dispose()
})
</script>

<style scoped>
.controls-card {
  margin-bottom: var(--space-md);
  background: white;
}

.edit-tip {
  margin-top: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  background: linear-gradient(135deg, #ecfdf5 0%, #d1fae5 100%);
  border: 1px solid #a7f3d0;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  color: #065f46;
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
  max-width: 800px;
  align-items: center;
  flex-wrap: wrap;
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
  height: calc(100vh - 340px);
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
.dot.user-contrib { background: #f59e0b; }

.path-line {
  width: 20px;
  height: 3px;
  background: repeating-linear-gradient(90deg, #f97316 0px, #f97316 4px, transparent 4px, transparent 8px);
  margin-right: 8px;
  border-radius: 2px;
}

.context-menu {
  position: absolute;
  z-index: 100;
  background: white;
  border-radius: var(--radius-md);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  border: 1px solid var(--c-border-light);
  min-width: 280px;
  max-width: 360px;
  padding: var(--space-sm);
}

.context-menu-header {
  padding: 4px 6px;
  font-size: var(--font-size-sm);
  color: var(--c-text-primary);
}

.context-menu-sub {
  color: var(--c-text-secondary);
  margin-left: 4px;
  font-size: 12px;
}

.context-menu-empty {
  padding: var(--space-md);
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.context-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 6px;
  border-radius: var(--radius-sm);
  transition: background-color 0.2s;
}

.context-menu-item:hover {
  background: var(--c-bg-subtle);
}

.relation-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.relation-target {
  font-size: var(--font-size-sm);
  color: var(--c-text-primary);
  font-weight: 500;
}

.relation-dialog :deep(.el-dialog__body) {
  padding-top: var(--space-md);
}

.relation-dialog-content {
  padding: 0;
}

.relation-nodes {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-sm);
}

.relation-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: var(--space-sm) var(--space-md);
  background: var(--c-bg-subtle);
  border-radius: var(--radius-md);
  min-width: 100px;
}

.relation-node.source {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}

.relation-node.target {
  background: linear-gradient(135deg, #d1fae5 0%, #a7f3d0 100%);
}

.node-label {
  font-size: 11px;
  color: var(--c-text-secondary);
}

.node-word {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text-primary);
}

.relation-arrow {
  font-size: 24px;
  color: var(--c-text-secondary);
}

.relation-types {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
}

.relation-type-card {
  padding: 12px;
  border: 2px solid var(--c-border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.relation-type-card:hover {
  border-color: var(--c-primary-light);
  background: var(--c-bg-subtle);
}

.relation-type-card.active {
  border-color: var(--c-primary);
  background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
}

.relation-type-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-primary);
  margin-bottom: 2px;
}

.relation-type-desc {
  font-size: 11px;
  color: var(--c-text-secondary);
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
  
  .relation-types {
    grid-template-columns: 1fr;
  }
}
</style>
