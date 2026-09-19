import request from './request'

/** 认证 */
export const authApi = {
  register: (data) => request.post('/auth/register', data),
  login: (data) => request.post('/auth/login', data),
  me: () => request.get('/auth/me'),
  updateProfile: (data) => request.put('/auth/profile', data)
}

/** 今日学习 */
export const dashboardApi = {
  get: () => request.get('/dashboard')
}

/** 背单词 */
export const wordApi = {
  today: (limit = 20) => request.get('/word/today', { params: { limit } }),
  review: (limit = 35) => request.get('/word/review', { params: { limit } }),
  grade: (data) => request.post('/word/grade', data),
  progress: () => request.get('/word/progress'),
  page: (params) => request.get('/word/page', { params })
}

/** 查词 */
export const dictApi = {
  lookup: (word) => request.get('/dict/lookup', { params: { word } }),
  suggest: (q, limit = 10) => request.get('/dict/suggest', { params: { q, limit } }),
  hot: (limit = 20) => request.get('/dict/hot', { params: { limit } }),
  recent: (limit = 20) => request.get('/dict/recent', { params: { limit } }),
  stats: () => request.get('/dict/stats'),
  addWrong: (wordId) => request.post('/dict/wrong', null, { params: { wordId } })
}

/** 单词自测 */
export const quizApi = {
  generate: (count = 10) => request.get('/quiz/generate', { params: { count } }),
  submit: (data) => request.post('/quiz/submit', data),
  history: (limit = 10) => request.get('/quiz/history', { params: { limit } })
}

/** 悦读 */
export const readingApi = {
  list: (params) => request.get('/reading/list', { params }),
  stats: () => request.get('/reading/stats'),
  detail: (id) => request.get(`/reading/${id}`),
  /** 记录一次打开（打开次数 +1） */
  open: (id) => request.post(`/reading/${id}/open`),
  /** 上报进度与新增阅读秒数 */
  progress: (data) => request.post('/reading/progress', data)
}

/** 错词本 */
export const wrongApi = {
  list: (mastered) => request.get('/wrong/list', { params: { mastered } }),
  master: (id, mastered = true) => request.post('/wrong/master', null, { params: { id, mastered } }),
  remove: (id) => request.delete(`/wrong/${id}`)
}

/** 统计 */
export const statsApi = {
  overview: (days = 30) => request.get('/stats/overview', { params: { days } })
}

/** 翻译（中译英） */
export const translationApi = {
  random: (count = 1) => request.get('/translation/random', { params: { count } }),
  submit: (data) => request.post('/translation/submit', data)
}

/** 写作（命题作文） */
export const writingApi = {
  random: (count = 1) => request.get('/writing/random', { params: { count } }),
  submit: (data) => request.post('/writing/submit', data)
}

/** 听力 */
export const listeningApi = {
  random: (count = 1) => request.get('/listening/random', { params: { count } }),
  submit: (data) => request.post('/listening/submit', data)
}

/** 真题模考 */
export const examApi = {
  prepare: () => request.get('/exam/prepare'),
  finish: (data) => request.post('/exam/finish', data)
}

/** 设置（外部数据源：词典 / 发音 / 翻译） */
export const settingsApi = {
  get: () => request.get('/settings'),
  update: (data) => request.put('/settings', data),
  /** 连通性测试，type ∈ dict | pron | translate */
  test: (type) => request.post('/settings/test', null, { params: { type } })
}
