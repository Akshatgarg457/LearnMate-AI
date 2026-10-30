import { useEffect, useRef, useState, type FormEvent, type ReactNode } from 'react'
import { Bot, Check, Copy, MessageSquarePlus, Mic, MicOff, RefreshCw, Send, Sparkles, User, Volume2, VolumeX } from 'lucide-react'
import ResourceView from '../../components/ResourceView'
import { Btn, PageHeader } from '../../components/ui'
import { useLive } from '../../hooks/useLive'
import { useResource } from '../../hooks/useResource'
import { useToast } from '../../toast/toastContext'
import type { AiConversation, AiMessage, Page } from '../../lib/types'
import api, { describeError } from '../../services/api'

type Msg = { id: string; role: 'USER' | 'ASSISTANT'; text: string; failed?: boolean }

type Block =
  | { kind: 'code'; code: string }
  | { kind: 'heading'; text: string }
  | { kind: 'list'; ordered: boolean; items: string[] }
  | { kind: 'paragraph'; text: string }

type RecognitionResult = { 0: { transcript: string }; isFinal: boolean }
type RecognitionEvent = { results: { length: number; [index: number]: RecognitionResult } }
type Recognition = {
  lang: string
  interimResults: boolean
  continuous: boolean
  start: () => void
  stop: () => void
  onresult: ((e: RecognitionEvent) => void) | null
  onerror: (() => void) | null
  onend: (() => void) | null
}

const starters = ['Explain recursion with a simple example', 'What is the difference between a stack and a queue?', 'Quiz me on SQL joins']

/** Parses the subset of markdown the AI actually emits, so ### and ** never reach the screen. */
function parseBlocks(text: string): Block[] {
  const lines = text.replace(/\r\n/g, '\n').split('\n')
  const blocks: Block[] = []
  let paragraph: string[] = []
  let i = 0
  const flush = () => {
    if (paragraph.length) {
      blocks.push({ kind: 'paragraph', text: paragraph.join('\n').trim() })
      paragraph = []
    }
  }
  while (i < lines.length) {
    const line = lines[i]
    if (line.trimStart().startsWith('```')) {
      flush()
      const code: string[] = []
      i++
      while (i < lines.length && !lines[i].trimStart().startsWith('```')) code.push(lines[i++])
      i++
      blocks.push({ kind: 'code', code: code.join('\n') })
      continue
    }
    const heading = /^\s{0,3}#{1,6}\s+(.*)$/.exec(line)
    if (heading) {
      flush()
      blocks.push({ kind: 'heading', text: heading[1].trim() })
      i++
      continue
    }
    if (/^\s*[-*+]\s+/.test(line)) {
      flush()
      const items: string[] = []
      while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) items.push(lines[i++].replace(/^\s*[-*+]\s+/, ''))
      blocks.push({ kind: 'list', ordered: false, items })
      continue
    }
    if (/^\s*\d+[.)]\s+/.test(line)) {
      flush()
      const items: string[] = []
      while (i < lines.length && /^\s*\d+[.)]\s+/.test(lines[i])) items.push(lines[i++].replace(/^\s*\d+[.)]\s+/, ''))
      blocks.push({ kind: 'list', ordered: true, items })
      continue
    }
    if (/^\s*(?:-{3,}|\*{3,}|_{3,})\s*$/.test(line) || !line.trim()) {
      flush()
      i++
      continue
    }
    paragraph.push(line)
    i++
  }
  flush()
  return blocks
}

function inline(text: string, keyBase: string): ReactNode[] {
  const nodes: ReactNode[] = []
  const pattern = /(\*\*[^*]+\*\*|__[^_]+__|\*[^*\n]+\*|_[^_\n]+_|`[^`\n]+`)/g
  let last = 0
  let n = 0
  let match: RegExpExecArray | null
  while ((match = pattern.exec(text)) !== null) {
    if (match.index > last) nodes.push(text.slice(last, match.index))
    const token = match[0]
    const key = `${keyBase}-${n++}`
    if (token.startsWith('**') || token.startsWith('__')) nodes.push(<strong key={key}>{token.slice(2, -2)}</strong>)
    else if (token.startsWith('`')) nodes.push(<code key={key}>{token.slice(1, -1)}</code>)
    else nodes.push(<em key={key}>{token.slice(1, -1)}</em>)
    last = match.index + token.length
  }
  if (last < text.length) nodes.push(text.slice(last))
  return nodes
}

function speechText(text: string) {
  return text
    .replace(/```[\s\S]*?```/g, ' Code block. ')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/[*_#>]/g, '')
    .replace(/\s+/g, ' ')
    .trim()
}

function Rich({ text }: { text: string }) {
  const toast = useToast()
  return (
    <>
      {parseBlocks(text).map((block, i) => {
        if (block.kind === 'code') {
          return (
            <div className="code-block" key={i}>
              <button type="button" className="icon-btn" aria-label="Copy code" onClick={() => navigator.clipboard.writeText(block.code).then(() => toast('Code copied', 'success'), () => toast('Could not copy', 'error'))}>
                <Copy size={14} aria-hidden="true" />
              </button>
              <pre><code>{block.code}</code></pre>
            </div>
          )
        }
        if (block.kind === 'heading') return <p className="md-heading" key={i}>{inline(block.text, `h${i}`)}</p>
        if (block.kind === 'list') {
          const List = block.ordered ? 'ol' : 'ul'
          return (
            <List className="md-list" key={i}>
              {block.items.map((item, j) => <li key={j}>{inline(item, `l${i}-${j}`)}</li>)}
            </List>
          )
        }
        return <p key={i}>{inline(block.text, `p${i}`)}</p>
      })}
    </>
  )
}

export default function AiTutorPage() {
  const live = useLive()
  const toast = useToast()
  const [messages, setMessages] = useState<Msg[]>([])
  const [input, setInput] = useState('')
  const [busy, setBusy] = useState(false)
  const [conversationId, setConversationId] = useState<number | null>(null)
  const [copied, setCopied] = useState<string | null>(null)
  const [speakingId, setSpeakingId] = useState<string | null>(null)
  const [listening, setListening] = useState(false)
  const nextId = useRef(1)
  const bottom = useRef<HTMLDivElement>(null)
  const recognition = useRef<Recognition | null>(null)
  const convos = useResource<Page<AiConversation>>(live ? '/ai/conversations' : null, { size: '30' })

  const toMsg = (m: AiMessage): Msg => ({ id: `s${m.id}`, role: m.role, text: m.content })

  useEffect(() => () => {
    window.speechSynthesis?.cancel()
    recognition.current?.stop()
  }, [])

  const stopSpeaking = () => {
    window.speechSynthesis?.cancel()
    setSpeakingId(null)
  }

  const speak = (m: Msg) => {
    const synth = window.speechSynthesis
    if (!synth) {
      toast('Read aloud is not supported in this browser', 'error')
      return
    }
    if (speakingId === m.id) {
      stopSpeaking()
      return
    }
    synth.cancel()
    const utterance = new SpeechSynthesisUtterance(speechText(m.text))
    utterance.lang = 'en-US'
    utterance.onend = () => setSpeakingId((current) => (current === m.id ? null : current))
    utterance.onerror = () => setSpeakingId((current) => (current === m.id ? null : current))
    setSpeakingId(m.id)
    synth.speak(utterance)
  }

  const toggleMic = () => {
    if (listening) {
      recognition.current?.stop()
      return
    }
    const win = window as unknown as {
      SpeechRecognition?: new () => Recognition
      webkitSpeechRecognition?: new () => Recognition
    }
    const RecognitionCtor = win.SpeechRecognition ?? win.webkitSpeechRecognition
    if (!RecognitionCtor) {
      toast('Voice input is not supported in this browser. Try Chrome or Edge.', 'error')
      return
    }
    const rec = new RecognitionCtor()
    rec.lang = 'en-US'
    rec.interimResults = false
    rec.continuous = false
    rec.onresult = (event) => {
      let transcript = ''
      for (let i = 0; i < event.results.length; i++) transcript += event.results[i][0].transcript
      setInput((prev) => (prev ? `${prev} ${transcript}` : transcript).slice(0, 4000))
    }
    rec.onerror = () => {
      setListening(false)
      toast('Could not capture voice. Check microphone permission.', 'error')
    }
    rec.onend = () => setListening(false)
    recognition.current = rec
    try {
      rec.start()
      setListening(true)
    } catch {
      setListening(false)
    }
  }

  const openConversation = async (id: number) => {
    if (busy) return
    stopSpeaking()
    try {
      const { data } = await api.get<AiMessage[]>(`/ai/conversations/${id}/messages`)
      setConversationId(id)
      setMessages(data.map(toMsg))
    } catch (err) {
      toast(describeError(err).message, 'error')
    }
  }

  const send = async (text: string, retryOf?: string) => {
    const content = text.trim()
    if (!content || busy) return
    stopSpeaking()
    setBusy(true)
    setMessages((m) => (retryOf ? m.filter((x) => x.id !== retryOf) : [...m, { id: `l${nextId.current++}`, role: 'USER', text: content }]))
    setInput('')
    try {
      let id = conversationId
      if (id === null) {
        const { data } = await api.post<AiConversation>('/ai/conversations', { title: content.slice(0, 60) })
        id = data.id
        setConversationId(id)
      }
      const { data } = await api.post<AiMessage>(`/ai/conversations/${id}/messages`, { content })
      setMessages((m) => [...m, toMsg(data)])
      convos.reload()
    } catch (err) {
      const reason = describeError(err).message
      setMessages((m) => [...m, { id: `l${nextId.current++}`, role: 'ASSISTANT', text: `I couldn't answer that. ${reason}`, failed: true }])
    } finally {
      setBusy(false)
      requestAnimationFrame(() => bottom.current?.scrollIntoView({ block: 'end' }))
    }
  }

  const submit = (e: FormEvent) => { e.preventDefault(); void send(input) }
  const lastUser = [...messages].reverse().find((m) => m.role === 'USER')

  return (
    <>
      <PageHeader
        eyebrow="AI Tutor"
        title="Ask anything you're studying"
        description="Answers come from the LearnMate backend. If the AI provider isn't configured you'll see a clear error, never a pretend answer."
        actions={<Btn variant="secondary" icon={MessageSquarePlus} onClick={() => { stopSpeaking(); setMessages([]); setConversationId(null) }}>New conversation</Btn>}
      />
      <div className="tutor">
        <section className="card tutor-chat" aria-label="Conversation">
          <div className="chat-log" role="log" aria-live="polite">
            {messages.length === 0 && (
              <div className="chat-empty">
                <span className="empty-icon" aria-hidden="true"><Sparkles size={22} /></span>
                <h2 className="h3">What would you like to understand today?</h2>
                <div className="chips">{starters.map((s) => (<button key={s} type="button" className="chip" onClick={() => void send(s)}>{s}</button>))}</div>
              </div>
            )}
            {messages.map((m) => (
              <div key={m.id} className={`msg msg--${m.role === 'USER' ? 'user' : 'assistant'} ${m.failed ? 'msg--failed' : ''}`}>
                <span className="msg-avatar" aria-hidden="true">{m.role === 'USER' ? <User size={15} /> : <Bot size={15} />}</span>
                <div className="msg-body">
                  <Rich text={m.text} />
                  {m.role === 'ASSISTANT' && (
                    <div className="msg-actions">
                      {m.failed && lastUser && <Btn variant="ghost" icon={RefreshCw} disabled={busy} onClick={() => void send(lastUser.text, m.id)}>Retry</Btn>}
                      {!m.failed && <Btn variant="ghost" icon={copied === m.id ? Check : Copy} onClick={() => navigator.clipboard.writeText(m.text).then(() => { setCopied(m.id); toast('Response copied', 'success') }, () => toast('Could not copy', 'error'))}>Copy</Btn>}
                      {!m.failed && <Btn variant="ghost" icon={speakingId === m.id ? VolumeX : Volume2} onClick={() => speak(m)}>{speakingId === m.id ? 'Stop' : 'Speak'}</Btn>}
                    </div>
                  )}
                </div>
              </div>
            ))}
            {busy && <p className="muted typing">Thinking…</p>}
            <div ref={bottom} />
          </div>
          <form className="composer" onSubmit={submit}>
            <label htmlFor="tutor-input" className="sr-only">Message the AI tutor</label>
            <textarea id="tutor-input" rows={2} maxLength={4000} value={input} onChange={(e) => setInput(e.target.value)} placeholder={listening ? 'Listening…' : 'Ask a question…'} onKeyDown={(e) => { if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); void send(input) } }} />
            <button type="button" className={`icon-btn mic-btn ${listening ? 'is-listening' : ''}`} aria-label={listening ? 'Stop dictating' : 'Dictate question'} aria-pressed={listening} title={listening ? 'Stop dictating' : 'Dictate question'} onClick={toggleMic}>
              {listening ? <MicOff size={18} aria-hidden="true" /> : <Mic size={18} aria-hidden="true" />}
            </button>
            <Btn type="submit" icon={Send} loading={busy} disabled={!input.trim()}>Send</Btn>
          </form>
        </section>

        <aside className="card pad tutor-side">
          <h2 className="h3">History</h2>
          <ResourceView resource={convos} endpoint="GET /ai/conversations" isEmpty={(d) => d.content.length === 0} emptyIcon={MessageSquarePlus} emptyTitle="No saved conversations">
            {(d) => (
              <ul className="plain-list">
                {d.content.map((c) => (
                  <li key={c.id} className={`history-item ${conversationId === c.id ? 'is-active' : ''}`}>
                    <button type="button" className="text-link history-title" onClick={() => void openConversation(c.id)}>{c.title}</button>
                    {c.lastMessage && <p className="history-preview">{c.lastMessageRole === 'USER' ? 'You: ' : ''}{c.lastMessage}</p>}
                  </li>
                ))}
              </ul>
            )}
          </ResourceView>
        </aside>
      </div>
    </>
  )
}
