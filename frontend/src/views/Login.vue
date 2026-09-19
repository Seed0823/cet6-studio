<template>
  <div class="login-page">
    <!-- 主题切换（登录页自包含，跟随全局主题） -->
    <button class="theme-toggle" @click="toggleTheme" :title="theme === 'minimal' ? '切换为书房暖纸' : '切换为极简黑白'">
      <el-icon><Moon v-if="theme === 'paper'" /><Sunny v-else /></el-icon>
      <span>{{ theme === 'paper' ? '极简' : '暖纸' }}</span>
    </button>

    <!-- 左侧品牌区 -->
    <aside class="brand">
      <span class="orb orb-1" />
      <span class="orb orb-2" />
      <div class="brand-watermark">6</div>

      <div class="brand-inner">
        <div class="brand-eyebrow">CET-6 备考 · 个人学习平台</div>

        <div class="brand-head">
          <div class="brand-mark">6</div>
          <div class="brand-titles">
            <h1 class="brand-title">六级工坊</h1>
            <div class="brand-en">CET6 STUDIO</div>
          </div>
        </div>

        <p class="brand-desc">
          把六级备考拆成每天 20 分钟可完成的小任务，单词、阅读、听力、写作、翻译一站式闭环。
        </p>

        <ul class="brand-features">
          <li
            v-for="(f, i) in features"
            :key="i"
            class="feat"
            :style="{ animationDelay: 0.18 + i * 0.08 + 's' }"
          >
            <span class="feat-icon"><el-icon><Check /></el-icon></span>
            <span class="feat-text">{{ f }}</span>
          </li>
        </ul>
      </div>
    </aside>

    <!-- 右侧表单区 -->
    <main class="form-wrap">
      <div class="form-card">
        <header class="form-head">
          <h2>{{ isRegister ? '创建你的账号' : '欢迎回来' }}</h2>
          <p class="tip">{{ isRegister ? '注册后即刻开启你的冲刺计划' : '登录以继续你的学习进度' }}</p>
        </header>

        <el-form :model="form" @submit.prevent>
          <el-input
            v-model="form.username"
            class="field"
            size="large"
            placeholder="账号 / 手机号"
            :prefix-icon="User"
          />
          <el-input
            v-model="form.password"
            type="password"
            class="field"
            size="large"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
            @keyup.enter="submit"
          />
          <transition name="field">
            <el-input
              v-if="isRegister"
              v-model="form.nickname"
              class="field"
              size="large"
              placeholder="昵称（选填）"
              :prefix-icon="Edit"
            />
          </transition>

          <div v-if="!isRegister" class="form-extra">
            <label class="remember">
              <el-checkbox v-model="remember" size="small" /> 记住账号
            </label>
          </div>

          <el-button
            class="submit"
            type="primary"
            size="large"
            :loading="loading"
            @click="submit"
          >
            {{ isRegister ? '注册并开始学习' : '登 录' }}
          </el-button>
        </el-form>

        <div class="demo-hint">
          演示账号 <code>cuijiabing</code> / <code>123456</code>
          <a @click="fillDemo">一键填入</a>
        </div>

        <div class="switch">
          <template v-if="!isRegister">
            还没有账号？<a @click="toggle">立即注册</a>
          </template>
          <template v-else>
            已有账号？<a @click="toggle">去登录</a>
          </template>
        </div>
      </div>

      <div class="form-foot">© 2026 CET6 Studio · 六级工坊</div>
    </main>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Edit } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import { useTheme } from '@/composables/theme'

const { theme, toggleTheme } = useTheme()
const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isRegister = ref(false)
const loading = ref(false)
const remember = ref(false)
const form = reactive({ username: '', password: '', nickname: '' })

const features = [
  '5400 个六级大纲词，艾宾浩斯智能复习排期',
  '5.7 万词条离线词典，遇到生词点一下就查',
  '查词自动计数，你最常查的词一眼可见',
  '错词自动入本，反复错的重点攻克'
]

onMounted(() => {
  const saved = localStorage.getItem('c6_remember_user')
  if (saved) {
    form.username = saved
    remember.value = true
  }
})

function toggle() {
  isRegister.value = !isRegister.value
}

function fillDemo() {
  form.username = 'cuijiabing'
  form.password = '123456'
}

async function submit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请填写账号和密码')
    return
  }
  loading.value = true
  try {
    if (isRegister.value) {
      await userStore.register({ ...form })
      ElMessage.success('注册成功，正在登录…')
    }
    const profile = await userStore.login({ username: form.username, password: form.password })
    if (remember.value) localStorage.setItem('c6_remember_user', form.username)
    else localStorage.removeItem('c6_remember_user')
    ElMessage.success(`欢迎回来，${profile.nickname || profile.username}`)
    router.push(route.query.redirect || '/dashboard')
  } catch (e) {
    /* 错误提示已由拦截器统一处理 */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* ============================================================
 * 登录页 · 现代分栏设计
 * 整页只引用主题变量，并补齐 [data-theme='minimal'] 的覆写，
 * 保证暖纸 / 极简两套皮肤下都高级且一致。
 * ============================================================ */
.login-page {
  position: relative;
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--c6-bg);
  animation: pageIn 0.5s ease both;
}

/* 主题切换胶囊（右上角，自包含） */
.theme-toggle {
  position: absolute;
  top: 22px;
  right: 26px;
  z-index: 20;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid rgba(255, 255, 255, 0.28);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
  font-size: 12px;
  font-family: var(--c6-sans);
  cursor: pointer;
  backdrop-filter: blur(6px);
  transition: background 0.2s, transform 0.2s, border-color 0.2s;
}
.theme-toggle:hover {
  background: rgba(255, 255, 255, 0.22);
  transform: translateY(-1px);
}
.theme-toggle .el-icon {
  font-size: 14px;
}

/* ===================== 左侧品牌区 ===================== */
.brand {
  position: relative;
  flex: 1.15;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 56px 48px;
  overflow: hidden;
  color: #fff;
  /* 书房暖纸：暖赭石深棕渐变 */
  background:
    radial-gradient(120% 120% at 12% 18%, rgba(209, 154, 74, 0.35) 0%, rgba(209, 154, 74, 0) 46%),
    linear-gradient(155deg, #6b4f2e 0%, #46331c 55%, #2c2011 100%);
  animation: fadeRight 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

/* 柔光球装饰 */
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(46px);
  opacity: 0.55;
  pointer-events: none;
}
.orb-1 {
  width: 340px;
  height: 340px;
  top: -90px;
  right: -70px;
  background: radial-gradient(circle, rgba(217, 154, 74, 0.9), rgba(217, 154, 74, 0) 70%);
}
.orb-2 {
  width: 300px;
  height: 300px;
  bottom: -110px;
  left: -60px;
  background: radial-gradient(circle, rgba(176, 113, 32, 0.85), rgba(176, 113, 32, 0) 70%);
}

/* 巨型水印数字 */
.brand-watermark {
  position: absolute;
  right: 24px;
  bottom: -40px;
  font-family: var(--c6-serif);
  font-size: 360px;
  font-weight: 800;
  line-height: 1;
  color: rgba(255, 255, 255, 0.06);
  user-select: none;
  pointer-events: none;
}

.brand-inner {
  position: relative;
  z-index: 2;
  max-width: 440px;
  width: 100%;
}

.brand-eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.62);
  margin-bottom: 22px;
}

.brand-head {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 22px;
}
.brand-mark {
  width: 60px;
  height: 60px;
  flex-shrink: 0;
  border-radius: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--c6-num);
  font-size: 30px;
  font-weight: 800;
  color: #fff;
  background: linear-gradient(145deg, rgba(255, 255, 255, 0.28), rgba(255, 255, 255, 0.08));
  border: 1px solid rgba(255, 255, 255, 0.28);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.25);
}
.brand-title {
  margin: 0;
  font-family: var(--c6-serif);
  font-size: 38px;
  font-weight: 800;
  letter-spacing: 3px;
  line-height: 1.1;
}
.brand-en {
  margin-top: 4px;
  font-size: 12px;
  letter-spacing: 4px;
  color: rgba(255, 255, 255, 0.6);
}

.brand-desc {
  font-size: 15px;
  line-height: 1.75;
  color: rgba(255, 255, 255, 0.82);
  margin: 0 0 30px;
}

.brand-features {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.feat {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: rgba(255, 255, 255, 0.9);
  opacity: 0;
  animation: fadeUp 0.6s cubic-bezier(0.22, 1, 0.36, 1) both;
}
.feat-icon {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 2px;
  background: rgba(255, 255, 255, 0.16);
  color: #fff;
}
.feat-icon .el-icon {
  font-size: 13px;
}

/* ===================== 右侧表单区 ===================== */
.form-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px;
  animation: fadeUp 0.7s cubic-bezier(0.22, 1, 0.36, 1) both;
}

.form-card {
  width: 100%;
  max-width: 380px;
  background: var(--c6-card);
  border: 1px solid var(--c6-border);
  border-radius: 22px;
  padding: 38px 36px 30px;
  --lp-glow: rgba(176, 113, 32, 0.18);
  box-shadow:
    0 1px 2px rgba(96, 74, 40, 0.05),
    0 18px 44px rgba(96, 74, 40, 0.10);
}

.form-head {
  margin-bottom: 28px;
}
.form-head h2 {
  margin: 0 0 8px;
  font-family: var(--c6-serif);
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0.5px;
  color: var(--c6-text);
}
.tip {
  margin: 0;
  font-size: 13px;
  color: var(--c6-text-sub);
}

/* 输入框：填充式 + 聚焦光环 */
.field {
  margin-bottom: 16px;
}
.field :deep(.el-input__wrapper) {
  border-radius: 12px;
  background: var(--c6-fill);
  box-shadow: 0 0 0 1px var(--c6-border) inset;
  padding: 2px 14px;
  transition: box-shadow 0.22s ease, background 0.22s ease;
}
.field :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--c6-primary) inset, 0 6px 18px var(--lp-glow);
}

.form-extra {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin: -4px 0 16px;
}
.remember {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--c6-text-sub);
  cursor: pointer;
}

/* 主按钮：渐变 + 悬浮上浮 + 按压回弹 */
.submit.el-button {
  width: 100%;
  margin-top: 4px;
  border: none;
  font-weight: 600;
  letter-spacing: 2px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--c6-primary-light), var(--c6-primary));
  box-shadow: 0 8px 18px var(--lp-glow);
  transition: transform 0.18s ease, box-shadow 0.22s ease, filter 0.2s ease;
}
.submit.el-button:hover {
  transform: translateY(-2px);
  filter: brightness(1.04);
  box-shadow: 0 12px 26px var(--lp-glow);
}
.submit.el-button:active {
  transform: translateY(0);
}
.submit.el-button.is-loading {
  transform: none;
}

/* 演示账号提示 */
.demo-hint {
  margin-top: 18px;
  padding: 10px 12px;
  border-radius: 10px;
  background: var(--c6-primary-soft);
  font-size: 12px;
  color: var(--c6-text-sub);
  text-align: center;
}
.demo-hint code {
  font-family: var(--c6-num);
  color: var(--c6-primary-dark);
  background: rgba(255, 255, 255, 0.6);
  padding: 1px 6px;
  border-radius: 5px;
}
.demo-hint a {
  margin-left: 6px;
  color: var(--c6-primary);
  font-weight: 600;
  cursor: pointer;
}

.switch {
  margin-top: 20px;
  text-align: center;
  font-size: 13px;
  color: var(--c6-text-sub);
}
.switch a {
  color: var(--c6-primary);
  cursor: pointer;
  font-weight: 600;
}

.form-foot {
  margin-top: 26px;
  font-size: 12px;
  color: var(--c6-text-sub);
  opacity: 0.75;
}

/* 注册态：昵称字段平滑展开 */
.field-enter-active,
.field-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease, margin 0.25s ease;
}
.field-enter-from,
.field-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ===================== 动效关键帧 ===================== */
@keyframes pageIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
@keyframes fadeRight {
  from { opacity: 0; transform: translateX(-26px); }
  to { opacity: 1; transform: none; }
}
@keyframes fadeUp {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: none; }
}

/* ===================== 极简黑白主题覆写 ===================== */
[data-theme='minimal'] .brand {
  background:
    radial-gradient(120% 120% at 12% 18%, rgba(255, 255, 255, 0.10) 0%, rgba(255, 255, 255, 0) 46%),
    linear-gradient(155deg, #2a2a2a 0%, #161616 55%, #000 100%);
}
[data-theme='minimal'] .orb-1 {
  background: radial-gradient(circle, rgba(255, 255, 255, 0.5), rgba(255, 255, 255, 0) 70%);
}
[data-theme='minimal'] .orb-2 {
  background: radial-gradient(circle, rgba(200, 200, 200, 0.4), rgba(200, 200, 200, 0) 70%);
}
[data-theme='minimal'] .brand-watermark {
  color: rgba(255, 255, 255, 0.05);
}
[data-theme='minimal'] .theme-toggle {
  border-color: rgba(255, 255, 255, 0.22);
}
[data-theme='minimal'] .form-card {
  --lp-glow: rgba(0, 0, 0, 0.14);
  box-shadow:
    0 1px 2px rgba(0, 0, 0, 0.04),
    0 18px 44px rgba(0, 0, 0, 0.10);
}
[data-theme='minimal'] .submit.el-button {
  background: linear-gradient(135deg, #3a3a3a, #000);
}
[data-theme='minimal'] .demo-hint {
  background: var(--c6-fill);
}

/* ===================== 响应式 ===================== */
@media (max-width: 960px) {
  .login-page {
    flex-direction: column;
    overflow-y: auto;
  }
  .brand {
    flex: none;
    padding: 52px 30px 38px;
    min-height: auto;
  }
  .brand-desc,
  .brand-features,
  .brand-watermark,
  .orb {
    display: none;
  }
  .brand-head {
    margin-bottom: 0;
  }
  .form-wrap {
    padding: 34px 22px 40px;
  }
  .form-card {
    max-width: 440px;
  }
}

@media (max-width: 420px) {
  .brand {
    padding: 40px 24px 30px;
  }
  .brand-title {
    font-size: 30px;
  }
  .form-card {
    padding: 30px 22px 26px;
    border-radius: 18px;
  }
  .theme-toggle {
    top: 16px;
    right: 16px;
  }
}

/* 尊重「减少动态效果」偏好 */
@media (prefers-reduced-motion: reduce) {
  .login-page,
  .brand,
  .form-wrap,
  .feat {
    animation: none !important;
  }
  .submit.el-button,
  .field :deep(.el-input__wrapper),
  .theme-toggle {
    transition: none !important;
  }
}
</style>
