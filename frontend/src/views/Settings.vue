<template>
  <div class="settings-page">
    <div class="page-head">
      <div>
        <h2 class="sec-title">设置</h2>
        <p class="c6-sub">
          配置查词、发音、翻译所用的外部数据源。改完点保存即时生效，无需重启后端。
          外部源全部是「增强」——关掉或不可用时会自动回退到本地内容，不影响主功能。
        </p>
      </div>
      <el-button type="primary" :loading="saving" @click="save">保存设置</el-button>
    </div>

    <!-- 总开关 -->
    <div class="c6-card">
      <div class="card-head">
        <h3 class="c6-title">总开关与通用参数</h3>
        <span class="c6-sub">关掉总开关相当于一键断网：所有外部请求都不再发出</span>
      </div>
      <el-form label-width="120px" label-position="left">
        <el-form-item label="启用外部源">
          <el-switch v-model="form.enabled" active-text="开启" inactive-text="关闭" />
        </el-form-item>
        <el-form-item label="结果缓存">
          <el-input-number v-model="form.cacheMinutes" :min="0" :max="1440" :step="5" />
          <span class="hint">分钟。免费接口有额度限制，缓存能避免同一个词重复消耗额度；填 0 表示不缓存</span>
        </el-form-item>
        <el-form-item label="请求超时">
          <el-input-number v-model="form.timeoutMs" :min="1000" :max="30000" :step="500" />
          <span class="hint">毫秒。失败会自动重试一次，因此源站故障时最多等待约 2 倍该值；之后 60 秒内直接短路，不再等待</span>
        </el-form-item>
      </el-form>
    </div>

    <!-- 三个源 -->
    <div v-for="sec in sections" :key="sec.type" class="c6-card">
      <div class="card-head">
        <h3 class="c6-title">{{ sec.title }}</h3>
        <span class="c6-sub">{{ sec.sub }}</span>
      </div>
      <el-form label-width="120px" label-position="left">
        <el-form-item label="数据源">
          <el-select v-model="form[sec.sourceKey]" style="width: 300px">
            <el-option
              v-for="o in form[sec.optionsKey] || []"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
          <span class="hint">{{ optionDesc(form[sec.optionsKey], form[sec.sourceKey]) }}</span>
        </el-form-item>
        <el-form-item label="自定义地址">
          <el-input
            v-model="form[sec.baseUrlKey]"
            :placeholder="sec.placeholder"
            style="max-width: 520px"
            clearable
          />
          <el-button class="test-btn" :loading="testing[sec.type]" @click="test(sec.type)">测试连通</el-button>
        </el-form-item>
        <div v-if="testResult[sec.type]" class="test-result" :class="testResult[sec.type].ok ? 'ok' : 'bad'">
          <el-tag :type="testResult[sec.type].ok ? 'success' : 'danger'" size="small" effect="light">
            {{ testResult[sec.type].ok ? '可用' : '不可用' }}
          </el-tag>
          <span class="tr-source">{{ testResult[sec.type].source }}</span>
          <span class="tr-cost">{{ testResult[sec.type].costMs }} ms</span>
          <span class="tr-msg">{{ testResult[sec.type].message }}</span>
        </div>
      </el-form>
    </div>

    <!-- 说明 -->
    <div class="c6-card note">
      <h3 class="c6-title">这些源是怎么回事？</h3>
      <ul class="note-list">
        <li>
          <b>全部免密钥、无需注册</b>。Datamuse 与有道发音国内直连可用；MyMemory 单次上限 500 字符，
          后端会按句末标点自动分段翻译再拼接。
        </li>
        <li>
          <b>dictionaryapi.dev 目前不推荐</b>：实测其源站故障，只有 CDN 缓存里已有的词能返回，
          未缓存的词一律 522。保留为可选项，等它恢复后可直接切过去。
        </li>
        <li>
          <b>自定义地址</b>留空即用内置默认。填了会整体替换默认地址 ——
          可以指向你自己的中转/代理服务，用于统一管控或替换不可用的源。
        </li>
        <li>
          <b>降级策略</b>：在线源失败时，查词仍返回本地 ECDICT 的中文释义，发音回退到浏览器语音合成，
          翻译仍给出人工参考译文与关键点覆盖率。任何时候都不会因为外部源挂了而用不了。
        </li>
        <li>
          <b>隐私</b>：开启外部源意味着你查询的单词和翻译原文会发送到第三方服务。
          若在意这一点，把对应源切到「关闭」即可。
        </li>
      </ul>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { settingsApi } from '@/api'

const saving = ref(false)
const testing = reactive({ dict: false, pron: false, translate: false })
const testResult = reactive({ dict: null, pron: null, translate: null })

const form = reactive({
  enabled: true,
  dictSource: 'datamuse',
  dictBaseUrl: '',
  pronSource: 'youdao',
  pronBaseUrl: '',
  translateSource: 'mymemory',
  translateBaseUrl: '',
  cacheMinutes: 10,
  timeoutMs: 4000,
  dictOptions: [],
  pronOptions: [],
  translateOptions: []
})

/**
 * 三个源的差异只有 key 名，用配置驱动渲染，避免把同一段模板抄三遍。
 */
const sections = [
  {
    type: 'dict',
    title: '词典源',
    sub: '查词时补充音标、词性与英文释义；中文释义始终以本地词库为准',
    sourceKey: 'dictSource',
    baseUrlKey: 'dictBaseUrl',
    optionsKey: 'dictOptions',
    placeholder: '留空使用默认：https://api.datamuse.com/words'
  },
  {
    type: 'pron',
    title: '发音源',
    sub: '单词发音音频；关闭时回退到浏览器内置语音合成',
    sourceKey: 'pronSource',
    baseUrlKey: 'pronBaseUrl',
    optionsKey: 'pronOptions',
    placeholder: '留空使用默认：https://dict.youdao.com/dictvoice'
  },
  {
    type: 'translate',
    title: '翻译源',
    sub: '翻译练习中生成机器参考译文，与人工参考译文对照',
    sourceKey: 'translateSource',
    baseUrlKey: 'translateBaseUrl',
    optionsKey: 'translateOptions',
    placeholder: '留空使用默认：https://api.mymemory.translated.net/get'
  }
]

function optionDesc(options, value) {
  const hit = (options || []).find((o) => o.value === value)
  return hit ? hit.desc : ''
}

async function load() {
  const res = await settingsApi.get()
  Object.assign(form, res.data || {})
}

async function save() {
  saving.value = true
  try {
    await settingsApi.update({ ...form })
    ElMessage.success('设置已保存并生效')
    await load()
  } finally {
    saving.value = false
  }
}

/**
 * 连通性测试。
 * 用当前已保存的配置去测（不是表单里没保存的临时值），所以改了源要先保存再测，
 * 否则测的是旧配置，容易误判。
 */
async function test(type) {
  testing[type] = true
  testResult[type] = null
  try {
    const res = await settingsApi.test(type)
    testResult[type] = res.data
  } catch (e) {
    testResult[type] = { ok: false, source: '-', costMs: 0, message: '测试请求本身失败：' + (e?.message || '未知错误') }
  } finally {
    testing[type] = false
  }
}

onMounted(load)
</script>

<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  max-width: 1040px;
  margin: 0 auto;
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.sec-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 6px;
  color: var(--c6-text);
}

.page-head .c6-sub {
  line-height: 1.8;
  max-width: 720px;
  display: block;
  margin: 0;
}

.card-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.card-head .c6-title {
  margin: 0;
}

.hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--c6-text-sub);
  line-height: 1.6;
}

.test-btn {
  margin-left: 12px;
}

.test-result {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin: 0 0 4px 120px;
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 13px;
  background: var(--c6-fill);
  border: 1px solid var(--c6-border-soft);
}

.test-result.ok {
  border-color: var(--c6-primary-light);
}

.test-result .tr-source {
  font-family: var(--c6-serif);
  color: var(--c6-text);
}

.test-result .tr-cost {
  color: var(--c6-text-sub);
  font-size: 12px;
}

.test-result .tr-msg {
  color: var(--c6-text-sub);
  word-break: break-all;
  flex: 1;
  min-width: 200px;
}

.note-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: #6b5f4c;
  line-height: 2;
}

.note-list b {
  color: var(--c6-primary);
}

[data-theme='minimal'] .note-list {
  color: #9a9a9a;
}

[data-theme='minimal'] .note-list b {
  color: #ffffff;
}

@media (max-width: 768px) {
  .page-head {
    flex-direction: column;
  }

  .test-result {
    margin-left: 0;
  }

  .hint {
    display: block;
    margin: 6px 0 0;
  }
}
</style>
