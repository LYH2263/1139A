<template>
  <div class="review-page page-container">
    <PageHeader 
      :title="wordBookId ? `词书复习：${wordBookName}` : '今日复习'" 
      :subtitle="wordBookId ? '专注学习当前词书内的单词。' : '基于艾宾浩斯曲线智能安排复习计划。'"
    >
      <template #actions>
        <el-dropdown @command="handleModeChange" trigger="click">
          <el-button>
            <el-icon><Setting /></el-icon>
            {{ currentModeLabel }}
            <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="CARD" :disabled="reviewMode === 'CARD'">
                <el-icon><Tickets /></el-icon>卡片模式
              </el-dropdown-item>
              <el-dropdown-item command="LIST" :disabled="reviewMode === 'LIST'">
                <el-icon><ListIcon /></el-icon>列表模式
              </el-dropdown-item>
              <el-dropdown-item command="DICTATION" :disabled="reviewMode === 'DICTATION'">
                <el-icon><Microphone /></el-icon>听写模式
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </template>
    </PageHeader>

    <div class="review-content" v-loading="loading">
      <!-- Empty State -->
      <div v-if="!hasActiveWords && reviewList.length === 0 && !loading" class="empty-view">
        <EmptyState 
          title="无需复习" 
          description="您今天已经完成所有复习任务，去学习新单词或进行测验吧！"
          icon="Calendar"
        >
          <template #action>
            <div class="empty-actions">
              <el-button type="primary" @click="$router.push('/words')">添加单词</el-button>
              <el-button @click="$router.push('/quiz')">去测验</el-button>
            </div>
          </template>
        </EmptyState>
        
        <SectionCard class="algo-explanation">
          <template #header>
            <span class="algo-title">🧠 记忆算法说明</span>
          </template>
          <div class="algo-steps">
            <div class="step-item">
              <div class="step-icon known"><el-icon><Check /></el-icon></div>
              <div class="step-text">
                <strong>认识 (熟练度 +1)</strong>
                <span>连续3次自动移出复习队列</span>
              </div>
            </div>
            <div class="step-item">
              <div class="step-icon vague"><el-icon><Warning /></el-icon></div>
              <div class="step-text">
                <strong>模糊 (熟练度 -1)</strong>
                <span>复习间隔缩短</span>
              </div>
            </div>
            <div class="step-item">
              <div class="step-icon unknown"><el-icon><Close /></el-icon></div>
              <div class="step-text">
                <strong>不认识 (归零)</strong>
                <span>连续2次额外出现1次</span>
              </div>
            </div>
          </div>
        </SectionCard>
      </div>

      <!-- Review Session -->
      <template v-else-if="hasActiveWords || reviewList.length > 0">
        <!-- Real-time Stats Bar -->
        <ReviewStatsBar :stats="sessionStats" />

        <!-- Adaptive Notification -->
        <transition name="el-fade-in">
          <el-alert 
            v-if="notification.show" 
            :type="notification.type" 
            :title="notification.title"
            :description="notification.message"
            show-icon
            :closable="false"
            class="adaptive-notification"
          />
        </transition>

        <!-- Card Mode -->
        <div v-if="reviewMode === 'CARD'" class="review-session">
          <el-card class="review-card">
            <div class="session-progress">
              <span class="progress-text">进度 {{ currentIndex + 1 }} / {{ activeList.length }}</span>
              <el-progress 
                :percentage="Math.round(((currentIndex + 1) / activeList.length) * 100)" 
                :show-text="false"
                stroke-width="6"
              />
            </div>

            <div class="flashcard">
              <h1 class="word-text">{{ currentWord?.word }}</h1>
              <span class="phonetic" v-if="currentWord?.phonetic">/{{ currentWord?.phonetic }}/</span>
              
              <transition name="el-fade-in">
                <div v-if="showAnswer" class="answer-area">
                  <el-divider />
                  <p class="meaning">{{ currentWord?.meaning }}</p>
                  <p v-if="currentWord?.example" class="example">{{ currentWord?.example }}</p>
                </div>
              </transition>
            </div>

            <div class="action-area">
              <el-button 
                v-if="!showAnswer" 
                type="primary" 
                size="large" 
                class="reveal-btn"
                @click="showAnswer = true"
              >
                显示答案
              </el-button>

              <div v-else class="feedback-actions">
                <div class="action-col">
                  <el-button type="danger" size="large" circle class="action-btn" @click="submitReview('UNKNOWN')">
                    <el-icon><Close /></el-icon>
                  </el-button>
                  <span class="action-label">不认识</span>
                </div>
                
                <div class="action-col">
                  <el-button type="warning" size="large" circle class="action-btn" @click="submitReview('VAGUE')">
                    <el-icon><Warning /></el-icon>
                  </el-button>
                  <span class="action-label">模糊</span>
                </div>
                
                <div class="action-col">
                  <el-button type="success" size="large" circle class="action-btn" @click="submitReview('KNOWN')">
                    <el-icon><Check /></el-icon>
                  </el-button>
                  <span class="action-label">认识</span>
                </div>
              </div>
            </div>
          </el-card>
        </div>

        <!-- List Mode -->
        <div v-else-if="reviewMode === 'LIST'" class="list-mode">
          <div class="list-header">
            <h3>待复习单词列表</h3>
            <span class="list-subtitle">点击单词快速标记状态</span>
          </div>
          
          <div class="word-list">
            <div 
              v-for="(word, index) in reviewList" 
              :key="word.wordId + '-' + index"
              class="word-list-item"
              :class="{ 
                marked: word.marked, 
                known: word.markedResult === 'KNOWN',
                vague: word.markedResult === 'VAGUE',
                unknown: word.markedResult === 'UNKNOWN',
                removed: removedWords.has(word.wordId)
              }"
            >
              <div class="word-info">
                <div class="word-main">
                  <span class="word">{{ word.word }}</span>
                  <span class="phonetic" v-if="word.phonetic">/{{ word.phonetic }}/</span>
                </div>
                <div class="word-meaning" v-if="word.marked || showAllMeanings">
                  {{ word.meaning }}
                </div>
              </div>
              
              <div class="word-actions" v-if="!word.marked && !removedWords.has(word.wordId)">
                <el-button 
                  type="danger" 
                  size="small" 
                  circle 
                  @click.stop="markWord(word, 'UNKNOWN')"
                >
                  <el-icon><Close /></el-icon>
                </el-button>
                <el-button 
                  type="warning" 
                  size="small" 
                  circle 
                  @click.stop="markWord(word, 'VAGUE')"
                >
                  <el-icon><Warning /></el-icon>
                </el-button>
                <el-button 
                  type="success" 
                  size="small" 
                  circle 
                  @click.stop="markWord(word, 'KNOWN')"
                >
                  <el-icon><Check /></el-icon>
                </el-button>
              </div>
              
              <div class="word-status" v-else-if="word.marked">
                <el-tag :type="getResultTagType(word.markedResult!)" size="small">
                  {{ getResultLabel(word.markedResult!) }}
                </el-tag>
              </div>
              
              <div class="word-status" v-else-if="removedWords.has(word.wordId)">
                <el-tag type="info" size="small">已掌握</el-tag>
              </div>
            </div>
          </div>
          
          <div class="list-footer">
            <el-checkbox v-model="showAllMeanings">显示全部释义</el-checkbox>
            <el-button 
              type="primary" 
              :disabled="!canSubmitList"
              @click="submitAllListMarks"
            >
              提交已标记 ({{ markedCount }}/{{ reviewList.length }})
            </el-button>
          </div>
        </div>

        <!-- Dictation Mode -->
        <div v-else-if="reviewMode === 'DICTATION'" class="dictation-mode">
          <el-card class="dictation-card">
            <div class="session-progress">
              <span class="progress-text">进度 {{ currentIndex + 1 }} / {{ activeList.length }}</span>
              <el-progress 
                :percentage="Math.round(((currentIndex + 1) / activeList.length) * 100)" 
                :show-text="false"
                stroke-width="6"
              />
            </div>

            <div class="dictation-content">
              <div class="audio-section">
                <el-button 
                  type="primary" 
                  size="large" 
                  circle
                  class="play-btn"
                  @click="playPronunciation"
                  :disabled="isPlaying"
                >
                  <el-icon :size="32"><VideoPlay /></el-icon>
                </el-button>
                <p class="audio-hint">{{ isPlaying ? '正在播放...' : '点击播放发音' }}</p>
                <p class="hint-text" v-if="showDictationHint">
                  提示：共 {{ currentWord?.word.length }} 个字母，首字母是 "{{ currentWord?.word?.[0]?.toUpperCase() }}"
                </p>
              </div>

              <div class="input-section">
                <el-input 
                  v-model="dictationInput"
                  placeholder="请输入听到的单词..."
                  size="large"
                  class="dictation-input"
                  @keyup.enter="checkDictation"
                  :disabled="showDictationResult"
                  clearable
                />
                
                <transition name="el-fade-in">
                  <div v-if="showDictationResult" class="dictation-result">
                    <div v-if="dictationCorrect" class="result-correct">
                      <el-icon size="24" color="var(--c-success)"><CircleCheck /></el-icon>
                      <span>正确！</span>
                    </div>
                    <div v-else class="result-wrong">
                      <el-icon size="24" color="var(--c-danger)"><CircleClose /></el-icon>
                      <span>正确答案：<strong>{{ currentWord?.word }}</strong></span>
                    </div>
                    <div class="word-meaning-show">
                      <p class="meaning">{{ currentWord?.meaning }}</p>
                      <p v-if="currentWord?.example" class="example">{{ currentWord?.example }}</p>
                    </div>
                  </div>
                </transition>
              </div>

              <div class="action-area">
                <el-button 
                  v-if="!showDictationResult" 
                  type="primary" 
                  size="large"
                  @click="checkDictation"
                >
                  检查
                </el-button>
                
                <div v-else class="feedback-actions">
                  <div class="action-col">
                    <el-button type="danger" size="large" circle class="action-btn" @click="submitReview('UNKNOWN')">
                      <el-icon><Close /></el-icon>
                    </el-button>
                    <span class="action-label">不认识</span>
                  </div>
                  
                  <div class="action-col">
                    <el-button type="warning" size="large" circle class="action-btn" @click="submitReview('VAGUE')">
                      <el-icon><Warning /></el-icon>
                    </el-button>
                    <span class="action-label">模糊</span>
                  </div>
                  
                  <div class="action-col">
                    <el-button type="success" size="large" circle class="action-btn" @click="submitReview(dictationCorrect ? 'KNOWN' : 'UNKNOWN')">
                      <el-icon><Check /></el-icon>
                    </el-button>
                    <span class="action-label">{{ dictationCorrect ? '认识' : '再记一次' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </el-card>
        </div>
      </template>

      <!-- Completion State -->
      <div v-else class="completion-view">
        <EmptyState 
          title="复习完成！" 
          description="您已完成今日所有复习任务。"
          icon="CircleCheck"
        >
          <template #action>
            <div class="completion-stats">
              <StatCard 
                label="已复习" 
                :value="sessionStats.reviewedCount" 
                type="primary"
              >
                <template #icon><Tickets /></template>
              </StatCard>
              <StatCard 
                label="认识率" 
                :value="sessionStats.knownRate + '%'" 
                type="success"
              >
                <template #icon><TrendCharts /></template>
              </StatCard>
              <StatCard 
                label="掌握单词" 
                :value="removedWords.size" 
                type="success"
              >
                <template #icon><Star /></template>
              </StatCard>
            </div>
            <el-button type="primary" size="large" @click="$router.push('/dashboard')">
              返回仪表盘
            </el-button>
          </template>
        </EmptyState>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Calendar, Check, Close, Warning, CircleCheck, Setting, ArrowDown, 
  Tickets, List as ListIcon, Microphone, VideoPlay, CircleClose,
  TrendCharts, Star, Notebook
} from '@element-plus/icons-vue'
import type { ReviewRecord, ReviewMode, SessionStats } from '@/types'
import { reviewApi } from '@/api/study'
import { wordBookApi } from '@/api/wordbook'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SectionCard from '@/components/ui/SectionCard.vue'
import ReviewStatsBar from '@/components/ui/ReviewStatsBar.vue'
import StatCard from '@/components/ui/StatCard.vue'

const route = useRoute()
const loading = ref(false)
const reviewList = ref<ReviewRecord[]>([])
const sessionId = ref('')
const reviewMode = ref<ReviewMode>('CARD')
const currentIndex = ref(0)
const showAnswer = ref(false)
const removedWords = ref<Set<number>>(new Set())
const wordBookId = ref<number | null>(null)
const wordBookName = ref<string>('')

const showAllMeanings = ref(false)
const dictationInput = ref('')
const showDictationResult = ref(false)
const dictationCorrect = ref(false)
const isPlaying = ref(false)
const showDictationHint = ref(false)

const notification = ref({
  show: false,
  type: 'success' as 'success' | 'warning' | 'info',
  title: '',
  message: ''
})

const sessionStats = ref<SessionStats>({
  reviewedCount: 0,
  knownCount: 0,
  unknownCount: 0,
  vagueCount: 0,
  knownRate: 0,
  remainingCount: 0,
  estimatedRemainingSeconds: 0
})

let statsPollingTimer: number | null = null

const currentModeLabel = computed(() => {
  const labels: Record<ReviewMode, string> = {
    CARD: '卡片模式',
    LIST: '列表模式',
    DICTATION: '听写模式'
  }
  return labels[reviewMode.value]
})

const activeList = computed(() => {
  return reviewList.value.filter(w => !removedWords.value.has(w.wordId))
})

const hasActiveWords = computed(() => {
  return currentIndex.value < activeList.value.length
})

const currentWord = computed(() => {
  if (currentIndex.value < activeList.value.length) {
    return activeList.value[currentIndex.value]
  }
  return null
})

const markedCount = computed(() => {
  return reviewList.value.filter(w => w.marked).length
})

const canSubmitList = computed(() => {
  return markedCount.value > 0
})

const showNotification = (type: 'success' | 'warning' | 'info', title: string, message: string) => {
  notification.value = { show: true, type, title, message }
  setTimeout(() => {
    notification.value.show = false
  }, 3000)
}

const getResultTagType = (result: string) => {
  switch (result) {
    case 'KNOWN': return 'success'
    case 'VAGUE': return 'warning'
    case 'UNKNOWN': return 'danger'
    default: return 'info'
  }
}

const getResultLabel = (result: string) => {
  switch (result) {
    case 'KNOWN': return '认识'
    case 'VAGUE': return '模糊'
    case 'UNKNOWN': return '不认识'
    default: return ''
  }
}

const fetchTodayReviews = async () => {
  loading.value = true
  try {
    let res
    if (wordBookId.value) {
      const wb = await wordBookApi.getWordBookById(wordBookId.value)
      wordBookName.value = wb.name
      res = await reviewApi.getWordBookReviews(wordBookId.value)
    } else {
      res = await reviewApi.getTodayReviews()
    }
    reviewList.value = res.list.map(w => ({ ...w, marked: false }))
    sessionId.value = res.sessionId
    reviewMode.value = res.reviewMode
    currentIndex.value = 0
    showAnswer.value = false
    removedWords.value.clear()
    resetDictationState()
    startStatsPolling()
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const startStatsPolling = () => {
  stopStatsPolling()
  fetchSessionStats()
  statsPollingTimer = window.setInterval(fetchSessionStats, 5000)
}

const stopStatsPolling = () => {
  if (statsPollingTimer) {
    clearInterval(statsPollingTimer)
    statsPollingTimer = null
  }
}

const fetchSessionStats = async () => {
  if (!sessionId.value) return
  try {
    const stats = await reviewApi.getSessionStats(sessionId.value)
    sessionStats.value = stats
  } catch (error) {
    console.error('Failed to fetch stats:', error)
  }
}

const handleModeChange = async (mode: string) => {
  try {
    await reviewApi.saveSettings(mode)
    reviewMode.value = mode as ReviewMode
    currentIndex.value = 0
    showAnswer.value = false
    resetDictationState()
    ElMessage.success(`已切换到${currentModeLabel.value}`)
  } catch (error) {
    console.error(error)
    ElMessage.error('切换模式失败')
  }
}

const submitReview = async (result: string) => {
  if (!currentWord.value) return
  const currentWordId = currentWord.value.wordId
  
  try {
    const response = await reviewApi.submitReview(
      currentWordId, 
      result, 
      sessionId.value
    )
    
    const wasRemoved = response.removedFromQueue
    const wasAdded = response.addedToQueueEnd
    
    if (wasRemoved) {
      removedWords.value.add(currentWordId)
      showNotification(
        'success', 
        '🎉 已掌握', 
        `"${currentWord.value.word}" 连续3次认识，已移出今日复习队列，下次复习间隔加倍！`
      )
    }
    
    if (wasAdded) {
      reviewList.value.push({ ...currentWord.value, marked: false })
      showNotification(
        'warning', 
        '📝 加强记忆', 
        `"${currentWord.value.word}" 连续2次不认识，将在本次复习中额外出现一次。`
      )
    }
    
    if (reviewMode.value === 'CARD' || reviewMode.value === 'DICTATION') {
      if (!wasRemoved) {
        currentIndex.value++
      }
      showAnswer.value = false
      resetDictationState()
      
      if (currentIndex.value >= activeList.value.length) {
        stopStatsPolling()
        fetchSessionStats()
        ElMessage.success('今日复习完成！')
      }
    }
    
    await fetchSessionStats()
  } catch (error) {
    console.error(error)
  }
}

const markWord = (word: ReviewRecord, result: string) => {
  word.marked = true
  word.markedResult = result
}

const submitAllListMarks = async () => {
  const markedWords = reviewList.value.filter(w => w.marked && !w.markedResult?.includes('_submitted'))
  
  for (const word of markedWords) {
    try {
      const response = await reviewApi.submitReview(
        word.wordId, 
        word.markedResult!, 
        sessionId.value
      )
      
      word.markedResult = word.markedResult + '_submitted'
      
      if (response.removedFromQueue) {
        removedWords.value.add(word.wordId)
      }
      
      if (response.addedToQueueEnd) {
        reviewList.value.push({ ...word, marked: false, markedResult: undefined })
      }
    } catch (error) {
      console.error('Failed to submit word:', word.wordId, error)
    }
  }
  
  await fetchSessionStats()
  showNotification('success', '提交成功', `已提交 ${markedWords.length} 个单词的复习结果`)
  
  const allSubmitted = reviewList.value.every(w => w.marked || removedWords.value.has(w.wordId))
  if (allSubmitted && activeList.value.length === 0) {
    stopStatsPolling()
    ElMessage.success('今日复习完成！')
  }
}

const playPronunciation = () => {
  if (!currentWord.value) return
  
  isPlaying.value = true
  const utterance = new SpeechSynthesisUtterance(currentWord.value.word)
  utterance.lang = 'en-US'
  utterance.rate = 0.8
  utterance.onend = () => {
    isPlaying.value = false
  }
  utterance.onerror = () => {
    isPlaying.value = false
    ElMessage.error('语音播放失败，请检查浏览器设置')
  }
  
  window.speechSynthesis.speak(utterance)
}

const checkDictation = () => {
  if (!currentWord.value || !dictationInput.value.trim()) {
    ElMessage.warning('请输入单词')
    return
  }
  
  const userInput = dictationInput.value.trim().toLowerCase()
  const correctAnswer = currentWord.value.word.toLowerCase()
  dictationCorrect.value = userInput === correctAnswer
  showDictationResult.value = true
  
  if (!dictationCorrect.value) {
    showDictationHint.value = true
  }
}

const resetDictationState = () => {
  dictationInput.value = ''
  showDictationResult.value = false
  dictationCorrect.value = false
  showDictationHint.value = false
  window.speechSynthesis.cancel()
}

watch(reviewMode, (newMode) => {
  if (newMode === 'DICTATION') {
    setTimeout(playPronunciation, 500)
  }
})

onMounted(() => {
  const wbId = route.query.wordBookId
  if (wbId) {
    wordBookId.value = Number(wbId)
  }
  fetchTodayReviews()
})

onUnmounted(() => {
  stopStatsPolling()
  window.speechSynthesis.cancel()
})
</script>

<style scoped>
.review-content {
  max-width: 700px;
  margin: 0 auto;
}

.adaptive-notification {
  margin-bottom: var(--space-lg);
}

/* Empty View */
.empty-view {
  display: flex;
  flex-direction: column;
  gap: var(--space-xl);
}

.empty-actions {
  display: flex;
  gap: var(--space-md);
}

.algo-explanation {
  background: var(--c-bg-card);
}

.algo-title {
  font-weight: 600;
  font-size: var(--font-size-md);
}

.algo-steps {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-md);
}

.step-item {
  text-align: center;
  padding: var(--space-sm);
}

.step-icon {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-sm);
  color: white;
}

.step-icon.known { background-color: var(--c-success); }
.step-icon.vague { background-color: var(--c-warning); }
.step-icon.unknown { background-color: var(--c-danger); }

.step-text {
  display: flex;
  flex-direction: column;
  font-size: var(--font-size-sm);
}

/* Card Mode */
.review-card {
  min-height: 500px;
  display: flex;
  flex-direction: column;
  position: relative;
  border-radius: var(--radius-lg);
}

.session-progress {
  margin-bottom: var(--space-xl);
}

.progress-text {
  display: block;
  text-align: center;
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
  margin-bottom: var(--space-xs);
}

.flashcard {
  flex: 1;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: start;
  padding-top: var(--space-xl);
}

.word-text {
  font-size: 48px;
  font-weight: 700;
  color: var(--c-text-primary);
  margin-bottom: var(--space-xs);
}

.phonetic {
  color: var(--c-text-secondary);
  font-family: serif;
  font-size: 20px;
  margin-bottom: var(--space-lg);
}

.answer-area {
  width: 100%;
  margin-top: var(--space-md);
}

.meaning {
  font-size: 24px;
  color: var(--c-primary);
  font-weight: 500;
  margin-bottom: var(--space-md);
}

.example {
  background: var(--c-bg-body);
  padding: var(--space-md);
  border-radius: var(--radius-md);
  font-style: italic;
  color: var(--c-text-secondary);
}

/* Actions */
.action-area {
  margin-top: var(--space-xl);
  padding-top: var(--space-lg);
  display: flex;
  justify-content: center;
  height: 80px;
}

.reveal-btn {
  width: 200px;
  font-weight: 600;
}

.feedback-actions {
  display: flex;
  gap: 40px;
}

.action-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.action-btn {
  width: 64px;
  height: 64px;
  font-size: 24px;
  transition: transform var(--transition-fast);
}

.action-btn:hover {
  transform: scale(1.1);
}

.action-label {
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
}

/* List Mode */
.list-mode {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}

.list-header {
  text-align: center;
  margin-bottom: var(--space-lg);
}

.list-header h3 {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-size-lg);
  color: var(--c-text-primary);
}

.list-subtitle {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.word-list {
  max-height: 500px;
  overflow-y: auto;
  margin-bottom: var(--space-lg);
}

.word-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-sm);
  background: var(--c-bg-body);
  transition: all var(--transition-fast);
}

.word-list-item:hover {
  background: var(--c-primary-bg);
}

.word-list-item.marked.known {
  background: rgba(16, 185, 129, 0.1);
  border-left: 3px solid var(--c-success);
}

.word-list-item.marked.vague {
  background: rgba(245, 158, 11, 0.1);
  border-left: 3px solid var(--c-warning);
}

.word-list-item.marked.unknown {
  background: rgba(239, 68, 68, 0.1);
  border-left: 3px solid var(--c-danger);
}

.word-list-item.removed {
  opacity: 0.6;
  background: rgba(59, 130, 246, 0.1);
}

.word-info {
  flex: 1;
}

.word-main {
  display: flex;
  align-items: baseline;
  gap: var(--space-sm);
}

.word-main .word {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
}

.word-main .phonetic {
  font-size: var(--font-size-sm);
  margin: 0;
}

.word-meaning {
  margin-top: var(--space-xs);
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.word-actions {
  display: flex;
  gap: var(--space-sm);
}

.word-actions .el-button {
  width: 36px;
  height: 36px;
}

.word-status {
  min-width: 60px;
  text-align: right;
}

.list-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: var(--space-md);
  border-top: 1px solid var(--c-border-light);
}

/* Dictation Mode */
.dictation-card {
  min-height: 550px;
  display: flex;
  flex-direction: column;
  border-radius: var(--radius-lg);
}

.dictation-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.audio-section {
  text-align: center;
  padding: var(--space-xl) 0;
}

.play-btn {
  width: 80px;
  height: 80px;
  margin-bottom: var(--space-md);
  background: linear-gradient(135deg, var(--c-primary), var(--c-primary-dark));
  border: none;
}

.play-btn:hover {
  transform: scale(1.05);
}

.audio-hint {
  color: var(--c-text-secondary);
  margin: 0 0 var(--space-sm);
}

.hint-text {
  color: var(--c-primary);
  font-size: var(--font-size-sm);
  margin: 0;
}

.input-section {
  padding: 0 var(--space-xl);
}

.dictation-input {
  text-align: center;
  font-size: var(--font-size-lg);
  letter-spacing: 2px;
}

.dictation-result {
  margin-top: var(--space-lg);
  text-align: center;
}

.result-correct, .result-wrong {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: 600;
  margin-bottom: var(--space-md);
}

.result-correct {
  color: var(--c-success);
}

.result-wrong {
  color: var(--c-danger);
}

.word-meaning-show {
  background: var(--c-bg-body);
  padding: var(--space-md);
  border-radius: var(--radius-md);
}

.word-meaning-show .meaning {
  font-size: var(--font-size-md);
  margin: 0 0 var(--space-sm);
}

.word-meaning-show .example {
  font-size: var(--font-size-sm);
  margin: 0;
}

/* Completion */
.completion-view {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.completion-stats {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--space-md);
  margin-bottom: var(--space-xl);
}

@media (max-width: 640px) {
  .algo-steps {
    grid-template-columns: 1fr;
    text-align: left;
  }
  
  .step-item {
    display: flex;
    align-items: center;
    gap: var(--space-md);
    margin: 0;
  }
  
  .step-icon {
    margin: 0;
  }
  
  .word-text {
    font-size: 36px;
  }
  
  .feedback-actions {
    gap: 20px;
  }
  
  .completion-stats {
    grid-template-columns: 1fr;
  }
  
  .list-footer {
    flex-direction: column;
    gap: var(--space-md);
  }
}
</style>
