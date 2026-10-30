import type { CSSProperties } from 'react'
import { Link } from 'react-router-dom'
import {
  ArrowRight,
  Bot,
  Brain,
  CircleCheck,
  ClipboardCheck,
  CodeXml,
  Flame,
  Gift,
  LayoutDashboard,
  MessageCircle,
  NotebookPen,
  Play,
  ShieldCheck,
  Sparkles,
  Timer,
  UsersRound,
  type LucideIcon,
} from 'lucide-react'
import Button from '../components/Button'
import { useReveal } from '../hooks/useReveal'
import ThemeToggle from '../theme/ThemeToggle'

function Brand({ small }: { small?: boolean }) {
  return (
    <Link className="brand" to="/" aria-label="LearnMate AI home">
      <span className="brand-mark" aria-hidden="true">
        <Sparkles size={small ? 17 : 19} strokeWidth={2.2} />
      </span>
      <span>
        LearnMate <span className="brand-ai">AI</span>
      </span>
    </Link>
  )
}

type Feature = { title: string; text: string; icon: LucideIcon; tone: 'violet' | 'blue' | 'green' | 'amber'; size?: 'big' }
const features: Feature[] = [
  { title: 'Learn with an AI tutor', text: 'Get explanations, worked examples and practice questions when a topic feels difficult, with a tutor built around your learning journey.', icon: Brain, tone: 'violet', size: 'big' },
  { title: 'Get help from fellow students', text: 'Ask for a peer tutor, or teach someone else. Learning together is how many ideas finally click.', icon: UsersRound, tone: 'blue', size: 'big' },
  { title: 'Discover and share notes', text: 'Upload your notes and find the resources other students recommend.', icon: NotebookPen, tone: 'green' },
  { title: 'Ask the community', text: 'Bring questions to a student community where answers and follow-ups belong.', icon: MessageCircle, tone: 'amber' },
  { title: 'Practise with confidence', text: 'Quizzes, mock exams and coding practice, all in one Exam Centre.', icon: ClipboardCheck, tone: 'violet' },
]

const faqs: [string, string][] = [
  ['Is LearnMate AI live yet?', 'LearnMate AI runs on its own Spring Boot backend and MySQL database. Once both are running, register an account and sign in to use every screen with real data.'],
  ['Does my code run in the browser?', 'No. Code is sent to an isolated server-side judge (Judge0). When no judge is configured, Code Studio says so instead of showing pretend results.'],
  ['How are points and streaks decided?', 'By the server, never by this app. Only qualifying activity counts toward a streak (opening the site does not), and reward amounts are configured on the backend.'],
  ['Can I use it in light or dark mode?', 'Yes. Use the sun/moon button in the header. Your choice is remembered on this device.'],
]

/** Deterministic decorative pattern for the illustration (not real data). */
const heatCells = Array.from({ length: 7 * 22 }, (_, i) => {
  const n = (i * 37 + (i % 7) * 11 + Math.floor(i / 7) * 5) % 13
  return n < 4 ? 0 : n < 7 ? 1 : n < 9 ? 2 : n < 11 ? 3 : 4
})

function LandingPage() {
  useReveal()
  return (
    <div className="lp" id="top">
      <header className="lp-header">
        <div className="lp-wrap">
          <Brand />
          <nav className="lp-nav" aria-label="Main navigation">
            <a href="#features">Features</a>
            <a href="#exam-centre">Exam Centre</a>
            <a href="#ai-tutor">AI Tutor</a>
            <a href="#progress">Progress</a>
            <a href="#faq">FAQ</a>
          </nav>
          <div className="lp-actions">
            <ThemeToggle />
            <Button to="/login" variant="secondary">
              Sign in
            </Button>
            <Button to="/register">
              Get started <ArrowRight size={15} aria-hidden="true" />
            </Button>
          </div>
        </div>
      </header>

      <main>
        <section className="lp-hero" aria-labelledby="hero-title">
          <div className="lp-bg" aria-hidden="true" />
          <div className="lp-wrap">
            <div>
              <p className="eyebrow">
                <span className="eyebrow-dot" aria-hidden="true" />
                A learning community in the making
              </p>
              <h1 id="hero-title">
                Learn with AI.
                <br />
                <span className="grad-text">Grow with people.</span>
              </h1>
              <p className="lp-lede">Ask the AI tutor, get help from a fellow student, practise in the Exam Centre, and watch your progress build, all in one calm, focused workspace.</p>
              <div className="lp-cta-row">
                <Button to="/register">
                  Create your account <ArrowRight size={17} aria-hidden="true" />
                </Button>
                <Button to="/login" variant="secondary">
                  Sign in
                </Button>
              </div>
              <ul className="lp-trust">
                <li><CircleCheck size={15} aria-hidden="true" /> Light and dark themes</li>
                <li><CircleCheck size={15} aria-hidden="true" /> Honest about what is and isn&apos;t connected</li>
                <li><CircleCheck size={15} aria-hidden="true" /> Built for phones too</li>
              </ul>
            </div>

            <div className="lp-stage" aria-label="Illustration of the LearnMate dashboard">
              <div className="lp-window">
                <span className="lp-tag">Illustration</span>
                <div className="lp-chrome" aria-hidden="true"><i /><i /><i /></div>
                <div className="lp-body" aria-hidden="true">
                  <div className="lp-rail">
                    <span className="on"><LayoutDashboard size={17} /></span>
                    <span><Bot size={17} /></span>
                    <span><CodeXml size={17} /></span>
                    <span><Flame size={17} /></span>
                    <span><Gift size={17} /></span>
                  </div>
                  <div className="lp-main">
                    <span className="sk w1" />
                    <span className="sk w2" />
                    <div className="lp-tiles">
                      {([['violet', Flame], ['blue', CodeXml], ['green', Timer]] as const).map(([tone, Icon]) => (
                        <div className="lp-tile" key={tone}>
                          <span className={`feature-icon tone-${tone}`}><Icon size={15} /></span>
                          <span className="sk w3" />
                          <span className="sk w1" />
                        </div>
                      ))}
                    </div>
                    <div className="lp-chart">
                      {[38, 62, 46, 80, 58, 92, 70].map((h, i) => (
                        <span key={i} style={{ height: `${h}%`, '--i': i } as CSSProperties} />
                      ))}
                    </div>
                  </div>
                </div>
              </div>
              <div className="lp-float a" aria-hidden="true">
                <span className="feature-icon tone-violet" style={{ width: 32, height: 32 }}><Bot size={16} /></span>
                <span>AI tutor<small>Explains step by step</small></span>
              </div>
              <div className="lp-float b" aria-hidden="true">
                <span className="feature-icon tone-amber" style={{ width: 32, height: 32 }}><Flame size={16} /></span>
                <span>Streaks<small>Real activity only</small></span>
              </div>
            </div>
          </div>
        </section>

        <section className="lp-section" id="features">
          <div className="lp-wrap">
            <div className="lp-head">
              <p className="eyebrow">Everything in one place</p>
              <h2>Everything learning should make room for.</h2>
              <p>LearnMate brings study tools and a supportive student community together, so you can move from confusion to understanding without switching apps.</p>
            </div>
            <div className="lp-bento">
              {features.map(({ title, text, icon: Icon, tone, size }, i) => (
                <article className={`card reveal ${size ?? ''}`} data-reveal="" style={{ '--i': i } as CSSProperties} key={title}>
                  <span className={`feature-icon tone-${tone}`} aria-hidden="true">
                    <Icon size={20} strokeWidth={1.8} />
                  </span>
                  <h3>{title}</h3>
                  <p>{text}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="lp-section" id="exam-centre">
          <div className="lp-wrap lp-show">
            <div className="lp-show-copy reveal" data-reveal="">
              <p className="eyebrow">Exam Centre</p>
              <h2>Practise like it counts.</h2>
              <p>One workspace for coding practice, quick quizzes and full mock exams, designed to feel focused while the clock is running.</p>
              <ul className="lp-points">
                <li><CodeXml size={18} aria-hidden="true" /><span><strong>Code Studio.</strong> Java, Python and C++ with a split problem and editor view. Code runs in an isolated server-side sandbox, never in your browser.</span></li>
                <li><Timer size={18} aria-hidden="true" /><span><strong>Quick Revision.</strong> Timed multiple-choice quizzes by default, with untimed practice when you want it.</span></li>
                <li><ClipboardCheck size={18} aria-hidden="true" /><span><strong>Mock Exams.</strong> A question navigator, mark-for-review and topic-wise results. The server owns the clock and the score.</span></li>
              </ul>
              <Button to="/register">Try the Exam Centre <ArrowRight size={16} aria-hidden="true" /></Button>
            </div>
            <div className="lp-window reveal" data-reveal="" style={{ '--i': 1 } as CSSProperties} aria-label="Illustration of the Code Studio editor">
              <span className="lp-tag">Illustration</span>
              <div className="lp-tabs" aria-hidden="true"><span>Java</span><span className="on">Python</span><span>C++</span></div>
              <pre className="lp-code" aria-hidden="true">
                <span className="ln">1</span><span className="k">def</span> <span className="f">reverse</span>(s):{'\n'}
                <span className="ln">2</span>    <span className="c"># write your solution</span>{'\n'}
                <span className="ln">3</span>    <span className="k">return</span> s{'\n'}
                <span className="ln">4</span>{'\n'}
                <span className="ln">5</span><span className="f">print</span>(<span className="f">reverse</span>(<span className="f">input</span>())){'\n'}
              </pre>
              <div className="lp-run"><Play size={15} aria-hidden="true" /> Results come from the execution service, not the browser.</div>
            </div>
          </div>
        </section>

        <section className="lp-section" id="ai-tutor">
          <div className="lp-wrap lp-show flip">
            <div className="lp-show-copy reveal" data-reveal="">
              <p className="eyebrow">AI Tutor</p>
              <h2>A patient explainer, any time.</h2>
              <p>Ask a question in your own words and get an explanation you can build on, plus follow-up suggestions to keep going.</p>
              <ul className="lp-points">
                <li><Brain size={18} aria-hidden="true" /><span>Explanations, worked examples and practice questions.</span></li>
                <li><CodeXml size={18} aria-hidden="true" /><span>Code in replies is formatted, readable and copyable.</span></li>
                <li><ShieldCheck size={18} aria-hidden="true" /><span>If the AI service isn&apos;t available, LearnMate says so rather than pretending to answer.</span></li>
              </ul>
              <Button to="/register" variant="secondary">Meet the tutor <ArrowRight size={16} aria-hidden="true" /></Button>
            </div>
            <div className="lp-window reveal" data-reveal="" style={{ '--i': 1 } as CSSProperties} aria-label="Illustration of a tutor conversation">
              <span className="lp-tag">Illustration</span>
              <div className="lp-chrome" aria-hidden="true"><i /><i /><i /></div>
              <div className="lp-chat" aria-hidden="true">
                <div className="lp-bubble me">Can you explain recursion simply?</div>
                <div className="lp-bubble ai">Think of it as a function that solves a big problem by solving a smaller copy of the same problem, until it reaches a case simple enough to answer directly.</div>
                <div className="lp-bubble me">Show me a tiny example.</div>
              </div>
            </div>
          </div>
        </section>

        <section className="lp-section" id="peers">
          <div className="lp-wrap">
            <div className="lp-head">
              <p className="eyebrow">Peer learning</p>
              <h2>Ask, learn, then pass it on.</h2>
              <p>Great learners also teach. Peer sessions turn understanding into something you can share, and verified teaching can earn points.</p>
            </div>
            <div className="lp-steps">
              {[
                ['Ask for help', 'Post what you are stuck on and pick a topic. A fellow student can accept your request.'],
                ['Learn together', 'Meet in a session, work through the idea, then both confirm it happened and leave feedback.'],
                ['Teach and earn', 'When the platform verifies a teaching session, points are awarded by the server and can be redeemed for approved courses.'],
              ].map(([t, p], i) => (
                <article className="card lp-step reveal" data-reveal="" style={{ '--i': i } as CSSProperties} key={t}>
                  <h3>{t}</h3>
                  <p>{p}</p>
                </article>
              ))}
            </div>
          </div>
        </section>

        <section className="lp-section" id="progress">
          <div className="lp-wrap lp-show">
            <div className="lp-show-copy reveal" data-reveal="">
              <p className="eyebrow">Progress Centre</p>
              <h2>See your consistency build.</h2>
              <p>Streaks, XP, an activity calendar and achievements, based only on real, qualifying learning activity.</p>
              <ul className="lp-points">
                <li><Flame size={18} aria-hidden="true" /><span>Opening the site doesn&apos;t count. Solving a problem, finishing a quiz or a study session does.</span></li>
                <li><Sparkles size={18} aria-hidden="true" /><span><strong>XP</strong> tracks progression. <strong>Points</strong> are a separate currency for the Rewards Store.</span></li>
              </ul>
              <Button to="/register">Start your streak <ArrowRight size={16} aria-hidden="true" /></Button>
            </div>
            <div className="lp-window reveal" data-reveal="" style={{ '--i': 1 } as CSSProperties} aria-label="Illustration of the activity calendar">
              <span className="lp-tag">Illustration, not real data</span>
              <div className="lp-chrome" aria-hidden="true"><i /><i /><i /></div>
              <div className="lp-heat" aria-hidden="true">
                {heatCells.map((l, i) => (<i key={i} className={l ? `l${l}` : ''} />))}
              </div>
            </div>
          </div>
        </section>

        <section className="lp-section" id="faq">
          <div className="lp-wrap lp-faq">
            <div className="lp-head"><p className="eyebrow">Questions</p><h2>Good to know.</h2></div>
            {faqs.map(([q, a]) => (
              <details key={q}>
                <summary>{q}</summary>
                <p>{a}</p>
              </details>
            ))}
          </div>
        </section>

        <div className="lp-wrap">
          <section className="lp-final reveal" data-reveal="" aria-labelledby="final-title">
            <h2 id="final-title">Ready to make room for better learning?</h2>
            <p>Create an account, or sign in to continue where you left off. Nothing is loaded until you are signed in.</p>
            <div className="lp-cta-row">
              <Button to="/register">Create your account</Button>
              <Button to="/login" variant="secondary">Sign in</Button>
            </div>
          </section>
        </div>
      </main>

      <footer className="lp-footer">
        <div className="lp-wrap">
          <Brand small />
          <nav aria-label="Footer">
            <a href="#features">Features</a>
            <a href="#exam-centre">Exam Centre</a>
            <a href="#ai-tutor">AI Tutor</a>
            <Link to="/login">Sign in</Link>
            <Link to="/register">Register</Link>
          </nav>
          <small>A student-first learning platform, taking shape. Illustrations show the interface, not real users or results.</small>
        </div>
      </footer>
    </div>
  )
}

export default LandingPage
