import { Component } from 'react';
import type { ErrorInfo, ReactNode } from 'react';

type AppErrorBoundaryProps = {
  children: ReactNode;
};

type AppErrorBoundaryState = {
  hasError: boolean;
  message: string;
};

/**
 * 렌더링 단계의 예기치 않은 오류를 전역에서 처리한다.
 */
export class AppErrorBoundary extends Component<AppErrorBoundaryProps, AppErrorBoundaryState> {
  state: AppErrorBoundaryState = {
    hasError: false,
    message: '',
  };

  static getDerivedStateFromError(error: Error): AppErrorBoundaryState {
    return {
      hasError: true,
      message: error.message || '예기치 않은 오류가 발생했습니다.',
    };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('AppErrorBoundary caught an error', error, errorInfo);
  }

  render() {
    if (!this.state.hasError) {
      return this.props.children;
    }
    return (
      <div className="login-root">
        <div className="login-card">
          <h1 className="login-title">오류가 발생했습니다</h1>
          <p className="error-text">{this.state.message}</p>
          <button className="btn btn-primary btn-block" type="button" onClick={() => window.location.reload()}>
            새로고침
          </button>
        </div>
      </div>
    );
  }
}
