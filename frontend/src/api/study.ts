import api from './request'
import type { 
  MindMapResponse, 
  TodayReviewResponse, 
  QuizStartResponse, 
  QuizSubmitResponse, 
  StatsResponse, 
  StudyPlan, 
  LearningPathResponse, 
  MistakesResponse, 
  WeaknessAnalysisResponse, 
  QuizMode, 
  QuizAnswer, 
  ReportResponse, 
  ReportRequest, 
  ShareResponse, 
  ShareRequest, 
  PublicReportResponse,
  SubmitReviewResponse,
  SessionStats,
  ReviewSettingsResponse,
  StudySchedule,
  StudyScheduleDetail,
  TodayScheduleResponse,
  ScheduleListResponse,
  CreateScheduleRequest,
  UpdateScheduleRequest,
  CompleteTodayRequest,
  CalendarMonthResponse,
  CalendarDayDetail
} from '@/types'

export const mindMapApi = {
  getMindMap: (wordId: number, depth: number = 1): Promise<MindMapResponse> => {
    return api.get(`/mindmap/${wordId}`, { params: { depth } })
  },
  getLearningPath: (wordId: number, depth: number = 2): Promise<LearningPathResponse> => {
    return api.get(`/mindmap/${wordId}/learning-path`, { params: { depth } })
  }
}

export const reviewApi = {
  getTodayReviews: (): Promise<TodayReviewResponse> => {
    return api.get('/reviews/today')
  },

  getWordBookReviews: (wordBookId: number): Promise<TodayReviewResponse> => {
    return api.get(`/reviews/wordbook/${wordBookId}`)
  },

  submitReview: (wordId: number, result: string, sessionId?: string): Promise<SubmitReviewResponse> => {
    return api.post('/reviews/submit', { wordId, result, sessionId })
  },

  getSessionStats: (sessionId: string): Promise<SessionStats> => {
    return api.get('/reviews/stats', { params: { sessionId } })
  },

  getSettings: (): Promise<ReviewSettingsResponse> => {
    return api.get('/reviews/settings')
  },

  saveSettings: (reviewMode: string): Promise<ReviewSettingsResponse> => {
    return api.put('/reviews/settings', { reviewMode })
  },

  getCalendarMonth: (month: string): Promise<CalendarMonthResponse> => {
    return api.get('/reviews/calendar', { params: { month } })
  },

  getCalendarDayDetail: (date: string): Promise<CalendarDayDetail> => {
    return api.get('/reviews/calendar/day', { params: { date } })
  }
}

export const quizApi = {
  startQuiz: (count: number = 10, mode: QuizMode = 'CHOICE'): Promise<QuizStartResponse> => {
    return api.post('/quiz/start', null, { params: { count, mode } })
  },

  submitQuiz: (quizId: string, answers: QuizAnswer[]): Promise<QuizSubmitResponse> => {
    return api.post('/quiz/submit', { quizId, answers })
  },

  getMistakes: (page: number = 0, size: number = 10, pos?: string): Promise<MistakesResponse> => {
    const params: Record<string, any> = { page, size }
    if (pos) params.pos = pos
    return api.get('/quiz/mistakes', { params })
  },

  getWeaknessAnalysis: (): Promise<WeaknessAnalysisResponse> => {
    return api.get('/quiz/weakness-analysis')
  }
}

export const statsApi = {
  getStats: (): Promise<StatsResponse> => {
    return api.get('/stats/me')
  },

  getStudyPlans: (): Promise<StudyPlan[]> => {
    return api.get('/study-records')
  },

  createStudyPlan: (wordId: number, planType: string): Promise<StudyPlan> => {
    return api.post('/study-plans', { wordId, planType })
  },

  generateReport: (request: ReportRequest): Promise<ReportResponse> => {
    return api.post('/stats/report', request)
  },

  createShare: (request: ShareRequest): Promise<ShareResponse> => {
    return api.post('/stats/share', request)
  },

  getPublicReport: (token: string): Promise<PublicReportResponse> => {
    return api.get(`/public/report/${token}`)
  }
}

export const scheduleApi = {
  createSchedule: (request: CreateScheduleRequest): Promise<StudySchedule> => {
    return api.post('/schedules', request)
  },

  updateSchedule: (id: number, request: UpdateScheduleRequest): Promise<StudySchedule> => {
    return api.put(`/schedules/${id}`, request)
  },

  deleteSchedule: (id: number): Promise<void> => {
    return api.delete(`/schedules/${id}`)
  },

  getSchedules: (): Promise<ScheduleListResponse> => {
    return api.get('/schedules')
  },

  getScheduleDetail: (id: number): Promise<StudyScheduleDetail> => {
    return api.get(`/schedules/${id}`)
  },

  getTodaySchedule: (id: number): Promise<TodayScheduleResponse> => {
    return api.get(`/schedules/${id}/today`)
  },

  getTodayAllSchedules: (): Promise<TodayScheduleResponse[]> => {
    return api.get('/schedules/today')
  },

  completeToday: (id: number, request: CompleteTodayRequest): Promise<TodayScheduleResponse> => {
    return api.post(`/schedules/${id}/complete`, request)
  }
}
