import { HttpApiError, NetworkApiError, parseApiErrorResponse } from './errors';

/**
 * 공통 fetch 래퍼. 401은 로그인 페이지로 보낸다.
 */
export async function apiFetch<T>(url: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(url, {
      credentials: 'same-origin',
      ...init,
    });
  } catch (error) {
    throw new NetworkApiError('네트워크 오류로 요청에 실패했습니다.', error);
  }

  if (!response.ok) {
    const text = await response.text();
    const parsed = parseApiErrorResponse(text);
    const error = new HttpApiError({
      url,
      status: response.status,
      code: parsed?.code ?? (response.status === 401 ? 'SESSION_EXPIRED' : 'HTTP_ERROR'),
      message: parsed?.message?.trim() || text || `요청 실패 (${response.status})`,
      details: parsed?.details,
      traceId: parsed?.traceId,
      timestamp: parsed?.timestamp,
    });
    if (response.status === 401 && error.code === 'SESSION_EXPIRED') {
      window.location.href = '/app/login';
    }
    throw error;
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}
