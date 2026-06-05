<template>
  <div class="review-stats-bar">
    <div class="stats-row">
      <div class="stat-item">
        <span class="stat-label">已复习</span>
        <span class="stat-value highlighted">{{ stats.reviewedCount }}</span>
        <span class="stat-unit">词</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-label">认识率</span>
        <span class="stat-value" :class="rateClass">{{ stats.knownRate }}%</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-label">剩余</span>
        <span class="stat-value">{{ stats.remainingCount }}</span>
        <span class="stat-unit">词</span>
      </div>
      <div class="stat-divider" />
      <div class="stat-item">
        <span class="stat-label">预计剩余</span>
        <span class="stat-value">{{ formatTime(stats.estimatedRemainingSeconds) }}</span>
      </div>
    </div>
    <div class="progress-bar-container">
      <div class="progress-bar-bg">
        <div class="progress-bar-fill known" :style="{ width: knownPercent + '%' }" />
        <div class="progress-bar-fill vague" :style="{ width: vaguePercent + '%', left: knownPercent + '%' }" />
        <div class="progress-bar-fill unknown" :style="{ width: unknownPercent + '%', left: (knownPercent + vaguePercent) + '%' }" />
      </div>
      <div class="progress-legend">
        <span class="legend-item"><span class="legend-dot known" />认识 {{ stats.knownCount }}</span>
        <span class="legend-item"><span class="legend-dot vague" />模糊 {{ stats.vagueCount }}</span>
        <span class="legend-item"><span class="legend-dot unknown" />不认识 {{ stats.unknownCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { SessionStats } from '@/types'

const props = defineProps<{
  stats: SessionStats
}>()

const knownPercent = computed(() => {
  const total = props.stats.reviewedCount
  if (total === 0) return 0
  return Math.round((props.stats.knownCount / total) * 100)
})

const vaguePercent = computed(() => {
  const total = props.stats.reviewedCount
  if (total === 0) return 0
  return Math.round((props.stats.vagueCount / total) * 100)
})

const unknownPercent = computed(() => {
  const total = props.stats.reviewedCount
  if (total === 0) return 0
  return Math.round((props.stats.unknownCount / total) * 100)
})

const rateClass = computed(() => {
  const rate = props.stats.knownRate
  if (rate >= 80) return 'success'
  if (rate >= 60) return 'warning'
  return 'danger'
})

const formatTime = (seconds: number): string => {
  if (seconds < 60) return `${seconds}秒`
  if (seconds < 3600) return `${Math.floor(seconds / 60)}分钟`
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return `${hours}小时${minutes}分`
}
</script>

<style scoped>
.review-stats-bar {
  background: var(--c-bg-card);
  border-radius: var(--radius-lg);
  padding: var(--space-lg);
  margin-bottom: var(--space-lg);
  box-shadow: var(--shadow-sm);
}

.stats-row {
  display: flex;
  align-items: center;
  justify-content: space-around;
  margin-bottom: var(--space-md);
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-label {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
}

.stat-value {
  font-size: var(--font-size-xl);
  font-weight: 700;
  color: var(--c-text-primary);
}

.stat-value.highlighted {
  color: var(--c-primary);
}

.stat-value.success {
  color: var(--c-success);
}

.stat-value.warning {
  color: var(--c-warning);
}

.stat-value.danger {
  color: var(--c-danger);
}

.stat-unit {
  font-size: var(--font-size-xs);
  color: var(--c-text-tertiary);
  margin-left: 2px;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: var(--c-border-light);
}

.progress-bar-container {
  margin-top: var(--space-sm);
}

.progress-bar-bg {
  position: relative;
  height: 8px;
  background: var(--c-bg-body);
  border-radius: var(--radius-full);
  overflow: hidden;
}

.progress-bar-fill {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  transition: width var(--transition-normal);
}

.progress-bar-fill.known {
  background: var(--c-success);
}

.progress-bar-fill.vague {
  background: var(--c-warning);
}

.progress-bar-fill.unknown {
  background: var(--c-danger);
}

.progress-legend {
  display: flex;
  justify-content: center;
  gap: var(--space-lg);
  margin-top: var(--space-sm);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: var(--font-size-xs);
  color: var(--c-text-secondary);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.known {
  background: var(--c-success);
}

.legend-dot.vague {
  background: var(--c-warning);
}

.legend-dot.unknown {
  background: var(--c-danger);
}

@media (max-width: 640px) {
  .stats-row {
    flex-wrap: wrap;
    gap: var(--space-md);
  }
  
  .stat-divider {
    display: none;
  }
  
  .stat-item {
    flex: 1;
    min-width: 80px;
  }
  
  .progress-legend {
    flex-wrap: wrap;
    gap: var(--space-sm);
  }
}
</style>
