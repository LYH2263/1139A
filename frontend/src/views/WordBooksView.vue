<template>
  <div class="wordbooks-page page-container">
    <PageHeader
      title="词书中心"
      subtitle="选择适合你的词书，开启系统化学习之旅。"
    >
      <template #actions>
        <el-button type="primary" @click="activeTab = activeTab === 'all' ? 'my' : 'all'">
          <el-icon class="mr-1">
            <component :is="activeTab === 'all' ? 'Collection' : 'Reading'" />
          </el-icon>
          {{ activeTab === 'all' ? '查看我的词书' : '浏览全部词书' }}
        </el-button>
      </template>
    </PageHeader>

    <div class="wordbooks-content" v-loading="loading">
      <div v-if="activeTab === 'all'">
        <div class="filter-bar">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索词书名称或描述..."
            style="width: 300px"
            clearable
            @clear="fetchWordBooks"
            @keyup.enter="fetchWordBooks"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-select
            v-model="difficultyFilter"
            placeholder="难度等级"
            style="width: 150px"
            clearable
            @change="fetchWordBooks"
            @clear="fetchWordBooks"
          >
            <el-option label="初级" value="BEGINNER" />
            <el-option label="中级" value="INTERMEDIATE" />
            <el-option label="高级" value="ADVANCED" />
          </el-select>
        </div>

        <div v-if="wordBooks.length === 0" class="empty-state">
          <EmptyState description="暂无词书数据" />
        </div>

        <div v-else class="wordbook-grid">
          <div
            v-for="wordbook in wordBooks"
            :key="wordbook.id"
            class="wordbook-card"
          >
            <div class="wordbook-cover">
              <img
                v-if="wordbook.coverImage"
                :src="wordbook.coverImage"
                :alt="wordbook.name"
              />
              <div v-else class="cover-placeholder">
                <el-icon><Notebook /></el-icon>
              </div>
              <el-tag
                :type="getDifficultyTagType(wordbook.difficultyLevel)"
                class="difficulty-tag"
                size="small"
              >
                {{ formatDifficulty(wordbook.difficultyLevel) }}
              </el-tag>
            </div>
            <div class="wordbook-body">
              <h3 class="wordbook-title">{{ wordbook.name }}</h3>
              <p class="wordbook-desc">{{ wordbook.description || '暂无描述' }}</p>
              <div class="wordbook-meta">
                <span class="meta-item">
                  <el-icon><Document /></el-icon>
                  {{ wordbook.wordCount }} 词
                </span>
              </div>
              <div class="wordbook-actions">
                <el-button
                  v-if="!isEnrolled(wordbook.id)"
                  type="primary"
                  size="small"
                  @click="handleEnroll(wordbook)"
                >
                  <el-icon class="mr-1"><Plus /></el-icon>
                  加入学习
                </el-button>
                <el-button
                  v-else
                  type="success"
                  size="small"
                  disabled
                >
                  <el-icon class="mr-1"><Check /></el-icon>
                  已加入
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <div class="pagination-wrapper">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="pagination.total"
            :page-sizes="[12, 24, 48]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
      </div>

      <div v-else>
        <div v-if="myWordBooks.length === 0" class="empty-state">
          <EmptyState description="你还没有加入任何词书，快去浏览全部词书选择感兴趣的加入吧！">
            <el-button type="primary" @click="activeTab = 'all'">
              <el-icon class="mr-1"><Search /></el-icon>
              去浏览词书
            </el-button>
          </EmptyState>
        </div>

        <div v-else class="wordbook-grid">
          <div
            v-for="wordbook in myWordBooks"
            :key="wordbook.id"
            class="wordbook-card enrolled"
          >
            <div class="wordbook-cover">
              <img
                v-if="wordbook.coverImage"
                :src="wordbook.coverImage"
                :alt="wordbook.name"
              />
              <div v-else class="cover-placeholder">
                <el-icon><Notebook /></el-icon>
              </div>
              <el-tag
                :type="getDifficultyTagType(wordbook.difficultyLevel)"
                class="difficulty-tag"
                size="small"
              >
                {{ formatDifficulty(wordbook.difficultyLevel) }}
              </el-tag>
            </div>
            <div class="wordbook-body">
              <h3 class="wordbook-title">{{ wordbook.name }}</h3>
              <p class="wordbook-desc">{{ wordbook.description || '暂无描述' }}</p>
              <div class="wordbook-meta">
                <span class="meta-item">
                  <el-icon><Document /></el-icon>
                  {{ wordbook.masteredCount }}/{{ wordbook.wordCount }} 词
                </span>
              </div>
              <div class="progress-section">
                <div class="progress-header">
                  <span class="progress-label">学习进度</span>
                  <span class="progress-value">{{ wordbook.progress.toFixed(1) }}%</span>
                </div>
                <el-progress
                  :percentage="wordbook.progress"
                  :stroke-width="8"
                  :show-text="false"
                  color="var(--c-primary)"
                />
              </div>
              <div class="wordbook-actions">
                <el-button
                  type="primary"
                  size="small"
                  @click="$router.push('/review')"
                >
                  <el-icon class="mr-1"><Calendar /></el-icon>
                  开始学习
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  text
                  @click="handleUnenroll(wordbook)"
                >
                  退出学习
                </el-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Search,
  Plus,
  Check,
  Document,
  Notebook,
  Calendar,
  Collection,
  Reading
} from '@element-plus/icons-vue'
import type { WordBook, UserWordBook } from '@/types'
import { wordBookApi } from '@/api/wordbook'
import PageHeader from '@/components/ui/PageHeader.vue'
import EmptyState from '@/components/ui/EmptyState.vue'

const loading = ref(false)
const activeTab = ref<'all' | 'my'>('all')
const searchKeyword = ref('')
const difficultyFilter = ref('')
const wordBooks = ref<WordBook[]>([])
const myWordBooks = ref<UserWordBook[]>([])

const pagination = reactive({
  page: 1,
  size: 12,
  total: 0
})

const enrolledIds = computed(() => new Set(myWordBooks.value.map(w => w.id)))

const isEnrolled = (id: number) => enrolledIds.value.has(id)

const fetchWordBooks = async () => {
  loading.value = true
  try {
    const res = await wordBookApi.getWordBooks({
      keyword: searchKeyword.value || undefined,
      difficulty: difficultyFilter.value || undefined,
      page: pagination.page,
      size: pagination.size
    })
    wordBooks.value = res.list
    pagination.total = res.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const fetchMyWordBooks = async () => {
  try {
    myWordBooks.value = await wordBookApi.getMyWordBooks()
  } catch (error) {
    console.error(error)
  }
}

const handleEnroll = async (wordbook: WordBook) => {
  try {
    await ElMessageBox.confirm(
      `确定要将词书 "${wordbook.name}" 加入学习计划吗？`,
      '加入学习',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }
    )
    await wordBookApi.enrollWordBook(wordbook.id)
    ElMessage.success(`已加入词书：${wordbook.name}`)
    await fetchMyWordBooks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handleUnenroll = async (wordbook: UserWordBook) => {
  try {
    await ElMessageBox.confirm(
      `确定要退出词书 "${wordbook.name}" 的学习吗？学习进度将会保留。`,
      '退出学习',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    await wordBookApi.unenrollWordBook(wordbook.id)
    ElMessage.success(`已退出词书：${wordbook.name}`)
    await fetchMyWordBooks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

const handlePageChange = (page: number) => {
  pagination.page = page
  fetchWordBooks()
}

const handleSizeChange = (size: number) => {
  pagination.size = size
  pagination.page = 1
  fetchWordBooks()
}

const formatDifficulty = (level: string) => {
  const map: Record<string, string> = {
    BEGINNER: '初级',
    INTERMEDIATE: '中级',
    ADVANCED: '高级'
  }
  return map[level] || level
}

const getDifficultyTagType = (level: string) => {
  const map: Record<string, 'success' | 'warning' | 'danger' | 'info'> = {
    BEGINNER: 'success',
    INTERMEDIATE: 'warning',
    ADVANCED: 'danger'
  }
  return map[level] || 'info'
}

onMounted(() => {
  fetchWordBooks()
  fetchMyWordBooks()
})
</script>

<style scoped>
.filter-bar {
  display: flex;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.wordbook-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: var(--space-lg);
}

.wordbook-card {
  background: var(--c-bg-card);
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition: all 0.3s ease;
}

.wordbook-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.wordbook-card.enrolled {
  border-color: var(--c-primary-light);
}

.wordbook-cover {
  position: relative;
  height: 160px;
  background: linear-gradient(135deg, var(--c-primary-light), var(--c-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.wordbook-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  font-size: 64px;
  color: rgba(255, 255, 255, 0.5);
}

.difficulty-tag {
  position: absolute;
  top: 12px;
  right: 12px;
}

.wordbook-body {
  padding: var(--space-md);
}

.wordbook-title {
  font-size: var(--font-size-md);
  font-weight: 600;
  color: var(--c-text-primary);
  margin: 0 0 var(--space-xs) 0;
}

.wordbook-desc {
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
  line-height: 1.5;
  margin: 0 0 var(--space-md) 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.wordbook-meta {
  display: flex;
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-sm);
  color: var(--c-text-secondary);
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

.wordbook-actions {
  display: flex;
  gap: var(--space-sm);
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: var(--space-xl);
}

.empty-state {
  padding: var(--space-xxl) 0;
}

.mr-1 {
  margin-right: 4px;
}

@media (max-width: 768px) {
  .wordbook-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-direction: column;
  }

  .filter-bar .el-input,
  .filter-bar .el-select {
    width: 100% !important;
  }
}
</style>
