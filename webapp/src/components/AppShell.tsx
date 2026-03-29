import { useEffect, useState } from 'react';
import type { Dispatch, SetStateAction } from 'react';
import { Navigate, Outlet, useLocation, useNavigate } from 'react-router-dom';
import { PiRobotLight } from 'react-icons/pi';
import { apiFetch } from '../api/http';
import { formatErrorMessage } from '../api/errors';

type CurrentUser = {
  authenticated: boolean;
  loginId: string;
  email: string;
  roles: string[];
};

type CsrfPayload = {
  parameterName: string;
  token: string;
};

export type AppShellOutletContext = {
  setNoteHeaderTitle: Dispatch<SetStateAction<string>>;
};

const NOTE_HEADER_TITLE_MAX_CHARS = 25;

function truncateNoteTitleForUi(title: string): string {
  const normalized = title.trim();
  if (!normalized) return '새 채팅';
  const chars = Array.from(normalized);
  if (chars.length <= NOTE_HEADER_TITLE_MAX_CHARS) return normalized;
  return `${chars.slice(0, NOTE_HEADER_TITLE_MAX_CHARS).join('')}...`;
}

function pageMeta(pathname: string, noteHeaderTitle: string): { title: string; subtitle: string } {
  if (pathname.startsWith('/admin')) {
    return {
      title: 'Admin',
      subtitle: 'DocuSearch Administration',
    };
  }
  if (pathname.startsWith('/board')) {
    return {
      title: 'Board',
      subtitle: 'DocuSearch Community',
    };
  }
  return {
    title: truncateNoteTitleForUi(noteHeaderTitle),
    subtitle: 'DocuSearch Notebook',
  };
}

export function AppShell() {
  const location = useLocation();
  const navigate = useNavigate();
  const [user, setUser] = useState<CurrentUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [noteHeaderTitle, setNoteHeaderTitle] = useState('새 채팅');

  const isNoteRoute = location.pathname.startsWith('/note');
  const isBoardRoute = location.pathname.startsWith('/board');
  const headerClassName = isNoteRoute
    ? 'top-header notebook-top-header note-top-header'
    : 'top-header notebook-top-header';
  const meta = pageMeta(location.pathname, noteHeaderTitle);
  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false;

  useEffect(() => {
    let mounted = true;
    apiFetch<CurrentUser>('/api/auth/me')
      .then((data) => {
        if (!mounted) return;
        setUser({
          authenticated: data.authenticated,
          loginId: data.loginId,
          email: data.email ?? '',
          roles: Array.isArray(data.roles) ? data.roles : [],
        });
      })
      .catch((error) => {
        if (!mounted) return;
        console.warn(formatErrorMessage(error, '인증 상태를 확인하지 못했습니다.'));
        setUser(null);
      })
      .finally(() => {
        if (!mounted) return;
        setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [location.pathname]);

  const handleLogout = async () => {
    const csrf = await apiFetch<CsrfPayload>('/api/auth/csrf');
    const form = document.createElement('form');
    form.method = 'post';
    form.action = '/logout';
    const hidden = document.createElement('input');
    hidden.type = 'hidden';
    hidden.name = csrf.parameterName;
    hidden.value = csrf.token;
    form.appendChild(hidden);
    document.body.appendChild(form);
    form.submit();
  };

  if (loading) {
    return <div className="login-root">로딩 중...</div>;
  }

  if (!user?.authenticated) {
    return <Navigate to="/login" replace />;
  }

  return (
    <div className="app-layout">
      <header className={headerClassName}>
        <div className="note-brand">
          <PiRobotLight className="note-brand-logo" aria-hidden="true" focusable="false" />
          <div className="note-brand-text">
            <strong className="note-brand-title">{meta.title}</strong>
            <span>{meta.subtitle}</span>
          </div>
        </div>
        <div className="top-header-actions">
          {isAdmin && (
            <button
              className="btn btn-secondary note-header-admin-btn"
              type="button"
              onClick={() => navigate('/admin')}
            >
              Admin
            </button>
          )}
          <button
            className="btn btn-secondary note-header-board-btn"
            type="button"
            onClick={() => navigate(isBoardRoute ? '/note' : '/board')}
          >
            {isBoardRoute ? 'Notebook' : 'Board'}
          </button>
          <button className="btn btn-secondary" type="button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>
      <main className="page-wrap">
        <Outlet context={{ setNoteHeaderTitle }} />
      </main>
    </div>
  );
}
