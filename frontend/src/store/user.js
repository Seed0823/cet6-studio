import { defineStore } from 'pinia'
import { authApi } from '@/api'

/**
 * 用户状态
 * <p>
 * token 持久化在 localStorage，刷新页面不用重新登录。
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('c6_token') || '',
    profile: JSON.parse(localStorage.getItem('c6_user') || 'null')
  }),

  getters: {
    isLogin: (state) => !!state.token,
    displayName: (state) => state.profile?.nickname || state.profile?.username || '同学'
  },

  actions: {
    async login(form) {
      const res = await authApi.login(form)
      const { token, ...profile } = res.data
      this.token = token
      this.profile = profile
      localStorage.setItem('c6_token', token)
      localStorage.setItem('c6_user', JSON.stringify(profile))
      return profile
    },

    async register(form) {
      return authApi.register(form)
    },

    async fetchProfile() {
      const res = await authApi.me()
      this.profile = res.data
      localStorage.setItem('c6_user', JSON.stringify(res.data))
      return res.data
    },

    async saveProfile(form) {
      const res = await authApi.updateProfile(form)
      this.profile = { ...this.profile, ...res.data }
      localStorage.setItem('c6_user', JSON.stringify(this.profile))
      return res.data
    },

    logout() {
      this.token = ''
      this.profile = null
      localStorage.removeItem('c6_token')
      localStorage.removeItem('c6_user')
    }
  }
})
