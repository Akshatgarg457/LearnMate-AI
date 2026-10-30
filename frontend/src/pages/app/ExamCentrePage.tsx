import { ClipboardCheck, CodeXml, Info, Timer } from 'lucide-react'
import { LinkCard } from '../../components/cards'
import { PageHeader } from '../../components/ui'

export default function ExamCentrePage() {
  return (
    <>
      <PageHeader eyebrow="Exam Centre" title="Your practice workspace" description="Pick a workflow. Scores and code results always come from the server, and sample modes are clearly labelled." />
      <div className="exam-tiles">
        <LinkCard index={0} to="/exam-centre/code-studio" icon={CodeXml} tone="violet" title="Code Studio" text="Read the problem, write a solution in the editor, then run sample tests or submit." meta={['Java', 'Python', 'C++']} cta="Open Code Studio" />
        <LinkCard index={1} to="/exam-centre/quick-revision" icon={Timer} tone="blue" title="Quick Revision" text="Short multiple-choice quizzes by subject and difficulty, timed by default." meta={['Timed', 'Untimed practice']} cta="Start a quiz" />
        <LinkCard index={2} to="/exam-centre/mock-exams" icon={ClipboardCheck} tone="green" title="Mock Exams" text="Full-length timed exams with a question navigator and topic-wise results." meta={['Mark for review', 'Auto-submit']} cta="Browse mock exams" />
      </div>
      <p className="exam-note">
        <Info size={16} aria-hidden="true" />
        <span>Code only ever runs in an isolated server-side sandbox, never in your browser. If that service isn&apos;t connected, Code Studio tells you instead of showing results.</span>
      </p>
    </>
  )
}
