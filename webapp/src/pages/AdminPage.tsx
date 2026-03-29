import { useEffect, useMemo, useState } from 'react';
import { formatErrorMessage } from '../api/errors';
import { apiFetch } from '../api/http';

type CurrentUser = {
  authenticated: boolean;
  loginId: string;
  email: string;
  roles: string[];
};

type AdminUserListItem = {
  loginId: string;
  username: string;
  email: string;
  roleName: string;
  createdAt: string;
};

function formatAdminDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  const hour = String(date.getHours()).padStart(2, '0');
  const minute = String(date.getMinutes()).padStart(2, '0');
  return `${year}.${month}.${day} ${hour}:${minute}`;
}

export function AdminPage() {
  const [users, setUsers] = useState<AdminUserListItem[]>([]);
  const [selectedMap, setSelectedMap] = useState<Record<string, boolean>>({});
  const [mailContent, setMailContent] = useState('');
  const [authChecked, setAuthChecked] = useState(false);
  const [authorized, setAuthorized] = useState(false);
  const [loadingUsers, setLoadingUsers] = useState(false);
  const [actionLoading, setActionLoading] = useState(false);
  const [currentLoginId, setCurrentLoginId] = useState('');
  const [statusMessage, setStatusMessage] = useState('');
  const [errorMessage, setErrorMessage] = useState('');

  const selectedLoginIds = useMemo(
    () => users.filter((user) => Boolean(selectedMap[user.loginId])).map((user) => user.loginId),
    [selectedMap, users],
  );
  const allSelected = users.length > 0 && selectedLoginIds.length === users.length;

  const loadUsers = async () => {
    setLoadingUsers(true);
    try {
      const data = await apiFetch<AdminUserListItem[]>('/api/admin/users');
      setUsers(data);
      setErrorMessage('');
    } catch (error) {
      setErrorMessage(formatErrorMessage(error, '사용자 목록을 불러오지 못했습니다.'));
    } finally {
      setLoadingUsers(false);
    }
  };

  useEffect(() => {
    let mounted = true;
    void (async () => {
      try {
        const me = await apiFetch<CurrentUser>('/api/auth/me');
        if (!mounted) return;

        const roles = Array.isArray(me.roles) ? me.roles : [];
        setCurrentLoginId(me.loginId);
        if (!roles.includes('ROLE_ADMIN')) {
          setAuthorized(false);
          setErrorMessage('관리자 권한이 없습니다.');
          return;
        }

        setAuthorized(true);
        await loadUsers();
      } catch (error) {
        if (!mounted) return;
        setAuthorized(false);
        setErrorMessage(formatErrorMessage(error, '권한 확인에 실패했습니다.'));
      } finally {
        if (!mounted) return;
        setAuthChecked(true);
      }
    })();
    return () => {
      mounted = false;
    };
  }, []);

  useEffect(() => {
    setSelectedMap((previous) => {
      const next: Record<string, boolean> = {};
      for (const user of users) {
        if (previous[user.loginId]) {
          next[user.loginId] = true;
        }
      }
      return next;
    });
  }, [users]);

  const toggleSelectAll = () => {
    if (allSelected) {
      setSelectedMap({});
      return;
    }
    const next: Record<string, boolean> = {};
    for (const user of users) {
      next[user.loginId] = true;
    }
    setSelectedMap(next);
  };

  const toggleSelectUser = (loginId: string) => {
    setSelectedMap((previous) => ({
      ...previous,
      [loginId]: !previous[loginId],
    }));
  };

  const deleteSelectedUsers = async () => {
    if (selectedLoginIds.length === 0) {
      setStatusMessage('삭제할 사용자를 선택하세요.');
      return;
    }
    if (!window.confirm(`선택한 사용자 ${selectedLoginIds.length}명을 삭제할까요?`)) {
      return;
    }

    setActionLoading(true);
    setStatusMessage('');
    setErrorMessage('');
    try {
      const result = await apiFetch<{ deletedCount: number }>('/api/admin/users', {
        method: 'DELETE',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ loginIds: selectedLoginIds }),
      });
      setStatusMessage(`삭제 완료: ${result.deletedCount}명`);
      setSelectedMap({});
      await loadUsers();
    } catch (error) {
      setErrorMessage(formatErrorMessage(error, '사용자 삭제에 실패했습니다.'));
    } finally {
      setActionLoading(false);
    }
  };

  const sendUpdateNotification = async () => {
    if (selectedLoginIds.length === 0) {
      setStatusMessage('메일을 보낼 사용자를 선택하세요.');
      return;
    }
    if (!mailContent.trim()) {
      setStatusMessage('업데이트 메일 내용을 입력하세요.');
      return;
    }
    if (!window.confirm(`선택한 사용자 ${selectedLoginIds.length}명에게 업데이트 메일을 보낼까요?`)) {
      return;
    }

    setActionLoading(true);
    setStatusMessage('');
    setErrorMessage('');
    try {
      const result = await apiFetch<{ sentCount: number }>('/api/admin/users/update-notification', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          loginIds: selectedLoginIds,
          content: mailContent.trim(),
        }),
      });
      setStatusMessage(`메일 발송 완료: ${result.sentCount}명`);
    } catch (error) {
      setErrorMessage(formatErrorMessage(error, '업데이트 메일 발송에 실패했습니다.'));
    } finally {
      setActionLoading(false);
    }
  };

  if (!authChecked) {
    return <div className="board-note-loading">권한을 확인하는 중...</div>;
  }

  if (!authorized) {
    return (
      <div className="admin-page-layout">
        <section className="note-panel admin-forbidden-panel">
          <h2 className="note-panel-title">Admin</h2>
          <p className="error-text">{errorMessage || '관리자 권한이 없습니다.'}</p>
        </section>
      </div>
    );
  }

  return (
    <div className="admin-page-layout">
      <section className="note-panel admin-user-panel">
        <header className="admin-panel-header">
          <div className="panel-header-title-group">
            <h2 className="note-panel-title">사용자 목록</h2>
            <p className="muted-text">총 {users.length}명</p>
          </div>
          <button className="btn btn-secondary" type="button" onClick={() => void loadUsers()} disabled={loadingUsers || actionLoading}>
            새로고침
          </button>
        </header>

        <div className="admin-user-table-wrap">
          <table className="admin-user-table">
            <thead>
              <tr>
                <th>
                  <input
                    className="admin-user-checkbox"
                    type="checkbox"
                    aria-label="전체 선택"
                    checked={allSelected}
                    onChange={toggleSelectAll}
                    disabled={users.length === 0 || loadingUsers}
                  />
                </th>
                <th>아이디</th>
                <th>이름</th>
                <th>이메일</th>
                <th>권한</th>
                <th>가입일</th>
              </tr>
            </thead>
            <tbody>
              {users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="admin-user-empty">
                    {loadingUsers ? '불러오는 중...' : '조회된 사용자가 없습니다.'}
                  </td>
                </tr>
              ) : (
                users.map((user) => {
                  const isCurrentUser = currentLoginId === user.loginId;
                  return (
                    <tr key={user.loginId}>
                      <td>
                        <input
                          className="admin-user-checkbox"
                          type="checkbox"
                          checked={Boolean(selectedMap[user.loginId])}
                          onChange={() => toggleSelectUser(user.loginId)}
                        />
                      </td>
                      <td>
                        {user.loginId}
                        {isCurrentUser && <span className="admin-current-user-tag"> (내 계정)</span>}
                      </td>
                      <td>{user.username || '-'}</td>
                      <td>{user.email || '-'}</td>
                      <td>
                        <span
                          className={`admin-role-badge ${
                            user.roleName === 'ROLE_ADMIN' ? 'is-admin' : 'is-user'
                          }`}
                        >
                          {user.roleName || '-'}
                        </span>
                      </td>
                      <td>{formatAdminDateTime(user.createdAt)}</td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </section>

      <section className="note-panel admin-action-panel">
        <h2 className="note-panel-title">선택 사용자 작업</h2>
        <p className="muted-text">선택됨: {selectedLoginIds.length}명</p>
        <div className="admin-action-buttons">
          <button
            className="btn btn-danger"
            type="button"
            onClick={() => void deleteSelectedUsers()}
            disabled={actionLoading || loadingUsers}
          >
            선택 사용자 삭제
          </button>
          <button
            className="btn btn-primary"
            type="button"
            onClick={() => void sendUpdateNotification()}
            disabled={actionLoading || loadingUsers}
          >
            업데이트 메일 발송
          </button>
        </div>

        <div className="form-field">
          <label htmlFor="admin-update-content">업데이트 메일 내용</label>
          <textarea
            id="admin-update-content"
            className="admin-update-mail-textarea"
            placeholder="업데이트 내용을 입력하세요."
            value={mailContent}
            onChange={(event) => setMailContent(event.target.value)}
          />
        </div>

        <p className="muted-text">보내는 사람은 현재 로그인 사용자 이메일이 자동 적용됩니다.</p>
        {statusMessage && <p className="success-text">{statusMessage}</p>}
        {errorMessage && <p className="error-text">{errorMessage}</p>}
      </section>
    </div>
  );
}
