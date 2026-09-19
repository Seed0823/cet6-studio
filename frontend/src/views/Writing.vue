<template>
  <div class="practice">
    <div class="practice-head">
      <div>
        <h2 class="sec-title">写作 · 命题作文</h2>
        <p class="c6-sub">根据命题写作，提交后查看字数达标情况、关键词覆盖率与参考范文。</p>
      </div>
      <el-button :loading="loading" @click="loadOne">换一题</el-button>
    </div>

    <div v-if="item" class="c6-card w-card">
      <div class="t-meta">
        <el-tag size="small" effect="plain">{{ catLabel(item.category) }}</el-tag>
        <span class="c6-sub">建议 {{ item.minWords }}-{{ item.maxWords }} 词</span>
      </div>
      <div class="w-prompt">{{ item.prompt }}</div>
      <div v-if="item.requirement" class="w-req c6-sub">{{ item.requirement }}</div>

      <el-collapse v-if="item.outlinePoints && item.outlinePoints.length" class="w-outline">
        <el-collapse-item title="思路提纲（可选参考）" name="1">
          <ol class="w-outline-list">
            <li v-for="(o, i) in item.outlinePoints" :key="i">{{ o }}</li>
          </ol>
        </el-collapse-item>
      </el-collapse>

      <el-input v-model="answer" type="textarea" :rows="10" placeholder="在此写作文…" class="w-input" />
      <div class="w-actions">
        <el-button type="primary" :disabled="!answer.trim()" :loading="submitting" @click="submit">
          提交并看范文
        </el-button>
        <span :class="['w-count', wordCountClass]">{{ wordCount }} 词 / 要求 ≥ {{ item.minWords }}</span>
      </div>

      <div v-if="result" class="w-result">
        <div class="w-stat">
          <span>字数：<b :class="result.meetsLength ? 'ok' : 'bad'">{{ result.wordCount }}</b> / 要求 ≥ {{ result.minWords }}</span>
          <span>关键词覆盖率：<b>{{ result.coverage }}%</b>（命中 {{ result.matchedKeywords.length }} / {{ result.totalKeywords }}）</span>
        </div>
        <div class="t-ref-title">参考范文</div>
        <div class="w-ref-en">{{ result.referenceEssay }}</div>
        <div class="t-key-list">
          <span
            v-for="k in result.keywords"
            :key="k"
            :class="['t-key', result.matchedKeywords.includes(k) ? 'hit' : '']"
          >{{ k }}</span>
        </div>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="暂无题目" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { writingApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const item = ref(null)
const answer = ref('')
const result = ref(null)

const wordCount = computed(() => {
  const t = answer.value.trim()
  return t ? t.split(/\s+/).filter(Boolean).length : 0
})
const wordCountClass = computed(() =>
  item.value && wordCount.value >= (item.value.minWords || 0) ? 'ok' : 'bad'
)

async function loadOne() {
  loading.value = true
  result.value = null
  answer.value = ''
  try {
    const res = await writingApi.random(1)
    item.value = (res.data || [])[0] || null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!item.value) return
  submitting.value = true
  try {
    const res = await writingApi.submit({ id: item.value.id, answer: answer.value })
    result.value = res.data
  } finally {
    submitting.value = false
  }
}

function catLabel(c) {
  return { argument: '议论文', letter: '书信', chart: '图表' }[c] || c || '写作'
}

onMounted(loadOne)
</script>

<style scoped>
.practice { max-width: 880px; margin: 0 auto; }
.practice-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; gap: 12px; }
.sec-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; color: var(--c6-text); }
.w-card { padding: 22px; }
.t-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.w-prompt { font-size: 16px; line-height: 1.8; color: var(--c6-text); white-space: pre-wrap; }
.w-req { margin-top: 6px; }
.w-outline { margin: 14px 0; }
.w-outline-list { margin: 0; padding-left: 20px; color: var(--c6-text-sub); line-height: 1.9; }
.w-input { margin: 8px 0 12px; }
.w-actions { display: flex; align-items: center; gap: 14px; }
.w-count { font-size: 14px; }
.w-count.ok { color: var(--c6-ok, #6f7a6f); }
.w-count.bad { color: var(--c6-danger, #8a7d7d); }
.w-result { margin-top: 18px; border-top: 1px solid var(--c6-border-soft); padding-top: 16px; }
.w-stat { display: flex; flex-wrap: wrap; gap: 18px; font-size: 15px; margin-bottom: 14px; color: var(--c6-text); }
.w-stat b.ok { color: var(--c6-ok, #6f7a6f); }
.w-stat b.bad { color: var(--c6-danger, #8a7d7d); }
.t-ref-title { font-size: 13px; color: var(--c6-text-sub); margin: 10px 0 6px; }
.w-ref-en { font-size: 16px; line-height: 1.9; color: var(--c6-text); white-space: pre-wrap; }
.t-key-list { display: flex; flex-wrap: wrap; gap: 8px; margin-top: 10px; }
.t-key { font-size: 13px; padding: 3px 10px; border-radius: 20px; border: 1px solid var(--c6-border); color: var(--c6-text-sub); }
.t-key.hit { border-color: var(--c6-primary); color: var(--c6-primary); background: var(--c6-primary-soft); }
</style>
