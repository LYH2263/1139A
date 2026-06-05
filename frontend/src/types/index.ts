export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface UserInfo {
  id: number
  username: string
  email?: string
  role: string
}

export interface LoginRequest {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
  user: UserInfo
}

export interface RegisterRequest {
  username: string
  password: string
  email?: string
}

export interface Word {
  id: number
  word: string
  phonetic?: string
  pos?: string
  meaning: string
  example?: string
  memoryTip?: string
  createdAt?: string
}

export interface WordListResponse {
  list: Word[]
  total: number
  page: number
  size: number
}

export interface MindMapNode {
  id: number
  word: string
  meaning: string
  category?: string
  depth: number
}

export interface MindMapEdge {
  source: number
  target: number
  relationType: string
  label: string
}

export interface MindMapResponse {
  centerWord: MindMapNode
  nodes: MindMapNode[]
  edges: MindMapEdge[]
}

export interface PathWordNode {
  id: number
  word: string
  meaning: string
  category?: string
  proficiency: number
  mastered: boolean
}

export interface PathEdge {
  source: number
  target: number
  relationType: string
  label: string
  weight: number
}

export interface LearningPathResponse {
  nodes: PathWordNode[]
  edges: PathEdge[]
  unmasteredNodes: PathWordNode[]
  recommendedPath: PathEdge[]
}

export type ReviewMode = 'CARD' | 'LIST' | 'DICTATION'

export interface ReviewRecord {
  id: number
  wordId: number
  word: string
  phonetic?: string
  meaning: string
  example?: string
  result: string
  proficiency: number
  nextReviewAt?: string
  createdAt?: string
  consecutiveKnown?: number
  consecutiveUnknown?: number
  marked?: boolean
  markedResult?: string
}

export interface TodayReviewResponse {
  list: ReviewRecord[]
  total: number
  sessionId: string
  reviewMode: ReviewMode
}

export interface SubmitReviewResponse {
  id: number
  wordId: number
  word: string
  meaning: string
  result: string
  proficiency: number
  nextReviewAt: string
  createdAt: string
  removedFromQueue: boolean
  addedToQueueEnd: boolean
  nextWord: ReviewRecord
}

export interface ReviewSettingsRequest {
  reviewMode: ReviewMode
}

export interface ReviewSettingsResponse {
  reviewMode: ReviewMode
}

export interface SessionStats {
  reviewedCount: number
  knownCount: number
  unknownCount: number
  vagueCount: number
  knownRate: number
  remainingCount: number
  estimatedRemainingSeconds: number
}

export type QuizMode = 'CHOICE' | 'SPELLING'
export type AnswerResult = 'CORRECT' | 'PARTIAL' | 'WRONG' | 'TIMEOUT'

export interface QuizQuestion {
  wordId: number
  word: string
  type: string
  question: string
  options: string[]
  correctAnswer: string
  hint: string
  meaning: string
}

export interface QuizStartResponse {
  quizId: string
  mode: QuizMode
  questions: QuizQuestion[]
}

export interface QuizAnswer {
  wordId: number
  answer: string
  timedOut?: boolean
}

export interface AnswerDetail {
  wordId: number
  word: string
  meaning: string
  userAnswer: string
  correctAnswer: string
  result: AnswerResult
  feedback: string
}

export interface QuizSubmitResponse {
  score: number
  correctCount: number
  partialCount: number
  wrongCount: number
  totalCount: number
  duration: number
  wrongWords: Word[]
  answerDetails: AnswerDetail[]
  mode: QuizMode
  accuracy: number
  choiceAccuracy: number | null
  spellingAccuracy: number | null
}

export interface AccuracyPoint {
  date: string
  accuracy: number
  attempts: number
}

export interface MistakeItem {
  wordId: number
  word: string
  phonetic?: string
  pos?: string
  meaning: string
  example?: string
  errorCount: number
  lastErrorTime: string
  accuracy: number
  totalAttempts: number
  accuracyTrend: AccuracyPoint[]
  inTodayReview: boolean
}

export interface MistakesResponse {
  list: MistakeItem[]
  total: number
  page: number
  size: number
}

export interface PosDistribution {
  pos: string
  posName: string
  errorCount: number
  totalCount: number
  errorRate: number
}

export interface RelationTypeDistribution {
  relationType: string
  typeName: string
  errorCount: number
  totalCount: number
  errorRate: number
}

export interface WeaknessAnalysisResponse {
  posDistribution: PosDistribution[]
  relationTypeDistribution: RelationTypeDistribution[]
  weakPos: string[]
  weakRelationTypes: string[]
  overallSuggestion: string
}

export interface StatsResponse {
  totalWords: number
  todayReviewCount: number
  accuracy: number
  streakDays: number
}

export interface StudyPlan {
  id: number
  wordId: number
  word: string
  meaning: string
  planType: string
  createdAt: string
}

export interface WordErrorItem {
  wordId: number
  word: string
  meaning: string
  errorCount: number
}

export interface ReportResponse {
  startDate: string
  endDate: string
  totalStudyMinutes: number
  reviewedWordsCount: number
  quizCount: number
  averageScore: number
  proficiencyDistribution: Record<number, number>
  topErrorWords: WordErrorItem[]
  continuityScore: number
  username: string
}

export interface ReportRequest {
  timeRange: 'WEEK' | 'MONTH' | 'CUSTOM'
  startDate?: string
  endDate?: string
}

export interface ShareRequest {
  startDate: string
  endDate: string
  reportData: string
}

export interface ShareResponse {
  token: string
  shareUrl: string
  expiresAt: string
}

export interface PublicReportResponse {
  reportData: string
  startDate: string
  endDate: string
  username: string
  createdAt: string
}
