<template>
  <div class="word-detail-page page-container" v-if="word">
    <PageHeader title="单词详情" @back="$router.back()">
      <template #actions>
        <el-button @click="$router.push(`/mindmap/${word.id}`)">
          <el-icon class="mr-1"><Share /></el-icon> 思维导图
        </el-button>
        <el-button type="primary" @click="addToPlan">
          <el-icon class="mr-1"><Plus /></el-icon> 加入计划
        </el-button>
      </template>
    </PageHeader>
    
    <div class="detail-content" v-loading="loading">
      <!-- Main Word Card -->
      <el-card class="main-card">
        <div class="word-header">
          <div class="word-title-row">
            <h1 class="word-title">{{ word.word }}</h1>
            <div class="pronounce-group">
              <el-tooltip content="美式发音" placement="top">
                <button
                  class="pronounce-btn"
                  :class="{ active: accent === 'en-US' }"
                  @click="speak('en-US')"
                >
                  <el-icon><VideoPlay /></el-icon>
                  <span class="accent-label">美</span>
                </button>
              </el-tooltip>
              <el-tooltip content="英式发音" placement="top">
                <button
                  class="pronounce-btn"
                  :class="{ active: accent === 'en-GB' }"
                  @click="speak('en-GB')"
                >
                  <el-icon><VideoPlay /></el-icon>
                  <span class="accent-label">英</span>
                </button>
              </el-tooltip>
            </div>
          </div>
          <div class="word-meta">
            <span v-if="word.phonetic" class="phonetic">/{{ word.phonetic }}/</span>
            <el-tag v-if="word.pos" size="large" effect="dark" type="primary" class="pos-tag">
              {{ formatPos(word.pos) }}
            </el-tag>
          </div>
        </div>
        
        <el-divider />
        
        <div class="word-body">
          <div class="detail-section">
            <h3 class="section-label">释义</h3>
            <p class="meaning-text">{{ word.meaning }}</p>
          </div>
          
          <div class="detail-section" v-if="word.example">
            <h3 class="section-label">基础例句</h3>
            <div class="example-box">
              <p class="example-text" v-html="highlightTargetWord(word.example)"></p>
            </div>
          </div>
          
          <div class="detail-section" v-if="word.memoryTip">
            <h3 class="section-label">记忆提示</h3>
            <el-alert 
              type="success" 
              :title="word.memoryTip" 
              :closable="false" 
              show-icon
              class="memory-alert"
            />
          </div>
        </div>
      </el-card>

      <!-- Context Examples Card -->
      <el-card v-if="examples.length > 0" class="section-card">
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Document /></el-icon>
            <span>语境例句扩展</span>
            <span class="example-count">共 {{ examples.length }} 条</span>
          </div>
        </template>
        <div class="examples-list" @click="handleExampleClick">
          <div
            v-for="(ex, idx) in examples"
            :key="ex.id"
            class="example-item"
          >
            <div class="example-item-header">
              <span class="example-index">{{ idx + 1 }}</span>
              <el-tag v-if="ex.scene" size="small" type="info" effect="plain" class="scene-tag">
                {{ ex.scene }}
              </el-tag>
            </div>
            <p class="example-sentence" v-html="renderExampleSentence(ex.sentence)"></p>
            <p v-if="ex.translation" class="example-translation">{{ ex.translation }}</p>
          </div>
        </div>
      </el-card>

      <!-- Synonyms & Antonyms Card -->
      <el-card
        v-if="(word.synonyms && word.synonyms.length > 0) || (word.antonyms && word.antonyms.length > 0)"
        class="section-card"
      >
        <template #header>
          <div class="card-header">
            <el-icon class="header-icon"><Connection /></el-icon>
            <span>近义词 / 反义词</span>
          </div>
        </template>
        <div class="relations-container">
          <div v-if="word.synonyms && word.synonyms.length > 0" class="relation-group">
            <h4 class="relation-label synonym-label">
              <el-icon><Select /></el-icon> 近义词
            </h4>
            <div class="relation-tags">
              <el-tag
                v-for="syn in word.synonyms"
                :key="'syn-' + syn.id"
                class="relation-tag synonym-tag"
                effect="light"
                type="success"
                size="large"
                @click="goToWord(syn.id)"
              >
                <span class="tag-word">{{ syn.word }}</span>
                <span class="tag-meaning">{{ syn.meaning }}</span>
              </el-tag>
            </div>
          </div>
          <div v-if="word.antonyms && word.antonyms.length > 0" class="relation-group">
            <h4 class="relation-label antonym-label">
              <el-icon><Close /></el-icon> 反义词
            </h4>
            <div class="relation-tags">
              <el-tag
                v-for="ant in word.antonyms"
                :key="'ant-' + ant.id"
                class="relation-tag antonym-tag"
                effect="light"
                type="danger"
                size="large"
                @click="goToWord(ant.id)"
              >
                <span class="tag-word">{{ ant.word }}</span>
                <span class="tag-meaning">{{ ant.meaning }}</span>
              </el-tag>
            </div>
          </div>
        </div>
      </el-card>
      
      <!-- Related Actions -->
      <div class="related-actions">
         <ActionCard 
          title="查看思维导图" 
          description="探索该单词的关联词汇网络"
          @click="$router.push(`/mindmap/${word.id}`)"
        >
          <template #icon><Share /></template>
        </ActionCard>
        
        <ActionCard 
          title="立即加入复习" 
          description="将此单词加入今日复习队列"
          @click="addToPlan"
        >
          <template #icon><Calendar /></template>
        </ActionCard>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Share, Plus, Calendar, VideoPlay, Document, Connection, Select, Close } from '@element-plus/icons-vue'
import type { Word, WordExample } from '@/types'
import { wordApi } from '@/api/word'
import { statsApi } from '@/api/study'
import PageHeader from '@/components/ui/PageHeader.vue'
import ActionCard from '@/components/ui/ActionCard.vue'

const route = useRoute()
const router = useRouter()
const word = ref<Word | null>(null)
const examples = ref<WordExample[]>([])
const loading = ref(false)
const accent = ref<'en-US' | 'en-GB'>('en-US')

const allWordsMap = computed(() => {
  const map = new Map<string, number>()
  examples.value.forEach(ex => {
    const tokens = ex.sentence.split(/[^a-zA-Z]+/).filter(t => t.length > 1)
    tokens.forEach(t => {
      const lower = t.toLowerCase()
      if (!map.has(lower) && lower !== word.value?.word.toLowerCase()) {
        map.set(lower, 0)
      }
    })
  })
  return map
})

const fetchWord = async () => {
  const id = Number(route.params.id)
  if (!id) return
  
  loading.value = true
  try {
    const [wordData, exampleData] = await Promise.all([
      wordApi.getWordById(id),
      wordApi.getWordExamples(id)
    ])
    word.value = wordData
    examples.value = exampleData
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

const addToPlan = async () => {
  if (!word.value) return
  try {
    await statsApi.createStudyPlan(word.value.id, 'TODAY')
    ElMessage.success(`已将 "${word.value.word}" 加入今日学习计划`)
  } catch (error) {
    console.error(error)
  }
}

const formatPos = (pos: string) => {
  const map: Record<string, string> = {
    noun: '名词',
    verb: '动词',
    adjective: '形容词',
    adverb: '副词'
  }
  return map[pos] || pos
}

const speak = (lang: 'en-US' | 'en-GB') => {
  if (!word.value) return
  accent.value = lang

  if (!('speechSynthesis' in window)) {
    ElMessage.warning('当前浏览器不支持语音朗读功能')
    return
  }

  window.speechSynthesis.cancel()
  const utterance = new SpeechSynthesisUtterance(word.value.word)
  utterance.lang = lang
  utterance.rate = 0.9
  utterance.pitch = 1

  const voices = window.speechSynthesis.getVoices()
  const matchedVoice = voices.find(v => v.lang === lang)
  if (matchedVoice) {
    utterance.voice = matchedVoice
  }

  window.speechSynthesis.speak(utterance)
}

const escapeHtml = (text: string) => {
  const div = document.createElement('div')
  div.textContent = text
  return div.innerHTML
}

const highlightTargetWord = (sentence: string) => {
  if (!word.value) return escapeHtml(sentence)
  const target = word.value.word
  const escapedSentence = escapeHtml(sentence)
  const regex = new RegExp(`\\b(${target.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})\\b`, 'gi')
  return escapedSentence.replace(regex, '<span class="target-highlight">$1</span>')
}

const renderExampleSentence = (sentence: string) => {
  if (!word.value) return escapeHtml(sentence)
  const escapedSentence = escapeHtml(sentence)
  const target = word.value.word
  const targetRegex = new RegExp(`\\b(${target.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')})\\b`, 'gi')

  let result = escapedSentence.replace(targetRegex, '||TARGET_START||$1||TARGET_END||')

  const wordRegex = /\b([a-zA-Z]{2,})\b/g
  result = result.replace(wordRegex, (match, group1) => {
    if (match.includes('||TARGET_')) return match
    return `<span class="clickable-word" data-word="${group1.toLowerCase()}">${group1}</span>`
  })

  result = result.replace(/\|\|TARGET_START\|\|/g, '<span class="target-highlight">')
  result = result.replace(/\|\|TARGET_END\|\|/g, '</span>')

  return result
}

const handleExampleClick = (e: MouseEvent) => {
  const target = e.target as HTMLElement
  if (target.classList.contains('clickable-word')) {
    const wordText = target.dataset.word
    if (wordText) {
      ElMessage.info(`正在搜索单词: ${wordText}，请在单词列表中查找`)
    }
  }
}

const goToWord = (id: number) => {
  router.push(`/words/${id}`)
}

onMounted(() => {
  fetchWord()

  if ('speechSynthesis' in window) {
    window.speechSynthesis.getVoices()
    window.speechSynthesis.onvoiceschanged = () => {
      window.speechSynthesis.getVoices()
    }
  }
})
</script>

<style scoped>
.word-detail-page {
  max-width: 1000px;
}

.main-card {
  margin-bottom: var(--space-xl);
  border-radius: var(--radius-lg);
}

.section-card {
  margin-bottom: var(--space-xl);
  border-radius: var(--radius-lg);
}

.card-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  font-size: var(--font-size-lg);
  font-weight: 600;
  color: var(--c-text-primary);
}

.header-icon {
  color: var(--c-primary);
  font-size: 20px;
}

.example-count {
  margin-left: auto;
  font-size: var(--font-size-sm);
  color: var(--c-text-tertiary);
  font-weight: 400;
}

.word-header {
  text-align: center;
  padding: var(--space-lg) 0;
}

.word-title-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
  margin-bottom: var(--space-xs);
}

.word-title {
  font-size: 48px;
  font-weight: 800;
  color: var(--c-text-primary);
  margin: 0;
  letter-spacing: -1px;
}

.pronounce-group {
  display: flex;
  gap: var(--space-xs);
}

.pronounce-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 6px 12px;
  border: 2px solid var(--c-border);
  border-radius: var(--radius-full);
  background: var(--c-bg-card);
  color: var(--c-text-secondary);
  cursor: pointer;
  font-size: var(--font-size-sm);
  font-weight: 500;
  transition: all var(--transition-fast);
}

.pronounce-btn:hover {
  border-color: var(--c-primary);
  color: var(--c-primary);
  background: var(--c-primary-bg);
}

.pronounce-btn.active {
  border-color: var(--c-primary);
  color: var(--c-primary);
  background: var(--c-primary-bg);
}

.accent-label {
  font-size: 12px;
}

.word-meta {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-md);
}

.phonetic {
  font-family: 'Times New Roman', serif;
  font-size: 20px;
  color: var(--c-text-secondary);
  font-style: italic;
}

.pos-tag {
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.word-body {
  padding: 0 var(--space-lg);
}

.detail-section {
  margin-bottom: var(--space-xl);
}

.section-label {
  font-size: var(--font-size-sm);
  text-transform: uppercase;
  color: var(--c-text-tertiary);
  letter-spacing: 1px;
  margin-bottom: var(--space-sm);
  font-weight: 600;
}

.meaning-text {
  font-size: 20px;
  color: var(--c-text-primary);
  line-height: 1.6;
  font-weight: 500;
}

.example-box {
  background: var(--c-bg-body);
  padding: var(--space-lg);
  border-radius: var(--radius-md);
  border-left: 4px solid var(--c-primary);
}

.example-text {
  font-size: 18px;
  color: var(--c-text-secondary);
  font-style: italic;
  line-height: 1.6;
  margin: 0;
}

.target-highlight {
  background: linear-gradient(transparent 60%, rgba(59, 130, 246, 0.25) 60%);
  font-weight: 700;
  color: var(--c-primary-dark);
  padding: 0 2px;
  border-radius: 2px;
}

.examples-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-md);
}

.example-item {
  padding: var(--space-md) var(--space-lg);
  background: var(--c-bg-body);
  border-radius: var(--radius-md);
  border-left: 3px solid var(--c-primary-light);
  transition: all var(--transition-fast);
}

.example-item:hover {
  border-left-color: var(--c-primary);
  background: var(--c-primary-bg);
}

.example-item-header {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-xs);
}

.example-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  background: var(--c-primary);
  color: white;
  border-radius: var(--radius-full);
  font-size: 12px;
  font-weight: 700;
}

.scene-tag {
  font-size: 12px;
}

.example-sentence {
  font-size: 16px;
  color: var(--c-text-primary);
  line-height: 1.7;
  margin: 0 0 var(--space-xs) 0;
  word-break: break-word;
}

.example-sentence :deep(.clickable-word) {
  color: var(--c-primary);
  cursor: pointer;
  border-bottom: 1px dashed transparent;
  transition: all var(--transition-fast);
  padding: 0 1px;
}

.example-sentence :deep(.clickable-word:hover) {
  border-bottom-color: var(--c-primary);
  background: var(--c-primary-bg);
  border-radius: 2px;
}

.example-translation {
  font-size: 14px;
  color: var(--c-text-tertiary);
  margin: 0;
  line-height: 1.5;
}

.relations-container {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.relation-group {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
}

.relation-label {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--font-size-base);
  margin: 0;
  font-weight: 600;
}

.synonym-label {
  color: var(--c-success);
}

.antonym-label {
  color: var(--c-danger);
}

.relation-tags {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-sm);
}

.relation-tag {
  cursor: pointer;
  transition: all var(--transition-fast);
  display: inline-flex !important;
  align-items: baseline;
  gap: var(--space-xs);
  padding: 8px 14px !important;
  border-radius: var(--radius-md) !important;
  font-weight: 500;
}

.relation-tag:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.tag-word {
  font-size: 15px;
  font-weight: 700;
}

.tag-meaning {
  font-size: 12px;
  opacity: 0.85;
}

.related-actions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-lg);
}

.mr-1 { margin-right: 4px; }

/* Responsive */
@media (max-width: 640px) {
  .word-title {
    font-size: 32px;
  }

  .word-title-row {
    flex-direction: column;
    gap: var(--space-sm);
  }

  .pronounce-group {
    flex-wrap: wrap;
    justify-content: center;
  }
  
  .related-actions {
    grid-template-columns: 1fr;
  }

  .relation-tags {
    flex-direction: column;
  }

  .relation-tag {
    width: 100%;
  }
}
</style>
