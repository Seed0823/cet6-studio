<template>
  <div class="reader">
    <!-- 工具栏 -->
    <div class="c6-card toolbar">
      <el-button link class="back" @click="back">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>

      <div class="tb-title">
        <div class="tb-en">{{ article?.title || '加载中…' }}</div>
        <div class="tb-cn">{{ article?.titleCn }}</div>
      </div>

      <div class="tb-right">
        <span class="tb-chip cat" :style="catStyle">{{ catLabel }}</span>
        <span class="tb-chip">{{ article?.wordCount }} 词</span>
        <span class="tb-chip diff">{{ stars(article?.difficulty) }}</span>
        <span class="tb-chip timer">
          <el-icon><Timer /></el-icon>
          {{ mmss(elapsed) }}
        </span>
      </div>
    </div>

    <!-- 控制条 -->
    <div class="c6-card controls">
      <el-switch v-model="showTranslation" active-text="译文对照" />
      <el-switch v-model="showCet6" active-text="六级词高亮" />
      <span v-if="article?.cet6Words?.length" class="cet6-hint">
        本篇含 {{ article.cet6Words.length }} 个六级词
      </span>

      <div class="font-ctl">
        <el-button size="small" :disabled="fontSize <= 14" @click="fontSize--">A－</el-button>
        <span class="font-val">{{ fontSize }}px</span>
        <el-button size="small" :disabled="fontSize >= 24" @click="fontSize++">A＋</el-button>
      </div>

      <div class="ctrl-right">
        <span v-if="savedProgress > 0 && savedProgress < 100" class="resume-tip">
          上次读到 {{ savedProgress }}%
        </span>
        <span class="prog-text">本次已读 {{ progress }}%</span>
      </div>
    </div>

    <!-- 正文 -->
    <div
      ref="bodyRef"
      v-loading="loading"
      class="c6-card body"
      :style="{ fontSize: fontSize + 'px' }"
      @scroll="onScroll"
    >
      <div class="head-note">
        <span>{{ article?.source }}</span>
        <a v-if="article?.sourceUrl" :href="article.sourceUrl" target="_blank" rel="noreferrer">
          查看原文 ↗
        </a>
      </div>

      <div v-for="(p, i) in paras" :key="i" class="para">
        <ClickableText :text="p" :cet6-list="showCet6 ? cet6Words : []" />
        <div v-if="showTranslation && trans[i]" class="para-cn">{{ trans[i] }}</div>
      </div>

      <div class="body-foot">
        <el-button type="primary" :disabled="progress >= 100" @click="markDone">
          标记为已读完
        </el-button>
        <el-button @click="back">返回列表</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { readingApi } from '@/api'
import { useTheme } from '@/composables/theme'
import ClickableText from '@/components/ClickableText.vue'

const { theme } = useTheme()
const isMinimal = computed(() => theme.value === 'minimal')

const route = useRoute()
const router = useRouter()
const articleId = Number(route.params.id)

const CATEGORIES = {
  exam: { label: '真题仿真', color: '#854f0b', bg: '#faeeda' },
  essay: { label: '公版美文', color: '#993c1d', bg: '#faece7' },
  news: { label: '外刊新闻', color: '#3b6d11', bg: '#eaf3de' },
  science: { label: '科普短文', color: '#72243e', bg: '#fbeaf0' }
}

const article = ref(null)
const loading = ref(true)
const showTranslation = ref(false)
const showCet6 = ref(true)
const fontSize = ref(17)

/** 本次会话的滚动进度 0-100 */
const progress = ref(0)
/** 服务端已保存的进度，用于提示「上次读到」 */
const savedProgress = ref(0)
/** 计时器累计的本页停留秒数（展示用） */
const elapsed = ref(0)

const bodyRef = ref(null)

/** 自上次上报以来累计的、尚未提交的秒数 */
let pendingSeconds = 0
let tickTimer = null
let scrollTimer = null

const catLabel = computed(() => CATEGORIES[article.value?.category]?.label || '')
const catStyle = computed(() => {
  // 极简主题：分类标签走中性细描边，不用彩色块（交给 CSS 处理）
  if (isMinimal.value) return {}
  const c = CATEGORIES[article.value?.category]
  return c ? { color: c.color, background: c.bg } : {}
})
const cet6Words = computed(() => article.value?.cet6Words || [])

/**
 * 正文按空行切段
 * <p>
 * 后端存的是「段落以空行分隔」，切成数组后才能逐段渲染，
 * 也才能把译文精确插到对应段落下面 —— 整篇并排对照会迫使读者
 * 在两栏之间来回找位置，逐段插入则想不看就跳过。
 */
function splitParas(text) {
  if (!text) return []
  return text
    .split(/\n\s*\n/)
    .map((s) => s.trim())
    .filter(Boolean)
}

const paras = computed(() => splitParas(article.value?.content))
const trans = computed(() => splitParas(article.value?.translation))

function stars(d) {
  const n = Math.min(Math.max(d || 0, 0), 5)
  return '★'.repeat(n)
}

function mmss(sec) {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

/** 上报进度与新增时长；addSeconds 为 0 且非强制时跳过，避免无意义的空请求 */
async function flush(force = false) {
  const add = pendingSeconds
  if (!force && add <= 0) return
  pendingSeconds = 0
  try {
    await readingApi.progress({
      articleId,
      progress: progress.value,
      addSeconds: add
    })
    if (progress.value > savedProgress.value) savedProgress.value = progress.value
  } catch (e) {
    // 上报失败把秒数还回去，下次一起提交，避免阅读时长凭空丢失
    pendingSeconds += add
  }
}

function onScroll() {
  const el = bodyRef.value
  if (el) {
    const max = el.scrollHeight - el.clientHeight
    const p = max > 0 ? Math.round((el.scrollTop / max) * 100) : 100
    progress.value = Math.min(100, Math.max(0, p))
  }
  // 滚动停止 600ms 后再上报：滚动过程中每帧上报会打爆接口
  clearTimeout(scrollTimer)
  scrollTimer = setTimeout(() => flush(), 600)
}

async function markDone() {
  progress.value = 100
  await flush(true)
  ElMessage.success('已标记为读完')
}

async function load() {
  loading.value = true
  try {
    const res = await readingApi.detail(articleId)
    article.value = res.data
    savedProgress.value = res.data?.progress || 0
    progress.value = savedProgress.value
    // 记录一次打开（放在详情之后：详情失败就没必要计入打开次数）
    readingApi.open(articleId).catch(() => {})
  } finally {
    loading.value = false
  }
}

function back() {
  router.push('/reading')
}

onMounted(() => {
  load()
  // 每秒累计，满 10 秒提交一次 —— 间隔太短会频繁写库，太长则意外关闭页面时丢时长
  tickTimer = setInterval(() => {
    elapsed.value++
    pendingSeconds++
    if (pendingSeconds >= 10) flush()
  }, 1000)
})

onBeforeUnmount(() => {
  clearInterval(tickTimer)
  clearTimeout(scrollTimer)
  // 离开时把剩余秒数与最终进度补交
  flush(true)
})
</script>

<style scoped>
.reader {
  display: flex;
  flex-direction: column;
  gap: 14px;
  height: 100%;
}

.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 18px;
}

.back {
  flex-shrink: 0;
}

.tb-title {
  flex: 1;
  min-width: 0;
}

.tb-en {
  font-family: var(--c6-serif);
  font-size: 15.5px;
  font-weight: 700;
  color: var(--c6-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tb-cn {
  font-size: 12px;
  color: var(--c6-text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.tb-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.tb-chip {
  font-size: 11.5px;
  color: var(--c6-text-sub);
  background: var(--c6-fill);
  padding: 3px 9px;
  border-radius: 6px;
  white-space: nowrap;
}

.tb-chip.cat {
  font-weight: 600;
}

.tb-chip.diff {
  color: #c9a24a;
  letter-spacing: 1px;
}

.tb-chip.timer {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--c6-primary);
  font-family: var(--c6-num);
  font-variant-numeric: lining-nums tabular-nums;
}

.controls {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 10px 18px;
  flex-wrap: wrap;
}

.cet6-hint {
  font-size: 12px;
  color: #a2661a;
}

.font-ctl {
  display: flex;
  align-items: center;
  gap: 6px;
}

.font-val {
  font-size: 12px;
  color: var(--c6-text-sub);
  min-width: 38px;
  text-align: center;
}

.ctrl-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 14px;
}

.resume-tip {
  font-size: 12px;
  color: #a2661a;
}

.prog-text {
  font-size: 12px;
  color: var(--c6-text-sub);
}

.body {
  flex: 1;
  overflow-y: auto;
  padding: 26px 40px 40px;
  scroll-behavior: smooth;
}

.head-note {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--c6-text-sub);
  padding-bottom: 14px;
  margin-bottom: 20px;
  border-bottom: 1px dashed var(--c6-border);
}

.head-note a {
  color: var(--c6-primary);
  text-decoration: none;
}

.head-note a:hover {
  text-decoration: underline;
}

.para {
  font-family: var(--c6-serif);
  line-height: 1.95;
  margin-bottom: 20px;
  color: #3a3226;
  letter-spacing: 0.1px;
}

.para-cn {
  font-family: var(--c6-sans);
  margin-top: 10px;
  padding-left: 12px;
  border-left: 3px solid #eadfc9;
  color: #7d7059;
  font-size: 0.86em;
  line-height: 1.85;
}

.body-foot {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding-top: 24px;
  margin-top: 12px;
  border-top: 1px dashed var(--c6-border);
}

/* ============================================================
 * 极简黑白主题下的阅读器覆写
 * 目标：纯黑/米白、低对比、无衬线、宽松行高，六级词只留细灰下划线。
 * ============================================================ */
[data-theme='minimal'] .tb-chip.cat {
  color: #6b6b6b;
  background: #f1efea;
  border: 1px solid #e5e2db;
}

[data-theme='minimal'] .tb-chip.diff {
  color: #b5b0a6;
}

[data-theme='minimal'] .cet6-hint,
[data-theme='minimal'] .resume-tip {
  color: #8a8a8a;
}

[data-theme='minimal'] .para {
  color: #33322f;
  line-height: 2.05;
  letter-spacing: 0.2px;
}

[data-theme='minimal'] .para-cn {
  border-left-color: #e5e2db;
  color: #86807a;
}
</style>
