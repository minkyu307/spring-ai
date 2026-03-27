import { useEffect, useRef, useState } from 'react';
import type { FormEvent, KeyboardEvent } from 'react';
import { FiClock, FiTrash2 } from 'react-icons/fi';
import { renderMarkdown } from '../../lib/markdown';

export type ChatHistoryItem = {
  conversationId: string;
  title: string;
  lastUpdated: string;
};

export type ChatViewMessage = {
  id: string;
  role: 'user' | 'ai' | 'error';
  content: string;
};

type ChatPanelProps = {
  histories: ChatHistoryItem[];
  conversationId: string | null;
  activeHistoryTitle: string;
  messages: ChatViewMessage[];
  message: string;
  sending: boolean;
  processing: boolean;
  onMessageChange: (value: string) => void;
  onSubmit: (event: FormEvent<HTMLFormElement>) => void;
  onSelectHistory: (id: string) => void | Promise<void>;
  onDeleteHistory: (id: string) => void | Promise<void>;
  onNewChat: () => void;
};

const CHAT_TITLE_UI_MAX_CHARS = 25;

function formatUpdatedAt(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function truncateChatTitleForUi(title: string): string {
  const normalized = title.trim();
  if (!normalized) return '새 채팅';
  const chars = Array.from(normalized);
  if (chars.length <= CHAT_TITLE_UI_MAX_CHARS) return normalized;
  return `${chars.slice(0, CHAT_TITLE_UI_MAX_CHARS).join('')}...`;
}

/**
 * 노트 중앙 채팅 영역과 히스토리 패널을 렌더링한다.
 */
export function ChatPanel({
  histories,
  conversationId,
  activeHistoryTitle,
  messages,
  message,
  sending,
  processing,
  onMessageChange,
  onSubmit,
  onSelectHistory,
  onDeleteHistory,
  onNewChat,
}: ChatPanelProps) {
  const messageViewportRef = useRef<HTMLDivElement | null>(null);
  const chatFormRef = useRef<HTMLFormElement | null>(null);
  const historyMenuRef = useRef<HTMLDivElement | null>(null);
  const [historyMenuOpen, setHistoryMenuOpen] = useState(false);

  const handleTextareaKeyDown = (event: KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key !== 'Enter') return;
    if (event.shiftKey || event.nativeEvent.isComposing) return;
    event.preventDefault();
    chatFormRef.current?.requestSubmit();
  };

  useEffect(() => {
    const viewport = messageViewportRef.current;
    if (!viewport) return;
    viewport.scrollTop = viewport.scrollHeight;
  }, [messages, processing]);

  useEffect(() => {
    if (!historyMenuOpen) return;

    const handleOutsideClick = (event: MouseEvent) => {
      const target = event.target;
      if (!(target instanceof Node)) return;
      if (!historyMenuRef.current?.contains(target)) {
        setHistoryMenuOpen(false);
      }
    };

    const handleEscapeKey = (event: globalThis.KeyboardEvent) => {
      if (event.key === 'Escape') {
        setHistoryMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleOutsideClick);
    document.addEventListener('keydown', handleEscapeKey);
    return () => {
      document.removeEventListener('mousedown', handleOutsideClick);
      document.removeEventListener('keydown', handleEscapeKey);
    };
  }, [historyMenuOpen]);

  const handleNewChatClick = () => {
    setHistoryMenuOpen(false);
    onNewChat();
  };

  const handleSelectHistoryClick = (id: string) => {
    setHistoryMenuOpen(false);
    void onSelectHistory(id);
  };

  const handleDeleteHistoryClick = (id: string) => {
    setHistoryMenuOpen(false);
    void onDeleteHistory(id);
  };

  return (
    <section className="note-chat-column">
      <section className="note-chat-panel">
        <header className="note-chat-panel-header note-panel-header">
          <div className="panel-header-title-group">
            <h2 className="note-panel-title">AI Retrieve</h2>
            <p className="muted-text panel-header-subtitle chat-panel-active-title">
              {truncateChatTitleForUi(activeHistoryTitle)}
            </p>
          </div>
          <div className="chat-panel-header-actions">
            <button className="btn btn-primary chat-header-new-chat-btn" type="button" onClick={handleNewChatClick}>
              New Chat
            </button>
            <div className="chat-history-menu-wrap" ref={historyMenuRef}>
              <button
                className="chat-history-menu-trigger"
                type="button"
                aria-haspopup="dialog"
                aria-expanded={historyMenuOpen}
                aria-label="채팅 히스토리 열기"
                onClick={() => setHistoryMenuOpen((prev) => !prev)}
              >
                <FiClock className="chat-history-menu-icon" size={16} aria-hidden="true" />
              </button>
              {historyMenuOpen && (
                <section className="chat-history-menu-dialog" role="dialog" aria-label="채팅 히스토리 메뉴">
                  <div className="drawer-history-scroll">
                    {histories.length === 0 && <p className="muted-text">대화 히스토리가 없습니다.</p>}
                    {histories.map((history) => (
                      <article
                        key={history.conversationId}
                        className={`drawer-history-item ${
                          conversationId === history.conversationId ? 'active' : ''
                        }`}
                      >
                        <button
                          className="drawer-history-main"
                          type="button"
                          onClick={() => handleSelectHistoryClick(history.conversationId)}
                        >
                          <strong>{truncateChatTitleForUi(history.title)}</strong>
                          <span>{formatUpdatedAt(history.lastUpdated)}</span>
                        </button>
                        <button
                          className="btn btn-danger icon-action-btn"
                          type="button"
                          aria-label="대화 삭제"
                          onClick={() => handleDeleteHistoryClick(history.conversationId)}
                        >
                          <FiTrash2 aria-hidden="true" />
                        </button>
                      </article>
                    ))}
                  </div>
                </section>
              )}
            </div>
          </div>
        </header>

        <div className="note-chat-messages" ref={messageViewportRef}>
          {messages.length === 0 && !processing ? (
            <div className="note-empty-hint">질문을 입력해 대화를 시작하세요.</div>
          ) : (
            <>
              {messages.map((item) => (
                <div key={item.id} className={`chat-message-row ${item.role}`}>
                  <article className={`chat-message-card ${item.role}`}>
                    {item.role === 'error' ? (
                      <p>{item.content}</p>
                    ) : (
                      <div
                        dangerouslySetInnerHTML={{
                          __html: renderMarkdown(item.content || ''),
                        }}
                      />
                    )}
                  </article>
                </div>
              ))}
              {processing && (
                <div className="chat-message-row ai">
                  <article className="chat-message-card ai chat-processing-card" role="status" aria-live="polite">
                    <span className="chat-processing-text">AI Reasoning...</span>
                    <span className="chat-processing-dots" aria-hidden="true">
                      <span />
                      <span />
                      <span />
                    </span>
                  </article>
                </div>
              )}
            </>
          )}
        </div>

        <footer className="note-chat-input-box">
          <form className="note-chat-form" onSubmit={onSubmit} ref={chatFormRef}>
            <textarea
              value={message}
              onChange={(event) => onMessageChange(event.target.value)}
              onKeyDown={handleTextareaKeyDown}
              placeholder="메시지를 입력하세요..."
              disabled={sending}
            />
            <button className="btn btn-primary" type="submit" disabled={sending}>
              전송
            </button>
          </form>
        </footer>
      </section>
    </section>
  );
}
