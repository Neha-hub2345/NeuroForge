import { useState, useRef, useEffect } from 'react'
import { Sparkles, Send, Loader2, Bot, User } from 'lucide-react'
import { aiService } from '../../services/aiService'
import { Alert } from '../ui'

const QUICK_PROMPT = "Give me a quick summary of everything important right now."

// ---------------------------------------------------------------------------
// Lightweight markdown-ish renderer — no dependency needed for the small
// subset the assistant actually produces: **bold**, `code`, "- " bullet
// lines, and blank-line-separated paragraphs.
// ---------------------------------------------------------------------------
function renderInline(text, keyPrefix) {
  // Split on **bold** and `code` spans, keep the delimiters via capture groups
  const parts = text.split(/(\*\*[^*]+\*\*|`[^`]+`)/g)
  return parts.map((part, i) => {
    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={`${keyPrefix}-${i}`}>{part.slice(2, -2)}</strong>
    }
    if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code
          key={`${keyPrefix}-${i}`}
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--line)',
            borderRadius: 4,
            padding: '1px 5px',
            fontSize: '0.9em',
            fontFamily: 'SF Mono, Fira Code, monospace'
          }}
        >
          {part.slice(1, -1)}
        </code>
      )
    }
    return part
  })
}

function MarkdownLite({ content }) {
  const lines = content.split('\n')
  const blocks = []
  let currentList = []

  const flushList = (key) => {
    if (currentList.length > 0) {
      blocks.push(
        <ul key={`list-${key}`} style={{ margin: '4px 0 8px', paddingLeft: 18 }}>
          {currentList.map((item, i) => (
            <li key={i} style={{ marginBottom: 4, lineHeight: 1.5 }}>
              {renderInline(item, `li-${key}-${i}`)}
            </li>
          ))}
        </ul>
      )
      currentList = []
    }
  }

  lines.forEach((rawLine, idx) => {
    const line = rawLine.trim()

    if (line.startsWith('- ') || line.startsWith('* ')) {
      currentList.push(line.slice(2).trim())
      return
    }

    flushList(idx)

    if (line.startsWith('### ')) {
      blocks.push(
        <div key={idx} style={{ fontWeight: 700, fontSize: 13, marginTop: 8, marginBottom: 2 }}>
          {renderInline(line.slice(4), `h-${idx}`)}
        </div>
      )
    } else if (line === '') {
      // paragraph break — skip, spacing handled by block margins
    } else {
      blocks.push(
        <div key={idx} style={{ lineHeight: 1.5, marginBottom: 4 }}>
          {renderInline(line, `p-${idx}`)}
        </div>
      )
    }
  })
  flushList('end')

  return <>{blocks}</>
}

export default function AiAssistantPanel() {
  const [messages, setMessages] = useState([])
  const [input, setInput] = useState('')
  const [sending, setSending] = useState(false)
  const [error, setError] = useState('')
  const scrollRef = useRef(null)

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' })
  }, [messages, sending])

  const send = async (text) => {
    const content = text.trim()
    if (!content || sending) return
    setError('')
    setInput('')

    const nextMessages = [...messages, { role: 'user', content }]
    setMessages(nextMessages)
    setSending(true)

    try {
      const history = nextMessages.slice(0, -1)
      const { reply } = await aiService.chat(content, history)
      setMessages((prev) => [...prev, { role: 'assistant', content: reply }])
    } catch (err) {
      setError(err.message)
      setMessages((prev) => prev.slice(0, -1))
    } finally {
      setSending(false)
    }
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    send(input)
  }

  return (
    <div className="panel">
      <div className="panel-header">
        <h2><Sparkles size={16} /> Workspace Assistant</h2>
        <button className="btn-ghost-sm" onClick={() => send(QUICK_PROMPT)} disabled={sending}>
          <Sparkles size={13} /> Quick summary
        </button>
      </div>

      <Alert onClose={() => setError('')}>{error}</Alert>

      <div
        ref={scrollRef}
        style={{
          maxHeight: 380,
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: 14,
          marginBottom: 14,
          paddingRight: 4
        }}
      >
        {messages.length === 0 && !sending && (
          <div className="empty-state" style={{ padding: '28px 10px' }}>
            <div className="empty-title">Ask me anything about your workspace</div>
            <div className="empty-sub">
              Projects, sprints, pipelines, releases, blockers — or just hit "Quick summary".
            </div>
          </div>
        )}

        {messages.map((m, i) => {
          const isUser = m.role === 'user'
          return (
            <div
              key={i}
              style={{
                display: 'flex',
                gap: 10,
                alignSelf: isUser ? 'flex-end' : 'flex-start',
                flexDirection: isUser ? 'row-reverse' : 'row',
                maxWidth: '92%'
              }}
            >
              <div
                style={{
                  width: 28,
                  height: 28,
                  borderRadius: 8,
                  flexShrink: 0,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  background: isUser ? 'var(--accent-soft)' : 'var(--surface-2)',
                  border: '1px solid var(--line)',
                  color: isUser ? 'var(--accent-3)' : 'var(--accent-2)'
                }}
              >
                {isUser ? <User size={14} /> : <Bot size={14} />}
              </div>

              <div
                style={{
                  background: isUser ? 'var(--accent-soft)' : 'var(--surface-2)',
                  border: '1px solid var(--line)',
                  borderRadius: 12,
                  borderTopLeftRadius: isUser ? 12 : 4,
                  borderTopRightRadius: isUser ? 4 : 12,
                  padding: '10px 14px',
                  fontSize: 13.5,
                  color: 'var(--ink)',
                  minWidth: 0
                }}
              >
                {isUser ? (
                  <div style={{ lineHeight: 1.5 }}>{m.content}</div>
                ) : (
                  <MarkdownLite content={m.content} />
                )}
              </div>
            </div>
          )
        })}

        {sending && (
          <div style={{ display: 'flex', gap: 10, alignSelf: 'flex-start' }}>
            <div
              style={{
                width: 28,
                height: 28,
                borderRadius: 8,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'var(--surface-2)',
                border: '1px solid var(--line)',
                color: 'var(--accent-2)'
              }}
            >
              <Bot size={14} />
            </div>
            <div
              style={{
                background: 'var(--surface-2)',
                border: '1px solid var(--line)',
                borderRadius: 12,
                borderTopLeftRadius: 4,
                padding: '10px 14px',
                fontSize: 13,
                color: 'var(--ink-soft)',
                display: 'flex',
                alignItems: 'center',
                gap: 6
              }}
            >
              <Loader2 size={13} className="bd-loading-spinner" /> Thinking…
            </div>
          </div>
        )}
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: 8 }}>
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="e.g. Which environments are unhealthy right now?"
          style={{ flex: 1 }}
          disabled={sending}
        />
        <button className="btn-primary" type="submit" disabled={sending || !input.trim()}>
          <Send size={15} />
        </button>
      </form>
    </div>
  )
}