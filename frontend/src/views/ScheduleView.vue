<template>
  <div class="schedule-page page-container">
    <PageHeader title="学习计划" subtitle="系统化管理你的单词学习，每天进步一点点。">
      <template #actions>
        <el-button type="primary" @click="openCreateDialog">
          <el-icon class="mr-1"><Plus /></el-icon>
          创建新计划
        </el-button>
      </template>
    </PageHeader>

    <div class="schedule-content">
      <el-tabs v-model="activeTab" class="schedule-tabs">
        <el-tab-pane label="今日任务" name="today">
          <div v-loading="todayLoading" class="tab-content">
            <div v-if="todaySchedules.length === 0">
              <EmptyState description="今天没有待完成的学习任务，去创建一个新计划吧！">
                <template #action>
                  <el-button type="primary" @click="openCreateDialog">
                    <el-icon class="mr-1"><Plus /></el-icon>
                    创建新计划
                  </el-button>
                </template>
              </EmptyState>
            </div>

            <div v-else class="today-schedule-list">
              <div
                v-for="schedule in todaySchedules"
                :key="schedule.scheduleId"
                class="today-schedule-card"
              >
                <SectionCard>
                  <template #header>
                    <div class="schedule-card-header">
                      <div class="schedule-info">
                        <h4 class="schedule-name">{{ schedule.scheduleName }}</h4>
                        <span class="schedule-date">
                          <el-icon><Calendar /></el-icon>
                          {{ formatDate(schedule.date) }}
                        </span>
                      </div>
                      <el-tag
                        v-if="schedule.completed"
                        type="success"
                        size="small"
                      >
                        已完成
                      </el-tag>
                    </div>
                  </template>

                  <div class="word-list-section">
                    <div class="word-list-header">
                      <span class="word-count">
                        共 {{ schedule.plannedWords.length }} 个单词
                        <span class="new-word-count">
                          (新词 {{ getNewWords(schedule).length }} 个)
                        </span>
                        <span class="review-word-count">
                          (复习 {{ getReviewWords(schedule).length }} 个)
                        </span>
                      </span>
                    </div>

                    <div class="word-checklist">
                      <div
                        v-for="word in schedule.plannedWords"
                        :key="word.id"
                        class="word-item"
                        :class="{ 'is-completed': isWordCompleted(schedule, word.id) }"
                      >
                        <el-checkbox
                          :model-value="isWordCompleted(schedule, word.id)"
                          @change="(val: boolean) => toggleWord(schedule, word.id, val)"
                          :disabled="schedule.completed"
                        >
                          <div class="word-content">
                            <span class="word-text">{{ word.word }}</span>
                            <span class="word-meaning">{{ word.meaning }}</span>
                            <el-tag
                              v-if="word.isReview"
                              type="warning"
                              size="small"
                              effect="light"
                            >
                              第{{ word.reviewDay }}天复习
                            </el-tag>
                            <el-tag
                              v-else
                              type="primary"
                              size="small"
                              effect="light"
                            >
                              新词
                            </el-tag>
                          </div>
                        </el-checkbox>
                      </div>
                    </div>
                  </div>

                  <div class="card-actions" v-if="!schedule.completed">
                    <el-button
                      type="primary"
                      :disabled="getSelectedWords(schedule).length === 0"
                      @click="submitTodayComplete(schedule)"
                    >
                      <el-icon class="mr-1"><Check /></el-icon>
                      标记完成 ({{ getSelectedWords(schedule).length }})
                    </el-button>
                  </div>
                </SectionCard>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="计划列表" name="list">
          <div v-loading="listLoading" class="tab-content">
            <div v-if="activeSchedules.length === 0">
              <EmptyState description="暂无活跃的学习计划，创建一个开始你的学习之旅！">
                <template #action>
                  <el-button type="primary" @click="openCreateDialog">
                    <el-icon class="mr-1"><Plus /></el-icon>
                    创建新计划
                  </el-button>
                </template>
              </EmptyState>
            </div>

            <div v-else class="schedule-grid">
              <div
                v-for="schedule in activeSchedules"
                :key="schedule.id"
                class="schedule-card"
                @click="openScheduleDetail(schedule)"
              >
                <div class="schedule-card-top">
                  <h4 class="schedule-title">{{ schedule.name }}</h4>
                  <el-tag
                    :type="schedule.status === 'PAUSED' ? 'info' : 'success'"
                    size="small"
                  >
                    {{ schedule.status === 'PAUSED' ? '已暂停' : '进行中' }}
                  </el-tag>
                </div>

                <div class="schedule-stats">
                  <div class="stat-item">
                    <span class="stat-label">总单词</span>
                    <span class="stat-value">{{ schedule.totalWords }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">每日学习</span>
                    <span class="stat-value">{{ schedule.dailyCount }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">已完成</span>
                    <span class="stat-value">{{ schedule.completedDays }}/{{ schedule.totalDays }}天</span>
                  </div>
                </div>

                <div class="schedule-dates">
                  <span class="date-item">
                    <el-icon><Calendar /></el-icon>
                    {{ formatShortDate(schedule.startDate) }}
                  </span>
                  <span class="date-separator">→</span>
                  <span class="date-item">
                    <el-icon><Calendar /></el-icon>
                    {{ formatShortDate(schedule.endDate) }}
                  </span>
                </div>

                <div class="progress-section">
                  <div class="progress-header">
                    <span class="progress-label">学习进度</span>
                    <span class="progress-value">{{ schedule.progress.toFixed(1) }}%</span>
                  </div>
                  <el-progress
                    :percentage="schedule.progress"
                    :stroke-width="6"
                    :show-text="false"
                    color="var(--c-primary)"
                  />
                </div>

                <div class="schedule-card-actions" @click.stop>
                  <el-button
                    size="small"
                    type="primary"
                    text
                    @click="openScheduleDetail(schedule)"
                  >
                    <el-icon class="mr-1"><View /></el-icon>
                    详情
                  </el-button>
                  <el-button
                    size="small"
                    :type="schedule.status === 'PAUSED' ? 'success' : 'warning'"
                    text
                    @click="toggleSchedulePause(schedule)"
                  >
                    <el-icon class="mr-1">
                      <component :is="schedule.status === 'PAUSED' ? 'VideoPlay' : 'VideoPause'" />
                    </el-icon>
                    {{ schedule.status === 'PAUSED' ? '恢复' : '暂停' }}
                  </el-button>
                  <el-button
                    size="small"
                    type="danger"
                    text
                    @click="handleDeleteSchedule(schedule)"
                  >
                    <el-icon class="mr-1"><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <el-tab-pane label="已完成" name="completed">
          <div v-loading="listLoading" class="tab-content">
            <div v-if="completedSchedules.length === 0">
              <EmptyState description="暂无已完成的学习计划，继续加油！" />
            </div>

            <div v-else class="schedule-grid">
              <div
                v-for="schedule in completedSchedules"
                :key="schedule.id"
                class="schedule-card completed"
                @click="openScheduleDetail(schedule)"
              >
                <div class="schedule-card-top">
                  <h4 class="schedule-title">{{ schedule.name }}</h4>
                  <el-tag type="success" size="small">已完成</el-tag>
                </div>

                <div class="schedule-stats">
                  <div class="stat-item">
                    <span class="stat-label">总单词</span>
                    <span class="stat-value">{{ schedule.totalWords }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">每日学习</span>
                    <span class="stat-value">{{ schedule.dailyCount }}</span>
                  </div>
                  <div class="stat-item">
                    <span class="stat-label">总天数</span>
                    <span class="stat-value">{{ schedule.totalDays }}天</span>
                  </div>
                </div>

                <div class="schedule-dates">
                  <span class="date-item">
                    <el-icon><Calendar /></el-icon>
                    {{ formatShortDate(schedule.startDate) }}
                  </span>
                  <span class="date-separator">→</span>
                  <span class="date-item">
                    <el-icon><Calendar /></el-icon>
                    {{ formatShortDate(schedule.endDate) }}
                  </span>
                </div>

                <div class="progress-section">
                  <div class="progress-header">
                    <span class="progress-label">完成度</span>
                    <span class="progress-value">{{ schedule.progress.toFixed(1) }}%</span>
                  </div>
                  <el-progress
                    :percentage="schedule.progress"
                    :stroke-width="6"
                    :show-text="false"
                    color="var(--c-success)"
                  />
                </div>

                <div class="schedule-card-actions" @click.stop>
                  <el-button
                    size="small"
                    type="primary"
                    text
                    @click="openScheduleDetail(schedule)"
                  >
                    <el-icon class="mr-1"><View /></el-icon>
                    查看详情
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-drawer
      v-model="detailDrawerVisible"
      title="计划详情"
      direction="rtl"
      size="600px"
      :before-close="handleCloseDetail"
    >
      <div v-if="selectedSchedule" class="schedule-detail">
        <div class="detail-section">
          <h4 class="detail-title">{{ selectedSchedule.name }}</h4>
          <div class="detail-basic-info">
            <div class="info-row">
              <span class="info-label">状态</span>
              <el-tag :type="getStatusTagType(selectedSchedule.status)" size="small">
                {{ formatStatus(selectedSchedule.status) }}
              </el-tag>
            </div>
            <div class="info-row">
              <span class="info-label">总单词数</span>
              <span class="info-value">{{ selectedSchedule.totalWords }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">每日学习量</span>
              <span class="info-value">{{ selectedSchedule.dailyCount }} 词/天</span>
            </div>
            <div class="info-row">
              <span class="info-label">开始日期</span>
              <span class="info-value">{{ formatDate(selectedSchedule.startDate) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">结束日期</span>
              <span class="info-value">{{ formatDate(selectedSchedule.endDate) }}</span>
            </div>
            <div class="info-row">
              <span class="info-label">已完成天数</span>
              <span class="info-value">{{ selectedSchedule.completedDays }}/{{ selectedSchedule.totalDays }} 天</span>
            </div>
          </div>
        </div>

        <div class="detail-section">
          <h5 class="section-subtitle">学习进度甘特图</h5>
          <div class="gantt-container" v-if="scheduleDetail">
            <div
              v-for="(bar, index) in scheduleDetail.ganttData"
              :key="index"
              class="gantt-row"
            >
              <div class="gantt-date">
                <span class="date-text">{{ formatShortDate(bar.date) }}</span>
                <span class="day-label">第{{ bar.dayIndex }}天</span>
              </div>
              <div class="gantt-bar-wrapper">
                <div
                  class="gantt-bar"
                  :class="{
                    'is-completed': bar.completed,
                    'is-today': isToday(bar.date)
                  }"
                  :style="{ width: getGanttBarWidth(bar) + '%' }"
                >
                  <div class="bar-content">
                    <span class="bar-count">
                      <el-icon><Plus /></el-icon>
                      {{ bar.newWordCount }}
                    </span>
                    <span class="bar-count">
                      <el-icon><Refresh /></el-icon>
                      {{ bar.reviewWordCount }}
                    </span>
                    <span class="bar-status">
                      {{ bar.completed ? `${bar.completedCount}/${bar.totalCount}` : `${bar.totalCount}词` }}
                    </span>
                  </div>
                </div>
              </div>
              <div class="gantt-status">
                <el-icon
                  v-if="bar.completed"
                  class="status-icon completed"
                >
                  <CircleCheckFilled />
                </el-icon>
                <el-icon
                  v-else-if="isToday(bar.date)"
                  class="status-icon today"
                >
                  <Clock />
                </el-icon>
                <el-icon
                  v-else
                  class="status-icon pending"
                >
                  <CircleCloseFilled />
                </el-icon>
              </div>
            </div>
          </div>
          <el-empty v-else description="暂无甘特图数据" />
        </div>
      </div>
    </el-drawer>

    <el-dialog
      v-model="createDialogVisible"
      title="创建学习计划"
      width="700px"
      :close-on-click-modal="false"
      :before-close="handleCloseCreateDialog"
    >
      <el-steps :active="createStep" finish-status="success" class="create-steps">
        <el-step title="选词" />
        <el-step title="设量" />
        <el-step title="选日期" />
        <el-step title="确认" />
      </el-steps>

      <div class="step-content">
        <div v-if="createStep === 0" class="step-1">
          <div class="step-description">
            选择要学习的单词，可以从词书选择或手动搜索添加。
          </div>

          <div class="word-source-tabs">
            <el-radio-group v-model="wordSource" size="small">
              <el-radio-button value="wordbook">从词书选择</el-radio-button>
              <el-radio-button value="search">手动搜索</el-radio-button>
            </el-radio-group>
          </div>

          <div v-if="wordSource === 'wordbook'" class="wordbook-selection">
            <el-select
              v-model="selectedWordBookId"
              placeholder="选择词书"
              style="width: 100%"
              @change="handleWordBookChange"
            >
              <el-option
                v-for="wb in wordBooks"
                :key="wb.id"
                :label="`${wb.name} (${wb.wordCount}词)`"
                :value="wb.id"
              />
            </el-select>

            <div v-if="wordBookWords.length > 0" class="filter-bar">
              <el-input
                v-model="wordBookFilterKeyword"
                placeholder="搜索单词或释义..."
                clearable
                class="filter-input"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
              <el-select
                v-model="wordBookFilterPos"
                placeholder="词性筛选"
                clearable
                class="filter-pos"
              >
                <el-option
                  v-for="opt in posOptions"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
            </div>

            <div v-if="wordBookWords.length > 0" class="wordbook-actions">
              <el-checkbox
                v-model="selectAllWordBook"
                @change="handleSelectAllWordBook"
              >
                全选词书 ({{ wordBookWords.length }} 词)
              </el-checkbox>
              <el-checkbox
                v-if="wordBookFilterKeyword || wordBookFilterPos"
                v-model="selectAllFilteredWordBook"
              >
                全选筛选结果 ({{ filteredWordBookWords.length }} 词)
              </el-checkbox>
              <span class="selected-count">
                已选 {{ getWordBookSelectedCount() }} 词
              </span>
            </div>

            <div class="word-list-scroll">
              <el-empty
                v-if="filteredWordBookWords.length === 0 && wordBookWords.length > 0"
                description="没有匹配的单词"
                :image-size="80"
              />
              <el-checkbox-group v-else v-model="selectedWordIds">
                <div
                  v-for="word in filteredWordBookWords"
                  :key="word.id"
                  class="word-select-item"
                >
                  <el-checkbox :value="word.id">
                    <span class="word-name">{{ word.word }}</span>
                    <el-tag v-if="word.pos" size="small" type="info" class="word-pos-tag">{{ word.pos }}</el-tag>
                    <span class="word-mean">{{ word.meaning }}</span>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>
          </div>

          <div v-else class="word-search-section">
            <el-input
              v-model="searchKeyword"
              placeholder="搜索单词..."
              clearable
              @keyup.enter="searchWords"
              @clear="searchWords"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
              <template #append>
                <el-button @click="searchWords">搜索</el-button>
              </template>
            </el-input>

            <div v-if="searchedWords.length > 0" class="word-list-scroll">
              <el-checkbox-group v-model="selectedWordIds">
                <div
                  v-for="word in searchedWords"
                  :key="word.id"
                  class="word-select-item"
                >
                  <el-checkbox :value="word.id">
                    <span class="word-name">{{ word.word }}</span>
                    <span class="word-mean">{{ word.meaning }}</span>
                  </el-checkbox>
                </div>
              </el-checkbox-group>
            </div>

            <el-empty v-else description="搜索单词添加到计划" />
          </div>

          <div class="selected-summary">
            <el-tag type="primary">已选择 {{ selectedWordIds.length }} 个单词</el-tag>
          </div>
        </div>

        <div v-if="createStep === 1" class="step-2">
          <div class="step-description">
            设置计划的基本信息和每日学习量。
          </div>

          <el-form :model="createForm" label-width="100px" class="create-form">
            <el-form-item label="计划名称" required>
              <el-input
                v-model="createForm.name"
                placeholder="请输入计划名称"
                maxlength="50"
                show-word-limit
              />
            </el-form-item>

            <el-form-item label="每日学习量" required>
              <el-input-number
                v-model="createForm.dailyCount"
                :min="5"
                :max="200"
                :step="5"
                placeholder="每天学习的单词数量"
              />
              <span class="form-tip">
                共 {{ selectedWordIds.length }} 个单词，预计需要
                {{ Math.ceil(selectedWordIds.length / createForm.dailyCount) }} 天完成
              </span>
            </el-form-item>

            <el-form-item label="单词总数">
              <span class="info-value">{{ selectedWordIds.length }} 个单词</span>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="createStep === 2" class="step-3">
          <div class="step-description">
            选择计划的开始日期，系统将自动计算结束日期。
          </div>

          <el-form label-width="100px" class="create-form">
            <el-form-item label="开始日期" required>
              <el-date-picker
                v-model="createForm.startDate"
                type="date"
                placeholder="选择开始日期"
                :disabled-date="disabledStartDate"
                format="YYYY-MM-DD"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </el-form-item>

            <el-form-item label="结束日期">
              <span class="info-value">{{ calculateEndDate() }}</span>
              <span class="form-tip">
                共 {{ calculateTotalDays() }} 天
              </span>
            </el-form-item>
          </el-form>
        </div>

        <div v-if="createStep === 3" class="step-4">
          <div class="step-description">
            请确认以下计划信息，确认无误后点击创建。
          </div>

          <div class="summary-card">
            <div class="summary-item">
              <span class="summary-label">计划名称</span>
              <span class="summary-value">{{ createForm.name || '未设置' }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">单词数量</span>
              <span class="summary-value">{{ selectedWordIds.length }} 个</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">每日学习量</span>
              <span class="summary-value">{{ createForm.dailyCount }} 词/天</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">开始日期</span>
              <span class="summary-value">{{ createForm.startDate }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">结束日期</span>
              <span class="summary-value">{{ calculateEndDate() }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">总天数</span>
              <span class="summary-value">{{ calculateTotalDays() }} 天</span>
            </div>
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handlePrevStep" v-if="createStep > 0">
            上一步
          </el-button>
          <el-button
            type="primary"
            v-if="createStep < 3"
            :disabled="!canProceed()"
            @click="handleNextStep"
          >
            下一步
          </el-button>
          <el-button
            type="primary"
            v-else
            :disabled="submitting"
            :loading="submitting"
            @click="handleCreateSchedule"
          >
            创建计划
          </el-button>
          <el-button @click="handleCloseCreateDialog">取消</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Calendar,
  Check,
  View,
  VideoPlay,
  VideoPause,
  Delete,
  Search,
  Refresh,
  Clock,
  CircleCheckFilled,
  CircleCloseFilled
} from '@element-plus/icons-vue'
import type {
  StudySchedule,
  StudyScheduleDetail,
  TodayScheduleResponse,
  Word,
  WordBook,
  CreateScheduleRequest
} from '@/types'
import { scheduleApi } from '@/api/study'
import { wordApi } from '@/api/word'
import { wordBookApi } from '@/api/wordbook'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import SectionCard from '@/components/ui/SectionCard.vue'

const activeTab = ref<'today' | 'list' | 'completed'>('today')
const todayLoading = ref(false)
const listLoading = ref(false)
const todaySchedules = ref<TodayScheduleResponse[]>([])
const activeSchedules = ref<StudySchedule[]>([])
const completedSchedules = ref<StudySchedule[]>([])
const selectedWordMap = reactive<Record<number, number[]>>({})

const detailDrawerVisible = ref(false)
const selectedSchedule = ref<StudySchedule | null>(null)
const scheduleDetail = ref<StudyScheduleDetail | null>(null)
const detailLoading = ref(false)

const createDialogVisible = ref(false)
const createStep = ref(0)
const wordSource = ref<'wordbook' | 'search'>('wordbook')
const selectedWordBookId = ref<number | null>(null)
const selectAllWordBook = ref(false)
const searchKeyword = ref('')
const wordBookFilterKeyword = ref('')
const wordBookFilterPos = ref('')
const wordBooks = ref<WordBook[]>([])
const wordBookWords = ref<Word[]>([])
const searchedWords = ref<Word[]>([])
const selectedWordIds = ref<number[]>([])
const submitting = ref(false)

const posOptions = [
  { label: '全部词性', value: '' },
  { label: '名词 n.', value: 'n' },
  { label: '动词 v.', value: 'v' },
  { label: '形容词 adj.', value: 'adj' },
  { label: '副词 adv.', value: 'adv' },
  { label: '介词 prep.', value: 'prep' },
  { label: '连词 conj.', value: 'conj' },
  { label: '代词 pron.', value: 'pron' },
  { label: '冠词 art.', value: 'art' },
  { label: '数词 num.', value: 'num' },
  { label: '感叹词 int.', value: 'int' }
]

const filteredWordBookWords = computed(() => {
  let result = wordBookWords.value
  if (wordBookFilterKeyword.value.trim()) {
    const kw = wordBookFilterKeyword.value.trim().toLowerCase()
    result = result.filter(w =>
      w.word.toLowerCase().includes(kw) ||
      w.meaning.toLowerCase().includes(kw)
    )
  }
  if (wordBookFilterPos.value) {
    result = result.filter(w => w.pos === wordBookFilterPos.value)
  }
  return result
})

const filteredSearchedWords = computed(() => {
  return searchedWords.value
})

const selectAllFilteredWordBook = ref(false)

watch(selectAllFilteredWordBook, (val: boolean) => {
  const filteredIds = filteredWordBookWords.value.map(w => w.id)
  if (val) {
    const existing = new Set(selectedWordIds.value)
    filteredIds.forEach(id => existing.add(id))
    selectedWordIds.value = Array.from(existing)
  } else {
    const filteredSet = new Set(filteredIds)
    selectedWordIds.value = selectedWordIds.value.filter(id => !filteredSet.has(id))
  }
})

watch([filteredWordBookWords, selectedWordIds], () => {
  if (filteredWordBookWords.value.length === 0) {
    selectAllFilteredWordBook.value = false
    return
  }
  const filteredIds = filteredWordBookWords.value.map(w => w.id)
  selectAllFilteredWordBook.value = filteredIds.every(id => selectedWordIds.value.includes(id))
}, { deep: true })

const createForm = reactive({
  name: '',
  dailyCount: 20,
  startDate: formatDateString(new Date()),
  endDate: ''
})

function formatDateString(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function formatDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
    weekday: 'long'
  })
}

function formatShortDate(dateStr: string): string {
  const date = new Date(dateStr)
  return date.toLocaleDateString('zh-CN', {
    month: 'short',
    day: 'numeric'
  })
}

function formatStatus(status: string): string {
  const map: Record<string, string> = {
    ACTIVE: '进行中',
    COMPLETED: '已完成',
    PAUSED: '已暂停'
  }
  return map[status] || status
}

function getStatusTagType(status: string): 'success' | 'info' | 'warning' | 'danger' {
  const map: Record<string, 'success' | 'info' | 'warning' | 'danger'> = {
    ACTIVE: 'success',
    COMPLETED: 'success',
    PAUSED: 'info'
  }
  return map[status] || 'info'
}

function isToday(dateStr: string): boolean {
  const today = new Date()
  const date = new Date(dateStr)
  return (
    today.getFullYear() === date.getFullYear() &&
    today.getMonth() === date.getMonth() &&
    today.getDate() === date.getDate()
  )
}

function getNewWords(schedule: TodayScheduleResponse) {
  return schedule.plannedWords.filter(w => !w.isReview)
}

function getReviewWords(schedule: TodayScheduleResponse) {
  return schedule.plannedWords.filter(w => w.isReview)
}

function isWordCompleted(schedule: TodayScheduleResponse, wordId: number): boolean {
  if (schedule.completed) return true
  const selected = selectedWordMap[schedule.scheduleId] || []
  return selected.includes(wordId) || schedule.completedWordIds.includes(wordId)
}

function toggleWord(schedule: TodayScheduleResponse, wordId: number, checked: boolean) {
  if (!selectedWordMap[schedule.scheduleId]) {
    selectedWordMap[schedule.scheduleId] = [...schedule.completedWordIds]
  }
  const selected = selectedWordMap[schedule.scheduleId]
  if (checked) {
    if (!selected.includes(wordId)) {
      selected.push(wordId)
    }
  } else {
    const idx = selected.indexOf(wordId)
    if (idx > -1) {
      selected.splice(idx, 1)
    }
  }
}

function getSelectedWords(schedule: TodayScheduleResponse): number[] {
  const selected = selectedWordMap[schedule.scheduleId] || []
  return selected.filter(id => !schedule.completedWordIds.includes(id))
}

async function submitTodayComplete(schedule: TodayScheduleResponse) {
  const selected = getSelectedWords(schedule)
  if (selected.length === 0) {
    ElMessage.warning('请先勾选要完成的单词')
    return
  }
  try {
    await ElMessageBox.confirm(
      `确定要将选中的 ${selected.length} 个单词标记为完成吗？`,
      '确认完成',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    const allCompleted = [...schedule.completedWordIds, ...selected]
    await scheduleApi.completeToday(schedule.scheduleId, {
      completedWordIds: allCompleted
    })
    ElMessage.success('已标记完成')
    await fetchTodaySchedules()
    await fetchSchedules()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

async function fetchTodaySchedules() {
  todayLoading.value = true
  try {
    todaySchedules.value = await scheduleApi.getTodayAllSchedules()
    todaySchedules.value.forEach(s => {
      selectedWordMap[s.scheduleId] = [...s.completedWordIds]
    })
  } catch (error) {
    console.error(error)
  } finally {
    todayLoading.value = false
  }
}

async function fetchSchedules() {
  listLoading.value = true
  try {
    const res = await scheduleApi.getSchedules()
    activeSchedules.value = [...res.activeSchedules, ...res.pausedSchedules]
    completedSchedules.value = res.completedSchedules
  } catch (error) {
    console.error(error)
  } finally {
    listLoading.value = false
  }
}

async function openScheduleDetail(schedule: StudySchedule) {
  selectedSchedule.value = schedule
  detailDrawerVisible.value = true
  detailLoading.value = true
  try {
    scheduleDetail.value = await scheduleApi.getScheduleDetail(schedule.id)
  } catch (error) {
    console.error(error)
  } finally {
    detailLoading.value = false
  }
}

function handleCloseDetail() {
  detailDrawerVisible.value = false
  selectedSchedule.value = null
  scheduleDetail.value = null
}

async function toggleSchedulePause(schedule: StudySchedule) {
  const newStatus = schedule.status === 'PAUSED' ? 'ACTIVE' : 'PAUSED'
  const action = newStatus === 'PAUSED' ? '暂停' : '恢复'
  try {
    await ElMessageBox.confirm(
      `确定要${action}计划 "${schedule.name}" 吗？`,
      `${action}计划`,
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await scheduleApi.updateSchedule(schedule.id, { status: newStatus })
    ElMessage.success(`已${action}计划`)
    await fetchSchedules()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

async function handleDeleteSchedule(schedule: StudySchedule) {
  try {
    await ElMessageBox.confirm(
      `确定要删除计划 "${schedule.name}" 吗？此操作不可恢复。`,
      '删除计划',
      {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'danger'
      }
    )
    await scheduleApi.deleteSchedule(schedule.id)
    ElMessage.success('已删除计划')
    await fetchSchedules()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

function getGanttBarWidth(bar: { totalCount: number; completedCount: number }): number {
  if (bar.totalCount === 0) return 0
  const maxCount = Math.max(...(scheduleDetail.value?.ganttData.map(b => b.totalCount) || [1]))
  return Math.max((bar.totalCount / maxCount) * 100, 15)
}

function openCreateDialog() {
  createDialogVisible.value = true
  createStep.value = 0
  resetCreateForm()
  fetchWordBooks()
}

function resetCreateForm() {
  createStep.value = 0
  createForm.name = ''
  createForm.dailyCount = 20
  createForm.startDate = formatDateString(new Date())
  createForm.endDate = ''
  wordSource.value = 'wordbook'
  selectedWordBookId.value = null
  selectAllWordBook.value = false
  selectAllFilteredWordBook.value = false
  searchKeyword.value = ''
  wordBookFilterKeyword.value = ''
  wordBookFilterPos.value = ''
  wordBookWords.value = []
  searchedWords.value = []
  selectedWordIds.value = []
}

async function fetchWordBooks() {
  try {
    const res = await wordBookApi.getWordBooks({ size: 100 })
    wordBooks.value = res.list
  } catch (error) {
    console.error(error)
  }
}

async function handleWordBookChange(wordBookId: number) {
  wordBookWords.value = []
  selectAllWordBook.value = false
  selectAllFilteredWordBook.value = false
  wordBookFilterKeyword.value = ''
  wordBookFilterPos.value = ''
  try {
    wordBookWords.value = await wordBookApi.getWordBookWords(wordBookId)
  } catch (error) {
    console.error(error)
  }
}

function handleSelectAllWordBook(val: boolean) {
  if (val) {
    const wordBookWordIds = wordBookWords.value.map(w => w.id)
    const otherSelected = selectedWordIds.value.filter(
      id => !wordBookWordIds.includes(id)
    )
    selectedWordIds.value = [...otherSelected, ...wordBookWordIds]
  } else {
    const wordBookWordIds = new Set(wordBookWords.value.map(w => w.id))
    selectedWordIds.value = selectedWordIds.value.filter(
      id => !wordBookWordIds.has(id)
    )
  }
}

function getWordBookSelectedCount(): number {
  const wordBookWordIds = new Set(wordBookWords.value.map(w => w.id))
  return selectedWordIds.value.filter(id => wordBookWordIds.has(id)).length
}

async function searchWords() {
  if (!searchKeyword.value.trim()) {
    searchedWords.value = []
    return
  }
  try {
    const res = await wordApi.getWords({
      keyword: searchKeyword.value,
      size: 50
    })
    searchedWords.value = res.list
  } catch (error) {
    console.error(error)
  }
}

function disabledStartDate(date: Date): boolean {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date < today
}

function calculateEndDate(): string {
  if (!createForm.startDate) return ''
  const start = new Date(createForm.startDate)
  const totalDays = calculateTotalDays()
  start.setDate(start.getDate() + totalDays - 1)
  return formatDateString(start)
}

function calculateTotalDays(): number {
  if (selectedWordIds.value.length === 0 || createForm.dailyCount === 0) return 0
  return Math.ceil(selectedWordIds.value.length / createForm.dailyCount)
}

function canProceed(): boolean {
  switch (createStep.value) {
    case 0:
      return selectedWordIds.value.length > 0
    case 1:
      return createForm.name.trim() !== '' && createForm.dailyCount > 0
    case 2:
      return !!createForm.startDate
    default:
      return true
  }
}

function handlePrevStep() {
  if (createStep.value > 0) {
    createStep.value--
  }
}

function handleNextStep() {
  if (createStep.value < 3) {
    createStep.value++
  }
}

async function handleCreateSchedule() {
  if (!canProceed()) return

  submitting.value = true
  try {
    const endDate = calculateEndDate()
    const request: CreateScheduleRequest = {
      name: createForm.name,
      targetWordIds: selectedWordIds.value,
      dailyCount: createForm.dailyCount,
      startDate: createForm.startDate,
      endDate: endDate
    }
    await scheduleApi.createSchedule(request)
    ElMessage.success('计划创建成功！')
    createDialogVisible.value = false
    await fetchSchedules()
    await fetchTodaySchedules()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

function handleCloseCreateDialog() {
  if (selectedWordIds.value.length > 0 || createForm.name.trim()) {
    ElMessageBox.confirm(
      '确定要关闭吗？已填写的信息将会丢失。',
      '关闭确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
      .then(() => {
        createDialogVisible.value = false
        resetCreateForm()
      })
      .catch(() => {})
  } else {
    createDialogVisible.value = false
    resetCreateForm()
  }
}

watch(activeTab, (val) => {
  if (val === 'today') {
    fetchTodaySchedules()
  } else if (val === 'list' || val === 'completed') {
    fetchSchedules()
  }
})

onMounted(() => {
  fetchTodaySchedules()
  fetchSchedules()
})
</script>

<style scoped>
.schedule-page {
  min-height: 100%;
}

.schedule-content {
  margin-top: var(--space-lg);
}

.schedule-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-lg);
}

.tab-content {
  min-height: 400px;
}

.today-schedule-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.schedule-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.schedule-info {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  flex-wrap: wrap;
}

.schedule-name {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
  margin: 0;
}

.schedule-date {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.word-list-section {
  margin-bottom: var(--space-md);
}

.word-list-header {
  margin-bottom: var(--space-md);
}

.word-count {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.new-word-count {
  color: var(--c-primary);
  margin-left: var(--space-xs);
}

.review-word-count {
  color: var(--c-warning);
  margin-left: var(--space-xs);
}

.word-checklist {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  max-height: 400px;
  overflow-y: auto;
  padding-right: var(--space-sm);
}

.word-item {
  padding: var(--space-sm) var(--space-md);
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.word-item:hover {
  background: var(--c-primary-bg);
}

.word-item.is-completed {
  opacity: 0.6;
}

.word-content {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.word-text {
  font-weight: 600;
  color: var(--c-text-primary);
  min-width: 120px;
}

.word-meaning {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
  flex: 1;
}

.card-actions {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-md);
  border-top: 1px solid var(--c-border-light);
}

.schedule-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--space-lg);
}

.schedule-card {
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  cursor: pointer;
  transition: all var(--transition-normal);
}

.schedule-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--c-primary-light);
}

.schedule-card.completed {
  border-color: var(--c-success);
}

.schedule-card-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: var(--space-md);
}

.schedule-title {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
  margin: 0;
}

.schedule-stats {
  display: flex;
  gap: var(--space-lg);
  margin-bottom: var(--space-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
}

.stat-value {
  font-size: var(--font-size-base);
  font-weight: 600;
  color: var(--c-text-primary);
}

.schedule-dates {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
}

.date-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.date-separator {
  color: var(--c-text-tertiary);
}

.progress-section {
  margin-bottom: var(--space-md);
}

.progress-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: var(--space-xs);
}

.progress-label {
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
}

.progress-value {
  font-size: var(--font-size-sm);
  font-weight: 600;
  color: var(--c-primary);
}

.schedule-card.completed .progress-value {
  color: var(--c-success);
}

.schedule-card-actions {
  display: flex;
  gap: var(--space-sm);
  padding-top: var(--space-md);
  border-top: 1px solid var(--c-border-light);
}

.schedule-detail {
  padding: var(--space-md);
}

.detail-section {
  margin-bottom: var(--space-xl);
}

.detail-title {
  font-size: var(--font-size-xl);
  font-weight: 600;
  color: var(--c-text-primary);
  margin: 0 0 var(--space-md) 0;
}

.section-subtitle {
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
  margin: 0 0 var(--space-md) 0;
}

.detail-basic-info {
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
  padding: var(--space-md);
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--c-border-light);
}

.info-row:last-child {
  border-bottom: none;
}

.info-label {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
}

.info-value {
  color: var(--c-text-primary);
  font-weight: 500;
}

.gantt-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  max-height: 500px;
  overflow-y: auto;
  padding-right: var(--space-sm);
}

.gantt-row {
  display: flex;
  align-items: center;
  gap: var(--space-md);
}

.gantt-date {
  display: flex;
  flex-direction: column;
  min-width: 100px;
}

.date-text {
  font-size: var(--font-size-sm);
  color: var(--c-text-primary);
  font-weight: 500;
}

.day-label {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
}

.gantt-bar-wrapper {
  flex: 1;
  height: 40px;
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
  overflow: hidden;
  position: relative;
}

.gantt-bar {
  height: 100%;
  background: var(--c-border);
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  padding: 0 var(--space-sm);
  transition: all var(--transition-fast);
  min-width: 80px;
  border: 2px solid transparent;
}

.gantt-bar.is-completed {
  background: var(--c-success);
}

.gantt-bar.is-completed .bar-content {
  color: #fff;
}

.gantt-bar.is-today {
  border-color: var(--c-primary);
}

.bar-content {
  display: flex;
  align-items: center;
  gap: var(--space-md);
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
}

.bar-count {
  display: flex;
  align-items: center;
  gap: 2px;
}

.bar-status {
  font-weight: 600;
}

.gantt-status {
  width: 24px;
  display: flex;
  justify-content: center;
}

.status-icon {
  font-size: 20px;
}

.status-icon.completed {
  color: var(--c-success);
}

.status-icon.today {
  color: var(--c-primary);
}

.status-icon.pending {
  color: var(--c-text-tertiary);
}

.create-steps {
  margin-bottom: var(--space-xl);
}

.step-content {
  min-height: 400px;
}

.step-description {
  color: var(--c-text-secondary);
  margin-bottom: var(--space-lg);
  font-size: var(--font-size-sm);
}

.word-source-tabs {
  margin-bottom: var(--space-md);
}

.wordbook-selection,
.word-search-section {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.wordbook-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-count {
  font-size: var(--font-size-sm);
  color: var(--c-primary);
  font-weight: 500;
}

.word-list-scroll {
  max-height: 300px;
  overflow-y: auto;
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-md);
  padding: var(--space-sm);
}

.word-select-item {
  padding: var(--space-sm);
  border-radius: var(--radius-sm);
  transition: background var(--transition-fast);
}

.word-select-item:hover {
  background: var(--c-bg-body);
}

.word-select-item :deep(.el-checkbox__label) {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  width: 100%;
}

.word-name {
  font-weight: 600;
  color: var(--c-text-primary);
  min-width: 100px;
}

.word-mean {
  color: var(--c-text-secondary);
  font-size: var(--font-size-sm);
  flex: 1;
}

.selected-summary {
  margin-top: var(--space-md);
  text-align: right;
}

.create-form {
  max-width: 500px;
}

.form-tip {
  margin-left: var(--space-sm);
  color: var(--c-text-tertiary);
  font-size: var(--font-size-sm);
}

.summary-card {
  background: var(--c-bg-body);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
}

.summary-item {
  display: flex;
  justify-content: space-between;
  padding: var(--space-sm) 0;
  border-bottom: 1px solid var(--c-border-light);
}

.summary-item:last-child {
  border-bottom: none;
}

.summary-label {
  color: var(--c-text-secondary);
}

.summary-value {
  font-weight: 600;
  color: var(--c-text-primary);
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-sm);
}

.mr-1 {
  margin-right: 4px;
}

@media (max-width: 640px) {
  .schedule-grid {
    grid-template-columns: 1fr;
  }

  .schedule-stats {
    gap: var(--space-md);
    flex-wrap: wrap;
  }

  .schedule-card-actions {
    flex-wrap: wrap;
  }

  .gantt-date {
    min-width: 70px;
  }

  .bar-content {
    gap: var(--space-xs);
  }
}

.filter-bar {
  display: flex;
  gap: var(--space-sm);
  margin-top: var(--space-md);
}

.filter-input {
  flex: 1;
}

.filter-pos {
  width: 160px;
  flex-shrink: 0;
}

.word-pos-tag {
  margin: 0 var(--space-sm);
  font-weight: 400;
}

@media (max-width: 640px) {
  .filter-bar {
    flex-direction: column;
  }

  .filter-pos {
    width: 100%;
  }
}
</style>
