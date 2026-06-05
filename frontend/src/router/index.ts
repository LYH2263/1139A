import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true }
    },
    {
      path: '/public/report/:token',
      name: 'PublicReport',
      component: () => import('@/views/PublicReportView.vue'),
      meta: { public: true }
    },
    {
      path: '/',
      name: 'Layout',
      component: () => import('@/views/LayoutView.vue'),
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/DashboardView.vue'),
          meta: { title: '仪表盘' }
        },
        {
          path: 'words',
          name: 'Words',
          component: () => import('@/views/WordsView.vue'),
          meta: { title: '单词列表' }
        },
        {
          path: 'words/:id',
          name: 'WordDetail',
          component: () => import('@/views/WordDetailView.vue'),
          meta: { title: '单词详情' }
        },
        {
          path: 'wordbooks',
          name: 'WordBooks',
          component: () => import('@/views/WordBooksView.vue'),
          meta: { title: '词书中心' }
        },
        {
          path: 'mindmap',
          name: 'MindMap',
          component: () => import('@/views/MindMapView.vue'),
          meta: { title: '思维导图' }
        },
        {
          path: 'mindmap/:wordId',
          name: 'MindMapDetail',
          component: () => import('@/views/MindMapView.vue'),
          meta: { title: '思维导图' }
        },
        {
          path: 'review',
          name: 'Review',
          component: () => import('@/views/ReviewView.vue'),
          meta: { title: '今日复习' }
        },
        {
          path: 'quiz',
          name: 'Quiz',
          component: () => import('@/views/QuizView.vue'),
          meta: { title: '单词测验' }
        },
        {
          path: 'quiz/mistakes',
          name: 'QuizMistakes',
          component: () => import('@/views/QuizMistakesView.vue'),
          meta: { title: '错题本' }
        },
        {
          path: 'records',
          name: 'Records',
          redirect: '/records/plans',
          meta: { title: '学习记录' },
          children: [
            {
              path: 'plans',
              name: 'RecordsPlans',
              component: () => import('@/views/RecordsView.vue'),
              meta: { title: '学习计划' }
            },
            {
              path: 'report',
              name: 'RecordsReport',
              component: () => import('@/views/ReportView.vue'),
              meta: { title: '学习报告' }
            }
          ]
        },
        {
          path: 'admin',
          name: 'Admin',
          component: () => import('@/views/AdminView.vue'),
          meta: { title: '词库管理', admin: true }
        }
      ]
    }
  ]
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (!to.meta.public && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.admin && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router
