import DOMPurify from 'dompurify';
import { marked } from 'marked';

/**
 * 마크다운 문자열을 안전한 HTML로 변환한다.
 */
export function renderMarkdown(input: string): string {
  const rawHtml = marked.parse(input ?? '', { breaks: true, gfm: true, async: false });
  return DOMPurify.sanitize(rawHtml);
}
