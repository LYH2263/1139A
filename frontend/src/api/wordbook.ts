import api from './request'
import type {
  WordBook,
  WordBookListResponse,
  UserWordBook,
  WordBookProgress,
  WordBookCreateRequest,
  WordBookUpdateRequest,
  WordBatchRequest,
  Word
} from '@/types'

export const wordBookApi = {
  getWordBooks: (params?: {
    keyword?: string
    difficulty?: string
    page?: number
    size?: number
  }): Promise<WordBookListResponse> => {
    return api.get('/wordbooks', { params })
  },

  getWordBookById: (id: number): Promise<WordBook> => {
    return api.get(`/wordbooks/${id}`)
  },

  getWordBookWords: (id: number): Promise<Word[]> => {
    return api.get(`/wordbooks/${id}/words`)
  },

  enrollWordBook: (id: number): Promise<void> => {
    return api.post(`/wordbooks/${id}/enroll`)
  },

  unenrollWordBook: (id: number): Promise<void> => {
    return api.delete(`/wordbooks/${id}/enroll`)
  },

  getMyWordBooks: (): Promise<UserWordBook[]> => {
    return api.get('/wordbooks/my')
  },

  getMyWordBookProgress: (): Promise<WordBookProgress[]> => {
    return api.get('/wordbooks/my/progress')
  },

  createWordBook: (data: WordBookCreateRequest): Promise<WordBook> => {
    return api.post('/admin/wordbooks', data)
  },

  updateWordBook: (id: number, data: WordBookUpdateRequest): Promise<WordBook> => {
    return api.put(`/admin/wordbooks/${id}`, data)
  },

  deleteWordBook: (id: number): Promise<void> => {
    return api.delete(`/admin/wordbooks/${id}`)
  },

  addWordsToWordBook: (id: number, data: WordBatchRequest): Promise<void> => {
    return api.post(`/admin/wordbooks/${id}/words`, data)
  },

  removeWordsFromWordBook: (id: number, data: WordBatchRequest): Promise<void> => {
    return api.delete(`/admin/wordbooks/${id}/words`, { data })
  }
}
