import { lazy } from 'react'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import './App.css'
import './styles/app.css'
import './styles/system.css'
import { AuthProvider } from './auth/AuthProvider'
import AdminToHome from './auth/AdminToHome'
import ProtectedRoute from './auth/ProtectedRoute'
import { ToastProvider } from './toast/ToastProvider'
import { ThemeProvider } from './theme/ThemeProvider'
import AppLayout from './layouts/AppLayout'
import LandingPage from './pages/LandingPage'
import NotFoundPage from './pages/NotFoundPage'
import LoginPage from './pages/auth/LoginPage'
import AdminLoginPage from './pages/auth/AdminLoginPage'
import RegisterPage from './pages/auth/RegisterPage'
import ForbiddenPage from './pages/auth/ForbiddenPage'

// Feature pages are code-split so the landing page stays light.
const DashboardPage = lazy(() => import('./pages/app/DashboardPage'))
const AiTutorPage = lazy(() => import('./pages/app/AiTutorPage'))
const ExamCentrePage = lazy(() => import('./pages/app/ExamCentrePage'))
const CodeStudioPage = lazy(() => import('./pages/app/CodeStudioPage'))
const QuickRevisionPage = lazy(() => import('./pages/app/QuickRevisionPage'))
const MockExamsPage = lazy(() => import('./pages/app/MockExamsPage'))
const PeerLearningPage = lazy(() => import('./pages/app/PeerLearningPage'))
const NotesPage = lazy(() => import('./pages/app/NotesPage'))
const CommunityPage = lazy(() => import('./pages/app/CommunityPage'))
const RewardsPage = lazy(() => import('./pages/app/RewardsPage'))
const ProgressPage = lazy(() => import('./pages/app/ProgressPage'))
const HistoryPage = lazy(() => import('./pages/app/HistoryPage'))
const SettingsPage = lazy(() => import('./pages/app/SettingsPage'))
const AdminOverviewPage = lazy(() => import('./pages/app/admin/AdminOverviewPage'))
const AdminSectionPage = lazy(() => import('./pages/app/admin/AdminSectionPage'))
const AdminAnalyticsPage = lazy(() => import('./pages/app/admin/AdminAnalyticsPage'))

function App() {
  return (
    <ThemeProvider>
    <BrowserRouter>
      <AuthProvider>
        <ToastProvider>
          <Routes>
            <Route path="/" element={<LandingPage />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/admin/login" element={<AdminLoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forbidden" element={<ForbiddenPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<AppLayout />}>
                <Route path="/dashboard" element={<AdminToHome><DashboardPage /></AdminToHome>} />
                <Route path="/ai-tutor" element={<AiTutorPage />} />
                <Route path="/exam-centre" element={<ExamCentrePage />} />
                <Route path="/exam-centre/code-studio" element={<CodeStudioPage />} />
                <Route path="/exam-centre/quick-revision" element={<QuickRevisionPage />} />
                <Route path="/exam-centre/mock-exams" element={<MockExamsPage />} />
                <Route path="/peer-learning" element={<PeerLearningPage />} />
                <Route path="/notes" element={<NotesPage />} />
                <Route path="/community" element={<CommunityPage />} />
                <Route path="/rewards" element={<RewardsPage />} />
                <Route path="/progress" element={<ProgressPage />} />
                <Route path="/history" element={<HistoryPage />} />
                <Route path="/settings" element={<SettingsPage />} />
              </Route>
            </Route>
            <Route element={<ProtectedRoute role="ADMIN" loginPath="/admin/login" />}>
              <Route element={<AppLayout />}>
                <Route path="/admin" element={<AdminOverviewPage />} />
                <Route path="/admin/analytics" element={<AdminAnalyticsPage />} />
                <Route path="/admin/users" element={<AdminSectionPage section="users" />} />
                <Route path="/admin/courses" element={<AdminSectionPage section="courses" />} />
                <Route path="/admin/exams" element={<AdminSectionPage section="exams" />} />
                <Route path="/admin/moderation" element={<AdminSectionPage section="moderation" />} />
                <Route path="/admin/disputes" element={<AdminSectionPage section="disputes" />} />
                <Route path="/admin/rewards" element={<AdminSectionPage section="rewards" />} />
                <Route path="/admin/settings" element={<SettingsPage />} />
              </Route>
            </Route>
            <Route path="*" element={<NotFoundPage />} />
          </Routes>
        </ToastProvider>
      </AuthProvider>
    </BrowserRouter>
    </ThemeProvider>
  )
}

export default App
