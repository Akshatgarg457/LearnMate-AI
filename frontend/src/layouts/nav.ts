import {
  Bot,
  ChartColumn,
  ClipboardCheck,
  ClipboardList,
  CodeXml,
  Coins,
  Flag,
  Flame,
  Gift,
  GraduationCap,
  History,
  LayoutDashboard,
  Library,
  MessagesSquare,
  NotebookPen,
  Scale,
  Settings,
  ShieldCheck,
  Timer,
  Users,
  UsersRound,
  type LucideIcon,
} from 'lucide-react'
import type { Role } from '../lib/types'

export type NavItem = { to: string; label: string; icon: LucideIcon; children?: NavItem[] }
export type NavGroup = { label: string; items: NavItem[] }

export const studentNavGroups: NavGroup[] = [
  {
    label: 'Learn',
    items: [
      { to: '/dashboard', label: 'Dashboard', icon: LayoutDashboard },
      { to: '/ai-tutor', label: 'AI Tutor', icon: Bot },
      {
        to: '/exam-centre',
        label: 'Exam Centre',
        icon: GraduationCap,
        children: [
          { to: '/exam-centre/code-studio', label: 'Code Studio', icon: CodeXml },
          { to: '/exam-centre/quick-revision', label: 'Quick Revision', icon: Timer },
          { to: '/exam-centre/mock-exams', label: 'Mock Exams', icon: ClipboardCheck },
        ],
      },
    ],
  },
  {
    label: 'Together',
    items: [
      { to: '/peer-learning', label: 'Peer Learning', icon: UsersRound },
      { to: '/notes', label: 'Notes Library', icon: NotebookPen },
      { to: '/community', label: 'Community', icon: MessagesSquare },
    ],
  },
  {
    label: 'Growth',
    items: [
      { to: '/progress', label: 'Progress Centre', icon: Flame },
      { to: '/rewards', label: 'Rewards Store', icon: Gift },
      { to: '/history', label: 'Learning History', icon: History },
    ],
  },
  { label: 'Account', items: [{ to: '/settings', label: 'Profile & Settings', icon: Settings }] },
]

/** Administrators get their own console navigation, not the student list with an extra link. */
export const adminNavGroups: NavGroup[] = [
  {
    label: 'Console',
    items: [
      { to: '/admin', label: 'Admin Overview', icon: ShieldCheck },
      { to: '/admin/analytics', label: 'Platform Analytics', icon: ChartColumn },
    ],
  },
  {
    label: 'Manage',
    items: [
      { to: '/admin/users', label: 'User Management', icon: Users },
      { to: '/admin/courses', label: 'Course Management', icon: Library },
      { to: '/admin/exams', label: 'Quiz & Exam Management', icon: ClipboardList },
    ],
  },
  {
    label: 'Moderation',
    items: [
      { to: '/admin/moderation', label: 'Community Moderation', icon: Flag },
      { to: '/admin/disputes', label: 'Peer-Learning Disputes', icon: Scale },
      { to: '/admin/rewards', label: 'Rewards & Transactions', icon: Coins },
    ],
  },
  { label: 'Account', items: [{ to: '/admin/settings', label: 'Account Settings', icon: Settings }] },
]

export function navFor(role: Role): NavGroup[] {
  return role === 'ADMIN' ? adminNavGroups : studentNavGroups
}

/** Every known route label, used by breadcrumbs. */
export const labelByPath: Record<string, string> = Object.fromEntries(
  [...studentNavGroups, ...adminNavGroups].flatMap((g) => g.items.flatMap((i) => [[i.to, i.label], ...(i.children ?? []).map((c) => [c.to, c.label])])),
)
