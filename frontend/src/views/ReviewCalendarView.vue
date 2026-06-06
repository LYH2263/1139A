<template>
  <div class="calendar-page page-container">
    <PageHeader
      title="复习日历"
      subtitle="查看过去的复习记录和未来的复习安排，一目了然掌握复习节奏。"
    >
      <template #actions>
        <el-button @click="$router.push('/review')">
          <el-icon><Back /></el-icon>
          返回今日复习
        </el-button>
      </template>
    </PageHeader>

    <div class="calendar-content" v-loading="loading">
      <div class="calendar-wrapper">
        <div class="calendar-header">
          <el-button-group>
            <el-button @click="prevMonth">
              <el-icon><ArrowLeft /></el-icon>
            </el-button>
            <el-button @click="goToToday">
              今天
            </el-button>
            <el-button @click="nextMonth">
              <el-icon><ArrowRight /></el-icon>
            </el-button>
          </el-button-group>
          <h2 class="month-title">{{ displayMonth }}</h2>
          <div class="legend">
            <div class="legend-item">
              <span class="legend-color color-gray"></span>
              <span>0 个</span>
            </div>
            <div class="legend-item">
              <span class="legend-color color-green"></span>
              <span>1-3 个</span>
            </div>
            <div class="legend-item">
              <span class="legend-color color-yellow"></span>
              <span>4-7 个</span>
            </div>
            <div class="legend-item">
              <span class="legend-color color-red"></span>
              <span>8+ 个</span>
            </div>
          </div>
        </div>

        <div class="weekday-row">
          <div v-for="day in weekDays" :key="day" class="weekday-cell">
            {{ day }}
          </div>
        </div>

        <div class="calendar-grid">
          <div
            v-for="(cell, index) in calendarCells"
            :key="index"
            class="day-cell"
            :class="getCellClasses(cell)"
            @click="handleDateClick(cell)"
          >
            <div class="day-number">{{ cell.day }}</div>
            <div v-if="cell.stats" class="day-count" :class="getCountClass(cell.stats.totalCount)">
              {{ cell.stats.totalCount }}
            </div>
            <div v-if="cell.stats" class="day-detail">
              <span v-if="cell.stats.reviewedCount > 0" class="detail-item reviewed">
                ✓ {{ cell.stats.reviewedCount }}
              </span>
              <span v-if="cell.stats.pendingCount > 0" class="detail-item pending">
                ⏰ {{ cell.stats.pendingCount }}
              </span>
              <span v-if="cell.stats.predictedCount > 0" class="detail-item predicted">
                ◇ {{ cell.stats.predictedCount }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <transition name="slide-fade">
        <div v-if="selectedDate && dayDetail" class="day-detail-panel">
          <div class="detail-header">
            <div class="detail-title">
              <h3>{{ formatSelectedDate }}</h3>
              <div class="detail-summary">
                <el-tag v-if="dayDetail.reviewedCount > 0" type="success" size="small">
                  已复习 {{ dayDetail.reviewedCount }}
                </el-tag>
                <el-tag v-if="dayDetail.pendingCount > 0" type="warning" size="small">
                  待复习 {{ dayDetail.pendingCount }}
                </el-tag>
              </div>
            </div>
            <el-button circle @click="selectedDate = null">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>

          <div class="detail-body" v-loading="detailLoading">
            <div v-if="dayDetail.reviewedWords.length > 0" class="word-section">
              <h4 class="section-title reviewed-title">
                <el-icon><CircleCheck /></el-icon>
                已复习单词
              </h4>
              <div class="word-list">
                <div
                  v-for="word in dayDetail.reviewedWords"
                  :key="'r-' + word.wordId"
                  class="word-item reviewed"
                >
                  <div class="word-main">
                    <span class="word">{{ word.word }}</span>
                    <span v-if="word.phonetic" class="phonetic">/{{ word.phonetic }}/</span>
                    <el-tag :type="getResultTagType(word.result)" size="small" class="result-tag">
                      {{ getResultLabel(word.result) }}
                    </el-tag>
                  </div>
                  <div class="word-meaning">{{ word.meaning }}</div>
                  <div class="word-meta">
                    <span>熟练度: {{ word.proficiency }}/5</span>
                    <span v-if="word.reviewedAt">
                      {{ formatTime(word.reviewedAt) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <div v-if="dayDetail.pendingWords.length > 0" class="word-section">
              <h4 class="section-title pending-title">
                <el-icon><Clock /></el-icon>
                {{ isSelectedDateFuture ? '预测复习' : '待复习单词' }}
              </h4>
              <div class="word-list">
                <div
                  v-for="word in dayDetail.pendingWords"
                  :key="'p-' + word.wordId"
                  class="word-item pending"
                >
                  <div class="word-main">
                    <span class="word">{{ word.word }}</span>
                    <span v-if="word.phonetic" class="phonetic">/{{ word.phonetic }}/</span>
                    <el-tag size="small" :type="word.status === 'PREDICTED' ? 'info' : 'warning'">
                      {{ word.status === 'PREDICTED' ? '预测' : '待复习' }}
                    </el-tag>
                  </div>
                  <div class="word-meaning">{{ word.meaning }}</div>
                  <div class="word-meta">
                    <span>熟练度: {{ word.proficiency }}/5</span>
                    <span v-if="word.nextReviewAt">
                      预计: {{ formatTime(word.nextReviewAt) }}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            <EmptyState
              v-if="dayDetail.reviewedWords.length === 0 && dayDetail.pendingWords.length === 0"
              title="暂无数据"
              description="这一天没有复习记录也没有待复习的单词。"
              icon="Calendar"
            />
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowLeft, ArrowRight, Back, Close, CircleCheck, Clock
} from '@element-plus/icons-vue'
import { reviewApi } from '@/api/study'
import type { CalendarDayStats, CalendarDayDetail, CalendarMonthResponse } from '@/types'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const router = useRouter()
const loading = ref(false)
const detailLoading = ref(false)
const currentDate = ref(new Date())
const calendarData = ref<CalendarMonthResponse | null>(null)
const selectedDate = ref<string | null>(null)
const dayDetail = ref<CalendarDayDetail | null>(null)

const weekDays = ['日', '一', '二', '三', '四', '五', '六']

interface CalendarCell {
  date: string
  day: number
  isCurrentMonth: boolean
  isToday: boolean
  isPast: boolean
  isFuture: boolean
  stats: CalendarDayStats | null
}

const displayMonth = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth() + 1
  return `${year} 年 ${month} 月`
})

const currentMonthStr = computed(() => {
  const year = currentDate.value.getFullYear()
  const month = String(currentDate.value.getMonth() + 1).padStart(2, '0')
  return `${year}-${month}`
})

const todayStr = computed(() => {
  const t = new Date()
  const year = t.getFullYear()
  const month = String(t.getMonth() + 1).padStart(2, '0')
  const day = String(t.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
})

const isSelectedDateFuture = computed(() => {
  if (!selectedDate.value) return false
  return selectedDate.value > todayStr.value
})

const formatSelectedDate = computed(() => {
  if (!selectedDate.value) return ''
  const [y, m, d] = selectedDate.value.split('-')
  return `${y} 年 ${parseInt(m)} 月 ${parseInt(d)} 日`
})

const calendarCells = computed((): CalendarCell[] => {
  const cells: CalendarCell[] = []
  const year = currentDate.value.getFullYear()
  const month = currentDate.value.getMonth()

  const firstDay = new Date(year, month, 1)
  const startOffset = firstDay.getDay()
  const daysInMonth = new Date(year, month + 1, 0).getDate()
  const daysInPrevMonth = new Date(year, month, 0).getDate()

  const statsMap = new Map<string, CalendarDayStats>()
  if (calendarData.value) {
    for (const day of calendarData.value.days) {
      statsMap.set(day.date, day)
    }
  }

  for (let i = startOffset - 1; i >= 0; i--) {
    const day = daysInPrevMonth - i
    const prevMonth = month === 0 ? 11 : month - 1
    const prevYear = month === 0 ? year - 1 : year
    const dateStr = formatDateStr(prevYear, prevMonth, day)
    cells.push({
      date: dateStr,
      day,
      isCurrentMonth: false,
      isToday: false,
      isPast: true,
      isFuture: false,
      stats: statsMap.get(dateStr) || null
    })
  }

  for (let day = 1; day <= daysInMonth; day++) {
    const dateStr = formatDateStr(year, month, day)
    const cellDate = new Date(year, month, day)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    cellDate.setHours(0, 0, 0, 0)
    cells.push({
      date: dateStr,
      day,
      isCurrentMonth: true,
      isToday: dateStr === todayStr.value,
      isPast: cellDate < today,
      isFuture: cellDate > today,
      stats: statsMap.get(dateStr) || null
    })
  }

  const remaining = 42 - cells.length
  for (let day = 1; day <= remaining; day++) {
    const nextMonth = month === 11 ? 0 : month + 1
    const nextYear = month === 11 ? year + 1 : year
    const dateStr = formatDateStr(nextYear, nextMonth, day)
    cells.push({
      date: dateStr,
      day,
      isCurrentMonth: false,
      isToday: false,
      isPast: false,
      isFuture: true,
      stats: statsMap.get(dateStr) || null
    })
  }

  return cells
})

const formatDateStr = (year: number, month: number, day: number): string => {
  return `${year}-${String(month + 1).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

const fetchCalendarData = async () => {
  loading.value = true
  try {
    calendarData.value = await reviewApi.getCalendarMonth(currentMonthStr.value)
  } catch (error) {
    console.error('Failed to fetch calendar:', error)
    ElMessage.error('获取日历数据失败')
  } finally {
    loading.value = false
  }
}

const fetchDayDetail = async (date: string) => {
  detailLoading.value = true
  try {
    dayDetail.value = await reviewApi.getCalendarDayDetail(date)
  } catch (error) {
    console.error('Failed to fetch day detail:', error)
    ElMessage.error('获取日期详情失败')
  } finally {
    detailLoading.value = false
  }
}

const prevMonth = () => {
  currentDate.value = new Date(
    currentDate.value.getFullYear(),
    currentDate.value.getMonth() - 1,
    1
  )
}

const nextMonth = () => {
  currentDate.value = new Date(
    currentDate.value.getFullYear(),
    currentDate.value.getMonth() + 1,
    1
  )
}

const goToToday = () => {
  currentDate.value = new Date()
}

const handleDateClick = (cell: CalendarCell) => {
  selectedDate.value = cell.date
  fetchDayDetail(cell.date)
}

const getCellClasses = (cell: CalendarCell) => {
  return {
    'is-current-month': cell.isCurrentMonth,
    'is-other-month': !cell.isCurrentMonth,
    'is-today': cell.isToday,
    'is-past': cell.isPast && !cell.isToday,
    'is-future': cell.isFuture,
    'is-selected': selectedDate.value === cell.date,
    'has-data': cell.stats && cell.stats.totalCount > 0
  }
}

const getCountClass = (count: number) => {
  if (count === 0) return 'count-gray'
  if (count <= 3) return 'count-green'
  if (count <= 7) return 'count-yellow'
  return 'count-red'
}

const getResultTagType = (result?: string) => {
  switch (result) {
    case 'KNOWN': return 'success'
    case 'VAGUE': return 'warning'
    case 'UNKNOWN': return 'danger'
    default: return 'info'
  }
}

const getResultLabel = (result?: string) => {
  switch (result) {
    case 'KNOWN': return '认识'
    case 'VAGUE': return '模糊'
    case 'UNKNOWN': return '不认识'
    default: return ''
  }
}

const formatTime = (isoStr: string) => {
  const d = new Date(isoStr)
  const h = String(d.getHours()).padStart(2, '0')
  const m = String(d.getMinutes()).padStart(2, '0')
  return `${h}:${m}`
}

watch(currentMonthStr, () => {
  fetchCalendarData()
})

onMounted(() => {
  fetchCalendarData()
})
</script>

<style scoped>
.calendar-content {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: var(--space-xl);
  align-items: start;
}

.calendar-wrapper {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-xl);
  box-shadow: var(--shadow-sm);
}

.calendar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-lg);
  flex-wrap: wrap;
  gap: var(--space-md);
}

.month-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--c-text-primary);
  margin: 0;
}

.legend {
  display: flex;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
}

.legend-color {
  width: 14px;
  height: 14px;
  border-radius: 3px;
}

.color-gray { background: #e5e7eb; }
.color-green { background: #86efac; }
.color-yellow { background: #fde047; }
.color-red { background: #fca5a5; }

.weekday-row {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  margin-bottom: var(--space-xs);
}

.weekday-cell {
  text-align: center;
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--c-text-secondary);
  padding: var(--space-sm) 0;
}

.calendar-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 4px;
}

.day-cell {
  aspect-ratio: 1;
  min-height: 80px;
  border-radius: var(--radius-md);
  padding: var(--space-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: 2px solid transparent;
  display: flex;
  flex-direction: column;
  position: relative;
  background: var(--c-bg-body);
}

.day-cell:hover {
  background: var(--c-primary-bg);
  transform: translateY(-1px);
}

.day-cell.is-other-month {
  opacity: 0.35;
}

.day-cell.is-today {
  border-color: var(--c-primary);
  background: var(--c-primary-bg);
}

.day-cell.is-today .day-number {
  color: var(--c-primary);
  font-weight: 700;
}

.day-cell.is-selected {
  border-color: var(--c-primary-dark);
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.2);
}

.day-cell.is-past:not(.has-data) {
  opacity: 0.6;
}

.day-number {
  font-size: var(--font-size-sm);
  font-weight: 500;
  color: var(--c-text-primary);
  margin-bottom: var(--space-xs);
}

.day-count {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
  margin-bottom: var(--space-xs);
}

.day-count.count-gray { color: #9ca3af; }
.day-count.count-green { color: #16a34a; }
.day-count.count-yellow { color: #ca8a04; }
.day-count.count-red { color: #dc2626; }

.day-detail {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  font-size: 10px;
  color: var(--c-text-tertiary);
  margin-top: auto;
}

.detail-item {
  white-space: nowrap;
}

.detail-item.reviewed { color: var(--c-success); }
.detail-item.pending { color: var(--c-warning); }
.detail-item.predicted { color: var(--c-info); }

.day-detail-panel {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-md);
  position: sticky;
  top: 0;
  max-height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: var(--space-lg);
  border-bottom: 1px solid var(--c-border-light);
}

.detail-title h3 {
  margin: 0 0 var(--space-xs);
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
}

.detail-summary {
  display: flex;
  gap: var(--space-sm);
}

.detail-body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-lg);
}

.word-section {
  margin-bottom: var(--space-xl);
}

.word-section:last-child {
  margin-bottom: 0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-base);
  font-weight: 600;
  margin: 0 0 var(--space-md);
  color: var(--c-text-primary);
}

.reviewed-title { color: var(--c-success); }
.pending-title { color: var(--c-warning); }

.word-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.word-item {
  padding: var(--space-md);
  border-radius: var(--radius-md);
  background: var(--c-bg-body);
  border-left: 3px solid transparent;
}

.word-item.reviewed {
  border-left-color: var(--c-success);
}

.word-item.pending {
  border-left-color: var(--c-warning);
}

.word-main {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-xs);
  flex-wrap: wrap;
}

.word-main .word {
  font-weight: 600;
  font-size: var(--font-size-base);
  color: var(--c-text-primary);
}

.word-main .phonetic {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  font-family: serif;
}

.result-tag {
  margin-left: auto;
}

.word-meaning {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  margin-bottom: var(--space-xs);
}

.word-meta {
  display: flex;
  justify-content: space-between;
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
}

.slide-fade-enter-active,
.slide-fade-leave-active {
  transition: all 0.25s ease;
}

.slide-fade-enter-from,
.slide-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

@media (max-width: 1024px) {
  .calendar-content {
    grid-template-columns: 1fr;
  }

  .day-detail-panel {
    position: static;
    max-height: none;
  }
}

@media (max-width: 640px) {
  .calendar-wrapper {
    padding: var(--space-md);
  }

  .day-cell {
    min-height: 60px;
    padding: var(--space-xs);
  }

  .day-count {
    font-size: 14px;
  }

  .day-detail {
    display: none;
  }

  .calendar-header {
    flex-direction: column;
    align-items: stretch;
  }

  .month-title {
    text-align: center;
  }

  .legend {
    justify-content: center;
  }
}
</style>
