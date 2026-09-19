/*
 * 主题切换（单例）
 *
 * 设计：全站颜色都收敛在 main.css 的 CSS 变量里。这里只负责在 <html> 上
 * 挂 / 摘 data-theme 属性，并持久化用户选择。真正的「另一套皮肤」是
 * main.css 里 [data-theme='minimal'] 那一整块变量覆写 —— 不动任何组件
 * 结构，组件永远只引用变量。
 *
 * 两个主题：
 *   - 'paper'   书房暖纸（默认，衬线英文 + 赭石色）
 *   - 'minimal' 极简黑白（无衬线 + 纯黑/米白低对比灰阶，面向长篇英文阅读）
 *
 * 模块级单例：所有组件 import 到的是同一个 theme ref，一处切换处处同步。
 * applyTheme() 在模块加载时立即执行一次，配合 main.js 的提前引入，
 * 保证首屏渲染前 data-theme 就已就位，不会先闪一下暖纸再跳极简。
 */
import { ref } from 'vue'

const STORAGE_KEY = 'c6_theme'
export const THEMES = ['paper', 'minimal']

const theme = ref(THEMES.includes(localStorage.getItem(STORAGE_KEY)) ? localStorage.getItem(STORAGE_KEY) : 'paper')

function applyTheme() {
  const root = document.documentElement
  if (theme.value === 'minimal') {
    root.setAttribute('data-theme', 'minimal')
  } else {
    root.removeAttribute('data-theme')
  }
}

// 首屏前应用，避免闪烁
applyTheme()

export function useTheme() {
  function setTheme(next) {
    if (!THEMES.includes(next)) return
    theme.value = next
    localStorage.setItem(STORAGE_KEY, next)
    applyTheme()
  }
  function toggleTheme() {
    setTheme(theme.value === 'minimal' ? 'paper' : 'minimal')
  }
  return { theme, setTheme, toggleTheme }
}
