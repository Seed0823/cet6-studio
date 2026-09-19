<template>
  <div v-loading="loading" class="stats-page">
    <!-- 汇总卡 -->
    <div class="summary">
      <div v-for="c in cards" :key="c.label" class="c6-card sum-item">
        <div class="c6-num" :style="{ color: c.color }">{{ c.value }}</div>
        <div class="c6-sub">{{ c.label }}</div>
      </div>
    </div>

    <!-- 学习时长趋势 -->
    <div class="c6-card">
      <div class="card-head">
        <h3 class="c6-title">学习时长趋势</h3>
        <el-radio-group v-model="days" size="small" @change="load">
          <el-radio-button :value="7">近 7 天</el-radio-button>
          <el-radio-button :value="30">近 30 天</el-radio-button>
        </el-radio-group>
      </div>
      <div ref="timeChartRef" class="chart" />
    </div>

    <!-- 正确率趋势 -->
    <div class="c6-card">
      <div class="card-head">
        <h3 class="c6-title">测验正确率</h3>
        <span class="c6-sub">共 {{ quiz?.count ?? 0 }} 次自测 · 平均 {{ quiz?.avgAccuracy ?? 0 }}%</span>
      </div>
      <div v-if="!quiz?.trend?.length" class="empty">还没有自测记录，去测一组吧</div>
      <div v-else ref="accChartRef" class="chart" />
    </div>

    <!-- 自测明细 -->
    <div class="c6-card">
      <h3 class="c6-title">最近自测</h3>
      <el-table :data="quiz?.trend?.slice().reverse() || []" size="small" empty-text="暂无记录">
        <el-table-column prop="time" label="时间" width="130" />
        <el-table-column label="得分" width="100">
          <template #default="{ row }">{{ row.correct }}/{{ row.total }}</template>
        </el-table-column>
        <el-table-column label="正确率" width="120">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.accuracy >= 80 ? 'success' : row.accuracy >= 60 ? 'warning' : 'danger'"
              effect="light"
            >
              {{ row.accuracy }}%
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="水平">
          <template #default="{ row }">{{ levelText(row.accuracy) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
/*
 * ECharts 按需引入：只注册本页真正用到的「折线图 + 柱状图」及其依赖组件，
 * 相比 `import * as echarts from 'echarts'`（引入全部图表类型）可显著减小
 * 该 chunk 体积。新增图表类型时，记得在下方 use() 里补注册对应模块。
 */
import * as echarts from 'echarts/core'
import { LineChart, BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, MarkLineComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { statsApi } from '@/api'
import { useTheme } from '@/composables/theme'

echarts.use([LineChart, BarChart, GridComponent, TooltipComponent, MarkLineComponent, CanvasRenderer])

const { theme } = useTheme()
const isMinimal = computed(() => theme.value === 'minimal')

const days = ref(30)
const loading = ref(true)
const summary = ref({})
const trend = ref([])
const quiz = ref({})
const timeChartRef = ref(null)
const accChartRef = ref(null)

let timeChart = null
let accChart = null

const cards = computed(() => {
  const base = [
    { label: '累计学习时长（分钟）', value: summary.value.totalMinutes ?? 0 },
    { label: '已学单词', value: summary.value.learnedWords ?? 0 },
    { label: '已掌握单词', value: summary.value.knownWords ?? 0 },
    { label: '待复习', value: summary.value.dueReview ?? 0 },
    { label: '未攻克错词', value: summary.value.unmasteredWrong ?? 0 },
    { label: '连续打卡（天）', value: summary.value.streakDays ?? 0 }
  ]
  // 极简主题：数字统一墨色，去掉暖纸下的分色编码
  if (isMinimal.value) return base.map((s) => ({ ...s, color: '#1a1a1a' }))
  return [
    { ...base[0], color: '#b07120' },
    { ...base[1], color: '#2e2820' },
    { ...base[2], color: '#b07120' },
    { ...base[3], color: '#b98a0e' },
    { ...base[4], color: '#c0504d' },
    { ...base[5], color: '#2e2820' }
  ]
})

/*
 * 图表取色：暖纸用赭石/青绿渐变，极简全部压成墨色 + 灰阶，
 * 去掉一切高饱和（青色渐变、粉红标记线、红色及格线标签）。
 */
const PAPER = {
  axis: '#e7dcc8',
  label: '#93866f',
  split: '#f5efe1',
  line: '#b07120',
  areaFrom: 'rgba(91,207,197,0.45)',
  areaTo: 'rgba(91,207,197,0.02)',
  bar: '#d19a4a',
  mark: '#e0a3a0',
  markLabel: '#c0504d'
}
const MINIMAL = {
  axis: '#e5e2db',
  label: '#9a948a',
  split: '#f1efea',
  line: '#1a1a1a',
  areaFrom: 'rgba(26,26,26,0.30)',
  areaTo: 'rgba(26,26,26,0.02)',
  bar: '#1a1a1a',
  mark: '#cfccc6',
  markLabel: '#8a8a8a'
}
const pal = computed(() => (isMinimal.value ? MINIMAL : PAPER))

function levelText(a) {
  if (a >= 90) return '优秀'
  if (a >= 75) return '良好'
  if (a >= 60) return '及格'
  return '需加强'
}

async function load() {
  loading.value = true
  try {
    const res = await statsApi.overview(days.value)
    summary.value = res.data.summary || {}
    trend.value = res.data.trend || []
    quiz.value = res.data.quiz || {}
    await nextTick()
    renderTimeChart()
    renderAccChart()
  } finally {
    loading.value = false
  }
}

function renderTimeChart() {
  if (!timeChartRef.value) return
  if (!timeChart) {
    timeChart = echarts.init(timeChartRef.value)
  }
  const p = pal.value
  timeChart.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis' },
    xAxis: {
      type: 'category',
      data: trend.value.map((d) => d.date.slice(5)),
      axisLine: { lineStyle: { color: p.axis } },
      axisLabel: { color: p.label, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      name: '分钟',
      nameTextStyle: { color: p.label, fontSize: 11 },
      splitLine: { lineStyle: { color: p.split } },
      axisLabel: { color: p.label, fontSize: 11 }
    },
    series: [
      {
        data: trend.value.map((d) => d.minutes),
        type: 'line',
        smooth: true,
        symbolSize: 6,
        itemStyle: { color: p.line },
        lineStyle: { width: 2.5 },
        // 用 ECharts 原生对象式渐变，避免额外依赖 echarts.graphic 命名空间
        areaStyle: {
          color: {
            type: 'linear',
            x: 0,
            y: 0,
            x2: 0,
            y2: 1,
            colorStops: [
              { offset: 0, color: p.areaFrom },
              { offset: 1, color: p.areaTo }
            ]
          }
        }
      }
    ]
  })
  timeChart.resize()
}

function renderAccChart() {
  if (!accChartRef.value) return
  if (!accChart) {
    accChart = echarts.init(accChartRef.value)
  }
  const p = pal.value
  const list = quiz.value.trend || []
  accChart.setOption({
    grid: { left: 40, right: 20, top: 30, bottom: 30 },
    tooltip: { trigger: 'axis', formatter: '{b}<br/>正确率 {c}%' },
    xAxis: {
      type: 'category',
      data: list.map((d) => d.time),
      axisLine: { lineStyle: { color: p.axis } },
      axisLabel: { color: p.label, fontSize: 11 }
    },
    yAxis: {
      type: 'value',
      max: 100,
      name: '%',
      nameTextStyle: { color: p.label, fontSize: 11 },
      splitLine: { lineStyle: { color: p.split } },
      axisLabel: { color: p.label, fontSize: 11 }
    },
    series: [
      {
        data: list.map((d) => d.accuracy),
        type: 'bar',
        barMaxWidth: 42,
        itemStyle: { color: p.bar, borderRadius: [6, 6, 0, 0] },
        markLine: {
          silent: true,
          symbol: 'none',
          lineStyle: { color: p.mark, type: 'dashed' },
          data: [{ yAxis: 60, label: { formatter: '及格线', color: p.markLabel, fontSize: 11 } }]
        }
      }
    ]
  })
  accChart.resize()
}

function onResize() {
  timeChart?.resize()
  accChart?.resize()
}

watch(days, load)

// 主题切换时重绘图表（取色随主题变化）；用 nextTick 等 DOM/尺寸稳定
watch(theme, () => {
  nextTick(() => {
    renderTimeChart()
    renderAccChart()
  })
})

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  timeChart?.dispose()
  accChart?.dispose()
})
</script>

<style scoped>
.stats-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.summary {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 14px;
}

.sum-item {
  padding: 16px;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}

.card-head .c6-title {
  margin: 0;
}

.chart {
  height: 260px;
  width: 100%;
}

.empty {
  color: var(--c6-text-sub);
  font-size: 13px;
  padding: 40px 0;
  text-align: center;
}

@media (max-width: 1280px) {
  .summary {
    grid-template-columns: repeat(3, 1fr);
  }
}

/* 移动端：汇总卡两列排布、图表降高，避免横向溢出 */
@media (max-width: 768px) {
  .summary {
    grid-template-columns: repeat(2, 1fr);
    gap: 10px;
  }

  .sum-item {
    padding: 12px;
  }

  .c6-num {
    font-size: 22px;
  }

  .chart {
    height: 220px;
  }
}
</style>
