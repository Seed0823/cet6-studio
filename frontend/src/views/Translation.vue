<template>
  <div class="practice">
    <div class="practice-head">
      <div>
        <h2 class="sec-title">翻译 · 中译英</h2>
        <p class="c6-sub">把下方中文译成英文，提交后对照参考译文并查看关键点覆盖率（覆盖率仅供参考自测，非官方评分）。</p>
      </div>
      <el-button :loading="loading" @click="loadOne">换一题</el-button>
    </div>

    <div v-if="item" class="c6-card t-card">
      <div class="t-meta">
        <el-tag size="small" effect="plain">{{ catLabel(item.category) }}</el-tag>
        <span class="c6-sub">难度 {{ item.difficulty }}</span>
      </div>
      <div class="t-chinese">{{ item.chineseText }}</div>

      <el-input
        v-model="answer"
        type="textarea"
        :rows="5"
        placeholder="在此输入你的英文译文…"
        class="t-input"
      />
      <div class="t-actions">
        <el-button type="primary" :disabled="!answer.trim()" :loading="submitting" @click="submit">
          对照参考并自测
        </el-button>
        <span class="c6-sub">{{ wordCount }} 词</span>
      </div>

      <div v-if="result" class="t-result">
        <div class="t-score">
          关键点覆盖率 <b>{{ result.coverage }}%</b>
          <span class="c6-sub">（命中 {{ result.matchedPoints.length }} / {{ result.totalPoints }}）</span>
        </div>
        <div class="t-ref">
          <div class="t-ref-title">参考译文</div>
          <div class="t-ref-en">{{ result.referenceEn }}</div>
        </div>

        <!-- 机器译文：给「同一句中文的另一种译法」，和你自己的译文三方对照 -->
        <div v-if="result.machineTranslation" class="t-ref t-machine">
          <div class="t-ref-title">
            机器参考译文
            <el-tag v-if="result.machineEngine" size="small" effect="plain">{{ result.machineEngine }}</el-tag>
          </div>
          <div class="t-ref-en">{{ result.machineTranslation }}</div>
          <div v-if="result.machineNote" class="t-note">{{ result.machineNote }}</div>
        </div>

        <div class="t-keys">
          <div class="t-ref-title">关键点</div>
          <div class="t-key-list">
            <span
              v-for="k in result.keyPoints"
              :key="k"
              :class="['t-key', result.matchedPoints.includes(k) ? 'hit' : '']"
            >{{ k }}</span>
          </div>
        </div>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="暂无题目" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { translationApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const item = ref(null)
const answer = ref('')
const result = ref(null)

const wordCount = computed(() => {
  const t = answer.value.trim()
  return t ? t.split(/\s+/).filter(Boolean).length : 0
})

async function loadOne() {
  loading.value = true
  result.value = null
  answer.value = ''
  try {
    const res = await translationApi.random(1)
    item.value = (res.data || [])[0] || null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!item.value) return
  submitting.value = true
  try {
    const res = await translationApi.submit({ id: item.value.id, answer: answer.value })
    result.value = res.data
  } finally {
    submitting.value = false
  }
}

function catLabel(c) {
  return { culture: '文化', society: '社会', tech: '科技', economy: '经济' }[c] || c || '翻译'
}

onMounted(loadOne)
</script>

<style scoped>
.practice { max-width: 860px; margin: 0 auto; }
.practice-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; gap: 12px; }
.sec-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; color: var(--c6-text); }
.t-card { padding: 22px; }
.t-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.t-chinese { font-size: 17px; line-height: 1.9; color: var(--c6-text); background: var(--c6-primary-soft); padding: 16px 18px; border-radius: 10px; margin-bottom: 16px; }
.t-input { margin-bottom: 12px; }
.t-actions { display: flex; align-items: center; gap: 14px; }
.t-result { margin-top: 18px; border-top: 1px solid var(--c6-border-soft); padding-top: 16px; }
.t-score { font-size: 15px; margin-bottom: 14px; color: var(--c6-text); }
.t-score b { color: var(--c6-primary); font-size: 20px; }
.t-ref-title { font-size: 13px; color: var(--c6-text-sub); margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.t-ref-en { font-size: 16px; line-height: 1.9; color: var(--c6-text); }
.t-machine { margin-top: 14px; }
.t-note { margin-top: 6px; font-size: 12px; color: var(--c6-text-sub); line-height: 1.7; }
.t-key-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 4px; }
.t-key { font-size: 13px; padding: 3px 10px; border-radius: 20px; border: 1px solid var(--c6-border); color: var(--c6-text-sub); }
.t-key.hit { border-color: var(--c6-primary); color: var(--c6-primary); background: var(--c6-primary-soft); }
</style>
