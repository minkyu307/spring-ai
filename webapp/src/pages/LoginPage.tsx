import { useEffect, useMemo, useState } from 'react';
import type { FormEvent } from 'react';
import { formatErrorMessage } from '../api/errors';
import { apiFetch } from '../api/http';

type LoginMeta = {
  csrfParameterName: string;
  csrfToken: string;
};

type SignUpForm = {
  loginId: string;
  username: string;
  email: string;
  password: string;
  passwordConfirm: string;
};

const initialSignUp: SignUpForm = {
  loginId: '',
  username: '',
  email: '',
  password: '',
  passwordConfirm: '',
};

export function LoginPage() {
  const params = useMemo(() => new URLSearchParams(window.location.search), []);
  const hasLoginError = params.has('error');
  const [meta, setMeta] = useState<LoginMeta | null>(null);
  const [mode, setMode] = useState<'login' | 'signup'>('login');
  const [signUpForm, setSignUpForm] = useState<SignUpForm>(initialSignUp);
  const [signUpError, setSignUpError] = useState('');
  const [signUpSuccess, setSignUpSuccess] = useState('');
  const [metaError, setMetaError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    apiFetch<LoginMeta>('/api/auth/login-meta')
      .then((data) => {
        setMeta(data);
        setMetaError('');
      })
      .catch((error) => {
        setMetaError(formatErrorMessage(error, '로그인 준비 정보를 불러오지 못했습니다.'));
      });
  }, []);

  const handleSignUp = async (e: FormEvent) => {
    e.preventDefault();
    setSignUpError('');
    setSignUpSuccess('');
    setLoading(true);
    try {
      await apiFetch<{ message: string }>('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(signUpForm),
      });
      setSignUpSuccess('회원가입이 완료되었습니다. 로그인해 주세요.');
      setMode('login');
      setSignUpForm(initialSignUp);
    } catch (error) {
      setSignUpError(formatErrorMessage(error, '회원가입에 실패했습니다.'));
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-root">
      <div className="login-card">
        <h1 className="login-title">{mode === 'login' ? '로그인' : '회원가입'}</h1>

        {mode === 'login' && signUpSuccess && <p className="success-text">{signUpSuccess}</p>}
        {mode === 'login' && hasLoginError && (
          <p className="error-text">아이디 또는 비밀번호가 올바르지 않습니다.</p>
        )}
        {mode === 'login' && metaError && <p className="error-text">{metaError}</p>}

        {mode === 'login' ? (
          <>
            <form action="/login" method="post">
              <input
                type="hidden"
                name={meta?.csrfParameterName ?? '_csrf'}
                value={meta?.csrfToken ?? ''}
              />
              <div className="form-field">
                <label htmlFor="login-id">아이디</label>
                <input id="login-id" name="username" required maxLength={64} />
              </div>
              <div className="form-field">
                <label htmlFor="login-password">비밀번호</label>
                <input id="login-password" name="password" type="password" required />
              </div>
              <button className="btn btn-primary login-action-btn" type="submit">
                로그인
              </button>
            </form>
            <button
              className="btn btn-secondary login-action-btn login-action-btn-secondary"
              type="button"
              onClick={() => setMode('signup')}
            >
              회원가입
            </button>
          </>
        ) : (
          <>
            <form onSubmit={handleSignUp}>
              <div className="form-field">
                <label>아이디</label>
                <input
                  value={signUpForm.loginId}
                  onChange={(e) => setSignUpForm((prev) => ({ ...prev, loginId: e.target.value }))}
                  required
                  maxLength={64}
                />
              </div>
              <div className="form-field">
                <label>이름</label>
                <input
                  value={signUpForm.username}
                  onChange={(e) => setSignUpForm((prev) => ({ ...prev, username: e.target.value }))}
                  required
                  maxLength={100}
                />
              </div>
              <div className="form-field">
                <label>이메일</label>
                <input
                  type="email"
                  value={signUpForm.email}
                  onChange={(e) => setSignUpForm((prev) => ({ ...prev, email: e.target.value }))}
                  required
                />
              </div>
              <div className="form-field">
                <label>비밀번호</label>
                <input
                  type="password"
                  value={signUpForm.password}
                  onChange={(e) => setSignUpForm((prev) => ({ ...prev, password: e.target.value }))}
                  required
                />
              </div>
              <div className="form-field">
                <label>비밀번호 확인</label>
                <input
                  type="password"
                  value={signUpForm.passwordConfirm}
                  onChange={(e) =>
                    setSignUpForm((prev) => ({ ...prev, passwordConfirm: e.target.value }))
                  }
                  required
                />
              </div>
              {signUpError && <p className="error-text">{signUpError}</p>}
              <button className="btn btn-primary btn-block" type="submit" disabled={loading}>
                가입하기
              </button>
            </form>
            <button
              className="btn btn-secondary btn-block"
              type="button"
              style={{ marginTop: 8 }}
              onClick={() => setMode('login')}
            >
              로그인으로 돌아가기
            </button>
          </>
        )}
      </div>
    </div>
  );
}
