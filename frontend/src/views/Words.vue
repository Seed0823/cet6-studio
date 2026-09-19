<template>
  <div class="words-page">
    <!-- 工具条 -->
    <div class="c6-card toolbar">
      <el-radio-group v-model="mode" @change="load">
        <el-radio-button value="today">今日新词</el-radio-button>
        <el-radio-button value="review">待复习 · {{ progress.dueReview ?? 0 }}</el-radio-button>
      </el-radio-group>
      <div class="toolbar-right">
        <span class="c6-sub">
          已掌握 <b>{{ progress.known ?? 0 }}</b> · 已学 {{ progress.learned ?? 0 }} / 5407
        </span>
        <el-button size="small" :icon="Refresh" @click="load">换一组</el-button>
      </div>
    </div>

    <!-- 分组进度 -->
    <div v-if="list.length && !finished" class="c6-card word-card">
      <div class="progress-line">
        <span class="c6-sub">{{ index + 1 }} / {{ list.length }}</span>
        <div class="bar">
          <div class="bar-fill" :style="{ width: (index / list.length) * 100 + '%' }" />
        </div>
      </div>

      <div class="word-main">
        <div class="word-row">
          <h1 class="word">{{ current.word }}</h1>
          <el-button circle :icon="Headset" @click="speak(current.word)" />
          <el-tag v-if="current.isCet6 === 1" type="success" effect="light" size="small">六级</el-tag>
        </div>
        <div class="phonetic">{{ current.phonetic }}</div>
        <div v-if="current.pos" class="pos">{{ current.pos }}</div>
        <div class="translation">
          <div v-for="(line, i) in meaningLines(current.translation)" :key="i">{{ line }}</div>
        </div>
        <div v-if="current.exchange" class="exchange">变形：{{ current.exchange }}</div>
      </div>

      <div class="grade-actions">
        <el-button class="g g0" @click="grade(0)">不认识</el-button>
        <el-button class="g g1" @click="grade(1)">模糊</el-button>
        <el-button class="g g2" @click="grade(2)">认识</el-button>
      </div>
      <div class="hint c6-sub">快捷键：1 不认识 · 2 模糊 · 3 认识 · 空格 发音</div>
    </div>

    <!-- 空态 -->
    <div v-else-if="!loading && !list.length" class="c6-card empty-card">
      <el-empty :description="mode === 'review' ? '今天没有到期的复习词，去背新词吧' : '词库暂时取不到数据'" />
      <el-button type="primary" @click="$router.push('/quiz')">去自测</el-button>
    </div>

    <!-- 完成 -->
    <div v-else-if="finished" class="c6-card done-card">
      <div class="done-ring">
        <div class="done-num">{{ counts[2] }}</div>
        <div class="done-label">本组认识</div>
      </div>
      <h2>本组完成！</h2>
      <p class="c6-sub">
        共 {{ list.length }} 个词 · 认识 {{ counts[2] }} · 模糊 {{ counts[1] }} · 不认识 {{ counts[0] }}
      </p>
      <div class="done-actions">
        <el-button type="primary" @click="load">再来一组</el-button>
        <el-button @click="$router.push('/quiz')">去做自测</el-button>
        <el-button text @click="$router.push('/wrong')">看错词本</el-button>
      </div>
    </div>

    <div v-else class="c6-card loading-card" v-loading="true" />
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { Refresh, Headset } from '@element-plus/icons-vue'
import { wordApi } from '@/api'

const mode = ref('today')
const list = ref([])
const index = ref(0)
const loading = ref(true)
const finished = ref(false)
const counts = ref({ 0: 0, 1: 0, 2: 0 })
const progress = ref({})

let startAt = Date.now()

const current = computed(() => list.value[index.value] || {})

function meaningLines(t) {
  if (!t) return ['（暂无中文释义）']
  return String(t).split(/\r?\n/).filter((s) => s.trim())
}

/** 浏览器原生发音（Web Speech API），零依赖零成本 */
function speak(text) {
  if (!text || !window.speechSynthesis) return
  const utter = new SpeechSynthesisUtterance(text)
  utter.lang = 'en-US'
  utter.rate = 0.9
  window.speechSynthesis.cancel()
  window.speechSynthesis.speak(utter)
}

async function load() {
  loading.value = true
  finished.value = false
  index.value = 0
  counts.value = { 0: 0, 1: 0, 2: 0 }
  startAt = Date.now()
  try {
    const [wordsRes, progRes] = await Promise.all([
      mode.value === 'review' ? wordApi.review(20) : wordApi.today(20),
      wordApi.progress()
    ])
    list.value = wordsRes.data || []
    progress.value = progRes.data || {}
  } finally {
    loading.value = false
  }
}

async function grade(g) {
  const w = current.value
  if (!w.id) return
  counts.value[g] = (counts.value[g] || 0) + 1

  const isLast = index.value + 1 >= list.value.length
  // 本组最后一张卡时，把这一组的学习时长一并上报
  const durationMin = isLast ? Math.max(1, Math.round((Date.now() - startAt) / 60000)) : 0

  try {
    await wordApi.grade({
      wordId: w.id,
      grade: g,
      durationMin,
      fromReview: mode.value === 'review'
    })
  } catch (e) {
    /* 提示已由拦截器处理，不阻塞继续学下一个 */
  }

  if (isLast) {
    finished.value = true
  } else {
    index.value++
  }
}

function onKey(e) {
  if (finished.value || !list.value.length) return
  if (e.key === '1') grade(0)
  else if (e.key === '2') grade(1)
  else if (e.key === '3') grade(2)
  else if (e.code === 'Space') {
    e.preventDefault()
    speak(current.value.word)
  }
}

onMounted(() => {
  load()
  window.addEventListener('keydown', onKey)
})
onBeforeUnmount(() => window.removeEventListener('keydown', onKey))
</script>

<style scoped>
.words-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.word-card {
  min-height: 380px;
  display: flex;
  flex-direction: column;
}

.loading-card {
  height: 380px;
}

.progress-line {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 8px;
}

.progress-line .bar {
  flex: 1;
  height: 5px;
  border-radius: 3px;
  background: var(--c6-track);
  overflow: hidden;
}

.progress-line .bar-fill {
  height: 100%;
  background: var(--c6-primary-light);
  transition: width 0.3s;
}

.word-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 20px 0;
}

.word-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.word {
  font-size: 42px;
  margin: 0;
  letter-spacing: 0.5px;
  font-family: var(--c6-serif);
}

.phonetic {
  color: var(--c6-text-sub);
  font-size: 15px;
  margin-top: 8px;
}

.pos {
  color: var(--c6-primary);
  font-size: 13px;
  margin-top: 6px;
}

.translation {
  margin-top: 18px;
  font-size: 17px;
  line-height: 1.9;
  color: #3a3226;
  max-width: 620px;
}

.exchange {
  margin-top: 14px;
  font-size: 12px;
  color: var(--c6-text-sub);
}

.grade-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-top: 8px;
}

.grade-actions .g {
  width: 140px;
  height: 46px;
  font-size: 15px;
}

.g0 {
  border-color: #f0c9c7;
  color: #c0504d;
}
.g0:hover {
  background: #fdf3f2;
  border-color: #c0504d;
  color: #c0504d;
}

.g1 {
  border-color: #f0e0b8;
  color: #b98a0e;
}
.g1:hover {
  background: #fdf9f0;
  border-color: #b98a0e;
  color: #b98a0e;
}

.g2 {
  border-color: #dcc79c;
  color: var(--c6-primary);
}
.g2:hover {
  background: #fdf6e8;
  border-color: var(--c6-primary);
  color: var(--c6-primary);
}

.hint {
  text-align: center;
  margin-top: 14px;
}

.empty-card,
.done-card {
  min-height: 380px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.done-ring {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  border: 8px solid var(--c6-track);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
}

.done-num {
  font-size: 34px;
  font-weight: 800;
  color: var(--c6-primary);
  font-family: var(--c6-num);
  font-variant-numeric: lining-nums tabular-nums;
}

.done-label {
  font-size: 12px;
  color: var(--c6-text-sub);
}

.done-actions {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
