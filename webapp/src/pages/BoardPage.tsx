import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { FiEdit3, FiTrash2 } from 'react-icons/fi';
import { formatErrorMessage } from '../api/errors';
import { apiFetch } from '../api/http';
import { renderMarkdown } from '../lib/markdown';

type PostListItem = {
  postId: number;
  title: string;
  writerLoginId: string;
  writerUsername: string;
  updatedAt: string;
};

type CommentItem = {
  commentId: number;
  content: string;
  writerLoginId: string;
  writerUsername: string;
  updatedAt: string;
};

type PostDetail = {
  postId: number;
  title: string;
  content: string;
  writerLoginId: string;
  writerUsername: string;
  updatedAt: string;
  comments: CommentItem[];
};

type CurrentUser = {
  authenticated: boolean;
  loginId: string;
};

function formatBoardListDate(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}.${month}.${day}`;
}

function formatBoardDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}.${month}.${day} ${hour}:${minute}`;
}

export function BoardPage() {
  const [posts, setPosts] = useState<PostListItem[]>([]);
  const [currentPostId, setCurrentPostId] = useState<number | null>(null);
  const [detail, setDetail] = useState<PostDetail | null>(null);
  const [loading, setLoading] = useState(false);
  const [comment, setComment] = useState('');
  const [commentError, setCommentError] = useState('');
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingPostId, setEditingPostId] = useState<number | null>(null);
  const [editorTitle, setEditorTitle] = useState('');
  const [editorBody, setEditorBody] = useState('');
  const [editorError, setEditorError] = useState('');
  const [pageError, setPageError] = useState('');
  const [me, setMe] = useState<CurrentUser | null>(null);

  const isOwner = useMemo(() => {
    if (!detail || !me) return false;
    return detail.writerLoginId === me.loginId;
  }, [detail, me]);

  const loadPosts = async () => {
    try {
      const data = await apiFetch<PostListItem[]>('/api/board/posts');
      setPosts(data);
      setPageError('');
    } catch (error) {
      setPageError(formatErrorMessage(error, '게시글 목록을 불러오지 못했습니다.'));
    }
  };

  const loadDetail = async (postId: number) => {
    setLoading(true);
    try {
      const data = await apiFetch<PostDetail>(`/api/board/posts/${postId}`);
      setDetail(data);
      setCurrentPostId(postId);
      setPageError('');
    } catch (error) {
      setPageError(formatErrorMessage(error, '게시글 상세를 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void loadPosts();
    apiFetch<CurrentUser>('/api/auth/me')
      .then(setMe)
      .catch((error) => {
        setPageError(formatErrorMessage(error, '사용자 정보를 불러오지 못했습니다.'));
      });
  }, []);

  const openCreate = () => {
    setEditingPostId(null);
    setEditorTitle('');
    setEditorBody('');
    setEditorError('');
    setEditorOpen(true);
  };

  const openEdit = () => {
    if (!detail) return;
    setEditingPostId(detail.postId);
    setEditorTitle(detail.title);
    setEditorBody(detail.content);
    setEditorError('');
    setEditorOpen(true);
  };

  const submitEditor = async (e: FormEvent) => {
    e.preventDefault();
    if (!editorTitle.trim()) {
      setEditorError('제목을 입력하세요.');
      return;
    }
    if (!editorBody.trim()) {
      setEditorError('내용을 입력하세요.');
      return;
    }
    setEditorError('');
    const isEdit = editingPostId != null;
    try {
      if (isEdit) {
        await apiFetch<void>(`/api/board/posts/${editingPostId}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ title: editorTitle, content: editorBody }),
        });
        setEditorOpen(false);
        await loadPosts();
        if (editingPostId) {
          await loadDetail(editingPostId);
        }
        return;
      }

      const created = await apiFetch<{ postId: number }>('/api/board/posts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ title: editorTitle, content: editorBody }),
      });
      setEditorOpen(false);
      await loadPosts();
      await loadDetail(created.postId);
    } catch (error) {
      setEditorError(formatErrorMessage(error, '게시글 저장에 실패했습니다.'));
    }
  };

  const deletePost = async () => {
    if (!detail) return;
    if (!window.confirm('이 글을 삭제할까요? 댓글도 함께 삭제됩니다.')) return;
    try {
      await apiFetch<void>(`/api/board/posts/${detail.postId}`, { method: 'DELETE' });
      setDetail(null);
      setCurrentPostId(null);
      await loadPosts();
    } catch (error) {
      setPageError(formatErrorMessage(error, '게시글 삭제에 실패했습니다.'));
    }
  };

  const submitComment = async () => {
    if (!detail) return;
    if (!comment.trim()) {
      setCommentError('댓글 내용을 입력하세요.');
      return;
    }
    setCommentError('');
    try {
      await apiFetch<{ commentId: number }>(`/api/board/posts/${detail.postId}/comments`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ content: comment }),
      });
      setComment('');
      await loadDetail(detail.postId);
    } catch (error) {
      setCommentError(formatErrorMessage(error, '댓글 작성에 실패했습니다.'));
    }
  };

  const deleteComment = async (commentId: number) => {
    if (!window.confirm('이 댓글을 삭제할까요?')) return;
    try {
      await apiFetch<void>(`/api/board/comments/${commentId}`, { method: 'DELETE' });
      if (detail) {
        await loadDetail(detail.postId);
      }
    } catch (error) {
      setCommentError(formatErrorMessage(error, '댓글 삭제에 실패했습니다.'));
    }
  };

  return (
    <>
      <div className="board-note-layout">
        <aside className="board-note-sidebar">
          <header className="board-note-sidebar-top note-panel-header">
            <div className="panel-header-title-group">
              <h2 className="note-panel-title">Board</h2>
              <p className="muted-text panel-header-subtitle">게시글 {posts.length}개</p>
            </div>
            <div className="note-panel-actions">
              <button className="btn btn-primary" type="button" onClick={openCreate}>
                글쓰기
              </button>
            </div>
          </header>
          <div className="board-note-list">
            {posts.length === 0 ? (
              <p className="muted-text">등록된 글이 없습니다.</p>
            ) : (
              posts.map((item) => (
                <button
                  key={item.postId}
                  className={`board-note-item ${item.postId === currentPostId ? 'active' : ''}`}
                  type="button"
                  onClick={() => void loadDetail(item.postId)}
                >
                  <div className="board-note-item-header">
                    <strong>{item.title}</strong>
                    <span className="board-note-item-date">{formatBoardListDate(item.updatedAt)}</span>
                  </div>
                  <span className="board-note-item-author">{item.writerUsername || item.writerLoginId}</span>
                </button>
              ))
            )}
          </div>
        </aside>

        <section className="board-note-main">
          {pageError && <p className="error-text">{pageError}</p>}
          {!detail && !loading && <div className="board-note-empty">글을 선택하거나 새 글을 작성하세요.</div>}
          {loading && <div className="board-note-loading">불러오는 중...</div>}

          {detail && (
            <>
              <article className="note-panel board-note-post">
                <div className="board-note-post-header">
                  <h2>{detail.title}</h2>
                  {isOwner && (
                    <div className="board-note-post-actions">
                      <button
                        className="btn btn-secondary icon-action-btn"
                        type="button"
                        aria-label="게시글 수정"
                        onClick={openEdit}
                      >
                        <FiEdit3 aria-hidden="true" />
                      </button>
                      <button
                        className="btn btn-danger icon-action-btn"
                        type="button"
                        aria-label="게시글 삭제"
                        onClick={() => void deletePost()}
                      >
                        <FiTrash2 aria-hidden="true" />
                      </button>
                    </div>
                  )}
                </div>
                <div className="board-note-meta">
                  <span className="board-note-meta-author">{detail.writerUsername || detail.writerLoginId}</span>
                  <span className="board-note-meta-date">{formatBoardDateTime(detail.updatedAt)}</span>
                </div>
                <div dangerouslySetInnerHTML={{ __html: renderMarkdown(detail.content) }} />
              </article>

              <section className="board-note-comment-panels" aria-label={`댓글 ${detail.comments.length}개`}>
                {detail.comments.length === 0 ? (
                  <section className="note-panel board-note-comment-empty">
                    <p className="muted-text">댓글이 없습니다.</p>
                  </section>
                ) : (
                  detail.comments.map((item) => {
                    const commentOwner = me?.loginId === item.writerLoginId;
                    return (
                      <article key={item.commentId} className="note-panel board-note-comment-panel">
                        <div className="board-note-comment-meta">
                          <div className="board-note-comment-meta-main">
                            <span className="board-note-comment-author">{item.writerUsername || item.writerLoginId}</span>
                            <span className="board-note-comment-date">{formatBoardDateTime(item.updatedAt)}</span>
                          </div>
                          {commentOwner && (
                            <button
                              className="btn btn-danger icon-action-btn"
                              type="button"
                              aria-label="댓글 삭제"
                              onClick={() => void deleteComment(item.commentId)}
                            >
                              <FiTrash2 aria-hidden="true" />
                            </button>
                          )}
                        </div>
                        <div className="board-note-comment-body" dangerouslySetInnerHTML={{ __html: renderMarkdown(item.content) }} />
                      </article>
                    );
                  })
                )}
              </section>

              <section className="note-panel board-note-comment-compose-panel">
                <h3 className="board-note-comment-compose-title">댓글 작성</h3>
                <div className="board-note-comment-editor">
                  <div className="board-note-comment-editor-row">
                    <textarea
                      value={comment}
                      onChange={(event) => setComment(event.target.value)}
                      placeholder="댓글을 입력하세요."
                    />
                    <button className="btn btn-primary" type="button" onClick={() => void submitComment()}>
                      댓글 작성
                    </button>
                  </div>
                  {commentError && <p className="error-text">{commentError}</p>}
                </div>
              </section>
            </>
          )}
        </section>
      </div>

      {editorOpen && (
        <div className="note-modal-backdrop" role="presentation">
          <section
            className="board-editor-dialog"
            role="dialog"
            aria-modal="true"
            aria-label={editingPostId ? '글 수정' : '글쓰기'}
            onClick={(event) => event.stopPropagation()}
          >
            <h3>{editingPostId ? '글 수정' : '글쓰기'}</h3>
            <form onSubmit={submitEditor}>
              <div className="form-field">
                <label htmlFor="board-editor-title">제목</label>
                <input
                  id="board-editor-title"
                  value={editorTitle}
                  onChange={(event) => setEditorTitle(event.target.value)}
                />
              </div>
              <div className="form-field">
                <label htmlFor="board-editor-body">본문</label>
                <textarea
                  id="board-editor-body"
                  value={editorBody}
                  onChange={(event) => setEditorBody(event.target.value)}
                  className="board-editor-textarea"
                />
              </div>
              {editorError && <p className="error-text">{editorError}</p>}
              <div className="board-note-actions board-note-actions-right">
                <button className="btn btn-secondary" type="button" onClick={() => setEditorOpen(false)}>
                  취소
                </button>
                <button className="btn btn-primary btn-inline-icon" type="submit">
                  {editingPostId ? (
                    <>
                      <FiEdit3 aria-hidden="true" />
                      수정
                    </>
                  ) : (
                    '등록'
                  )}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </>
  );
}
