export type ApiErrorResponse = {
  code?: string;
  message?: string;
  details?: unknown;
  traceId?: string;
  timestamp?: string;
};

type HttpApiErrorOptions = {
  url: string;
  status: number;
  code: string;
  message: string;
  details?: unknown;
  traceId?: string;
  timestamp?: string;
};

/**
 * 서버가 반환한 HTTP 에러 응답을 표현한다.
 */
export class HttpApiError extends Error {
  readonly url: string;
  readonly status: number;
  readonly code: string;
  readonly details?: unknown;
  readonly traceId?: string;
  readonly timestamp?: string;

  constructor(options: HttpApiErrorOptions) {
    super(options.message);
    this.name = 'HttpApiError';
    this.url = options.url;
    this.status = options.status;
    this.code = options.code;
    this.details = options.details;
    this.traceId = options.traceId;
    this.timestamp = options.timestamp;
  }
}

/**
 * 네트워크 연결 단계에서 발생한 에러를 표현한다.
 */
export class NetworkApiError extends Error {
  readonly cause?: unknown;

  constructor(message: string, cause?: unknown) {
    super(message);
    this.name = 'NetworkApiError';
    this.cause = cause;
  }
}

/**
 * 응답 JSON이 표준 API 에러 형태인지 검사한다.
 */
export function isApiErrorResponse(value: unknown): value is ApiErrorResponse {
  if (!value || typeof value !== 'object') return false;
  const candidate = value as Record<string, unknown>;
  return (
    (typeof candidate.code === 'string' || candidate.code === undefined) &&
    (typeof candidate.message === 'string' || candidate.message === undefined) &&
    (typeof candidate.traceId === 'string' || candidate.traceId === undefined) &&
    (typeof candidate.timestamp === 'string' || candidate.timestamp === undefined)
  );
}

/**
 * 문자열 응답에서 표준 API 에러를 파싱한다.
 */
export function parseApiErrorResponse(text: string): ApiErrorResponse | null {
  if (!text) return null;
  try {
    const parsed = JSON.parse(text) as unknown;
    return isApiErrorResponse(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

/**
 * 사용자에게 표시할 에러 메시지를 만든다.
 */
export function formatErrorMessage(error: unknown, fallback: string): string {
  if (error instanceof HttpApiError) {
    return error.traceId ? `${error.message} (traceId: ${error.traceId})` : error.message;
  }
  if (error instanceof NetworkApiError) {
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}
