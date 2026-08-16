import { defineStore } from 'pinia'
import { login as loginApi, logout as logoutApi } from '../api/auth'

/** 用户会话（Pinia）：token 放 localStorage，刷新不丢 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('satoken') || '',
    userId: null,
    username: '',
    nickName: '',
    roleCode: ''
  }),
  actions: {
    async login(username, password) {
      const vo = await loginApi({ username, password })
      this.token = vo.token
      this.userId = vo.userId
      this.username = vo.username
      this.nickName = vo.nickName
      this.roleCode = vo.roleCode
      localStorage.setItem('satoken', vo.token)
    },
    async logout() {
      try { await logoutApi() } catch (e) { /* 忽略 */ }
      this.$reset()
      localStorage.removeItem('satoken')
    }
  }
})
