import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: '登录',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: '今日学习',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: 'words',
        name: '背单词',
        component: () => import('@/views/Words.vue')
      },
      {
        path: 'quiz',
        name: '单词自测',
        component: () => import('@/views/Quiz.vue')
      },
      {
        path: 'reading',
        name: '悦读',
        component: () => import('@/views/Reading.vue')
      },
      {
        path: 'reading/:id',
        name: '精读',
        component: () => import('@/views/ReadingDetail.vue'),
        meta: { parent: '/reading' }
      },
      {
        path: 'listening',
        name: '听力',
        component: () => import('@/views/Listening.vue')
      },
      {
        path: 'writing',
        name: '写作',
        component: () => import('@/views/Writing.vue')
      },
      {
        path: 'translation',
        name: '翻译',
        component: () => import('@/views/Translation.vue')
      },
      {
        path: 'dict',
        name: '查词',
        component: () => import('@/views/Dict.vue')
      },
      {
        path: 'wrong',
        name: '错词本',
        component: () => import('@/views/WrongBook.vue')
      },
      {
        path: 'exam',
        name: '真题模考',
        component: () => import('@/views/Exam.vue')
      },
      {
        path: 'stats',
        name: '学习统计',
        component: () => import('@/views/Stats.vue')
      },
      {
        path: 'settings',
        name: '设置',
        component: () => import('@/views/Settings.vue')
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('c6_token')
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  return true
})

export default router
