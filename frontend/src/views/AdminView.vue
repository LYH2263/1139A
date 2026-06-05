<template>
  <div class="admin-page page-container">
    <PageHeader
      title="词库管理"
      subtitle="管理系统中的单词和词书数据。"
    />

    <div class="admin-content">
      <el-tabs v-model="activeTab" class="admin-tabs">
        <el-tab-pane label="单词管理" name="words">
          <template #label>
            <el-icon><Document /></el-icon>
            <span class="tab-label">单词管理</span>
          </template>

          <div class="tab-actions">
            <el-button type="success" @click="showImportDialog = true">
              <el-icon class="mr-1"><Upload /></el-icon> 批量导入
            </el-button>
            <el-button type="primary" @click="openWordAddDialog">
              <el-icon class="mr-1"><Plus /></el-icon> 新增单词
            </el-button>
          </div>

          <SectionCard :body-style="{ padding: '0' }">
            <el-table
              :data="wordList"
              v-loading="wordLoading"
              stripe
              style="width: 100%"
            >
              <el-table-column prop="id" label="ID" width="80" align="center" />

              <el-table-column prop="word" label="单词" width="160">
                <template #default="{ row }">
                  <span class="word-text">{{ row.word }}</span>
                </template>
              </el-table-column>

              <el-table-column prop="phonetic" label="音标" width="140">
                <template #default="{ row }">
                  <span class="phonetic-text">{{ row.phonetic }}</span>
                </template>
              </el-table-column>

              <el-table-column prop="pos" label="词性" width="100">
                <template #default="{ row }">
                  <el-tag v-if="row.pos" size="small" effect="plain">{{ formatPos(row.pos) }}</el-tag>
                </template>
              </el-table-column>

              <el-table-column prop="meaning" label="释义" min-width="200" show-overflow-tooltip />

              <el-table-column label="操作" width="180" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleWordEdit(row)">
                    <el-icon class="mr-1"><Edit /></el-icon> 编辑
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleWordDelete(row)">
                    <el-icon class="mr-1"><Delete /></el-icon> 删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="wordPagination.page"
                v-model:page-size="wordPagination.size"
                :total="wordPagination.total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                background
                @size-change="handleWordSizeChange"
                @current-change="handleWordPageChange"
              />
            </div>
          </SectionCard>
        </el-tab-pane>

        <el-tab-pane label="词书管理" name="wordbooks">
          <template #label>
            <el-icon><Collection /></el-icon>
            <span class="tab-label">词书管理</span>
          </template>

          <div class="tab-actions">
            <el-button type="primary" @click="openWordBookAddDialog">
              <el-icon class="mr-1"><Plus /></el-icon> 新增词书
            </el-button>
          </div>

          <SectionCard :body-style="{ padding: '0' }">
            <el-table
              :data="wordBookList"
              v-loading="wordBookLoading"
              stripe
              style="width: 100%"
            >
              <el-table-column prop="id" label="ID" width="80" align="center" />

              <el-table-column label="封面" width="100" align="center">
                <template #default="{ row }">
                  <div class="cover-preview">
                    <img v-if="row.coverImage" :src="row.coverImage" :alt="row.name" />
                    <el-icon v-else><Notebook /></el-icon>
                  </div>
                </template>
              </el-table-column>

              <el-table-column prop="name" label="词书名称" width="200">
                <template #default="{ row }">
                  <span class="wordbook-name">{{ row.name }}</span>
                </template>
              </el-table-column>

              <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />

              <el-table-column label="难度" width="100">
                <template #default="{ row }">
                  <el-tag
                    :type="getDifficultyTagType(row.difficultyLevel)"
                    size="small"
                  >
                    {{ formatDifficulty(row.difficultyLevel) }}
                  </el-tag>
                </template>
              </el-table-column>

              <el-table-column prop="wordCount" label="单词数" width="100" align="center" />

              <el-table-column label="操作" width="240" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="handleWordBookEdit(row)">
                    <el-icon class="mr-1"><Edit /></el-icon> 编辑
                  </el-button>
                  <el-button link type="success" size="small" @click="openManageWordsDialog(row)">
                    <el-icon class="mr-1"><SetUp /></el-icon> 管理单词
                  </el-button>
                  <el-button link type="danger" size="small" @click="handleWordBookDelete(row)">
                    <el-icon class="mr-1"><Delete /></el-icon> 删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pagination-wrapper">
              <el-pagination
                v-model:current-page="wordBookPagination.page"
                v-model:page-size="wordBookPagination.size"
                :total="wordBookPagination.total"
                :page-sizes="[10, 20, 50]"
                layout="total, sizes, prev, pager, next"
                background
                @size-change="handleWordBookSizeChange"
                @current-change="handleWordBookPageChange"
              />
            </div>
          </SectionCard>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- Word Edit Dialog -->
    <el-dialog
      v-model="showWordAddDialog"
      :title="isWordEdit ? '编辑单词' : '新增单词'"
      width="600px"
      class="custom-dialog"
      destroy-on-close
    >
      <el-form
        :model="wordForm"
        :rules="wordRules"
        ref="wordFormRef"
        label-position="top"
        class="word-form"
      >
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="单词" prop="word">
              <el-input v-model="wordForm.word" placeholder="Example: apple" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="音标">
              <el-input v-model="wordForm.phonetic" placeholder="Example: [ˈæpl]" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="词性">
          <el-select v-model="wordForm.pos" placeholder="选择词性" style="width: 100%">
            <el-option label="名词 (Noun)" value="noun" />
            <el-option label="动词 (Verb)" value="verb" />
            <el-option label="形容词 (Adjective)" value="adjective" />
            <el-option label="副词 (Adverb)" value="adverb" />
          </el-select>
        </el-form-item>

        <el-form-item label="释义" prop="meaning">
          <el-input
            v-model="wordForm.meaning"
            type="textarea"
            :rows="2"
            placeholder="请输入中文释义"
          />
        </el-form-item>

        <el-form-item label="例句">
          <el-input
            v-model="wordForm.example"
            type="textarea"
            :rows="3"
            placeholder="请输入英文例句"
          />
        </el-form-item>

        <el-form-item label="记忆提示">
          <el-input
            v-model="wordForm.memoryTip"
            type="textarea"
            :rows="2"
            placeholder="助记技巧或联想提示"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showWordAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitWord" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- Import Dialog -->
    <el-dialog v-model="showImportDialog" title="批量导入单词" width="500px" class="custom-dialog">
      <el-upload
        class="upload-area"
        drag
        action=""
        :auto-upload="false"
        :on-change="handleFileChange"
        accept=".csv"
      >
        <el-icon class="el-icon--upload"><upload-filled /></el-icon>
        <div class="el-upload__text">
          拖拽 CSV 文件到此处，或 <em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip">
            支持 UTF-8 编码的 CSV 文件，需包含表头: word, meaning, example (可选)
          </div>
        </template>
      </el-upload>

      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="submitImport" :loading="importing">开始导入</el-button>
      </template>
    </el-dialog>

    <!-- WordBook Edit Dialog -->
    <el-dialog
      v-model="showWordBookAddDialog"
      :title="isWordBookEdit ? '编辑词书' : '新增词书'"
      width="600px"
      class="custom-dialog"
      destroy-on-close
    >
      <el-form
        :model="wordBookForm"
        :rules="wordBookRules"
        ref="wordBookFormRef"
        label-position="top"
        class="wordbook-form"
      >
        <el-form-item label="词书名称" prop="name">
          <el-input v-model="wordBookForm.name" placeholder="请输入词书名称" />
        </el-form-item>

        <el-form-item label="描述" prop="description">
          <el-input
            v-model="wordBookForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入词书描述"
          />
        </el-form-item>

        <el-form-item label="封面图片URL">
          <el-input v-model="wordBookForm.coverImage" placeholder="请输入封面图片URL（可选）" />
        </el-form-item>

        <el-form-item label="难度等级" prop="difficultyLevel">
          <el-select v-model="wordBookForm.difficultyLevel" placeholder="选择难度等级" style="width: 100%">
            <el-option label="初级" value="BEGINNER" />
            <el-option label="中级" value="INTERMEDIATE" />
            <el-option label="高级" value="ADVANCED" />
          </el-select>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showWordBookAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitWordBook" :loading="submittingWordBook">保存</el-button>
      </template>
    </el-dialog>

    <!-- Manage Words Dialog -->
    <el-dialog
      v-model="showManageWordsDialog"
      :title="`管理词书单词 - ${currentWordBook?.name || ''}`"
      width="900px"
      class="custom-dialog"
      destroy-on-close
    >
      <div class="manage-words-content">
        <div class="words-selector">
          <div class="selector-header">
            <h4>可选单词</h4>
            <el-input
              v-model="wordSearchKeyword"
              placeholder="搜索单词..."
              size="small"
              clearable
              style="width: 200px"
            />
          </div>
          <div class="words-list-container">
            <el-table
              :data="availableWords"
              v-loading="wordsLoading"
              size="small"
              height="400"
              @selection-change="handleAvailableSelectionChange"
              ref="availableWordsTable"
            >
              <el-table-column type="selection" width="50" />
              <el-table-column prop="word" label="单词" width="140" />
              <el-table-column prop="meaning" label="释义" show-overflow-tooltip />
            </el-table>
          </div>
          <div class="selector-actions">
            <el-button type="primary" size="small" @click="addSelectedWords" :disabled="selectedAvailableWords.length === 0">
              <el-icon><ArrowRight /></el-icon> 添加
            </el-button>
          </div>
        </div>

        <div class="words-selected">
          <div class="selector-header">
            <h4>已添加单词 ({{ currentWordBookWords.length }})</h4>
          </div>
          <div class="words-list-container">
            <el-table
              :data="currentWordBookWords"
              size="small"
              height="400"
              @selection-change="handleSelectedSelectionChange"
              ref="selectedWordsTable"
            >
              <el-table-column type="selection" width="50" />
              <el-table-column prop="word" label="单词" width="140" />
              <el-table-column prop="meaning" label="释义" show-overflow-tooltip />
            </el-table>
          </div>
          <div class="selector-actions">
            <el-button type="danger" size="small" @click="removeSelectedWords" :disabled="selectedWordsToRemove.length === 0">
              <el-icon><Delete /></el-icon> 移除
            </el-button>
          </div>
        </div>
      </div>

      <template #footer>
        <el-button @click="showManageWordsDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Plus,
  Upload,
  Edit,
  Delete,
  UploadFilled,
  Collection,
  Notebook,
  Document,
  SetUp,
  ArrowRight
} from '@element-plus/icons-vue'
import type { Word, WordBook } from '@/types'
import { wordApi } from '@/api/word'
import { wordBookApi } from '@/api/wordbook'
import PageHeader from '@/components/ui/PageHeader.vue'
import SectionCard from '@/components/ui/SectionCard.vue'

const activeTab = ref('words')

// Word management
const wordLoading = ref(false)
const submitting = ref(false)
const importing = ref(false)
const showWordAddDialog = ref(false)
const showImportDialog = ref(false)
const isWordEdit = ref(false)
const editWordId = ref<number | null>(null)
const wordFormRef = ref()
const wordList = ref<Word[]>([])
const importFile = ref<File | null>(null)

const wordForm = reactive({
  word: '',
  phonetic: '',
  pos: '',
  meaning: '',
  example: '',
  memoryTip: ''
})

const wordRules = {
  word: [
    { required: true, message: '请输入单词', trigger: 'blur' }
  ],
  meaning: [
    { required: true, message: '请输入释义', trigger: 'blur' }
  ]
}

const wordPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// WordBook management
const wordBookLoading = ref(false)
const submittingWordBook = ref(false)
const showWordBookAddDialog = ref(false)
const showManageWordsDialog = ref(false)
const isWordBookEdit = ref(false)
const editWordBookId = ref<number | null>(null)
const wordBookFormRef = ref()
const wordBookList = ref<WordBook[]>([])
const currentWordBook = ref<WordBook | null>(null)
const currentWordBookWords = ref<Word[]>([])
const availableWords = ref<Word[]>([])
const wordsLoading = ref(false)
const wordSearchKeyword = ref('')
const selectedAvailableWords = ref<Word[]>([])
const selectedWordsToRemove = ref<Word[]>([])
const availableWordsTable = ref()
const selectedWordsTable = ref()

const wordBookForm = reactive({
  name: '',
  description: '',
  coverImage: '',
  difficultyLevel: ''
})

const wordBookRules = {
  name: [
    { required: true, message: '请输入词书名称', trigger: 'blur' }
  ],
  difficultyLevel: [
    { required: true, message: '请选择难度等级', trigger: 'change' }
  ]
}

const wordBookPagination = reactive({
  page: 1,
  size: 20,
  total: 0
})

// Fetch functions
const fetchWords = async () => {
  wordLoading.value = true
  try {
    const res = await wordApi.getWords({
      page: wordPagination.page,
      size: wordPagination.size
    })
    wordList.value = res.list
    wordPagination.total = res.total
  } catch (error) {
    console.error(error)
  } finally {
    wordLoading.value = false
  }
}

const fetchWordBooks = async () => {
  wordBookLoading.value = true
  try {
    const res = await wordBookApi.getWordBooks({
      page: wordBookPagination.page,
      size: wordBookPagination.size
    })
    wordBookList.value = res.list
    wordBookPagination.total = res.total
  } catch (error) {
    console.error(error)
  } finally {
    wordBookLoading.value = false
  }
}

const fetchAvailableWords = async () => {
  wordsLoading.value = true
  try {
    const res = await wordApi.getWords({
      keyword: wordSearchKeyword.value || undefined,
      page: 1,
      size: 500
    })
    const currentWordIds = new Set(currentWordBookWords.value.map(w => w.id))
    availableWords.value = res.list.filter(w => !currentWordIds.has(w.id))
  } catch (error) {
    console.error(error)
  } finally {
    wordsLoading.value = false
  }
}

const fetchWordBookWords = async (wordBookId: number) => {
  try {
    currentWordBookWords.value = await wordBookApi.getWordBookWords(wordBookId)
  } catch (error) {
    console.error(error)
  }
}

// Word handlers
const openWordAddDialog = () => {
  resetWordForm()
  showWordAddDialog.value = true
}

const handleWordEdit = (row: Word) => {
  isWordEdit.value = true
  editWordId.value = row.id
  wordForm.word = row.word
  wordForm.phonetic = row.phonetic || ''
  wordForm.pos = row.pos || ''
  wordForm.meaning = row.meaning
  wordForm.example = row.example || ''
  wordForm.memoryTip = row.memoryTip || ''
  showWordAddDialog.value = true
}

const handleWordDelete = async (row: Word) => {
  try {
    await ElMessageBox.confirm(`确定要删除单词 "${row.word}" 吗？`, '警告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await wordApi.deleteWord(row.id)
    ElMessage.success('删除成功')
    fetchWords()
  } catch (error) {
    console.error(error)
  }
}

const submitWord = async () => {
  const valid = await wordFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isWordEdit.value && editWordId.value) {
      await wordApi.updateWord(editWordId.value, wordForm)
      ElMessage.success('更新成功')
    } else {
      await wordApi.createWord(wordForm)
      ElMessage.success('添加成功')
    }
    showWordAddDialog.value = false
    fetchWords()
  } catch (error) {
    console.error(error)
  } finally {
    submitting.value = false
  }
}

const resetWordForm = () => {
  isWordEdit.value = false
  editWordId.value = null
  wordForm.word = ''
  wordForm.phonetic = ''
  wordForm.pos = ''
  wordForm.meaning = ''
  wordForm.example = ''
  wordForm.memoryTip = ''
  nextTick(() => {
    wordFormRef.value?.clearValidate()
  })
}

const handleFileChange = (file: any) => {
  importFile.value = file.raw
}

const submitImport = async () => {
  if (!importFile.value) {
    ElMessage.warning('请选择文件')
    return
  }

  importing.value = true
  try {
    const res = await wordApi.importWords(importFile.value)
    ElMessage.success(`成功导入 ${res.importedCount} 个单词`)
    if (res.failedRows.length > 0) {
      ElMessage.warning(`${res.failedRows.length} 行数据格式错误已跳过`)
    }
    showImportDialog.value = false
    importFile.value = null
    fetchWords()
  } catch (error) {
    console.error(error)
  } finally {
    importing.value = false
  }
}

const handleWordPageChange = (page: number) => {
  wordPagination.page = page
  fetchWords()
}

const handleWordSizeChange = (size: number) => {
  wordPagination.size = size
  wordPagination.page = 1
  fetchWords()
}

// WordBook handlers
const openWordBookAddDialog = () => {
  resetWordBookForm()
  showWordBookAddDialog.value = true
}

const handleWordBookEdit = (row: WordBook) => {
  isWordBookEdit.value = true
  editWordBookId.value = row.id
  wordBookForm.name = row.name
  wordBookForm.description = row.description || ''
  wordBookForm.coverImage = row.coverImage || ''
  wordBookForm.difficultyLevel = row.difficultyLevel
  showWordBookAddDialog.value = true
}

const handleWordBookDelete = async (row: WordBook) => {
  try {
    await ElMessageBox.confirm(`确定要删除词书 "${row.name}" 吗？所有关联的单词归属关系将被解除。`, '警告', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await wordBookApi.deleteWordBook(row.id)
    ElMessage.success('删除成功')
    fetchWordBooks()
  } catch (error) {
    console.error(error)
  }
}

const submitWordBook = async () => {
  const valid = await wordBookFormRef.value?.validate().catch(() => false)
  if (!valid) return

  submittingWordBook.value = true
  try {
    if (isWordBookEdit.value && editWordBookId.value) {
      await wordBookApi.updateWordBook(editWordBookId.value, wordBookForm)
      ElMessage.success('更新成功')
    } else {
      await wordBookApi.createWordBook(wordBookForm)
      ElMessage.success('添加成功')
    }
    showWordBookAddDialog.value = false
    fetchWordBooks()
  } catch (error) {
    console.error(error)
  } finally {
    submittingWordBook.value = false
  }
}

const resetWordBookForm = () => {
  isWordBookEdit.value = false
  editWordBookId.value = null
  wordBookForm.name = ''
  wordBookForm.description = ''
  wordBookForm.coverImage = ''
  wordBookForm.difficultyLevel = ''
  nextTick(() => {
    wordBookFormRef.value?.clearValidate()
  })
}

const handleWordBookPageChange = (page: number) => {
  wordBookPagination.page = page
  fetchWordBooks()
}

const handleWordBookSizeChange = (size: number) => {
  wordBookPagination.size = size
  wordBookPagination.page = 1
  fetchWordBooks()
}

// Manage words dialog handlers
const openManageWordsDialog = async (row: WordBook) => {
  currentWordBook.value = row
  currentWordBookWords.value = []
  availableWords.value = []
  showManageWordsDialog.value = true
  await Promise.all([
    fetchWordBookWords(row.id),
    fetchAvailableWords()
  ])
}

const handleAvailableSelectionChange = (selection: Word[]) => {
  selectedAvailableWords.value = selection
}

const handleSelectedSelectionChange = (selection: Word[]) => {
  selectedWordsToRemove.value = selection
}

const addSelectedWords = async () => {
  if (!currentWordBook.value || selectedAvailableWords.value.length === 0) return

  try {
    await wordBookApi.addWordsToWordBook(currentWordBook.value.id, {
      wordIds: selectedAvailableWords.value.map(w => w.id)
    })
    ElMessage.success(`成功添加 ${selectedAvailableWords.value.length} 个单词`)
    selectedAvailableWords.value = []
    availableWordsTable.value?.clearSelection()
    await Promise.all([
      fetchWordBookWords(currentWordBook.value.id),
      fetchAvailableWords()
    ])
    fetchWordBooks()
  } catch (error) {
    console.error(error)
  }
}

const removeSelectedWords = async () => {
  if (!currentWordBook.value || selectedWordsToRemove.value.length === 0) return

  try {
    await ElMessageBox.confirm(`确定要从词书中移除选中的 ${selectedWordsToRemove.value.length} 个单词吗？`, '移除单词', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    await wordBookApi.removeWordsFromWordBook(currentWordBook.value.id, {
      wordIds: selectedWordsToRemove.value.map(w => w.id)
    })
    ElMessage.success(`成功移除 ${selectedWordsToRemove.value.length} 个单词`)
    selectedWordsToRemove.value = []
    selectedWordsTable.value?.clearSelection()
    await Promise.all([
      fetchWordBookWords(currentWordBook.value.id),
      fetchAvailableWords()
    ])
    fetchWordBooks()
  } catch (error) {
    if (error !== 'cancel') {
      console.error(error)
    }
  }
}

watch(wordSearchKeyword, () => {
  if (showManageWordsDialog.value) {
    fetchAvailableWords()
  }
})

// Formatters
const formatPos = (pos: string) => {
  const map: Record<string, string> = {
    noun: '名词',
    verb: '动词',
    adjective: '形容词',
    adverb: '副词'
  }
  return map[pos] || pos
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
  fetchWords()
  fetchWordBooks()
})
</script>

<style scoped>
.admin-tabs :deep(.el-tabs__header) {
  margin-bottom: var(--space-lg);
}

.tab-label {
  margin-left: 6px;
}

.tab-actions {
  display: flex;
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
}

.word-text {
  font-weight: 600;
  color: var(--c-text-primary);
}

.phonetic-text {
  font-family: serif;
  color: var(--c-text-secondary);
}

.wordbook-name {
  font-weight: 600;
  color: var(--c-text-primary);
}

.cover-preview {
  width: 60px;
  height: 60px;
  border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--c-primary-light), var(--c-primary));
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(255, 255, 255, 0.7);
  font-size: 24px;
  overflow: hidden;
  margin: 0 auto;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pagination-wrapper {
  padding: var(--space-md);
  border-top: 1px solid var(--c-border-light);
  display: flex;
  justify-content: flex-end;
}

.manage-words-content {
  display: flex;
  gap: var(--space-md);
  align-items: stretch;
}

.words-selector,
.words-selected {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.selector-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--space-sm);
}

.selector-header h4 {
  margin: 0;
  color: var(--c-text-primary);
  font-size: var(--font-size-md);
}

.words-list-container {
  flex: 1;
  border: 1px solid var(--c-border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.selector-actions {
  display: flex;
  justify-content: center;
  padding: var(--space-sm) 0;
}

.mr-1 {
  margin-right: 4px;
}
</style>
