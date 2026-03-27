/**
 * 노트 우측 스튜디오 패널의 기본 배경 영역을 제공한다.
 */
export function StudioPanel({
  collapsed,
  onToggle,
}: {
  collapsed: boolean;
  onToggle: () => void;
}) {
  return (
    <aside className={`note-studio-column${collapsed ? ' is-collapsed' : ''}`}>
      <section className={`note-panel note-studio-panel note-collapsible-panel${collapsed ? ' is-hidden' : ''}`}>
        <p className="note-panel-eyebrow">스튜디오</p>
        <h2 className="note-panel-title">준비 중</h2>
        <p className="muted-text">오른쪽 영역은 다음 단계에서 기능을 추가할 예정입니다.</p>
      </section>
      <button
        className="note-collapse-handle note-collapse-handle-right"
        type="button"
        aria-label={collapsed ? '스튜디오 패널 펼치기' : '스튜디오 패널 접기'}
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
            <path d="M12 5L7 10L12 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          ) : (
            <path d="M8 5L13 10L8 15" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
          )}
        </svg>
      </button>
    </aside>
  );
}
