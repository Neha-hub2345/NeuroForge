import { useState, useRef, useEffect,createElement  } from 'react'
import { Sparkles, Send, Loader2, Bot, User, Copy, Check } from 'lucide-react'
import { aiService } from '../../services/aiService'
import { Alert } from '../ui'

const QUICK_PROMPT = "Give me a quick summary of everything important right now."

const SUGGESTIONS = [
  'Which environments are unhealthy right now?',
  'Summarize this sprint\'s open blockers',
  'What shipped in the last release?'
]

// ---------------------------------------------------------------------------
// Markdown-ish renderer for the assistant's replies. Supports the subset the
// model actually produces: headers (##/###), bold/italic, inline code,
// fenced code blocks, bullet + numbered lists, links, and paragraphs.
// ---------------------------------------------------------------------------
function renderInline(text, keyPrefix) {
  const parts = text.split(/(\*\*[^*]+\*\*|\*[^*]+\*|`[^`]+`|\[[^\]]+\]\([^)]+\))/g)
  return parts.map((part, i) => {
    const key = `${keyPrefix}-${i}`
    if (!part) return null

    if (part.startsWith('**') && part.endsWith('**')) {
      return <strong key={key} style={{ fontWeight: 650 }}>{part.slice(2, -2)}</strong>
    }
    if (part.startsWith('*') && part.endsWith('*') && !part.startsWith('**')) {
      return <em key={key}>{part.slice(1, -1)}</em>
    }
    if (part.startsWith('`') && part.endsWith('`')) {
      return (
        <code
          key={key}
          style={{
            background: 'var(--surface)',
            border: '1px solid var(--line)',
            borderRadius: 4,
            padding: '1px 5px',
            fontSize: '0.88em',
            fontFamily: 'SF Mono, Fira Code, monospace',
            color: 'var(--accent-3, var(--ink))'
          }}
        >
          {part.slice(1, -1)}
        </code>
      )
    }
        const linkMatch = part.match(/^\[([^\]]+)\]\(([^)]+)\)$/)
    if (linkMatch) {
      return createElement(
        'a',
        {
          key,
          href: linkMatch[2],
          target: '_blank',
          rel: 'noreferrer',
          style: { color: 'var(--accent-2)', textDecoration: 'underline', textUnderlineOffset: 2 }
        },
        linkMatch[1]
      )
    }
    return part
  })
}

function CodeBlock({ code, lang }) {
  const [copied, setCopied] = useState(false)
  return (
    <div
      style={{
        background: 'var(--surface)',
        border: '1px solid var(--line)',
        borderRadius: 8,
        margin: '6px 0',
        overflow: 'hidden'
      }}
    >
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          padding: '5px 10px',
          borderBottom: '1px solid var(--line)',
          fontSize: 11,
          color: 'var(--ink-soft)',
          fontFamily: 'SF Mono, Fira Code, monospace'
        }}
      >
        <span>{lang || 'code'}</span>
        <button
          onClick={() => {
            navigator.clipboard?.writeText(code)
            setCopied(true)
            setTimeout(() => setCopied(false), 1200)
          }}
          style={{
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            color: 'inherit',
            display: 'flex',
            alignItems: 'center',
            gap: 4,
            padding: 2
          }}
        >
          {copied ? <Check size={12} /> : <Copy size={12} />}
        </button>
      </div>
      <pre
        style={{
          margin: 0,
          padding: '10px 12px',
          overflowX: 'auto',
          fontSize: 12.5,
          lineHeight: 1.55,
          fontFamily: 'SF Mono, Fira Code, monospace'
        }}
      >
        {code}
      </pre>
    </div>
  )
}

function MarkdownLite({ content }) {
  // Split out fenced code blocks first so their content is never parsed as markdown
  const segments = content.split(/(```[\s\S]*?```)/g)

  return (
    <>
      {segments.map((segment, segIdx) => {
        if (segment.startsWith('```')) {
          const inner = segment.slice(3, -3)
          const firstBreak = inner.indexOf('\n')
          const lang = firstBreak === -1 ? '' : inner.slice(0, firstBreak).trim()
          const code = (firstBreak === -1 ? inner : inner.slice(firstBreak + 1)).replace(/\n$/, '')
          return <CodeBlock key={`code-${segIdx}`} code={code} lang={lang} />
        }
        return <MarkdownBlocks key={`md-${segIdx}`} content={segment} keyPrefix={segIdx} />
      })}
    </>
  )
}

function MarkdownBlocks({ content, keyPrefix }) {
  const lines = content.split('\n')
  const blocks = []
  let currentList = null // { type: 'ul' | 'ol', items: [] }

  const flushList = (key) => {
    if (!currentList) return
    const { type, items } = currentList
    blocks.push(
      <div key={`list-${keyPrefix}-${key}`} style={{ margin: '4px 0 10px' }}>
        {items.map((item, i) => (
          <div key={i} style={{ display: 'flex', gap: 8, marginBottom: 5 }}>
            <div
              style={{
                flexShrink: 0,
                width: type === 'ol' ? 16 : 5,
                height: type === 'ol' ? 'auto' : 5,
                marginTop: type === 'ol' ? 0 : 7,
                borderRadius: '50%',
                background: type === 'ul' ? 'var(--accent-2)' : 'transparent',
                fontSize: 12.5,
                fontWeight: 600,
                color: 'var(--accent-2)',
                textAlign: type === 'ol' ? 'right' : undefined
              }}
            >
              {type === 'ol' ? `${i + 1}.` : ''}
            </div>
            <div style={{ lineHeight: 1.55, flex: 1 }}>
              {renderInline(item, `li-${keyPrefix}-${key}-${i}`)}
            </div>
          </div>
        ))}
      </div>
    )
    currentList = null
  }

  lines.forEach((rawLine, idx) => {
    const line = rawLine.trim()
    const bullet = line.match(/^[-*]\s+(.*)/)
    const numbered = line.match(/^\d+\.\s+(.*)/)

    if (bullet) {
      if (!currentList || currentList.type !== 'ul') { flushList(idx); currentList = { type: 'ul', items: [] } }
      currentList.items.push(bullet[1])
      return
    }
    if (numbered) {
      if (!currentList || currentList.type !== 'ol') { flushList(idx); currentList = { type: 'ol', items: [] } }
      currentList.items.push(numbered[1])
      return
    }
    flushList(idx)

    if (line.startsWith('### ')) {
      blocks.push(
        <div key={idx} style={{ fontWeight: 650, fontSize: 13.5, marginTop: 10, marginBottom: 3 }}>
          {renderInline(line.slice(4), `h3-${idx}`)}
        </div>
      )
    } else if (line.startsWith('## ')) {
      blocks.push(
        <div
          key={idx}
          style={{
            fontWeight: 700,
            fontSize: 14.5,
            marginTop: 12,
            marginBottom: 4,
            paddingBottom: 5,
            borderBottom: '1px solid var(--line)'
          }}
        >
          {renderInline(line.slice(3), `h2-${idx}`)}
        </div>
      )
    } else if (line === '') {
      // paragraph break — spacing handled by block margins
    } else {
      blocks.push(
        <div key={idx} style={{ lineHeight: 1.55, marginBottom: 6 }}>
          {renderInline(line, `p-${idx}`)}
        </div>
      )
    }
  })
  flushList('end')

  return <>{blocks}</>
}

function TypingDots() {
  return (
    <div style={{ display: 'flex', gap: 4, alignItems: 'center', padding: '2px 2px' }}>
      {[0, 1, 2].map((i) => (
        <span
          key={i}
          className="bd-typing-dot"
          style={{
            width: 5,
            height: 5,
            borderRadius: '50%',
            background: 'var(--ink-soft)',
            animation: `bd-typing-bounce 1.1s ${i * 0.15}s infinite ease-in-out`
          }}
        />
      ))}
      <style>{`
        @keyframes bd-typing-bounce {
          0%, 60%, 100% { transform: translateY(0); opacity: 0.5; }
          30% { transform: translateY(-3px); opacity: 1; }
        }
      `}</style>
    </div>
  )
}

function MessageActions({ content }) {
  const [copied, setCopied] = useState(false)
  return (
    <button
      onClick={() => {
        navigator.clipboard?.writeText(content)
        setCopied(true)
        setTimeout(() => setCopied(false), 1200)
      }}
      className="bd-msg-copy"
      style={{
        background: 'none',
        border: 'none',
        cursor: 'pointer',
        color: 'var(--ink-soft)',
        display: 'flex',
        alignItems: 'center',
        gap: 4,
        fontSize: 11,
        padding: '3px 4px',
        marginTop: 4,
        opacity: 0,
        transition: 'opacity 0.15s'
      }}
    >
      {copied ? <Check size={11} /> : <Copy size={11} />} {copied ? 'Copied' : 'Copy'}
    </button>
  )
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
          maxHeight: 420,
          overflowY: 'auto',
          display: 'flex',
          flexDirection: 'column',
          gap: 16,
          marginBottom: 14,
          paddingRight: 4
        }}
      >
        {messages.length === 0 && !sending && (
          <div className="empty-state" style={{ padding: '28px 10px' }}>
            <div className="empty-title">Ask me anything about your workspace</div>
            <div className="empty-sub" style={{ marginBottom: 12 }}>
              Projects, sprints, pipelines, releases, blockers — or just hit "Quick summary".
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6, justifyContent: 'center' }}>
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  onClick={() => send(s)}
                  style={{
                    background: 'var(--surface-2)',
                    border: '1px solid var(--line)',
                    borderRadius: 999,
                    padding: '6px 12px',
                    fontSize: 12,
                    color: 'var(--ink)',
                    cursor: 'pointer'
                  }}
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) => {
          const isUser = m.role === 'user'
          return (
            <div
              key={i}
              className="bd-msg-row"
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
                  background: isUser
                    ? 'linear-gradient(135deg, var(--accent-soft), var(--accent-3, var(--accent-soft)))'
                    : 'linear-gradient(135deg, var(--surface-2), var(--surface))',
                  border: '1px solid var(--line)',
                  color: isUser ? 'var(--accent-3)' : 'var(--accent-2)',
                  boxShadow: '0 1px 2px rgba(0,0,0,0.06)'
                }}
              >
                {isUser ? <User size={14} /> : <Bot size={14} />}
              </div>

              <div style={{ minWidth: 0 }}>
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
                    boxShadow: '0 1px 2px rgba(0,0,0,0.04)'
                  }}
                >
                  {isUser ? (
                    <div style={{ lineHeight: 1.55 }}>{m.content}</div>
                  ) : (
                    <MarkdownLite content={m.content} />
                  )}
                </div>
                {!isUser && <MessageActions content={m.content} />}
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
                background: 'linear-gradient(135deg, var(--surface-2), var(--surface))',
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
                display: 'flex',
                alignItems: 'center'
              }}
            >
              <TypingDots />
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
          {sending ? <Loader2 size={15} className="bd-loading-spinner" /> : <Send size={15} />}
        </button>
      </form>

      <style>{`
        .bd-msg-row:hover .bd-msg-copy { opacity: 1; }
      `}</style>
    </div>
  )
}