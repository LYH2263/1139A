import api from './request'
import type { Word, WordListResponse, WordExample, PendingRelationItem } from '@/types'

export const wordApi = {
  getWords: (params?: { keyword?: string; pos?: string; page?: number; size?: number }): Promise<WordListResponse> => {
    return api.get('/words', { params })
  },

  getWordById: (id: number): Promise<Word> => {
    return api.get(`/words/${id}`)
  },

  getWordExamples: (id: number): Promise<WordExample[]> => {
    return api.get(`/words/${id}/examples`)
  },

  createWord: (data: Partial<Word>): Promise<Word> => {
    return api.post('/admin/words', data)
  },

  updateWord: (id: number, data: Partial<Word>): Promise<Word> => {
    return api.put(`/admin/words/${id}`, data)
  },

  deleteWord: (id: number): Promise<void> => {
    return api.delete(`/admin/words/${id}`)
  },

  importWords: (file: File): Promise<{ importedCount: number; failedRows: string[] }> => {
    const formData = new FormData()
    formData.append('file', file)
    return api.post('/admin/words/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },
  getPendingRelations: (): Promise<PendingRelationItem[]> => {
    return api.get('/admin/relations/pending')
  },
  reviewRelation: (id: number, approved: boolean, rejectReason?: string): Promise<void> => {
    return api.post(`/admin/relations/${id}/review`, { approved, rejectReason })
  },
  adminCreateRelation: (data: { sourceWordId: number; targetWordId: number; relationType: string }) => {
    return api.post('/admin/relations', data)
  },
  adminDeleteRelation: (id: number): Promise<void> => {
    return api.delete(`/admin/relations/${id}`)
  }
}
