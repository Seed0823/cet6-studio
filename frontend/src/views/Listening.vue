<template>
  <div class="practice">
    <div class="practice-head">
      <div>
        <h2 class="sec-title">听力</h2>
        <p class="c6-sub">点击播放听原文（浏览器语音合成朗读），作答后揭示答案与听力原文。</p>
      </div>
      <el-button :loading="loading" @click="loadOne">换一篇</el-button>
    </div>

    <div v-if="paper" class="c6-card l-card">
      <div class="t-meta">
        <el-tag size="small" effect="plain">{{ catLabel(paper.category) }}</el-tag>
        <span class="c6-sub">{{ paper.title }} · 难度 {{ paper.difficulty }}</span>
      </div>

      <div class="l-play">
        <el-button type="primary" :icon="VideoPlay" :disabled="!supported" @click="play">播放原文</el-button>
        <el-button :disabled="!supported" @click="stop">停止</el-button>
        <span v-if="!supported" class="c6-sub">当前浏览器不支持语音合成，可手动朗读下方原文练习</span>
      </div>

      <div v-for="q in paper.questions" :key="q.questionId" class="l-q" :class="{ done: submitted }">
        <div class="l-q-title">{{ q.index }}. {{ q.question }}</div>
        <el-radio-group v-model="chosen[q.questionId]" :disabled="submitted" class="l-opts">
          <el-radio v-for="(opt, i) in q.options" :key="i" :value="i" class="l-opt">
            {{ String.fromCharCode(65 + i) }}. {{ opt }}
          </el-radio>
        </el-radio-group>
        <div v-if="submitted && detailMap[q.questionId]" class="l-feedback" :class="detailMap[q.questionId].right ? 'right' : 'wrong'">
          <span>{{ detailMap[q.questionId].right ? '✓ 正确' : '✗ 正确选项：' + String.fromCharCode(65 + detailMap[q.questionId].correctIndex) }}</span>
          <span v-if="detailMap[q.questionId].explain" class="c6-sub">解析：{{ detailMap[q.questionId].explain }}</span>
        </div>
      </div>

      <div class="l-actions">
        <el-button type="primary" :disabled="submitted || !allAnswered" :loading="submitting" @click="submit">
          提交并看答案
        </el-button>
        <el-button v-if="submitted" @click="toggleScript">{{ showScript ? '隐藏原文' : '查看听力原文' }}</el-button>
      </div>

      <div v-if="submitted" class="l-score">得分：<b>{{ result.correct }}</b> / {{ result.total }}</div>
      <div v-if="showScript && submitted" class="l-script">
        <div class="t-ref-title">听力原文</div>
        <div class="l-script-text">{{ paper.script }}</div>
      </div>
    </div>
    <el-empty v-else-if="!loading" description="暂无题目" />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { VideoPlay } from '@element-plus/icons-vue'
import { listeningApi } from '@/api'

const loading = ref(false)
const submitting = ref(false)
const paper = ref(null)
const chosen = reactive({})
const submitted = ref(false)
const result = ref(null)
const showScript = ref(false)
const supported = typeof window !== 'undefined' && 'speechSynthesis' in window

const allAnswered = computed(() =>
  paper.value && paper.value.questions.every((q) => chosen[q.questionId] !== undefined)
)
const detailMap = computed(() => {
  const m = {}
  if (result.value && result.value.details) {
    result.value.details.forEach((d) => { m[d.questionId] = d })
  }
  return m
})

function play() {
  if (!supported || !paper.value) return
  window.speechSynthesis.cancel()
  const u = new SpeechSynthesisUtterance(paper.value.script)
  u.lang = 'en-US'
  u.rate = 0.9
  window.speechSynthesis.speak(u)
}
function stop() {
  if (supported) window.speechSynthesis.cancel()
}

async function loadOne() {
  loading.value = true
  submitted.value = false
  showScript.value = false
  result.value = null
  for (const k in chosen) delete chosen[k]
  try {
    const res = await listeningApi.random(1)
    paper.value = (res.data || [])[0] || null
  } finally {
    loading.value = false
  }
}

async function submit() {
  if (!paper.value) return
  submitting.value = true
  try {
    const answers = paper.value.questions.map((q) => ({
      questionId: q.questionId,
      chosenIndex: chosen[q.questionId]
    }))
    const res = await listeningApi.submit({ id: paper.value.id, answers })
    result.value = res.data
    submitted.value = true
  } finally {
    submitting.value = false
  }
}

function toggleScript() {
  showScript.value = !showScript.value
}
function catLabel(c) {
  return { dialog: '对话', passage: '短文', news: '新闻' }[c] || c || '听力'
}

onMounted(loadOne)
onBeforeUnmount(() => { if (supported) window.speechSynthesis.cancel() })
</script>

<style scoped>
.practice { max-width: 860px; margin: 0 auto; }
.practice-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; gap: 12px; }
.sec-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; color: var(--c6-text); }
.l-card { padding: 22px; }
.t-meta { display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
.l-play { display: flex; align-items: center; gap: 12px; margin-bottom: 18px; flex-wrap: wrap; }
.l-q { padding: 14px 0; border-top: 1px solid var(--c6-border-soft); }
.l-q-title { font-size: 15px; color: var(--c6-text); margin-bottom: 10px; }
.l-opts { display: flex; flex-direction: column; gap: 6px; }
.l-opt { white-space: normal; height: auto; margin-right: 0; }
.l-feedback { margin-top: 8px; font-size: 13px; display: flex; gap: 12px; flex-wrap: wrap; align-items: center; }
.l-feedback.right { color: var(--c6-ok, #6f7a6f); }
.l-feedback.wrong { color: var(--c6-danger, #8a7d7d); }
.l-actions { margin-top: 16px; display: flex; gap: 12px; }
.l-score { margin-top: 14px; font-size: 15px; color: var(--c6-text); }
.l-score b { color: var(--c6-primary); font-size: 20px; }
.l-script { margin-top: 14px; border-top: 1px solid var(--c6-border-soft); padding-top: 14px; }
.t-ref-title { font-size: 13px; color: var(--c6-text-sub); margin-bottom: 6px; }
.l-script-text { font-size: 16px; line-height: 1.9; color: var(--c6-text); white-space: pre-wrap; }
</style>
