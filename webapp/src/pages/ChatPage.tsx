import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { FiTrash2 } from 'react-icons/fi';
import { apiFetch } from '../api/http';
import { renderMarkdown } from '../lib/markdown';

type HistoryItem = {
  conversationId: string;
  title: string;
  lastUpdated: string;
};

type HistoryDetail = {
  conversationId: string;
  messages: Array<{ role: 'user' | 'assistant'; content: string }>;
};

type ChatResult = {
  conversationId: string;
  response: string;
};

type ViewMessage = {
  id: string;
  role: 'user' | 'ai' | 'error';
  content: string;
};

export function ChatPage() {
  const [histories, setHistories] = useState<HistoryItem[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ViewMessage[]>([]);
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);

  const activeHistory = useMemo(
    () => histories.find((item) => item.conversationId === conversationId),
    [conversationId, histories],
  );

  const loadHistories = async () => {
    const data = await apiFetch<HistoryItem[]>('/api/chat/histories');
    setHistories(data);
    return data;
  };

  const loadHistoryMessages = async (id: string) => {
    const detail = await apiFetch<HistoryDetail>(`/api/chat/histories/${encodeURIComponent(id)}`);
    setMessages(
      detail.messages.map((item, index) => ({
        id: `${index}-${Math.random().toString(16).slice(2)}`,
        role: item.role === 'user' ? 'user' : 'ai',
        content: item.content,
      })),
    );
  };

  useEffect(() => {
    loadHistories().then((items) => {
      if (items.length === 0) {
        const newId = crypto.randomUUID();
        setConversationId(newId);
        setMessages([]);
        return;
      }

      const stored = localStorage.getItem('conversationId');
      const found = items.find((item) => item.conversationId === stored) ?? items[0];
      setConversationId(found.conversationId);
      localStorage.setItem('conversationId', found.conversationId);
      void loadHistoryMessages(found.conversationId);
    });
  }, []);

  const handleSelectHistory = async (id: string) => {
    setConversationId(id);
    localStorage.setItem('conversationId', id);
    await loadHistoryMessages(id);
    await loadHistories();
  };

  const handleDeleteHistory = async (id: string) => {
    if (!window.confirm('이 대화를 삭제할까요?')) return;
    await apiFetch<void>(`/api/chat/histories/${encodeURIComponent(id)}`, { method: 'DELETE' });
    if (id === conversationId) {
      const newId = crypto.randomUUID();
      setConversationId(newId);
      setMessages([]);
      localStorage.setItem('conversationId', newId);
    }
    await loadHistories();
  };

  const handleNewChat = () => {
    const newId = crypto.randomUUID();
    setConversationId(newId);
    setMessages([]);
    localStorage.setItem('conversationId', newId);
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = message.trim();
    if (!trimmed) return;

    const currentId = conversationId ?? crypto.randomUUID();
    setConversationId(currentId);
    setMessage('');
    setSending(true);

    const userMessage: ViewMessage = {
      id: crypto.randomUUID(),
      role: 'user',
      content: trimmed,
    };
    setMessages((prev) => [...prev, userMessage]);

    try {
      const result = await apiFetch<ChatResult>('/api/chat/message', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          conversationId: currentId,
          message: trimmed,
        }),
      });
      setConversationId(result.conversationId);
      localStorage.setItem('conversationId', result.conversationId);
      setMessages((prev) => [
        ...prev,
        { id: crypto.randomUUID(), role: 'ai', content: result.response },
      ]);
      await loadHistories();
    } catch (error) {
      setMessages((prev) => [
        ...prev,
        {
          id: crypto.randomUUID(),
          role: 'error',
          content: error instanceof Error ? error.message : '요청 실패',
        },
      ]);
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      <aside className="chat-sidebar">
        <div style={{ padding: 12 }}>
          <button className="btn btn-primary btn-block" onClick={handleNewChat}>
            New Chat
          </button>
        </div>
        <div className="list-scroll">
          {histories.length === 0 ? (
            <div>대화 히스토리가 없습니다.</div>
          ) : (
            histories.map((history) => (
              <div
                key={history.conversationId}
                className={`list-item ${
                  conversationId === history.conversationId ? 'active' : ''
                }`}
                onClick={() => handleSelectHistory(history.conversationId)}
              >
                <div style={{ display: 'flex', justifyContent: 'space-between', gap: 8 }}>
                  <strong style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {history.title}
                  </strong>
                  <button
                    className="btn btn-danger icon-action-btn"
                    type="button"
                    aria-label="대화 삭제"
                    onClick={(e) => {
                      e.stopPropagation();
                      void handleDeleteHistory(history.conversationId);
                    }}
                  >
                    <FiTrash2 aria-hidden="true" />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </aside>

      <section className="chat-main">
        <div className="chat-messages">
          {messages.length === 0 ? (
            <div style={{ margin: 'auto', color: '#5f6368' }}>
              {activeHistory ? '대화 내용을 불러왔습니다.' : '새 대화를 시작하세요'}
            </div>
          ) : (
            messages.map((item) => (
              <div key={item.id} className={`msg-card ${item.role}`}>
                <div
                  dangerouslySetInnerHTML={{
                    __html:
                      item.role === 'error'
                        ? item.content
                        : renderMarkdown(item.content || ''),
                  }}
                />
              </div>
            ))
          )}
        </div>

        <div className="chat-input-box">
          <form className="chat-form" onSubmit={handleSubmit}>
            <textarea
              value={message}
              onChange={(e) => setMessage(e.target.value)}
              placeholder="메시지를 입력하세요..."
              disabled={sending}
            />
            <button className="btn btn-primary" type="submit" disabled={sending}>
              전송
            </button>
          </form>
        </div>
      </section>
    </>
  );
}
