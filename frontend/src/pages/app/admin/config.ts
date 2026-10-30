import { ClipboardList, Coins, Flag, Library, Scale, Users, type LucideIcon } from 'lucide-react'

export type AdminSectionKey = 'users' | 'courses' | 'exams' | 'moderation' | 'disputes' | 'rewards'

export const adminSections: Record<AdminSectionKey, { title: string; description: string; icon: LucideIcon }> = {
  users: { title: 'User Management', description: 'Review accounts, suspend or reactivate access, and change roles. Permissions are enforced by the backend.', icon: Users },
  courses: { title: 'Course Management', description: 'Publish or unpublish courses in the Rewards Store. Only publish courses LearnMate is authorised to distribute.', icon: Library },
  exams: { title: 'Quiz & Exam Management', description: 'Switch quizzes and mock exams on or off for students.', icon: ClipboardList },
  moderation: { title: 'Community Moderation', description: 'Questions and answers reported by students.', icon: Flag },
  disputes: { title: 'Peer-Learning Disputes', description: 'Sessions a participant reported. Resolving a dispute here does not issue teaching rewards; use a reward adjustment if one is owed.', icon: Scale },
  rewards: { title: 'Rewards & Transactions', description: 'Adjust a student\u2019s points balance. Every adjustment is recorded by the backend with your reason.', icon: Coins },
}
