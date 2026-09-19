<template>
  <div v-loading="loading" class="dashboard">
    <!-- 第一行：今日目标 + 统计 -->
    <div class="row row-hero">
      <div class="c6-card hero">
        <div class="ring-wrap">
          <svg width="132" height="132" viewBox="0 0 132 132">
            <circle cx="66" cy="66" r="56" fill="none" stroke="var(--c6-track)" stroke-width="11" />
            <circle
              cx="66"
              cy="66"
              r="56"
              fill="none"
              stroke="var(--c6-primary)"
              stroke-width="11"
              stroke-linecap="round"
              :stroke-dasharray="circumference"
              :stroke-dashoffset="dashOffset"
              transform="rotate(-90 66 66)"
              class="ring-progress"
            />
          </svg>
          <div class="ring-center">
            <div class="ring-num">{{ data.taskFinished || 0 }}/{{ data.taskTotal || 0 }}</div>
            <div class="ring-label">今日任务</div>
          </div>
        </div>

        <div class="hero-text">
          <h2>今日目标：{{ remaining > 0 ? `还有 ${remaining} 项任务未完成` : '全部完成，漂亮！' }}</h2>
          <p class="c6-sub">
            已连续打卡 <b class="hl">{{ data.streakDays || 0 }}</b> 天 · 今日已学
            <b class="hl">{{ data.todayMinutes || 0 }}</b> 分钟 · 距考试
            <b class="hl">{{ data.daysToExam ?? '--' }}</b> 天
          </p>
          <div class="hero-actions">
            <el-button type="primary" @click="$router.push('/words')">
              <el-icon><Notebook /></el-icon>&nbsp;继续背单词
            </el-button>
            <el-button @click="$router.push('/quiz')">开始自测</el-button>
            <el-button text @click="$router.push('/dict')">查词 →</el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 第二行：4 个统计卡 -->
    <div class="row row-stats">
      <div v-for="s in statCards" :key="s.label" class="c6-card stat">
        <div class="c6-num" :style="{ color: s.color }">{{ s.value }}</div>
        <div class="c6-sub">{{ s.label }}</div>
      </div>
    </div>

    <!-- 第三行：任务清单 + 本周时长 -->
    <div class="row row-mid">
      <div class="c6-card">
        <div class="card-head">
          <h3 class="c6-title">今日任务清单</h3>
          <span class="c6-sub">{{ data.taskFinished || 0 }} 项已完成 · {{ remaining }} 项待完成</span>
        </div>
        <div v-if="!data.tasks?.length" class="empty">暂无任务</div>
        <div v-for="t in data.tasks" :key="t.id" class="task">
          <div class="task-check" :class="{ done: t.status === 1 }">
            <span v-if="t.status === 1">✓</span>
          </div>
          <div class="task-name">{{ t.taskName }}</div>
          <div class="task-progress">
            <div class="bar"><div class="bar-fill" :style="{ width: pct(t) + '%' }" /></div>
          </div>
          <div class="task-meta">{{ t.finished }}/{{ t.target }}</div>
        </div>
      </div>

      <div class="c6-card">
        <div class="card-head">
          <h3 class="c6-title">最近 7 天学习时长</h3>
          <span class="c6-sub">累计 {{ data.totalMinutes || 0 }} 分钟</span>
        </div>
        <div class="week">
          <div v-for="d in data.week || []" :key="d.date" class="week-col">
            <div class="week-min">{{ d.minutes || '' }}</div>
            <div class="week-bar-wrap">
              <div
                class="week-bar"
                :class="{ today: d.today }"
                :style="{ height: barHeight(d.minutes) }"
              />
            </div>
            <div class="week-label" :class="{ today: d.today }">{{ d.label }}</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { dashboardApi } from '@/api'
import { useTheme } from '@/composables/theme'

const { theme } = useTheme()
const isMinimal = computed(() => theme.value === 'minimal')

const loading = ref(true)
const data = ref({})

const CIRCUMFERENCE = 2 * Math.PI * 56
const circumference = CIRCUMFERENCE

const dashOffset = computed(() => {
  const total = data.value.taskTotal || 0
  const done = data.value.taskFinished || 0
  const ratio = total === 0 ? 0 : done / total
  return CIRCUMFERENCE * (1 - ratio)
})

const remaining = computed(() => {
  const total = data.value.taskTotal || 0
  const done = data.value.taskFinished || 0
  return Math.max(0, total - done)
})

const statCards = computed(() => {
  const base = [
    { label: '已掌握词汇', value: data.value.knownWords ?? 0 },
    { label: '待复习（艾宾浩斯）', value: data.value.dueReview ?? 0 },
    { label: '未攻克错词', value: data.value.unmasteredWrong ?? 0 },
    { label: '今日学习时长', value: (data.value.todayMinutes ?? 0) + '′' }
  ]
  // 极简主题：数字统一墨色，去掉暖纸下的分色编码
  if (isMinimal.value) return base.map((s) => ({ ...s, color: '#1a1a1a' }))
  return [
    { ...base[0], color: '#2e2820' },
    { ...base[1], color: '#b98a0e' },
    { ...base[2], color: '#c0504d' },
    { ...base[3], color: '#b07120' }
  ]
})

function pct(t) {
  if (!t.target) return 0
  return Math.min(100, Math.round(((t.finished || 0) / t.target) * 100))
}

function barHeight(minutes) {
  const max = Math.max(...(data.value.week || []).map((d) => d.minutes || 0), 30)
  const h = ((minutes || 0) / max) * 100
  return `${Math.max(h, minutes > 0 ? 6 : 2)}%`
}

onMounted(async () => {
  try {
    const res = await dashboardApi.get()
    data.value = res.data
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.row {
  display: flex;
  gap: 16px;
}

.row-stats > .stat {
  flex: 1;
}

.row-mid > .c6-card:first-child {
  flex: 1.5;
}

.row-mid > .c6-card:last-child {
  flex: 1;
}

.hero {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 32px;
  padding: 24px 28px;
  background: #fffdf6;
}

.ring-wrap {
  position: relative;
  width: 132px;
  height: 132px;
  flex-shrink: 0;
}

.ring-progress {
  transition: stroke-dashoffset 0.6s ease;
}

.ring-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ring-num {
  font-size: 26px;
  font-weight: 700;
  font-family: var(--c6-num);
  font-variant-numeric: lining-nums tabular-nums;
}

.ring-label {
  font-size: 12px;
  color: var(--c6-text-sub);
}

.hero-text {
  flex: 1;
}

.hero-text h2 {
  font-size: 20px;
  margin: 0 0 8px;
}

.hl {
  color: var(--c6-primary);
}

.hero-actions {
  margin-top: 18px;
  display: flex;
  gap: 10px;
  align-items: center;
}

.stat {
  padding: 16px 18px;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-head .c6-title {
  margin: 0;
}

.empty {
  color: var(--c6-text-sub);
  font-size: 13px;
  padding: 20px 0;
  text-align: center;
}

.task {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed var(--c6-border-soft);
}

.task:last-child {
  border-bottom: none;
}

.task-check {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: var(--c6-track);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.task-check.done {
  background: var(--c6-primary);
}

.task-name {
  width: 130px;
  font-size: 14px;
  flex-shrink: 0;
}

.task-progress {
  flex: 1;
}

.bar {
  height: 6px;
  border-radius: 3px;
  background: var(--c6-track);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--c6-primary-light);
  transition: width 0.4s ease;
}

.task-meta {
  width: 56px;
  text-align: right;
  font-size: 12px;
  color: var(--c6-text-sub);
}

.week {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 160px;
  gap: 8px;
}

.week-col {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.week-min {
  font-size: 11px;
  color: var(--c6-text-sub);
  height: 16px;
}

.week-bar-wrap {
  flex: 1;
  width: 100%;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.week-bar {
  width: 60%;
  border-radius: 5px 5px 2px 2px;
  background: #e6d3a6;
  transition: height 0.4s ease;
}

.week-bar.today {
  background: var(--c6-primary);
}

.week-label {
  font-size: 11px;
  color: var(--c6-text-sub);
  margin-top: 6px;
}

.week-label.today {
  color: var(--c6-primary);
  font-weight: 600;
}

/* 极简主题：hero 去暖底、周柱图去赭石，全部压成中性灰阶 */
[data-theme='minimal'] .hero {
  background: #fbfaf7;
}

[data-theme='minimal'] .week-bar {
  background: #cfccc6;
}

[data-theme='minimal'] .week-bar.today {
  background: #1a1a1a;
}

/* ============================================================
 * 移动端适配（≤768px）
 * 桌面端靠横向 flex 并排的几行卡片，窄屏会被压扁，改为纵向堆叠/两列。
 * ============================================================ */
@media (max-width: 768px) {
  /* 今日目标卡：圆环与文案由左右并排改为上下堆叠 */
  .hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 18px;
    padding: 20px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  /* 4 个统计卡：两列排布 */
  .row-stats {
    flex-wrap: wrap;
  }

  .row-stats > .stat {
    flex: 1 1 calc(50% - 8px);
    min-width: calc(50% - 8px);
  }

  /* 任务清单 + 本周时长：上下堆叠，避免左右挤压 */
  .row-mid {
    flex-direction: column;
  }

  /* 任务行：任务名不再固定 130px，允许收缩并省略号截断，
     把横向空间让给进度条，避免窄屏下进度条被挤没 */
  .task-name {
    width: auto;
    flex-shrink: 1;
    min-width: 0;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .week {
    height: 130px;
  }
}
</style>
