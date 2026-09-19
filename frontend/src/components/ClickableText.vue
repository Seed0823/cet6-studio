<template>
  <span ref="root" class="clickable-text" @click="onRootClick">
    <template v-for="(seg, i) in segments" :key="i">
      <span
        v-if="seg.isWord"
        class="ct-word"
        :class="{ 'ct-cet6': seg.cet6 }"
        :data-w="seg.text"
      >{{ seg.text }}</span>
      <template v-else>{{ seg.text }}</template>
    </template>
  </span>

  <!-- 查词浮层 -->
  <Teleport to="body">
    <div v-if="visible" class="ct-mask" @click="close"></div>
    <div
      v-if="visible"
      ref="card"
      class="ct-card"
      :style="{ left: pos.left + 'px', top: pos.top + 'px' }"
      @click.stop
    >
      <div v-if="loading" class="ct-loading">查询中…</div>

      <template v-else-if="result">
        <div class="ct-head">
          <span class="ct-word-title">{{ result.word }}</span>
          <span v-if="result.phonetic" class="ct-phonetic">{{ result.phonetic }}</span>
          <el-tag v-if="result.cet6" size="small" type="success" effect="light">六级</el-tag>
          <span class="ct-close" @click="close">✕</span>
        </div>

        <template v-if="result.exactMatch">
          <div v-if="result.pos" class="ct-pos">{{ result.pos }}</div>
          <div class="ct-meaning">
            <div v-for="(line, i) in meaningLines" :key="i">{{ line }}</div>
          </div>
          <div v-if="result.exchange" class="ct-exchange">变形：{{ result.exchange }}</div>
          <div class="ct-foot">
            <span class="ct-count">已查 {{ result.queryCount }} 次</span>
            <el-button
              size="small"
              :type="result.inWrongBook ? 'info' : 'primary'"
              link
              :disabled="result.inWrongBook"
              @click="addWrong"
            >
              {{ result.inWrongBook ? '已在错词本' : '+ 加入错词本' }}
            </el-button>
          </div>
        </template>

        <template v-else>
          <div class="ct-meaning ct-empty">词库中未收录该词</div>
          <div v-if="result.suggestions?.length" class="ct-suggest">
            你是不是想查：
            <span
              v-for="s in result.suggestions"
              :key="s"
              class="ct-suggest-item"
              @click="switchWord(s)"
            >{{ s }}</span>
          </div>
        </template>
      </template>
    </div>
  </Teleport>
</template>

<script setup>
/**
 * 可点击查词的文本
 * <p>
 * 把一段文本切成「单词 / 非单词」两类片段，单词着色可点。
 * 用「事件委托」而不是给每个词单独绑定 click：
 * 一篇文章有几百个词，逐个绑定会产生几百个监听器，点击时组件还要在
 * patch 阶段为每个词创建闭包；委托只需在容器上挂 1 个监听器，
 * 通过 event.target.closest() 反查点中了哪个词，性能差一个数量级。
 */
import { computed, ref, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { dictApi } from '@/api'

const props = defineProps({
  text: { type: String, default: '' },
  /**
   * 需要高亮的六级大纲词（小写）。
   * 传空数组即不高亮 —— 背单词 / 自测页不传这个 prop，行为与改造前一致。
   */
  cet6List: { type: Array, default: () => [] }
})

const root = ref(null)
const visible = ref(false)
const loading = ref(false)
const result = ref(null)
const pos = ref({ left: 0, top: 0 })
const currentTarget = ref(null)

/** 六级词集合（小写），用于高亮判断 */
const cet6Set = computed(() => {
  const s = new Set()
  for (const w of props.cet6List || []) {
    if (w) s.add(String(w).toLowerCase())
  }
  return s
})

/** 正则分词：字母开头，允许中间出现 ' 和 - */
const segments = computed(() => {
  const source = props.text || ''
  const dict = cet6Set.value
  const highlight = dict.size > 0
  const out = []
  const re = /[A-Za-z][A-Za-z'-]*/g
  let last = 0
  let m
  while ((m = re.exec(source)) !== null) {
    if (m.index > last) {
      out.push({ isWord: false, text: source.slice(last, m.index) })
    }
    out.push({
      isWord: true,
      text: m[0],
      cet6: highlight && dict.has(m[0].toLowerCase())
    })
    last = m.index + m[0].length
  }
  if (last < source.length) {
    out.push({ isWord: false, text: source.slice(last) })
  }
  return out
})

const meaningLines = computed(() => {
  const t = result.value?.translation
  if (!t) return ['（暂无中文释义）']
  return String(t).split(/\r?\n/).filter((s) => s.trim())
})

function onRootClick(e) {
  const el = e.target.closest?.('.ct-word')
  if (!el) return
  e.stopPropagation()
  currentTarget.value = el
  openAt(el, el.dataset.w)
}

/** 浮层定位：默认贴在被点词下方，靠近右/下边界时自动翻转到另一侧 */
function openAt(el, word) {
  const rect = el.getBoundingClientRect()
  const width = 320
  let left = rect.left
  if (left + width > window.innerWidth - 12) {
    left = window.innerWidth - width - 12
  }
  let top = rect.bottom + 8
  if (top + 220 > window.innerHeight) {
    top = Math.max(12, rect.top - 228)
  }
  pos.value = { left: Math.max(12, left), top }
  visible.value = true
  doLookup(word)
}

async function doLookup(word) {
  loading.value = true
  try {
    const res = await dictApi.lookup(word)
    result.value = res.data
  } catch (e) {
    close()
  } finally {
    loading.value = false
  }
}

function switchWord(word) {
  if (currentTarget.value) {
    openAt(currentTarget.value, word)
  } else {
    doLookup(word)
  }
}

async function addWrong() {
  if (!result.value?.wordId) return
  await dictApi.addWrong(result.value.wordId)
  result.value.inWrongBook = true
  ElMessage.success('已加入错词本')
}

function close() {
  visible.value = false
  result.value = null
}

function onKeydown(e) {
  if (e.key === 'Escape') close()
}
window.addEventListener('keydown', onKeydown)
onBeforeUnmount(() => window.removeEventListener('keydown', onKeydown))
</script>

<style scoped>
.clickable-text {
  line-height: 1.9;
}

.ct-word {
  cursor: pointer;
  border-bottom: 1px dashed transparent;
  border-radius: 3px;
  transition: color 0.15s, border-color 0.15s, background 0.15s;
}

.ct-word:hover {
  color: var(--c6-primary-dark);
  background: var(--c6-primary-soft);
  border-bottom-color: var(--c6-primary);
}

/*
 * 六级大纲词：琥珀色常驻标出，和正文的深棕色拉开区别。
 *
 * 换到暖纸主题后主题色本身就是赭石，原来「六级词用暖色、hover 用主题色」
 * 的分工失效了 —— 两个信息都靠颜色表达，读者分不清一处高亮到底是词表
 * 标记还是鼠标悬停反馈。现在改成：高亮靠色相（琥珀 vs 深棕正文），
 * 交互靠形态（浅底色块 + 实线 vs 常驻虚线下划线）。
 */
.ct-cet6 {
  color: #a2661a;
  font-weight: 600;
  border-bottom: 1px dashed rgba(162, 102, 26, 0.4);
}

.ct-cet6:hover {
  color: #854f0b;
  border-bottom-color: #854f0b;
}

/*
 * 极简黑白主题：六级词高亮改成「细灰下划线」，绝不用刺眼色块或加粗。
 * 这是整个阅读器在极简模式下最关键的观感改动 —— 长文里一大片琥珀虚线
 * 会破坏「冷静克制」的氛围。这里把高亮信息全部收敛到「一条 1px 灰线」，
 * 标题/正文/高亮都是同一套无衬线灰阶，层次靠粗细而非颜色区分。
 */
[data-theme='minimal'] .ct-word:hover {
  color: #000;
  background: #f0eeea;
  border-bottom-color: #1a1a1a;
}

[data-theme='minimal'] .ct-cet6 {
  color: var(--c6-text);
  font-weight: 400;
  border-bottom: 1px solid #9a9a9a;
}

[data-theme='minimal'] .ct-cet6:hover {
  color: #000;
  border-bottom-color: #000;
}

.ct-mask {
  position: fixed;
  inset: 0;
  z-index: 3000;
}

.ct-card {
  position: fixed;
  z-index: 3001;
  width: 320px;
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  box-shadow: 0 8px 28px rgba(96, 74, 40, 0.18);
  border: 1px solid var(--c6-border);
  /* 浮层是正文（衬线体）的后代节点，不显式声明就会把中文释义也带成宋体 */
  font-family: var(--c6-sans);
}

.ct-loading {
  color: var(--c6-text-sub);
  font-size: 13px;
  padding: 8px 0;
}

.ct-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.ct-word-title {
  font-family: var(--c6-serif);
  font-size: 17px;
  font-weight: 700;
  color: var(--c6-text);
}

.ct-phonetic {
  font-size: 12px;
  color: var(--c6-text-sub);
}

.ct-close {
  margin-left: auto;
  cursor: pointer;
  color: #c3b8a3;
  font-size: 13px;
}

.ct-close:hover {
  color: var(--c6-text);
}

.ct-pos {
  font-size: 12px;
  color: var(--c6-primary);
  margin-bottom: 4px;
}

.ct-meaning {
  font-size: 13px;
  color: #6b5f4c;
  line-height: 1.7;
  max-height: 160px;
  overflow-y: auto;
}

.ct-empty {
  color: var(--c6-text-sub);
}

.ct-exchange {
  margin-top: 8px;
  font-size: 12px;
  color: var(--c6-text-sub);
}

.ct-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px solid var(--c6-border);
}

.ct-count {
  font-size: 12px;
  color: var(--c6-text-sub);
}

.ct-suggest {
  margin-top: 8px;
  font-size: 12px;
  color: var(--c6-text-sub);
}

.ct-suggest-item {
  color: var(--c6-primary);
  cursor: pointer;
  margin-right: 8px;
}

.ct-suggest-item:hover {
  text-decoration: underline;
}
</style>
