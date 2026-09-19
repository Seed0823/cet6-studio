<template>
  <div class="wrong-page">
    <div class="c6-card toolbar">
      <el-radio-group v-model="filter" @change="load">
        <el-radio-button :value="undefined">全部</el-radio-button>
        <el-radio-button :value="0">未攻克</el-radio-button>
        <el-radio-button :value="1">已攻克</el-radio-button>
      </el-radio-group>
      <div class="toolbar-right">
        <span class="c6-sub">共 {{ list.length }} 条</span>
        <el-button size="small" :icon="Refresh" @click="load">刷新</el-button>
      </div>
    </div>

    <div v-if="loading" v-loading="true" class="c6-card" style="height: 240px" />

    <div v-else-if="!list.length" class="c6-card empty-card">
      <el-empty description="错词本是空的 —— 去自测几组，答错的词会自动收进来" />
      <el-button type="primary" @click="$router.push('/quiz')">去自测</el-button>
    </div>

    <div v-else class="list">
      <div v-for="item in list" :key="item.wrongId" class="c6-card wrong-item" :class="{ mastered: item.mastered === 1 }">
        <div class="item-main">
          <div class="item-head">
            <span class="w-word">{{ item.word }}</span>
            <span v-if="item.phonetic" class="c6-sub">{{ item.phonetic }}</span>
            <el-button circle size="small" :icon="Headset" @click="speak(item.word)" />
            <el-tag
              size="small"
              :type="item.mastered === 1 ? 'success' : 'warning'"
              effect="light"
            >
              {{ item.mastered === 1 ? '已攻克' : '未攻克' }}
            </el-tag>
            <el-tag size="small" type="info" effect="plain">{{ sourceText(item.source) }}</el-tag>
          </div>
          <div class="w-meaning">
            <div v-for="(line, i) in meaningLines(item.translation)" :key="i">{{ line }}</div>
          </div>
          <div class="c6-sub">答错 {{ item.wrongCount }} 次 · 收录于 {{ formatTime(item.createTime) }}</div>
        </div>

        <div class="item-actions">
          <el-button
            size="small"
            :type="item.mastered === 1 ? 'default' : 'primary'"
            @click="toggleMastered(item)"
          >
            {{ item.mastered === 1 ? '取消攻克' : '标记攻克' }}
          </el-button>
          <el-button size="small" text type="danger" @click="remove(item)">移除</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Headset } from '@element-plus/icons-vue'
import { wrongApi } from '@/api'

const filter = ref(undefined)
const list = ref([])
const loading = ref(true)

function meaningLines(t) {
  if (!t) return ['（暂无中文释义）']
  return String(t).split(/\r?\n/).filter((s) => s.trim())
}

function sourceText(s) {
  return { quiz: '自测答错', exam: '模考答错', dict: '查词收藏' }[s] || s
}

function formatTime(t) {
  return t ? String(t).slice(0, 10) : ''
}

function speak(text) {
  if (!text || !window.speechSynthesis) return
  const u = new SpeechSynthesisUtterance(text)
  u.lang = 'en-US'
  window.speechSynthesis.cancel()
  window.speechSynthesis.speak(u)
}

async function load() {
  loading.value = true
  try {
    const res = await wrongApi.list(filter.value)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function toggleMastered(item) {
  const next = item.mastered !== 1
  await wrongApi.master(item.wrongId, next)
  item.mastered = next ? 1 : 0
  ElMessage.success(next ? '已标记为攻克' : '已取消攻克')
  if (filter.value !== undefined) {
    load()
  }
}

async function remove(item) {
  try {
    await ElMessageBox.confirm(`确定把「${item.word}」移出错词本吗？`, '提示', {
      confirmButtonText: '移除',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (e) {
    return
  }
  await wrongApi.remove(item.wrongId)
  ElMessage.success('已移除')
  load()
}

onMounted(load)
</script>

<style scoped>
.wrong-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 900px;
  margin: 0 auto;
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

.empty-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 40px 0;
}

.list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.wrong-item {
  display: flex;
  align-items: flex-start;
  gap: 20px;
  padding: 16px 20px;
}

.wrong-item.mastered {
  opacity: 0.62;
}

.item-main {
  flex: 1;
  min-width: 0;
}

.item-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.w-word {
  font-size: 19px;
  font-weight: 700;
  font-family: 'Georgia', 'Times New Roman', serif;
}

.w-meaning {
  font-size: 14px;
  line-height: 1.8;
  margin: 8px 0 6px;
  color: #3a3226;
}

.item-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: stretch;
  flex-shrink: 0;
}
</style>
