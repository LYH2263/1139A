<template>
  <div class="quiz-page page-container">
    <PageHeader title="单词测验" subtitle="通过测试检验学习成果。" />

    <div class="quiz-content" v-loading="loading">
      <!-- 1. Mode Selection Screen -->
      <transition name="el-fade-in">
        <div v-if="!modeSelected" class="mode-select-screen">
          <div class="hero-start">
            <div class="hero-icon">
              <el-icon><EditPen /></el-icon>
            </div>
            <h2 class="hero-title">选择测验模式</h2>
            <p class="hero-desc">选择适合你的测验方式，挑战自我！</p>
          </div>

          <div class="mode-cards">
            <div
              class="mode-card"
              :class="{ selected: selectedMode === 'CHOICE' }"
              @click="selectedMode = 'CHOICE'"
            >
              <div class="mode-icon choice-icon">
                <el-icon><List /></el-icon>
              </div>
              <h3 class="mode-title">选择题模式</h3>
              <p class="mode-desc">看英文单词，选择正确的中文释义</p>
              <ul class="mode-features">
                <li><el-icon><CircleCheck /></el-icon> 四选一</li>
                <li><el-icon><Timer /></el-icon> 不限时间</li>
                <li><el-icon><TrendCharts /></el-icon> 记录成绩</li>
              </ul>
            </div>

            <div
              class="mode-card"
              :class="{ selected: selectedMode === 'SPELLING' }"
              @click="selectedMode = 'SPELLING'"
            >
              <div class="mode-icon spelling-icon">
                <el-icon><Keyboard /></el-icon>
              </div>
              <h3 class="mode-title">拼写模式</h3>
              <p class="mode-desc">看中文释义，输入完整的英文单词</p>
              <ul class="mode-features">
                <li><el-icon><Edit /></el-icon> 手动输入</li>
                <li><el-icon><Timer /></el-icon> 30秒/题</li>
                <li><el-icon><Warning /></el-icon> 容错机制</li>
              </ul>
            </div>
          </div>

          <el-button
            type="primary"
            size="large"
            class="start-btn"
            :disabled="!selectedMode"
            @click="startQuiz"
          >
            开始测验
          </el-button>

          <SectionCard title="测验说明" class="info-card">
            <div class="info-grid">
              <div class="info-item">
                <el-icon class="info-icon"><Timer /></el-icon>
                <span>拼写模式限时30秒/题</span>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><CircleCheck /></el-icon>
                <span>即时反馈答题结果</span>
              </div>
              <div class="info-item">
                <el-icon class="info-icon"><TrendCharts /></el-icon>
                <span>分别统计各模式正确率</span>
              </div>
            </div>
          </SectionCard>
        </div>
      </transition>

      <!-- 2. Quiz In Progress -->
      <transition name="el-fade-in">
        <div v-if="quizStarted && !quizFinished" class="quiz-session">
          <el-card class="question-card">
            <div class="quiz-header">
              <div class="header-left">
                <span class="q-counter">Question {{ currentIndex + 1 }} / {{ questions.length }}</span>
                <el-tag :type="quizMode === 'CHOICE' ? 'primary' : 'success'" size="small" class="mode-tag">
                  {{ quizMode === 'CHOICE' ? '选择题' : '拼写题' }}
                </el-tag>
              </div>
              <div v-if="quizMode === 'SPELLING'" class="timer-container">
                <el-icon :class="{ 'timer-warning': timeLeft <= 10 }"><Timer /></el-icon>
                <span :class="{ 'timer-warning': timeLeft <= 10 }" class="timer-text">{{ timeLeft }}s</span>
              </div>
              <el-progress
                :percentage="Math.round(((currentIndex + 1) / questions.length) * 100)"
                :show-text="false"
                class="q-progress"
              />
            </div>

            <!-- Choice Mode -->
            <div v-if="quizMode === 'CHOICE'" class="question-body">
              <h1 class="target-word">{{ currentQuestion?.question }}</h1>
              <p class="instruction">请选择正确的中文释义：</p>

              <div class="options-list">
                <div
                  v-for="option in currentQuestion?.options"
                  :key="option"
                  class="option-item"
                  :class="{ selected: currentAnswer === option }"
                  @click="currentAnswer = option"
                >
                  <div class="radio-circle"></div>
                  <span class="option-text">{{ option }}</span>
                </div>
              </div>
            </div>

            <!-- Spelling Mode -->
            <div v-else class="question-body spelling-body">
              <div class="meaning-display">
                <h2 class="meaning-text">{{ currentQuestion?.meaning }}</h2>
              </div>
              <div class="hint-display">
                <span class="hint-text">{{ currentQuestion?.hint }}</span>
              </div>
              <p class="instruction">请输入完整的英文单词：</p>

              <el-input
                ref="spellingInput"
                v-model="currentAnswer"
                class="spelling-input"
                size="large"
                placeholder="请输入单词..."
                @keyup.enter="nextQuestion"
                autocomplete="off"
                :disabled="isAnswered"
              />

              <div v-if="isAnswered" class="answer-feedback" :class="feedbackClass">
                <el-icon>
                  <CircleCheck v-if="currentFeedback === 'CORRECT'" />
                  <Warning v-else-if="currentFeedback === 'PARTIAL'" />
                  <CircleClose v-else />
                </el-icon>
                <span>{{ feedbackText }}</span>
                <span v-if="currentFeedback !== 'CORRECT'" class="correct-answer">
                  正确答案：{{ currentQuestion?.correctAnswer }}
                </span>
              </div>
            </div>

            <div class="quiz-footer">
              <el-button
                v-if="quizMode === 'SPELLING' && !isAnswered"
                type="primary"
                size="large"
                class="next-btn"
                :disabled="!currentAnswer.trim()"
                @click="submitSpellingAnswer"
              >
                提交答案
              </el-button>
              <el-button
                v-else-if="quizMode === 'SPELLING' && isAnswered"
                type="primary"
                size="large"
                class="next-btn"
                @click="nextQuestion"
              >
                {{ isLastQuestion ? '查看结果' : '下一题' }}
              </el-button>
              <el-button
                v-else
                type="primary"
                size="large"
                class="next-btn"
                :disabled="!currentAnswer"
                @click="nextQuestion"
              >
                {{ isLastQuestion ? '提交试卷' : '下一题' }}
              </el-button>
            </div>
          </el-card>
        </div>
      </transition>

      <!-- 3. Result Screen -->
      <transition name="el-fade-in">
        <div v-if="quizFinished" class="result-screen">
          <el-card class="result-card">
            <div class="score-display" :class="scoreLevelClass">
              <div class="score-circle">
                <span class="score-value">{{ quizResult?.score }}</span>
                <span class="score-label">分</span>
              </div>
              <h2 class="result-title">{{ resultTitle }}</h2>
              <p class="result-subtitle">{{ resultSubtitle }}</p>
            </div>

            <div class="mode-badge">
              <el-tag :type="quizMode === 'CHOICE' ? 'primary' : 'success'" size="large">
                {{ quizMode === 'CHOICE' ? '选择题模式' : '拼写模式' }}
              </el-tag>
            </div>

            <div class="stats-row">
              <div class="stat-box correct">
                <div class="val">{{ quizResult?.correctCount }}</div>
                <div class="lbl">答对</div>
              </div>
              <div v-if="quizResult?.partialCount" class="stat-box partial">
                <div class="val">{{ quizResult?.partialCount }}</div>
                <div class="lbl">部分正确</div>
              </div>
              <div class="stat-box wrong">
                <div class="val">{{ quizResult?.wrongCount }}</div>
                <div class="lbl">答错</div>
              </div>
              <div class="stat-box">
                <div class="val">{{ quizResult?.totalCount }}</div>
                <div class="lbl">总题数</div>
              </div>
            </div>

            <div class="accuracy-section">
              <h4 class="section-title">正确率统计</h4>
              <div class="accuracy-grid">
                <div class="accuracy-item" :class="{ active: quizMode === 'CHOICE' }">
                  <div class="accuracy-label">
                    <el-icon><List /></el-icon>
                    <span>选择题</span>
                  </div>
                  <div class="accuracy-value">
                    {{ quizResult?.choiceAccuracy !== null ? quizResult.choiceAccuracy + '%' : '-' }}
                  </div>
                </div>
                <div class="accuracy-item" :class="{ active: quizMode === 'SPELLING' }">
                  <div class="accuracy-label">
                    <el-icon><Keyboard /></el-icon>
                    <span>拼写题</span>
                  </div>
                  <div class="accuracy-value">
                    {{ quizResult?.spellingAccuracy !== null ? quizResult.spellingAccuracy + '%' : '-' }}
                  </div>
                </div>
              </div>
            </div>

            <div class="stats-row">
              <div class="stat-box">
                <div class="val">{{ quizResult?.duration }}s</div>
                <div class="lbl">用时</div>
              </div>
              <div class="stat-box">
                <div class="val">{{ quizResult?.accuracy }}%</div>
                <div class="lbl">正确率</div>
              </div>
            </div>

            <el-divider v-if="quizResult?.answerDetails?.length">答题详情</el-divider>

            <div v-if="quizResult?.answerDetails?.length" class="answer-details-list">
              <div
                v-for="(detail, idx) in quizResult.answerDetails"
                :key="idx"
                class="detail-item"
                :class="detail.result.toLowerCase()"
              >
                <div class="detail-header">
                  <span class="detail-num">{{ idx + 1 }}</span>
                  <span class="detail-word">{{ detail.word }}</span>
                  <el-tag :type="getResultTagType(detail.result)" size="small">
                    {{ getResultText(detail.result) }}
                  </el-tag>
                </div>
                <div class="detail-body">
                  <div class="detail-meaning">{{ detail.meaning }}</div>
                  <div class="detail-answers">
                    <span v-if="detail.userAnswer" class="user-answer">你的答案：{{ detail.userAnswer }}</span>
                    <span v-else class="user-answer">你的答案：（未作答）</span>
                    <span class="correct-answer">正确答案：{{ detail.correctAnswer }}</span>
                  </div>
                  <div class="detail-feedback">{{ detail.feedback }}</div>
                </div>
              </div>
            </div>

            <el-divider v-if="quizResult?.wrongWords?.length">错题回顾</el-divider>

            <div v-if="quizResult?.wrongWords?.length" class="wrong-list">
              <div v-for="w in quizResult.wrongWords" :key="w.id" class="wrong-item">
                <span class="w-word">{{ w.word }}</span>
                <span class="w-meaning">{{ w.meaning }}</span>
              </div>
            </div>

            <div class="result-actions">
              <el-button @click="$router.push('/dashboard')">返回仪表盘</el-button>
              <el-button type="primary" @click="restartQuiz">再测一次</el-button>
            </div>
          </el-card>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen, Timer, CircleCheck, TrendCharts, List, Keyboard, Edit, Warning, CircleClose } from '@element-plus/icons-vue'
import type { QuizQuestion, QuizSubmitResponse, QuizMode as QuizModeType, QuizAnswer, AnswerResult } from '@/types'
import { quizApi } from '@/api/study'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'

const loading = ref(false)
const modeSelected = ref(false)
const quizStarted = ref(false)
const quizFinished = ref(false)
const selectedMode = ref<QuizModeType | null>(null)
const quizMode = ref<QuizModeType>('CHOICE')
const quizId = ref('')
const questions = ref<QuizQuestion[]>([])
const currentIndex = ref(0)
const currentAnswer = ref('')
const answers = ref<QuizAnswer[]>([])
const quizResult = ref<QuizSubmitResponse | null>(null)
const timeLeft = ref(30)
const timerRef = ref<number | null>(null)
const isAnswered = ref(false)
const currentFeedback = ref<AnswerResult | null>(null)
const spellingInput = ref<InstanceType<typeof import('element-plus').ElInput> | null>(null)

const currentQuestion = computed(() => questions.value[currentIndex.value] || null)
const isLastQuestion = computed(() => currentIndex.value === questions.value.length - 1)

const scoreLevelClass = computed(() => {
  const s = quizResult.value?.score || 0
  if (s >= 80) return 'level-high'
  if (s >= 60) return 'level-mid'
  return 'level-low'
})

const resultTitle = computed(() => {
  const s = quizResult.value?.score || 0
  if (s >= 90) return '太棒了！'
  if (s >= 80) return '成绩优秀'
  if (s >= 60) return '合格'
  return '继续努力'
})

const resultSubtitle = computed(() => {
  return quizResult.value?.score === 100
    ? '完美通关，你的词汇量令人印象深刻！'
    : '多加练习，你一定可以做得更好。'
})

const feedbackClass = computed(() => {
  if (!currentFeedback.value) return ''
  return currentFeedback.value.toLowerCase()
})

const feedbackText = computed(() => {
  switch (currentFeedback.value) {
    case 'CORRECT': return '回答正确！'
    case 'PARTIAL': return '接近正确，注意单复数形式'
    case 'WRONG': return '回答错误'
    case 'TIMEOUT': return '时间到！'
    default: return ''
  }
})

const getResultTagType = (result: AnswerResult) => {
  switch (result) {
    case 'CORRECT': return 'success'
    case 'PARTIAL': return 'warning'
    case 'WRONG': return 'danger'
    case 'TIMEOUT': return 'info'
    default: return 'info'
  }
}

const getResultText = (result: AnswerResult) => {
  switch (result) {
    case 'CORRECT': return '正确'
    case 'PARTIAL': return '部分正确'
    case 'WRONG': return '错误'
    case 'TIMEOUT': return '超时'
    default: return '未知'
  }
}

const startQuiz = async () => {
  if (!selectedMode.value) return
  loading.value = true
  try {
    const res = await quizApi.startQuiz(10, selectedMode.value)
    quizId.value = res.quizId
    quizMode.value = res.mode
    questions.value = res.questions
    modeSelected.value = true
    quizStarted.value = true
    currentIndex.value = 0
    currentAnswer.value = ''
    answers.value = []
    isAnswered.value = false
    currentFeedback.value = null
    if (selectedMode.value === 'SPELLING') {
      startTimer()
      await nextTick()
      spellingInput.value?.focus()
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const startTimer = () => {
  stopTimer()
  timeLeft.value = 30
  timerRef.value = window.setInterval(() => {
    timeLeft.value--
    if (timeLeft.value <= 0) {
      handleTimeout()
    }
  }, 1000)
}

const stopTimer = () => {
  if (timerRef.value) {
    clearInterval(timerRef.value)
    timerRef.value = null
  }
}

const handleTimeout = () => {
  stopTimer()
  if (!currentQuestion.value) return

  currentFeedback.value = 'TIMEOUT'
  isAnswered.value = true

  answers.value.push({
    wordId: currentQuestion.value.wordId,
    answer: currentAnswer.value,
    timedOut: true
  })
}

const submitSpellingAnswer = () => {
  if (!currentQuestion.value || !currentAnswer.value.trim()) return

  stopTimer()
  const userAnswer = currentAnswer.value.trim()
  const correctWord = currentQuestion.value.correctAnswer

  const normalizedCorrect = correctWord.trim().toLowerCase()
  const normalizedUser = userAnswer.trim().toLowerCase()

  if (normalizedCorrect === normalizedUser) {
    currentFeedback.value = 'CORRECT'
  } else if (isPluralVariant(normalizedCorrect, normalizedUser) || isPluralVariant(normalizedUser, normalizedCorrect)) {
    currentFeedback.value = 'PARTIAL'
  } else {
    currentFeedback.value = 'WRONG'
  }

  isAnswered.value = true

  answers.value.push({
    wordId: currentQuestion.value.wordId,
    answer: userAnswer,
    timedOut: false
  })
}

const isPluralVariant = (singular: string, plural: string): boolean => {
  if (!singular || !plural) return false
  if (plural === singular + 's') return true
  if (plural === singular + 'es') return true
  if (singular.endsWith('y') && plural === singular.substring(0, singular.length - 1) + 'ies') return true
  if (singular.endsWith('f') && plural === singular.substring(0, singular.length - 1) + 'ves') return true
  if (singular.endsWith('fe') && plural === singular.substring(0, singular.length - 2) + 'ves') return true
  return false
}

const nextQuestion = () => {
  if (quizMode.value === 'CHOICE') {
    if (!currentQuestion.value || !currentAnswer.value) return

    answers.value.push({
      wordId: currentQuestion.value.wordId,
      answer: currentAnswer.value
    })
  } else if (quizMode.value === 'SPELLING' && !isAnswered.value) {
    submitSpellingAnswer()
    return
  }

  if (isLastQuestion.value) {
    submitQuiz()
  } else {
    currentIndex.value++
    currentAnswer.value = ''
    isAnswered.value = false
    currentFeedback.value = null
    if (quizMode.value === 'SPELLING') {
      startTimer()
      nextTick().then(() => spellingInput.value?.focus())
    }
  }
}

const submitQuiz = async () => {
  stopTimer()
  loading.value = true
  try {
    const res = await quizApi.submitQuiz(quizId.value, answers.value)
    quizResult.value = res
    quizFinished.value = true
    ElMessage.success('测验完成！')
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const restartQuiz = () => {
  stopTimer()
  modeSelected.value = false
  quizStarted.value = false
  quizFinished.value = false
  selectedMode.value = null
  quizId.value = ''
  questions.value = []
  currentIndex.value = 0
  currentAnswer.value = ''
  answers.value = []
  quizResult.value = null
  timeLeft.value = 30
  isAnswered.value = false
  currentFeedback.value = null
}

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
.quiz-content {
  max-width: 700px;
  margin: 0 auto;
}

/* Mode Selection Screen */
.mode-select-screen {
  text-align: center;
  padding-top: var(--space-xl);
}

.hero-start {
  margin-bottom: var(--space-xl);
}

.hero-icon {
  font-size: 80px;
  color: var(--c-primary);
  margin-bottom: var(--space-md);
}

.hero-title {
  font-size: 32px;
  color: var(--c-text-primary);
  margin-bottom: var(--space-sm);
}

.hero-desc {
  color: var(--c-text-secondary);
  margin-bottom: var(--space-lg);
}

.mode-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(260px, 1fr));
  gap: var(--space-lg);
  margin-bottom: var(--space-xl);
}

.mode-card {
  background: white;
  border: 2px solid var(--c-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  cursor: pointer;
  transition: all 0.3s ease;
  text-align: left;
}

.mode-card:hover {
  border-color: var(--c-primary-light);
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
}

.mode-card.selected {
  border-color: var(--c-primary);
  background: var(--c-primary-bg);
}

.mode-icon {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 28px;
  margin-bottom: var(--space-md);
}

.choice-icon {
  background: #dbeafe;
  color: #2563eb;
}

.spelling-icon {
  background: #dcfce7;
  color: #16a34a;
}

.mode-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--c-text-primary);
  margin: 0 0 8px;
}

.mode-desc {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  margin: 0 0 var(--space-md);
}

.mode-features {
  list-style: none;
  padding: 0;
  margin: 0;
}

.mode-features li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  margin-bottom: 6px;
}

.start-btn {
  width: 200px;
  font-weight: 600;
  margin-bottom: var(--space-xl);
}

.info-grid {
  display: flex;
  justify-content: space-around;
  padding: var(--space-md);
}

.info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.info-icon {
  font-size: 24px;
  color: var(--c-primary-light);
}

/* Quiz Session */
.question-card {
  min-height: 500px;
  display: flex;
  flex-direction: column;
}

.quiz-header {
  margin-bottom: var(--space-lg);
}

.header-left {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.q-counter {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
}

.mode-tag {
  margin-left: auto;
}

.timer-container {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--c-text-secondary);
  font-weight: 600;
  margin-left: var(--space-md);
}

.timer-text {
  font-size: 18px;
}

.timer-warning {
  color: var(--c-danger) !important;
  animation: pulse 1s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.question-body {
  flex: 1;
}

.target-word {
  font-size: 36px;
  color: var(--c-text-primary);
  text-align: center;
  margin-bottom: var(--space-md);
}

.instruction {
  text-align: center;
  color: var(--c-text-secondary);
  margin-bottom: var(--space-lg);
}

.options-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.option-item {
  display: flex;
  align-items: center;
  padding: var(--space-md) var(--space-lg);
  border: 2px solid var(--c-border-light);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 0.2s;
}

.option-item:hover {
  border-color: var(--c-primary-light);
  background: var(--c-bg-body);
}

.option-item.selected {
  border-color: var(--c-primary);
  background: var(--c-primary-bg);
}

.radio-circle {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  border: 2px solid var(--c-border);
  margin-right: var(--space-md);
  flex-shrink: 0;
}

.selected .radio-circle {
  border-color: var(--c-primary);
  background: var(--c-primary);
  box-shadow: inset 0 0 0 4px white;
}

.option-text {
  font-size: var(--font-size-base);
  color: var(--c-text-primary);
}

/* Spelling Mode */
.spelling-body {
  display: flex;
  flex-direction: column;
}

.meaning-display {
  background: var(--c-bg-body);
  padding: var(--space-lg);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-lg);
}

.meaning-text {
  font-size: 28px;
  color: var(--c-text-primary);
  text-align: center;
  margin: 0;
  font-weight: 600;
}

.hint-display {
  text-align: center;
  margin-bottom: var(--space-lg);
}

.hint-text {
  font-family: 'Courier New', monospace;
  font-size: 32px;
  font-weight: 700;
  color: var(--c-primary);
  letter-spacing: 8px;
}

.spelling-input {
  margin-bottom: var(--space-lg);
}

.spelling-input :deep(.el-input__wrapper) {
  font-size: 20px;
  padding: 12px 16px;
  font-family: 'Courier New', monospace;
}

.answer-feedback {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  font-weight: 600;
  margin-bottom: var(--space-lg);
}

.answer-feedback.correct {
  background: #f0fdf4;
  color: #16a34a;
}

.answer-feedback.partial {
  background: #fffbeb;
  color: #d97706;
}

.answer-feedback.wrong,
.answer-feedback.timeout {
  background: #fef2f2;
  color: #dc2626;
}

.answer-feedback .correct-answer {
  font-weight: 500;
  font-size: var(--font-size-sm);
  opacity: 0.9;
}

.quiz-footer {
  margin-top: var(--space-xl);
  text-align: center;
}

.next-btn {
  width: 100%;
}

/* Result Screen */
.result-screen {
  text-align: center;
}

.score-display {
  padding: var(--space-lg);
  background: var(--c-bg-body);
  border-radius: var(--radius-lg);
  margin-bottom: var(--space-md);
}

.score-circle {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 8px solid currentColor;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto var(--space-md);
}

.score-value {
  font-size: 48px;
  font-weight: 800;
  line-height: 1;
}

.score-label {
  font-size: 16px;
  margin-top: 12px;
  margin-left: 2px;
}

.score-display.level-high { color: var(--c-success); }
.score-display.level-mid { color: var(--c-warning); }
.score-display.level-low { color: var(--c-danger); }

.mode-badge {
  margin-bottom: var(--space-lg);
}

.result-title {
  margin: 0 0 8px;
}

.result-subtitle {
  color: var(--c-text-secondary);
  margin: 0;
}

.stats-row {
  display: flex;
  justify-content: space-around;
  margin-bottom: var(--space-lg);
}

.stat-box {
  text-align: center;
}

.stat-box .val {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.stat-box.correct .val { color: var(--c-success); }
.stat-box.partial .val { color: var(--c-warning); }
.stat-box.wrong .val { color: var(--c-danger); }

.stat-box .lbl {
  font-size: 12px;
  color: var(--c-text-tertiary);
}

.accuracy-section {
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
}

.section-title {
  margin: 0 0 var(--space-md);
  font-size: 16px;
  color: var(--c-text-primary);
  text-align: left;
}

.accuracy-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-md);
}

.accuracy-item {
  background: white;
  border-radius: var(--radius-md);
  padding: var(--space-md);
  border: 2px solid transparent;
  transition: all 0.2s;
}

.accuracy-item.active {
  border-color: var(--c-primary);
  background: var(--c-primary-bg);
}

.accuracy-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  margin-bottom: 4px;
}

.accuracy-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--c-text-primary);
}

.answer-details-list {
  text-align: left;
  margin-bottom: var(--space-lg);
  max-height: 400px;
  overflow-y: auto;
}

.detail-item {
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-sm);
  overflow: hidden;
}

.detail-item.correct { border-color: var(--c-success); }
.detail-item.partial { border-color: var(--c-warning); }
.detail-item.wrong,
.detail-item.timeout { border-color: var(--c-danger); }

.detail-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  padding: var(--space-sm) var(--space-md);
  background: var(--c-bg-body);
}

.detail-num {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--c-primary);
  color: white;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-word {
  font-weight: 600;
  color: var(--c-text-primary);
  flex: 1;
}

.detail-body {
  padding: var(--space-sm) var(--space-md);
}

.detail-meaning {
  color: var(--c-text-secondary);
  margin-bottom: 4px;
}

.detail-answers {
  display: flex;
  flex-direction: column;
  gap: 2px;
  font-size: var(--font-size-sm);
}

.user-answer {
  color: var(--c-text-secondary);
}

.correct-answer {
  color: var(--c-success);
  font-weight: 500;
}

.detail-feedback {
  margin-top: 4px;
  font-size: var(--font-size-sm);
  color: var(--c-text-tertiary);
  font-style: italic;
}

.wrong-list {
  text-align: left;
  background: #fef2f2;
  padding: var(--space-md);
  border-radius: var(--radius-md);
  margin-bottom: var(--space-lg);
}

.wrong-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid rgba(0,0,0,0.05);
}

.w-word { font-weight: 600; color: var(--c-danger); }
.w-meaning { color: var(--c-text-secondary); }

.result-actions {
  display: flex;
  justify-content: center;
  gap: var(--space-md);
}

/* Responsive */
@media (max-width: 640px) {
  .hero-title { font-size: 24px; }
  .target-word { font-size: 28px; }
  .meaning-text { font-size: 22px; }
  .hint-text { font-size: 24px; }
  .mode-cards {
    grid-template-columns: 1fr;
  }
}
</style>
