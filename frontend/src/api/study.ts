import api from './request'
import type { MindMapResponse, TodayReviewResponse, ReviewRecord, QuizStartResponse, QuizSubmitResponse, StatsResponse, StudyPlan, LearningPathResponse, MistakesResponse, WeaknessAnalysisResponse, QuizMode, QuizAnswer, ReportResponse, ReportRequest, ShareResponse, ShareRequest, PublicReportResponse } from '@/types'

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

  submitReview: (wordId: number, result: string): Promise<ReviewRecord> => {
    return api.post('/reviews/submit', { wordId, result })
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
