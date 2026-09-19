<template>
  <div class="practice exam">
    <div class="practice-head">
      <div>
        <h2 class="sec-title">真题模考</h2>
        <p class="c6-sub">限时组合卷：听力 + 翻译 + 写作。逐节作答并提交，最后查看估分报告。</p>
      </div>
      <div class="exam-timer" :class="{ warn: remaining <= 300 }">
        <el-icon><Timer /></el-icon>
        <span>{{ fmtTime(remaining) }}</span>
      </div>
    </div>

    <div v-if="loading" class="c6-card" style="padding:40px;text-align:center;color:var(--c6-text-sub)">组卷中…</div>
    <div v-else-if="!paper && !transItem && !writeItem" class="c6-card" style="padding:40px;text-align:center;color:var(--c6-text-sub)">题库不足，无法组卷</div>

    <template v-else>
      <!-- 听力 -->
      <div class="c6-card ex-sec" :class="{ locked: listenResult }">
        <div class="ex-sec-head">
          <span class="ex-no">Section A</span>
          <span class="ex-name">听力</span>
          <el-tag v-if="listenResult" type="success" size="small" effect="plain">已提交 {{ listenResult.correct }}/{{ listenResult.total }}</el-tag>
        </div>
        <template v-if="paper">
          <div class="l-play">
            <el-button type="primary" :icon="VideoPlay" :disabled="!supported || listenResult" @click="play">播放原文</el-button>
            <el-button :disabled="!supported || listenResult" @click="stop">停止</el-button>
          </div>
          <div v-for="q in paper.questions" :key="q.questionId" class="l-q">
            <div class="l-q-title">{{ q.index }}. {{ q.question }}</div>
            <el-radio-group v-model="listenChosen[q.questionId]" :disabled="!!listenResult" class="l-opts">
              <el-radio v-for="(opt, i) in q.options" :key="i" :value="i" class="l-opt">{{ String.fromCharCode(65 + i) }}. {{ opt }}</el-radio>
            </el-radio-group>
            <div v-if="listenResult && listenDetail[q.questionId]" class="l-feedback" :class="listenDetail[q.questionId].right ? 'right' : 'wrong'">
              <span>{{ listenDetail[q.questionId].right ? '✓ 正确' : '✗ 正确选项：' + String.fromCharCode(65 + listenDetail[q.questionId].correctIndex) }}</span>
            </div>
          </div>
          <el-button v-if="!listenResult" type="primary" :disabled="!allListenAnswered" :loading="listenSubmitting" @click="submitListen">提交本节（听力）</el-button>
        </template>
      </div>

      <!-- 翻译 -->
      <div class="c6-card ex-sec" :class="{ locked: transResult }">
        <div class="ex-sec-head">
          <span class="ex-no">Section B</span>
          <span class="ex-name">翻译</span>
          <el-tag v-if="transResult" type="success" size="small" effect="plain">已提交 覆盖率 {{ transResult.coverage }}%</el-tag>
        </div>
        <template v-if="transItem">
          <div class="t-chinese">{{ transItem.chineseText }}</div>
          <el-input v-model="transAnswer" type="textarea" :rows="4" :disabled="!!transResult" placeholder="在此输入英文译文…" />
          <div class="ex-actions">
            <el-button v-if="!transResult" type="primary" :disabled="!transAnswer.trim()" :loading="transSubmitting" @click="submitTrans">提交本节（翻译）</el-button>
            <span v-if="transResult" class="c6-sub">参考译文已记录，可在下方查看</span>
          </div>
          <div v-if="transResult" class="ex-ref">
            <div class="t-ref-title">参考译文</div>
            <div class="t-ref-en">{{ transResult.referenceEn }}</div>
          </div>
        </template>
      </div>

      <!-- 写作 -->
      <div class="c6-card ex-sec" :class="{ locked: writeResult }">
        <div class="ex-sec-head">
          <span class="ex-no">Section C</span>
          <span class="ex-name">写作</span>
          <el-tag v-if="writeResult" type="success" size="small" effect="plain">已提交 {{ writeResult.wordCount }} 词</el-tag>
        </div>
        <template v-if="writeItem">
          <div class="w-prompt">{{ writeItem.prompt }}</div>
          <el-input v-model="writeAnswer" type="textarea" :rows="8" :disabled="!!writeResult" placeholder="在此写作文…" />
          <div class="ex-actions">
            <el-button v-if="!writeResult" type="primary" :disabled="!writeAnswer.trim()" :loading="writeSubmitting" @click="submitWrite">提交本节（写作）</el-button>
            <span class="c6-sub">{{ writeWordCount }} 词</span>
          </div>
          <div v-if="writeResult" class="ex-ref">
            <div class="t-ref-title">参考范文</div>
            <div class="w-ref-en">{{ writeResult.referenceEssay }}</div>
          </div>
        </template>
      </div>

      <div class="ex-finish">
        <el-button type="success" size="large" :loading="finishing" @click="finishExam">完成模考并查看报告</el-button>
      </div>

      <div v-if="reportShown" class="c6-card ex-report">
        <h3 class="ex-report-title">估分报告（参考自测，非官方成绩）</h3>
        <div class="ex-report-row"><span>听力</span><b>{{ listenScore }} / 40</b></div>
        <div class="ex-report-row"><span>翻译</span><b>{{ transScore }} / 30</b></div>
        <div class="ex-report-row"><span>写作</span><b>{{ writeScore }} / 30</b></div>
        <div class="ex-report-total">预估总分 <b>{{ estTotal }}</b> / 100</div>
        <p class="c6-sub">说明：听力按正确率折算；翻译/写作按关键点覆盖率折算，仅为自测参考，不代表真实阅卷得分。</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { VideoPlay, Timer } from '@element-plus/icons-vue'
import { examApi, listeningApi, translationApi, writingApi } from '@/api'

const DURATION = 40 * 60 // 40 分钟
const loading = ref(false)
const finishing = ref(false)
const reportShown = ref(false)

const paper = ref(null)
const transItem = ref(null)
const writeItem = ref(null)

const listenChosen = reactive({})
const transAnswer = ref('')
const writeAnswer = ref('')

const listenResult = ref(null)
const transResult = ref(null)
const writeResult = ref(null)

const listenSubmitting = ref(false)
const transSubmitting = ref(false)
const writeSubmitting = ref(false)

const startedAt = ref(Date.now())
const remaining = ref(DURATION)
let timerId = null
const supported = typeof window !== 'undefined' && 'speechSynthesis' in window

const allListenAnswered = computed(() =>
  paper.value && paper.value.questions.every((q) => listenChosen[q.questionId] !== undefined)
)
const writeWordCount = computed(() => {
  const t = writeAnswer.value.trim()
  return t ? t.split(/\s+/).filter(Boolean).length : 0
})
const listenDetail = computed(() => {
  const m = {}
  if (listenResult.value && listenResult.value.details) listenResult.value.details.forEach((d) => { m[d.questionId] = d })
  return m
})

const listenScore = computed(() => (listenResult.value && listenResult.value.total ? Math.round(listenResult.value.correct / listenResult.value.total * 40) : 0))
const transScore = computed(() => (transResult.value ? Math.round(Number(transResult.value.coverage) / 100 * 30) : 0))
const writeScore = computed(() => (writeResult.value ? Math.round(Number(writeResult.value.coverage) / 100 * 30) : 0))
const estTotal = computed(() => listenScore.value + transScore.value + writeScore.value)

function fmtTime(s) {
  s = Math.max(0, s)
  const m = Math.floor(s / 60)
  const sec = s % 60
  return String(m).padStart(2, '0') + ':' + String(sec).padStart(2, '0')
}

async function prepare() {
  loading.value = true
  try {
    const res = await examApi.prepare()
    paper.value = res.data.listening || null
    transItem.value = res.data.translation || null
    writeItem.value = res.data.writing || null
    if (paper.value) paper.value.questions.forEach((q) => { listenChosen[q.questionId] = undefined })
  } finally {
    loading.value = false
  }
}

function play() {
  if (!supported || !paper.value) return
  window.speechSynthesis.cancel()
  const u = new SpeechSynthesisUtterance(paper.value.script)
  u.lang = 'en-US'
  u.rate = 0.9
  window.speechSynthesis.speak(u)
}
function stop() { if (supported) window.speechSynthesis.cancel() }

async function submitListen() {
  if (!paper.value) return
  listenSubmitting.value = true
  try {
    const answers = paper.value.questions.map((q) => ({ questionId: q.questionId, chosenIndex: listenChosen[q.questionId] }))
    const res = await listeningApi.submit({ id: paper.value.id, answers })
    listenResult.value = res.data
  } finally {
    listenSubmitting.value = false
  }
}
async function submitTrans() {
  if (!transItem.value) return
  transSubmitting.value = true
  try {
    const res = await translationApi.submit({ id: transItem.value.id, answer: transAnswer.value })
    transResult.value = res.data
  } finally {
    transSubmitting.value = false
  }
}
async function submitWrite() {
  if (!writeItem.value) return
  writeSubmitting.value = true
  try {
    const res = await writingApi.submit({ id: writeItem.value.id, answer: writeAnswer.value })
    writeResult.value = res.data
  } finally {
    writeSubmitting.value = false
  }
}
async function finishExam() {
  finishing.value = true
  try {
    const elapsed = Math.round((Date.now() - startedAt.value) / 1000)
    await examApi.finish({ durationSecond: elapsed })
    reportShown.value = true
  } finally {
    finishing.value = false
  }
}

onMounted(() => {
  prepare()
  timerId = setInterval(() => {
    remaining.value = Math.max(0, DURATION - Math.round((Date.now() - startedAt.value) / 1000))
    if (remaining.value <= 0 && timerId) { clearInterval(timerId); timerId = null }
  }, 1000)
})
onBeforeUnmount(() => {
  if (timerId) clearInterval(timerId)
  if (supported) window.speechSynthesis.cancel()
})
</script>

<style scoped>
.practice { max-width: 900px; margin: 0 auto; }
.practice-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; gap: 12px; }
.sec-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; color: var(--c6-text); }
.exam-timer { display: flex; align-items: center; gap: 6px; font-size: 20px; font-weight: 700; color: var(--c6-primary); font-variant-numeric: tabular-nums; }
.exam-timer.warn { color: var(--c6-danger, #8a7d7d); }
.ex-sec { padding: 20px 22px; margin-bottom: 16px; transition: opacity .2s; }
.ex-sec.locked { opacity: 0.92; }
.ex-sec-head { display: flex; align-items: center; gap: 12px; margin-bottom: 14px; }
.ex-no { font-weight: 700; color: var(--c6-primary); background: var(--c6-primary-soft); border-radius: 6px; padding: 2px 10px; font-size: 13px; }
.ex-name { font-size: 17px; font-weight: 600; color: var(--c6-text); }
.l-play { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; flex-wrap: wrap; }
.l-q { padding: 12px 0; border-top: 1px solid var(--c6-border-soft); }
.l-q-title { font-size: 15px; color: var(--c6-text); margin-bottom: 8px; }
.l-opts { display: flex; flex-direction: column; gap: 6px; }
.l-opt { white-space: normal; height: auto; margin-right: 0; }
.l-feedback { margin-top: 6px; font-size: 13px; }
.l-feedback.right { color: var(--c6-ok, #6f7a6f); }
.l-feedback.wrong { color: var(--c6-danger, #8a7d7d); }
.t-chinese { font-size: 16px; line-height: 1.9; color: var(--c6-text); background: var(--c6-primary-soft); padding: 14px 16px; border-radius: 10px; margin-bottom: 12px; }
.ex-actions { display: flex; align-items: center; gap: 14px; margin-top: 10px; }
.ex-ref { margin-top: 14px; border-top: 1px solid var(--c6-border-soft); padding-top: 12px; }
.t-ref-title { font-size: 13px; color: var(--c6-text-sub); margin-bottom: 6px; }
.t-ref-en { font-size: 15px; line-height: 1.9; color: var(--c6-text); white-space: pre-wrap; }
.w-prompt { font-size: 15px; line-height: 1.8; color: var(--c6-text); white-space: pre-wrap; margin-bottom: 10px; }
.ex-finish { text-align: center; margin: 8px 0 20px; }
.ex-report { padding: 22px; }
.ex-report-title { margin: 0 0 14px; font-size: 17px; color: var(--c6-text); }
.ex-report-row { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--c6-border-soft); font-size: 15px; color: var(--c6-text); }
.ex-report-total { text-align: right; font-size: 18px; margin-top: 12px; color: var(--c6-text); }
.ex-report-total b { color: var(--c6-primary); font-size: 24px; }
.ex-report .c6-sub { margin-top: 10px; display: block; }
</style>
