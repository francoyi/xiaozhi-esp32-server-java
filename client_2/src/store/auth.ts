const TOKEN_KEY = 'xiaozhi_token'
const USER_ID_KEY = 'xiaozhi_user_id'

export const authStore = {
  getToken(): string {
    return localStorage.getItem(TOKEN_KEY) || ''
  },
  setToken(token: string) {
    localStorage.setItem(TOKEN_KEY, token)
  },
  clearToken() {
    localStorage.removeItem(TOKEN_KEY)
    localStorage.removeItem(USER_ID_KEY)
  },

  getUserId(): string {
    return localStorage.getItem(USER_ID_KEY) || ''
  },
  setUserId(userId: string | number | null | undefined) {
    if (userId === null || typeof userId === 'undefined' || String(userId).trim() === '') return
    localStorage.setItem(USER_ID_KEY, String(userId))
  },
}
