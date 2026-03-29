import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent, DragEvent } from 'react';
import { FiTrash2, FiX } from 'react-icons/fi';
import { formatErrorMessage } from '../../api/errors';
import { apiFetch } from '../../api/http';
import { renderMarkdown } from '../../lib/markdown';
import {
  partitionSupportedUploadFiles,
  SUPPORTED_UPLOAD_ACCEPT,
  SUPPORTED_UPLOAD_PLACEHOLDER_TEXT,
} from '../../lib/uploadFileTypes';

type DocumentItem = {
  docId: string;
  title: string;
  filename: string;
  chunkCount: number;
};

type WikiItem = { id: string; name: string };
type WikiPage = { id: string; subject: string; wikiId: string };
type WikiPageDetail = {
  wikiId: string;
  pageId: string;
  title: string;
  body: string;
};
type WikiApiResponse<T> = { result: T };
type WikiSubtreePage = {
  id: string;
  subject?: string;
  parentPageId?: string | null;
};
type WikiTreeNode = {
  key: string;
  nodeType: 'wiki' | 'page';
  wikiId: string;
  pageId?: string;
  label: string;
  depth: number;
  expanded: boolean;
  loading: boolean;
  loaded: boolean;
  hasChildren: boolean;
  selectable: boolean;
  checked: boolean;
  indeterminate: boolean;
  children: WikiTreeNode[];
};

type SourceTab = 'file' | 'url' | 'wiki';

const SOURCE_TAB_LABELS: Record<SourceTab, string> = {
  file: '파일 업로드',
  url: 'URL 업로드',
  wiki: 'Dooray WiKi',
};
const MAX_WIKI_SELECTABLE_PAGES = 30;
const WIKI_SELECTION_LIMIT_MESSAGE = `[Dooray v1 api 요청 제한으로 인해 최대 ${MAX_WIKI_SELECTABLE_PAGES} 페이지 선택 가능합니다]`;

/**
 * 위키 루트 노드를 생성한다.
 */
function createWikiNode(wiki: WikiItem): WikiTreeNode {
  return {
    key: `wiki:${wiki.id}`,
    nodeType: 'wiki',
    wikiId: wiki.id,
    label: wiki.name,
    depth: 0,
    expanded: false,
    loading: false,
    loaded: false,
    hasChildren: true,
    selectable: false,
    checked: false,
    indeterminate: false,
    children: [],
  };
}

/**
 * 페이지 트리 노드를 생성한다.
 */
function createPageNode({
  wikiId,
  pageId,
  label,
  depth,
  selectable,
  hasChildren,
  loaded,
  children,
}: {
  wikiId: string;
  pageId: string;
  label: string;
  depth: number;
  selectable: boolean;
  hasChildren: boolean;
  loaded: boolean;
  children: WikiTreeNode[];
}): WikiTreeNode {
  return {
    key: `page:${wikiId}:${pageId}`,
    nodeType: 'page',
    wikiId,
    pageId,
    label,
    depth,
    expanded: false,
    loading: false,
    loaded,
    hasChildren,
    selectable,
    checked: false,
    indeterminate: false,
    children,
  };
}

/**
 * subtree 평탄 목록을 parentPageId 기준 트리로 변환한다.
 */
function buildSubtreeNodes(
  wikiId: string,
  parentPageId: string,
  allPages: WikiSubtreePage[],
  depth: number,
): WikiTreeNode[] {
  return allPages
    .filter((page) => String(page.parentPageId ?? '') === String(parentPageId))
    .map((page) => {
      const children = buildSubtreeNodes(wikiId, page.id, allPages, depth + 1);
      return createPageNode({
        wikiId,
        pageId: page.id,
        label: page.subject || page.id,
        depth,
        selectable: depth >= 2,
        hasChildren: children.length > 0,
        loaded: true,
        children,
      });
    });
}

/**
 * 대상 key 노드를 불변 업데이트한다.
 */
function updateTreeNodeByKey(
  nodes: WikiTreeNode[],
  targetKey: string,
  updater: (node: WikiTreeNode) => WikiTreeNode,
): WikiTreeNode[] {
  let changed = false;
  const nextNodes = nodes.map((node) => {
    if (node.key === targetKey) {
      changed = true;
      return updater(node);
    }
    if (node.children.length === 0) {
      return node;
    }
    const nextChildren = updateTreeNodeByKey(node.children, targetKey, updater);
    if (nextChildren !== node.children) {
      changed = true;
      return { ...node, children: nextChildren };
    }
    return node;
  });
  return changed ? nextNodes : nodes;
}

/**
 * 선택 상태를 현재 노드부터 하위로 전파한다.
 */
function applySelectionToBranch(node: WikiTreeNode, checked: boolean): WikiTreeNode {
  const nextChildren = node.children.map((child) => applySelectionToBranch(child, checked));
  return {
    ...node,
    checked: node.selectable ? checked : node.checked,
    indeterminate: false,
    children: nextChildren,
  };
}

/**
 * 하위 선택 상태를 기반으로 상위 checked/indeterminate를 계산한다.
 */
function syncSelectableNodeState(node: WikiTreeNode): WikiTreeNode {
  if (!node.selectable) {
    return node;
  }
  const selectableChildren = node.children.filter((child) => child.selectable);
  if (selectableChildren.length === 0) {
    return node.indeterminate ? { ...node, indeterminate: false } : node;
  }
  const allChecked = selectableChildren.every((child) => child.checked && !child.indeterminate);
  const noneChecked = selectableChildren.every((child) => !child.checked && !child.indeterminate);
  if (allChecked) {
    return { ...node, checked: true, indeterminate: false };
  }
  if (noneChecked) {
    return { ...node, checked: false, indeterminate: false };
  }
  return { ...node, checked: false, indeterminate: true };
}

/**
 * 특정 노드의 체크 상태를 바꾸고 상하위 전파를 반영한다.
 */
function toggleWikiTreeSelection(nodes: WikiTreeNode[], targetKey: string, checked: boolean): WikiTreeNode[] {
  const visit = (node: WikiTreeNode): [WikiTreeNode, boolean] => {
    let nextNode = node;
    let changed = false;
    if (node.key === targetKey) {
      nextNode = applySelectionToBranch(node, checked);
      changed = true;
    } else if (node.children.length > 0) {
      const nextChildren = node.children.map((child) => {
        const [nextChild, childChanged] = visit(child);
        if (childChanged) {
          changed = true;
        }
        return nextChild;
      });
      if (changed) {
        nextNode = { ...node, children: nextChildren };
      }
    }
    if (changed && nextNode.selectable) {
      nextNode = syncSelectableNodeState(nextNode);
    }
    return [nextNode, changed];
  };

  let changed = false;
  const nextNodes = nodes.map((node) => {
    const [nextNode, nodeChanged] = visit(node);
    if (nodeChanged) {
      changed = true;
    }
    return nextNode;
  });
  return changed ? nextNodes : nodes;
}

/**
 * 현재 트리에서 체크된 페이지 목록을 수집한다.
 */
function collectCheckedWikiPages(nodes: WikiTreeNode[]): WikiPage[] {
  const selectedPages: WikiPage[] = [];
  const seen = new Set<string>();

  const visit = (node: WikiTreeNode) => {
    if (node.selectable && node.checked && node.pageId) {
      const key = `${node.wikiId}:${node.pageId}`;
      if (!seen.has(key)) {
        seen.add(key);
        selectedPages.push({
          id: node.pageId,
          subject: node.label,
          wikiId: node.wikiId,
        });
      }
    }
    node.children.forEach(visit);
  };

  nodes.forEach(visit);
  return selectedPages;
}

/**
 * 노트 좌측 문서 소스 목록과 소스 추가 다이얼로그를 제공한다.
 */
export function SourcesPanel({
  collapsed,
  onToggle,
}: {
  collapsed: boolean;
  onToggle: () => void;
}) {
  const inputRef = useRef<HTMLInputElement | null>(null);
  const [docs, setDocs] = useState<DocumentItem[]>([]);
  const [loadingDocs, setLoadingDocs] = useState(false);

  const [dialogOpen, setDialogOpen] = useState(false);
  const [activeTab, setActiveTab] = useState<SourceTab>('file');
  const [fileDragOver, setFileDragOver] = useState(false);
  const [pendingFiles, setPendingFiles] = useState<File[]>([]);

  const [status, setStatus] = useState('');
  const [urlInput, setUrlInput] = useState('');
  const [urlStatus, setUrlStatus] = useState('');

  const [apiKey, setApiKey] = useState('');
  const [wikiStatus, setWikiStatus] = useState('');
  const [wikiTreeNodes, setWikiTreeNodes] = useState<WikiTreeNode[]>([]);
  const [wikiTreeLoaded, setWikiTreeLoaded] = useState(false);
  const [wikiTreeLoading, setWikiTreeLoading] = useState(false);

  const [previewOpen, setPreviewOpen] = useState(false);
  const [previewItems, setPreviewItems] = useState<WikiPageDetail[]>([]);
  const [previewIndex, setPreviewIndex] = useState(0);

  const selectedList = useMemo(() => collectCheckedWikiPages(wikiTreeNodes), [wikiTreeNodes]);
  const wikiSelectionLimitExceeded = selectedList.length > MAX_WIKI_SELECTABLE_PAGES;
  const wikiDisplayStatus = wikiSelectionLimitExceeded ? WIKI_SELECTION_LIMIT_MESSAGE : wikiStatus;
  const documentsHeaderSubtitle = loadingDocs ? '문서 목록 로딩 중' : `문서 ${docs.length}개`;

  const loadDocs = useCallback(async () => {
    setLoadingDocs(true);
    try {
      const data = await apiFetch<DocumentItem[]>('/api/rag/documents');
      setDocs(data);
      setStatus('');
    } catch (error) {
      setStatus(formatErrorMessage(error, '문서 목록을 불러오지 못했습니다.'));
    } finally {
      setLoadingDocs(false);
    }
  }, []);

  useEffect(() => {
    void loadDocs();
    void (async () => {
      try {
        const data = await apiFetch<{ apiKey?: string | null }>('/api/settings/dooray-apikey');
        setApiKey(data.apiKey ?? '');
      } catch {
        setWikiStatus('Dooray API Key를 불러오지 못했습니다.');
      }
    })();
  }, [loadDocs]);

  useEffect(() => {
    if (!collapsed) return;
    setDialogOpen(false);
    setPreviewOpen(false);
  }, [collapsed]);

  const addPendingFiles = (files: File[]) => {
    if (files.length === 0) return;
    const { supported, unsupportedNames } = partitionSupportedUploadFiles(files);
    const invalidText =
      unsupportedNames.length > 0
        ? `지원하지 않는 파일 제외: ${unsupportedNames.slice(0, 3).join(', ')}${unsupportedNames.length > 3 ? ' 외' : ''}`
        : '';
    if (supported.length === 0) {
      setStatus(invalidText || '지원하지 않는 파일 형식입니다.');
      return;
    }

    setPendingFiles((prev) => {
      const next = [...prev];
      const seen = new Set(prev.map((file) => `${file.name}:${file.size}:${file.lastModified}`));
      supported.forEach((file) => {
        const key = `${file.name}:${file.size}:${file.lastModified}`;
        if (seen.has(key)) return;
        seen.add(key);
        next.push(file);
      });
      return next;
    });
    setStatus(invalidText);
  };

  const uploadFiles = useCallback(
    async (files: File[]) => {
      if (files.length === 0) return;
      const form = new FormData();
      files.forEach((file) => form.append('file', file));
      setStatus('업로드/임베딩 중...');
      try {
        await apiFetch('/api/rag/documents/upload', {
          method: 'POST',
          body: form,
        });
        setStatus('업로드 완료');
        setPendingFiles([]);
        await loadDocs();
      } catch (error) {
        setStatus(formatErrorMessage(error, '업로드 실패'));
      }
    },
    [loadDocs],
  );

  const onFileChange = (event: ChangeEvent<HTMLInputElement>) => {
    const files = event.target.files ? Array.from(event.target.files) : [];
    addPendingFiles(files);
    event.target.value = '';
  };

  const onFileDragOver = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    event.dataTransfer.dropEffect = 'copy';
    setFileDragOver(true);
  };

  const onFileDragLeave = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setFileDragOver(false);
  };

  const onFileDrop = (event: DragEvent<HTMLDivElement>) => {
    event.preventDefault();
    setFileDragOver(false);
    const files = Array.from(event.dataTransfer.files ?? []);
    addPendingFiles(files);
  };

  const removePendingFile = (target: File) => {
    setPendingFiles((prev) =>
      prev.filter(
        (file) =>
          !(
            file.name === target.name &&
            file.size === target.size &&
            file.lastModified === target.lastModified
          ),
      ),
    );
  };

  const uploadPendingFiles = async () => {
    await uploadFiles(pendingFiles);
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
      setUrlStatus(formatErrorMessage(error, 'URL 적재 실패'));
    }
  };

  const deleteDoc = async (docId: string) => {
    if (!window.confirm('이 문서를 삭제할까요?')) return;
    try {
      await apiFetch<void>(`/api/rag/documents/${encodeURIComponent(docId)}`, { method: 'DELETE' });
      await loadDocs();
    } catch (error) {
      setStatus(formatErrorMessage(error, '문서 삭제에 실패했습니다.'));
    }
  };

  const saveApiKey = async () => {
    try {
      await apiFetch('/api/settings/dooray-apikey', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ apiKey }),
      });
      setWikiStatus('API Key가 저장되었습니다.');
    } catch (error) {
      setWikiStatus(formatErrorMessage(error, 'API Key 저장 실패'));
    }
  };

  const loadWikiTree = async () => {
    setWikiStatus('위키 목록 로딩 중...');
    setWikiTreeLoading(true);
    setWikiTreeLoaded(true);
    setWikiTreeNodes([]);
    try {
      const response = await apiFetch<WikiApiResponse<Array<{ id: string; name: string }>>>(
        '/api/dooray/wiki/wikis?size=100',
      );
      const rootNodes = response.result.map((item) => createWikiNode({ id: item.id, name: item.name }));
      setWikiTreeNodes(rootNodes);
      setWikiStatus(rootNodes.length === 0 ? '접근 가능한 위키가 없습니다.' : '위키 목록을 불러왔습니다.');
    } catch (error) {
      setWikiStatus(formatErrorMessage(error, '위키 목록 로딩 실패'));
    } finally {
      setWikiTreeLoading(false);
    }
  };

  const toggleWikiTreeNode = async (node: WikiTreeNode) => {
    if (node.loading) return;
    const canToggle = node.loading || !node.loaded || node.hasChildren;
    if (!canToggle) return;

    if (node.expanded) {
      setWikiTreeNodes((prev) =>
        updateTreeNodeByKey(prev, node.key, (current) => ({
          ...current,
          expanded: false,
        })),
      );
      return;
    }

    if (node.loaded) {
      setWikiTreeNodes((prev) =>
        updateTreeNodeByKey(prev, node.key, (current) => ({
          ...current,
          expanded: true,
        })),
      );
      return;
    }

    setWikiTreeNodes((prev) =>
      updateTreeNodeByKey(prev, node.key, (current) => ({
        ...current,
        expanded: true,
        loading: true,
      })),
    );

    try {
      if (node.nodeType === 'wiki') {
        const response = await apiFetch<WikiApiResponse<Array<{ id: string; subject: string }>>>(
          `/api/dooray/wiki/wikis/${encodeURIComponent(node.wikiId)}/pages`,
        );
        const childPages = response.result.map((page) =>
          createPageNode({
            wikiId: node.wikiId,
            pageId: page.id,
            label: page.subject || page.id,
            depth: node.depth + 1,
            selectable: false,
            hasChildren: true,
            loaded: false,
            children: [],
          }),
        );
        setWikiTreeNodes((prev) =>
          updateTreeNodeByKey(prev, node.key, (current) => ({
            ...current,
            loading: false,
            loaded: true,
            hasChildren: childPages.length > 0,
            expanded: childPages.length > 0,
            children: childPages,
          })),
        );
        if (childPages.length === 0) {
          setWikiStatus('하위 페이지가 없습니다.');
        }
        return;
      }

      if (!node.pageId) {
        throw new Error('페이지 정보가 올바르지 않습니다.');
      }

      const response = await apiFetch<WikiApiResponse<WikiSubtreePage[]>>(
        `/api/dooray/wiki/wikis/${encodeURIComponent(node.wikiId)}/pages/${encodeURIComponent(node.pageId)}/subtree`,
      );
      const subtreeNodes = buildSubtreeNodes(node.wikiId, node.pageId, response.result ?? [], node.depth + 1);
      setWikiTreeNodes((prev) =>
        updateTreeNodeByKey(prev, node.key, (current) => {
          const nextChildren = current.checked
            ? subtreeNodes.map((child) => applySelectionToBranch(child, true))
            : subtreeNodes;
          const nextNode: WikiTreeNode = {
            ...current,
            loading: false,
            loaded: true,
            hasChildren: nextChildren.length > 0,
            expanded: nextChildren.length > 0,
            children: nextChildren,
            indeterminate: false,
          };
          return current.selectable ? syncSelectableNodeState(nextNode) : nextNode;
        }),
      );
      if (subtreeNodes.length === 0) {
        setWikiStatus('하위 페이지가 없습니다.');
      }
    } catch (error) {
      setWikiStatus(formatErrorMessage(error, '위키 트리 로딩 실패'));
      setWikiTreeNodes((prev) =>
        updateTreeNodeByKey(prev, node.key, (current) => ({
          ...current,
          loading: false,
          expanded: false,
        })),
      );
    }
  };

  const onWikiNodeCheckChange = (nodeKey: string, checked: boolean) => {
    setWikiTreeNodes((prev) => toggleWikiTreeSelection(prev, nodeKey, checked));
  };

  const openPreview = async () => {
    if (selectedList.length === 0) {
      setWikiStatus('선택된 페이지가 없습니다.');
      return;
    }
    if (wikiSelectionLimitExceeded) {
      setWikiStatus(WIKI_SELECTION_LIMIT_MESSAGE);
      return;
    }
    setWikiStatus('미리보기 로딩 중...');
    try {
      const details = await Promise.all(
        selectedList.map(async (item) => {
          const data = await apiFetch<{ result: { subject: string; body?: { content?: string } } }>(
            `/api/dooray/wiki/wikis/${encodeURIComponent(item.wikiId)}/pages/${encodeURIComponent(item.id)}`,
          );
          return {
            wikiId: item.wikiId,
            pageId: item.id,
            title: data.result.subject || item.subject,
            body: data.result.body?.content || '',
          };
        }),
      );
      setPreviewItems(details);
      setPreviewIndex(0);
      setPreviewOpen(true);
      setWikiStatus('');
    } catch (error) {
      setWikiStatus(formatErrorMessage(error, '미리보기 로딩 실패'));
    }
  };

  const ingestSelectedWiki = async () => {
    if (selectedList.length === 0) return;
    if (wikiSelectionLimitExceeded) {
      setWikiStatus(WIKI_SELECTION_LIMIT_MESSAGE);
      return;
    }
    setWikiStatus('Vector DB 적재 중...');
    try {
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
    } catch (error) {
      setWikiStatus(formatErrorMessage(error, '선택 페이지 적재 실패'));
    }
  };

  /**
   * 위키 트리 노드를 재귀 렌더링한다.
   */
  const renderWikiTreeNodes = (nodes: WikiTreeNode[]) =>
    nodes.map((node) => {
      const canToggle = node.loading || !node.loaded || node.hasChildren;
      return (
        <div className="tree-node" key={node.key}>
          <div className="tree-row" style={{ paddingLeft: `${8 + node.depth * 20}px` }}>
            <button
              className={`tree-toggle${canToggle ? '' : ' spacer'}`}
              type="button"
              aria-label={node.expanded ? '접기' : '펼치기'}
              disabled={!canToggle}
              onClick={() => void toggleWikiTreeNode(node)}
            >
              {node.loading ? '...' : node.expanded ? '▾' : '▸'}
            </button>
            {node.selectable && (
              <input
                className="tree-cb"
                type="checkbox"
                checked={node.checked}
                onChange={(event) => onWikiNodeCheckChange(node.key, event.target.checked)}
                ref={(element) => {
                  if (element) {
                    element.indeterminate = node.indeterminate;
                  }
                }}
              />
            )}
            <span className={`tree-label${node.nodeType === 'wiki' ? ' wiki-label' : ''}`}>{node.label}</span>
          </div>
          {node.expanded && (
            <div className="tree-children open">
              {node.loading ? (
                <div className="tree-loading">로딩 중...</div>
              ) : node.children.length === 0 ? (
                <div className="tree-loading">하위 페이지 없음</div>
              ) : (
                renderWikiTreeNodes(node.children)
              )}
            </div>
          )}
        </div>
      );
    });

  return (
    <aside className={`note-sources-column${collapsed ? ' is-collapsed' : ''}`}>
      <section className={`note-panel note-sources-list note-collapsible-panel${collapsed ? ' is-hidden' : ''}`}>
        <header className="note-panel-header">
          <div className="panel-header-title-group">
            <h2 className="note-panel-title">Documents</h2>
            <p className="muted-text panel-header-subtitle">{documentsHeaderSubtitle}</p>
          </div>
          <div className="note-panel-actions">
            <button
              className="btn btn-primary"
              type="button"
              onClick={() => {
                setDialogOpen(true);
                setActiveTab('file');
              }}
            >
              소스 추가
            </button>
            <button className="btn btn-secondary" type="button" onClick={() => void loadDocs()}>
              새로고침
            </button>
          </div>
        </header>
        <div className="source-items-scroll">
          {loadingDocs && <p className="muted-text">문서 목록을 불러오는 중...</p>}
          {!loadingDocs && docs.length === 0 && <p className="muted-text">등록된 문서가 없습니다.</p>}
          {docs.map((item) => (
            <article className="source-item" key={item.docId}>
              <div className="source-item-main">
                <strong>{item.title || '(untitled)'}</strong>
              </div>
              <button
                className="btn btn-danger icon-action-btn"
                type="button"
                aria-label="문서 삭제"
                onClick={() => void deleteDoc(item.docId)}
              >
                <FiTrash2 aria-hidden="true" />
              </button>
            </article>
          ))}
        </div>
      </section>

      <button
        className="note-collapse-handle note-collapse-handle-left"
        type="button"
        aria-label={collapsed ? 'Documents 패널 펼치기' : 'Documents 패널 접기'}
        onClick={onToggle}
      >
        <svg
          className="note-collapse-icon"
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          aria-hidden="true"
        >
          {collapsed ? (
            <path d="M8 5L13 10L8 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          ) : (
            <path d="M12 5L7 10L12 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          )}
        </svg>
      </button>

      {dialogOpen && (
        <div className="note-modal-backdrop" role="presentation">
          <section
            className="source-dialog"
            role="dialog"
            aria-modal="true"
            aria-label="소스 추가"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="note-panel-row">
              <h3 className="source-dialog-title">소스 추가</h3>
              <button className="source-dialog-close-btn" type="button" aria-label="닫기" onClick={() => setDialogOpen(false)}>
                <FiX aria-hidden="true" />
              </button>
            </div>

            <div className="source-tab-row" role="tablist" aria-label="소스 유형">
              {(Object.keys(SOURCE_TAB_LABELS) as SourceTab[]).map((tab) => (
                <button
                  key={tab}
                  className={`source-tab ${activeTab === tab ? 'active' : ''}`}
                  type="button"
                  role="tab"
                  aria-selected={activeTab === tab}
                  onClick={() => setActiveTab(tab)}
                >
                  {SOURCE_TAB_LABELS[tab]}
                </button>
              ))}
            </div>

            {activeTab === 'file' && (
              <div className="source-tab-panel" role="tabpanel">
                <input ref={inputRef} type="file" multiple accept={SUPPORTED_UPLOAD_ACCEPT} hidden onChange={onFileChange} />
                <div
                  className={`source-dropzone ${fileDragOver ? 'is-drag-over' : ''}`}
                  onDragOver={onFileDragOver}
                  onDragLeave={onFileDragLeave}
                  onDrop={onFileDrop}
                >
                  <p className="source-dropzone-placeholder">{SUPPORTED_UPLOAD_PLACEHOLDER_TEXT}</p>
                </div>
                <div className="source-ready-list">
                  {pendingFiles.length === 0 ? (
                    <p className="muted-text source-ready-empty">준비된 파일이 없습니다.</p>
                  ) : (
                    pendingFiles.map((file) => (
                      <article className="source-ready-item" key={`${file.name}:${file.size}:${file.lastModified}`}>
                        <span className="source-ready-name">{file.name}</span>
                        <button
                          className="btn btn-danger icon-action-btn"
                          type="button"
                          aria-label="준비 파일 제거"
                          onClick={() => removePendingFile(file)}
                        >
                          <FiTrash2 aria-hidden="true" />
                        </button>
                      </article>
                    ))
                  )}
                </div>
                <div className="source-upload-actions">
                  <button className="btn btn-secondary" type="button" onClick={() => inputRef.current?.click()}>
                    파일 선택
                  </button>
                  <button
                    className="btn btn-primary"
                    type="button"
                    disabled={pendingFiles.length === 0}
                    onClick={() => void uploadPendingFiles()}
                  >
                    업로드
                  </button>
                </div>
                <p className="status-text">{status}</p>
              </div>
            )}

            {activeTab === 'url' && (
              <div className="source-tab-panel" role="tabpanel">
                <div className="form-field">
                  <label htmlFor="url-source-input">URL</label>
                  <input
                    id="url-source-input"
                    value={urlInput}
                    onChange={(event) => setUrlInput(event.target.value)}
                    placeholder="https://example.com/document"
                  />
                </div>
                <button className="btn btn-primary" type="button" onClick={() => void ingestUrl()}>
                  URL 적재
                </button>
                <p className="status-text">{urlStatus}</p>
              </div>
            )}

            {activeTab === 'wiki' && (
              <div className="source-tab-panel" role="tabpanel">
                <div className="form-field">
                  <label htmlFor="dooray-api-key">Dooray API Key</label>
                  <div className="form-field-inline-action">
                    <input
                      id="dooray-api-key"
                      type="password"
                      value={apiKey ?? ''}
                      onChange={(event) => setApiKey(event.target.value)}
                      placeholder="dooray-api key"
                    />
                    <button className="btn btn-secondary" type="button" onClick={() => void saveApiKey()}>
                      저장
                    </button>
                  </div>
                </div>
                <div className="wiki-tree-section">
                  <div className="wiki-tree-header">
                    <strong className="wiki-tree-title">WIKI 페이지 선택</strong>
                    <button
                      className="btn btn-secondary wiki-tree-load-btn"
                      type="button"
                      disabled={wikiTreeLoading}
                      onClick={() => void loadWikiTree()}
                    >
                      {wikiTreeLoading ? '불러오는 중...' : '트리 불러오기'}
                    </button>
                  </div>
                  <div className="wiki-tree-wrap">
                    {!wikiTreeLoaded && !wikiTreeLoading && (
                      <div className="wiki-tree-empty">트리 불러오기 버튼을 눌러 위키 목록을 가져오세요.</div>
                    )}
                    {wikiTreeLoading && wikiTreeNodes.length === 0 && (
                      <div className="wiki-tree-empty">불러오는 중...</div>
                    )}
                    {wikiTreeLoaded && !wikiTreeLoading && wikiTreeNodes.length === 0 && (
                      <div className="wiki-tree-empty">접근 가능한 위키가 없습니다.</div>
                    )}
                    {wikiTreeNodes.length > 0 && renderWikiTreeNodes(wikiTreeNodes)}
                  </div>
                </div>

                <div className="wiki-upload-bar">
                  <button
                    className="btn btn-primary"
                    type="button"
                    disabled={selectedList.length === 0 || wikiSelectionLimitExceeded}
                    onClick={() => void openPreview()}
                  >
                    선택 항목 업로드
                  </button>
                  <span className="wiki-selected-count">
                    {selectedList.length > 0 ? `${selectedList.length}개 선택됨` : '선택된 항목 없음'}
                  </span>
                </div>
                <p className="status-text">{wikiDisplayStatus}</p>
              </div>
            )}
          </section>
        </div>
      )}

      {previewOpen && (
        <div className="note-modal-backdrop" role="presentation">
          <section
            className="source-preview-dialog"
            role="dialog"
            aria-modal="true"
            aria-label="Dooray Wiki 미리보기"
            onClick={(event) => event.stopPropagation()}
          >
            <div className="note-panel-row">
              <h3 className="source-dialog-title">Dooray Wiki 미리보기</h3>
              <button className="source-dialog-close-btn" type="button" aria-label="닫기" onClick={() => setPreviewOpen(false)}>
                <FiX aria-hidden="true" />
              </button>
            </div>
            <div className="source-tab-row source-preview-tab-row">
              {previewItems.map((item, index) => (
                <button
                  key={`${item.wikiId}:${item.pageId}`}
                  className={`source-tab ${index === previewIndex ? 'active' : ''}`}
                  type="button"
                  onClick={() => setPreviewIndex(index)}
                >
                  {item.title}
                </button>
              ))}
            </div>
            {previewItems[previewIndex] && (
              <div
                className="source-preview-body"
                dangerouslySetInnerHTML={{
                  __html: renderMarkdown(previewItems[previewIndex].body || '(본문 없음)'),
                }}
              />
            )}
            <div className="note-inline-actions note-inline-actions-right">
              <button className="btn btn-primary" type="button" onClick={() => void ingestSelectedWiki()}>
                Vector DB 적재
              </button>
            </div>
          </section>
        </div>
      )}
    </aside>
  );
}
