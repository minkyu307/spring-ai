import { useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { FiTrash2, FiX } from 'react-icons/fi';
import { apiFetch } from '../api/http';
import { renderMarkdown } from '../lib/markdown';
import { partitionSupportedUploadFiles, SUPPORTED_UPLOAD_ACCEPT } from '../lib/uploadFileTypes';

type DocumentItem = {
  docId: string;
  title: string;
  filename: string;
  chunkCount: number;
};

type WikiItem = { id: string; name: string };
type WikiPage = { id: string; subject: string; wikiId: string };
type WikiPageDetail = {
  pageId: string;
  title: string;
  body: string;
};

export function DocumentsPage() {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [docs, setDocs] = useState<DocumentItem[]>([]);
  const [status, setStatus] = useState('');
  const [urlInput, setUrlInput] = useState('');
  const [urlStatus, setUrlStatus] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [wikiStatus, setWikiStatus] = useState('');
  const [wikis, setWikis] = useState<WikiItem[]>([]);
  const [pages, setPages] = useState<WikiPage[]>([]);
  const [selected, setSelected] = useState<Record<string, WikiPage>>({});
  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewItems, setPreviewItems] = useState<WikiPageDetail[]>([]);
  const [activeTab, setActiveTab] = useState(0);

  const selectedList = useMemo(() => Object.values(selected), [selected]);

  const loadDocs = async () => {
    const data = await apiFetch<DocumentItem[]>('/api/rag/documents');
    setDocs(data);
  };

  useEffect(() => {
    void loadDocs();
    apiFetch<{ apiKey?: string | null }>('/api/settings/dooray-apikey').then((data) =>
      setApiKey(data.apiKey ?? ''),
    );
  }, []);

  const onFileChange = async (event: ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files ? Array.from(event.target.files) : [];
    if (files.length === 0) return;
    const { supported, unsupportedNames } = partitionSupportedUploadFiles(files);
    const invalidText =
      unsupportedNames.length > 0
        ? `지원하지 않는 파일 제외: ${unsupportedNames.slice(0, 3).join(', ')}${unsupportedNames.length > 3 ? ' 외' : ''}`
        : '';
    if (supported.length === 0) {
      setStatus(invalidText || '지원하지 않는 파일 형식입니다.');
      event.target.value = '';
      return;
    }

    const form = new FormData();
    supported.forEach((file) => form.append('file', file));
    setStatus('업로드/임베딩 중...');
    try {
      await apiFetch('/api/rag/documents/upload', {
        method: 'POST',
        body: form,
      });
      setStatus(invalidText ? `업로드 완료 (${invalidText})` : '업로드 완료');
      await loadDocs();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : '업로드 실패');
    } finally {
      event.target.value = '';
    }
  };

  const ingestUrl = async () => {
    if (!urlInput.trim()) return;
    setUrlStatus('URL 적재 중...');
    try {
      await apiFetch('/api/rag/documents/url', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ url: urlInput.trim() }),
      });
      setUrlStatus('URL 적재 완료');
      setUrlInput('');
      await loadDocs();
    } catch (error) {
      setUrlStatus(error instanceof Error ? error.message : 'URL 적재 실패');
    }
  };

  const deleteDoc = async (docId: string) => {
    if (!window.confirm('이 문서를 삭제할까요?')) return;
    await apiFetch<void>(`/api/rag/documents/${encodeURIComponent(docId)}`, { method: 'DELETE' });
    await loadDocs();
  };

  const saveApiKey = async () => {
    await apiFetch('/api/settings/dooray-apikey', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ apiKey }),
    });
    setWikiStatus('API 키가 저장되었습니다.');
  };

  const loadWikis = async () => {
    setWikiStatus('위키 목록 로딩 중...');
    const response = await apiFetch<{ result: Array<{ id: string; name: string }> }>(
      '/api/dooray/wiki/wikis?size=100',
    );
    setWikis(response.result.map((item) => ({ id: item.id, name: item.name })));
    setWikiStatus('위키 목록을 불러왔습니다. 위키를 선택하세요.');
  };

  const loadWikiPages = async (wikiId: string) => {
    const response = await apiFetch<{ result: Array<{ id: string; subject: string }> }>(
      `/api/dooray/wiki/wikis/${wikiId}/pages`,
    );
    setPages(response.result.map((item) => ({ id: item.id, subject: item.subject, wikiId })));
  };

  const toggleSelected = (page: WikiPage) => {
    setSelected((prev) => {
      const key = `${page.wikiId}:${page.id}`;
      if (prev[key]) {
        const next = { ...prev };
        delete next[key];
        return next;
      }
      return { ...prev, [key]: page };
    });
  };

  const openPreview = async () => {
    if (selectedList.length === 0) {
      setWikiStatus('선택된 페이지가 없습니다.');
      return;
    }
    setWikiStatus('미리보기 로딩 중...');
    const details = await Promise.all(
      selectedList.map(async (item) => {
        const data = await apiFetch<{ result: { subject: string; body?: { content?: string } } }>(
          `/api/dooray/wiki/wikis/${encodeURIComponent(item.wikiId)}/pages/${encodeURIComponent(item.id)}`,
        );
        return {
          pageId: item.id,
          title: data.result.subject || item.subject,
          body: data.result.body?.content || '',
        };
      }),
    );
    setPreviewItems(details);
    setActiveTab(0);
    setPreviewOpen(true);
    setWikiStatus('');
  };

  const ingestSelectedWiki = async () => {
    if (selectedList.length === 0) return;
    setWikiStatus('Vector DB 적재 중...');
    await apiFetch('/api/rag/documents/wiki', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        pages: selectedList.map((item) => ({ wikiId: item.wikiId, pageId: item.id })),
      }),
    });
    setWikiStatus('선택 페이지 적재가 완료되었습니다.');
    setPreviewOpen(false);
    await loadDocs();
  };

  return (
    <>
      <aside className="documents-left">
        <div className="panel" style={{ marginBottom: 12 }}>
          <h3 style={{ marginTop: 0 }}>파일 업로드</h3>
          <input
            ref={inputRef}
            type="file"
            multiple
            accept={SUPPORTED_UPLOAD_ACCEPT}
            style={{ display: 'none' }}
            onChange={onFileChange}
          />
          <button className="btn btn-primary" onClick={() => inputRef.current?.click()}>
            파일 선택
          </button>
          <p style={{ fontSize: 12, color: '#5f6368' }}>{status}</p>
        </div>

        <div className="panel" style={{ marginBottom: 12 }}>
          <h3 style={{ marginTop: 0 }}>URL 업로드</h3>
          <div className="form-field">
            <input
              value={urlInput}
              onChange={(e) => setUrlInput(e.target.value)}
              placeholder="https://example.com/doc"
            />
          </div>
          <button className="btn btn-primary" onClick={ingestUrl}>
            URL 적재
          </button>
          <p style={{ fontSize: 12, color: '#5f6368' }}>{urlStatus}</p>
        </div>

        <div className="panel">
          <h3 style={{ marginTop: 0 }}>Dooray Wiki</h3>
          <div className="form-field">
            <label>API Key</label>
            <input
              type="password"
              value={apiKey ?? ''}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="dooray-api key"
            />
          </div>
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={saveApiKey}>
              저장
            </button>
            <button className="btn btn-secondary" onClick={loadWikis}>
              위키 불러오기
            </button>
          </div>
          <div style={{ marginTop: 10, maxHeight: 140, overflow: 'auto' }}>
            {wikis.map((wiki) => (
              <div key={wiki.id} style={{ paddingBottom: 6 }}>
                <button className="btn btn-secondary" onClick={() => void loadWikiPages(wiki.id)}>
                  {wiki.name}
                </button>
              </div>
            ))}
          </div>
          <div style={{ marginTop: 10, maxHeight: 220, overflow: 'auto' }}>
            {pages.map((page) => {
              const key = `${page.wikiId}:${page.id}`;
              return (
                <label key={key} style={{ display: 'flex', gap: 6, paddingBottom: 4 }}>
                  <input
                    type="checkbox"
                    checked={Boolean(selected[key])}
                    onChange={() => toggleSelected(page)}
                  />
                  <span>{page.subject || page.id}</span>
                </label>
              );
            })}
          </div>
          <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>
            <button className="btn btn-secondary" onClick={openPreview}>
              미리보기
            </button>
            <button className="btn btn-primary" onClick={ingestSelectedWiki}>
              선택 적재
            </button>
          </div>
          <p style={{ fontSize: 12, color: '#5f6368' }}>{wikiStatus}</p>
        </div>
      </aside>

      <section className="documents-main">
        <div className="panel">
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <h3 style={{ marginTop: 0 }}>문서 목록</h3>
            <button className="btn btn-secondary" onClick={loadDocs}>
              새로고침
            </button>
          </div>
          <table className="doc-table">
            <thead>
              <tr>
                <th>제목</th>
                <th>파일명</th>
                <th>청크</th>
                <th>작업</th>
              </tr>
            </thead>
            <tbody>
              {docs.length === 0 ? (
                <tr>
                  <td colSpan={4}>문서가 없습니다.</td>
                </tr>
              ) : (
                docs.map((item) => (
                  <tr key={item.docId}>
                    <td>{item.title || '(untitled)'}</td>
                    <td>{item.filename || '-'}</td>
                    <td>{item.chunkCount}</td>
                    <td>
                      <button
                        className="btn btn-danger icon-action-btn"
                        type="button"
                        aria-label="문서 삭제"
                        onClick={() => void deleteDoc(item.docId)}
                      >
                        <FiTrash2 aria-hidden="true" />
                      </button>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </section>

      {previewOpen && (
        <div className="editor-modal">
          <div className="editor-dialog">
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <h3 style={{ margin: 0 }}>Wiki 미리보기</h3>
              <button className="source-dialog-close-btn" type="button" aria-label="닫기" onClick={() => setPreviewOpen(false)}>
                <FiX aria-hidden="true" />
              </button>
            </div>
            <div style={{ display: 'flex', gap: 8, marginBottom: 8, flexWrap: 'wrap' }}>
              {previewItems.map((item, index) => (
                <button
                  key={item.pageId}
                  className="btn btn-secondary"
                  style={{
                    border: index === activeTab ? '1px solid #1967d2' : undefined,
                  }}
                  onClick={() => setActiveTab(index)}
                >
                  {item.title}
                </button>
              ))}
            </div>
            {previewItems[activeTab] && (
              <div
                className="panel"
                dangerouslySetInnerHTML={{
                  __html: renderMarkdown(previewItems[activeTab].body || '(본문 없음)'),
                }}
              />
            )}
            <div style={{ marginTop: 12, display: 'flex', justifyContent: 'flex-end' }}>
              <button className="btn btn-primary" onClick={ingestSelectedWiki}>
                Vector DB 적재
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
