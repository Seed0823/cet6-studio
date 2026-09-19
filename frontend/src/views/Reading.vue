<template>
  <div class="reading">
    <!-- 概览 -->
    <div class="c6-card overview">
      <div v-for="s in overviewItems" :key="s.label" class="ov-item">
        <div class="ov-num" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="ov-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 筛选 -->
    <div class="c6-card filters">
      <el-tabs v-model="query.category" class="cat-tabs" @tab-change="reload">
        <el-tab-pane label="全部" name="" />
        <el-tab-pane v-for="c in CATEGORIES" :key="c.value" :label="c.label" :name="c.value" />
      </el-tabs>

      <div class="filter-right">
        <el-select
          v-model="query.difficulty"
          placeholder="难度"
          clearable
          style="width: 108px"
          @change="reload"
        >
          <el-option v-for="d in [1, 2, 3, 4, 5]" :key="d" :label="'难度 ' + d" :value="d" />
        </el-select>
        <el-checkbox v-model="query.unfinishedOnly" @change="reload">只看未读完</el-checkbox>
        <el-input
          v-model="query.keyword"
          placeholder="搜标题或简介"
          clearable
          style="width: 180px"
          @keyup.enter="reload"
        />
      </div>
    </div>

    <!-- 列表 -->
    <div v-loading="loading" class="grid">
      <article
        v-for="a in list"
        :key="a.id"
        class="c6-card art"
        @click="goRead(a.id)"
      >
        <div class="art-top">
          <span class="art-cat" :style="catStyle(a.category)">{{ catLabel(a.category) }}</span>
          <span class="art-diff" :title="'难度 ' + a.difficulty">{{ stars(a.difficulty) }}</span>
        </div>

        <h3 class="art-title">{{ a.title }}</h3>
        <div class="art-title-cn">{{ a.titleCn }}</div>

        <p class="art-summary">{{ a.summary }}</p>

        <div class="art-meta">
          <span>{{ a.wordCount }} 词</span>
          <span v-if="a.author">· {{ a.author }}</span>
        </div>

        <div class="art-foot">
          <el-progress
            :percentage="a.finished ? 100 : a.progress || 0"
            :stroke-width="4"
            :show-text="false"
            :color="a.finished ? (isMinimal ? '#1a1a1a' : '#3b6d11') : (isMinimal ? '#1a1a1a' : '#b07120')"
            class="art-bar"
          />
          <span class="art-state" :class="{ done: a.finished }">{{ stateText(a) }}</span>
        </div>
      </article>

      <el-empty v-if="!loading && list.length === 0" description="没有符合条件的文章" class="empty-all" />
    </div>

    <div v-if="total > query.size" class="pager">
      <el-pagination
        :current-page="query.page"
        :page-size="query.size"
        :total="total"
        layout="prev, pager, next"
        background
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { readingApi } from '@/api'
import { useTheme } from '@/composables/theme'

const { theme } = useTheme()
const isMinimal = computed(() => theme.value === 'minimal')

const router = useRouter()

const CATEGORIES = [
  { value: 'exam', label: '真题仿真', color: '#854f0b', bg: '#faeeda' },
  { value: 'essay', label: '公版美文', color: '#993c1d', bg: '#faece7' },
  { value: 'news', label: '外刊新闻', color: '#3b6d11', bg: '#eaf3de' },
  { value: 'science', label: '科普短文', color: '#72243e', bg: '#fbeaf0' }
]

const loading = ref(false)
const list = ref([])
const total = ref(0)
const stats = ref({})

const query = reactive({
  page: 1,
  size: 9,
  category: '',
  difficulty: null,
  keyword: '',
  unfinishedOnly: false
})

const overviewItems = computed(() => {
  const base = [
    { label: '精读文章', value: stats.value.total ?? 0 },
    { label: '已读完', value: stats.value.finishedCount ?? 0 },
    { label: '在读', value: stats.value.readingCount ?? 0 },
    { label: '已接触六级词', value: stats.value.cet6WordCount ?? 0 },
    { label: '阅读分钟', value: stats.value.totalMinutes ?? 0 }
  ]
  // 极简主题：数字统一墨色，去掉暖纸下的分色编码（克制、低对比）
  if (isMinimal.value) return base.map((s) => ({ ...s, color: '#1a1a1a' }))
  return [
    { ...base[0], color: '#2e2820' },
    { ...base[1], color: '#3b6d11' },
    { ...base[2], color: '#b07120' },
    { ...base[3], color: '#72243e' },
    { ...base[4], color: '#993c1d' }
  ]
})

function catItem(v) {
  return CATEGORIES.find((c) => c.value === v) || { label: v, color: '#93866f', bg: '#f2ebdd' }
}
function catLabel(v) {
  return catItem(v).label
}
function catStyle(v) {
  // 极简主题：分类标签中性化，交给 CSS 处理（细描边浅底），不用彩色块
  if (isMinimal.value) return {}
  const c = catItem(v)
  return { color: c.color, background: c.bg }
}
function stars(d) {
  const n = Math.min(Math.max(d || 0, 0), 5)
  return '★'.repeat(n) + '☆'.repeat(Math.max(0, 5 - n))
}
function stateText(a) {
  if (a.finished) return '已读完'
  if (a.progress > 0) return `读到 ${a.progress}%`
  return '未读'
}

async function load() {
  loading.value = true
  try {
    const res = await readingApi.list({
      page: query.page,
      size: query.size,
      category: query.category || undefined,
      difficulty: query.difficulty || undefined,
      keyword: query.keyword || undefined,
      unfinishedOnly: query.unfinishedOnly || undefined
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function loadStats() {
  const res = await readingApi.stats()
  stats.value = res.data || {}
}

function reload() {
  query.page = 1
  load()
}

function onPageChange(p) {
  query.page = p
  load()
}

function goRead(id) {
  router.push(`/reading/${id}`)
}

onMounted(() => {
  load()
  loadStats()
})
</script>

<style scoped>
.reading {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.overview {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 18px 24px;
}

.ov-item {
  flex: 1;
  text-align: center;
  border-right: 1px solid var(--c6-border);
}

.ov-item:last-child {
  border-right: none;
}

.ov-num {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.2;
  font-family: var(--c6-num);
  font-variant-numeric: lining-nums tabular-nums;
}

.ov-label {
  font-size: 12px;
  color: var(--c6-text-sub);
  margin-top: 2px;
}

.filters {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 6px 18px 0;
  flex-wrap: wrap;
}

.cat-tabs {
  flex: 1;
  min-width: 320px;
}

.cat-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.filter-right {
  display: flex;
  align-items: center;
  gap: 14px;
  padding-bottom: 6px;
}

.grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  min-height: 200px;
}

.art {
  padding: 16px 18px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  cursor: pointer;
  transition: transform 0.15s, box-shadow 0.15s;
}

.art:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 22px rgba(96, 74, 40, 0.14);
  border-color: var(--c6-primary-light);
}

.art-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.art-cat {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 600;
}

.art-diff {
  font-size: 11px;
  color: #c9a24a;
  letter-spacing: 1px;
}

.art-title {
  margin: 2px 0 0;
  font-family: var(--c6-serif);
  font-size: 15.5px;
  line-height: 1.4;
  font-weight: 700;
  color: var(--c6-text);
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.art-title-cn {
  font-size: 12px;
  color: var(--c6-text-sub);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.art-summary {
  margin: 2px 0 0;
  font-size: 12.5px;
  line-height: 1.6;
  color: #6b5f4c;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
}

.art-meta {
  font-size: 11.5px;
  color: var(--c6-text-sub);
  display: flex;
  gap: 4px;
}

.art-foot {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: auto;
  padding-top: 6px;
}

.art-bar {
  flex: 1;
}

.art-state {
  font-size: 11.5px;
  color: #a2661a;
  white-space: nowrap;
}

.art-state.done {
  color: #3b6d11;
}

.empty-all {
  grid-column: 1 / -1;
}

.pager {
  display: flex;
  justify-content: center;
  padding: 4px 0 8px;
}

/* 极简主题：列表卡片的暖色信息全部降为中性灰阶，分类标签改细描边浅底 */
[data-theme='minimal'] .art:hover {
  transform: none;
  box-shadow: none;
  border-color: var(--c6-border);
}

[data-theme='minimal'] .art-cat {
  color: #6b6b6b;
  background: #f1efea;
  border: 1px solid #e5e2db;
}

[data-theme='minimal'] .art-diff {
  color: #b5b0a6;
}

[data-theme='minimal'] .art-summary {
  color: #6f6a62;
}

[data-theme='minimal'] .art-state {
  color: #8a8a8a;
}

[data-theme='minimal'] .art-state.done {
  color: #1a1a1a;
}

/* ============================================================
 * 移动端适配：文章网格 3 列 → 2 列 → 1 列，筛选区收窄
 * ============================================================ */
@media (max-width: 900px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .filters {
    padding: 4px 2px 0;
    gap: 10px;
  }

  /* 取消定宽，避免窄屏下 tabs 被强制撑宽溢出 */
  .cat-tabs {
    min-width: 0;
    width: 100%;
  }

  .filter-right {
    width: 100%;
    justify-content: space-between;
    flex-wrap: wrap;
  }
}
</style>
