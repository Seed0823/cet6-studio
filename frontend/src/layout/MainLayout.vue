<template>
  <div class="layout" :class="{ 'sidebar-open': sidebarOpen }">
    <!-- 移动端抽屉遮罩：点击空白处收起侧栏 -->
    <div v-if="sidebarOpen" class="sidebar-backdrop" @click="sidebarOpen = false" />

    <!-- 侧边栏（≤768px 时变为可滑出的抽屉） -->
    <aside class="sidebar">
      <div class="logo">
        <div class="logo-mark">6</div>
        <div>
          <div class="logo-title">六级工坊</div>
          <div class="logo-sub">CET6 STUDIO</div>
        </div>
      </div>

      <nav class="nav">
        <router-link
          v-for="item in menus"
          :key="item.path"
          :to="item.path"
          class="nav-item"
          :class="{ active: isActive(item.path) }"
        >
          <el-icon class="nav-icon"><component :is="item.icon" /></el-icon>
          <span>{{ item.name }}</span>
        </router-link>
      </nav>

      <div class="sidebar-foot">
        <div class="cd-box">
          <div class="cd-label">距考试</div>
          <div class="cd-num">{{ daysToExam ?? '--' }}</div>
          <div class="cd-unit">天 · {{ examDate || '未设置' }}</div>
        </div>
      </div>
    </aside>

    <!-- 主区 -->
    <main class="main">
      <header class="header">
        <div class="header-left">
          <button class="menu-btn" type="button" aria-label="展开菜单" @click="sidebarOpen = !sidebarOpen">
            <el-icon><Expand /></el-icon>
          </button>
          <h1 class="page-title">{{ route.name }}</h1>
          <span class="c6-sub">{{ todayText }}</span>
        </div>
        <div class="header-right">
          <div class="theme-switch" :title="theme === 'minimal' ? '当前：极简黑白' : '当前：书房暖纸'">
            <button :class="{ active: theme === 'paper' }" @click="setTheme('paper')">暖纸</button>
            <button :class="{ active: theme === 'minimal' }" @click="setTheme('minimal')">极简</button>
          </div>
          <el-button link title="设置（外部数据源）" @click="router.push('/settings')">
            <el-icon><Setting /></el-icon>
          </el-button>
          <el-dropdown @command="onCommand">
            <div class="user">
              <div class="avatar">{{ avatarText }}</div>
              <span class="user-name">{{ userStore.displayName }}</span>
              <el-icon><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人设置</el-dropdown-item>
                <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="content">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </section>
    </main>

    <!-- 个人设置 -->
    <el-dialog v-model="profileVisible" title="个人设置" width="420px">
      <el-form label-width="90px">
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="怎么称呼你" />
        </el-form-item>
        <el-form-item label="考试日期">
          <el-date-picker
            v-model="form.examDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="选择 CET-6 考试日期"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="目标分数">
          <el-input-number v-model="form.targetScore" :min="200" :max="710" :step="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { useTheme } from '@/composables/theme'

const { theme, setTheme } = useTheme()

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/**
 * 移动端侧栏抽屉开关。
 * <p>
 * 桌面端（>768px）侧栏常驻，此状态不产生任何视觉影响（见样式里的媒体查询）；
 * 仅 ≤768px 时生效，由顶栏汉堡按钮控制展开/收起。
 */
const sidebarOpen = ref(false)

// 切换路由后自动收起抽屉，否则新页面会被遮罩挡住
watch(
  () => route.path,
  () => {
    sidebarOpen.value = false
  }
)

const menus = [
  { path: '/dashboard', name: '今日学习', icon: 'HomeFilled' },
  { path: '/words', name: '背单词', icon: 'Notebook' },
  { path: '/quiz', name: '单词自测', icon: 'EditPen' },
  { path: '/reading', name: '悦读', icon: 'Reading' },
  { path: '/listening', name: '听力', icon: 'Headset' },
  { path: '/writing', name: '写作', icon: 'Brush' },
  { path: '/translation', name: '翻译', icon: 'Document' },
  { path: '/dict', name: '查词', icon: 'Search' },
  { path: '/wrong', name: '错词本', icon: 'WarningFilled' },
  { path: '/exam', name: '真题模考', icon: 'Trophy' },
  { path: '/stats', name: '学习统计', icon: 'DataLine' },
  { path: '/settings', name: '设置', icon: 'Setting' }
]

/**
 * 菜单高亮判断用前缀匹配而不是全等
 * <p>
 * 悦读有 /reading 和 /reading/:id 两个路由，全等匹配时打开阅读器
 * 侧边栏会「掉高亮」。前缀匹配兼顾了这两个路径，也不会误伤别的菜单项
 * （各菜单路径互不为前缀）。
 */
function isActive(path) {
  return route.path === path || route.path.startsWith(path + '/')
}

const profileVisible = ref(false)
const saving = ref(false)
const form = reactive({ nickname: '', examDate: '', targetScore: 425 })

const todayText = computed(() => {
  const d = new Date()
  const week = ['日', '一', '二', '三', '四', '五', '六'][d.getDay()]
  return `${d.getMonth() + 1}月${d.getDate()}日 周${week}`
})

const avatarText = computed(() => {
  const n = userStore.displayName || '同学'
  return n.slice(-1)
})

const examDate = computed(() => userStore.profile?.examDate || '')
const daysToExam = computed(() => {
  const d = userStore.profile?.examDate
  if (!d) return null
  return Math.max(0, Math.ceil((new Date(d + 'T00:00:00') - new Date().setHours(0, 0, 0, 0)) / 86400000))
})

watch(profileVisible, (v) => {
  if (v) {
    form.nickname = userStore.profile?.nickname || ''
    form.examDate = userStore.profile?.examDate || ''
    form.targetScore = userStore.profile?.targetScore || 425
  }
})

async function save() {
  saving.value = true
  try {
    await userStore.saveProfile({ ...form })
    ElMessage.success('已保存')
    profileVisible.value = false
  } finally {
    saving.value = false
  }
}

function onCommand(cmd) {
  if (cmd === 'profile') {
    profileVisible.value = true
  } else if (cmd === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  // 刷新页面后同步一次服务端资料（考试日期可能被其他端改过）
  userStore.fetchProfile().catch(() => {})
})
</script>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  width: 224px;
  flex-shrink: 0;
  background: var(--c6-sidebar);
  border-right: 1px solid var(--c6-border);
  display: flex;
  flex-direction: column;
  padding: 20px 14px;
}

.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 6px 20px;
}

.logo-mark {
  width: 38px;
  height: 38px;
  border-radius: 11px;
  background: var(--c6-primary);
  color: #fff;
  font-weight: 700;
  font-size: 19px;
  font-family: var(--c6-num);
  display: flex;
  align-items: center;
  justify-content: center;
}

.logo-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}

.logo-sub {
  font-size: 10px;
  color: var(--c6-text-sub);
  letter-spacing: 1px;
}

.nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex: 1;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 42px;
  padding: 0 14px;
  border-radius: 10px;
  color: #5c5140;
  text-decoration: none;
  font-size: 14px;
  transition: background 0.15s, color 0.15s;
}

.nav-item:hover {
  background: var(--c6-fill);
}

.nav-item.active {
  background: var(--c6-primary);
  color: #fff;
  font-weight: 600;
}

.nav-icon {
  font-size: 16px;
}

.sidebar-foot {
  padding-top: 12px;
}

.cd-box {
  background: var(--c6-fill);
  border: 1px solid var(--c6-border);
  border-radius: 12px;
  padding: 14px 16px;
}

.cd-label {
  font-size: 12px;
  color: var(--c6-text-sub);
}

.cd-num {
  font-size: 30px;
  font-weight: 700;
  color: var(--c6-primary);
  line-height: 1.15;
  font-family: var(--c6-num);
  font-variant-numeric: lining-nums tabular-nums;
}

.cd-unit {
  font-size: 11px;
  color: var(--c6-text-sub);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.header {
  height: 64px;
  flex-shrink: 0;
  background: var(--c6-header);
  border-bottom: 1px solid var(--c6-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.header-left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  margin: 0;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.user {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--c6-primary);
  color: #fff;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.user-name {
  font-size: 14px;
  color: var(--c6-text);
}

/* 主题一键切换：胶囊分段控件，激活态走主题色（暖纸=赭石，极简=墨黑） */
.theme-switch {
  display: inline-flex;
  align-items: center;
  border: 1px solid var(--c6-border);
  border-radius: 999px;
  padding: 2px;
  background: var(--c6-card);
  user-select: none;
}

.theme-switch button {
  border: none;
  background: transparent;
  font-size: 12px;
  line-height: 1;
  padding: 5px 11px;
  border-radius: 999px;
  cursor: pointer;
  color: var(--c6-text-sub);
  font-family: var(--c6-sans);
  transition: background 0.15s, color 0.15s;
}

.theme-switch button.active {
  background: var(--c6-primary);
  color: #fff;
}

.content {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px 32px;
}

/* ============================================================
 * 极简黑白主题下的侧边栏与顶栏覆写
 * 侧栏近黑、导航项浅灰、激活态白底墨字；文字颜色整体翻转，
 * 否则深色 body 文字在黑底侧栏上会看不见。
 * ============================================================ */
[data-theme='minimal'] .sidebar {
  color: #b3b3b3;
  border-right: 1px solid #2a2a2a;
}

[data-theme='minimal'] .logo-title {
  color: #ededea;
  font-weight: 600;
}

[data-theme='minimal'] .logo-sub {
  color: #7e7e7e;
}

[data-theme='minimal'] .logo-mark {
  background: #ffffff;
  color: #161616;
}

[data-theme='minimal'] .nav-item {
  color: #b3b3b3;
}

[data-theme='minimal'] .nav-item:hover {
  background: #242424;
}

[data-theme='minimal'] .nav-item.active {
  background: #ffffff;
  color: #161616;
}

[data-theme='minimal'] .nav-icon {
  color: inherit;
}

[data-theme='minimal'] .cd-box {
  background: #242424;
  border-color: #2e2e2e;
}

[data-theme='minimal'] .cd-label,
[data-theme='minimal'] .cd-unit {
  color: #8a8a8a;
}

[data-theme='minimal'] .cd-num {
  color: #ffffff;
}

[data-theme='minimal'] .avatar {
  background: #ffffff;
  color: #161616;
}

/* ============================================================
 * 移动端适配（≤768px）
 * 侧栏由「固定 224px 占位」改为「可滑出的抽屉」：
 *   - 默认 translateX(-100%) 移出视口，不占空间；
 *   - 点顶栏汉堡按钮加 .sidebar-open，滑入 + 背板遮罩。
 * 桌面端以下规则全部不生效，布局与交互保持原样。
 * ============================================================ */
.menu-btn {
  display: none;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  flex-shrink: 0;
  border: 1px solid var(--c6-border);
  border-radius: 8px;
  background: var(--c6-card);
  color: var(--c6-text);
  cursor: pointer;
  transition: background 0.15s;
}

.menu-btn:hover {
  background: var(--c6-fill);
}

.sidebar-backdrop {
  display: none;
}

@media (max-width: 768px) {
  .menu-btn {
    display: inline-flex;
  }

  .sidebar {
    position: fixed;
    top: 0;
    left: 0;
    bottom: 0;
    z-index: 60;
    width: 248px;
    transform: translateX(-100%);
    transition: transform 0.24s ease;
    box-shadow: 0 10px 34px rgba(56, 43, 26, 0.22);
    overflow-y: auto;
  }

  .layout.sidebar-open .sidebar {
    transform: translateX(0);
  }

  .sidebar-backdrop {
    display: block;
    position: fixed;
    inset: 0;
    z-index: 50;
    background: rgba(38, 30, 18, 0.42);
  }

  .header {
    padding: 0 14px;
  }

  .header-left {
    gap: 8px;
    min-width: 0;
  }

  .page-title {
    font-size: 16px;
    white-space: nowrap;
  }

  /* 窄屏空间有限：隐藏日期与用户名，只保留头像，避免顶栏拥挤换行 */
  .header-left .c6-sub,
  .user-name {
    display: none;
  }

  .header-right {
    gap: 10px;
  }

  .content {
    padding: 14px 14px 24px;
  }
}

@media (max-width: 480px) {
  .theme-switch button {
    padding: 5px 9px;
    font-size: 11px;
  }
}
</style>
