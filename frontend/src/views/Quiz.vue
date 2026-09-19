<template>
  <div class="quiz-page">
    <!-- ① 准备 -->
    <div v-if="stage === 'idle'" class="c6-card start-card">
      <div class="start-icon">✎</div>
      <h2>单词自测</h2>
      <p class="c6-sub">
        从 5407 个六级词中随机抽题，检验掌握程度。<br />
        答错的词会自动进入错词本，题目里的生词可以直接点击查询。
      </p>
      <div class="count-pick">
        <span class="c6-sub">题目数量</span>
        <el-radio-group v-model="count">
          <el-radio-button :value="5">5 题</el-radio-button>
          <el-radio-button :value="10">10 题</el-radio-button>
          <el-radio-button :value="20">20 题</el-radio-button>
        </el-radio-group>
      </div>
      <el-button type="primary" size="large" :loading="loading" @click="start">开始自测</el-button>

      <div v-if="history.length" class="history">
        <div class="c6-sub history-title">最近记录</div>
        <div v-for="h in history" :key="h.id" class="history-row">
          <span>{{ formatTime(h.createTime) }}</span>
          <span>{{ h.correct }}/{{ h.total }}</span>
          <span :class="h.accuracy >= 80 ? 'ok' : h.accuracy >= 60 ? 'mid' : 'bad'">
            {{ h.accuracy }}%
          </span>
        </div>
      </div>
    </div>

    <!-- ② 答题 -->
    <div v-else-if="stage === 'doing'" class="c6-card quiz-card">
      <div class="quiz-head">
        <span class="c6-sub">第 {{ index + 1 }} / {{ questions.length }} 题</span>
        <el-progress
          :percentage="Math.round((index / questions.length) * 100)"
          :show-text="false"
          style="flex: 1; margin: 0 16px"
        />
        <span class="c6-sub">{{ elapsedText }}</span>
      </div>

      <div class="question">
        <ClickableText :text="current.question" />
      </div>
      <div class="c6-sub tip">题干里的英文单词可以点击直接查释义</div>

      <div class="options">
        <div
          v-for="(opt, i) in current.options"
          :key="i"
          class="option"
          :class="{ selected: answers[current.wordId] === i }"
          @click="choose(i)"
        >
          <span class="opt-idx" :class="{ active: answers[current.wordId] === i }">{{ 'ABCD'[i] }}</span>
          <span class="opt-text">{{ opt }}</span>
        </div>
      </div>

      <div class="quiz-actions">
        <el-button :disabled="index === 0" @click="index--">上一题</el-button>
        <el-button v-if="index < questions.length - 1" type="primary" @click="index++">下一题</el-button>
        <el-button v-else type="primary" :loading="submitting" @click="submit">提交答卷</el-button>
        <el-button text @click="reset">放弃</el-button>
      </div>
      <div class="c6-sub tip">已作答 {{ answeredCount }} / {{ questions.length }}</div>
    </div>

    <!-- ③ 结果 -->
    <div v-else class="result">
      <div class="c6-card result-head">
        <div class="score-ring">
          <div class="score-num">{{ result.correct }}/{{ result.total }}</div>
          <div class="score-label">正确率 {{ result.accuracy }}%</div>
        </div>
        <div class="result-text">
          <h2>{{ resultTitle }}</h2>
          <p class="c6-sub">用时 {{ Math.round((result.costSecond || 0) / 60) }} 分 {{ (result.costSecond || 0) % 60 }} 秒</p>
          <div class="result-actions">
            <el-button type="primary" @click="start">再来一组</el-button>
            <el-button @click="$router.push('/wrong')">看错词本</el-button>
          </div>
        </div>
      </div>

      <div class="c6-card">
        <h3 class="c6-title">逐题解析</h3>
        <div v-for="(d, i) in result.details" :key="i" class="detail" :class="{ wrong: !d.right }">
          <div class="detail-head">
            <span class="mark" :class="d.right ? 'ok' : 'bad'">{{ d.right ? '✓' : '✗' }}</span>
            <ClickableText :text="d.question" class="detail-q" />
          </div>
          <div class="detail-line">
            正确释义：<b>{{ d.correctTranslation }}</b>
            <span v-if="d.phonetic" class="c6-sub">{{ d.phonetic }}</span>
          </div>
          <div v-if="!d.right" class="detail-line c6-sub">
            你的选择：{{ d.chosenIndex >= 0 ? d.options[d.chosenIndex] : '未作答' }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import ClickableText from '@/components/ClickableText.vue'
import { quizApi } from '@/api'

const stage = ref('idle')
const count = ref(10)
const loading = ref(false)
const submitting = ref(false)
const questions = ref([])
const index = ref(0)
const answers = reactive({})
const result = ref(null)
const history = ref([])

let startedAt = 0
const now = ref(Date.now())
let timer = null

const current = computed(() => questions.value[index.value] || {})
const answeredCount = computed(() => Object.keys(answers).length)
const elapsedText = computed(() => {
  const s = Math.floor((now.value - startedAt) / 1000)
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`
})
const resultTitle = computed(() => {
  const a = Number(result.value?.accuracy || 0)
  if (a >= 90) return '非常棒，这套题几乎全对'
  if (a >= 75) return '不错，继续保持'
  if (a >= 60) return '及格线上下，错词记得复习'
  return '错得不少，去错词本过一遍'
})

async function start() {
  loading.value = true
  try {
    const res = await quizApi.generate(count.value)
    questions.value = res.data || []
    if (!questions.value.length) {
      ElMessage.warning('题目生成失败，请重试')
      return
    }
    Object.keys(answers).forEach((k) => delete answers[k])
    index.value = 0
    result.value = null
    startedAt = Date.now()
    now.value = Date.now()
    stage.value = 'doing'
    if (timer) clearInterval(timer)
    timer = setInterval(() => (now.value = Date.now()), 1000)
  } finally {
    loading.value = false
  }
}

function choose(i) {
  answers[current.value.wordId] = i
}

async function submit() {
  if (answeredCount.value < questions.value.length) {
    ElMessage.warning(`还有 ${questions.value.length - answeredCount.value} 题未作答`)
    return
  }
  submitting.value = true
  try {
    const payload = {
      costSecond: Math.floor((Date.now() - startedAt) / 1000),
      answers: questions.value.map((q) => ({
        wordId: q.wordId,
        question: q.question,
        options: q.options,
        chosenIndex: answers[q.wordId]
      }))
    }
    const res = await quizApi.submit(payload)
    result.value = res.data
    stage.value = 'result'
    if (timer) clearInterval(timer)
    loadHistory()
  } finally {
    submitting.value = false
  }
}

function reset() {
  stage.value = 'idle'
  result.value = null
  questions.value = []
  Object.keys(answers).forEach((k) => delete answers[k])
  if (timer) clearInterval(timer)
}

async function loadHistory() {
  try {
    const res = await quizApi.history(5)
    history.value = res.data || []
  } catch (e) {
    /* ignore */
  }
}

function formatTime(t) {
  return t ? String(t).slice(5, 16).replace('T', ' ') : ''
}

onMounted(loadHistory)
onBeforeUnmount(() => timer && clearInterval(timer))
</script>

<style scoped>
.quiz-page {
  max-width: 860px;
  margin: 0 auto;
}

.start-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 46px 24px;
  text-align: center;
}

.start-icon {
  width: 64px;
  height: 64px;
  border-radius: 18px;
  background: #fdf6e8;
  color: var(--c6-primary);
  font-size: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.start-card h2 {
  margin: 0 0 10px;
  font-size: 22px;
}

.start-card .c6-sub {
  line-height: 1.9;
}

.count-pick {
  display: flex;
  align-items: center;
  gap: 14px;
  margin: 24px 0;
}

.history {
  width: 100%;
  max-width: 380px;
  margin-top: 34px;
  text-align: left;
}

.history-title {
  margin-bottom: 8px;
}

.history-row {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  padding: 7px 0;
  border-bottom: 1px dashed var(--c6-border-soft);
  color: #6b5f4c;
}

.ok {
  color: var(--c6-primary);
}
.mid {
  color: #b98a0e;
}
.bad {
  color: #c0504d;
}

.quiz-card {
  min-height: 440px;
  display: flex;
  flex-direction: column;
}

.quiz-head {
  display: flex;
  align-items: center;
}

.question {
  font-size: 21px;
  font-weight: 600;
  margin: 36px 0 8px;
  line-height: 1.7;
  text-align: center;
}

.tip {
  text-align: center;
}

.options {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin: 26px auto 0;
  width: 100%;
  max-width: 560px;
}

.option {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 15px 18px;
  border: 1px solid var(--c6-border);
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.15s;
  font-size: 14px;
}

.option:hover {
  border-color: var(--c6-primary-light);
  background: #fdf9f0;
}

.option.selected {
  border-color: var(--c6-primary);
  background: #fdf6e8;
}

.opt-idx {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--c6-track);
  color: var(--c6-text-sub);
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.opt-idx.active {
  background: var(--c6-primary);
  color: #fff;
}

.quiz-actions {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: auto;
  padding-top: 30px;
}

.result {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-head {
  display: flex;
  align-items: center;
  gap: 30px;
  padding: 26px 30px;
}

.score-ring {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 8px solid var(--c6-track);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.score-num {
  font-size: 24px;
  font-weight: 800;
  color: var(--c6-primary);
  font-family: var(--c6-num);
  font-variant-numeric: lining-nums tabular-nums;
}

.score-label {
  font-size: 11px;
  color: var(--c6-text-sub);
}

.result-text h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.result-actions {
  margin-top: 16px;
  display: flex;
  gap: 10px;
}

.detail {
  padding: 14px 0;
  border-bottom: 1px dashed var(--c6-border-soft);
}

.detail:last-child {
  border-bottom: none;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  font-weight: 600;
}

.detail-q {
  flex: 1;
}

.mark {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-top: 3px;
}

.mark.ok {
  background: var(--c6-primary-light);
}

.mark.bad {
  background: #e0a3a0;
}

.detail-line {
  margin-left: 30px;
  font-size: 13px;
  margin-top: 6px;
  color: #6b5f4c;
}
</style>
