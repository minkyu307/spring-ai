import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { FormEvent } from 'react';
import { useOutletContext } from 'react-router-dom';
import { formatErrorMessage } from '../api/errors';
import { apiFetch } from '../api/http';
import type { AppShellOutletContext } from '../components/AppShell';
import { ChatPanel } from '../components/note/ChatPanel';
import type { ChatHistoryItem, ChatSourceViewItem, ChatViewMessage } from '../components/note/ChatPanel';
import { SourcesPanel } from '../components/note/SourcesPanel';
import { StudioPanel } from '../components/note/StudioPanel';

type ApiChatSource = {
  sourceType: string;
  label: string;
  href?: string | null;
};

type HistoryDetail = {
  conversationId: string;
  messages: Array<{ role: 'user' | 'assistant' | 'system'; content: string; sources?: ApiChatSource[] }>;
};

type ChatResult = {
  conversationId: string;
  response: string;
  sources?: ApiChatSource[];
};

/**
 * API 응답 출처 목록을 UI 렌더링 가능한 형태로 정규화한다.
 */
function normalizeSources(sources: ApiChatSource[] | undefined): ChatSourceViewItem[] {
  if (!Array.isArray(sources) || sources.length === 0) {
    return [];
  }
  return sources
    .map((source) => ({
      sourceType: typeof source.sourceType === 'string' ? source.sourceType : '',
      label: typeof source.label === 'string' ? source.label.trim() : '',
      href: typeof source.href === 'string' && source.href.trim() ? source.href.trim() : null,
    }))
    .filter((source) => source.label.length > 0);
}

/**
 * NotebookLM 스타일의 노트 3패널 화면을 구성한다.
 */
export function NotePage() {
  const { setNoteHeaderTitle } = useOutletContext<AppShellOutletContext>();

  const [leftPanelCollapsed, setLeftPanelCollapsed] = useState(false);
  const [rightPanelCollapsed, setRightPanelCollapsed] = useState(true);
  const [histories, setHistories] = useState<ChatHistoryItem[]>([]);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<ChatViewMessage[]>([]);
  const [message, setMessage] = useState('');
  const [sending, setSending] = useState(false);
  const [processing, setProcessing] = useState(false);
  const typingFrameRef = useRef<number | null>(null);
  const typingResolveRef = useRef<(() => void) | null>(null);

  const activeHistoryTitle = useMemo(() => {
    const found = histories.find((item) => item.conversationId === conversationId);
    if (found) return found.title;
    return '새 채팅';
  }, [conversationId, histories]);

  const stopTypingAnimation = useCallback(() => {
    if (typingFrameRef.current !== null) {
      window.cancelAnimationFrame(typingFrameRef.current);
      typingFrameRef.current = null;
    }
    if (typingResolveRef.current) {
      typingResolveRef.current();
      typingResolveRef.current = null;
    }
  }, []);

  const animateAiResponse = useCallback(
    (messageId: string, response: string): Promise<void> => {
      stopTypingAnimation();
      const fullText = response ?? '';
      if (!fullText) {
        setMessages((prev) =>
          prev.map((item) => (item.id === messageId ? { ...item, content: '' } : item)),
        );
        return Promise.resolve();
      }

      const totalLength = fullText.length;
      const baseDuration = Math.min(900, Math.max(240, totalLength * 12));
      const duration = baseDuration * 4;
      const startedAt = performance.now();
      let visibleLength = 0;

      return new Promise<void>((resolve) => {
        typingResolveRef.current = resolve;

        const frame = (now: number) => {
          const progress = Math.min(1, (now - startedAt) / duration);
          const nextVisibleLength =
            progress >= 1 ? totalLength : Math.max(1, Math.ceil(totalLength * progress));

          if (nextVisibleLength !== visibleLength) {
            visibleLength = nextVisibleLength;
            setMessages((prev) =>
              prev.map((item) =>
                item.id === messageId
                  ? { ...item, content: fullText.slice(0, nextVisibleLength) }
                  : item,
              ),
            );
          }

          if (progress >= 1) {
            typingFrameRef.current = null;
            typingResolveRef.current = null;
            resolve();
            return;
          }

          typingFrameRef.current = window.requestAnimationFrame(frame);
        };

        typingFrameRef.current = window.requestAnimationFrame(frame);
      });
    },
    [stopTypingAnimation],
  );

  const loadHistories = useCallback(async () => {
    const data = await apiFetch<ChatHistoryItem[]>('/api/chat/histories');
    setHistories(data);
    return data;
  }, []);

  const loadHistoryMessages = useCallback(
    async (id: string) => {
      stopTypingAnimation();
      setProcessing(false);
      const detail = await apiFetch<HistoryDetail>(`/api/chat/histories/${encodeURIComponent(id)}`);
      setMessages(
        detail.messages.map((item) => ({
          id: crypto.randomUUID(),
          role: item.role === 'user' ? 'user' : 'ai',
          content: item.content,
          sources: normalizeSources(item.sources),
        })),
      );
    },
    [stopTypingAnimation],
  );

  useEffect(() => {
    void (async () => {
      try {
        const items = await loadHistories();
        if (items.length === 0) {
          const newId = crypto.randomUUID();
          setConversationId(newId);
          setMessages([]);
          localStorage.setItem('conversationId', newId);
          return;
        }

        const stored = localStorage.getItem('conversationId');
        const found = items.find((item) => item.conversationId === stored) ?? items[0];
        setConversationId(found.conversationId);
        localStorage.setItem('conversationId', found.conversationId);
        await loadHistoryMessages(found.conversationId);
      } catch (error) {
        setMessages([
          {
            id: crypto.randomUUID(),
            role: 'error',
            content: formatErrorMessage(error, '대화 목록을 불러오지 못했습니다.'),
          },
        ]);
      }
    })();
  }, [loadHistories, loadHistoryMessages]);

  const handleSelectHistory = useCallback(
    async (id: string) => {
      stopTypingAnimation();
      setProcessing(false);
      try {
        setConversationId(id);
        localStorage.setItem('conversationId', id);
        await loadHistoryMessages(id);
        await loadHistories();
      } catch (error) {
        setMessages((prev) => [
          ...prev,
          {
            id: crypto.randomUUID(),
            role: 'error',
            content: formatErrorMessage(error, '대화 기록을 불러오지 못했습니다.'),
          },
        ]);
      }
    },
    [loadHistories, loadHistoryMessages, stopTypingAnimation],
  );

  const handleDeleteHistory = useCallback(
    async (id: string) => {
      stopTypingAnimation();
      setProcessing(false);
      if (!window.confirm('이 대화를 삭제할까요?')) return;
      try {
        await apiFetch<void>(`/api/chat/histories/${encodeURIComponent(id)}`, { method: 'DELETE' });
        if (id === conversationId) {
          const newId = crypto.randomUUID();
          setConversationId(newId);
          setMessages([]);
          localStorage.setItem('conversationId', newId);
        }
        await loadHistories();
      } catch (error) {
        setMessages((prev) => [
          ...prev,
          {
            id: crypto.randomUUID(),
            role: 'error',
            content: formatErrorMessage(error, '대화 삭제에 실패했습니다.'),
          },
        ]);
      }
    },
    [conversationId, loadHistories, stopTypingAnimation],
  );

  const handleNewChat = useCallback(() => {
    stopTypingAnimation();
    setProcessing(false);
    setSending(false);
    const newId = crypto.randomUUID();
    setConversationId(newId);
    setMessages([]);
    setMessage('');
    localStorage.setItem('conversationId', newId);
  }, [stopTypingAnimation]);

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (sending) return;

    const trimmed = message.trim();
    if (!trimmed) return;

    void (async () => {
      const currentId = conversationId ?? crypto.randomUUID();
      setConversationId(currentId);
      setMessage('');
      setSending(true);
      setProcessing(true);

      const userMessage: ChatViewMessage = {
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
        setProcessing(false);
        const aiMessageId = crypto.randomUUID();
        setMessages((prev) => [
          ...prev,
          {
            id: aiMessageId,
            role: 'ai',
            content: '',
            sources: normalizeSources(result.sources),
          },
        ]);
        await animateAiResponse(aiMessageId, result.response);
        await loadHistories();
      } catch (error) {
        setProcessing(false);
        setMessages((prev) => [
          ...prev,
          {
            id: crypto.randomUUID(),
            role: 'error',
            content: formatErrorMessage(error, '요청 실패'),
          },
        ]);
      } finally {
        setSending(false);
      }
    })();
  };

  useEffect(() => {
    return () => {
      setNoteHeaderTitle('새 채팅');
    };
  }, [setNoteHeaderTitle]);

  useEffect(() => {
    setNoteHeaderTitle(activeHistoryTitle);
  }, [activeHistoryTitle, setNoteHeaderTitle]);

  useEffect(() => {
    return () => {
      stopTypingAnimation();
    };
  }, [stopTypingAnimation]);

  return (
    <div
      className={`note-layout${leftPanelCollapsed ? ' is-left-collapsed' : ''}${
        rightPanelCollapsed ? ' is-right-collapsed' : ''
      }`}
    >
      <SourcesPanel
        collapsed={leftPanelCollapsed}
        onToggle={() => setLeftPanelCollapsed((prev) => !prev)}
      />
      <ChatPanel
        histories={histories}
        conversationId={conversationId}
        activeHistoryTitle={activeHistoryTitle}
        messages={messages}
        message={message}
        sending={sending}
        processing={processing}
        onMessageChange={setMessage}
        onSubmit={handleSubmit}
        onSelectHistory={handleSelectHistory}
        onDeleteHistory={handleDeleteHistory}
        onNewChat={handleNewChat}
      />
      <StudioPanel
        collapsed={rightPanelCollapsed}
        onToggle={() => setRightPanelCollapsed((prev) => !prev)}
      />
    </div>
  );
}
