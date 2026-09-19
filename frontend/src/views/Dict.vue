<template>
  <div class="dict-page">
    <!-- 搜索 -->
    <div class="c6-card search-card">
      <h2>查词</h2>
      <p class="c6-sub">
        输入前几个字母就会联想出候选词，选中即查，不用把单词拼完整。
        每次查询都会被记录，日积月累形成你的个人热词榜——查得最多的词，往往就是你最没记住的词。
      </p>
      <el-autocomplete
        v-model="keyword"
        size="large"
        class="search-input"
        placeholder="输入前几个字母，例如 alle → alleviate"
        clearable
        value-key="word"
        :debounce="220"
        :trigger-on-focus="true"
        :select-when-unmatched="true"
        :fetch-suggestions="fetchSuggestions"
        popper-class="dict-suggest-popper"
        @select="onSelect"
        @keyup.enter="searchWord(keyword)"
      >
        <template #default="{ item }">
          <div class="sg-row">
            <span class="sg-word">{{ item.word }}</span>
            <span v-if="item.phonetic && !item.fromRecent" class="sg-phonetic">{{ item.phonetic }}</span>
            <el-tag v-if="item.cet6" size="small" type="success" effect="plain">六级</el-tag>
            <span v-if="item.fromRecent" class="sg-recent-tag">最近</span>
            <span class="sg-brief">{{ item.brief }}</span>
          </div>
        </template>
        <template #append>
          <el-button :icon="Search" @click="searchWord(keyword)">查询</el-button>
        </template>
      </el-autocomplete>

      <div class="quick">
        <span class="c6-sub">试试：</span>
        <el-tag
          v-for="w in QUICK_WORDS"
          :key="w"
          class="qtag"
          effect="plain"
          @click="searchWord(w)"
        >
          {{ w }}
        </el-tag>
      </div>
    </div>

    <!-- 查询结果 -->
    <div v-if="result" class="c6-card result-card">
      <div class="res-head">
        <h1 class="res-word">{{ result.word }}</h1>
        <span v-if="result.phonetic" class="res-phonetic">{{ result.phonetic }}</span>
        <el-button
          circle
          :icon="Headset"
          size="small"
          :title="result.external?.audioUrl
            ? '播放真人发音（' + (result.external.pronSource || '在线源') + '）'
            : '浏览器语音合成发音'"
          @click="speak(result.word)"
        />
        <el-tag v-if="result.cet6" type="success" effect="light" size="small">六级大纲词</el-tag>
        <div class="res-right">
          <span class="c6-sub">已查 {{ result.queryCount || 0 }} 次</span>
        </div>
      </div>

      <template v-if="result.exactMatch">
        <div v-if="result.pos" class="res-pos">{{ result.pos }}</div>
        <div class="res-meaning">
          <div v-for="(line, i) in meaningLines" :key="i">{{ line }}</div>
        </div>
        <div v-if="result.definition" class="res-def">
          <span class="c6-sub">英文释义：</span>{{ result.definition }}
        </div>
        <div v-if="result.exchange" class="res-ex">词形变化：{{ result.exchange }}</div>
        <div v-if="result.tag" class="res-tag c6-sub">考试标签：{{ result.tag }}</div>

        <!-- 在线源补充的英文释义：本地词库的 definition 往往只有一行，这里能看到多个义项 -->
        <div v-if="externalDefs.length" class="res-ext">
          <div class="ext-head">
            <span class="c6-sub">在线释义</span>
            <el-tag v-if="result.external?.source" size="small" effect="plain">
              {{ result.external.source }}
            </el-tag>
            <span v-if="result.external?.pos" class="ext-pos">{{ result.external.pos }}</span>
          </div>
          <ol class="ext-list">
            <li v-for="(d, i) in externalDefs" :key="i">{{ d }}</li>
          </ol>
        </div>

        <div class="res-actions">
          <el-button
            :type="result.inWrongBook ? 'info' : 'primary'"
            :disabled="result.inWrongBook"
            @click="addWrong"
          >
            {{ result.inWrongBook ? '已在错词本' : '+ 加入错词本' }}
          </el-button>
          <span class="c6-sub">查得多的词建议加进错词本，反复复习</span>
        </div>
      </template>

      <template v-else>
        <!-- 本地词库没收录，但在线源可能有：有内容就不显示「未收录」的空白态 -->
        <template v-if="externalDefs.length || result.external?.phonetic">
          <div class="res-ext">
            <div class="ext-head">
              <span class="c6-sub">本地词库未收录，以下内容来自在线源</span>
              <el-tag v-if="result.external?.source" size="small" effect="plain">
                {{ result.external.source }}
              </el-tag>
              <span v-if="result.external?.phonetic" class="ext-pos">{{ result.external.phonetic }}</span>
              <span v-if="result.external?.pos" class="ext-pos">{{ result.external.pos }}</span>
            </div>
            <ol class="ext-list">
              <li v-for="(d, i) in externalDefs" :key="i">{{ d }}</li>
            </ol>
          </div>
        </template>
        <el-empty v-else description="词库中未收录该词" :image-size="70" />
        <div v-if="result.suggestions?.length" class="suggest">
          你是不是想查：
          <el-tag
            v-for="s in result.suggestions"
            :key="s"
            size="small"
            class="qtag"
            @click="searchWord(s)"
          >
            {{ s }}
          </el-tag>
        </div>
      </template>
    </div>

    <!-- 统计条 -->
    <div class="stats-strip">
      <div class="c6-card strip-item">
        <div class="c6-num">{{ dictStats.distinctWords ?? 0 }}</div>
        <div class="c6-sub">查过的词（去重）</div>
      </div>
      <div class="c6-card strip-item">
        <div class="c6-num">{{ dictStats.totalTimes ?? 0 }}</div>
        <div class="c6-sub">累计查词次数</div>
      </div>
      <div class="c6-card strip-item">
        <div class="c6-num word-num">{{ dictStats.topWord || '—' }}</div>
        <div class="c6-sub">查得最多（{{ dictStats.topWordCount || 0 }} 次）</div>
      </div>
    </div>

    <!-- 热词榜 + 最近查询 -->
    <div class="grid">
      <div class="c6-card">
        <div class="card-head">
          <h3 class="c6-title">热词榜</h3>
          <span class="c6-sub">按查询次数从高到低</span>
        </div>
        <div v-if="!hot.length" class="empty">还没有查询记录，去查几个词吧</div>
        <div v-for="(item, i) in hot" :key="item.id" class="hot-row">
          <span class="rank" :class="{ top3: i < 3 }">{{ i + 1 }}</span>
          <span class="hot-word" @click="searchWord(item.word)">{{ item.word }}</span>
          <div class="hot-bar">
            <div class="hot-bar-fill" :style="{ width: hotBarWidth(item.queryCount) }" />
          </div>
          <span class="hot-count">{{ item.queryCount }} 次</span>
        </div>
      </div>

      <div class="c6-card">
        <div class="card-head">
          <h3 class="c6-title">最近查询</h3>
          <span class="c6-sub">按时间倒序</span>
        </div>
        <div v-if="!recent.length" class="empty">暂无记录</div>
        <div v-for="item in recent" :key="item.id" class="recent-row">
          <span class="recent-word" @click="searchWord(item.word)">{{ item.word }}</span>
          <span class="c6-sub">{{ item.queryCount }} 次 · {{ formatTime(item.lastTime) }}</span>
        </div>
      </div>
    </div>

    <!-- 查词热度说明 -->
    <div class="c6-card note">
      <h3 class="c6-title">这个榜单怎么用？</h3>
      <ul class="note-list">
        <li>查得次数多的词，说明你反复遇到它却还没记住 —— 优先加进错词本反复过。</li>
        <li>做题（自测）时直接点击题干里的英文单词，也会查词并计入这里的统计。</li>
        <li>数据都在你自己的账号下，换设备登录依然保留。</li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Headset } from '@element-plus/icons-vue'
import { dictApi } from '@/api'

const QUICK_WORDS = ['alleviate', 'comprehensive', 'ambiguous', 'controversy']

const keyword = ref('')
const result = ref(null)
const hot = ref([])
const recent = ref([])
const dictStats = ref({})

/**
 * 最近一次查询，用于短时间去重。
 * el-autocomplete 选中候选时触发 select，紧接着 keyup.enter 也会走到 searchWord，
 * 不去重的话一次操作会被记两次查询次数，热词榜的排序就失真了。
 */
let lastLookup = { word: '', at: 0 }

const meaningLines = computed(() => {
  const t = result.value?.translation
  if (!t) return ['（暂无中文释义）']
  return String(t).split(/\r?\n/).filter((s) => s.trim())
})

/** 在线源的英文释义（本地词库的 definition 往往只有一行，在线源给多个义项） */
const externalDefs = computed(() => result.value?.external?.definitions || [])

const maxCount = computed(() => Math.max(...hot.value.map((h) => h.queryCount || 0), 1))

function hotBarWidth(count) {
  return Math.max(4, Math.round(((count || 0) / maxCount.value) * 100)) + '%'
}

/**
 * 发音：优先用在线源返回的真实录音（有道 mp3），取不到再回退浏览器语音合成。
 * 回退很重要——不同设备/浏览器装的英文语音包差异很大，甚至可能没声音。
 */
function speak(text) {
  const url = result.value?.external?.audioUrl
  if (url) {
    try {
      const audio = new Audio(url)
      audio.play().catch(() => tts(text))
      return
    } catch (e) {
      // 落到下面的 TTS
    }
  }
  tts(text)
}

function tts(text) {
  if (!text || !window.speechSynthesis) return
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'en-US'
  u.rate = 0.9
  window.speechSynthesis.cancel()
  window.speechSynthesis.speak(u)
}

/**
 * 补全候选有两个来源：
 * - 有输入 → 调后端前缀联想（走 uk_word 唯一索引，毫秒级）
 * - 空输入（刚聚焦输入框）→ 回退成「最近查过的词」，省去重新回忆要查什么
 */
async function fetchSuggestions(query, cb) {
  const q = String(query || '').trim()
  if (!q) {
    cb(recent.value.slice(0, 8).map((r) => ({ word: r.word, brief: '', fromRecent: true })))
    return
  }
  try {
    const res = await dictApi.suggest(q, 10)
    cb(res.data || [])
  } catch (e) {
    // 联想失败不该打断查词主流程，静默降级成「无候选」
    cb([])
  }
}

/** 选中候选。输入未匹配任何词时按回车，也会以 { value } 形式走到这里 */
function onSelect(item) {
  const w = String(item?.word || item?.value || '').trim()
  if (w) searchWord(w)
}

async function searchWord(word) {
  const w = String(word || '').trim()
  if (!w) {
    ElMessage.warning('请输入要查询的单词')
    return
  }
  const now = Date.now()
  if (lastLookup.word === w && now - lastLookup.at < 400) return
  lastLookup = { word: w, at: now }

  keyword.value = w
  const res = await dictApi.lookup(w)
  result.value = res.data
  await loadSide()
}

async function addWrong() {
  if (!result.value?.wordId) return
  await dictApi.addWrong(result.value.wordId)
  result.value.inWrongBook = true
  ElMessage.success('已加入错词本')
}

async function loadSide() {
  const [hotRes, recentRes, statsRes] = await Promise.all([
    dictApi.hot(20),
    dictApi.recent(10),
    dictApi.stats()
  ])
  hot.value = hotRes.data || []
  recent.value = recentRes.data || []
  dictStats.value = statsRes.data || {}
}

function formatTime(t) {
  return t ? String(t).slice(5, 16).replace('T', ' ') : ''
}

onMounted(loadSide)
</script>

<style scoped>
.dict-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1040px;
  margin: 0 auto;
}

.search-card h2 {
  margin: 0 0 8px;
  font-size: 20px;
}

.search-card .c6-sub {
  line-height: 1.8;
  display: block;
  margin-bottom: 18px;
}

.search-input {
  max-width: 620px;
}

.quick {
  margin-top: 14px;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.qtag {
  cursor: pointer;
}

.result-card {
  border-left: 4px solid var(--c6-primary-light);
}

.res-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.res-word {
  font-size: 32px;
  margin: 0;
  font-family: var(--c6-serif);
}

.res-phonetic {
  color: var(--c6-text-sub);
  font-size: 14px;
}

.res-right {
  margin-left: auto;
}

.res-pos {
  color: var(--c6-primary);
  font-size: 13px;
  margin-top: 14px;
}

.res-meaning {
  font-size: 16px;
  line-height: 1.9;
  margin-top: 8px;
  color: #3a3226;
}

.res-def {
  margin-top: 12px;
  font-size: 13px;
  color: #6b5f4c;
  line-height: 1.7;
}

.res-ex,
.res-tag {
  margin-top: 8px;
  font-size: 12px;
  color: var(--c6-text-sub);
}

/* 在线源补充的释义块：与本地释义做视觉区隔，让人一眼看出「这条不是词库里的」 */
.res-ext {
  margin-top: 16px;
  padding: 14px 16px;
  border-radius: 10px;
  background: var(--c6-fill);
  border: 1px solid var(--c6-border-soft);
}

.ext-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}

.ext-pos {
  font-size: 12px;
  color: var(--c6-primary);
}

.ext-list {
  margin: 0;
  padding-left: 20px;
  font-size: 13px;
  line-height: 1.9;
  color: #6b5f4c;
}

.ext-list li {
  margin-bottom: 2px;
}

[data-theme='minimal'] .ext-list {
  color: #9a9a9a;
}

.res-actions {
  margin-top: 18px;
  display: flex;
  align-items: center;
  gap: 14px;
}

.suggest {
  font-size: 13px;
  color: var(--c6-text-sub);
  display: flex;
  align-items: center;
  gap: 8px;
  justify-content: center;
}

.stats-strip {
  display: flex;
  gap: 16px;
}

.strip-item {
  flex: 1;
  padding: 16px 18px;
}

.word-num {
  font-size: 20px;
  font-family: var(--c6-serif);
}

.grid {
  display: flex;
  gap: 16px;
}

.grid > .c6-card {
  flex: 1;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 14px;
}

.card-head .c6-title {
  margin: 0;
}

.empty {
  color: var(--c6-text-sub);
  font-size: 13px;
  padding: 24px 0;
  text-align: center;
}

.hot-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 0;
}

.rank {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: var(--c6-track);
  color: var(--c6-text-sub);
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank.top3 {
  background: var(--c6-primary);
  color: #fff;
  font-weight: 700;
}

.hot-word {
  width: 130px;
  font-size: 14px;
  cursor: pointer;
  flex-shrink: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hot-word:hover {
  color: var(--c6-primary);
  text-decoration: underline;
}

.hot-bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--c6-track);
  overflow: hidden;
}

.hot-bar-fill {
  height: 100%;
  background: var(--c6-primary-light);
  border-radius: 3px;
}

.hot-count {
  width: 46px;
  text-align: right;
  font-size: 12px;
  color: var(--c6-text-sub);
}

.recent-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 9px 0;
  border-bottom: 1px dashed var(--c6-border-soft);
}

.recent-row:last-child {
  border-bottom: none;
}

.recent-word {
  font-size: 14px;
  cursor: pointer;
}

.recent-word:hover {
  color: var(--c6-primary);
  text-decoration: underline;
}

.note-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #6b5f4c;
  line-height: 2;
}
</style>

<!--
  联想下拉面板被 el-autocomplete teleport 到 body 上，
  scoped 的作用域属性够不着它，只能单开一个非 scoped 样式块。
-->
<style>
.dict-suggest-popper .sg-row {
  display: flex;
  align-items: center;
  gap: 8px;
  line-height: 1.4;
}

.dict-suggest-popper .sg-word {
  font-family: var(--c6-serif);
  font-size: 15px;
  font-weight: 600;
  color: #3a3226;
}

.dict-suggest-popper .sg-phonetic {
  font-size: 12px;
  color: #a2957f;
}

.dict-suggest-popper .sg-recent-tag {
  font-size: 11px;
  color: #b3a893;
  border: 1px solid var(--c6-border);
  border-radius: 4px;
  padding: 0 4px;
}

.dict-suggest-popper .sg-brief {
  margin-left: auto;
  font-size: 12px;
  color: #93866f;
  max-width: 260px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* Element Plus 默认把候选行限高 34px，两行内容会被裁掉，这里放开 */
.dict-suggest-popper .el-autocomplete-suggestion__list li {
  height: auto;
  line-height: 1.6;
  padding: 7px 12px;
}
</style>
